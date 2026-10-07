# 工程质量分析报告

评审日期：2026-10-07 · 代码版本 0.20.0（提交 089825f）· 范围：`app/src/main` 全部源码（Kotlin 约 3,240 行、Java Room 实体/DAO 约 200 行）、构建配置、既有 Lint/Compose 编译器报告与 JVM 测试。

基线：`:app:testDebugUnitTest` 68 项全部通过；`:app:lintDebug` 0 errors / 17 warnings；Compose 编译器报告中所有 UI 函数均为 restartable + skippable（strong skipping 默认开启）。本报告为静态审阅结论，未做真机性能测量；凡标注"未测"的性能判断都需要用 Perfetto / Macrobenchmark 验证后再投入。

## 后续实施（0.22.0，2026-10-07）

下方 0.21.0 处置表是历史记录；此前暂缓的 19–21 已按独立批次落地。尚无正式发布版本，不再为旧 UI 调用保留兼容适配；测试机已有数据仍保留。

0.22.1进一步移除数据库旧版本迁移/导出schema、旧Tab/Filter键适配与三项退役迁移检查。当前schema5不变，物理手机覆盖安装成功；正常状态校验和学校实际响应格式处理继续保留。

| 编号 | 当前结果 |
| --- | --- |
| 19 | Kotlin DAO/实体 + KSP；全部缓存表 Flow 信号驱动事务快照，值去重与 revision 顺序保护；同步只负责写入，删除写后/失败全量重读；退出等待同步/观察 |
| 20 | Browser 1.10.0、OkHttp 5.5.0、协程 1.11.0、JVM JSON 20260814；Gradle 9.8.0/AGP 9.4.1/configuration cache；targetSdk37、Network Security Config。既有取消/账户隔离规则保留 |
| 21 | strings.xml + plurals + 参数化 UiText，命名配置保存资源 ID；不仅为翻译，也用于一致文案/无障碍/格式化；主要页面、错误和进度已迁移，开发者诊断/少量时间组合仍为英文 |

完整实现约定见 [architecture.md](architecture.md)，检查及手机状态见 [validation.md](validation.md)。既有 UI Test 源码未改动也未执行，旧签名/空值假设使其不再编译；不记作测试通过。Room 引擎/Android17运行/性能测量仍与 JVM 编排检查区分。

## 实施核对（0.21.0，2026-10-07）

下文保留 0.20.0 的静态审阅基线，已发现的问题不再视为当前仍全部存在。认可风险与优先级，但评分是主观排序；“全部 UI 函数可跳过”不用于证明所有组合或真实帧性能。未采纳所有建议中的示例实现。

| 原编号 | 当前处置 | 落地点 / 理由 |
| --- | --- | --- |
| 1 | 已完成 | dataExtractionRules 排除 cloud-backup/D2D 九个域，低版本 fullBackupContent=false；未实测厂商迁移 |
| 2 | 已完成 | JsonFields 实际类型读取，三类 Parser 与 user/info 共用；聚焦 null/异型字段检查 |
| 3 | 已完成，调整定义 | 没有活动网络直接结束；重试耗尽的 IOException 停止本轮后续资源，不声称已确认离线；HTTP 503 保持资源独立恢复 |
| 4 | 已完成 | refreshAfterSignIn 合并一次待刷新，退出丢弃；Sign in 可继续使用，结果不会被 busy 丢弃 |
| 5 | 已完成 | LoginActivity 使用应用级 API 和可取消 request 包装 |
| 6 | 已完成 | SyncSlots 固定值不变；DAO/FeedKind/资源共用；deleteNonCoreStates 正确命名 |
| 7 | 采用最小修复 | globalMessage 与课表 issue 分离；未执行资源登录提示统一；completionNotice 提取。暂不引入整张 ResourceStatus 表，避免同时重写全部页面和测试构造器 |
| 8 | 已完成核心，保留兼容边界 | PlusNavigator、PlusActions、MeTab 与单次详情 when；正式入口删除回调回退链。旧入口集中 LegacyUiAdapters，未改写/扩展任何 UI Test |
| 9 | 已完成 | envelope/mapObjects/唯一 ID/字符串工具，authenticatedSync 统一保存与日志；显式资源差异仍保留；不用吞取消异常的 Result 封装 |
| 10 | 已完成代码，性能未测 | Feed HTML 在 Default 转换，列表/详情共用按账户创建的约 2 MiB 文本缓存；未模拟学校 500 条数据做性能断言 |
| 11 | 采用局部结构调整 | components 包名与目录一致，Feed 显示文案移到 UI；暂不整体改包/改名，避免把旧入口和 Room/Manifest 身份兼容一起扩大 |
| 12 | 已完成 | StudentCache 窄接口，Java DAO 实现，假缓存只实现八项操作；schema5/identity hash 不变 |
| 13 | 已完成 | :00/:30 时钟、详情按钮关闭动画、有效选日/月份/模式保存、Now/Next 规则共用、冲突集合只算一次 |
| 14 | 已完成可直接处理项 | UseKtx、ModifierParameter、备份警告修复；SSO JavaScript 抑制注明必要原因；网络状态权限用于快速失败 |
| 15 | 已完成必要清理 | 改动文件显式 import/长参数命名/格式清理；不新增 detekt/全仓重排 |
| 16 | 已完成 | 移除未用 ui-tooling-preview 直接依赖，保留 debug tooling |
| 17–18 | 保留测量前提 | 暂不加入基准/自动 UI 操作，也不未经测量改 CookieManager 初始化；下一步可独立采集真机基线 |
| 19–20 | 暂不采用 | 不同时升级 Room 处理链、HTTP/SDK/Gradle；这些需要各自协议、迁移与构建验证 |
| 21 | 暂不采用 | 仍为英文单语，AppLabels 是显示事实源；本地化需求出现后再迁移 |

本次布局/调色值、API 方法/路径、schema5 与有网络时的单资源重试预算保持不变。最终构建、测试、安装和手测状态见 [validation.md](validation.md)。

## 0. 结论摘要

项目整体质量较高：数据边界清晰（只读 GET、响应大小限制、整批校验、账户隔离、原子写入），协程取消语义处理得比多数同规模项目严谨，展示层已经做过值快照与 remember 收敛。主要问题不在"架构缺失"，而在以下几类：

1. **边缘情况**：离线时完整刷新会对六个资源逐一重试（最长可达数分钟）；登录成功回调在同步进行中会被静默丢弃；Android 12+ 设备迁移（D2D）仍会带走 Cookie 和数据库。
2. **测试与设备行为不一致**：设备上的 `org.json` 把 JSON `null` 转成字符串 `"null"`，而 JVM 测试依赖的 `org.json:json` 返回空串或抛异常，解析器测试无法覆盖这一差异。
3. **可维护性债务**：`TimetableState` 中按资源分叉的 `when` 重复四处、`message` 字段一身两用；`sync_state` 的魔法 ID（1/2/3/4/5/6、`id > 2`）散落在 Java DAO、Repository、测试中；为旧 androidTest 保留的兼容入口在主代码中累积。
4. **根组合函数过重**：`PlusScreen` 同时持有路由、筛选、日期、时钟、Snackbar、恢复分派和 11 个参数，是后续功能开发的主要摩擦点。

不建议做 Gradle 多模块拆分；建议做**包级按功能重组 + 少量状态模型收敛**。改进路线见第 3 节。

| 维度 | 评分 | 简述 |
| --- | --- | --- |
| 项目结构与分层 | 7.5 | 分层正确；命名（Timetable*）与包/目录不一致拖累可读性 |
| 正确性与数据可靠性 | 8 | 账户隔离、事务、取消、失败保留都到位；JSON null 处理有缺口 |
| 可维护性 / 可读性 | 6.5 | 重复分支、魔法 ID、兼容入口、超长多语句行 |
| 前端渲染性能 | 7.5 | 全部可跳过、列表有 key；Feed HTML 解析在主线程 |
| 核心业务性能 | 7 | 每次完整刷新 6 次 user/info + 12 次全量快照；规模内可接受 |
| 边缘稳定性 | 6.5 | 离线重试风暴、登录结果丢失、D2D 迁移 |
| 测试有效性 | 7 | 关键编排有聚焦测试；org.json 差异、DAO 假实现成本高 |

评分以"小规模单人维护的 Android 学生客户端"为参照，10 分表示边界清晰、可验证且有持续测量，仅用于排序优先级。

---

## 1. 整体项目评估

### 1(a) 项目结构

当前为单 `:app` 模块，源码按技术层分包：

```text
uk.ac.warwick.plus
├── auth/        AuthSession（CookieJar）、LoginActivity（WebView SSO）
├── config/      AppLabels / AppActions（显示文案）
├── data/        MyWarwickApi、CancellableRequests、3 个 Parser、TimetableRepository、
│                Room 实体/DAO（Java）、StudentContent（只读契约+快照）、BrowserLinks
├── ui/          ViewModel、State、全部页面、主题、背景、导航状态、展示函数
│   └── components/  基础组件（但 package 仍是 uk.ac.warwick.plus.ui）
├── MainActivity / PlusApplication（手工依赖装配）
```

优点：

- 依赖方向基本正确：`ui → data → auth`，组件不持有 ViewModel、不发请求。
- 手工 DI（`PlusApplication` 的 `lazy`）对当前规模足够，引入 Hilt/Koin 收益小于成本。
- Room schema 导出完整（1–5），迁移显式、无破坏性回退。
- 展示规则（日期、链接、服务目录、颜色角色）都有单一来源。

问题：

| 问题 | 影响 | 位置 |
| --- | --- | --- |
| 名称与职责不符：`TimetableRepository / TimetableStore / TimetableDao / TimetableDatabase / TimetableViewModel / TimetableState` 实际管理六类资源 | 新读者误判范围；`sync()` 实为"同步课表"，`syncCoursework()` 等却并列 | data/、ui/ |
| `ui/components/` 目录下文件的 package 仍为 `uk.ac.warwick.plus.ui` | IDE 移动/重构、按包可见性推理失效；Kotlin 官方约定目录与包一致 | ui/components/*.kt |
| `data` 依赖显示文案：`FeedKind.label` 取自 `AppLabels`，`SyncResource.label` 同理 | 数据层携带 UI 字符串，未来本地化或改名会波及数据枚举 | data/FeedParser.kt、ui/TimetableState.kt |
| 纯函数展示逻辑（`StudentDates`、`TimetableTime`、`ClassPresentation`、`CourseworkPresentation`）与 Composable 混在 `ui` 包 | 难以一眼区分"可 JVM 测试的规则"与"渲染代码" | ui/ |
| 为旧测试保留的兼容入口仍在主代码：`ScheduleContent(TimetableState, …, (EventEntity) -> Unit)` 会反向构造可变实体；`SyncNotice` 次构造器及派生属性；`filterCoursework(String)`；`PlusScreen` 的 `onFeedRefresh`/`onCourseworkLink` 回退链；`ResourceRecoveryRow(TimetableState, …)` 重载 | 主代码为测试形态让步，增加分支与理解成本 | 见第 2 节 |
| `ui-tooling-preview` 已引入但无任何 `@Preview` | 依赖无用；也失去了组件级快速视觉检查手段 | app/build.gradle.kts |

### 1(b) 是否需要模块化拆分

**结论：现阶段不做 Gradle 多模块，做包级重组。**

理由：

- 规模约 3,400 行、单人维护、单一应用，没有跨应用复用需求；当前增量构建为秒级（本次 lint+test 8 秒，均为 up-to-date）。
- 多模块的主要收益（并行编译、强制依赖边界、团队隔离）在此规模下都不显著，而成本（version catalog、convention plugin、`internal` 可见性调整、Room/Compose 配置复制）是确定的。
- Room 使用 Java 注解处理（无 KSP/kapt），拆出 `:core:data` 需要重新验证注解处理配置，属于额外风险。

建议的包级结构（只移动文件和改 package，不改逻辑；可分两三次提交完成）：

```text
uk.ac.warwick.plus
├── app/              MainActivity、PlusApplication（装配）
├── auth/             AuthSession、LoginActivity
├── data/
│   ├── remote/       MyWarwickApi、CancellableRequests、parser/、ApiProbe
│   ├── local/        Room 实体、DAO、Database、SyncSlots
│   └── StudentDataRepository.kt（原 TimetableRepository）
├── domain/           StudentContent 契约、StudentDates、TimetableTime、ClassPresentation、
│                     CourseworkPresentation、BrowserLinks（纯 Kotlin，无 Compose）
├── sync/             StudentDataViewModel、状态、SyncRetry、SyncOperation、RecoveryAction
└── ui/
    ├── theme/        Theme、ThemeBackground、Appearance*
    ├── components/   （package 与目录一致）
    ├── navigation/   NavigationState、AppNavigation、PlusScreen
    ├── home/  classes/  tasks/  me/  feed/
```

Room 数据库文件名 `timetable.db`、实体表名与 schema 保持不变；类名重命名可借 IDE 完成，Room 生成代码会随之更新。

未来触发拆分的条件（满足任意一项再评估）：

- 加入桌面小组件（Glance）、Wear OS 或 WorkManager 后台同步，需要在多个入口共享数据层；
- 清理后的全量构建明显变慢（例如超过 1–2 分钟）或增加协作者；
- 需要通过模块边界强制"UI 不能直接访问 DAO"。

届时目标图为 `:app → :feature:{home,classes,tasks,me} → :core:{ui,data,model}`，并先引入 `gradle/libs.versions.toml`。

### 1(c) 代码质量

#### 代码风格与可读性

- 大量单行多语句、超长参数行（不少超过 150 列），例如 `TimetableRepository` 的 `this.id = id; userCode = …; syncedAt = …`、`PlusScreen` 中 `HomeContent(...)` 的 10 个位置参数。`kotlin.code.style=official` 已声明但没有工具约束。
- 内联全限定名共 26 处、分布在 17 个文件（如 `kotlinx.coroutines.currentCoroutineContext()`、`uk.ac.warwick.plus.BuildConfig.DEBUG`、`androidx.compose.foundation.text.KeyboardOptions`），改为 import 即可。
- 未使用的 import：`WelcomeScreen.kt` 引入了 `semantics.onClick` 与 `data.*`。
- Lint 17 条警告中可直接处理的有：`DataExtractionRules`（见边缘情况）、7 条 `UseKtx`、2 条 `ModifierParameter`（`DeadlineRow` 的 modifier 不是首个可选参数；`AppPageHeader` 的 `titleModifier` 命名）。

建议：只对本次改动的文件做格式整理，不做全仓重排；如需工具化，引入 detekt 或 ktlint 并生成 baseline，只约束新代码。

#### 前端渲染性能

已有的好做法：编译器报告中所有 UI 函数均可跳过；`LazyColumn` 都有稳定 key；`collectAsStateWithLifecycle`；时钟绑定 `repeatOnLifecycle(STARTED)`；首页、课表、Tasks 的计算已按依赖 `remember`；背景位图在 `Dispatchers.Default` 生成并用 LRU 复用。

待改进（按预期收益排序）：

1. **Feed 的 HTML 解析在主线程组合期执行**。`FeedContent` 中 `remember(state.entries) { state.entries.associate { it.id to feedText(it) } }` 对最多 500 条消息调用 `Html.fromHtml`；首次打开 Messages 或刷新后都会在主线程整批执行，`FeedDetails` 中又会再解析一次。条目多且富文本较长时可能造成可见掉帧（未测）。
2. **根组合读取整份 `TimetableState`**。每次状态变化（开始资源、清除问题、账户验证、写入缓存、重试、完成资源、Feed loading 开关；完整刷新约 40 次发射）都会重组 `PlusScreen` 函数体：重建 `RecoveryState`、对 `events`/`entries` 做 `firstOrNull` 查找等。子组件可跳过，所以成本有限；但 `MoreContent(state)`、`DataStatusSheet(state)` 直接接收整份状态，在 Me 页刷新时会整体重组。
3. **30 秒时钟未对齐整点**。`now` 每 30 秒更新一次，但相位取决于启动时间，"In N min"、Now/Next 切换最多滞后 30 秒；把 delay 对齐到 :00/:30 可让课程开始（通常整分钟）准时切换，且不增加唤醒次数。
4. **底部面板关闭无退出动画**。`DetailClose`/外部关闭直接把 `detail` 置空，`ModalBottomSheet` 被立即移出组合，跳过了 hide 动画；Material 推荐先 `sheetState.hide()` 再移除。
5. 次要：`ThemeBackground` 只为获取宽高比使用 `BoxWithConstraints`（SubcomposeLayout），可用 `onSizeChanged` 替代；异构 `LazyColumn`（头部 + 列表项）未声明 `contentType`；列表状态类因含 `List/Map` 在报告中为 unstable，但 strong skipping 下以引用比较且已做 `reuseIfEqual` 引用复用，**不建议**为此引入 `kotlinx.collections.immutable` 或稳定性配置文件。

#### 核心业务处理性能

- **每次完整刷新**：6 次 `GET /user/info`（每个资源先验证账户）+ 12 次 `dao.snapshot()`（验证回调一次、写入后一次，每次读取六类全部数据并在 `withCache` 中逐条转换、深比较）。在 186 课程 + 500 消息的规模下 CPU 成本很小，主要代价是 6 次额外往返。按资源验证是有意设计——它能在刷新途中切换账户时阻止跨账户写入——**建议保留**，只优化回调：账户未变化时验证回调无需重读全量快照，只需更新身份字段。
- **冲突检测重复计算**：`conflictingEventIds` 为 O(n²)，`PlusScreen`、`HomeContent`、`ScheduleContent` 各自 `remember` 一份，课表变化时至少计算两次。n≈200 时约 2 万次比较，不是瓶颈；建议只在一处计算，不必换算法。
- `reuseIfEqual` 对最多 500 条快照做深比较，字符串逐字符比较，成本与文本总量线性相关，可接受。

#### 边缘情况稳定性

| # | 场景 | 现状 | 后果 |
| --- | --- | --- | --- |
| E1 | 离线或弱网时再次下拉刷新（本进程曾成功登录，`signedIn = true`） | 课表因 IOException 失败后仍继续尝试其余 5 个资源；每个资源 3 次尝试 + 2 秒/5 秒退避 | 立即失败的离线场景约 42 秒进度条；连接挂起（门户 Wi-Fi、弱信号）时每次尝试受 15 秒连接超时限制，总时长可达数分钟。期间 busy 锁住其他刷新 |
| E2 | 同步进行中打开登录（Me 账户卡片的 Sign in 按钮没有 busy 判断）并登录成功 | `MainActivity` 收到 `RESULT_OK` 后调用 `model.refresh()`，被 `startSync` 的 busy 守卫静默忽略 | 登录后数据不更新，需要用户手动下拉 |
| E3 | Android 12+ 设备间迁移 | 仅 `allowBackup="false"`；targetSdk 31+ 时它不阻止 D2D 迁移 | WebView Cookie（会话）和 Room 数据库可能被迁移到新设备；Lint `DataExtractionRules` 即指向此处 |
| E4 | 接口字段为 JSON `null` | 设备上 `optString`/`getString` 返回字符串 `"null"`。`TimetableParser` 的 module/location/locationUrl/title、`CourseworkParser` 的 href、`user()` 的 name 均受影响 | 显示 "null"；`href: null` 会被解析为 `https://my.warwick.ac.uk/null` 并通过链接白名单，出现指向 404 的按钮；问候语可能变成 "Good morning, null"。JVM 测试使用的 org.json 实现返回空串，测试无法发现 |
| E5 | 非课表资源出现登录失效 | `resourceFailed` 把 `message`（同时是课表 issue）改写为会话过期文案 | 课表刚成功也会被标记为有问题；`message` 同时承载"课表错误、全局缓存错误、退出失败"三种含义 |
| E6 | 课表失败且未登录时 | 只给 coursework 写入"需要登录"提示，Messages/Library/Modules/Account 不写 | 各页面的恢复提示不一致 |
| E7 | 登录页 | 每次 `onPageFinished` 都执行 `MyWarwickApi(AuthSession()).user()`，**新建 OkHttpClient**（独立连接池与线程） | 资源浪费，且绕过了应用级客户端配置 |
| E8 | 日期选择对话框打开时旋转屏幕 | `remember { DatePickerState(...) }` 不可保存 | 已输入/选择的日期丢失（对话框本身会因 `rememberSaveable` 重新出现） |
| E9 | 声明了 `ACCESS_NETWORK_STATE` | 代码中未使用 | 多余权限；或应借它实现 E1 的快速失败 |
| E10 | 冷启动 | `PlusApplication.onCreate` 调用 `CookieManager.getInstance().setAcceptCookie(true)` | 默认值本来就是 true；该调用会在主线程提前初始化 WebView provider，可能增加冷启动时间（未测，需测量后再决定是否移除或延后） |

#### 测试有效性

- 68 项 JVM 测试覆盖了重试策略、快照隔离、取消、账户切换、导航存档等复杂编排，与 AGENTS.md"精简、只测复杂逻辑"的约定一致。
- `RepositorySyncTest` 的假 DAO 必须覆盖 15 个 `error("Unused")` 方法，原因是 Repository 依赖整个抽象 DAO 类而不是窄接口。
- org.json 实现差异（E4）意味着解析器测试对 null、数字型 id 的判断与设备相反，测试通过不能代表设备行为。
- `android.text.Html` 在 JVM 测试中不可用，若把纯文本转换下沉到状态层，需要注入转换函数。

---

## 2. 核心代码细致分析

下面每个文件按"(a) 复用/简化、(b) 语法糖、(c) 框架特性"给出具体建议；只列有实际收益的项。

### 2.1 `ui/TimetableViewModel.kt`（153 行，同步编排核心）

**(a) 复用 / 简化**

- `startSync` 的 `finally` 中内联了约 20 行"根据失败生成 Notice"的逻辑，可提取为纯函数，便于阅读和测试：

```kotlin
internal fun TimetableState.completionNotice(id: Long, resource: SyncResource?, olderMessages: Boolean): SyncNotice? {
    val failures = (resource?.let(::listOf) ?: SyncResource.entries).mapNotNull { kind -> issue(kind)?.let { kind to it } }
    if (failures.isEmpty()) return null
    val action = when {
        olderMessages -> RecoveryAction.OlderMessages
        resource != null -> RecoveryAction.Refresh(resource)
        else -> failures.singleOrNull()?.let { RecoveryAction.Refresh(it.first) } ?: RecoveryAction.RefreshAll
    }
    val text = when {
        needsLogin -> "Sign in to update your information."
        failures.size > 1 -> "Couldn't update some information. Your saved data has been kept."
        else -> failures.single().second
    }
    return SyncNotice(id, text, action)
}
```

  调用方仅在返回非空时递增 `noticeId`，保持现有 ID 语义。

- **E1 离线快速失败**：让 `syncResource` 返回结果类型，完整刷新在网络类失败后停止后续资源：

```kotlin
private enum class ResourceOutcome { Updated, Failed, Offline }

// refresh() 内
for (resource in SyncResource.entries.drop(1)) {
    currentCoroutineContext().ensureActive()
    if (mutable.value.needsLogin || outcome == ResourceOutcome.Offline) break
    outcome = syncResource(resource)
}
```

  `Offline` 定义为"重试预算耗尽且最后一次为 IOException"。未执行的资源不补满进度（与现有"提前停止"规则一致），Notice 文案可改为"Couldn't connect. Your saved data has been kept."。可选：在 `withSyncRetry` 前用 `ConnectivityManager.activeNetwork` 的 `NET_CAPABILITY_VALIDATED` 判断，无网络时直接跳过重试——这也让已声明的 `ACCESS_NETWORK_STATE` 有了用途。

- **E2 登录结果不丢失**：增加一个待刷新标记。

```kotlin
private var refreshAfterCurrent = false
fun refreshAfterSignIn() { if (mutable.value.busy) refreshAfterCurrent = true else refresh() }
// startSync 的 finally 末尾（非 signingOut 分支）
if (refreshAfterCurrent) { refreshAfterCurrent = false; refresh() }
```

  `signOut` 时清除该标记。

**(b) 语法糖**

- `import kotlinx.coroutines.*` 与内联 `uk.ac.warwick.plus.BuildConfig.DEBUG` 并存；建议统一显式 import，并提取 `debugLog { }`（见 2.3），与 Repository 共用。
- `catch (error: Exception) { if (error is CancellationException) throw error; … }` 出现 3 次，可提取为 `inline fun <T> runCatchingNonCancellation(block: () -> T): Result<T>`。不建议直接用标准库 `runCatching`，因为它会吞掉 `CancellationException`。

**(c) 框架特性**

- `viewModelScope` + `MutableStateFlow.update` 用法正确，不建议改成 `stateIn`/`combine` 管线：同步状态是命令式状态机，Flow 组合反而更难读。
- 若未来改用 Room 可观察查询（见 2.3），`applyCache`/`restoreCache` 可以删除：缓存变化自动流入状态，失败时无需"恢复快照"。

### 2.2 `ui/TimetableState.kt`（124 行，状态模型）

**(a) 复用 / 简化**

`syncedAt`、`issue`、`withIssue`、`updating` 四个函数都按 `TIMETABLE / COURSEWORK / ACCOUNT / else -> feed!!` 分叉；`SyncResource` 的元数据也分散在 enum、`FeedKind`、DAO ID 三处。建议把"每个资源的同步状态"统一为一张表，内容数据保持原字段：

```kotlin
data class ResourceStatus(val lastSynced: Long? = null, val issue: String? = null,
    val loading: Boolean = false, val olderPageFailed: Boolean = false)

data class TimetableState(
    val events: List<EventContentItem> = emptyList(),
    val coursework: List<CourseworkContentItem> = emptyList(),
    val feeds: Map<FeedKind, FeedContent> = emptyMap(),   // 只含 entries/description/url/hasMore/webReadMillis
    val email: String = "",
    val status: Map<SyncResource, ResourceStatus> = emptyMap(),
    val globalMessage: String? = null,                       // 缓存打开失败、退出失败
    /* 其余身份/会话/进度字段不变 */
) {
    fun status(resource: SyncResource) = status[resource] ?: ResourceStatus()
}

internal fun TimetableState.withStatus(resource: SyncResource, transform: (ResourceStatus) -> ResourceStatus) =
    copy(status = status + (resource to transform(status(resource))))
```

收益：四个 `when` 和两处 `resource.feed!!` 消失；`message` 不再一身两用（修复 E5）；E6 的不一致可以用一次循环统一处理。这是本报告中改动面最大的一项，需要同步调整页面读取点和 `TimetableStateTest`/`FeedStateTest`，建议单独一批完成。

若暂不做整体重构，最小修复是：新增 `globalMessage` 字段，让 `resourceFailed` 的会话过期文案写入该字段，而不是写入课表的 `message`。

**(b) 语法糖**

- `withCache` 中 `owner` 的计算可用 `sequenceOf(...)` / `buildList` 一行表达，但当前写法已足够清楚，优先级低。
- `resource.label.lowercase(java.util.Locale.UK)` → import `java.util.Locale`。
- `SyncNotice` 的次构造器和 `resource/feed/olderMessages` 派生属性仅供旧 androidTest 使用；更新测试后删除。

**(c) 框架特性**

- 不建议给状态类加 `@Immutable`：`List` 只读不等于深不可变，现有 `reuseIfEqual` 已提供引用稳定性。

### 2.3 `data/TimetableRepository.kt` + `TimetableDao.java`

**(a) 复用 / 简化**

- **魔法 ID 集中**：`sync_state.id` 的 1/2/3/4/5/6 和 `id > 2` 分布在 DAO 注解、`authenticatedSync(1/2/6, …)`、`FeedKind(3/4/5)`、`replaceAccount` 校验和测试假实现中。Kotlin `const val` 在 Java 注解中可直接作为编译期常量拼接：

```kotlin
object SyncSlots {
    const val TIMETABLE = 1; const val COURSEWORK = 2
    const val MESSAGES = 3; const val LIBRARY = 4; const val MODULES = 5
    const val ACCOUNT = 6
}
```

```java
@Query("SELECT * FROM sync_state WHERE id = " + SyncSlots.TIMETABLE)
public abstract SyncEntity state();
```

  然后让 `SyncResource` 持有 `slot: Int`，`FeedKind.key` 引用同一常量。数值不变，schema 不变。另外 `deleteFeedStates()`（`id > 2`）实际也删除 Account（6），名称应改为 `deleteNonCoreStates()` 或在 `clear()` 中直接 `DELETE FROM sync_state`。

- **三个同构 sync 方法**只在 slot、读取函数、保存函数与日志文案上不同，日志可以并入 `authenticatedSync`：

```kotlin
override suspend fun sync(onAuthenticated: AuthCallback) =
    authenticatedSync(SyncSlots.TIMETABLE, onAuthenticated, read = { api.request { api.timetable(it) } },
        describe = { "events=${it.size}" }, save = dao::replace)
```

- **窄接口**：为 Repository 定义 `interface StudentCache { snapshot(); states(); feedEntries(); replace…(); clear() }`，由 DAO 实现。测试假实现从 20 个方法降到 8 个，也阻止 Repository 误用底层 `delete*/insert*`。
- 验证回调 `onAuthenticated(user, dao.snapshot())`：只在发生 `dao.clear()` 时才需要传新快照，账户未变时可以只传身份，减少一半全量读取。

**(b) 语法糖**

- `kotlinx.coroutines.currentCoroutineContext().ensureActive()` 写了 3 次，import 后即为 `currentCoroutineContext().ensureActive()`。
- 日志判断重复 3 次，可提取：

```kotlin
internal inline fun debugLog(message: () -> String) {
    if (BuildConfig.DEBUG) Log.i("MyWarwickPlus", message())
}
```

- `existing.mapTo(HashSet()) { it.id }` 已经是合适写法；`(existing + additions).sortedWith(...)` 可提取为 `FeedOrder` 比较器常量，与 `FeedParser` 共用（两处排序规则相同）。

**(c) 框架特性**

- **Room 可观察查询**：DAO 返回 `Flow<CachedTimetable>` 后，ViewModel 用 `collect` 驱动状态，`applyCache/restoreCache` 及"写入后再读一次快照"都可删除。前提是 DAO 改用 Kotlin + KSP（Java APT 不能生成 Flow 返回类型），需要先验证 KSP 与 AGP 9 内置 Kotlin 的兼容性。收益明确但牵涉构建链，放在 P3。
- 迁移集中注册：`Room.databaseBuilder(...).addMigrations(*TimetableDatabase.MIGRATIONS)`，并按版本顺序声明（当前源码中 4→5 写在 3→4 之前）。
- 可选：使用 Room 的 `MigrationTestHelper` 编写迁移测试（instrumentation，但不是 UI 测试），替代当前"虚构 SQLite 列定义核对"的手工方式。是否加入由你决定。

### 2.4 `data/MyWarwickApi.kt` + `CancellableRequests.kt`

**(a) 复用 / 简化**

- `getResponse` 的 `JsonResponse(code, body)` 只在 probe 中需要 code；可以返回 `Pair` 或保留，影响很小。
- `probe()` 绕过了 `request {}` 包装和 Repository 锁，仅限 Debug 构建，文档已说明，保持即可。

**(b) 语法糖 / 健壮性**

- `FeedKind.path()` 用字符串拼接 URL 并手动 `URLEncoder.encode`，改用 OkHttp 的 `HttpUrl.Builder` 更不容易出错：

```kotlin
fun FeedKind.url(before: String?): HttpUrl = MY_WARWICK.toHttpUrl().newBuilder().apply {
    if (this@url == FeedKind.MESSAGES) {
        addPathSegments("api/streams/notifications"); addQueryParameter("limit", "100")
        before?.let { addQueryParameter("before", it) }
    } else addPathSegments("api/tiles/content/$tile")
}.build()
```

- `user()` 的 `optString("name")` 受 E4 影响，统一改用 2.5 的 `stringOrEmpty`。

**(c) 框架特性**

- `CancellableRequests` 用 ThreadLocal + 手动 `Dispatchers.IO.dispatch` 把阻塞 `execute()` 绑定到协程取消，并在释放锁前 `NonCancellable` 等待 IO 结束。它覆盖了**读取响应体时的取消**和**迟到的 CookieJar 回调**，测试也覆盖了竞态。OkHttp 5 的 `okhttp-coroutines` 提供 `Call.executeAsync()`，但它只在等待响应头时响应取消，不覆盖读取 body 的阶段，也不提供"等待 IO 完成再放锁"的语义。因此**不建议为了"用框架特性"而替换**；只有在因其他原因升级 OkHttp 5 时，才评估把 `StudentApi` 改为 suspend 接口。
- 当前实现直接调用 `CoroutineDispatcher.dispatch`，这属于偏底层的用法。可读性更好的等价写法是 `withContext(Dispatchers.IO)` 配合 `coroutineContext.job.invokeOnCompletion { scope.cancel() }`。但这会改动已验证的取消时序，收益只是可读性，列为可选。

### 2.5 解析器（`TimetableParser`、`CourseworkParser`、`FeedParser`、`ProbeSummary`）

**(a) 复用 / 简化**

四处都重复"校验 `success` envelope → `data.<tile>.content.items` → 按索引遍历 → 唯一 ID 校验"。建议抽出共享工具（同时修复 E4）：

```kotlin
internal fun tileContent(body: String, tile: String): JSONObject {
    val root = JSONObject(body)
    if (!root.optBoolean("success")) throw InvalidResponseException()
    return root.getJSONObject("data").getJSONObject(tile).getJSONObject("content")
}

internal inline fun <T> JSONArray.mapObjects(transform: (index: Int, item: JSONObject) -> T): List<T> =
    List(length()) { transform(it, getJSONObject(it)) }

internal fun <T> List<T>.requireUniqueIds(id: (T) -> String): List<T> =
    also { require(mapTo(HashSet(), id).size == size) }

// 不依赖设备/JVM 两种 org.json 的强制转换差异
internal fun JSONObject.stringOrEmpty(key: String): String = opt(key) as? String ?: ""
internal fun JSONObject.requiredString(key: String): String =
    (opt(key) as? String)?.takeIf { it.isNotBlank() } ?: throw InvalidResponseException()
```

`TimetableParser` 中 `item.optJSONObject("parent")`、`optJSONObject("location")` 各被调用两次，先存局部变量即可。

注意 `requiredString` 会拒绝数字型 `id`：设备上 `getString` 会把数字转成字符串，Modules 已显式用 `get("id").toString()` 处理；Timetable/Coursework 改用前，应先对照 [protocol.md](protocol.md) 确认 id 的类型。

**(b) 语法糖**

- `(0 until items.length()).map { … }` → `List(items.length()) { … }`，少一次 IntRange 迭代器分配，读起来也更直接。
- `it.map { entry -> entry.id }.distinct().size == it.size` → `mapTo(HashSet())`，少一个中间列表。

**(c) 框架特性 / 测试**

- 有了 `stringOrEmpty`/`requiredString`，JVM 测试与设备行为一致。建议补 1 个聚焦测试：`null` 字段得到空串，`href: null` 不产生链接——这属于 AGENTS.md 允许的"复杂业务逻辑精简测试"。
- 不建议为此引入 kotlinx.serialization：手写解析配合 `require` 拒绝整批的策略，已经满足"坏响应不覆盖缓存"的要求；换成序列化库会改变错误语义，还要增加编译插件。

### 2.6 `ui/PlusScreen.kt`（227 行，根组合）

**(a) 复用 / 简化**

- **抽出导航状态持有者**（Compose 官方的 state holder 模式）：`tab / meRoute / detail / courseworkFilter / selectedDay / followToday / showDatePicker` 及其转换（选 Tab 时重置 Me、返回逻辑、账户变化清理、详情校验）集中到一个类：

```kotlin
@Stable
internal class PlusNavigator(tab: AppTab, meRoute: MeRoute, detail: DetailSelection, filter: CourseworkFilter) {
    var tab by mutableStateOf(tab); private set
    var meRoute by mutableStateOf(meRoute); private set
    var detail by mutableStateOf(detail)
    var courseworkFilter by mutableStateOf(filter)

    fun select(next: AppTab) { tab = next; if (next == AppTab.ME) { meRoute = MeRoute.Overview; detail = DetailSelection.None } }
    fun openTasks(filter: CourseworkFilter) { courseworkFilter = filter; tab = AppTab.TASKS }
    fun back() { if (tab == AppTab.ME && meRoute != MeRoute.Overview) meRoute = MeRoute.Overview else tab = AppTab.HOME }
    fun resetForAccountChange() { detail = DetailSelection.None; meRoute = MeRoute.Overview; courseworkFilter = CourseworkFilter.UPCOMING }

    companion object { val Saver: Saver<PlusNavigator, Any> = listSaver(/* 复用现有各 saver 的 key */) }
}

@Composable
internal fun rememberPlusNavigator() = rememberSaveable(saver = PlusNavigator.Saver) { PlusNavigator(/* 默认值 */) }
```

  这些转换函数都是纯状态操作，可以直接用现有 `NavigationStateTest` 风格的 JVM 测试覆盖。

- **合并回调参数**：`PlusScreen` 有 11 个参数，其中 9 个是动作；`MainActivity` 调用时混用位置参数和命名参数。可以合并为一个只含函数类型的数据类（函数类型对 Compose 是稳定的）：

```kotlin
internal data class PlusActions(
    val refresh: () -> Unit, val signIn: () -> Unit, val signOut: () -> Unit,
    val refreshResource: (SyncResource) -> Unit, val loadOlderMessages: () -> Unit,
    val consumeNotice: (Long) -> Unit, val openExternal: ((String) -> Unit)? = null,
    val probe: ((ProbeEndpoint) -> ProbeResult)? = null)
```

  合并后，`refreshResource` 中 `onResourceRefresh ?: onFeedRefresh ?: onRefresh` 的三级回退可以删除，同时删除 `onFeedRefresh`、`onCourseworkLink`。代价是需要更新引用这些参数的既有 androidTest 源码（`ChromeUiTest`、`ServicesUiTest`、`TimetableUiTest`）。这属于修改既有测试而非扩展，但仍需你确认。

- `recoverResource` 内部重新实现了 `RecoveryAction.OlderMessages.resolve` 的判断（`olderPageFailed && hasMore && entries.isNotEmpty()`），可直接复用 `resolve()`。
- `conflicts` 在此处以及 Home、Schedule 中各算一次；可以在这里只算一次并传下去，或放进 `HomePageState`/`SchedulePageState`。
- Me 页的分支（Overview/Settings/Feed/DeveloperTools）可拆成 `MeTab(...)` 组合函数，把 `PlusScreen` 的 `when` 嵌套从三层降到两层。

**(b) 语法糖**

- `val selectedId = (detail as? DetailSelection.Class)?.id` 等四个派生变量，配合底部三段 `firstOrNull` 查找，可以改为对 `detail` 的一次穷尽 `when`：

```kotlin
when (val selection = navigator.detail) {
    is DetailSelection.Class -> state.events.firstOrNull { it.id == selection.id }?.let { EventDetails(it, it.id in conflicts, close, actions.openExternal) }
    is DetailSelection.Task -> state.coursework.entries.firstOrNull { it.id == selection.id }?.let { CourseworkDetails(it, close, actions.openExternal) }
    is DetailSelection.Feed -> if (route == selection.kind) state.feed(selection.kind).entries.firstOrNull { it.id == selection.id }?.let { FeedDetails(selection.kind, it, actions.openExternal, close) }
    DetailSelection.None -> Unit
}
```

- 对齐的时钟（对应 1(c) 渲染性能第 3 条）：

```kotlin
while (true) {
    now = System.currentTimeMillis()
    delay(30_000 - now % 30_000)
}
```

**(c) 框架特性**

- 当前手写路由（4 个 Tab + Me 子页 + 底部面板）规模可控，**暂不建议**迁移 Navigation Compose 或 Navigation 3：它们解决的是深层栈与深链，而这里的主要复杂度是"账户切换清理、缓存延迟校验"，这类领域规则在任何导航库里都要自己写。先做 state holder；当出现第三层页面或深链需求时再评估 Navigation 3。
- `ScheduleDateDialog` 旋转后保留输入（修复 E8）：不能直接换成 `rememberDatePickerState`——它使用设备 locale，会破坏刻意固定的 `Locale.UK` dd/MM/yyyy 输入，而 `DatePickerStateImpl.Saver` 是私有的。可以保留现有构造器，配一个只保存日期和模式的自定义 Saver：

```kotlin
val picker = rememberSaveable(saver = listSaver(
    save = { listOf(it.selectedDateMillis, it.displayMode == DisplayMode.Input) },
    restore = { DatePickerState(Locale.UK, initialSelectedDateMillis = it[0] as Long?,
        initialDisplayMode = if (it[1] as Boolean) DisplayMode.Input else DisplayMode.Picker) })) {
    DatePickerState(Locale.UK, initialSelectedDateMillis = pickerMillis(selected), initialDisplayMode = DisplayMode.Input)
}
```

### 2.7 `ui/FeedsScreen.kt` + `FeedPresentation.kt` + `FeedDetails.kt`

**(a) 复用 / 简化**

- 纯文本只转换一次并移出主线程。最小改动方案是在页面内用 `produceState`，计算完成前回退显示原始非 HTML 文本：

```kotlin
val textById by produceState(emptyMap<String, String>(), state.entries) {
    value = withContext(Dispatchers.Default) { state.entries.associate { it.id to feedText(it) } }
}
fun plain(entry: FeedContentItem) = textById[entry.id] ?: if (entry.html) "" else entry.text
```

  更彻底的方案是在快照阶段（IO 线程）就生成 `plainText`，让列表和详情共用、`FeedDetails` 不再调用 `feedText`。由于 `android.text.Html` 在 JVM 测试中不可用，应把转换函数作为参数注入快照投影。

- 搜索过滤中每条创建 `listOf(...)` 再 `any`，改为直接用 `||` 连接，可减少最多 500 次短列表分配。
- 三处 `kind.label.lowercase()` 以及三种 Feed 的说明文案 `when` 可以收进 `FeedKind` 的展示扩展（放在 ui 层，避免数据层持有文案）。

**(b) 语法糖**

- `if (state.loading) … else if (busy) … else when (kind) {…}` 是一个三层嵌套的表达式，改为单个 `when { }` 更易读。

**(c) 框架特性**

- `items(filtered, key = { it.id }, contentType = { "feed-entry" })`；头部 `item(contentType = "header")`。

### 2.8 `ui/HomeScreen.kt`、`ScheduleScreen.kt`、`TimetableTime.kt`

**(a) 复用 / 简化**

- Now/Next 状态判定写了三遍（`HomeContent` 的 Today 列表、`ScheduleContent`、`NextClassCard`），条件略有差别。可以提取 `fun classStatus(event, now, nextId, day): String?` 放在 `TimetableTime.kt`，与 `SchedulePresentationTest` 一起维护。
- `ScheduleContent(TimetableState, …, (EventEntity) -> Unit)` 这个公开重载为旧测试把值快照反向构造成可变实体，与"展示层不接触可变实体"的目标相反。更新 `ScheduleUiTest` 一处调用后即可删除。
- `filterCoursework(String filter)` 重载只被 `FeedParserTest` 使用，测试改传枚举后删除。

**(b) 语法糖**

- `nextTimedClass`/`currentOrNextClass` 的 `filter { }.minWithOrNull(…)` 会产生中间列表，可改为 `asSequence().filter { }.minWithOrNull(…)`，或直接用 `minOfWithOrNull`。收益很小，顺手处理即可。
- `compareBy<EventContentItem> { it.startMillis }.thenBy { it.id }` 出现 4 次，提取为 `private val EventOrder` 常量。
- `CalendarPickerIcon` 等图标常量分散在页面文件中，可与 `NavigationIcons`、`MeIcons` 一起放进 `ui/icons/`。

**(c) 框架特性**

- `ScheduleClassRow` 在每行组合期调用 `scheduleTime(...)` 和 `atWarwick(...)`，成本很低；如果以后测量发现课表滚动掉帧，可以在 `scheduleDays` 阶段一次性预计算每行的展示值。目前不需要。

### 2.9 `auth/LoginActivity.kt` + `AuthSession.kt`

- **E7**：登录检查改为复用 `(application as PlusApplication).api.user()`，不再新建客户端。
- `Uri.parse(url)` → `url.toUri()`（Lint UseKtx，共 4 处）。
- `AuthSession.saveFromResponse` 对每个响应调用 `manager.flush()`（同步磁盘写入）。`clear()` 和登录完成时已经各 flush 一次，这里可以只在实际写入 Cookie 后 flush；属于低优先级。
- `loadForRequest` 使用的 `split(';')` + `split('=', limit = 2)` 能正确处理值中含 `=` 的情况，保持不变。
- `WebView` 设置（禁止文件访问、混合内容、多窗口，拒绝权限请求，SSL 错误直接取消，主框架域名白名单）都合理；Lint 的 `SetJavaScriptEnabled` 是 SSO 的必要条件，可以加 `@SuppressLint` 并写明理由。

### 2.10 `ui/ThemeBackground.kt`、`Theme.kt`

- 背景生成与缓存设计合理（Default 线程渲染、按量化尺寸做 LRU、不回收可能仍在显示的位图）。
- `BoxWithConstraints` → `Modifier.onSizeChanged { size = it }` + 普通 `Box`，避免 SubcomposeLayout；同时 `Bitmap.createBitmap` → KTX `createBitmap`（Lint）。
- `Theme.kt` 的 `palette(...)` 有 14 个位置参数且全是 `Long`，容易传错顺序；改为命名参数调用可在不改结构的前提下消除这个风险。

---

## 3. 改进方案与路线

按"风险 × 收益 / 工作量"排序。每批保持 schema 5、API 协议、重试预算和页面布局不变；验证方式遵循 AGENTS.md：复杂业务逻辑只补精简 JVM 测试，样式类改动只做构建、既有检查和真机手动反馈，不新增 UI Test。

### P0：真实风险，改动小（建议下一批完成）

| # | 改动 | 工作量 | 验证 |
| --- | --- | --- | --- |
| 1 | 新增 `res/xml/data_extraction_rules.xml`，排除 cloud-backup 与 device-transfer 的全部域，并在 manifest 引用（E3） | 0.5h | 构建 + Lint 警告消失 |
| 2 | 解析器统一使用 `stringOrEmpty`/`requiredString`，覆盖 `user()`（E4） | 2h | 1 个 JVM 测试：null 字段、null href |
| 3 | 完整刷新的离线快速失败（E1） | 2–3h | 扩展 `SyncRetryTest` 或新增 1 个编排测试：IOException 后不再请求后续资源 |
| 4 | 登录成功后的待刷新标记（E2）；Me 账户卡片的 Sign in 按钮在 busy 时禁用或保留标记 | 1h | 1 个 ViewModel 测试；真机手动确认 |
| 5 | LoginActivity 复用应用级 API 客户端（E7） | 0.5h | 构建 + 真机登录手测 |

### P1：可维护性收敛（1–2 批）

| # | 改动 | 工作量 | 说明 |
| --- | --- | --- | --- |
| 6 | `SyncSlots` 常量集中，替换 DAO/Repository/FeedKind/测试中的魔法 ID；为 `deleteFeedStates` 改名 | 1–2h | 数值不变；核对 Room 导出 schema 无 diff |
| 7 | `TimetableState` 引入 `ResourceStatus` 表和 `globalMessage`（E5、E6）；提取 `completionNotice` | 0.5–1 天 | 先做最小版（只拆 `globalMessage`）也可 |
| 8 | `PlusNavigator` 状态持有者 + `PlusActions`；删除回调回退链和兼容入口 | 0.5–1 天 | 需要你同意更新 3–4 个既有 androidTest 源码，使其能编译 |
| 9 | 解析器共享工具（`tileContent`/`mapObjects`/`requireUniqueIds`）、Repository 同构方法合并、`debugLog` | 2–3h | 现有解析/仓库测试应全部保持通过 |
| 10 | Feed 纯文本移出主线程，列表和详情共用 | 2–3h | 真机打开 500 条 Messages 手测；可选用 Perfetto 对比 |

### P2：结构与细节（随功能开发顺带）

| # | 改动 |
| --- | --- |
| 11 | 按 1(b) 的包级结构重组；`components/` 的 package 与目录对齐；`Timetable*` 系列重命名为 `StudentData*`（数据库文件名不变） |
| 12 | Repository 依赖窄接口 `StudentCache`，简化测试假实现 |
| 13 | 时钟对齐 30 秒边界；`DetailsSheet` 先 hide 再移除；日期选择状态可保存（E8，保留 `Locale.UK`）；Now/Next 判定函数合一；冲突集合只计算一次 |
| 14 | Lint 清理：UseKtx、ModifierParameter、`SetJavaScriptEnabled` 加注明理由的抑制；删除未用 import；处理 `ACCESS_NETWORK_STATE`（用于 P0-3 或删除） |
| 15 | 内联全限定名改为 import；只对改动文件做格式整理；可选引入 detekt 并生成 baseline |
| 16 | 为基础组件补少量 `@Preview`，或移除 `ui-tooling-preview` 依赖 |

### P3：先测量再决定

| # | 方向 | 前置条件 |
| --- | --- | --- |
| 17 | Baseline Profile（`androidx.baselineprofile` + profileinstaller） | 先用 Macrobenchmark 记录冷启动与 Classes/Messages 滚动基线 |
| 18 | 评估 `Application.onCreate` 中提前初始化 CookieManager 的成本（E10） | Perfetto 冷启动跟踪 |
| 19 | Room DAO 改 Kotlin + KSP，使用 `Flow` 可观察查询 | 先验证 KSP 与 AGP 9 内置 Kotlin 的兼容性 |
| 20 | 依赖升级：OkHttp 5、browser 1.10、targetSdk 37、Gradle 9.8；`org.gradle.configuration-cache=true` | 单独一批，与功能改动分开 |
| 21 | 文案迁移到 `strings.xml` | 只有在需要本地化或第三方翻译时才做；目前英文单语，`AppLabels` 已集中管理 |

### 明确建议保留、不要"顺手优化"的部分

- `CancellableRequests` 的取消与"等待 IO 完成后再放锁"语义，以及按资源验证账户（防止跨账户写入）。
- `withSyncRetry` 的重试预算与 `CancellationException` 传播；不要换成 `runCatching`。
- 整批 `require` 拒绝坏响应的解析策略；不要改成 `mapNotNull` 跳过坏条目。
- 值快照 + `reuseIfEqual` 的引用复用；不要给 Entity 或任意 `List` 加 `@Immutable`。
- 类型化导航 Saver（存 key 不存 ordinal）、`BrowserLinks` 的 URL 白名单、背景位图 LRU。

---

## 附：本次审阅依据

- 源码：`app/src/main` 全部 Kotlin/Java 文件；`app/build.gradle.kts`、`AndroidManifest.xml`、Gradle wrapper。
- 报告：`app/build/reports/compose/`（0.20.0 全量编译生成）、`app/build/reports/lint-results-debug.xml`。
- 测试：`:app:testDebugUnitTest` 68/68 通过（2026-10-07，本地 JDK 21）。
- 未执行：真机性能测量、UI Test、模拟器、学校接口请求。
- 参考：[Compose 性能最佳实践](https://developer.android.com/develop/ui/compose/performance/bestpractices)、[Compose 状态持有者](https://developer.android.com/topic/architecture/ui-layer/stateholders)、[Android 12 备份行为变更](https://developer.android.com/about/versions/12/behavior-changes-12#backup-restore)、[ModalBottomSheet](https://developer.android.com/develop/ui/compose/components/bottom-sheets)。
