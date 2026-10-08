# API 接入进度

更新：2026-10-08；代码 0.25.1 / versionCode38。首页Today/Deadlines左列48→40sp转dp，正文左移8dp（默认字体缩放）；Inbox标题14sp Medium、摘要/搜索12sp、来源/日期11sp，来源与日期同行，消息详情标题18sp/正文14sp；Tab图标24→22dp，点击区域不变。接口、认证、只读边界及schema5不变；公交与非空Library结构仍未验证。下表为历史证据，不代表当前账户实时数量。协议见 [protocol.md](protocol.md)，架构见 [architecture.md](architecture.md)，交付状态见 [validation.md](validation.md)。

0.25.1已覆盖安装到物理手机，构建/Lint通过；Inbox来源日期同行与缩小图标经只读截图核对，搜索/详情、大字体与首页左列仍需用户手动验收。详细证据见validation.md。


## 已接入

| API / 方法 | 接入情况 | 使用与展示 | 验证 / 限制 |
| --- | --- | --- | --- |
| `GET /user/info` | 原生已验证 | 每资源更新前验证账户并取得 CSRF；Me 使用姓名、usercode | WebView session；不保存或输出认证字段 |
| `GET /api/tiles/content/timetable` | 原生已验证 | Home Now / Next、Today剩余课程/Tomorrow预览、周末无剩余课程祝语；Classes日期分组、冲突/跨日、共享地点图标与详情入口 | 早期原生样本143条；0.15.0本地缓存186条。英国时区；全日项不参加Now / Next，保留在日程列表 |
| `GET /api/tiles/content/coursework` | 浏览器、原生已验证 | Home 最近 3 个未来 deadline / Recently passed；Tasks 搜索、Upcoming / Past、详情与原站链接 | 历史样本 3 条；使用原始 title，无独立课程名字段。近期聚合，不代表完整历史或提交状态 |
| `GET /api/streams/notifications?limit=100[&before={id}]` | 已接入；浏览器协议验证、手机整体反馈正常 | Home最近两条摘要，点击详情后留在Home，All进入Inbox；Inbox独立Tab搜索/详情/原站入口/显式更早分页，页标题Messages。紧凑消息卡片：来源/英文短日期同行，标题/摘要各最多两行，详情保留完整正文/日期时间。下拉只刷新Messages最新页并重置历史页，最多缓存500条 | 0.4.0浏览器69条；未单独记录原生实时数量。摘要仅对两条可见消息在后台转纯文本；不写回网站已读、不新增未读计数或推断紧急等级 |
| `GET /api/tiles/content/modules` | 已接入；浏览器协议验证、手机整体反馈正常 | Me → Modules：名称、代码、学年、搜索、公告/评估数量和 Moodle 入口 | 0.4.0 浏览器 1 条；公告/评估当时为空，正文与完整选课覆盖未验证 |
| `GET /api/tiles/content/library` | 摘要页已接入；浏览器、原生空列表已验证 | Me → Library：学校说明、账户入口；非空时只展示已知 title/text/href 或通用原站提示 | 历史样本 0 条；非空借阅、到期日与欠费结构待验证 |
| `GET /api/tiles/content/account` | 原生解析与缓存已验证 | Me 邮箱/显式复制；独立 Account 状态与刷新。姓名/账号仍来自 user/info | 2026-10-06 前端结构证据 + 手机 schema 5、id=6、邮箱非空检查；未输出邮箱。空值不猜测地址，复制操作待手动核对 |

0.21.0 未新增接口或写操作。JSON 字符串按实际类型读取，null href/name 不再产生错误链接或姓名；登录检查复用应用级 API，成功登录会合并为一次待刷新。无活动网络直接结束，传输错误耗尽重试后停止本轮后续资源；已完成资源和缓存保留。Messages HTML 在后台转换，列表与详情共用按账户创建、约 2 MiB 的文本缓存；不改变服务器已读状态。Classes/Tasks 等显示名称仍来自 AppLabels，FeedKind 的显示扩展位于 UI 层。验证与物理状态见 [validation.md](validation.md)。

六类业务数据独立缓存与记录更新时间；切换账户清空旧数据，失败保留对应缓存。单项恢复保留在对应内容附近；普通页面不显示保存时间页脚，Me不再提供状态/调试面板。网站快捷入口只是浏览器跳转，不算新增 API。

## 未接入 / 未采用

| API / 方法 | 状态与下一步判断 |
| --- | --- |
| `GET /api/tiles/content/eventsmerge` | 前端候选，合并事件结构和课表去重关系待验证 |
| `GET /api/tiles/content/calendar` | 前端候选，个人日历覆盖待验证 |
| `GET /api/tiles/content/mail` | 前端候选，不能视为完整邮件 API |
| `GET /api/tiles/content/todo` | 前端候选，与 Coursework 的重叠待验证 |
| `GET /api/streams/notifications?limit=100&since={id}` | 浏览器已验证读较新记录；当前使用最新页刷新，不做增量同步 |
| `GET /api/timetable` + `X-Timetable-Token` | 未采用；仅 cookie 请求为 401，专用 token 生命周期和有效性未验证 |
| `POST /api/timetable/register` | 未调用、未签发 token；当前 cookie 链路可读取课表 |

后台同步、系统通知、多账户、作业提交与消息写操作尚未实现。空聚合列表不代表源系统没有数据或作业已经提交；新增接口前先补结构证据与产品用途，不批量探测。
