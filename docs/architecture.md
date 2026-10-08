# 架构与组件规范

MyWarwick+ 使用单个 Android app module，保持 UI、同步编排、接口与缓存的职责边界。本文只维护当前约定；接口见 [API 接入与协议](api-progress.md)，检查与待办见 [开发与验证](development.md)。

## 代码职责

源码根目录为 `app/src/main/java/uk/ac/warwick/plus/`。

| 位置 | 职责 |
| --- | --- |
| `auth/` | 官方 WebView SSO、CookieManager、限定 origin 的 CookieJar |
| `data/MyWarwickApi`、Parser、`JsonFields` | 只读请求、响应限制、实际 JSON 类型与整批结构校验 |
| `data/TimetableRepository`、`StudentCache`、DAO | 账户隔离、串行读写、原子缓存与事务快照 |
| `ui/TimetableViewModel`、`SyncRetry` | 单份同步任务、登录后待刷新、重试、阶段进度和恢复通知 |
| `ui/TimetableState`、`data/StudentContent` | 持久数据的只读展示快照，独立资源错误与全局会话问题 |
| `ui/PlusNavigator`、`NavigationState`、`PlusActions` | 类型化路由、轻量状态保存、当前状态下的动作分派 |
| `ui/*Screen`、详情、Presentation | 页面组合、纯展示计算、局部搜索/筛选和详情 |
| `ui/components/` | 卡片、列表、输入、标题、操作与详情骨架 |
| `config/AppLabels`、`UiText`、`strings.xml` | 集中显示名称、参数化文案和数量资源 |

组件接收展示值与动作，不持有 ViewModel、不请求网络、不修改账户。暂不增加多模块、DI 框架或每页 ViewModel；共享抽象以实际复用为依据。

## 同步与缓存

```text
启动：Room 快照 → StateFlow → 页面 → 前台刷新
同步：ViewModel → SyncRetry → Repository → 验证账户 → GET/解析 → 写事务
观察：Room invalidation Flow → 串行锁内事务快照 → 值去重 → StateFlow
```

- 核心刷新依次执行 Timetable、Coursework、Messages、Library、Modules、Account；Home 下拉在此之后刷新 Buses、Print、Events。首页进入/回前台仅补加载到期服务（公交 1 分钟、打印 10 分钟、活动 30 分钟）；繁忙时合并一次待请求，退出/切页撤销待请求，不后台轮询。分母来自本次任务列表，各资源尝试前验证账户，账户变化清旧内容，失败保留缓存。
- Repository 在 IO 线程通过 Mutex 串行操作；网络请求在数据库事务外。DAO 观察全部缓存表的变更，在一次读事务内取得完整快照；revision 防止旧快照覆盖新状态。同步命令不另行返回整份缓存。
- 无活动网络直接结束。IOException、HTTP 408/5xx 最多额外重试两次，延迟 2 秒和 5 秒；认证、解析、429 等其他错误不重试。认证失效或传输异常耗尽预算停止后续资源，其他资源错误按各自结果处理。取消异常继续传播。
- busy guard 防止重复刷新并发。同步期间登录成功会合并为一次待刷新，退出丢弃待刷新。单项恢复只处理对应资源。
- 退出先取消并等待同步与观察，再清理本应用会话和缓存。`CancellableRequests` 将协程取消绑定到实际 OkHttp Call，并等待 IO 结束，避免迟到写入或 CookieJar 回调恢复旧账户。不得将其简化为只等待响应头的取消。
- Messages 分页与响应规则集中在 [API 文档](api-progress.md)。错误、加载、进度与重试状态不持久化。

## 展示与状态

持久 Entity 只用于解析和 Room；UI 使用字段为 `val` 的展示快照及不可修改列表。内容相等时复用原列表，进度变化不重新投影全部数据；不为任意集合或可变 Entity 标注 `@Immutable`。

Home/Classes 输入限于本页数据，顶部单独读取进度。首页日程/截止日期、Tasks 筛选排序、冲突集合按实际数据和时间依赖缓存。Feed HTML 在 Default 线程转换成纯文本，列表、搜索与详情共用按账户创建的约 2 MiB 文本缓存，不显示未转换的原始 HTML。

时钟仅在 Lifecycle.STARTED 运行，对齐每分钟的 :00/:30，回前台立即校准。日期统一使用 `StudentDates` 的 Europe/London 和 Locale.UK；精确截止时间用于判断过期，自然日用于计算 days。冲突检查保留 O(n²)，不在缺少热点证据时增加复杂算法。

Tab/Filter/Me 子页/详情使用类型化身份，Saver 保存稳定 key、日期和内容 ID，不保存实体或 enum.ordinal。无效状态回退默认页面；账户变化清理旧详情和选择。`AppLabels` 管理名称资源，显示文字不作为接口、路由或存储键。Snackbar 只消费一次，恢复点击读取最新状态和回调。

## 数据反馈

| 状态 | 反馈方式 |
| --- | --- |
| 读取缓存 | 简短文字，不伪装成网络进度；已有内容优先显示 |
| 请求进行中 | 顶部确定进度条替代旋转圈；核心六项、Home 九项、到期服务或单资源/分页，分母按任务列表。账户验证完成半项，保存或最终失败处理完成整项 |
| 等待与结束 | 重试时不虚增进度，分母不变；进度变化平滑过渡，结束短暂停留后隐藏，提前停止不补满 |
| 错误与空列表 | 失败保留缓存，一次短 Snackbar 和内容附近的 Retry/Sign in；区分未加载、真空列表和筛选为空，不常驻顶部错误卡片或保存时间页脚 |

进度表示处理阶段，不是字节、耗时或成功率。更早消息失败保留游标，失去有效游标时恢复动作回到最新页刷新。普通复制、设置和外链操作不触发业务刷新。

## 页面行为

| 页面 | 布局与信息规则 |
| --- | --- |
| Home | 短问候语、唯一强调的 Now/Next、四个网站入口、日程、最近三条未来 Deadlines、最近两条 Messages；普通内容使用单层分区容器 |
| Now/Next | 日期时间右对齐，当天只显示时间，明天显示 Tomorrow，之后显示日期；课程名与代码同行，地点与右下箭头同行，整卡进详情 |
| Today/Tomorrow | 展示当日正在进行或未结束课程；无剩余课程时预览 Tomorrow，无明日记录则省略。周末无剩余课程显示祝语，当天有已结束课程时加 Well done；未加载不判断无课 |
| Deadlines | 左侧数字/days，右侧原标题及 `d MMM`，下一年才加年份；当天未过期为 0 days。Recently passed 是近七天缓存记录，不代表提交状态 |
| Classes | 连续日期分组，跳过未来空日；选定起点空日保留简短说明。保留全日、跨日、冲突与详情；日期只在组标题出现，选日与滚动位置可恢复 |
| Tasks | 紧凑搜索、Upcoming/Past，未来升序/过去降序；两行标题、英文短日期和准确时间，完整说明及原站链接放详情 |
| Inbox | Tab 短标签 Inbox，页标题 Messages；搜索叠加来源筛选，来源选项取自缓存（如 Tabula/Comms），All 解除来源限制。来源/日期同行、两行标题和摘要、完整详情、显式更早分页 |
| Home 服务摘要 | Deadlines 后 Buses/Print 各占半宽，大字体/窄屏可上下排列。公交最多两条原始时刻/线路，卡内注明拉取时间或缓存；打印原始余额/说明，点卡打开官方账户。Messages 后最后放最多三条公开活动，完整时间/地点/说明放详情；不混入课程 |
| Me | 姓名/usercode/真实邮箱和显式复制；Settings/Library/Modules 紧凑列表、八个网站入口网格、隐私/许可、版本号和末尾 Sign out；长标签/大字体可撑高 |

Classes 的跨日列表按单日裁剪至 00:00–24:00，午夜结束不插空日，详情保留完整范围。Home 四个网站入口保持正方形，大字体/窄屏改两列；Me 大字体/窄屏也改两列。网站与详情来源仅在用户点击后打开浏览器。

## 组件标准

| 组件或配置 | 统一规则 |
| --- | --- |
| `AppCard` | Normal/Quiet/Featured/Selected 颜色角色；基础卡不隐式添加间距、边框或阴影。可点击卡要求动作语义，非交互分区无按钮语义 |
| `SectionCard`、`GroupedListItem` | 标题与内容共享一层容器；稳定 Lazy key 和列表状态归页面，组内用浅分隔线 |
| `MetricListRow`、`ScheduleClassRow`、`DeadlineRow` | 基础行负责左列、对齐和点击；业务变体负责时间、期限、冲突及文字含义 |
| `ActionTile` | Home Square 入口、Me 紧凑 Grid 入口共用目的地/点击语义；外部跳转有角标，图标 20/22dp，文字常规字重 |
| `LocationLabel` | 统一定位针和文字；装饰图标不请求 GPS，地点链接由详情打开 |
| `SearchField`、`DetailsSheet` | 输入/清除/键盘动作和可滚动详情骨架；搜索状态归页面，关闭先 hide 再移除 |
| `PageHeader`、`AppNavigation` | 页眉统一几何；Tab 图标和文字共同高亮、轮廓/实心两态，无按压灰底，安全区仅应用一次 |
| `UiTokens` | 圆角与间距的事实源；父组件拥有组件间距，避免标题和首行留白叠加 |
| `HomeTypography`、`InboxTypography` | Home 内容 bodySmall，数字/起始时间 11sp Bold，days/结束时间 Regular；Next 名称 16sp Medium。Inbox 标题 14sp、摘要 12sp、元信息 11sp；详情保留完整可读正文 |

搜索和快捷操作保留至少 48dp 点击区；Tab 至少 64dp，图标 22dp/布局槽 24dp，文字 12sp Regular。Home 的左信息列使用 Compact，Classes/Tasks 使用 Standard。尺寸细节以组件源码为准，正文随系统字缩放，不用固定高度裁切长内容。不为每个 Text 添加包装组件。

## 主题与共享工具

颜色主题以 `Theme.kt` 的固定 palette 为准，统一系统栏图标。页面使用颜色角色，不判断主题名；Featured 的 Lake 使用较浅强调色。普通卡以底色区分，不加边框；错误/冲突同时有文字或图标。

`ThemeBackground` 在 Default 线程生成三层非线性径向渐变，细纹理默认关闭，是静态材质而非实时模糊。位图长边上限 1024px、短边量化；进程内 LRU 最多两张，键为主题/纹理/尺寸，重组和滚动复用，重启重建，不缓存到磁盘。主题 ID 和纹理偏好由 `AppearancePreferences` 保存。

`StudentDates`、`ClassPresentation`、`CourseworkPresentation`、`FeedPresentation` 提供纯展示转换；`NetworkDates` 处理已确认的服务端时间格式，缺时区拒绝。`ServiceCatalog` 管理网站地址/标签/图标；`BrowserLinks` 统一 HTTPS、端口和域校验，`BrowserActions` 统一 Custom Tabs 与失败反馈。普通页面使用 Snackbar，详情内显示链接失败，取消异常继续传播。

## 维护方式

行为和组件规范只在本文维护，API 状态只在 API 表维护，验证结果只在开发文档维护，签名和发行约束只在发布文档维护。版本/依赖以 Gradle 为准，尺寸/颜色以源码为准。历史评估、评分、备选方案与逐版日志通过 Git 查询，不作为当前标准重复保存。
