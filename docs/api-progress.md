# API 接入与协议

更新：2026-10-08，开发版本 0.27.0-beta.1。现有代码使用 MyWarwick 登录会话和只读接口，无新增 OAuth application。既有结构依据 2026-10-03 至 06 的观察，新服务另于 10 月 8 日只读验证；不将样本当持续实时状态。界面规范见 [架构](architecture.md)，当前交付见 [开发与验证](development.md)。

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

网站快捷入口只是浏览器跳转，不算 API 接入。核心六类业务与三类首页服务独立缓存，账户变化清旧数据，失败保留缓存；同步/重试与界面反馈规则见架构文档。

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

## 首页服务摘要

0.27.0-beta.1 已实现以下原生 GET、整批解析和 Room 缓存；2026-10-08 浏览器只读复核整批字段及唯一 ID 有效。本次样本为公交 11 条、打印 1 条、活动 100 条，不保存私人余额或原始响应。物理手机已在原生首页显示非空公交和打印余额；Events 详情及原站跳转仍待手测。安装与验证边界见开发文档。

| GET 路径 | 结构与原生展示 | 边界 |
| --- | --- | --- |
| `/api/tiles/content/bus` | items: id/callout/text 字符串；Home 半宽卡，最多两条时刻和线路/方向/站点；点开完整原文 | 顺序沿用原站，摘要仅按已见 from/to 文本格式收紧，格式不匹配回退原文；只有 HH:mm，不推断日期、实时预测、已发车或倒计时；注明拉取时间，过期/失败标为缓存 |
| `/api/tiles/content/print` | content.href/items；item: id/callout/text；Home 半宽余额/说明卡，点卡打开接口给出的官方账户入口 | 保留原始余额字符串，不推断币种、可打印页数或配额，不充值、不打印 |
| `/api/tiles/content/uni-events` | items: id/source/title/extraInfo/href/location[]{name}/start/end/isAllDay；Home 最底部最多三条未来/进行中活动，详情与原站链接 | 时间 offset 同 Coursework，全日结束边界按排他处理；未知来源保留；不搜索、不分页，不混入课程 Now/Next；HTML 仅转纯文本 |

核心六项仍优先同步。Home 下拉刷新九项，其他页面维持原刷新范围；进入/回前台 Home 补加载到期服务，繁忙时合并一次待请求，切页取消待请求。不后台轮询；公交/打印/活动初始间隔为 1/10/30 分钟，这是客户端节流策略，不代表后端有效期。失败尝试也节流，手动下拉/单项恢复可立即再试。进度分母依据实际任务列表。

三个资源沿用账户验证、取消、有限重试和失败保留缓存；退出/换账户清理所有新增内容。schema 6 通过非破坏的 5→6 迁移新增摘要、活动和元数据表，保留公开版本原缓存。显示名称仍由 AppLabels/strings 管理，新增资源不借用通知或课程身份。

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

本轮保留用户已有 UI 调整，新增首页 Buses/Print/Events 原生摘要、按需刷新和 schema 6 缓存；既有六类接口未更改。
