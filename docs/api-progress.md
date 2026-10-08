# API 接入与协议

更新：2026-10-08，开发版本 0.26.0-beta.2。现有代码使用 MyWarwick 登录会话和只读接口，无新增 OAuth application。既有结构依据 2026-10-03 至 06 的观察，新服务另于 10 月 8 日只读验证；不将样本当持续实时状态。界面规范见 [架构](architecture.md)，当前交付见 [开发与验证](development.md)。

## 接入与展示

| GET 路径 | 接入与用途 | 验证边界 |
| --- | --- | --- |
| `/user/info` | 每资源尝试前验证账户；Me 姓名/usercode | 原生已验证；CSRF 只用于请求，不记录认证值 |
| `/api/tiles/content/timetable` | Home Now/Next 卡片含倒计时与当日时间轴（Now 标记、剩余课程开始时间、模块色）、Later today/Tomorrow；Classes 连续列表、冲突/跨日、地点与详情 | 原生解析和缓存已验证；全日项保留在列表，不参加 Now/Next |
| `/api/tiles/content/coursework` | Home 最近三条未来期限/Recently passed，1/3/7 天内期限按紧急程度着色文字；Tasks 搜索、Upcoming/Past、详情与原站 | 浏览器/原生已验证；近期聚合，不代表完整历史或提交状态，使用原始 title |
| `/api/streams/notifications?limit=100[&before={id}]` | Home 两条摘要；Inbox 搜索叠加缓存来源筛选、详情、刷新最新页、更早分页 | 10 月 8 日浏览器确认 provider/displayName 为 tabula/Tabula、comms/Comms；既有分页及手机反馈正常，新筛选待手测；不写网站已读 |
| `/api/tiles/content/modules` | Me → Modules：名称、代码、学年、公告/评估数量、Moodle 入口 | 浏览器结构已验证，手机整体反馈正常；非空公告/评估正文及完整选课覆盖待验证 |
| `/api/tiles/content/library` | Me → Library：学校摘要、账户入口；未知条目提示原站 | 仅空列表被浏览器/原生验证；非空借阅、到期日和欠费字段待验证 |
| `/api/tiles/content/account` | Me 本人真实邮箱和显式复制；独立缓存/刷新 | 原生解析和持久化已验证；不拼邮箱，姓名/账号仍以 user/info 为准 |

网站快捷入口只是浏览器跳转，不算 API 接入。六类业务数据独立缓存，账户变化清旧数据，失败保留缓存；同步/重试与界面反馈规则见架构文档。

## 认证与请求规则

官方 WebView SSO/MFA → CookieManager → OkHttp CookieJar → 原生 GET 已跑通。Cookie 仅发送给 `https://my.warwick.ac.uk:443`，WebView 没有 JS bridge。Custom Tabs 和桌面浏览器拥有独立会话，不能直接提取其 cookie 代替手机登录。

`/user/info` 读取 `user.{authenticated,usercode,name,csrfHeader,csrfToken}`；根节点 `refresh` 为 URL 字符串或 authenticated=false 时要求登录。资源请求携带 session cookie、`Accept: application/json` 和版本 User-Agent；仅在预期 `Csrf-Token` 名称且 token 非空时添加该头。历史浏览器课表 GET 不带显式 CSRF 也成功，未逐个确认 cookie 的必要性。

原生不自动跟随重定向；3xx、401、403、非 JSON Content-Type 归为需登录。响应上限 4 MiB，连接/读取/整次超时为 15/25/35 秒。错误 envelope、必需字段缺失、重复 ID 或不合法时间按解析器规则拒绝整批，不跳过坏条目掩盖异常。

## 响应结构

聚合接口无额外查询参数，envelope 为 `{success,status?,data:{<tile>:{content:{...}}}}`；通知接口直接返回 `data.notifications`。下表仅列使用或理解协议所需的字段。

| 路径末段 | 主要响应字段 |
| --- | --- |
| `timetable` | content: currentWeek/defaultText/items；item: id/title/start/end/isAllDay/type/academicWeek/location{name,href}/parent{shortName,fullName}/staff |
| `coursework` | content: defaultText/items；item: id/title/text/href/date |
| `library` | content: defaultText/href/items；非空 item 结构未确认 |
| `modules` | item: 数字 id/fullName/moduleCode/academicYear/href/announcements/evaluations |
| `account` | content: email/fullName/userId 等；客户端只取 email |
| `notifications` | data: notifications/read；item: id/title/date/provider/providerDisplayName/type/text/textAsHtml/url 等 |

课表保留原始代码与 `parent.fullName`，不猜测缺失课程名。时间保存为 Instant，英国时区展示；已确认区域时区后缀、offset/Z，以及 Coursework 的 `+01` 小时级 offset。无时区拒绝，准确 deadline 不被截断。

Messages 的 `before` 是排除边界的更早 ID；URL 编码后传入。最新页 limit=100，刷新重置旧分页；更早页按日期/ID 稳定排序、去重，最多缓存 500 条，非空且无新增 ID 的页拒绝。`read` 是网站已读时间，`providerOverrideMuting` 不是紧急等级。HTML 转纯文本，不执行脚本、不加载嵌入图片。

Modules 仅存公告/评估数组数量；Library 未知字段不推断借阅状态。邮箱缺失/null/空串表示不可用，非字符串拒绝并保留缓存。

## 已确认且待原生接入

2026-10-08，在现有浏览器登录态中读取 `/api/tiles` 的官方卡片目录及以下 GET，均返回 200/有效 content；未修改卡片偏好、未申请 token、未输出打印余额或私人消息。浏览器可读不等于 Android 页面和缓存已接入。

| GET 路径 | 已确认结构 | 首轮用途与边界 |
| --- | --- | --- |
| `/api/tiles/content/bus` | content.items；item: id/callout/text，全为字符串；本次 18 条 | Home 半宽公交摘要，按原始顺序显示少量时刻和线路文字；没有结构化实时标志，不标注“实时”，旧缓存要注明过期，不先猜路线/站点字段 |
| `/api/tiles/content/print` | content.href/items；item: id/callout/text；本次 1 条，原站 printercredits.warwick.ac.uk | Home 半宽打印余额摘要和官方入口；不充值、不提交打印任务，不解析成未确认的账户/配额结构 |
| `/api/tiles/content/uni-events` | content.defaultText/currentWeek/items；item: id/source/start/end/isAllDay/title/location[]/href/extraInfo/academicWeek；本次 100 条，location item 已见 name，时间 offset 同 Coursework | Home 最底部少量未来/进行中的公开活动，详情及报名原站；source 已见 insite/student-events/library。与个人汇总日程 eventsmerge 分开，不直接影响课程 Now/Next |

### 原生接入实施计划

保持现有个人日程和 Deadlines 的优先级，其后增加一行 Buses/Print 两个半宽摘要卡，Messages 之后在首页最底部展示 Events。不增加底部 Tab 或独立服务页。以下均为待实施计划，不代表已接入。

| 批次 | 交付内容 | 验收重点 |
| --- | --- | --- |
| 1：缓存/刷新基础 + 两个半宽卡 | Buses 最多两条时刻与线路文字，Print 原始余额与说明、官方网页入口；普通单层卡、紧凑标题，窄屏/大字体可改上下排列 | 左右各占可用宽度的一半；公交不猜发车日期或实时性，余额不猜币种/配额；未加载、真空列表、缓存和失败明确区分 |
| 2：首页底部 Events | 最多三条未来/正在进行活动，日期时间、标题、可选地点；长说明放复用详情，报名跳学校给出的活动链接；初版不加搜索或来源筛选 | 全日/跨日、英国时区、重复 ID 与坏日期；空集合简短反馈，不虚构分页或完整历史，不混入课程 Now/Next |
| 3：手机验收与收尾 | 校验真实公交、余额、活动及覆盖升级；按反馈控制高度和文字截断，更新实际 API 状态 | 半宽长线路与大字体；失效会话、断网旧数据、账户切换；原站跳转与返回，首页刷新完成时间 |

**数据与同步：** 复用现有认证、有限重试、请求取消、账户隔离和事务缓存。新增资源使用稳定 slot/key；不能继续用 `SyncResource.entries` 隐式决定刷新范围。首页加载先完成核心六项，再刷新到期的服务摘要；Home 下拉包含三个新资源，其余页面只沿用原刷新范围。任务分母由本次实际执行列表决定；繁忙期间合并待执行请求，切页与重组不重复启动。新增内容进入 Room 快照和变更观察，退出/换账户同时清理。Buses/Print 可共用已确认的 id/callout/text 结构解析，业务展示保持独立；Events 使用独立活动模型，复用日期、地点和详情组件，不塞入课程或通知模型。缓存变更须提供从公开 schema 5 开始的非破坏迁移；后续批次递增并保留迁移链。

**新鲜度：** 首次首页加载三个资源；再次进入首页，公交缓存超过 1 分钟才尝试刷新，Print/Events 分别以 10/30 分钟为初始间隔，不后台轮询。Home 下拉始终刷新。间隔是客户端策略，不是后端有效期保证。公交读取旧缓存、离线或刷新失败时，在卡片内显示简短的更新时间及非实时提示，不恢复各页面日志页脚。服务只有 HH:mm，首版保留原始顺序，不据此猜测日期、过滤已发车或计算倒计时。缺少日期字段及余额/extraInfo 的实际格式在实现前做针对性的只读复核。

**检查：** 构建、Lint 与相关既有检查；只为时间边界、缓存隔离、迁移等复杂逻辑补必要的精简检查，不新增 UI Test。物理手机确认三类真实数据与 schema 升级保留旧缓存；构建或空响应不能代替非空内容验收。每批更新本表的“已确认且待接入”状态，不把浏览器证据改写成原生验证。

## 候选与未采用接口

| 接口 | 当前结论 |
| --- | --- |
| `/api/tiles/content/eventsmerge`、`calendar`、`mail`、`todo` | 浏览器主页观察到 200，完整结构、覆盖与产品用途未核对；eventsmerge 是 All my events，不是公共 Events |
| `/api/streams/notifications?limit=100&since={id}` | 浏览器确认读较新记录；客户端仍刷新最新页，不做增量同步 |
| `GET /api/timetable` + `X-Timetable-Token` | 旧移动端路径；仅 cookie 请求历史返回 401，专用 token 当前有效性/生命周期未验证 |
| `POST /api/timetable/register` | 前端存在注册后交 native bridge 的逻辑；本项目未调用、未签发 token |

新增接口前先确认结构与产品用途，不批量探测。后台同步、系统通知、消息写回和作业提交未实现。

## 安全与证据

登录由学校网页处理，禁止文件访问、明文/混合内容，证书错误取消。外链只允许合法 HTTPS/default-or-443、无 userinfo；作业/模块额外限定 Warwick 域。浏览器跳转不附应用 cookie/header。本地退出、存储和备份规则见 [隐私说明](../PRIVACY.md)。

这些是学校内部接口，没有兼容性承诺；个人会话可用不代表第三方分发获得许可，学校允许范围尚未确认。服务风险与发行约束集中在 [发布流程](release.md)。

证据索引：[官方 Android 仓库](https://github.com/UniversityofWarwick/mywarwick-android)（参考提交 `cb105bf`）、[MyWarwick](https://my.warwick.ac.uk/)、[CookieManager](https://developer.android.com/reference/android/webkit/CookieManager)、[Custom Tabs](https://developer.chrome.com/docs/android/custom-tabs)。早期手机已解析课表、Coursework 和空 Library；浏览器确认 Messages/Modules 与分页；2026-10-06 手机确认 account 邮箱持久化。前端文件 hash 和样本数量不作为长期协议要求，不保存原始 HAR、cookie/token 或私人正文。

本轮增加 Inbox 本地来源筛选，Home 正方形入口和更紧凑的 Me 网格；随后 Home 增加当日时间轴、模块色和期限紧急色，移除 Sand 主题（已保存的 sand 回退 Forest）；业务请求和 schema 5 未变。三个新服务目前只完成浏览器协议验证，尚未作为原生功能展示。
