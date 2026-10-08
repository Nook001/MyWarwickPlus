# 验证与交付记录

更新：2026-10-08。保留当前交付状态与必要历史证据；旧版逐项日志、预览和失败修复过程从 Git 历史查阅。此文档只记录已发生的验证，不能用构建/安装替代UI手动验收，不能用虚构数据测试补充学校接口证据。

## 2026-10-07：架构与 Kotlin 写法评估（第一批）

以0.18.0代码静态核对同步/退出、缓存快照、页面状态/导航、展示计算、实体可变性与既有检查，评估及优先级收录到 [architecture.md](architecture.md) 末节。发现退出等待早于HTTP取消的路径；明确区分代码事实、潜在维护风险与未测量性能影响。评分不作为生产安全/性能认证，该评估提交仅记录待办；后续0.19.0进度见最新代码节。

仅更新三个现有文档，不改源码/测试/依赖/schema/版本；不构建、不运行JVM/UI/设备测试、不部署或请求学校接口。核对本地引用、表格/代码块结构、评估中的源码符号及git diff --check；查阅Compose/Kotlin官方技术说明，不访问用户会话。之前0.18.0手机安装被拒和待验收状态保留。

## 最新发行：0.26.0-beta.1 / versionCode 39

2026-10-08：首次公开候选准备。公开包io.github.nook001.mywarwickplus，Debug/Profile独立后缀与启动器名，原namespace不变；旧内部包数据不迁移、不清理。Release仓库外PKCS12签名，DPAPI本地加密凭据、发布构建禁用configuration cache；脚本检查签名身份/版本/非Debug/源码提交并生成分离的public附件/private记录。MIT已由用户确认，APK内生成assets/legal许可证/隐私文件，Me增加Privacy/Licenses链接；schema5为后续公开迁移基线。

| 检查 | 本次结果 / 边界 |
| --- | --- |
| 构建 | 签名Release、Debug、Profile与既有JVM入口通过（2m17s）；补齐APK法律文件后三个变体及两项Lint再通过（39s）。日志work/release-preparation.log、work/release-final-assets.log |
| Lint | Debug无问题；Release为0 errors / 1 warning，仅Gradle 9.8.1可用提示，保持当前9.8.0工具链；无UI Test或新增测试 |
| 既有JVM | 76项，0失败/错误/跳过；未写新断言或适配UI Test |
| APK | release为0.26.0-beta.1/code39/Android9+，apksigner校验通过，v2签名/RSA3072；非debuggable，PERFORMANCE_TRACING=false。Debug/Profile实际包名及后缀核对；APK含MIT/Apache/第三方声明/隐私四文件 |
| 私人数据 / 文档 | 已跟踪文件高置信token/private-key扫描无匹配，未跟踪APK/keystore/HAR/local.properties/work/dist；不是完整历史秘密审计。隐私/发布分工/脱敏反馈/签名备份与手机清单已补齐；未访问学校数据 |
| 用户反馈 / 未核实项 | 用户于2026-10-08反馈“手动登录测试了，没有问题”，并明确要求发布；不据此声明离线/升级/退出/所有页面及多设备全部验收。独立备份密钥/密码、学校第三方发行允许范围未在本会话核实 |
| 最终打包 / 原Draft | package.ps1实际成功，产物源码提交5494241；签名指纹与固定文件一致，生成APK/SHA256SUMS/release-manifest及分离的private记录。最初创建v0.26.0-beta.1 Draft并上传8附件；随后公开发布，当前状态见公开发布行 |
| 新包安装 | 首次USB安装被拒；用户解锁授权重试后，指定10AG4S2KQJ0066R执行install -r成功。dumpsys确认新包0.26.0-beta.1/code39，无DEBUGGABLE；启动Status:ok/COLD/168ms（单次辅助值，非基准）。只读pull实际安装APK，SHA-256与候选一致；旧uk.ac.warwick.plus仍0.25.1/code38，未卸载/清数据。用户后续反馈登录正常，其他场景仍以真实手测为准 |
| 公开发布 | 2026-10-08按用户明确要求将现有Draft发布为Pre-release：isDraft=false/isPrerelease=true，目标5494241，tag及八附件已核对，APK哈希不变；仅更新发布说明并补充真实登录反馈，不重建APK或冒称学校授权 |

最终打包产物、SHA256与源码提交以dist/v0.26.0-beta.1/public/release-manifest.json为准；不把本地旧unsigned或Debug APK当作发行包。签名脚本不自动安装/发布；操作方法见[release.md](release.md)。

公开链接：[GitHub Release](https://github.com/Nook001/MyWarwickPlus/releases/tag/v0.26.0-beta.1)。后续此记录的文档提交不改变APK所对应的5494241源码与附件校验值。

## 上一版：0.25.1 / versionCode 38

2026-10-08：首页Today/Deadlines采用Compact左列40sp（原48sp），随系统字体缩放，默认正文左移8dp。Inbox标题14sp Medium、摘要/搜索12sp、来源日期11sp Regular；移除顶部说明、来源与英文短日期同行，标题/摘要各最多两行，卡片内边距12dp/内容间距4dp/卡片间距8dp。消息详情标题18sp/正文14sp，完整文本/日期保留；Tab图标24→22dp，原24dp布局槽/点击区不变。Library/Modules与Classes/Tasks保留标准布局；无API/认证/权限/依赖/schema5变更。

| 检查 | 本次结果 / 边界 |
| --- | --- |
| 构建 / Lint | assembleDebug/lintDebug成功（21s），Lint报告No issues found；日志work/0251-build.log。无新增测试，未运行JVM/UI Test或Release构建 |
| 数据 / 文档 | 消息搜索/账户文本缓存/排序/详情/分页与网站已读行为不变；README、架构/API进度已同步 |
| 物理部署 | 指定10AG4S2KQJ0066R执行install -r成功，dumpsys确认0.25.1 / versionCode38；启动Status:ok / COLD / TotalTime472ms，单次Debug值不作性能基准。未卸载/清数据或安装到模拟器 |
| 视觉核对 | 只读截图work/inbox0251.png（不跟踪）：手机已切到Inbox，来源/日期同行、标题紧凑、五条消息完整及第六条部分可见，Tab轮廓/实心两态可见，无可见重叠；未自动点击/滑动。首页左列、详情/搜索/分页与大字体仍待手动验收 |

手动重点：首页时间与倒计时左列仍居中、课程/作业名更靠左；Inbox首屏可见消息量、来源日期对齐、长标题/摘要省略与详情全文、搜索/清除/下拉/更早分页；五Tab两态图标及大字体可读性。构建/安装不能替代UI手动验收。

## 上一版：0.25.0 / versionCode 37

2026-10-08：Today起始时间及Deadline数字改为11sp Bold，结束时间/days保持Regular；Next当日只显示时间，未来显示Tomorrow或日期。Messages独立为Inbox Tab（页标题Messages），Home摘要保留、All到Inbox；Me删除Data status/Developer tools/重复Messages入口，Sign out到版本号后的页面末尾，保留确认与退出失败重试。无API/权限/认证/依赖/Room schema5变更。

| 检查 | 本次结果 / 边界 |
| --- | --- |
| 构建 / Lint | 首轮assembleDebug/lintDebug成功；删除面板后清理8条UnusedResources文案，最终assembleDebug/lintDebug成功（19s），Lint报告No issues found；日志work/025-build.log、work/025-final-build.log。未运行Release构建 |
| 既有导航检查 | NavigationStateTest三项通过（0失败/错误），仅适配Messages当前归属，不新增测试或UI Test；未运行全量JVM检查 |
| 数据 / 文档 | API/账户隔离/认证/权限/依赖/schema5不变；单资源刷新与消息分页沿用原实现，架构/API进度已同步 |
| 物理部署 | 指定10AG4S2KQJ0066R执行install -r成功，dumpsys确认0.25.0 / versionCode37；启动Status:ok / COLD / TotalTime413ms，单次Debug值不作性能基准。未卸载/清数据或安装到模拟器 |
| 视觉核对 | 只读首页截图work/home025.png（不跟踪）：Next当日仅时间、Today起始时间/Deadline数字加粗、days常规字重；Home/Classes/Tasks/Inbox/Me五标签完整，无可见重叠。未自动点击/滑动或退出账户 |

手动重点：Inbox搜索/详情关闭仍在Inbox/All跳转、下拉只更新Messages/更早分页；Me无状态/调试按钮，页面末尾Sign out弹窗可取消；Today起始时间和数字突出但days不变。不同主题/字体缩放、Tomorrow/未来日期及上述交互仍需用户手动验收。

## 上一版：0.24.3 / versionCode 36

2026-10-08：首页课程/作业/消息标题和空状态14→12sp，与分区标题同字号；Today起止时间与Deadline数字/days统一11sp，保留数字Medium、次要值Regular。HomeTypography集中此规则，沿用Material字体族与系统字体缩放；Next课程名仍16sp，Classes/Tasks及现有间距、卡片颜色、数据逻辑不变。

assembleDebug、lintDebug成功（26s，日志work/0243-build.log），Lint报告No issues found；未新增测试，未运行JVM/UI Test或Release构建。origin为https://github.com/Nook001/MyWarwickPlus.git，源码提交c075cc4已推送master并核对远程HEAD一致。

2026-10-08：按用户要求部署到物理手机。首次USB安装被拒（INSTALL_FAILED_ABORTED / User rejected permissions）；用户回复重试后指定10AG4S2KQJ0066R执行install -r成功，dumpsys确认0.24.3 / versionCode36。启动Status:ok / COLD / TotalTime570ms，单次Debug值不作性能基准；未卸载/清数据或安装到模拟器。未进行新版真机截图或手动验收；手动重点：时间/倒计时可读性、长标题省略减少、字体缩放与详情/All/刷新，安装与启动不能替代手动验收。

## 上一版：0.24.2 / versionCode 35

2026-10-08：首页减少同时强调的文本，普通课程/Deadline/消息标题14sp Regular、18sp行高，Next课程名16sp Medium、22sp行高；分区标题/时间/倒计时Medium。分区间距24→14dp；复用MetricListRow的显式Compact变体，后续行64→54dp/上下6dp，首行保留48dp/上0/下8dp。消息行对应收紧；网站入口64→48dp、Quiet底色、常规标签。Today移除重复Next，仍保留Now、地点、冲突和跨日提示；Classes/Tasks默认行布局与字重不变。

| 检查 | 本次结果 / 边界 |
| --- | --- |
| 构建 / Lint | assembleDebug、lintDebug成功，Lint报告No issues found；日志work/0242-build.log。不新增测试，未运行JVM/UI Test或Release构建 |
| 数据 / 文档 | API/认证/依赖/Room schema5与数据排序、筛选、刷新规则不变；架构及API进度已更新 |
| 物理部署 | 首次被手机拒绝：INSTALL_FAILED_ABORTED / User rejected permissions；用户回复“重试”后指定10AG4S2KQJ0066R执行install -r成功。dumpsys确认0.24.2 / versionCode35；启动Status:ok / COLD / TotalTime505ms，单次Debug值不作性能基准。未卸载/清数据或安装到模拟器 |
| 视觉核对 | 只读首页截图work/home0242.png（不跟踪）：当前Forest主题的快捷入口文字完整，课程/作业标题使用常规字重、没有重叠；首屏可见完整第一条消息及第二条的一部分。完整五主题/大字体、点击和刷新仍待用户手动验收；未自动点击/滑动 |

手动重点：对比首屏Messages露出与分区留白；Next突出但普通行不抢眼；长作业名可读性、长课程名/大字体自然换行；四个网站入口、详情/All与下拉刷新正常。保留系统字体缩放，不以固定高度裁切。

## 上一版：0.24.1 / versionCode 34

2026-10-07：修复首页分区标题上紧下松的问题。标题统一左右12/上8/下2dp与最小20dp内行；All采用相同内行高度。首条课程/Deadline/消息行最小48dp、上0，空状态行上0，减少首行最小高度居中与标题留白叠加；后续行和Next布局不变。

| 检查 | 本次结果 / 边界 |
| --- | --- |
| 构建 / Lint | assembleDebug与lintDebug成功，Lint无问题；仅间距和版本调整，未新增测试，未运行JVM/UI Test或Release构建 |
| 数据 / 文档 | API、认证、依赖、schema5、Today/Tomorrow/周末与Messages逻辑不变；组件间距规范及API进度同步更新 |
| 物理部署 | 指定10AG4S2KQJ0066R执行install -r成功，dumpsys确认0.24.1 / versionCode34。冷启动Status:ok / COLD / TotalTime470ms，单次Debug值不作性能基准；未卸载/清数据 |
| 视觉核对 | 只读首页截图核对：Tomorrow与Deadlines标题顶部留白增加、首行间距收紧，Messages标题采用同一标准；截图仅保留在不跟踪的work/home0241.png。五主题/大字体与点击交互仍待用户手动反馈，未自动点击/滑动 |

命令：`:app:assembleDebug :app:lintDebug`，JDK21；日志在不跟踪的work/0241-build.log。手动重点：Today/Tomorrow、Deadlines、Messages标题距卡片顶部及首行的留白更均衡；All不再撑高标题；长文本/大字体自然扩高，点击详情与All仍正常。

## 上一版：0.24.0 / versionCode 33

2026-10-07：首页课程分区改为Today剩余课程/Tomorrow预览与周末祝语；分区标题增加统一图标，Home/Classes/课程详情共用地点图标；Deadlines下新增最近两条Messages摘要，点开详情/关闭保留Home，All进入完整Messages。使用已有缓存与只读协议，无新增API、权限、依赖或schema改动。

| 检查 | 本次结果 / 边界 |
| --- | --- |
| 构建 | assembleDebug / assembleRelease成功；Release R8/资源收缩通过 |
| JVM | 76项全部通过；仅新增2项日程时间逻辑检查，覆盖精确结束边界、空今日与完成今日共用Tomorrow、跨日/英国夏令时、周末及零时长全日项。既有导航检查只适配新增当前Tab参数 |
| Lint / 静态 | lintDebug无问题，schema5无diff；稳定分区key、消息只转换两条可见HTML、账户隔离文本缓存与详情ID校验保留 |
| 物理部署 | 首次被拒，用户授权“重试”后指定10AG4S2KQJ0066R执行install -r成功。dumpsys确认0.24.0 / versionCode33 / DEBUGGABLE；冷启动Status:ok / COLD / TotalTime488ms，单次Debug值不作性能基准。未卸载/清数据，登录缓存交互待用户手动核对 |
| 视觉核对 | 只读截取当前主题首页：晚间已显示Tomorrow · 8 Oct，Now/Next、日程、Deadlines标题图标及地点定位针可见，课程代码/地点同行，Messages标题位于Deadlines下方。截图仅在不跟踪的work/home024.png，不写入Git；消息正文仍需用户滚动核对 |
| 未执行 | 未新增/执行UI Test；五主题/大字体、消息详情返回/All、进行中课程与周末祝语待手机手动验收；未改手机时间、自动点击/滑动或操作账户 |

命令：`:app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleRelease`，JDK21；日志在不跟踪的work/024-build.log。无数据的Tomorrow/消息摘要不显示空卡片；Library/公交数据发现仍待后续，未虚构摘要。

手动重点：标题图标和地点图标未撑高标题/挤压代码；Today只剩进行中/未来课、最后一节结束后可见Tomorrow（无记录时省略）；周末无剩余课祝语、Next仍保留下一次课；Messages最多两条、详情关闭仍在Home、All到完整列表；下拉刷新和原有登录缓存正常。当前时间无法触发的切换由聚焦JVM覆盖，不改系统时钟验证。

## 上一版：0.23.0 / versionCode 32

2026-10-07：继续17–18/A7，加入非debuggable、R8/资源收缩的profile变体与固定耗时标记、本地Perfetto采集/分析脚本。基于实际追踪将CookieManager从Application主线程初始化改为AuthSession首次需要时在IO初始化；学校只读协议、Cookie来源与schema5不变。

| 检查 | 本次结果 / 边界 |
| --- | --- |
| 构建 / JVM | 最终assembleProfile / assembleDebug / assembleRelease成功；profile与Release R8/资源收缩通过，既有74项JVM全部通过；未新增测试 |
| Lint / schema | lintDebug 0 errors / 0 warnings；schema5无diff，identity hash不变；Debug/Release PERFORMANCE_TRACING=false，profile=true且非debuggable/profileable |
| 采集入口 | 两个Python脚本仅标准库，语法核对并实际采集/分析；明确物理序列号，拒绝模拟器/debuggable，记录配置/版本/APK SHA-256与trace。官方Windows Trace Processor v58.2，SHA-256 adfa6bad3d72be3ba9b83fa2b17b69fa13b3ab1cad0f42e52b86188bd5f0f997 |
| 有效启动追踪 | vivo V2502A / API36；同profile类型、64MiB/8秒配置、各5次进程冷启动；未更改ART编译模式/网络/屏幕设置或清登录缓存。本次buffer丢包/解析错误均为0，服务跨会话累计计数单独保留 |
| 手动Classes | 用户明确“准备好了”，随后提示开Classes/来回滚动并录制12秒；非自动UI操作。1140个应用帧，App Deadline Missed 1、Buffer Stuffing 3、Dropped Frame 1、Prediction Error 298、None 837；平均frame slice时长3.703ms，不作为显示帧间隔或FPS |
| 物理部署 | 基线与候选profile均指定10AG4S2KQJ0066R执行install -r成功。恢复日常Debug前两次被手机拒绝，用户再次授权“重试”后覆盖安装成功；dumpsys确认0.23.0 / versionCode32 / DEBUGGABLE。冷启动Status:ok / COLD / TotalTime445ms，单次Debug值不作性能基准；未卸载/清数据，登录与缓存交互待用户手动核对 |
| 未执行 | UI Test、自动点击/滑动/截图/退出、API37运行验证、Messages滚动、主题切换内存、Baseline Profile生成或Macrobenchmark |

启动结果来自Perfetto的首帧TTID（ms）；am值只作采集辅助，不混作发布基准：

| 5次有效样本 | 提前初始化基线 | IO惰性初始化候选 |
| --- | --- | --- |
| TTID中位数 / 范围 | 192.210 / 174.474–229.781 | 174.983 / 150.963–185.851 |
| CookieManager首次初始化中位数 / 主线程样本 | 19.850 / 5 | 34.978 / 0 |
| 首次事务缓存快照中位数 / 主线程样本 | 8.307 / 0 | 7.719 / 0 |
| 首次背景生成中位数 / 主线程样本 | 25.663 / 0 | 19.567 / 0 |

初始化工作没有消失；可靠结论是CookieManager离开启动主线程。TTID观测差约17ms，小样本并未控制联网刷新、系统负载、页缓存与ART编译状态，不外推全部设备或所有场景。启动帧的Prediction Error标记在候选较多，手动滚动亦存在；与App Deadline Missed分列，不把所有标记合并解释为应用CPU卡顿，也不据此宣称滚动改善。每组各一条trace的进程名仍为pre-initialized，分析以固定MWP标记定位upid、按相同首帧终点算法补TTID，JSON标记了来源。

有效产物位于不跟踪的work/performance/：eager-baseline64-20261007T173819Z-c53b82b1、lazy-candidate-20261007T174357Z-c8601f18、classes-scroll-20261007T174908Z-72107c50。最初32MiB试采出现buffer丢弃，未用于结果；扩大缓冲后重采。原始trace只在本机保存，不上传或提交。

验证命令：`:app:assembleProfile :app:assembleDebug :app:testDebugUnitTest :app:lintDebug`，再单独`:app:assembleRelease`，JDK21。采集/分析用法见[README](../README.md)，手动复核：已有登录与缓存、联网下拉更新、重新进入Classes/Tasks；重新登录仅在用户愿意时测试。

## 上一版：0.22.1 / versionCode 31

2026-10-07：用户授权覆盖安装后清理旧版本兼容代码。删除数据库1→5迁移链/注册与schema1–4导出文件，导航仅接受当前Tab/Filter存储键；保留当前schema5、正常状态校验和学校API实际日期格式处理。Feed圆角token从legacyContent更名为feedContent，数值/布局不变。未增加破坏性数据库重建、卸载或清数据流程。

| 检查 | 本次结果 / 边界 |
| --- | --- |
| 构建 / JVM | assembleDebug / assembleRelease成功，Release R8/资源收缩通过；既有74项JVM全部通过，导航断言适配当前键，没有新增测试 |
| Lint | 0 errors / 0 warnings；移除历史迁移检查后再次lintDebug通过 |
| Room | schema5无diff、identity hash仍为a201057e1b8a41788b3c7026be00cfee；现有schema5可覆盖升级，schema1–4不再支持直接升级；未新增真实SQLite自动检查 |
| 历史检查清理 | 仅删除CacheAndAuthTest两项、FeedsCacheTest一项退役迁移检查；未新增/扩展或运行UI Test，其他旧设备检查的过时签名/非空假设未处理，不将其编译或运行计为通过 |
| 物理部署 | 指定10AG4S2KQJ0066R执行install -r成功；dumpsys确认versionCode31 / versionName0.22.1 / targetSdk37。冷启动Status:ok / COLD / TotalTime514ms，单次值不作性能基准；未卸载/清数据，登录与缓存内容仍需用户手动核对 |
| 未执行 | UI Test、模拟器、自动点击/截图/退出/清cookie、学校接口抓取、API37运行验证及性能测量 |

命令：`./gradlew.bat :app:assembleDebug :app:assembleRelease :app:testDebugUnitTest :app:lintDebug --console=plain`，JDK21；仅删除退役检查后补跑`:app:lintDebug`。Debug APK导出为MyWarwickPlus-0.22.1-debug.apk。

手动重点：已有登录/主题与缓存仍可用；下拉后数据更新、进度结束；Classes日期跳转及Tasks筛选切换正常。无需清理数据。

## 上一版：0.22.0 / versionCode 30

2026-10-07：19–21 改造完成，具体结构与版本见 [architecture.md](architecture.md)。Kotlin DAO/实体通过 KSP 生成 Room 实现；同步只保存数据，事务快照 Flow 发布内容，ViewModel 保留临时错误和进度。显示名称、常用文案及数量复数进入资源；构建链与依赖分批升级，compile/targetSdk37。保留物理手机缓存所需的 schema5 和迁移，不保留未发布的旧内部调用兼容层。

| 检查 | 本次结果 / 边界 |
| --- | --- |
| 构建 | assembleDebug / assembleRelease 成功；Release 的 R8 与资源收缩通过，产物未签名、未发布 |
| JVM | 74项全部通过；只新增1项缓存流更新/旧快照拒绝/退出状态检查，其余适配类型化文案与 Flow 命令接口；既有 HTTP 阻塞取消及取消注册竞争检查通过 |
| Lint | 0 errors / 0 warnings，报告为 No issues found |
| 配置缓存 | 相同四任务重复构建成功，Configuration cache entry reused；103任务中101项 up-to-date。外部插件仍有 Gradle 11 将移除的 setVisible 警告，不推断未来版本兼容 |
| Room / API | schema5无diff，identity hash a201057e1b8a41788b3c7026be00cfee 不变；核对生成 Flow 观察全部5表及事务实现，未新增真实SQLite或学校接口检查 |
| UI约定 | AndroidTest源码未改写/扩展/运行；旧接口与非空假设已过时，本轮不将其编译或运行计为通过，不为旧测试恢复兼容层 |
| 物理部署 | 首次安装被拒，用户授权重试后指定10AG4S2KQJ0066R执行install -r成功；dumpsys确认versionCode30 / versionName0.22.0 / targetSdk37，冷启动Status:ok / COLD / TotalTime533ms。单次值不作性能基准；未卸载或清除数据，交互待手动反馈 |
| 未执行 | UI Test、模拟器、自动点击/截图/退出/清cookie、Android17/API37运行验证、帧时间与实际重组性能测量 |

命令：`./gradlew.bat :app:assembleDebug :app:assembleRelease :app:testDebugUnitTest :app:lintDebug --console=plain`，JDK21。Debug APK 已导出为 MyWarwickPlus-0.22.0-debug.apk；构建、JVM检查和安装不能代替手动验收。

手动重点：已有缓存冷启动可见；下拉刷新后内容更新且进度结束；断网保留数据、联网恢复；Classes/Tasks/Me 的名称、数量文案与错误提示正常。真实账户切换与Sign out仅在用户愿意重新登录时测试。物理测试机为API36，target37的运行期行为仍需相应设备复核。

## 上一版：0.21.0 / versionCode 29

2026-10-07：按质量报告修复 P0 风险并落地维护性与必要细节项，取舍表见 [quality-review.md](quality-review.md)。未改变 API 路径、写操作边界、schema5 或调色值；有活动网络时仍最多额外两次 2s/5s 重试。

| 检查 | 本次结果 / 边界 |
| --- | --- |
| Debug 构建 | assembleDebug 成功 |
| JVM | 73项全部通过；原有68项 + 5项 null/异型字段、登录排队/退出、无网络/会话归属、导航存档转换、快照复用/换账户检查；旧网络失败继续刷新的断言改为新规格，独立HTTP 503恢复仍验证 |
| Lint | 0 errors / 6 warnings；剩余均为SDK/依赖/Gradle版本提示，不混入本轮升级 |
| Room / API | 导出schema5无diff，identity hash a201057e1b8a41788b3c7026be00cfee 不变；未新增真实SQLite或学校接口检查 |
| UI约定 | 既有 AndroidTest 只编译核对，未改写/扩展/执行；兼容入口集中隔离，旧运行期文案断言不记为通过 |
| 物理部署 | 指定10AG4S2KQJ0066R执行install -r成功；dumpsys确认versionCode29 / versionName0.21.0。冷启动Status:ok / COLD / TotalTime380ms；单次值不作性能基准，未卸载或清除登录缓存，交互待用户手动反馈 |
| 未执行 | UI Test、模拟器、自动点击/截图/退出/清cookie、OEM迁移、500条真实Messages性能或真机帧时间测量 |

命令：`./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin`，JDK21。

手动重点：断网下拉后尽快结束且保留数据；恢复网络可再次刷新；同步中完成登录后自动刷新；Classes 日期弹窗旋转保留有效选日/月份和模式；Me 子页返回、Messages 搜索/打开详情、详情关闭动画；五套背景和原首页布局一致。同步中重新登录只在用户愿意时测试，不自动退出账户。

## 上一版：0.20.0 / versionCode 28

2026-10-07：类型化Tab/筛选/Me子页/详情、同步操作与恢复动作；集中页面命名，Classes/Tasks顶部与Tab一致，Settings/Developer tools入口标题一致。API、schema5、依赖、布局和重试预算不变。

| 检查 | 本次结果 / 边界 |
| --- | --- |
| Debug 构建 | assembleDebug成功 |
| JVM | 68项全部通过；原有64项 + 4项导航存档/缓存等待/归属校验与延迟恢复/会话守卫/旧游标回退检查 |
| Lint | 0 errors / 17 warnings |
| Room / API | schema5无diff、数据库固定Feed key不变；未新增接口抓取或真实SQLite检查 |
| UI约定 | 既有AndroidTest仅编译核对，未改写/扩展/执行；其中旧标题断言未更新，不能记为运行通过 |
| 物理部署 | 指定10AG4S2KQJ0066R执行install -r成功；dumpsys确认versionCode28 / versionName0.20.0，冷启动Status:ok / COLD / TotalTime379ms。单次启动值不作性能基准；未卸载或清登录缓存，交互待用户手动反馈 |
| 未执行 | UI Test、模拟器、自动点击/截图/退出或清cookie、真机性能测量 |

命令：`./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin`。物理启动成功不等于手动验收通过。

手动重点：Classes/Tasks顶部名称；Me→Settings/Messages/Library/Modules/Developer tools返回；Home→Tasks分类、Classes选日与滚动位置；详情关闭及旋转/后台返回；有失败时Retry仍恢复正确资源。跨账户只在用户愿意重新登录时测试。

## 上一版：0.19.0 / versionCode 27

2026-10-07：代码提交fc5cc01，A1/A2/A3实施，随同前台时钟和少量语法清理；页面布局、依赖、API/认证协议与schema5保持不变。

| 检查 | 本次结果 / 边界 |
| --- | --- |
| Debug 构建 | assembleDebug 成功 |
| JVM | 全部64项通过：原有61项 + 3项取消/快照边界检查；本地HTTP阻塞读取消、注册前取消及后续请求隔离、快照不受原对象修改影响且相同内容复用 |
| Lint | 0 errors / 17 warnings；完整报告位于app/build/reports/lint-results-debug.html |
| Room | 导出schema5无差异；生成identity hash仍为a201057e1b8a41788b3c7026be00cfee；未改变列/迁移，未新增真实SQLite验证 |
| 兼容入口 | 既有AndroidTest源码只编译核对，未改写/扩展/执行；未运行instrumentation |
| 编译器报告 | 最终源码全量编译核对：HomeContent、ScheduleContent、CourseworkContent 均 restartable / skippable；不作为帧率或实际重组计数 |
| 物理部署 | 两次指定10AG4S2KQJ0066R执行install -r被手机拒绝；再次经用户授权“重试”后成功。dumpsys确认versionCode27 / versionName0.19.0；am start -W -S返回Status:ok / COLD / TotalTime553ms，单次启动值不作为性能基准。覆盖安装未卸载/清除登录缓存，界面与真实SQLite手动验收待反馈 |
| 未执行 | UI Test、模拟器、自动点击/截图、自动退出或清真实cookie、登录态浏览器API抓取、真机帧时间基线 |

命令：`./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin -PcomposeReports=true`；完整Compose报告另用 `:app:compileDebugKotlin -PcomposeReports=true --rerun-tasks`，避免增量报告只涵盖改动文件。JDK21沿用下面配置。

用户在本轮开始前反馈“重新登录已经测试了”；记录为重新登录手动反馈，不扩充为阻塞退出计时或全页面验收。刷新/搜索/后台返回、真实SQLite和帧时间的其他手动结论尚未追加。

## 上一版：0.18.0 / versionCode 26

代码提交 c8a2724，第四批同步/缓存整理完成。执行命令：

```powershell
$env:JAVA_HOME = 'D:/Dev/JDK21.0.8'
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest --tests uk.ac.warwick.plus.RepositorySyncTest --tests uk.ac.warwick.plus.TimetableStateTest --tests uk.ac.warwick.plus.FeedStateTest --tests uk.ac.warwick.plus.SyncRetryTest
```

| 检查 | 已记录结果 / 边界 |
| --- | --- |
| Debug 构建 | assembleDebug 成功 |
| 聚焦 JVM | 23项全部通过：TimetableStateTest6、FeedStateTest5、SyncRetryTest10、RepositorySyncTest2 |
| 新增范围 | 仅两项复杂Repository编排：换账户清快照且请求前拒旧游标；取消阻塞下载后不写缓存，本地退出仍可完成 |
| Room快照 | 生成的TimetableDao_Impl.snapshot事务包装已核对；内存DAO fixture不验证真实SQLite |
| 物理部署 | 指定10AG4S2KQJ0066R，首次install -r返回INSTALL_FAILED_ABORTED / User rejected permissions；**0.18.0尚未装上**，待解锁重试，未卸载或清除数据 |
| 未执行 | UI Test / Android instrumentation、模拟器、Lint、登录态浏览器API抓取、手机截图与自动UI操作 |
| 待验收 | 真实SQLite快照、0.18.0冷启动与刷新、用户手动交互 |

0.18.0未改变API/认证源码、schema5或页面布局。状态/重试/账户隔离规则见 [architecture.md](architecture.md)。

## 历史关键证据

以下均为原交付记录的摘要，本次未重跑：

| 阶段 | 验证 / 部署 / 反馈 |
| --- | --- |
| POC / 0.2.0（2026-10-03/04） | 修复区域时区后，用户确认“这次解析成功了”；原生课表143条，冷启动/缓存正常，Coursework3条、Library0条只读检查成功。扩展smoke最后遇连接中断，未记通过；后台会话未验证 |
| 0.3.0 / 0.4.0 | Coursework及Feed缓存/迁移、解析和业务状态检查通过；0.4.0浏览器Messages69/Modules1/Library0。覆盖安装后用户“测试都正常”；不补全Library非空结构 |
| 0.5.0–0.7.0 | 当时完成构建/JVM/Lint和虚构数据模拟器布局、主题、日期交互检查；0.6.0偏好重建、固定主题/背景缓存通过，75组纯色配色及32px背景抽样达到4.5:1，非所有像素/尺寸穷尽检查。0.7.0连续课表28项设备检查及后续4项课表复核通过。均为旧版证据，不证明当前所有UI通过 |
| 0.8.0 | 构建、56项JVM、Lint通过（0 errors / 17 warnings）。初轮UI32项中30项通过，修正后模拟器断开，最终未复跑，不能记为32项全过；随后按用户约定停止日常UI测试/模拟器流程 |
| 0.8.1–0.14.1 | 密度、导航、首页分层等迭代构建与必要既有检查通过；物理手机覆盖安装/启动成功。没有以安装成功替代视觉验收；简单样式调整未新增测试 |
| 0.15.0 / schema5 | 构建与31项聚焦JVM通过；虚构SQLite迁移检查1/2/3/4→5列定义及旧行保留，非完整设备Room测试。物理升级/冷启动529ms，schema5、账户id=6状态/非空邮箱各1条、课表186条；只查数量/标志，不输出私人值，不自动复制 |
| 0.16.0 | 提交13d1448；构建与41项既有JVM通过，物理覆盖安装/冷启动543ms。用户随后明确“验收通过了”，组件迁移手动验收通过；不补充API结构 |
| 0.17.0 | 提交e4b8f2d；构建与56项聚焦既有JVM通过。未新增测试，既有断言适配共享日期逻辑、移除退役周视图检查。首次安装被拒，授权重试成功；versionCode25，冷启动394ms，保留登录/缓存。页面/跳转待手动反馈 |

历史API验证的字段、时间与语义见 [protocol.md](protocol.md)；帧时间、GPU总内存、背景生成耗时、后台长期会话及Library非空数据仍无验收结论。旧截图/日志在不跟踪的work/仅作辅助，不作为可重复的当前自动化入口。

## 手动复核清单

| 范围 | 检查点 |
| --- | --- |
| 数据恢复 | 冷启动已有内容可读；下拉进度真实、多次下拉不并发；断网失败保留课表/作业/邮箱，联网后恢复；Data status单资源更新 |
| Home / Classes | Next日期靠右/地点行末箭头与整卡详情；Today/Deadlines左列；日期跳转/取消/Today、切页后位置、全日/跨日/冲突 |
| Tasks / Feed | Upcoming/Past、搜索/清除、英文月份/精确时间；消息更早分页/失败重试与纯文本详情 |
| Me / 外观 / 链接 | 真实邮箱与显式复制、网格、大字体、五个主题/纹理持久化；首页/Me及详情来源点击后才跳浏览器，失败提示可见、返回正常 |
| 会话 | 登录失效保留缓存并提供恢复；换账户/真正Sign out仅在用户愿意重新登录时测试，不自动清理真实会话 |

## 验证纪律与本轮文档清理

按 [AGENTS.md](../AGENTS.md) 控制测试范围。物理手机为10AG4S2KQJ0066R，日常使用adb install -r保留数据；不把模拟器安装记为手机部署，不在登录手机运行会清cookie/卸载应用的测试。旧androidTest保留，未经显式要求不作为日常UI验证流程。

本轮仅改README/docs并移除已完成的一次性重构脚本；应用代码、依赖、版本与设备状态未改动。不重跑构建/测试、不部署或抓取学校接口；核对本地文档引用、Markdown结构、API覆盖及源码关键事实，执行git diff --check。已完成的计划/备选草案从工作树移除，详细历史仍可通过Git恢复。
