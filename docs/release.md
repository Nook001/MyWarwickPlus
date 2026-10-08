# 首次发布与后续交付

候选：`v0.26.0-beta.1`，versionCode39，Android 9+，Room schema5。
公开包名 `io.github.nook001.mywarwickplus`；Debug 为 `.debug`、Profile 为 `.profile`。
namespace/Activity 类名仍为 `uk.ac.warwick.plus`。旧内部包保留，不复制其认证或私人数据。

## 工作分工

| 工作 | 执行与完成标准 |
| --- | --- |
| 签名、打包、签名/Manifest 核对、校验值 | Codex/本地脚本；检查实际 Release APK，不以 Debug 结果替代 |
| 既有 JVM 检查、Lint、Release R8 构建 | Codex；无需新增/运行 UI Test |
| README、MIT、隐私、依赖说明、发布说明 | Codex；隐私描述实际行为，不宣称端到端加密或学校授权 |
| 密钥与密码独立备份 | 用户；验证在另一安全位置可恢复，不能只有本机加密凭据 |
| 登录/MFA、退出和各页面验收 | 用户；只在明确的 Release 应用内操作，不清旧内部包数据 |
| 学校允许的第三方客户端发行范围 | 用户确认；Codex可整理咨询要点，不自行发消息 |
| Draft / Pre-release 附件与最后发布 | 先核对源码/附件/许可/验收，再执行；本轮不自动公开发布 |

## 签名与打包

签名只从四个 `MWP_RELEASE_*` 环境变量读取。无变量时贡献者可构建 unsigned
Release；`checkReleaseSigning` 和打包脚本拒绝将 unsigned APK 当作发布产物。
正式构建禁用 Gradle configuration cache，防止序列化签名密码；不要用
`--debug`、build scan、共享日志等方式泄露签名配置。

Windows 本机命令：

```powershell
$env:JAVA_HOME = 'D:/Dev/JDK21.0.8'
$env:ANDROID_HOME = 'D:/Dev/AndroidSDK'
# 首次使用一次；已生成后绝不覆盖/重新生成：
& tools/release/initialize-signing.ps1
# 确认提交且工作区干净后：
& tools/release/package.ps1 -Tag v0.26.0-beta.1
```

当前密钥位于 `%LOCALAPPDATA%/MyWarwickPlus/signing/release.p12`，凭据在同目录
`credentials.xml`，采用当前 Windows 用户的 DPAPI 加密并限制目录访问。
打包脚本只临时向子进程提供密码，结束后恢复环境；不会打印密码或上传密钥。

**用户必须备份 keystore 和密码，XML 本身不能跨 Windows 账户/重装后恢复。**
自行在本机 PowerShell 中将密码复制到密码管理器，勿把命令结果发给 Codex或Issues：

```powershell
$signing = Import-Clixml (Join-Path $env:LOCALAPPDATA 'MyWarwickPlus/signing/credentials.xml')
Set-Clipboard $signing.Credential.GetNetworkCredential().Password
# 粘贴至密码管理器，记录 alias = mywarwickplus-release；然后：
Set-Clipboard ''
$signing = $null
```

将 `release.p12` 复制到独立、安全的备份位置；密码与密钥分开保存。后续签名证书
SHA-256已记录在`tools/release/certificate-sha256.txt`，脚本默认核对；也可通过
`-ExpectedCertificateSha256`显式指定，避免误换密钥。指纹是公开信息，不是私钥。

脚本输出 `dist/<tag>/public/`（APK、SHA256SUMS、发布说明、源码提交/签名指纹
manifest、许可证/隐私）与 `private/`（mapping、构建/依赖与核对记录）。只上传
public 中的附件；private 长期本地归档，帮助解混淆崩溃。dist 不入Git。
脚本不安装应用、不操作账号、不创建标签、不发布 Release。

## 物理手机手动验收

指定物理手机 `10AG4S2KQJ0066R`，使用 `install -r`；安装新包无需删除旧应用。
新包显示 `MyWarwick+`，内部旧包同名，安装后通过明确组件启动确认目标。

```powershell
& 'D:/Dev/AndroidSDK/platform-tools/adb.exe' -s 10AG4S2KQJ0066R install -r dist/v0.26.0-beta.1/public/MyWarwickPlus-0.26.0-beta.1.apk
& 'D:/Dev/AndroidSDK/platform-tools/adb.exe' -s 10AG4S2KQJ0066R shell am start -W -n io.github.nook001.mywarwickplus/uk.ac.warwick.plus.MainActivity
```

| 检查 | 用户操作 / 通过条件 |
| --- | --- |
| 新安装与认证 | 新包首次登录，Warwick SSO/MFA后返回；姓名/邮箱、数据正常 |
| Home / Classes / Tasks | 时间、截止日期正确，选日/搜索/筛选/详情/地点与快捷链接正常 |
| Inbox | 搜索/清除、打开/关闭详情、All、最新刷新、更早分页正常 |
| 离线 | 飞行模式打开各页；已有缓存保留、失败反馈可恢复；恢复联网后正常刷新 |
| 外观与无障碍 | 浅/深颜色主题各一，系统大字体不重叠；五Tab标签完整 |
| 重启与更新 | 关闭应用/重开后登录和缓存保留；同签名 Release 覆盖安装后再验证 |
| 本地退出 | 最后在新 Release 内退出，数据消失；重新登录成功，不影响旧包/浏览器 |

覆盖相同 APK 只能验证重装保留数据，不能证明未来 schema 迁移。测试另一账户
不是本轮默认操作；没有授权账号则明确留空，不宣称多账户实测。

## 对外说明与许可

MIT仅覆盖本项目代码；依赖保留自身许可，学校内容/品牌不归本项目授权。
正式发布前检查截图、Git历史与附件没有私人数据，README/PRIVACY明确非官方身份。
不要把 Issues 当作私人支持渠道，不要求用户上传完整 HAR/logcat/database。

学校内部接口可用不代表第三方发行已被允许。用户向 IDG确认时可说明：Android
学生客户端、官方SSO/MFA、不接收密码、不设中转服务器、仅本人只读数据、有限刷新、
不修改账户/已读状态；询问该方式与公开Beta分发是否被允许及有没有推荐接口。
这与申请新的 OAuth application 是不同问题，不能用 Beta/免责声明替代确认。

## 创建发布

验收/备份/允许范围确认后，核对 `release-manifest.json` 的 `sourceCommit`，将
对应 tag 指向该提交，在 GitHub创建 Draft，添加 public 附件，再标为 Pre-release。
不要沿用旧的 `app-release-unsigned.apk`、Debug包或profile包。

第一次使用CLI时可手动核对：

```powershell
$manifest = Get-Content dist/v0.26.0-beta.1/public/release-manifest.json -Raw | ConvertFrom-Json
gh release create $manifest.tag --repo Nook001/MyWarwickPlus --target $manifest.sourceCommit --draft --prerelease --title 'MyWarwick+ 0.26.0-beta.1' --notes-file dist/v0.26.0-beta.1/public/RELEASE_NOTES.md
gh release upload $manifest.tag dist/v0.26.0-beta.1/public/* --repo Nook001/MyWarwickPlus
```

正式发布后保持包名/签名，versionCode递增，从公开schema5起新增数据库版本必须有
迁移；不得使用 destructive fallback 静默清缓存。已发布tag/APK不覆盖，修复发新版本。
首轮不必增加自动更新或复杂CI，保留手动、可核对的发布流程。

参考：[Android签名](https://developer.android.com/studio/publish/app-signing)、
[apksigner](https://developer.android.com/tools/apksigner)、
[GitHub Releases](https://docs.github.com/en/repositories/releasing-projects-on-github/managing-releases-in-a-repository)、
[Warwick IMST04](https://warwick.ac.uk/services/secretarytocouncil/info-security/im-policy-framework/standards/imst04/)。
