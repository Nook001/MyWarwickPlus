# 架构与组件规范

更新：2026-10-08，适用代码 0.24.2。前文记录已落地的组件、共享函数、同步、数据反馈、课表和主题标准；末节保留评估基线并标注实施进度。接口证据见 [protocol.md](protocol.md)，覆盖表见 [api-progress.md](api-progress.md)，验证状态见 [validation.md](validation.md)。

## 代码职责

单个 app module，Kotlin 业务/Compose UI，Room DAO/实体使用 Kotlin + KSP 2.3.12，无 kapt；数据库类保留原身份与 schema。源码根目录为 `app/src/main/java/uk/ac/warwick/plus/`。

| 来源 | 职责 |
| --- | --- |
| auth/ | 官方 WebView SSO、CookieManager、限定 MyWarwick origin 的 CookieJar |
| data/MyWarwickApi、各 Parser | 只读 GET、响应大小/结构校验、整批解析，不负责 UI |
| data/TimetableRepository、StudentCache、TimetableDao、TimetableDatabase | 仓库只依赖原子缓存接口；账户隔离、串行读写/退出、事务快照；SyncSlots 集中持久 ID |
| ui/TimetableViewModel | 单份同步任务、完整/单项/分页执行、缓存 Flow 观察、阶段进度及通知 |
| ui/TimetableState、SyncRetry | 状态类型与 withCache/withIssue/resourceFailed；独立重试策略 |
| config/AppLabels | 页面、分类、区段及动作的资源 ID；文字在 strings.xml，不作为存储/接口key |
| ui/NavigationState、SyncOperation | 类型化Tab/筛选/Me子页/详情、轻量Saver、同步操作和当前状态恢复动作 |
| ui/PlusNavigator、PlusActions、PlusScreen | 导航/选日与轻量 Saver、动作集合；根页面连接投影与当前状态恢复 |
| ui/*Screen、业务详情 | 组合页面、业务展示及按需详情；不直接读写数据库 |
| ui/components/ | 基础容器、列表、操作、输入、标题及详情骨架；package 与目录一致 |
| ui/UiText、res/values/strings.xml | 延迟解析的消息/进度描述、格式化参数、数量与静态文案 |

组件只接受展示内容与动作，不持有 ViewModel、不发网络请求、不修改账户。暂不引入多模块、资源插件或并发同步；抽象由实际复用场景推动。

## 0.21.0 质量收敛

- `JsonFields` 共用 envelope/对象数组/唯一 ID 校验和实际类型读取；可选字符串 null/异型值视为空，必需字段拒绝整批，Modules 保持已确认的数字 ID 支持。
- 全局会话/缓存/退出问题使用 globalMessage；message 只代表课表问题，其他资源保留独立问题。成功的课表不会因 Tasks 登录失效被改成失败；未执行资源的登录提示一致。
- 每资源仍验证账户。账户未变只回调身份与进度；发现归属变化，先原子清理并立即投影空快照。正常内容由 Room Flow 驱动，不保留同步后手工全量重读或失败恢复路径。
- Feed 文本使用 Default 线程和按账户创建的容量缓存，切换账户/退出后旧缓存不再被根页面持有；内容刷新按源列表校验，避免同 ID 显示旧正文。转换完成前不显示原始 HTML，搜索结果会随正文转换完成更新。
- 时钟仍只在 STARTED 运行，延迟对齐 :00/:30；冲突集合共用；详情关闭按钮先 hide 后移除，账号切换仍立即清理详情。日期选择保存有效日期、月份与模式，保留 Locale.UK；未完成/无效输入不承诺保存。
- Android 12+ 明确排除云备份与 D2D 全部存储域，低版本保留 allowBackup=false/fullBackupContent=false；未实测 OEM 迁移。

采用项和未采用理由按原报告编号记录在 [quality-review.md](quality-review.md)。不引入多模块、全量状态表、格式工具或新 UI Test；19–21 的升级见下节；尚未进行帧时间/启动成本测量。

## 0.22.0：19–21 改造

- 构建：AGP 9.4.1、Gradle 9.8.0（官方 SHA-256）、内置 Kotlin/Compose compiler 2.2.10、KSP 2.3.12；configuration cache 默认启用。Room 2.8.5、Browser 1.10.0、OkHttp 5.5.0、协程运行/测试 1.11.0；compile/target SDK 37，min SDK 28/JVM 17。
- StudentCache 增加变更观察；DAO RawQuery 显式跟踪 events/coursework/sync_state/feed_entries/feed_meta，信号触发 snapshot 单事务读取，避免 combine 独立查询形成混合快照。Kotlin 实体按值比较，Flow distinctUntilChanged；UI 仍接收不可修改的字段快照。
- 同步是命令，不返回/重读整份缓存。只有冷启动、Flow 或账户归属变化时读取快照；失败保留内容。内存 revision 在串行锁内更新，UI 拒绝较旧快照；退出取消观察，成功清理后重建观察。错误/加载/重试/进度不持久化。
- AppLabels/AppActions、导航、主题/网站目录保存资源 ID。UiText.Resource/Quantity/Literal 在 UI 解析，嵌套消息参数仍由资源格式化，ViewModel 不持有 Context。布局、API key/路径、存储身份不从显示文字派生。
- 尚无正式 release，移除 LegacyUiAdapters；旧设备测试非空假设和旧签名已过时，不为其保留生产适配。0.22.1 进一步删除 schema1–4 迁移及导出文件、旧数字 Tab/页面别名与文案筛选键适配，只使用当前存储键；缺失或无效状态仍回退默认值。未修改/执行 UI Test。
- Android 17 行为按官方文档审阅；使用 Network Security Config 禁止明文，不绕过 TLS/CT。没有 LAN/OTP/后台音频/RemoteViews 功能，不增加无关权限。现有手机 API36，API37 系统运行验证仍待对应设备；构建通过不能替代运行验证。

依据：[AGP 9.4](https://developer.android.com/build/releases/agp-9-4-0-release-notes)、[内置 Kotlin](https://developer.android.com/build/migrate-to-built-in-kotlin)、[Gradle 9.8](https://docs.gradle.org/9.8.0/release-notes.html)、[Android 17 目标行为](https://developer.android.com/about/versions/17/behavior-changes-17)。检查与交付见 [validation.md](validation.md)。工具链尚有 Configuration.setVisible 的 Gradle 11 弃用提示，不宣称兼容未来 Gradle。

## 0.23.0：性能采集与启动初始化

- `profile`变体继承Release的R8/资源收缩，非debuggable，以本地debug签名保留同包名数据；仅此变体启用`profileable android:shell=true`及`PERFORMANCE_TRACING`。Debug/Release的固定标记通过常量关闭，不引入自动UI测试/benchmark模块或新依赖。
- `traceWork`仅包同步区域，用try/finally在同一线程配对；标记固定为MWP.CookieManager.init、Session/Api/Repository.create、Appearance.load、Cache.snapshot、Background.render、Feed.html，不含姓名、响应或认证值。挂起/网络等待不使用同步trace跨线程配对。
- CookieManager从Application主线程提前初始化改为AuthSession线程安全lazy；原生CookieJar首次访问发生在请求IO线程。缓存离线阅读无需加载WebView provider；显式Sign out仍在Main清会话，LoginActivity仍设置SSO第三方Cookie策略。Cookie默认接受，无需重复setAcceptCookie(true)，未改变Cookie来源、host白名单、flush与取消规则。
- [capture.py](../tools/performance/capture.py)明确要求物理序列号，拒绝模拟器和debuggable测量；配置从stdin传入，避免OEM对配置目录的SELinux限制。冷启动模式只force-stop/start本应用；manual模式只录制，由用户操作手机。64MiB缓冲、固定有界时长，记录安装APK/hash、设备/版本、原始trace及am辅助值；不修改编译模式/电源/网络/登录，不收集logcat、截图或网络内容，不上传。
- [analyze.py](../tools/performance/analyze.py)按Perfetto启动/帧数据与固定应用标记统计多样本。拒绝缺失冷启动/关键标记或本次buffer丢包/解析错误；服务跨会话累计丢弃计数单独记录。OEM有时把新进程标成pre-initialized，此时由固定MWP标记定位upid，再按首个Choreographer/DrawFrame终点计算TTID，并明确标记来源。首帧可见不等于缓存、网络或背景全部就绪；启动帧计数不作为滚动基准。
- 原始trace只保留在不跟踪的work/performance，汇总与局限写入[validation.md](validation.md)。Baseline Profile和Classes/Messages手动滚动、主题切换内存仍需独立证据，不能从启动样本推出收益。

工具依据：[profileable](https://developer.android.com/guide/topics/manifest/profileable-element)、[Perfetto采集](https://perfetto.dev/docs/getting-started/system-tracing)、[Trace Processor](https://perfetto.dev/docs/reference/trace-processor-cli)、[官方工具下载脚本](https://raw.githubusercontent.com/google/perfetto/main/tools/trace_processor)、[CookieManager默认行为](https://developer.android.com/reference/android/webkit/CookieManager#setAcceptCookie(boolean))。本机使用官方Windows Trace Processor v58.2并核对其发布脚本内SHA-256；启动采集包含既有联网刷新和正常系统调度，不宣称实验室级稳定基准。

## 同步、缓存与恢复

```text
冷启动：Room snapshot → StateFlow → 页面 → 前台刷新
刷新：ViewModel → SyncRetry → Repository 串行锁
     → user/info 验证/账户隔离 → GET/解析 → 独立写事务
缓存：Room 表 invalidation Flow → 串行锁 + 单次事务快照 → 值去重 → StateFlow
```

- 完整刷新顺序：Timetable → Coursework → Messages → Library → Modules → Account；每次资源尝试仍先验证账户。验证成功后的课表解析失败可继续其他资源；未确认账户或登录失效则停止后续任务；传输异常耗尽当前资源预算后同样停止，不把 IOException 一律认定为离线。
- Repository 的 inStore 统一 IO 与 Mutex；authenticatedSync 统一验证、网络读取、同步状态创建与持久化；同步方法只返回 Unit。DAO snapshot 在单个 Room 读事务读取六类数据；网络请求在数据库事务外。各数据源保留独立写事务和同步时间。
- Room 当前 schema 5，仅保留当前导出 schema；数据库名称、字段与 identity hash 不变，现有 schema5 覆盖安装继续读取缓存。无迁移链或破坏性重建回退，schema1–4 不再支持直接升级；安装包不会自动清除数据库、Cookie 或主题偏好。
- 每份同步状态记录账户归属；发现账户变化后，在下载前清空旧数据，再一次应用新身份和快照。withCache 只替换持久字段，保留其他资源的错误、加载和进度。
- 同步中重复刷新受 busy guard 限制。成功登录在 busy 时排队，当前任务结束后合并为一次完整刷新；退出时清除待刷新。单项刷新只处理自己的问题和进度，成功不重报另一资源的旧失败；失败保留现有展示；无有效新写入时 Flow 不替换内容，不以坏响应覆盖缓存。
- IOException、HTTP 408 / 5xx 最多额外重试两次，延迟 2 秒、5 秒；无活动网络不进入请求和重试；有网络时预算不变。认证、解析异常及其他 HTTP 错误（含 429）不自动重试。CancellationException 继续传播。
- Messages 最新页 limit=100；只有用户要求才用缓存最旧 id 作 before 游标。先验证/清理账户，再检查游标。分页合并按日期倒序/id 排序、去重、最多 500 条；非空且无新增 ID 的页拒绝。最新页刷新重置旧分页，失败重试保留原游标。
- 同步调用通过 StudentApi.request → CancellableRequests 绑定协程取消与当前 OkHttp Call.cancel；注册前即建立线程安全取消作用域，取消不能漏掉随后注册的请求。网络读取/解析在 IO 线程执行，响应由 use 关闭，取消后的结果不交回持久化流程；退出前以 NonCancellable 等待该 IO 工作完成，避免迟到 CookieJar 回调恢复旧会话。Sign out 的 cancelAndJoin 会立即取消同步请求，再经 Repository 清理会话/数据库；验证后及写前 ensureActive、Mutex 均保留；退出先取消并等待同步与缓存观察。登录/Developer probe 的同步入口保持原行为；不在等待前清 cookie。

## 展示快照与计算边界

- withCache 将 Room Entity 转为私有 Kotlin 值快照（所有字段 val）；页面消费 EventContentItem / CourseworkContentItem / FeedContentItem 只读契约，保留 ID、完整详情、链接与排序信息。Entity 为 Kotlin 可变 data class，仅用于解析/Room；UI 不持有持久化对象。schema 未变，旧 UI 适配已删除。
- 新列表按字段值相等复用原列表，否则包装为不可修改列表；进度 copy 不重新投影数据。每次真实缓存投影仍读取全资源，并逐资源比对，不把同步时间当内容相等的依据。不向 Entity / 任意 List 添加 @Immutable。
- Home / Classes 使用只包含本页数据及恢复状态的投影，进度由顶部读取；恢复回调保持引用并在点击时取最新状态。首页 next/today/deadlines、Tasks 搜索分类/排序、详情冲突集合按各自数据/时间依赖 remember；查询仍留页面本地。
- 时钟在 Lifecycle.STARTED 期间每30秒更新，回前台立即校准，继续按 Europe/London 计算午夜与 DST；不截断原始 deadline 时间。冲突匹配仍为 O(n²)，内层索引循环避免反复 drop 临时列表。
- 可用 `:app:compileDebugKotlin -PcomposeReports=true --rerun-tasks` 输出完整编译器报告到 app/build/reports/compose；普通构建不开启报告。报告表示可跳过性/稳定性，不是实际重组次数或帧率；本批不新增性能插件或 UI Test。[编译器 DSL](https://kotlinlang.org/docs/compose-compiler-options.html)、[取消语义](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/suspend-cancellable-coroutine.html)。

## 页面命名、导航与恢复动作

- 页面/入口/同步资源共享 `config/AppLabels.kt` 的名称：Home、Classes、Tasks、Me；Settings、Data status、Developer tools、Messages、Library、Modules。Tab与顶部直接解析同一个 labelRes；Home顶部继续显示个性化招呼语。Now/Next/Today/Deadlines区段与常用Sign in/Sign out/Retry/Back等动作也集中维护，主要说明/错误/进度也使用资源；保留英文与英国日期格式，未新增翻译。
- 显示名与身份分开：AppTab/CourseworkFilter用显式key，MeRoute只允许Overview/Settings/DeveloperTools/Feed，DetailSelection只允许None/Class/Task/Feed。Saver仅保存key、Feed现有固定数据库key与内容ID，不保存实体、HTML或列表，不使用enum.ordinal。旧数字Tab/字符串筛选值有解码回退；这不是跨应用版本Compose存档位置的迁移保证。
- Classes选日、followToday与列表状态仍保留；Tasks查询仍在本页，不改切页后的查询产品规则。恢复详情先等资源缓存可判定，再检查ID和Feed归属；初始空身份/空缓存不当成账户切换。真正退出或账户变化仍清理详情/Me子路由/筛选；Debug工具不可用时回退Me首页。
- SyncProgress只存SyncOperation（Refresh(resource)/OlderMessages），界面再生成文字。SyncNotice只存一个RecoveryAction（RefreshAll/Refresh(resource)/OlderMessages/SignIn），旧feed/resource/olderMessages兼容入口已删除，不另存重叠状态。
- Snackbar按notice ID执行一次；动作及消费回调用rememberUpdatedState取得最新实现。点击时检查当前busy/退出/登录状态，过期会话转登录；旧更早消息提示仅在当前仍有失败标志与有效分页入口时沿用当前游标，否则刷新最新Messages。重试预算、顺序、缓存保留与notice消费ID规则不变。

## 数据反馈

| 状态 | 呈现与规则 |
| --- | --- |
| 初始缓存读取 | 简短加载文字，不伪装成网络进度；已有内容优先显示 |
| 正常更新 | 顶部确定进度条替代下拉旋转圈；全量六项，单资源/更早消息一项 |
| 阶段进度 | 账户验证完成半项；成功写入或最终失败处理完成整项。等权计量任务处理，不是字节、耗时或成功率 |
| 等待/重试 | 停留在真实进度；重复认证不重复计数，分母不变。原品牌小字位置显示资源/序号或 Retry 1/2 |
| 结束/提前停止 | 真实值变化以 200ms 平滑，结束最多停留 250ms 后隐藏；有失败用错误色与一次短 Snackbar。未执行项不补满，退出清除进度 |
| 未加载 / 真空列表 / 筛选为空 | 分别提示；Clear search 只清 query、不请求网络，Tasks 保留分类 |
| 请求失败 / 登录过期 | 缓存可读，内容附近按需 Retry / Sign in，Me 标记需要登录；不常驻顶部错误卡片或日志页脚 |
| 按需状态 | Me → Data status 查看六类更新时间/状态并单项刷新；Developer tools 为独立调试流程，不计入同步进度 |

更早消息失败独立记忆，恢复操作沿用游标；下拉读取最新页，失去旧游标时回退普通刷新。设置、复制邮箱、日期选择与普通链接点击不触发业务刷新。

## 页面与内容分层

| 页面 | 当前布局与行为 |
| --- | --- |
| Home | 16sp短招呼语；Now / Next唯一强调卡，时钟/标签/右对齐日期时间、16sp Medium名称/原代码、地点/右下箭头三行，整卡详情。四个网站在Next下12dp，Quiet入口最小48dp；课程与Deadlines普通单层分区，Messages使用Quiet底色；18dp标题图标共用紧凑标题留白，区块间14dp。Today仅保留Now，省略与强调卡重复的Next标记 |
| Today / Tomorrow / 周末 | homeAgenda按Europe/London日期与结束时间保留正在进行/未来课程，无剩余课程与今天本来无课共用Tomorrow切换。周末无剩余课程显示Enjoy your weekend；当天有已结束课程时加Well done，Next仍可展示未来课。Tomorrow没有记录时不显示空分区，不新增“明天没课”提示；未加载不判断无课。全日/跨日课程仍保留正确日界，稳定Lazy key避免后续分区错位 |
| Deadlines | 最多三条未来记录，左列数字/days，右列原始标题及d MMM，下一年才加年份；当天未过期为0 days。All进Upcoming，Recently passed统计近七天缓存内过去条目并进Past，不推断是否提交 |
| Home Messages | 最近两条按日期降序、position/id稳定排序；标题最多两行、摘要一行、来源/简短日期。仅转换两条HTML，共用按账户隔离的FeedTextCache；无记录时省略分区，不把未加载当成无消息。点击复用详情，关闭保留Home；All进入Me→Messages。只读，不改变网站已读/新增角标或后台请求 |
| Classes / Schedule | 从今天开始的连续日期分组；日期只在小标题出现，未来空日跳过，起点空日保留标题/一行说明。同日共用底色与浅分隔线，左侧起止时间、右侧名称及代码/地点、整行详情；保留 Now/Next、冲突、全日和跨日提示 |
| 日期选择与跨日 | 日期按钮默认 dd/MM/yyyy 输入，可切日历；只筛缓存，离开今天显示 Today。选择日与滚动位置在切页/刷新/Activity 保存状态恢复时保留，正常冷启动回今天。UTC 日期组件值先转日历日期；跨日条目按每日范围裁为 00:00–24:00，午夜结束不插空日，详情保留完整范围 |
| Tasks / Coursework | 紧凑搜索 + Upcoming / Past；未来升序、过去降序。连续列表左列数字/days、Today 或 Passed；标题最多两行，d MMM · HH:mm，不同年加年份。无匹配可清搜索，完整信息/来源说明留详情 |
| Me | 紧凑姓名/usercode 与真实邮箱，显式复制后短暂勾选；不拼邮箱、不读取剪贴板。Sign out 保留确认。App / Websites 分组圆角网格，默认三列，fontScale >1.3 或可用宽度 <280dp 改两列；网站有外部角标 |
| Feed / 详情 | Messages 纯文本搜索/详情与手动分页；Modules 显示已知字段及数组数量；Library 使用学校摘要与原站入口。详情可滚动读完整长内容；只有显式点击才打开来源网站 |

## 可复用组件标准

| 组件 | 责任 / 约束 |
| --- | --- |
| AppCard | Normal / Quiet / Featured / Selected 四种颜色角色、形状与整卡点击；不隐式加内外边距、边框或阴影。非点击分区无按钮语义；点击重载要求 actionLabel，单动作只有一个处理器 |
| SectionCard / SectionAllAction / GroupedListItem / ListDivider | 单层分区标题/可选18dp图标与颜色角色、共享All点击语义、连续列表首尾圆角和分隔线；不为标题再套卡片。LazyColumn稳定key和列表状态属于页面 |
| MetricListRow | 左信息列、居中、行高/留白与箭头；不接受领域 Entity，业务含义留在插槽 |
| ScheduleClassRow / DeadlineRow | 全日/跨日/冲突/Now-Next；deadline 的 Home/List 两个明确变体，保留日期与标题差异 |
| ContentIcons / LocationLabel | 统一线条风格；课程地点用14dp定位针+文字，Home/Classes/详情共用。缺少地点时仅显示文案；图标不请求GPS、不创建独立导航，已知地点链接仍由详情打开 |
| ActionTile | Compact 首页：Quiet底色、最小48dp、20dp图标/常规标签、上下4dp/内容间2dp；Grid Me保留Normal底色、24dp图标与原有尺寸。内外目的地角标与完整点击区共用 |
| SearchField | 图标、单行输入、清除与键盘 Search；筛选/query 归页面 |
| DetailsSheet / DetailHeader / DetailField / DetailClose | 面板、滚动、标题/字段/关闭骨架；业务使用 LazyListScope 组合字段，类详情保留 24/16dp，其余 24dp 留白 |
| AppPageHeader / AppNavigation / SyncProgressBar | 顶部几何、Tab 语义与状态、实际任务进度；由 PlusScreen 提供状态/动作 |

| 几何 / 颜色 | 标准（最小值允许长内容/大字体增长） |
| --- | --- |
| 颜色角色 | Normal=surfaceContainerLow/onSurface；Quiet=surfaceContainer/onSurface；Selected=primaryContainer/onPrimaryContainer；Featured=emphasisColours，由主题决定，页面不判断主题名 |
| 圆角 AppShapes | 分区/列表/紧凑入口 14dp；账户/网格 16dp；强调/设置/Tab 18dp；Feed 内容 20dp；搜索 24dp |
| Spacing | Home/Classes/Me 左右16、顶部4、底部16dp；Tasks/Feed/Appearance 保留20dp。首页区块14、Me区块16、列表12、网格8dp；父组件拥有间距 |
| 标题 / 页眉 | 分区内行最小20dp，外部左右12/上8/下2dp，共用sectionHeader/sectionHeadingHeight；All视觉高度同标题行，labelMedium/Medium/onSurfaceVariant。日期小标题labelLarge。AppPageHeader最小48dp、上下2dp，动作不改变品牌文字位置 |
| 信息行 | 左列48sp转dp，随字缩放，两行居中、间距2dp；左右列间距10dp。MetricListRow默认Standard：正文bodyMedium/SemiBold、最小64dp/上下10dp，Classes/Tasks保持此标准。首页显式Compact：14sp Regular标题/18sp行高、时间和倒计时Medium、最小54dp/上下6dp；分区首行48dp/上0/下8dp。Messages标题14sp Regular/18sp行高，首行48dp/上0/下6dp，后续54dp/上下6dp；空行上0。次要信息bodySmall/onSurfaceVariant；长内容/大字体自然增长，不以固定高度裁切 |
| 搜索 / 箭头 | 搜索最小48dp、图标20dp、清除操作区48dp；普通详情箭头16dp，Next地点行20dp，网格角标12dp；装饰图标的动作语义在点击容器 |
| 空状态 | SectionEmptyRow 分区内直接一行；DataEmptyState 用于独立未加载/空/无匹配，可带恢复动作，不嵌套空卡 |
| Tab | Home / Classes / Tasks / Me；12sp常规字重，轮廓/实心两态，图标和文字共用高亮。selectable + Role.Tab，indication=null，直接最终色；最小64dp，两侧20dp，安全区一次 |

不为每个 Text 建包装组件；变体需有实际复用需求。更改标准先改公共实现与此表，再复核受影响页面；保持 query、筛选、列表位置与 rememberSaveable 状态归属。

## 主题与背景

Me → Settings → Appearance 选择 Forest（默认）、Lake、Heather、Sand、Rosewood，Fine texture 默认关闭。固定 palette 决定明暗与系统栏图标，共用布局，不提供浅/深/系统模式或动态颜色。SharedPreferences 只保存主题 ID/纹理开关，StateFlow 即时应用，未知 ID 回退 Forest。

下表是 **Home Now / Next 实际 Featured 色**，不是 palette.next 字段的同名推断；Lake 使用 primaryContainer，其他使用 primary。普通卡片不透明，边界通过颜色区分，不添加边框。

| 主题 | 背景基底 | Featured / 文字 | 普通卡片 / 正文 |
| --- | --- | --- | --- |
| Forest | #111B17 | #BFE2CA / #111B17 | #465F4E / #E5ECE7 |
| Lake | #F0F6FA | #D6E8F1 / #183743 | #FBFDFE / #1D303C |
| Heather | #F5F1FA | #523367 / #F5F1FA | #FDFCFE / #2F2A3A |
| Sand | #F8F3EA | #5D3D23 / #F8F3EA | #FFFCF6 / #342B23 |
| Rosewood | #21181D | #F0C2D1 / #21181D | #674D5B / #F0E5E9 |

- Theme.kt 是完整配色唯一来源；错误/冲突仍用文字或图标表达，不只靠颜色。
- ThemeBackground 绘制三层非线性椭圆径向渐变：中心 (8%,2%)、(105%,48%)、(8%,104%)；宽/高半径 (85%,62%)、(90%,65%)、(95%,65%)；停点 0/.2/.5/.8/1，对应 alpha .78/.702/.351/.0624/0。光晕颜色取 palette.spots，不在文档再复制整套颜色。
- 纹理为固定种子73219、128×128灰度块、alpha 6/255，是静态材质，不是实时背景模糊。
- Dispatchers.Default 生成 ARGB 位图；长边1024px、短边按16px量化，ThemeBackgroundCache 的进程内 LRU 最多两张，键为主题/纹理/量化尺寸。准备时显示基底，普通重组/滚动复用；重启重建，无磁盘缓存，不 recycle 仍可能显示的旧位图。
- 1080×2400比例生成464×1024，单张约1.81MiB，两张约3.63MiB；方形两张像素上限8MiB，纹理约64KiB。数字不含淘汰后仍显示的位图及GPU纹理；帧时间、生成耗时与GPU总内存尚未测量。

## 共享函数与外部链接

| 来源 | 规则 |
| --- | --- |
| StudentDates | Europe/London、Locale.UK 不可变格式器；按英国自然日算days、按精确时间判断passed；首页仅未来年份加年，Tasks不同年加年并显示时间 |
| ClassPresentation | 名称/原代码选择、跨日范围、短问候语；缺失可读名称和原标题时回退 Class，完整字段留详情 |
| CourseworkPresentation / FeedPresentation | 纯筛选；query只trim一次，FeedContent每个条目快照转换一次HTML纯文本，搜索/列表复用；快照/账号变化重建 |
| NetworkDates | Coursework/Feed共用 +01、Z、区域后缀兼容；Timetable保留ISO_ZONED_DATE_TIME。无时区拒绝整批 |
| BrowserLinks / BrowserActions | HTTPS、默认或443端口、无userinfo；作业/模块额外限制Warwick域。地点支持MyWarwick相对地址；无效地址无按钮。唯一Custom Tabs入口不导出cookie/header |
| ServiceCatalog | 八个网站地址/短标签/图标集中维护，Home取四项；Library/Moodle回退也共用配置，不能匹配label选择目的地 |

普通页面 rememberBrowserOpener 失败用 Snackbar；详情 ExternalLinkButton 在面板内显示失败并可重试，避免提示被遮罩覆盖。保留作业链接注入回调优先级，协程取消继续传播，不记录失败URL。

## 维护约定

当前标准只在此文档维护；API 状态、协议证据和验证结果分别更新各自文档。已完成阶段计划不再另存，细节/备选设计从 Git 历史查阅。构建缓存、工作日志/截图不当作当前标准；work/ 的一次性重构脚本可在落地后移除。测试与物理部署按 [AGENTS.md](../AGENTS.md) 执行。

## 架构评估基线与实施进度

2026-10-07 静态评估基线：应用代码提交 c8a2724 / 0.18.0。审阅同步/状态/DAO、网络与登录、页面路由/展示计算、基础组件/主题缓存和相关既有检查；未运行编译器重组报告、手机测量或新学校请求。P1 表示下一轮优先处理，P2 表示随后改善；不是宣称已发生崩溃或数据泄漏。下方问题表描述0.18.0评估时的事实，不代表0.19.0仍有同样实现；现行行为以上文标准和下表状态为准。

| 范围 | 0.20.0 进度 |
| --- | --- |
| A1 | 已实现每次同步请求取消连接；真实本地阻塞响应与注册竞态检查通过，退出手动体验待验收 |
| A2 | 已建立生产展示值快照、不可修改列表和内容相等复用；schema 5 不变，快照隔离检查通过 |
| A3 | 已缓存 Home / Tasks 计算、缩小 Home / Classes 输入并隔离进度；完整编译器报告核对，真机性能未测 |
| A5 | 前台生命周期时钟已实施；进一步按页面需求暂停及耗电测量未做 |
| A4 / A6 | 已实施类型化导航/筛选/操作身份、统一页面命名与最新notice动作分派；存档/延迟恢复的聚焦检查通过，手机交互待手动验收 |
| A7 | 编译器报告入口已建立；真机帧时间/内存、SQLite基线仍待做 |

### 评分与已有优势

按小规模 Android 学生客户端的维护目标，综合 **7.5/10**；分项加权得到 7.475 后取一位小数。10 分代表职责明确、关键边界可验证且有持续测量，5 分代表具备基础但主要依赖人工判断；不是性能跑分或生产安全认证。

| 维度 | 分数 / 权重 | 依据与主要扣分 |
| --- | --- | --- |
| 职责分层 | 8 / 20% | UI、同步编排、API、DAO 已分离；PlusScreen 仍同时管理多个路由/详情状态 |
| 账户与缓存可靠性 | 8 / 25% | origin 限制、账户隔离、原子替换、读事务与失败保留已实现；阻塞 HTTP 取消/退出时序仍可改进，0.18.0 真机待验收 |
| 状态与类型边界 | 7 / 20% | StateFlow 与统一状态转换已建立；UI 仍拿可变 Entity、导航/筛选/进度身份部分依赖数字或文字 |
| 展示与组件复用 | 8.5 / 15% | 单层容器、行几何/业务变体、日期/链接配置集中；首页与 Tasks 计算失效范围仍可收紧 |
| 性能可观测性 | 5 / 10% | 已有懒列表 key、部分 remember、生命周期订阅、背景缓存；缺重组/帧时间/发布配置基线，不代表当前必然卡顿 |
| 交付与验证 | 7 / 10% | schema 历史、聚焦业务检查、API 证据和物理交付记录齐全；最新真实 SQLite/手机反馈尚未补齐 |

保留单 app module、一个共享同步 ViewModel、串行资源更新和现有组件层级。MainActivity 的 collectAsStateWithLifecycle、FeedContent 的纯文本/搜索缓存、Schedule 的分组/冲突缓存与背景 LRU 已是合理实践，不为评分增加多模块、DI 框架或每页 ViewModel。

### 架构问题清单

| 编号 / 优先级 | 源码依据与判定 | 建议与验收条件 |
| --- | --- | --- |
| A1 / P1：退出取消 HTTP 太晚 | [TimetableViewModel](../app/src/main/java/uk/ac/warwick/plus/ui/TimetableViewModel.kt) signOut 先 cancelAndJoin；[PlusApplication](../app/src/main/java/uk/ac/warwick/plus/PlusApplication.kt) 的 cancelRequests 直到 endSession 才调用。[MyWarwickApi](../app/src/main/java/uk/ac/warwick/plus/data/MyWarwickApi.kt) 使用同步 execute，callTimeout=35秒。**时序已确认**，阻塞时可能等读取结束/剩余超时；未量手机延迟 | 将“取消当前网络”与“清会话”拆开：先标记退出/取消任务、取消请求，再等待结束和清理；可通过单 Call 的协程取消连接保证更强时序，避免只 cancelAll 留竞态。保留写入前取消检查、Repository 锁、失败恢复，不在等待前清 cookie。用一个聚焦阻塞/取消编排检查验证及时完成及无晚写，真实退出由用户自愿手测 |
| A2 / P1：展示模型直接使用可变持久对象 | [TimetableState](../app/src/main/java/uk/ac/warwick/plus/ui/TimetableState.kt) 的 events/entries 是 Entity 列表，Entity 为公开可写 Java 字段；withCache 每次投影全部资源。[Repository](../app/src/main/java/uk/ac/warwick/plus/data/TimetableRepository.kt) 验证后及写后都读全快照，可能使未变内容拿到新对象。**耦合已确认**，重组代价未测 | 在持久化→展示边界建立字段为 val 的 Kotlin 快照，保留 ID/完整详情/来源；不改 Room/schema。按资源内容变化复用未变展示快照，新增投影不能在仅进度更新时重建全量数据（当前进度 copy 本身是浅拷贝）。List 只读不等于深不可变，不给 Entity 或任意集合批量加 @Immutable；账户变化仍立即清空全部展示数据 |
| A3 / P1：页面重复计算与状态范围 | [HomeScreen](../app/src/main/java/uk/ac/warwick/plus/ui/HomeScreen.kt) 在组合体内取 next/today/deadlines；[CourseworkScreen](../app/src/main/java/uk/ac/warwick/plus/ui/CourseworkScreen.kt) 每次筛选/排序，PlusScreen 接受整份状态。**计算路径已确认**，进度重组时可重新执行，但未证明用户可感知卡顿 | 先以 events、entries、query/filter、London 日期与必要时钟为依赖缓存纯结果；缩小内容组件输入，让进度单独消费。不要把页面 query 强搬到全局。编译器报告/手机跟踪验证失效范围，核对 midnight、正在上课、准确 deadline 边界与切页后位置 |
| A4 / P2：路由与筛选缺类型约束 | [PlusScreen](../app/src/main/java/uk/ac/warwick/plus/ui/PlusScreen.kt) 使用 tab=0..3、Feed key、多个详情 ID/show 布尔值；Tasks 使用 Upcoming/Past 字符串。**维护风险**，未记录非法状态导致的实际故障 | 少量 enum 表示 Tab/Filter，有限类型表示 Me 子页与当前详情；按稳定 ID/name 用 Saver 保存，兼容旧筛选值回退 Upcoming，不使用 enum.ordinal 替代数据库 Feed key。保持返回、选日/列表位置、账户切换清选择；搜索词是否切页保留按现行为，不顺带改产品规则 |
| A5 / P2：顶层时钟未绑定前台 | PlusScreen 的 LaunchedEffect(Unit) 每30秒更新 now；MainActivity 的生命周期订阅只控制 Flow 采集，不直接暂停这个循环。**未显式暂停已确认**，实际后台调度/耗电未测 | 时钟只在至少 STARTED 且对应内容需要时工作，回前台立即更新；保留30秒精度，跨英国午夜与 DST 正确。不以截断到分钟破坏 deadline 精确过期判断，不额外启动后台任务 |
| A6 / P2：操作身份与提示文案混合 | TimetableState.updating 对非 Feed 比较 SyncProgress.resource 与 label；SyncNotice 同时有 feed/resource/olderMessages；PlusScreen 在 notice.id Effect 内捕获 state 与动作。**演进风险**，未复现错误恢复 | 为进度保存资源/操作身份，文字仅生成描述；恢复动作明确为整轮/单资源/更早页/登录，并在点击时采用当前状态或更新后的回调，保留旧游标与 notice 消费 ID。不把普通状态布尔值全部一次改成复杂状态机 |
| A7 / P2：性能基线与数据库真机缺口 | [validation.md](validation.md) 记录0.18.0尚未安装；现有 Repository fixture 替代不了真实Room。release有R8，但当前无重组报告、滚动帧数据或可测发布配置 | 先完成已有 APK 的用户手机验收；随后用一致数据/设备/构建记录启动、Classes/Tasks/Messages滚动、刷新中重组和主题切换内存。记录前后差值；不拿单次 am start 时间当稳定基准，不未经要求新增 UI Test 或自动退出 |

0.18.0冲突检查的内层 drop 和详情重复计算已在0.19.0改为索引循环/remember；仍为 O(n²)，普通规模未证明是瓶颈，暂不引入区间树。Feed搜索每条创建短 listOf 后 any 可改直接 OR，属于低风险少量分配优化，不承诺可感知提速。

### Kotlin 写法与语法糖评估

“语法糖”分为类型安全、表达清晰和微小分配三类；新语言版本不是这些改动的前提。本批未升级 Kotlin、Gradle 或依赖，也不把代码缩短视为性能证据。

| 项目 | 评估 / 优先级 | 具体范围与保持规则 |
| --- | --- | --- |
| enum + 穷尽 when | 值得做，P2；主要是类型安全 | A4 的 Tab/Filter 及 A6 操作身份。界面仍显示原标签，保存状态有兼容回退；不把持久ID换成ordinal |
| 嵌套 let/Elvis、长位置参数 | 顺手整理，P2；主要是可读性 | PlusScreen 的刷新回退、ViewModel 的 notice 构造、MainActivity 的 PlusScreen 调用用命名参数/局部变量/明确 when 或 if。先保存 singleOrNull 结果，删 startSync 中 resource→target 的无意义别名；保留回调缺失时的降级与单次调用，不能删“看似重复”的账户验证 |
| 非空断言 | 小范围整理，P2 | ApiProbeSheet 的 problem!! 可用局部非空值/let 捕获；TimetableState 的 resource.feed!! 与 requireNotNull 承担当前 enum 不变量，优先随类型映射收敛，不能改为 ?: return 静默漏资源。未发现已复现空指针 |
| apply/also/use | 当前用途合理，保留 | Parser 的 apply 构造Java实体、also校验、HTTP use关闭响应；不批量互换作用域函数。require失败仍拒整批，不能改 mapNotNull 跳过坏条目以缩短代码 |
| runCatching / Result / fold | 不全局改写 | 同步URI等失败转空值可保留；ViewModel/SyncRetry 的显式catch继续传播CancellationException。登录检查目前runCatching只包同步user()，withContext在外，不能据此声称它已经吞协程取消；以后接入挂起网络须重新核对取消传播，避免无条件getOrNull/fold |
| Sequence / 集合链 | 按测量选用，低优先级 | 三个首页deadline、最多500条消息不值得全部转Sequence；保留稳定排序/重复ID拒绝。先缓存和减少重复计算，再考虑热点分配，不仅替换语法 |
| import / 多语句单行 | 顺手整理，低优先级 | 受影响文件显式必要import、展开有副作用/错误路径的分号串联；单行纯getter可保留。不做全仓格式重写或为了命名重建Room/KSP配置 |

建议最终约定：纯值转换可用简短表达式；多步副作用/失败处理展开；作用域函数只用于清楚的对象构造/非空捕获；字符串用于文案，类型用于状态；取消继续传播。上述写法变化不要求新增简单样式或镜像实现的单元测试。

### 后续实施边界

1. A1/A2/A3代码与必要检查已完成；优先手动验收刷新、搜索、后台返回及缓存恢复，不自动退出真实账号。视觉、协议、schema、重试预算及串行顺序保持原约定。
2. A4/A6代码已在0.20.0完成，保留返回/选日和账户隔离语义；先手动验收，再转回功能开发。A5剩余按页调度只在有收益时再做；工具链/依赖升级另外分批。
3. A7 的测量入口已在0.23.0建立，完成同设备/同profile配置5+5次启动及一次用户手动Classes录制。CookieManager已确认移出主线程，首帧小样本变化见validation；尚无所有设备的收益结论。Messages/主题切换内存和可重复滚动对比继续补证据；Baseline Profile与自动化benchmark留到测量确有需要的专项，不默认加入UI测试。

依据：[Compose计算与状态读取](https://developer.android.com/develop/ui/compose/performance/bestpractices)、[Compose稳定性契约](https://developer.android.com/develop/ui/compose/performance/stability/fix)、[Kotlin作用域函数](https://kotlinlang.org/docs/scope-functions.html)、[runCatching捕获范围](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/run-catching.html)。它们用于判断优化方法；具体待办来自本地源码，不代表官方对本项目的评分。
