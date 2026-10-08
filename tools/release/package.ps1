[CmdletBinding()]
param(
    [Parameter(Mandatory)][ValidatePattern('^v[0-9]+\.[0-9]+\.[0-9]+(-[A-Za-z0-9.-]+)?$')][string]$Tag,
    [string]$JdkPath = $env:JAVA_HOME,
    [string]$SdkPath = $env:ANDROID_HOME,
    [string]$CredentialsPath = (Join-Path $env:LOCALAPPDATA 'MyWarwickPlus\signing\credentials.xml'),
    [string]$ExpectedCertificateSha256 = (Get-Content -LiteralPath (Join-Path $PSScriptRoot 'certificate-sha256.txt') -Raw).Trim()
)
$ErrorActionPreference = 'Stop'
$repository = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..'))
Push-Location $repository
$variables = @('JAVA_HOME', 'MWP_RELEASE_STORE_FILE', 'MWP_RELEASE_KEY_ALIAS', 'MWP_RELEASE_STORE_PASSWORD', 'MWP_RELEASE_KEY_PASSWORD')
$previous = @{}
foreach ($name in $variables) { $previous[$name] = [Environment]::GetEnvironmentVariable($name, 'Process') }
try {
    $status = & git status --porcelain
    if ($LASTEXITCODE -ne 0 -or $status) { throw 'Commit tracked changes before packaging a release.' }
    $commit = (& git rev-parse HEAD).Trim()
    if ($LASTEXITCODE -ne 0) { throw 'Cannot determine source commit.' }
    $signing = Import-Clixml -LiteralPath $CredentialsPath
    if (!(Test-Path -LiteralPath $signing.StoreFile)) { throw 'Keystore was not found.' }
    $env:JAVA_HOME = $JdkPath
    $env:MWP_RELEASE_STORE_FILE = $signing.StoreFile
    $env:MWP_RELEASE_KEY_ALIAS = $signing.KeyAlias
    $env:MWP_RELEASE_STORE_PASSWORD = $signing.Credential.GetNetworkCredential().Password
    $env:MWP_RELEASE_KEY_PASSWORD = $env:MWP_RELEASE_STORE_PASSWORD
    New-Item -ItemType Directory -Path 'work\release' -Force | Out-Null
    # Never serialize signing passwords into Gradle's configuration cache.
    & .\gradlew.bat :app:checkReleaseSigning :app:assembleRelease :app:lintRelease :app:testDebugUnitTest `
        :app:dependencies --configuration releaseRuntimeClasspath --no-configuration-cache --console=plain *> 'work\release\build.log'
    if ($LASTEXITCODE -ne 0) { throw 'Release checks failed. See work/release/build.log.' }
    $metadata = Get-Content -LiteralPath 'app\build\outputs\apk\release\output-metadata.json' -Raw | ConvertFrom-Json
    $element = @($metadata.elements)
    if ($element.Count -ne 1 -or $metadata.applicationId -ne 'io.github.nook001.mywarwickplus' -or $metadata.variantName -ne 'release') {
        throw 'Unexpected release artifact metadata.'
    }
    if ($element[0].versionName -ne $Tag.Substring(1)) { throw 'Tag does not match the APK version.' }
    $apk = Join-Path 'app\build\outputs\apk\release' $element[0].outputFile
    if ($element[0].outputFile -match 'unsigned') { throw 'The APK is unsigned.' }
    $buildTools = Get-ChildItem -LiteralPath (Join-Path $SdkPath 'build-tools') -Directory |
        Where-Object { $_.Name -match '^\d+\.\d+\.\d+$' } | Sort-Object { [version]$_.Name } -Descending | Select-Object -First 1
    if (!$buildTools) { throw 'Android SDK Build Tools were not found.' }
    $verification = & (Join-Path $buildTools.FullName 'apksigner.bat') verify --verbose --print-certs $apk 2>&1
    if ($LASTEXITCODE -ne 0) { throw 'APK signature verification failed.' }
    $certificate = [regex]::Match(($verification -join "`n"), '(?:Signer #1|V\d(?:\.\d+)? Signer):? certificate SHA-256 digest: ([a-fA-F0-9]+)').Groups[1].Value.ToLowerInvariant()
    if (!$certificate) { throw 'Signing certificate digest was not found.' }
    if ($ExpectedCertificateSha256 -and $certificate -ne $ExpectedCertificateSha256.Replace(':', '').ToLowerInvariant()) {
        throw 'Signing certificate differs from the expected release identity.'
    }
    $badging = & (Join-Path $buildTools.FullName 'aapt2.exe') dump badging $apk 2>&1
    if ($LASTEXITCODE -ne 0 -or ($badging -join "`n") -match 'application-debuggable') { throw 'Release manifest verification failed.' }
    if (($badging -join "`n") -notmatch "(?:minSdkVersion|sdkVersion):'28'") { throw 'Unexpected minimum SDK.' }
    if (($badging -join "`n") -notmatch "package: name='io.github.nook001.mywarwickplus'") { throw 'APK package identity does not match metadata.' }
    if ((& git status --porcelain) -or (& git rev-parse HEAD).Trim() -ne $commit) { throw 'Source changed during the build.' }
    $destination = Join-Path 'dist' $Tag
    if (Test-Path -LiteralPath $destination) { throw 'Release folder already exists; preserve it rather than replacing published bytes.' }
    $public = Join-Path $destination 'public'
    $private = Join-Path $destination 'private'
    New-Item -ItemType Directory -Path $public, $private | Out-Null
    $name = "MyWarwickPlus-$($element[0].versionName).apk"
    Copy-Item -LiteralPath $apk -Destination (Join-Path $public $name)
    $hash = (Get-FileHash -LiteralPath (Join-Path $public $name) -Algorithm SHA256).Hash.ToLowerInvariant()
    "$hash  $name" | Set-Content -LiteralPath (Join-Path $public 'SHA256SUMS.txt') -Encoding ascii
    Copy-Item -LiteralPath 'app\build\outputs\mapping\release\mapping.txt' -Destination $private
    Copy-Item -LiteralPath 'work\release\build.log' -Destination $private
    $verification | Set-Content -LiteralPath (Join-Path $private 'signature-verification.txt') -Encoding utf8
    $badging | Set-Content -LiteralPath (Join-Path $private 'manifest-badging.txt') -Encoding utf8
    [ordered]@{
        tag = $Tag; sourceCommit = $commit; package = $metadata.applicationId
        versionName = $element[0].versionName; versionCode = $element[0].versionCode
        minimumAndroid = '9'; roomSchema = 5; apk = $name
        apkSha256 = $hash; signingCertificateSha256 = $certificate
    } | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $private 'release-manifest.json') -Encoding utf8
    Copy-Item -LiteralPath 'docs\release-notes.md' -Destination (Join-Path $private 'RELEASE_NOTES.md')
    Write-Output "Verified release bundle: $([IO.Path]::GetFullPath($destination))"
    Write-Output "Signing certificate SHA-256: $certificate"
    Write-Output 'Publish public/ only. Keep private/ locally; manual acceptance and distribution permission remain separate.'
} finally {
    foreach ($name in $variables) { [Environment]::SetEnvironmentVariable($name, $previous[$name], 'Process') }
    Pop-Location
}
