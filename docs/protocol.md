# MyWarwick 协议边界

最近补充验证：2026-10-06（公开前端账户摘要结构与手机原生邮箱缓存）。

核对日期：2026-10-03。依据登录态浏览器 CDP 网络观察、部署前端资源，以及官方 Android 仓库 commit `cb105bf13a3eb579d268163ff54a8418e43b8767`。不保存原始 cookie/token/HAR。

## 当前使用

`GET https://my.warwick.ac.uk/user/info`

```text
refresh: false 或需刷新时的 URL 字符串
user: { authenticated, usercode, name, csrfHeader, csrfToken, ... }
links: { login, logout }
```

`GET https://my.warwick.ac.uk/api/tiles/content/timetable`

```text
{ success, status,
  data: { timetable: { content: { currentWeek, defaultText, items: [
    { id, title, start, end, isAllDay, type, academicWeek,
      location: { name, href }, parent: { shortName, fullName }, staff: [...] }
  ] } } }
}
```

普通读取请求使用 SSO cookie；当前网页封装同时携带 `Csrf-Token`。桌面浏览器中移除显式 CSRF header 的课表 GET 仍返回 200，但应用继续遵循网页 header 约定。没有验证每个 SSO cookie 单独的必要性。

实际响应的时间格式包含 offset 和区域时区，例如 `2026-10-05T10:00:00+01:00[Europe/London]`。解析使用 `ZonedDateTime`，同时支持仅有 offset 或 `Z` 的 ISO 时间，按 Instant 保存、Europe/London 显示。错误 envelope、重复 id、缺失字段、无 offset 或反向时间范围都会拒绝整批数据，保留缓存。

课表 `title` 经常仅为课程条目代码；`parent.fullName` 提供可读模块名称（当前 143 条中 137 条具备）。应用分别保存原始 `title`、`parent.shortName` 和 `parent.fullName`，优先展示完整名称、同时保留条目代码；缺少完整名称时回退原始标题。

真机已验证 WebView 登录后使用上述 cookie jar 进行原生请求，成功读取 143 条课表记录；不需要签发移动端专用 token 或申请新的 OAuth application。后续原生只读检查也成功读取 Coursework 和 Library。此结果仅覆盖当前账户、当前部署和这三个聚合读取接口，不代表后台长期会话或所有业务接口已验证。

## 0.15.0：只读账户摘要

`GET https://my.warwick.ac.uk/api/tiles/content/account`

2026-10-06 读取公开部署的 vendor、main-import 前端资源。tile 注册键为 account，通用读取函数使用 `/api/tiles/content/{tile}`；账户组件以 content.email 展示邮箱，propTypes 同时声明 fullName / userId 等字段。这是前端协议证据，尚不等于本轮已观察到真实账户响应。

```text
{ success, data: { account: { content: { email, fullName, userId, ... } } } }
```

原生继续先用 /user/info 验证账户，再携带现有 cookie 与 Csrf-Token 读取 account。只缓存 email；姓名和 usercode 仍由 /user/info 决定身份。不解析或持久化其他个人字段，成功 envelope / account.content 不存在或邮箱为非字符串类型时保留旧缓存；缺失/null/空字符串表示邮箱不可用，不拼接替代地址。Room 4→5 在 sync_state 新增 email 字段，id=6 对应账户摘要；切换账户和本地退出时一并清除。日志只记录 emailAvailable 布尔值，不记录邮箱、姓名、CSRF 或原始响应。

0.15.0 覆盖安装到物理手机后正常启动。通过手机应用自身的只读 SQLite 查询确认 schema=5、sync_state id=6 有 1 条状态且邮箱非空；旧版本没有该状态，证明本轮原生 account 读取/解析/缓存链路成功。只返回数量/非空标志，不取出或记录邮箱值；未检查复制结果、未提交表单或修改账户。

## 本轮验证：Coursework 与 Library

两者均为无额外查询参数的 GET，沿用 `/user/info` 验证、MyWarwick cookie 和 CSRF header 约定；桌面浏览器与手机原生请求均返回 HTTP 200、`success: true`。

| 接口 | 当前响应 | 可行性边界 |
| --- | --- | --- |
| `/api/tiles/content/coursework` | `data.coursework.content.{defaultText,items}`；当前 3 条；条目字段 `id,title,text,href,date` | 足以制作近期截止日期列表和外部详情入口，未确认完整历史、已提交状态或所有来源覆盖 |
| `/api/tiles/content/library` | `data.library.content.{href,defaultText,items}`；当前 0 条 | 只能确认空列表可访问，条目结构、借阅/欠费等功能尚无证据 |

Coursework 日期实际有 `2026-10-15T12:00:00.000+01` 和 `2026-10-30T12:00:00.000Z` 两种形式；0.3.0 解析器已支持小时级 offset，并按 Instant 与英国时区处理。当前三条 `href` 指向 Tabula。此处日期仅用于格式证据，文档不保存作业标题、链接路径或个人标识；设备 Room 缓存保存业务展示所需字段。

默认说明提到未来一个月、Tabula/Moodle/my.wbs 聚合及在这些系统以外提交的作业不出现；这不是可自行推定的完整作业清单。条目没有独立结构化提交状态字段，不把文本或列表缺失当作“已提交”。

调试探查页只保存响应的字段名、数量和状态；不保存原始 JSON、cookie、token 或个人字段值。对空列表显示“尚无法确定条目结构”，不会把空响应当作功能验证完成。

## 0.4.0：Messages / Modules / Library

2026-10-04 登录态浏览器以同源 GET 读取，三个接口均为 HTTP 200 / success true；不提交表单、变更账户或调用已读/静音接口。请求认证沿用当前 session cookie；原生实现继续通过 `/user/info` 取得 CSRF header 后调用相同路径。本轮新增原生链路未在真实手机上验证。

| 方法与路径 | 响应字段（只记录结构） | 页面用途 |
| --- | --- | --- |
| `GET /api/streams/notifications?limit=100`，更早页追加 `before={id}` | `data.{notifications:[{id,title,date,provider,providerDisplayName,type,typeDisplayName,notification,providerOverrideMuting,tags,icon:{colour,name,variant},text,textAsHtml,url}],read}` | 列表/搜索/详情/原站入口；网站已读时间之后显示未读提示，不写回已读 |
| `GET /api/tiles/content/modules` | `data.modules.content.{defaultText,items:[{id:number,fullName,moduleCode,academicYear,href,announcements,evaluations}]}`；前端还支持可选 lastUpdated | 模块名称、代码、学年及原 Moodle 页面，公告/评估只显示返回条数 |
| `GET /api/tiles/content/library` | `data.library.content.{defaultText,href,items}` | 当前空列表说明及 Library 账户入口；非空条目语义未知 |

当前通知 69 条，日期倒序，5 条含 HTML 正文。`read` 可解析为时间而非消息 id。以 limit=2 验证：`before=第二条id` 返回不包含边界的两条更早记录；`since=最新id` 返回 0 条，`since=最旧id` 返回 68 条。因此不能用 since 读取历史。部署前端加载更早消息也使用 before。客户端刷新请求最新 100 条，旧页通过显式点击加载，合并去重、最多 500 条；刷新成功重置旧分页，错误保留旧缓存。同账户缓存的最旧 id 才能作分页游标，账户切换时先清缓存，拒绝旧游标。

通知 `providerOverrideMuting` 表示覆盖静音，不据此推断紧急程度。没有可靠 priority 字段，不制作自动“重要”排序。HTML 只转为纯文本，不渲染可执行网页，不加载嵌入图片。通知链接实际可能指向学校以外的网站，因此允许有效 HTTPS/443、无 userinfo 的外部链接，用户明确点击后交给系统浏览器；不向外部网站传本应用 cookie。

Modules 当前 1 条，id 为数值，announcements/evaluations 均为空，href 指向 Moodle。缓存名称、模块代码、学年、链接和数组条数；未声称公告内容、完整选课或成绩已接入。Library 当前 0 条，学校说明无当前 checkout/hold，href 指向 Library 账户。对未来非空响应只显示实际已知 title/text/href；未知结构给通用原站入口，不猜测 dueDate、fine 或借阅状态。

证据来自当前部署资源 `/assets/js/0b7e644fedf7b0fc1482-bundle.js`、`/assets/js/e322fe41339e34bebbd1-main-import.js` 与登录态只读请求；资源 hash 和协议可能随部署变化。只记录数量、字段名及日期格式，不保存响应正文、个人通知内容、cookie 或 token。

## 登录、缓存与本地退出

保留已跑通的官方 WebView SSO → CookieManager → 限定 MyWarwick origin 的原生请求。Custom Tabs 属于系统浏览器会话，不能直接从它读取 cookie 来替代本应用 WebView 登录；本轮不引入 OAuth application 或移动端专用 token。

More 展示本次会话检查时间与五类缓存同步时间。退出前确认，只退出本应用：取消/等待当前同步，取消 HTTP 请求，清理应用 WebView cookie、WebStorage、WebView cache 和五类 Room 数据。学校 `links.logout` 不调用，系统浏览器账户不受影响；失败需重试，避免重新登录混入未清理的旧数据。Room 1→2→3→4 显式迁移保留现有登录与缓存；注销或换账户才清除业务数据。

接口仍是学校内部聚合协议，无版本或兼容性保证。非公开移动端复用的允许范围尚未取得学校书面确认；准备公共发行前需核对学校服务使用条款与许可。代码中存在某接口、当前账户能读取，不等于学校承诺第三方长期使用；只读、最小存储和避免高频请求有助于减小维护范围，但不能代替许可。

## 后续候选

- `GET /api/tiles/content/eventsmerge`：合并事件。
- `GET /api/tiles/content/calendar`、`mail`、`todo`：聚合卡片内容，不能当作完整源系统 API。
- `GET /api/streams/notifications?limit=100&since={id}`：增量新消息已验证，当前客户端使用整页刷新；标记已读是独立写操作。

## 移动端 token：暂不调用

当前前端 `registerForTimetable()` 仍调用 `POST /api/timetable/register`，将响应的 `token` 交给 native bridge。旧 Android 使用 `GET /api/timetable` + `X-Timetable-Token`，响应为 `data.items[]`。普通登录 cookie、不带专用 token 的专用接口请求实际返回 401。

未创建 token，未验证签发成功、有效期、撤销规则或后台可用性。不要把存在代码等同于完整链路有效。

## 来源

- https://github.com/UniversityofWarwick/mywarwick-android
- https://my.warwick.ac.uk/
- https://developer.android.com/reference/android/webkit/CookieManager
- https://developer.chrome.com/docs/android/custom-tabs
- https://warwick.ac.uk/services/idg/learning-resources/knowledge/how-to-access-your-email/ （More 的 Email 入口为学校列出的 `https://warwick.ac.uk/mymail`）
- https://developer.android.com/develop/ui/compose/components/pull-to-refresh （滚动页面使用 Material3 PullToRefreshBox）
