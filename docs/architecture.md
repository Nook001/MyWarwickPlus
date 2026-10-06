# 架构与组件规范

更新：2026-10-06，适用代码 0.18.0。本文件合并已落地的组件、共享函数、同步、数据反馈、课表和主题方案，只记录当前标准。接口证据见 [protocol.md](protocol.md)，覆盖表见 [api-progress.md](api-progress.md)，验证状态见 [validation.md](validation.md)。

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
