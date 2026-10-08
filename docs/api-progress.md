# API 接入与协议

更新：2026-10-08。现有代码使用 MyWarwick 登录会话和只读接口，无新增 OAuth application。结构依据 2026-10-03 至 06 的浏览器、前端与手机观察；本次文档整理没有重新请求学校数据，不将历史样本当实时状态。界面规范见 [架构](architecture.md)，当前交付见 [开发与验证](development.md)。

## 接入与展示

| GET 路径 | 接入与用途 | 验证边界 |
| --- | --- | --- |
| `/user/info` | 每资源尝试前验证账户；Me 姓名/usercode | 原生已验证；CSRF 只用于请求，不记录认证值 |
| `/api/tiles/content/timetable` | Home Now/Next、Today/Tomorrow；Classes 连续列表、冲突/跨日、地点与详情 | 原生解析和缓存已验证；全日项保留在列表，不参加 Now/Next |
| `/api/tiles/content/coursework` | Home 最近三条未来期限/Recently passed；Tasks 搜索、Upcoming/Past、详情与原站 | 浏览器/原生已验证；近期聚合，不代表完整历史或提交状态，使用原始 title |
| `/api/streams/notifications?limit=100[&before={id}]` | Home 两条摘要；Inbox 搜索/详情/刷新最新页/显式加载更早记录 | 浏览器分页协议已验证，手机整体反馈正常；不写网站已读，不推断未读数或紧急等级 |
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

## 候选与未采用接口

| 接口 | 当前结论 |
| --- | --- |
| `/api/tiles/content/eventsmerge`、`calendar`、`mail`、`todo` | 仅前端候选，结构、覆盖及与已接入功能的重叠待验证 |
| `/api/streams/notifications?limit=100&since={id}` | 浏览器确认读较新记录；客户端仍刷新最新页，不做增量同步 |
| `GET /api/timetable` + `X-Timetable-Token` | 旧移动端路径；仅 cookie 请求历史返回 401，专用 token 当前有效性/生命周期未验证 |
| `POST /api/timetable/register` | 前端存在注册后交 native bridge 的逻辑；本项目未调用、未签发 token |
| 公交 | 尚未确认可接入接口，不展示虚构摘要 |

新增接口前先确认结构与产品用途，不批量探测。后台同步、系统通知、消息写回和作业提交未实现。

## 安全与证据

登录由学校网页处理，禁止文件访问、明文/混合内容，证书错误取消。外链只允许合法 HTTPS/default-or-443、无 userinfo；作业/模块额外限定 Warwick 域。浏览器跳转不附应用 cookie/header。本地退出、存储和备份规则见 [隐私说明](../PRIVACY.md)。

这些是学校内部接口，没有兼容性承诺；个人会话可用不代表第三方分发获得许可，学校允许范围尚未确认。服务风险与发行约束集中在 [发布流程](release.md)。

证据索引：[官方 Android 仓库](https://github.com/UniversityofWarwick/mywarwick-android)（参考提交 `cb105bf`）、[MyWarwick](https://my.warwick.ac.uk/)、[CookieManager](https://developer.android.com/reference/android/webkit/CookieManager)、[Custom Tabs](https://developer.chrome.com/docs/android/custom-tabs)。早期手机已解析课表、Coursework 和空 Library；浏览器确认 Messages/Modules 与分页；2026-10-06 手机确认 account 邮箱持久化。前端文件 hash 和样本数量不作为长期协议要求，不保存原始 HAR、cookie/token 或私人正文。

本轮交付只整理文档与后续打包附件目录，API 路径、认证和展示行为未变。
