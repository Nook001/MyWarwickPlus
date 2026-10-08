# 协议证据与边界

文档整理：2026-10-08，适用代码 0.26.0-beta.1；**本次未重新请求学校接口**。证据来自 2026-10-03/04 登录态浏览器 CDP 网络观察、部署前端资源、原生手机结果，以及 2026-10-06 账户组件/缓存检查。数量和资源 hash 仅代表观察时的样本；接入状态见 [api-progress.md](api-progress.md)，验证范围见 [validation.md](validation.md)。不保存原始 cookie/token/HAR、私人正文或账户值。

## 认证与请求

- 业务 origin 为 https://my.warwick.ac.uk:443。官方 WebView SSO → CookieManager → OkHttp CookieJar → 原生 GET 已跑通，不需要新的 OAuth application 或 timetable token。
- 每个资源尝试先 GET /user/info，读取 `user.{authenticated,usercode,name,csrfHeader,csrfToken}`；根节点还含 refresh 与 links.login/logout。refresh 为 URL 字符串或 authenticated=false 时要求重新登录。
- 资源 GET 使用 session cookie、Accept: application/json、MyWarwickPlus/version (Android) User-Agent；仅当 csrfHeader 为预期 Csrf-Token 且 token 非空时添加该 header。历史浏览器课表 GET 不带显式 CSRF 仍为200，客户端仍遵循网页约定；未逐个验证 SSO cookie 的必要性。
- 原生禁止自动重定向；3xx、401、403 或非 JSON Content-Type 归为需登录。其他失败状态抛出服务错误，响应上限4MiB；超时为连接15秒、读取25秒、整次35秒。
- WebView 会话与桌面浏览器、其他应用及 Custom Tabs 分离。新安装需在手机独立登录，用户自己输入凭据/MFA；Custom Tabs 无法直接交出其 cookie 替代本应用会话。

## 已使用接口结构

除消息外，聚合 GET 无额外查询参数，通用 envelope 为 `{success,status?,data:{tile:{content:{...}}}}`。字段表只记录结构，未列字段不代表已验证或需要缓存。

| 方法 / 路径 | content 或 data 结构 |
| --- | --- |
| `GET /api/tiles/content/timetable` | content: currentWeek, defaultText, items[]；item: id,title,start,end,isAllDay,type,academicWeek,location{name,href},parent{shortName,fullName},staff[] |
| `GET /api/tiles/content/coursework` | content: defaultText,items[]；item: id,title,text,href,date |
| `GET /api/tiles/content/library` | content: defaultText,href,items[]；非空条目结构未确认 |
| `GET /api/tiles/content/modules` | content: defaultText,items[]；item: id:number,fullName,moduleCode,academicYear,href,announcements[],evaluations[]；前端另支持可选 lastUpdated |
| `GET /api/tiles/content/account` | content: email,fullName,userId,...；应用只读取 email，身份仍由 user/info 决定 |
| `GET /api/streams/notifications?limit=100[&before={id}]` | data: notifications[],read；item: id,title,date,provider,providerDisplayName,type,typeDisplayName,notification,providerOverrideMuting,tags,icon{colour,name,variant},text,textAsHtml,url |

### 解析与语义

- 课表时间样本为 `2026-10-05T10:00:00+01:00[Europe/London]`，也接受 offset/Z；Instant 保存、Europe/London 展示。title 经常是原始课程条目代码，parent.fullName 为可读模块名；早期143条中137条有完整名称。分别保留字段，不猜测缺失名称。
- Coursework 样本包括 `2026-10-15T12:00:00.000+01` 和 `2026-10-30T12:00:00.000Z`。Coursework/Feed 共享小时级 offset 兼容，课表保留 ZonedDateTime 严格解析。错误 envelope、缺字段/无时区、重复 ID 或不合法时间范围按各解析器规则拒绝整批并保留缓存。
- Coursework 说明提到未来一个月以及 Tabula/Moodle/my.wbs 聚合；系统以外提交不出现。没有结构化提交状态，不从文本、过去日期或缺失条目推定已提交。历史3条链接指向Tabula，不代表完整源系统覆盖。
- Messages.read 是网站已读时间，不是消息 ID。limit=2 的历史观察确认 before 为排除边界的更早页；since=最新id 返回0条、since=最旧id 返回68条，since 用于较新记录。当前客户端不采用 since，也不写回已读。
- providerOverrideMuting 表示覆盖静音，不能当紧急等级。HTML 转纯文本，不执行网页或加载嵌入图片。消息可能指向校外 HTTPS 网站，显式点击交浏览器，不传应用 cookie。
- Modules 的公告/评估仅保存数组数量；观察样本均为空，正文/成绩/完整选课未验证。Library 历史空列表不能证明借阅、到期日或欠费字段；未来未知结构只给已知文本或原站入口。
- account.content.email 缺失/null/空字符串为邮箱不可用；非字符串或无有效 envelope/content 拒绝并保留缓存。只缓存本人邮箱；不拼接替代地址或存储其他账户字段。

## 证据范围

| 日期 / 来源 | 观察结果与限制 |
| --- | --- |
| 2026-10-03/04 手机原生 | WebView → 原生课表143条；Coursework3条、Library0条。修复区域时区后用户确认解析成功；连接中断时缓存保留。未验证后台长期会话 |
| 2026-10-04 浏览器 | Messages69条（5条含HTML）、Modules1条、Library0条，均200/success；验证before/since。后续0.4.0手机整体手测正常，不补充逐接口原生数量 |
| 2026-10-06 前端与手机 | account组件读取content.email，声明fullName/userId；0.15.0手机只读检查schema5、id=6有状态且邮箱非空，确认原生解析/持久化；未输出地址或自动复制 |

公开 Android 仓库参考 commit：cb105bf13a3eb579d268163ff54a8418e43b8767。早期前端证据包括 /assets/js/0b7e644fedf7b0fc1482-bundle.js 与 /assets/js/e322fe41339e34bebbd1-main-import.js；0.15.0另检查公开 vendor/main-import。hash、代码与内部协议可能随部署改变；不能把历史样本写成当前实时验证。

0.25.0已删除Me中的Developer tools/API explorer入口；保留的协议probe辅助方法无生产UI入口，Release仍禁止调用。历史debug探测只限五个固定GET且只显示结构摘要，未输出私人响应。

## 登录、安全与本地退出

- CookieManager 是唯一cookie存储来源，仅向 MyWarwick HTTPS/443 发 cookie，不导出到电脑、偏好、日志或 Git。CSRF 只驻留请求过程。
- 登录 WebView 无 JavaScript bridge，禁文件访问/明文/混合内容，证书错误取消，不自动授予网页权限。学校网页仍负责SSO/MFA，不由客户端模拟表单。
- 原站跳转共用 HTTPS/default-or-443、无userinfo 校验；作业/模块额外限 Warwick 域，普通外部消息/地点允许其他合法HTTPS。Custom Tabs 使用自己的浏览器会话，不附原生认证header。
- Room 保存页面所需字段、账户归属、同步时间与本人邮箱；不额外保存教师邮箱，系统备份关闭。公开schema5升级基线与账户隔离见 [architecture.md](architecture.md)。
- Me 的 Sign out 只退出本应用：先取消/等待同步，再清cookie、WebStorage、WebView缓存与六类Room数据；不调用学校links.logout，不影响系统浏览器会话。失败要求重试后再登录。更新安装保留数据，注销/换账户才主动清理业务缓存。

## 候选与专用移动接口

eventsmerge/calendar/mail/todo 仅为前端候选，未确认结构/覆盖；不把聚合卡片当完整源系统API。

前端 registerForTimetable() 存在 POST /api/timetable/register，将返回 token 交 native bridge；旧 Android 使用 GET /api/timetable + X-Timetable-Token，响应 data.items[]。历史仅cookie、无专用token的请求返回401。**未签发token，也未验证有效期、撤销、后台可用性或完整链路当前有效**；本应用无bridge，继续采用已跑通的cookie聚合路径。

这些是学校内部接口，无稳定性/兼容性承诺。第三方公开发行的允许范围尚未取得学校书面确认，服务条款与许可需在发行前核对。只读、最小存储和有界请求减少维护范围，不替代许可；接口存在或个人账户可访问不代表学校承诺长期支持。

## 来源

- [官方 Android 仓库](https://github.com/UniversityofWarwick/mywarwick-android)
- [MyWarwick](https://my.warwick.ac.uk/)
- [CookieManager](https://developer.android.com/reference/android/webkit/CookieManager)
- [Custom Tabs](https://developer.chrome.com/docs/android/custom-tabs)

以上为历史证据索引，本轮未重访外部来源。
