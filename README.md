# MyWarwick+

独立的 Android 学生客户端，Kotlin + Jetpack Compose。通过官方 WebView SSO 登录，复用 MyWarwick session 与只读聚合接口，不申请新的 OAuth application；不是 Warwick 官方应用，也不代表已获得学校发行授权。

当前候选：**0.26.0-beta.1 / versionCode 39**。Android 9+，需要Warwick账号；构建、物理安装、手动验收与公开发布是不同状态，见 [验收记录](docs/validation.md)。

首次发布准备见 [发布流程](docs/release.md) 与 [发布说明](docs/release-notes.md)。公开Beta附件尚未发布，请不要把Debug/unsigned包作为正式发行包。

## 当前功能

| 页面 | 内容 |
| --- | --- |
| Home | 招呼语、Now / Next、四个网站入口、Today剩余课程/提前预览Tomorrow、周末祝语、最近三个未来截止日期、最近两条Messages摘要 |
| Classes | Schedule 连续日期列表、日期跳转、冲突/跨日提示、课程详情与地点链接 |
| Tasks | Coursework 搜索、Upcoming / Past、准确截止时间、详情与原站链接 |
| Inbox | Messages 独立 Tab、搜索、详情、下拉刷新最新页与显式加载更早消息 |
| Me | 账户邮箱与显式复制、设置、Modules / Library、八个网站入口、页面末尾本地 Sign out |

五套固定颜色主题共用静态渐变背景和可选细纹理。启动先显示缓存；下拉刷新使用真实任务阶段进度，失败保留已存数据。日期按英国时区显示。当前没有后台同步、系统通知、提交作业或消息已读写回。

公开版以Room schema5为升级基线；后续数据库版本必须提供迁移，不恢复内部schema1–4。公开包名`io.github.nook001.mywarwickplus`，Debug/Profile使用独立后缀；旧内部`uk.ac.warwick.plus`继续保留，各包登录与缓存隔离，新包需独立登录。

## 隐私、许可与反馈

[隐私说明](PRIVACY.md) · [MIT许可证](LICENSE) · [第三方许可](THIRD_PARTY_NOTICES.md)

数据直接在设备与学校间传输，无项目中转服务器、广告SDK或自动崩溃上传；本地缓存/会话与退出行为见隐私说明。Me提供Privacy/Licenses入口。依赖与学校内容不由本项目MIT重新授权。

[GitHub Issues](https://github.com/Nook001/MyWarwickPlus/issues)用于脱敏反馈；请附应用/Android版本、复现步骤，勿上传私人消息、邮箱、学号、cookie/token、完整HAR或数据库。

## 开发与部署

Android Studio 打开本目录；需要 JDK 21、Android SDK Platform 37.0。compileSdk / targetSdk 37、minSdk 28；依赖版本以 Gradle 文件为准。SDK 路径保存在不跟踪的 local.properties。

本机命令：

```powershell
$env:JAVA_HOME = 'D:/Dev/JDK21.0.8'
.\gradlew.bat :app:assembleDebug
# 按改动范围运行必要的既有 JVM 检查；完整 JVM 入口：
.\gradlew.bat :app:testDebugUnitTest
& 'D:/Dev/AndroidSDK/platform-tools/adb.exe' -s 10AG4S2KQJ0066R install -r app/build/outputs/apk/debug/app-debug.apk
& 'D:/Dev/AndroidSDK/platform-tools/adb.exe' -s 10AG4S2KQJ0066R shell am start -n io.github.nook001.mywarwickplus.debug/uk.ac.warwick.plus.MainActivity
```

遵守 [AGENTS.md](AGENTS.md)：功能阶段未经用户显式要求不编写或扩展 UI Test；简单 UI 调整不新增单元测试。日常部署明确指定物理手机并覆盖安装，保留登录与缓存。既有 androidTest 不是默认交付检查，不在已登录手机运行认证清理或可能卸载应用的测试流程。

性能采集使用`:app:assembleProfile`：沿用Release的R8/资源收缩，以本地debug签名安装到独立`.profile`包，非debuggable，仅该变体启用shell profiling和固定耗时标记。首次需自行登录加载数据，后续同Profile包覆盖安装保留其数据；Debug/Profile不共享会话；正式Release不启用这些标记。

```powershell
.\gradlew.bat :app:assembleProfile
& 'D:/Dev/AndroidSDK/platform-tools/adb.exe' -s 10AG4S2KQJ0066R install -r app/build/outputs/apk/profile/app-profile.apk
& 'D:/Dev/Python/python.exe' tools/performance/capture.py --adb D:/Dev/AndroidSDK/platform-tools/adb.exe --serial 10AG4S2KQJ0066R --samples 5 --duration 8 --label baseline
# analyze.py 参数为上一步输出目录和官方Trace Processor可执行文件路径：
$captureDirectory = 'work/performance/替换为上一步输出目录'
$traceProcessor = 'C:/Tools/Perfetto/trace_processor_shell.exe'
& 'D:/Dev/Python/python.exe' tools/performance/analyze.py $captureDirectory --processor $traceProcessor
```

手机需保持解锁/亮屏。手动滚动采集使用`--mode manual --samples 1 --duration 12`，脚本不点击或滑动。配置/APK哈希/采集与分析JSON/trace均留在不跟踪的work/performance/；不上传，不清应用数据或修改ART编译模式。官方工具下载和测量边界见[架构说明](docs/architecture.md)。

## 文档入口

| 文档 | 维护内容 |
| --- | --- |
| [架构与组件规范](docs/architecture.md) | 职责、同步/缓存、页面布局、组件标准、主题渲染与共享函数 |
| [API 接入进度](docs/api-progress.md) | 每个接口的接入状态、展示用途和未验证范围 |
| [协议证据与边界](docs/protocol.md) | 请求、响应结构、认证、历史观察与复用风险 |
| [验证与交付记录](docs/validation.md) | 最新检查、部署/验收状态、必要历史证据与手动检查清单 |
| [发布与验收](docs/release.md) | 签名备份、实际Release打包/验收、首次公开schema基线与发布分工 |
| [质量分析与实施核对](docs/quality-review.md) | 0.20.0审阅基线、逐项采用结果与暂缓理由 |

文档描述当前行为，不逐版重复实现过程。已完成计划、旧预览设计和详细版本流水账从 Git 历史查阅；每次交付简要报告提交情况，API 覆盖变化更新进度表。
