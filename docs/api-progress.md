# API 接入进度

更新：2026-10-04，版本 0.3.0。每次接入接口时同步更新此表；协议细节见 [protocol.md](protocol.md)。

| API / 方法 | 接入情况 | 如何使用与展示 | 验证 / 限制 |
| --- | --- | --- | --- |
| `GET /user/info` | 已接入 | 启动、刷新和登录回跳时检查账户、会话与 CSRF 信息 | 原生已验证；cookie/token 不写入日志或 Git |
| `GET /api/tiles/content/timetable` | 已接入 | 首页下一节课、今日安排；日/周课表、课程详情、地点链接；Room 离线缓存 | 真机 143 条；英国时区；失败保留整批旧缓存 |
| `GET /api/tiles/content/coursework` | 已接入 | 首页最近 3 个未来截止日期；Coursework 全部返回条目、过去截止日期、详情、原站入口；独立缓存与同步提示 | 浏览器及真机 3 条，浏览器 200；支持 `+01` 和 `Z`。近期聚合，不推断已提交状态；原站浏览器可能需独立登录 |
| `GET /api/tiles/content/library` | 探查完成，未做业务页面 | Developer tools 显示状态、数量与字段摘要 | 当前 200 / 0 条；非空条目结构仍未知 |
| `GET /api/tiles/content/eventsmerge` | 待验证 | 评估与课表合并事件，避免重复日程 | 前端候选；当前无业务展示 |
| `GET /api/tiles/content/modules` | 待验证 | 评估模块列表与课程相关链接 | 前端候选；当前无业务展示 |
| `GET /api/tiles/content/calendar` | 待验证 | 评估个人日历与课表关系 | 前端候选；当前无业务展示 |
| `GET /api/tiles/content/mail` | 待验证 | 评估邮件摘要与原站入口 | 前端候选；不能视为完整邮件 API |
| `GET /api/tiles/content/todo` | 待验证 | 评估与 Coursework 的重叠及去重 | 前端候选；当前无业务展示 |
| `GET /api/streams/notifications?limit=100&since={id}` | 待验证 | 评估重要通知列表 | 仅计划只读验证；标记已读属于独立写操作 |
| `GET /api/timetable` + `X-Timetable-Token` | 未采用 | 暂使用已跑通的 cookie 聚合接口 | 仅 cookie 请求 401；专用 token 生命周期未验证 |
| `POST /api/timetable/register` | 未调用 | 暂无需要；已有登录可直接读取课表 | 前端/旧应用存在注册与 JS bridge 代码；未签发 token |

认证统一使用官方 WebView 登录得到的 MyWarwick session cookie，原生请求严格限定 MyWarwick HTTPS origin。当前业务接口都是只读 GET。切换账户清除课表与 Coursework 两份缓存；单个接口失败不覆盖另一份缓存。

“截止日期提醒”目前指首页信息展示，尚未实现系统通知、后台同步或提交操作。空聚合列表不代表全部作业已提交或源系统没有数据。
