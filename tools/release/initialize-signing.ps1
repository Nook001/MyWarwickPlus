[CmdletBinding()]
param(
    [string]$JdkPath = $env:JAVA_HOME,
    [string]$SigningDirectory = (Join-Path $env:LOCALAPPDATA 'MyWarwickPlus\signing')
)
$ErrorActionPreference = 'Stop'
$keytool = Join-Path $JdkPath 'bin\keytool.exe'
if (!(Test-Path -LiteralPath $keytool)) { throw 'Set JAVA_HOME to a JDK before initializing signing.' }
$directory = [IO.Path]::GetFullPath($SigningDirectory)
$repository = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..'))
if ($directory.StartsWith($repository + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase) -or $directory -eq $repository) {
    throw 'Signing material must be outside the repository.'
}
$store = Join-Path $directory 'release.p12'
$credentials = Join-Path $directory 'credentials.xml'
if ((Test-Path -LiteralPath $store) -or (Test-Path -LiteralPath $credentials)) {
    throw 'Signing material already exists. Reuse and back it up; do not regenerate it.'
}
New-Item -ItemType Directory -Path $directory -Force | Out-Null
# Protect this dedicated directory with the current Windows user's ACL.
$identity = [Security.Principal.WindowsIdentity]::GetCurrent().User
$acl = [Security.AccessControl.DirectorySecurity]::new()
$acl.SetAccessRuleProtection($true, $false)
$acl.AddAccessRule([Security.AccessControl.FileSystemAccessRule]::new($identity, 'FullControl',
    'ContainerInherit, ObjectInherit', 'None', 'Allow'))
Set-Acl -LiteralPath $directory -AclObject $acl
$bytes = [byte[]]::new(32)
$rng = [Security.Cryptography.RandomNumberGenerator]::Create()
try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
$password = [Convert]::ToBase64String($bytes)
$previous = $env:MWP_INIT_STORE_PASSWORD
try {
    $env:MWP_INIT_STORE_PASSWORD = $password
    $result = & $keytool -genkeypair -noprompt -storetype PKCS12 -keystore $store -alias 'mywarwickplus-release' `
        -keyalg RSA -keysize 3072 -validity 10000 -dname 'CN=MyWarwickPlus, O=Nook001' `
        -storepass:env MWP_INIT_STORE_PASSWORD -keypass:env MWP_INIT_STORE_PASSWORD 2>&1
    if ($LASTEXITCODE -ne 0) { throw 'Key generation failed; inspect the local keytool setup without exposing passwords.' }
    $secret = ConvertTo-SecureString $password -AsPlainText -Force
    [pscustomobject]@{
        StoreFile = $store
        KeyAlias = 'mywarwickplus-release'
        Credential = [pscredential]::new('mywarwickplus-release', $secret)
    } | Export-Clixml -LiteralPath $credentials
} finally {
    $env:MWP_INIT_STORE_PASSWORD = $previous
    $password = $null
    [Array]::Clear($bytes, 0, $bytes.Length)
}
Write-Output "Keystore: $store"
Write-Output "Windows-user-encrypted credentials: $credentials"
Write-Output 'Back up the keystore AND its password independently. DPAPI credentials are not portable.'
