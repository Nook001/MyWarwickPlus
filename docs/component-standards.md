# UI 组件规范

更新：2026-10-06，0.16.0。适用于前两批 UI 整理：公共标准与页面迁移。现有五个固定颜色主题、信息密度和业务行为沿用；同步执行、缓存快照、网络日期解析和链接策略属于后续批次。

## 分层与职责

| 层级 | 组件 / 来源 | 负责什么 |
| --- | --- | --- |
| 基础容器 | `components/AppCard.kt` | 普通、安静、强调、选中四种颜色角色，形状与整卡点击；无隐式内边距、外边距、边框或阴影 |
| 分区与列表 | `SectionCard` / `GroupedListItem` / `ListDivider` | 单层标题与内容、连续列表首尾圆角、统一分隔线；LazyColumn 仍由页面持有稳定 key 和列表状态 |
| 行几何 | `MetricListRow` | 左侧信息列、居中、最小高度、内边距、右侧详情箭头；不接受 EventEntity 或 CourseworkEntity |
| 业务行 | `ScheduleClassRow` / `DeadlineRow` | 课程的全日、跨日、冲突及 Now/Next；截止日期的 Home/List 展示规则 |
| 操作入口 | `ActionTile` | Compact / Grid 两种布局、图标和标签、内部/外部目的地角标、完整点击区 |
| 输入 | `SearchField` | 搜索图标、单行输入、清除、键盘 Search；筛选规则与 query 状态属于页面 |
| 详情与工具面板 | `DetailsSheet` / `DetailHeader` / `DetailField` / `DetailClose` | 底部面板、列表滚动和留白、标题/字段/关闭；面板字段、来源链接和状态属于业务 |
| 页面框架 | `AppPageHeader` / `AppNavigation` / `SyncProgressBar` | 统一标题几何、Tab 样式与状态、实际任务进度；导航和同步状态由 PlusScreen 提供 |

公共组件位于 `ui/components/`，与页面继续使用 `uk.ac.warwick.plus.ui` 包，保持小项目内直接组合，不增加模块或依赖。首页、课程详情、作业详情、消息详情分别独立成文件；PlusScreen 负责路由、返回、账户变更后的选择清理与回调连接。

## 视觉标准

| 项目 | 标准 |
| --- | --- |
| 卡片圆角 | 分区/连续列表/紧凑入口 14dp；账户/网格 16dp；强调/设置/Tab 底色 18dp。原 Feed 内容卡保留 20dp 的命名兼容形状，搜索 24dp，均集中在 AppShapes |
| 颜色 | Normal 为 surfaceContainerLow / onSurface；Quiet 为 surfaceContainer / onSurface；Selected 为 primaryContainer / onPrimaryContainer；Featured 从主题的 emphasisColours 取得。Lake 沿用淡蓝强调色，其余主题沿用 primary / onPrimary；页面不判断主题名称 |
| 页面留白 | Home/Schedule/Me 16dp 左右、4dp 顶部、16dp 底部；Tasks/Feed/Appearance 保留 20dp。由 Spacing 提供两种已存在的标准，避免迁移时改变密度 |
| 区块间距 | 首页 24dp；Me 16dp；列表/Feed 12dp；网格 8dp。父组件拥有区块间距，基础 AppCard 不增加空间 |
| 分区标题 | 首页最小 28dp 高，labelMedium / SemiBold / onSurfaceVariant；Schedule 日期小标题保持 labelLarge，保留不同阅读用途 |
| 信息行 | 左列 48sp 转 dp，随字体缩放；水平/纵向居中，两行间距 2dp；右内容与左列间距 10dp，正文 bodyMedium / SemiBold，次要信息 bodySmall / onSurfaceVariant |
| 行高与内边距 | 普通行最小 64dp、左右 12dp、上下 10dp；分区首行 compactTop 最小 56dp、上 2dp、下 8dp。最小值允许长内容和大字体自然增高 |
| 箭头 | 普通行 16dp；Next 地点行右端 20dp；网格角标 12dp。仅装饰图标，动作语义在整个点击容器 |
| 空状态 | SectionEmptyRow 在分区内直接显示一行文字，禁止嵌套空状态卡；DataEmptyState 用于独立内容的未加载/空列表/搜索无匹配，按需提供恢复操作 |
| 搜索 | 最小 48dp，24dp 圆角，搜索图标 20dp、清除图标 18dp/48dp 操作区；外部间距由页面 Modifier 提供 |
| 网格 | Me 默认三列，字体缩放 >1.3 或宽度 <280dp 改两列；方格最小高度按列宽计算，内容可撑高。首页 Compact 保留四项入口，不扩大为方格 |
| Tab | Home / Classes / Tasks / Me，12sp 常规字重，选中图标与文字一起高亮；使用 selectable + Role.Tab、indication=null、直接最终色，保留底部安全区一次 |

## 内容与交互约定

- 基础组件只接受展示内容和动作，不读 ViewModel、不发网络请求、不修改账户/数据库。
- 普通卡片不自带点击动作；可点击卡片使用独立 AppCard 重载，传入 actionLabel，避免为纯分区添加错误的按钮语义。每个动作只有一个点击处理器。
- Left metric 是布局插槽，领域判断留在业务组件。DeadlineRow.Home 保留数字/days（含 0 days）、单行标题、d MMM、下一年才显示年份；List 保留 Today/Passed、两行标题、准确时间和不同年显示年份。
- DetailsSheet 提供 LazyListScope，业务以 item 组合内容，保留长内容滚动；类详情保留 24/16dp 留白，其余详情 24dp。Feed 的类别与标题仍是独立条目，保留原间距。Data status / API explorer 使用同一面板骨架，但保留自己的间距。
- query、筛选、列表位置和选中项仍在原来持有状态的层；这次抽离不改变 rememberSaveable 的输入、请求触发或页面返回行为。
- 不通过基础容器叠加第二层卡片；也不为每个文字节点建立仅包装 Text 的组件。领域差异用少数明确变体表达，新增变体先寻找真实的两个以上使用场景。
- 当前链接域名校验、浏览器会话和失败处理沿用。后续第三批统一链接打开函数时再处理地点链接的失败反馈，避免与布局抽离混在一起。

## 维护与验证

以后变更组件标准先修改公共实现与本表，再复核受影响页面；兼容样式需要注明用途。数据字段与展示覆盖继续记录在 api-progress.md。

本批使用构建、相关既有 JVM 检查与物理手机手动验收，不新增 UI Test、简单组件单元测试、模拟器或学校接口操作。手动重点是五个主题、Next 三行对齐、Today/Deadlines 左列、Classes 日期跳转、Tasks 搜索/分类、Me 网格/复制、详情和返回；首次开屏与缓存保持可读。
