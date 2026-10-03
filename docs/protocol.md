# MyWarwick 协议边界

最近补充验证：2026-10-04。

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

## 本轮验证：Coursework 与 Library

两者均为无额外查询参数的 GET，沿用 `/user/info` 验证、MyWarwick cookie 和 CSRF header 约定；桌面浏览器与手机原生请求均返回 HTTP 200、`success: true`。

| 接口 | 当前响应 | 可行性边界 |
| --- | --- | --- |
| `/api/tiles/content/coursework` | `data.coursework.content.{defaultText,items}`；当前 3 条；条目字段 `id,title,text,href,date` | 足以制作近期截止日期列表和外部详情入口，未确认完整历史、已提交状态或所有来源覆盖 |
| `/api/tiles/content/library` | `data.library.content.{href,defaultText,items}`；当前 0 条 | 只能确认空列表可访问，条目结构、借阅/欠费等功能尚无证据 |

Coursework 日期实际有 `2026-10-15T12:00:00.000+01` 和 `2026-10-30T12:00:00.000Z` 两种形式；未来解析器必须支持小时级 offset，并按 Instant 与英国时区处理。当前三条 `href` 指向 Tabula。此处日期仅用于格式证据，不保存作业标题、链接路径或个人标识。

默认说明提到未来一个月、Tabula/Moodle/my.wbs 聚合及在这些系统以外提交的作业不出现；这不是可自行推定的完整作业清单。条目没有独立结构化提交状态字段，不把文本或列表缺失当作“已提交”。

调试探查页只保存响应的字段名、数量和状态；不保存原始 JSON、cookie、token 或个人字段值。对空列表显示“尚无法确定条目结构”，不会把空响应当作功能验证完成。

## 后续候选

- `GET /api/tiles/content/coursework`：`content.items[]`，含 `id,title,text,href,date`。
- `GET /api/tiles/content/eventsmerge`：合并事件。
- `GET /api/tiles/content/library`、`modules`、`calendar`、`mail`、`todo`：聚合卡片内容，不能当作完整源系统 API。
- `GET /api/streams/notifications?limit=100&since={id}`：增量通知；标记已读是独立写操作。

## 移动端 token：暂不调用

当前前端 `registerForTimetable()` 仍调用 `POST /api/timetable/register`，将响应的 `token` 交给 native bridge。旧 Android 使用 `GET /api/timetable` + `X-Timetable-Token`，响应为 `data.items[]`。普通登录 cookie、不带专用 token 的专用接口请求实际返回 401。

未创建 token，未验证签发成功、有效期、撤销规则或后台可用性。不要把存在代码等同于完整链路有效。

## 来源

- https://github.com/UniversityofWarwick/mywarwick-android
- https://my.warwick.ac.uk/
- https://developer.android.com/reference/android/webkit/CookieManager
- https://developer.chrome.com/docs/android/custom-tabs
