# API 接入进度

更新：2026-10-06，版本 0.17.0。每次接入接口时同步更新此表；协议细节见 [protocol.md](protocol.md)。本轮完成第三批共享函数、浏览器跳转和服务配置整理，接口接入情况和原展示字段沿用。

0.17.0：UI 的英国时区与日期格式集中到 StudentDates，课程名称/代码和跨日时间共用 ClassPresentation；Coursework 与 Feed 共用 networkDate，旧 date/URL helper 从具体 parser 移到公共文件。域名限制仍区分 Warwick 作业/模块与普通 HTTPS 外部链接；浏览器只在显式点击后打开，不导出 cookie/CSRF。课程地点链接也共用校验和浏览器打开入口：支持相对地址、拒绝 userinfo/非 443 端口，打开失败在详情内显示并可重试。首页与 Me 八个外部服务的短名称、图标和地址由 ServiceCatalog 集中维护，Feed Library/Moodle 回退地址共用配置；这些仍是跳转入口，不是新增 API。完整同步、资源重试/进度、账户隔离、分页游标与 Room schema 5 沿用。56 项既有检查和构建通过；用户授权重试后物理手机覆盖安装成功，确认 0.17.0 / versionCode 25，冷启动成功（394ms），页面与跳转待手动验收。0.16.0 已获用户手动验收通过。

0.16.0 UI 整理：Home / Schedule / Tasks / Me 复用 AppCard 与统一颜色/形状标准；首页与 Tasks 共用 DeadlineRow 的 Home/List 变体，首页 Today 与 Schedule 的课程行共用 MetricListRow 的居中信息列；Tasks 连续列表共用首尾圆角和分隔线。首页快捷入口与 Me 网格共用 ActionTile，Tasks 与 Messages/Modules 共用紧凑搜索框；课程/作业/消息详情和按需状态工具共用底部面板骨架。Lake 强调颜色判断集中到主题，首页 Next 的右对齐日期/右下角箭头、首行紧凑留白、Home 0 days / Tasks Today-Passed 等规则保留。规范见 [component-standards.md](component-standards.md)。

此次没有新增学校接口、重新抓取登录态响应、修改认证/域名校验/请求顺序/重试/分页/缓存或 Room schema（仍为 5）；常用服务入口仍是浏览器跳转。41 项既有检查与构建通过，物理手机覆盖安装 0.16.0 并冷启动成功；这不补充 Library 非空结构等接口验证，也不替代页面视觉和操作手动验收。

| API / 方法 | 接入情况 | 如何使用与展示 | 验证 / 限制 |
| --- | --- | --- | --- |
| `GET /user/info` | 已接入 | 启动、刷新和登录回跳时检查账户、会话与 CSRF 信息；Me 展示账户和登录状态，0.6.1 移除检查时间 | 原生已验证；cookie/token 不写入日志或 Git |
| `GET /api/tiles/content/account` | 已接入，物理手机取得非空邮箱 | Me 账户卡片读取 `data.account.content.email`，显示并显式点击复制；邮箱空值不猜测地址，复制按钮禁用。独立缓存/同步时间、单资源重试及 Data status；全量进度从五项变为六项 | 2026-10-06 前端 account 组件确认 email/fullName/userId 字段；原生同步后只读 SQLite 检查 id=6 有 1 条状态、邮箱非空，未输出地址。保留现有 cookie/CSRF；显示与复制待用户手动核对，姓名/账号仍用 /user/info |
| `GET /api/tiles/content/timetable` | 已接入 | 首页 Now / Next 标签收进三行主卡片，Lake 使用淡蓝 primaryContainer / onPrimaryContainer，其他主题保留 primary / onPrimary，并保留整卡详情；Today 标题/课程数量、透明课程行及单行空状态共用一个普通背景，不嵌套卡片；Schedule 连续日期列表、日期跳转/返回今天、Now/Next、冲突、跨午夜提示、课程详情和地点链接；缓存与下拉刷新 | 此前真机 143 条；英国时区；Now / Next 仅选择有时间的课程，全日项在当天列表保留；失败保留整批旧缓存；大字体允许行高增加，完整标题在详情阅读；Schedule 位置在切页/刷新/状态恢复时保留 |
| `GET /api/tiles/content/coursework` | 已接入 | 首页 Deadlines 标题、All 操作、最近 3 个未来截止日期、空状态及 Recently passed 共用单层背景；左列数字 / days，右列原始标题 / d MMM（6 Oct），下一年及以后显示年份；All 进入 Upcoming，Recently passed 进入 Past；独立页紧凑圆角搜索、Upcoming / Past、连续列表，左列数字 / days 或 Today / Passed，右列两行以内标题和准确截止时间，不同年显示年份；整行详情及原站入口；独立缓存 | 此前浏览器及真机 3 条；接口没有独立课程名字段，使用原始 title；天数按英国自然日计算，首页当天未过期仍为 0 days，独立页显示 Today；日期使用 Locale.UK 英文月份和 Warwick 时区。支持 `+01` 和 `Z`。近期聚合，不推断已提交状态；原站浏览器可能需独立登录 |
| `GET /api/tiles/content/library` | 摘要页面已接入；非空结构待验证 | Me → Library；学校空列表说明、账户入口、独立缓存与重试；非空条目仅展示实际存在的 title/text/href 或通用原站提示 | 当前浏览器 200 / 0 条；此前原生 0 条。借阅、到期日、欠费等字段未确认，不推定业务含义 |
| `GET /api/tiles/content/eventsmerge` | 待验证 | 评估与课表合并事件，避免重复日程 | 前端候选；当前无业务展示 |
| `GET /api/tiles/content/modules` | 已接入 | Me → Modules；名称、代码、学年、搜索、详情、公告/评估条数及 Moodle 链接；独立缓存 | 0.4.0 浏览器 200 / 1 条；id 为数字，announcements/evaluations 当时为空。不等同完整选课清单；公告正文仍在原站查看 |
| `GET /api/tiles/content/calendar` | 待验证 | 评估个人日历与课表关系 | 前端候选；当前无业务展示 |
| `GET /api/tiles/content/mail` | 待验证 | 评估邮件摘要与原站入口 | 前端候选；不能视为完整邮件 API |
| `GET /api/tiles/content/todo` | 待验证 | 评估与 Coursework 的重叠及去重 | 前端候选；当前无业务展示 |
| `GET /api/streams/notifications?limit=100[&before={id}]` | 已接入 | Me → Messages 列表、搜索、详情、显式原站点击、手动更早分页；最近 100 条，每次刷新重置分页，最多缓存 500 条；0.5.0 移除首页消息按钮 | 0.4.0 浏览器 200 / 69 条；before 为更早记录游标，since 为更新记录游标。read 为网站已读时间；查看不写回已读，无可靠紧急等级；HTML 转纯文本 |
| `GET /api/timetable` + `X-Timetable-Token` | 未采用 | 暂使用已跑通的 cookie 聚合接口 | 仅 cookie 请求 401；专用 token 生命周期未验证 |
| `POST /api/timetable/register` | 未调用 | 暂无需要；已有登录可直接读取课表 | 前端/旧应用存在注册与 JS bridge 代码；未签发 token |

认证统一使用官方 WebView 登录得到的 MyWarwick session cookie，原生请求严格限定 MyWarwick HTTPS origin。当前业务接口都是只读 GET。原有五类数据与新增账户摘要独立缓存、记录同步时间；切换账户清除全部缓存，单接口失败不覆盖另一份缓存。Me 的本地退出取消/等待同步并清除应用 cookie、WebStorage、缓存与数据库，不调用学校账户写接口。

0.15.0 Me：姓名/账号同行，邮箱单独一行并有复制图标，复制后短暂变为勾选；仅用户点击才写入系统剪贴板，不读取剪贴板，不自动复制或把邮箱写入日志。移除正常 Signed in / 未检查会话提示；Sign out 简化并保留确认框和本地退出说明，失效登录/退出失败仍有恢复操作。App 的 Settings、Data status、Messages、Library、Modules、调试版 Developer tools 使用三列 16dp 圆角方格；Websites 的八个原有链接使用同样网格和外部跳转角标，缩短 Wellbeing / Safety / Help 文案；大字体或极窄空间改为两列，长标签最多两行并保留完整操作语义。没有账户阴影、嵌套卡片或常态外部登录说明。

Room schema 5 在 sync_state 增加默认空字符串的 email 字段，并注册 4→5 迁移；账户摘要使用独立 id=6，五类旧数据不被重建或清除。成功替换邮箱，失败保留缓存；跨账户清空、退出取消和重试预算沿用现有规则。全量刷新六项，账户摘要最后处理，单独刷新 Account 为一项；邮箱不根据姓名/usercode 拼接。正常网格跳转和复制不触发网络读取。

0.14.0 首页分区标题最小高度由 48dp 减至 32dp，移除 All 的 Material 按钮最小布局高度，保留 foundation clickable 的默认触摸范围扩展和操作语义；大字体允许自然增高。Lake 的 Now / Next 使用已有淡蓝 #D6E8F1 / 深色 #183743 配对，其他四个主题保持 0.13.0 配色；未增加边框或第二套明暗模式。

0.14.1 Next 第一行日期/时间右对齐，箭头移至最下方地点行右侧；整卡详情入口和三行信息保留。分区标题及 All 最小高度从 32dp 减至 28dp，标题采用 labelMedium / SemiBold 与既有次要文字色，避免继续降低对比度。Today / Deadlines 首行最小高度 56dp、上/下内边距 2/8dp，其余行仍 64dp / 10dp；空状态上/下内边距 2/10dp。Today 复用的课程时间列移除强制顶对齐，与课程内容块纵向居中，宽度同 Deadlines 为 48sp 转 dp、两行间距 2dp、水平居中；起始时间使用 primary。Schedule 共用课程行同步使用该时间列，但保留原 64dp 行高/10dp 上下内边距。认证、排序、英国时区、跨日/冲突状态、缓存、重试和外部链接均不变。

常用服务入口是外部跳转，不表示接入该服务 API。四个入口与 Me 共用 ServiceLink 配置；Next 下方 12dp 处一行四项，单项最小 64dp 高度，图标和短标签共用点击区域；主要区块间距仍为 24dp。只在用户点击后通过原安全链接处理器打开 Custom Tab，不自动加载，不导出应用 cookie；浏览器可能需要独立登录。

| 外部入口 | 地址 | 当前展示 / 验证 |
| --- | --- | --- |
| Moodle | `https://moodle.warwick.ac.uk/` | 首页学位帽图标 / Me；本轮 web 抓取工具报错，未验证实际登录页，保留既有地址 |
| Email | `https://warwick.ac.uk/mymail` | 首页信封图标 / Me；本轮公开读取跳到 Outlook，手机登录和邮箱操作未验证 |
| Tabula | `https://tabula.warwick.ac.uk/` | 首页记录图标 / Me；本轮公开页面显示未登录，不检查个人内容 |
| Library | `https://warwick.ac.uk/services/library/account` | 首页书籍图标 / Me 的 Library account；本轮公开读取为账户跳转页面。Me → Library 原生摘要仍是独立入口，非空借阅结构未补充验证 |

0.6.0 五类页面及详情共用所选固定 palette；More → Settings → Appearance 只保存主题 ID 和细纹理开关，不读取新的学校接口。背景按主题、纹理和尺寸档后台生成并有界缓存；切主题、设置页滑动不触发数据刷新。认证协议、业务缓存与 Room schema 4 不变。

0.6.1 移除首页、课表、Coursework、Messages、Modules、Library 的保存/更新时间页脚，以及 More 的保存时间列表与会话检查时间。后台仍记录独立同步时间，失败不覆盖缓存，错误与重新登录操作继续显示。Developer tools 只在 More；课表的新日期分组列表尚未接入，方案见 [schedule-design.md](schedule-design.md)。

0.7.0 已将选定的连续列表接入 Schedule，移除周范围、七日选择器和 Day/Week 多层导航。日期选择仅筛选已有缓存，不请求新的接口；普通刷新沿用原有请求。跨午夜课程分别出现在所覆盖的日期，列表按当天范围显示 00:00/24:00 与延续提示；全日项不显示 Now/Next。数据与认证协议、数据库 schema 4 不变。

0.8.0 五类数据同步只对失败资源自动恢复：网络 IOException、HTTP 408 / 5xx 在失败后间隔 2 秒、5 秒各重试一次，每个资源最多三次同步尝试；每次资源同步仍先调用 user 验证账户。认证过期、解析异常、其他 HTTP 错误（包括 429）不自动重试；退出取消等待中的重试，同步中重复下拉不并发。成功资源不重新下载。最终失败保留缓存，只发一次短暂 Snackbar；Retry 按原操作重试，更早消息沿用 before 游标，首次/正常刷新不自动分页。错误字段仍保留在状态中，但首页、列表和顶部不常驻展示失败文字；More 保留登录和退出恢复入口。

0.8.1 仅修正 Forest / Rosewood 配色并移除新增卡片边框，认证、接口、缓存与重试逻辑不变。按用户约定，不为本轮简单配色调整新增单元测试或编写 UI Test；以构建、既有配色检查和物理手机反馈验证。

0.4.0 构建时手机不可用；随后已覆盖安装到 vivo V2502A，用户反馈手动测试均正常、未发现异常。该反馈不补充新的接口数量证据，也不补全 Library 非空借阅结构或 Modules 公告正文的验证。

“截止日期提醒”目前指首页信息展示，尚未实现系统通知、后台同步或提交操作。空聚合列表不代表全部作业已提交或源系统没有数据。

0.10.0 为现有同步添加进度反馈：全量任务为课表、Coursework、Messages、Modules、Library 五项；More 中单服务刷新和更早消息分页为一项。每项账户验证回调完成后前进半步，读取/解析/缓存成功，或重试耗尽后的失败处理完成后结算整项；计量的是任务处理阶段，不是 HTTP 请求数量、字节数或剩余时间。重试等待不推进、重复验证不重复计数。部分失败也会结算已处理任务，进度结束用错误色及原有一次短暂提示区分；登录过期/提前停止保留低于 100% 的进度，不填满未执行任务。退出取消并移除进度。顶部原品牌小字位置显示任务名与序号/重试次数，不增加新行。原接口、调用顺序、账户验证、重试预算、分页游标和 Room 缓存协议不变；后续状态方案见 [data-feedback.md](data-feedback.md)。

0.11.0 统一未加载、真正为空及筛选为空的提示；Messages / Modules 提供 Clear search，Coursework 提供 Clear filters。失败或需要登录时才显示对应内容的轻量操作行，旧缓存可读，不恢复页脚日志或顶部错误卡片。课表与 Coursework 新增单数据源刷新入口，使用现有 GET + user 验证，不重复刷新其他资源或重报其旧错误；More → Data status 显示五类状态和独立更新时间，并支持按源刷新。登录过期用 More 小标记及对应 Sign in 恢复，不写学校账户状态。更早消息失败单独记录，Retry 沿用原缓存游标；下拉仍刷新最新消息，失去旧游标时回退普通刷新。首页近七天已过截止日期入口打开 Coursework 的 Past，筛选状态在页面切换保留、账户切换重置；不推断是否提交。认证、数据库 schema 4、已有缓存及重试预算不变。本轮没有新增接口或重跑真实学校接口验证。

0.9.0 首页仅调整布局与现有数据的呈现。Today 和 Schedule 共用课程行与未来课程选择规则；NOW 优先展示正在进行的有时间课程，若同时多课按开始时间、id 稳定选择，Today 保留所有课程和冲突提示。首页 Deadlines 仍显示最多三个未过期条目；准确截止时间、完整标题和原站链接保留在详情，过去条目仍在 Coursework 的 Past 筛选查看。不改变认证、接口、缓存、重试或数据库。没有新增测试或运行 UI Test。

0.12.0 Coursework 移除顶部两行说明、重复分类标题与独立大卡片；保留紧凑圆角搜索和 Upcoming / Past 二选一。连续列表左侧显示数字 / days、Today 或 Passed，右侧原始标题和英文月份的准确截止时间，不同年显示年份；每行点击详情，列表首尾圆角、中间轻分隔线。默认与首页 View all coursework 进入 Upcoming，Recently passed 进入 Past；旧 All / Next 7 days 保存状态按 Upcoming 显示。未加载、空分类及无搜索结果分别提示，Clear search 保持分类，不触发请求。完整说明放在详情；认证、接口、缓存、Room schema 4 和重试不变。

0.12.1 统一顶部最小内容高度，修正日历/Today/Back 控件导致的品牌文字垂直位置差异；底部导航采用房屋、日历、作业清单、人物的轮廓/实心两种状态，选中保留紧凑圆角底色。原 More 更名为 Me（我的），账户、Settings、Data status、Messages、Modules、Library 与登录恢复标记仍在原路由。只调整展示，不新增依赖、API、网络请求、认证状态或数据库版本；表格入口名称已更新，历史版本记录保留当时名称。

0.12.2 底部 Tab 移除按压 indication，图标和文字放进同一个圆角高亮容器，选中底色使用 160ms 颜色过渡；保留轮廓/实心图标、Tab 语义、点击区域和底栏安全区。仅改变导航样式，不新增接口、请求、认证、缓存或数据库变化。

0.12.3 取消底部 Tab 透明色与高亮色之间的颜色插值，选中直接切换；标签缩短为 Home / Classes / Tasks / Me，统一 12sp 常规字重，外/内横向留白分别降为 2dp。Classes 对应现有课表，Tasks 对应现有 Coursework；页面完整标题、只读 GET、认证、缓存、重试、数据库与进度逻辑均不变。

0.13.0 首页落实单层分区容器：Now / Next 标签和时间同排收进 primary / onPrimary 主卡片，Today 的标题/数量/课程行、Deadlines 的标题/All/作业行/空状态/近期过去入口分别共用一层普通背景，行本身透明。区块间距 24dp，列表内仅使用内收浅分隔线，常态空列表不增加嵌套容器；恢复入口和缓存仍按原状态出现。课程选择、Today 全部课程、最近三条未来截止日期、英国日期格式及原有跳转规则不变；本轮没有增加快捷服务、网络请求、认证、缓存或 Room 变更。
