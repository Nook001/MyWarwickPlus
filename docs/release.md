# 签名与发布

本流程用于生成和分发正式签名 APK。当前发行和验证结果见 [开发与验证](development.md)，用户说明由 [release-notes.md](release-notes.md)维护。

## 密钥与备份

当前签名密钥在 `%LOCALAPPDATA%/MyWarwickPlus/signing/release.p12`，alias 为 `mywarwickplus-release`；同目录 `credentials.xml` 由当前 Windows 用户的 DPAPI 加密，不能作为跨电脑或重装后的密码备份。

用户需要独立备份 **keystore 和实际密码**，分开安全保存。可自行在本机 PowerShell 将密码复制到密码管理器，勿发送到聊天或 Issues：

```powershell
$signing = Import-Clixml (Join-Path $env:LOCALAPPDATA 'MyWarwickPlus/signing/credentials.xml')
Set-Clipboard $signing.Credential.GetNetworkCredential().Password
# 粘贴至密码管理器，然后清剪贴板：
Set-Clipboard ''
$signing = $null
```

已发布后不要重建或覆盖密钥；`initialize-signing.ps1` 仅用于首次创建。后续更新保持包名和签名，versionCode 递增。公开证书指纹保存在 `tools/release/certificate-sha256.txt`，打包时默认核对。

## 打包

1. 更新 Gradle 版本号/versionCode 与英文发布说明；提交并确认工作区干净。
2. 使用新 tag 打包，脚本检查实际 APK 的签名、包名、版本、最低 SDK、非 Debug 和源码一致性。
3. 按 [手动检查表](development.md#手动检查)验证该 Release，记录实际结果。

```powershell
$env:JAVA_HOME = 'D:/Dev/JDK21.0.8'
$env:ANDROID_HOME = 'D:/Dev/AndroidSDK'
$tag = 'v0.27.0-beta.1' # 示例：必须与本次已更新的 versionName 一致
& tools/release/package.ps1 -Tag $tag
```

脚本临时提供四个 `MWP_RELEASE_*` 环境变量，并禁用 Gradle configuration cache，结束后恢复环境。无凭据可构建 unsigned，但不能通过发布检查。签名配置不写入仓库，不用 debug/build scan 输出签名秘密。

| 位置 | 内容与用途 |
| --- | --- |
| `dist/<tag>/public/` | 仅签名 APK 和 `SHA256SUMS.txt`，作为下载附件 |
| `dist/<tag>/private/` | mapping、构建/依赖日志、签名与 Manifest 核对、源码/签名/哈希清单、发布说明副本；保留本地归档 |
| GitHub Release 正文 | 使用发布说明副本，不另上传同内容 Markdown 附件 |
| 仓库根与 APK `assets/legal/` | 隐私、MIT、Apache-2.0 和第三方声明；不重复上传为独立附件 |

`dist/` 不入 Git；private 中的发布说明可用于 Release 正文，其余构建记录不上传。脚本不安装、不创建 tag、不自动发布，也拒绝替换既有 dist 目录。

首个 Beta 使用旧附件布局，包含八个附件，manifest 在 `public/`；本次整理保留这些已发布文件。新打包使用上表布局，不混用两个目录位置。

GitHub 还会[自动提供源码 ZIP/tar.gz](https://docs.github.com/en/repositories/releasing-projects-on-github/about-releases)，它们不属于脚本上传的两个附件。

## 安装与验收

```powershell
$apk = "dist/$tag/public/MyWarwickPlus-$($tag.Substring(1)).apk"
& 'D:/Dev/AndroidSDK/platform-tools/adb.exe' -s 10AG4S2KQJ0066R install -r $apk
& 'D:/Dev/AndroidSDK/platform-tools/adb.exe' -s 10AG4S2KQJ0066R shell am start -n io.github.nook001.mywarwickplus/uk.ac.warwick.plus.MainActivity
```

用户完成 SSO/MFA 和实际页面验收，明确核对 Release 包；新公开包与旧内部/Debug/Profile 数据隔离。覆盖安装保留同包数据，无需卸载旧应用。公开 schema 5 起的数据库变更需要迁移，不能静默清缓存。

## 创建 GitHub Release

核对 manifest 的 sourceCommit，让 tag 对应实际打包源码，先创建 Draft、上传两个 public 文件，再按发行安排公开。当前 Latest 为 0.27.1-beta.1；0.27.0-beta.1 保留原 tag 和 APK。每次代码修复发新版本，不覆盖已发布 APK/tag。

```powershell
$manifest = Get-Content "dist/$tag/private/release-manifest.json" -Raw | ConvertFrom-Json
$notes = "dist/$tag/private/RELEASE_NOTES.md"
gh release create $manifest.tag --repo Nook001/MyWarwickPlus --target $manifest.sourceCommit --draft --title "MyWarwick+ $($manifest.versionName)" --notes-file $notes
gh release upload $manifest.tag "dist/$tag/public/$($manifest.apk)" "dist/$tag/public/SHA256SUMS.txt" --repo Nook001/MyWarwickPlus
# 发布时核对附件/标签/说明后执行：
gh release edit $manifest.tag --repo Nook001/MyWarwickPlus --draft=false --prerelease=false --latest
```

脚本和构建/核对由 Codex 执行；密钥独立备份、账号操作、手测和学校允许范围由用户确认。MIT 仅覆盖本项目代码，不授权学校服务、内容或品牌。发布前检查附件/截图/源码不含私人数据。

学校内部接口可用不代表第三方客户端分发获得许可。可向 IDG 说明官方 SSO/MFA、本人只读数据、无密码收集/中转服务器、有界刷新，询问这种方式和公开分发的允许范围；这与申请新的 OAuth application 是不同问题。当前没有学校书面确认，不把 Beta 或免责声明当许可。

参考：[Android 签名](https://developer.android.com/studio/publish/app-signing)、[GitHub Releases](https://docs.github.com/en/repositories/releasing-projects-on-github/managing-releases-in-a-repository)、[Warwick IMST04](https://warwick.ac.uk/services/secretarytocouncil/info-security/im-policy-framework/standards/imst04/)。
