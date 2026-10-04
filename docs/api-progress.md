# API 接入进度

更新：2026-10-04，版本 0.7.0。每次接入接口时同步更新此表；协议细节见 [protocol.md](protocol.md)。本轮接入连续日期分组课表，不新增 API。

| API / 方法 | 接入情况 | 如何使用与展示 | 验证 / 限制 |
| --- | --- | --- | --- |
| `GET /user/info` | 已接入 | 启动、刷新和登录回跳时检查账户、会话与 CSRF 信息；More 展示账户和登录状态，0.6.1 移除检查时间 | 原生已验证；cookie/token 不写入日志或 Git |
| `GET /api/tiles/content/timetable` | 已接入 | 首页 NEXT 三行与整卡详情；Schedule 从今天/选定日期开始连续分组，同一天共用列表底色，左侧时间、右侧课程/条目代码/地点，Now/Next、冲突、跨午夜提示；Today 无课小标题及单行说明；日期跳转/返回今天/历史查看，详情与地点链接；缓存与下拉刷新 | 此前真机 143 条；英国时区；失败保留整批旧缓存；大字体允许行高增加，完整标题在详情阅读；列表位置在切页/刷新/状态恢复时保留 |
| `GET /api/tiles/content/coursework` | 已接入 | 首页最近 3 个未来截止日期，Deadlines 标题与 View all coursework 同行；列表搜索、All / Upcoming / Next 7 days / Past 筛选、完整详情、原站入口；独立缓存与失败提示 | 此前浏览器及真机 3 条；支持 `+01` 和 `Z`。近期聚合，不推断已提交状态；原站浏览器可能需独立登录 |
| `GET /api/tiles/content/library` | 摘要页面已接入；非空结构待验证 | More → Library；学校空列表说明、账户入口、独立缓存与重试；非空条目仅展示实际存在的 title/text/href 或通用原站提示 | 当前浏览器 200 / 0 条；此前原生 0 条。借阅、到期日、欠费等字段未确认，不推定业务含义 |
| `GET /api/tiles/content/eventsmerge` | 待验证 | 评估与课表合并事件，避免重复日程 | 前端候选；当前无业务展示 |
| `GET /api/tiles/content/modules` | 已接入 | More → Modules；名称、代码、学年、搜索、详情、公告/评估条数及 Moodle 链接；独立缓存 | 0.4.0 浏览器 200 / 1 条；id 为数字，announcements/evaluations 当时为空。不等同完整选课清单；公告正文仍在原站查看 |
| `GET /api/tiles/content/calendar` | 待验证 | 评估个人日历与课表关系 | 前端候选；当前无业务展示 |
| `GET /api/tiles/content/mail` | 待验证 | 评估邮件摘要与原站入口 | 前端候选；不能视为完整邮件 API |
| `GET /api/tiles/content/todo` | 待验证 | 评估与 Coursework 的重叠及去重 | 前端候选；当前无业务展示 |
| `GET /api/streams/notifications?limit=100[&before={id}]` | 已接入 | More → Messages 列表、搜索、详情、显式原站点击、手动更早分页；最近 100 条，每次刷新重置分页，最多缓存 500 条；0.5.0 移除首页消息按钮 | 0.4.0 浏览器 200 / 69 条；before 为更早记录游标，since 为更新记录游标。read 为网站已读时间；查看不写回已读，无可靠紧急等级；HTML 转纯文本 |
| `GET /api/timetable` + `X-Timetable-Token` | 未采用 | 暂使用已跑通的 cookie 聚合接口 | 仅 cookie 请求 401；专用 token 生命周期未验证 |
| `POST /api/timetable/register` | 未调用 | 暂无需要；已有登录可直接读取课表 | 前端/旧应用存在注册与 JS bridge 代码；未签发 token |

认证统一使用官方 WebView 登录得到的 MyWarwick session cookie，原生请求严格限定 MyWarwick HTTPS origin。当前业务接口都是只读 GET。五类页面独立缓存与同步时间；切换账户清除全部缓存，单接口失败不覆盖另一份缓存。More 的本地退出取消/等待同步并清除应用 cookie、WebStorage、缓存与数据库，不调用学校账户写接口。

0.6.0 五类页面及详情共用所选固定 palette；More → Settings → Appearance 只保存主题 ID 和细纹理开关，不读取新的学校接口。背景按主题、纹理和尺寸档后台生成并有界缓存；切主题、设置页滑动不触发数据刷新。认证协议、业务缓存与 Room schema 4 不变。

0.6.1 移除首页、课表、Coursework、Messages、Modules、Library 的保存/更新时间页脚，以及 More 的保存时间列表与会话检查时间。后台仍记录独立同步时间，失败不覆盖缓存，错误与重新登录操作继续显示。Developer tools 只在 More；课表的新日期分组列表尚未接入，方案见 [schedule-design.md](schedule-design.md)。

0.7.0 已将选定的连续列表接入 Schedule，移除周范围、七日选择器和 Day/Week 多层导航。日期选择仅筛选已有缓存，不请求新的接口；普通刷新沿用原有请求。跨午夜课程分别出现在所覆盖的日期，列表按当天范围显示 00:00/24:00 与延续提示；全日项不显示 Now/Next。数据与认证协议、数据库 schema 4 不变。

0.4.0 构建时手机不可用；随后已覆盖安装到 vivo V2502A，用户反馈手动测试均正常、未发现异常。该反馈不补充新的接口数量证据，也不补全 Library 非空借阅结构或 Modules 公告正文的验证。

“截止日期提醒”目前指首页信息展示，尚未实现系统通知、后台同步或提交操作。空聚合列表不代表全部作业已提交或源系统没有数据。
