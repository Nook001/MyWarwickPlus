# 开发与验证

本文提供维护者的构建、手机验证、性能采集及当前待办。通用规则和 PR 规范见 [AGENTS.md](../AGENTS.md) 与 [CONTRIBUTING.md](../CONTRIBUTING.md)；代码职责见 [架构](architecture.md)，签名打包见 [发布流程](release.md)。

## 环境与构建

Android Studio 打开仓库根目录，需要 JDK 21 和 Android SDK Platform 37.0。compile/target SDK 为 37，min SDK 为 28，JVM 目标为 17。依赖版本以 Gradle 文件为准，SDK 路径写入不跟踪的 `local.properties`。2026-10-09 已升至 Gradle 9.8.1、Kotlin 2.4.21（根 `build.gradle.kts` 让 AGP 内置 Kotlin 使用与 Compose 编译器插件相同的版本），其余依赖已是当日最新稳定版。GitHub Actions 在每次推送 `master` 和 PR 时运行单元测试、Debug Lint、Debug 构建和 androidTest 编译，不含签名或设备测试。

| 变体 | applicationId | 用途 |
| --- | --- | --- |
| Release | `io.github.nook001.mywarwickplus` | 正式签名、R8/资源收缩、非 debuggable |
| Debug | `io.github.nook001.mywarwickplus.debug` | 日常开发，本地 debug 签名 |
| Profile | `io.github.nook001.mywarwickplus.profile` | R8/资源收缩、非 debuggable、本地 debug 签名、性能标记 |

各包会话与缓存独立，首次需分别登录；namespace/Activity 类名仍为 `uk.ac.warwick.plus`。旧内部包 `uk.ac.warwick.plus` 不迁移认证。公开 Room schema 5 是升级基线；未来 schema 变更必须提供迁移，不使用 destructive fallback。

```powershell
$env:JAVA_HOME = 'D:/Dev/JDK21.0.8'
.\gradlew.bat :app:assembleDebug :app:lintDebug
# 按改动范围选择既有 JVM 检查；完整入口：
.\gradlew.bat :app:testDebugUnitTest
& 'D:/Dev/AndroidSDK/platform-tools/adb.exe' -s 10AG4S2KQJ0066R install -r app/build/outputs/apk/debug/app-debug.apk
& 'D:/Dev/AndroidSDK/platform-tools/adb.exe' -s 10AG4S2KQJ0066R shell am start -n io.github.nook001.mywarwickplus.debug/uk.ac.warwick.plus.MainActivity
```

明确指定物理手机，覆盖安装保留登录与缓存；出现 USB 安装确认需用户点击允许。不将模拟器安装或构建成功记为手机验收。功能阶段不编写、扩展或默认运行 UI Test；简单样式/功能不新增单元测试。既有 androidTest 已修复为可编译（`:app:compileDebugAndroidTestKotlin`），但不计入默认检查；不要在已登录的手机上运行 connectedAndroidTest，它会卸载应用并清除登录。

## 最新公开版本验证

0.28.0-beta.1 / versionCode 43：

- 静默续期：`/user/info` 返回 refresh URL 时由不可见 WebView 完成 SSO 往返，手机模拟会话失效已验证。
- 更新检查：设置可关闭自动检查，Me → 应用 → 检查更新；Debug/Profile 只手动检查。语义化版本比较与 Release 响应校验有 JVM 检查。
- 提醒：设置 → 提醒，开启时请求通知权限；单一精确闹钟链按缓存重排，开机/升级/改时区后恢复，迟到的闹钟或缓存变化不会跳过已到期提醒。到期窗口规则有 JVM 检查。
- 桌面小组件：Next 卡片与当日时间轴，随缓存/主题更新并在课程边界、午夜刷新。
- 简体中文：跟随系统语言，Android 13+ 可在系统应用设置单独切换；日期用中文格式，时间仍为英国时间。

86 项 JVM 检查、Debug Lint 与 androidTest 编译通过，签名 Release 打包检查通过，证书指纹不变。物理手机覆盖安装 Debug 包由用户整体验收通过（未逐项记录边界场景）；签名包覆盖安装 0.27.1 后版本为 43。`v0.28.0-beta.1` 于 2026-10-09 [发布并设为 Latest](https://github.com/Nook001/MyWarwickPlus/releases/tag/v0.28.0-beta.1)，tag 对应 `4fb6047`，APK SHA-256 `1886b2ea…00cc49`，上传附件 digest 与本地一致。


更早版本的验证记录见 Git 历史和对应 [Release](https://github.com/Nook001/MyWarwickPlus/releases)。仍有效的结论：schema 5→6 迁移 SQL 已用 SQLite 核对，与 Room 导出的八张表一致并保留原有数据，覆盖前后的离线缓存仍需手测；签名目录曾从用户备份恢复，证书指纹自首个 Beta 起未变。

## 手动检查

| 范围 | 检查点 |
| --- | --- |
| 会话与缓存 | SSO/MFA 返回、冷启动缓存、过期后恢复；退出/换账户只在愿意重新登录时测试 |
| 刷新 | 真实任务进度、重复下拉不并发、断网保留数据、联网恢复、内容附近单资源恢复 |
| Home / Classes | 英国时间、Today/Tomorrow/周末边界、截止日期、选日与位置、跨日/全日/冲突、详情和地点 |
| Tasks / Inbox | 搜索/清除/分类、准确期限、详情关闭、最新消息刷新、更早分页及失败恢复 |
| Me / 外观 | 邮箱显式复制、网格、五主题/纹理、大字体和窄屏、隐私/许可及外链返回 |
| 提醒 / 小组件 | 通知权限拒绝提示、课前通知点开应用、关闭后不再提醒；小组件主题色、跨天标签、时间轴与空状态 |
| 语言 | 中英文切换后标签、日期、通知与小组件文字；长中文文本不截断关键字段 |
| Release 更新 | 同包名/签名覆盖后缓存和会话保留；同一 APK 重装不能证明未来数据库迁移 |

每次交付仅记录实际检查及用户反馈，不重复保存完整清单。发布前按改动范围补足手测，避免在已登录手机自动退出、清数据或改变时间。

## 性能采集

Profile 变体才启用固定 `MWP.*` 标记和 shell profiling。CookieManager 线程安全惰性初始化，原生首次访问在 IO，离线缓存读取无需加载 WebView provider。

```powershell
.\gradlew.bat :app:assembleProfile
& 'D:/Dev/AndroidSDK/platform-tools/adb.exe' -s 10AG4S2KQJ0066R install -r app/build/outputs/apk/profile/app-profile.apk
& 'D:/Dev/Python/python.exe' tools/performance/capture.py --adb D:/Dev/AndroidSDK/platform-tools/adb.exe --serial 10AG4S2KQJ0066R --samples 5 --duration 8 --label baseline
# 将目录与官方 Trace Processor 路径替换为实际值：
$captureDirectory = 'work/performance/采集输出目录'
$traceProcessor = 'C:/Tools/Perfetto/trace_processor_shell.exe'
& 'D:/Dev/Python/python.exe' tools/performance/analyze.py $captureDirectory --processor $traceProcessor
```

手动滚动使用 `--mode manual --samples 1 --duration 12`，由用户操作；脚本只录制。冷启动模式仅 force-stop/start 目标应用，拒绝模拟器/debuggable 测量。不清数据、不改变 ART 编译模式、不上传。配置、APK 哈希、原始 trace 和分析 JSON 保存在 `work/performance/`。分析拒绝丢包/解析错误；TTID 表示首帧，不代表缓存或网络全部就绪。

2026-10-07 在 vivo/API 36 上各 5 次 Profile 冷启动，提前初始化/IO 惰性初始化的 TTID 中位数为 192.210/174.983 ms，CookieManager 主线程样本从 5 降为 0。可靠结论是初始化离开主线程，小样本差值不外推全部设备；一次用户手动 Classes 滚动不是前后性能对比。单次 `am start` 时间不作为基准。

工具：[profileable](https://developer.android.com/guide/topics/manifest/profileable-element)、[Perfetto](https://perfetto.dev/docs/getting-started/system-tracing)、[官方 Trace Processor 下载](https://raw.githubusercontent.com/google/perfetto/main/tools/trace_processor)。Compose 稳定性报告可用 `:app:compileDebugKotlin -PcomposeReports=true --rerun-tasks` 生成至 `app/build/reports/compose/`，不等于实际重组次数或帧率。

## 尚有价值的待办

| 方向 | 开始条件或缺口 |
| --- | --- |
| 公开版验证 | 补离线/更新/退出、字体与主题、更多设备；API 37 运行仍待对应设备 |
| 数据覆盖 | Buses/Print/Events 首页摘要已手动验收；非空 Library、Modules 公告/评估仍待结构证据 |
| 性能 | Messages 滚动、主题切换内存、可重复 Classes 对比；有热点证据后才考虑 Baseline Profile 或进一步时钟调度 |
| 存储与备份 | 真设备 Room 迁移验证、OEM 迁移是否遵守禁备份规则；JVM 假缓存不能替代 Room 引擎检查 |
| 发行维护 | 密钥/密码独立备份及学校允许范围仍待用户确认，操作见发布文档 |

已完成的质量报告处置、旧架构评分和语法示例不继续作为待办。当前保持整批解析、按资源账户验证、取消后等待 IO、展示值快照与有限背景缓存。
