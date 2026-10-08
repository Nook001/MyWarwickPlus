# 开发与验证

本文提供构建、手机验证、性能采集及当前待办。日常开发遵守 [AGENTS.md](../AGENTS.md)；代码职责见 [架构](architecture.md)，签名打包见 [发布流程](release.md)。

## 环境与构建

Android Studio 打开仓库根目录，需要 JDK 21 和 Android SDK Platform 37.0。compile/target SDK 为 37，min SDK 为 28，JVM 目标为 17。依赖版本以 Gradle 文件为准，SDK 路径写入不跟踪的 `local.properties`。

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

明确指定物理手机，覆盖安装保留登录与缓存；出现 USB 安装确认需用户点击允许。不将模拟器安装或构建成功记为手机验收。功能阶段不编写、扩展或默认运行 UI Test；简单样式/功能不新增单元测试。既有 androidTest 保留但未维护为当前可运行入口，不计入默认检查。

## 最新公开版本验证

`v0.26.0-beta.1` 于 2026-10-08 [发布为 Pre-release](https://github.com/Nook001/MyWarwickPlus/releases/tag/v0.26.0-beta.1)，versionCode 39，APK 源码提交 `5494241`。后续文档提交不改变该 APK。

| 项目 | 已确认结果 |
| --- | --- |
| 构建 | 签名 Release、Debug、Profile 构建通过，Release R8/资源收缩通过 |
| JVM / Lint | 76 项既有 JVM 检查通过；Debug Lint 无问题，Release 0 errors/1 warning（可用 Gradle 补丁提示） |
| 包与签名 | Android 9+、非 debuggable、正式版性能标记关闭；签名和 SHA-256 校验通过，APK 内含四份隐私/许可文件 |
| 手机 | 指定物理手机覆盖安装与启动成功；实际安装 APK 哈希与发布 APK 相同，旧内部包保留 |
| 用户反馈 | 手动登录测试正常；不扩充为所有页面、离线、退出、更新或多设备全部验收 |

构建日志、mapping 和 trace 只在本地 `work/`、`dist/` 保存。先前跟踪文件扫描未发现高置信 token/private-key，不等于完整安全审计。历史 API 验证边界在 API 表，旧版本安装重试和逐次构建日志通过 Git 查询。

## 开发候选

0.26.0-beta.2 / versionCode 40：Inbox 本地来源筛选、Home 正方形入口、紧凑 Me 网格。源码提交 `fdb359d`；Debug/签名 Release 构建通过，76 项既有 JVM 检查通过，Debug Lint 无问题，Release 0 errors/1 warning（Gradle 补丁提示）。同签名 APK 与校验文件在 `dist/v0.26.0-beta.2/public/`，尚未发布 GitHub。已在指定物理手机覆盖安装并启动，版本核对通过；新 UI 手测反馈待用户确认。没有新增单元测试或 UI Test。

新增服务的浏览器只读证据及原生接入边界统一记录在 API 表，当前仍只同步六类业务数据、使用 schema 5。

## 手动检查

| 范围 | 检查点 |
| --- | --- |
| 会话与缓存 | SSO/MFA 返回、冷启动缓存、过期后恢复；退出/换账户只在愿意重新登录时测试 |
| 刷新 | 真实任务进度、重复下拉不并发、断网保留数据、联网恢复、内容附近单资源恢复 |
| Home / Classes | 英国时间、Today/Tomorrow/周末边界、截止日期、选日与位置、跨日/全日/冲突、详情和地点 |
| Tasks / Inbox | 搜索/清除/分类、准确期限、详情关闭、最新消息刷新、更早分页及失败恢复 |
| Me / 外观 | 邮箱显式复制、网格、五主题/纹理、大字体和窄屏、隐私/许可及外链返回 |
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
| 数据覆盖 | Buses/Print/Events 已确认登录态 GET 和结构，待独立服务页与按需缓存；非空 Library、Modules 公告/评估仍待结构证据 |
| 性能 | Messages 滚动、主题切换内存、可重复 Classes 对比；有热点证据后才考虑 Baseline Profile 或进一步时钟调度 |
| 存储与备份 | 真设备 Room 迁移验证、OEM 迁移是否遵守禁备份规则；JVM 假缓存不能替代 Room 引擎检查 |
| 发行维护 | 密钥/密码独立备份及学校允许范围仍待用户确认，操作见发布文档 |

已完成的质量报告处置、旧架构评分和语法示例不继续作为待办。当前保持整批解析、按资源账户验证、取消后等待 IO、展示值快照与有限背景缓存。
