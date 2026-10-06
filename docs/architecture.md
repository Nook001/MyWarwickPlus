# 架构与组件规范

更新：2026-10-07，适用代码 0.18.0。前文记录已落地的组件、共享函数、同步、数据反馈、课表和主题标准；末节单列架构评估与未实施待办。接口证据见 [protocol.md](protocol.md)，覆盖表见 [api-progress.md](api-progress.md)，验证状态见 [validation.md](validation.md)。

## 代码职责

单个 app module，Kotlin 业务/Compose UI，Room 使用 Java 注解处理，无 KSP/kapt。源码根目录为 `app/src/main/java/uk/ac/warwick/plus/`。

| 来源 | 职责 |
| --- | --- |
| auth/ | 官方 WebView SSO、CookieManager、限定 MyWarwick origin 的 CookieJar |
| data/MyWarwickApi、各 Parser | 只读 GET、响应大小/结构校验、整批解析，不负责 UI |
| data/TimetableRepository、TimetableDao、TimetableDatabase | 账户隔离、串行读写/退出、事务快照与各资源原子缓存 |
| ui/TimetableViewModel | 单份同步任务、完整/单项/分页执行、恢复缓存、阶段进度及通知 |
| ui/TimetableState、SyncRetry | 状态类型与 withCache/withIssue/resourceFailed；独立重试策略 |
| ui/PlusScreen | 路由/返回、选择项和页面状态、账户变化后的选择清理、连接动作回调 |
| ui/*Screen、业务详情 | 组合页面、业务展示及按需详情；不直接读写数据库 |
| ui/components/ | 基础容器、列表、操作、输入、标题及详情骨架；沿用 ui 包名 |

组件只接受展示内容与动作，不持有 ViewModel、不发网络请求、不修改账户。暂不引入多模块、资源插件或并发同步；抽象由实际复用场景推动。

## 同步、缓存与恢复

```text
冷启动：Room snapshot → StateFlow → 页面 → 前台刷新
刷新：ViewModel → SyncRetry → Repository 串行锁
     → user/info 验证/账户隔离 → GET/解析 → 独立写事务 → snapshot → StateFlow
```

- 完整刷新顺序：Timetable → Coursework → Messages → Library → Modules → Account；每次资源尝试仍先验证账户。验证成功后的课表解析失败可继续其他资源；未确认账户或登录失效则停止后续任务。
- Repository 的 inStore 统一 IO 与 Mutex；authenticatedSync 统一验证、读取、同步状态创建、持久化与快照。DAO snapshot 在单个 Room 读事务读取六类数据；网络请求在数据库事务外。各数据源保留独立写事务和同步时间。
- Room 当前 schema 5，显式迁移 1→2（moduleName）→3（Coursework）→4（Feed）→5（sync_state.email）。保留旧数据，不做破坏性重建；旧导出 schema 用于迁移核对，继续保留。
- 每份同步状态记录账户归属；发现账户变化后，在下载前清空旧数据，再一次应用新身份和快照。withCache 只替换持久字段，保留其他资源的错误、加载和进度。
- 同步中重复刷新受 busy guard 限制。单项刷新只处理自己的问题和进度，成功不重报另一资源的旧失败；失败恢复已保存快照，不以坏响应覆盖缓存。
- IOException、HTTP 408 / 5xx 最多额外重试两次，延迟 2 秒、5 秒；认证、解析异常及其他 HTTP 错误（含 429）不自动重试。CancellationException 继续传播。
- Messages 最新页 limit=100；只有用户要求才用缓存最旧 id 作 before 游标。先验证/清理账户，再检查游标。分页合并按日期倒序/id 排序、去重、最多 500 条；非空且无新增 ID 的页拒绝。最新页刷新重置旧分页，失败重试保留原游标。
- 验证后与写入前检查取消状态；阻塞下载返回时已取消则不写入。HTTP 阻塞不会因协程取消立即停止。Sign out 先 cancelAndJoin，再经 Repository 清理应用会话/数据库；失败阻止重新刷新并要求恢复。认证边界见协议文档。

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
| Home | 16sp 短招呼语带名字；不重复显示当前日期、Refresh 或 View messages。Now / Next 为唯一强调卡：标签/右对齐日期时间、名称/原代码、地点/右下箭头三行，整卡详情；全日项仅进 Today。四个常用网站在 Next 下 12dp；Today、Deadlines 各自单层背景容纳标题和透明内容行，区块间 24dp |
| Today / Deadlines | 已加载且今天无课只显示 No classes today。Deadlines 最多三条未来记录，左列数字/days，右列原始标题及 d MMM，下一年才加年份；当天未过期为 0 days。All 进 Upcoming，Recently passed 统计近七天缓存内过去条目并进 Past，不推断是否提交 |
| Classes / Schedule | 从今天开始的连续日期分组；日期只在小标题出现，未来空日跳过，起点空日保留标题/一行说明。同日共用底色与浅分隔线，左侧起止时间、右侧名称及代码/地点、整行详情；保留 Now/Next、冲突、全日和跨日提示 |
| 日期选择与跨日 | 日期按钮默认 dd/MM/yyyy 输入，可切日历；只筛缓存，离开今天显示 Today。选择日与滚动位置在切页/刷新/Activity 保存状态恢复时保留，正常冷启动回今天。UTC 日期组件值先转日历日期；跨日条目按每日范围裁为 00:00–24:00，午夜结束不插空日，详情保留完整范围 |
| Tasks / Coursework | 紧凑搜索 + Upcoming / Past；未来升序、过去降序。连续列表左列数字/days、Today 或 Passed；标题最多两行，d MMM · HH:mm，不同年加年份。无匹配可清搜索，完整信息/来源说明留详情 |
| Me | 紧凑姓名/usercode 与真实邮箱，显式复制后短暂勾选；不拼邮箱、不读取剪贴板。Sign out 保留确认。App / Websites 分组圆角网格，默认三列，fontScale >1.3 或可用宽度 <280dp 改两列；网站有外部角标 |
| Feed / 详情 | Messages 纯文本搜索/详情与手动分页；Modules 显示已知字段及数组数量；Library 使用学校摘要与原站入口。详情可滚动读完整长内容；只有显式点击才打开来源网站 |

## 可复用组件标准

| 组件 | 责任 / 约束 |
| --- | --- |
| AppCard | Normal / Quiet / Featured / Selected 四种颜色角色、形状与整卡点击；不隐式加内外边距、边框或阴影。非点击分区无按钮语义；点击重载要求 actionLabel，单动作只有一个处理器 |
| SectionCard / GroupedListItem / ListDivider | 单层分区标题/内容、连续列表首尾圆角和分隔线；不为标题再套一层卡片。LazyColumn 的稳定 key 和列表状态属于页面 |
| MetricListRow | 左信息列、居中、行高/留白与箭头；不接受领域 Entity，业务含义留在插槽 |
| ScheduleClassRow / DeadlineRow | 全日/跨日/冲突/Now-Next；deadline 的 Home/List 两个明确变体，保留日期与标题差异 |
| ActionTile | Compact 首页 / Grid Me、图标/标签、内外目的地角标与完整点击区 |
| SearchField | 图标、单行输入、清除与键盘 Search；筛选/query 归页面 |
| DetailsSheet / DetailHeader / DetailField / DetailClose | 面板、滚动、标题/字段/关闭骨架；业务使用 LazyListScope 组合字段，类详情保留 24/16dp，其余 24dp 留白 |
| AppPageHeader / AppNavigation / SyncProgressBar | 顶部几何、Tab 语义与状态、实际任务进度；由 PlusScreen 提供状态/动作 |

| 几何 / 颜色 | 标准（最小值允许长内容/大字体增长） |
| --- | --- |
| 颜色角色 | Normal=surfaceContainerLow/onSurface；Quiet=surfaceContainer/onSurface；Selected=primaryContainer/onPrimaryContainer；Featured=emphasisColours，由主题决定，页面不判断主题名 |
| 圆角 AppShapes | 分区/列表/紧凑入口 14dp；账户/网格 16dp；强调/设置/Tab 18dp；原 Feed 兼容形状 20dp；搜索 24dp |
| Spacing | Home/Classes/Me 左右16、顶部4、底部16dp；Tasks/Feed/Appearance 保留20dp。首页区块24、Me区块16、列表12、网格8dp；父组件拥有间距 |
| 标题 / 页眉 | 分区最小28dp，labelMedium/SemiBold/onSurfaceVariant；日期小标题 labelLarge。AppPageHeader 最小48dp、上下2dp，动作不改变品牌文字位置 |
| 信息行 | 左列48sp转dp，随字缩放，两行居中、间距2dp；左右列间距10dp。正文bodyMedium/SemiBold，次要bodySmall/onSurfaceVariant。普通行最小64dp、左右12/上下10dp；分区首行最小56dp、上2/下8dp |
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

## 架构评估与待办（第一批，仅评估）

2026-10-07 静态评估基线：应用代码提交 c8a2724 / 0.18.0。审阅同步/状态/DAO、网络与登录、页面路由/展示计算、基础组件/主题缓存和相关既有检查；未运行编译器重组报告、手机测量或新学校请求。P1 表示下一轮优先处理，P2 表示随后改善；不是宣称已发生崩溃或数据泄漏。以下全部**未实施**，不改变上文现行行为。

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

冲突检查是 O(n²)，且每轮内层 drop 会产生临时列表；首页/课表已按 events 缓存，普通规模未证明是瓶颈。下一轮可复用冲突结果、详情也按 events 缓存；暂不引入更复杂的区间树。Feed搜索每条创建短 listOf 后 any 可改直接 OR，属于低风险少量分配优化，不承诺可感知提速。

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

1. 小补丁优先解决 A1 的退出响应；验证不晚写/不混账户，依旧不自动清真实登录。下一批随后处理 A2/A3，先不可变展示快照与数据复用，再缓存首页/Tasks计算和隔离进度读取；不改视觉、协议、schema、重试预算或串行顺序。
2. A4/A6 与有触及范围的语法整理再做一小批，保留 rememberSaveable/Saver、返回与账户切换语义；A5配合时钟/重组边界处理。不要同一提交升级工具链、重写网络、重排导航和全部Entity。
3. A7 用物理手机建立同构建/数据的前后记录。先做编译器报告和手动/跟踪测量；Baseline Profile、自动化benchmark、额外插件留到测量确有需要的专项，不默认扩展本批范围。当前无已证实性能提升。

依据：[Compose计算与状态读取](https://developer.android.com/develop/ui/compose/performance/bestpractices)、[Compose稳定性契约](https://developer.android.com/develop/ui/compose/performance/stability/fix)、[Kotlin作用域函数](https://kotlinlang.org/docs/scope-functions.html)、[runCatching捕获范围](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/run-catching.html)。它们用于判断优化方法；具体待办来自本地源码，不代表官方对本项目的评分。
