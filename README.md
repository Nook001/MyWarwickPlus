# MyWarwick+

独立的 Android 学生客户端原型，Kotlin + Jetpack Compose。通过官方 WebView SSO 登录，复用 MyWarwick session 与只读聚合接口，不申请新的 OAuth application；不是 Warwick 官方应用。

当前代码：**0.24.2 / versionCode 35**。构建、物理安装和手动验收是不同状态，交付情况见 [验收记录](docs/validation.md)。

## 当前功能

| 页面 | 内容 |
| --- | --- |
| Home | 招呼语、Now / Next、四个网站入口、Today剩余课程/提前预览Tomorrow、周末祝语、最近三个未来截止日期、最近两条Messages摘要 |
| Classes | Schedule 连续日期列表、日期跳转、冲突/跨日提示、课程详情与地点链接 |
| Tasks | Coursework 搜索、Upcoming / Past、准确截止时间、详情与原站链接 |
| Me | 账户邮箱与显式复制、设置、按需数据状态、Messages / Modules / Library、八个网站入口、本地 Sign out |

五套固定颜色主题共用静态渐变背景和可选细纹理。启动先显示缓存；下拉刷新使用真实任务阶段进度，失败保留已存数据。日期按英国时区显示。当前没有后台同步、系统通知、提交作业或消息已读写回。

原型仅使用当前 Room schema5，不维护旧版数据库迁移或旧导航键适配。现有 schema5 可覆盖升级；schema1–4 不再支持直接升级。

## 开发与部署

Android Studio 打开本目录；需要 JDK 21、Android SDK Platform 37.0。compileSdk / targetSdk 37、minSdk 28；依赖版本以 Gradle 文件为准。SDK 路径保存在不跟踪的 local.properties。

本机命令：

```powershell
$env:JAVA_HOME = 'D:/Dev/JDK21.0.8'
.\gradlew.bat :app:assembleDebug
# 按改动范围运行必要的既有 JVM 检查；完整 JVM 入口：
.\gradlew.bat :app:testDebugUnitTest
& 'D:/Dev/AndroidSDK/platform-tools/adb.exe' -s 10AG4S2KQJ0066R install -r app/build/outputs/apk/debug/app-debug.apk
& 'D:/Dev/AndroidSDK/platform-tools/adb.exe' -s 10AG4S2KQJ0066R shell am start -n uk.ac.warwick.plus/.MainActivity
```

遵守 [AGENTS.md](AGENTS.md)：功能阶段未经用户显式要求不编写或扩展 UI Test；简单 UI 调整不新增单元测试。日常部署明确指定物理手机并覆盖安装，保留登录与缓存。既有 androidTest 不是默认交付检查，不在已登录手机运行认证清理或可能卸载应用的测试流程。

性能采集使用`:app:assembleProfile`：沿用Release的R8/资源收缩，以本地debug签名覆盖安装，非debuggable，仅该变体启用shell profiling和固定耗时标记。同包名保留登录/缓存，测完覆盖回Debug；正式Release不启用这些标记。

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
| [质量分析与实施核对](docs/quality-review.md) | 0.20.0审阅基线、逐项采用结果与暂缓理由 |

文档描述当前行为，不逐版重复实现过程。已完成计划、旧预览设计和详细版本流水账从 Git 历史查阅；每次交付简要报告提交情况，API 覆盖变化更新进度表。
