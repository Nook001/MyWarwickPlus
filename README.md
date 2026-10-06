# MyWarwick+

独立的 Android 学生客户端原型。Kotlin + Jetpack Compose，复用 MyWarwick 聚合后端，不申请新的 OAuth application。不是 Warwick 官方应用。

## 第一条完整链路

官方 SSO WebView 登录 → CookieManager → OkHttp 原生读取课表 → Room 原子缓存 → Compose 首页 / 连续日期课表。

- 首页：下一节课、开始倒计时、今日安排与截止日期；优先展示完整模块名称，保留原始课表条目代码。
- 0.5.0 首页：16sp 招呼语带短名字；移除顶部 Refresh、首页消息按钮及当前日期区。NEXT 卡片紧凑展示日期/时间、课程名/代码、地点/详情三行；长名称省略后可在详情阅读，正常刷新通过下拉触发。
- 0.5.1 首页：NEXT 移除独立文字按钮，三行内容上下留白一致，右侧居中箭头提示整卡可进入详情；保留点击反馈和无障碍操作说明。Today 无课只显示一行，Deadlines 与 View all coursework 同行。
- 0.6.0 外观：More → Settings → Appearance 提供 Forest、Lake、Heather、Sand、Rosewood 五套固定颜色，选择立即生效并保存，不跟随系统明暗。三层非线性渐变背景在后台生成，最多缓存两张；Fine texture 开关提供固定细颗粒材质，默认关闭。
- 0.6.1 信息清理：移除各页的 Saved on this device / Last updated 等常态页脚，More 移除保存时间列表和账户检查时间，Developer tools 只保留在 More。刷新失败、登录过期与未加载提示仍按状态出现；数据缓存不受影响。
- 0.7.0 Schedule：紧凑标题与日期图标，默认从今天开始的连续日期分组列表；同一天共用底色，左侧开始/结束时间，右侧课程名称、条目代码和地点，整行进入详情。今天无课显示 Today 小标题与单行 No classes today；未来空日期跳过。支持历史日期跳转、返回今天、Now/Next、冲突与跨午夜提示。
- 0.8.0 阅读与导航：Forest / Rosewood 卡片底色提亮并加 1dp 细边框；各页标题统一为紧凑字号与上下 2dp 边距。底部 Tab 内容普通字号为 64dp，两侧内收 20dp，保留一次系统安全区，大字体允许增高。
- 0.8.1 配色修正：移除 0.8.0 新增的卡片边框，仅以 Forest / Rosewood 更明亮的普通、空状态和 NEXT 底色区分背景；相应提高次要文字亮度，保持阅读对比。
- 0.9.0 首页收尾：Today 复用 Schedule 的紧凑课程行，同一天共用底色和分隔线；NOW / NEXT 区分正在进行与未来课程，全日项仅进入 Today。Deadlines 共用列表容器，左列 n days（1 day / 0 days），右列第一行原始课程/作业标题、第二行 dd-MM，下一年及以后才显示 dd-MM-yyyy；整行点击箭头进入完整详情。加载与空列表显示紧凑单行提示。
- 0.9.1 Deadlines：左列改为数字 / days 两行居中，右列日期使用英文月份缩写（6 Oct），下一年及以后显示年份（6 Jan 2027）；列宽随系统字号调整，整行详情入口保留。
- 0.10.0 数据反馈：移除下拉刷新的旋转圈，顶部进度条由当前数据源的账户验证、数据读取/缓存结果驱动；原品牌小字位置临时显示当前数据源与任务序号、重试次数。全量为五项，单服务/更早消息为一项，重试等待不推进或重复计数；失败和提前停止保留真实处理进度及短暂提示。具体规则与后续方案见 docs/data-feedback.md。
- 0.11.0 恢复入口：未加载、真实空列表与筛选为空分别显示；搜索/筛选为空可一键清除。更新失败或登录过期时在对应内容附近显示轻量 Retry / Sign in，缓存继续可读，More 标记需要登录；More → Data status 按需查看五类数据状态与独立更新时间。课表/作业也支持单资源重试，旧分页失败重试保留游标；首页 Recently passed 打开 Coursework 的 Past，过去截止日期不推断未提交。
- 请求失败：网络 IO、HTTP 408 / 5xx 按 2 秒、5 秒间隔自动重试，每个失败资源最多额外两次；成功资源不重刷。登录过期、解析异常及其他 HTTP 错误（包括 429）不自动重试。缓存持续可读；重试耗尽后只显示短暂 Snackbar 与 Retry，登录过期的登录入口仍在 More，不常驻顶部错误卡片。更早消息重试沿用原游标。
- Coursework：首页最多 3 个未来截止日期；独立页面按时间展示返回条目与过去截止日期，提供详情及原站链接。时间按 Warwick 时区显示，不推断提交状态。
- More：账户与会话状态、外观设置、仅退出本应用及重新登录、常用学校服务入口。
- Messages：只读列表、搜索、完整详情、手动加载更早消息；HTML 转为纯文本，查看不标记网站已读。
- Modules：模块名称、代码、学年、公告/评估数量与 Moodle 入口。Library：学校返回的账户摘要、空状态及账户入口；非空借阅结构仍待验证。
- 首页、课表、Coursework 和 More 的滚动页面支持下拉刷新；Coursework 支持搜索与 All / Upcoming / Next 7 days / Past 筛选，重叠课程会提示冲突，详情保留长标题与大字体阅读。
- 课表和 Coursework 分别原子缓存；单个接口失败保留旧数据，另一接口仍可更新。当前“提醒”是首页展示，不包括系统通知。
- 课表：日期按钮支持键入英国格式日期或切换日历选择；从选定日期往后查看，离开起点时显示 Today 返回操作。切换标签页、刷新及 Activity 状态恢复保留日期和列表位置；正常冷启动默认今天。
- 点击课程打开详情，显示模块、完整时间、地点、教学周；地点链接通过系统浏览器打开。
- 所有页面、详情和系统栏图标共用所选颜色主题；统一间距、课程条目、空状态和同步提示。
- 时间固定使用 `Europe/London`，不受手机所在时区影响。
- 未登录时打开 Warwick 官方网站，用户自行输入账号并完成 MFA。
- 缓存可在应用重启和离线时使用；失败响应不会覆盖已保存课表。
- 启动时先显示缓存，再检查会话与同步；登录过期保留缓存并提供登录入口，联网或重新登录后可刷新。
- 区分未取得数据、确实无课、会话过期和同步失败。
- 本版不注册课表 token、推送，不提交作业，也不标记通知已读。

## 开发

功能开发阶段仅在用户显式要求时编写 UI Test；简单功能和 UI 调整不新增单元测试。通过构建、必要的既有检查和物理手机手动反馈验证；具体约定见 [AGENTS.md](AGENTS.md)。

用 Android Studio 打开本目录。需要 JDK 21、Android SDK Platform 37.0；`targetSdk` 为 36，最低 Android 9（API 28）。构建工具与依赖版本已固定。

本机 SDK 已写入不跟踪的 `local.properties`。其他机器需要自行配置 Android SDK 路径。

```powershell
$env:JAVA_HOME = 'D:/Dev/JDK21.0.8'
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug :app:assembleDebugAndroidTest
adb -s <手机序列号> install -r app/build/outputs/apk/debug/app-debug.apk
adb -s <手机序列号> shell am start -n uk.ac.warwick.plus/.MainActivity
```

手机自动化测试只使用独立测试数据库和虚构课表，不需要学校账号。在专用测试手机或模拟器上可运行 `:app:connectedDebugAndroidTest`；该 Gradle 测试流程可能在结束时卸载应用，因此不要在需要保留登录会话的手机上使用。日常调试使用 `adb install -r` 覆盖安装。

也可对指定模拟器直接安装并运行测试，避免影响其他设备：

```powershell
adb -s <模拟器序列号> install -r app/build/outputs/apk/debug/app-debug.apk
adb -s <模拟器序列号> install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s <模拟器序列号> shell am instrument -w -r uk.ac.warwick.plus.test/androidx.test.runner.AndroidJUnitRunner
```

`LiveSessionSmokeTest` 默认跳过；只有显式传入 `-e class uk.ac.warwick.plus.LiveSessionSmokeTest -e liveSession true` 才会使用手机现有登录状态读取课表、Coursework 和 Library，仅输出数量与状态。

实际 SSO → 原生请求已在已连接手机上验证，成功读取 143 条课表记录。解析器支持真实响应中的 `[Europe/London]` 区域时区后缀；认证成功与课表加载失败分别显示。

## 接口探查

调试包 More 的 `Developer tools` 打开 API explorer，手动选择 Coursework / Library / Timetable / Messages / Modules 并点击 `Run request`。仅允许这五个固定 GET 路径，显示 HTTP 状态、总耗时（含会话检查）、envelope 成功与否、条目数、content 和首条条目的字段名。失败时显示连接/认证/服务错误，不显示原始异常信息。发布包不显示入口，API 方法也禁止在发布包执行。

此前真机原生 Coursework 已返回 3 条，Library 返回 0 条。0.4.0 浏览器验证 Messages 69 条、Modules 1 条和 Library 0 条；该版本随后覆盖安装到手机，用户反馈手动测试正常。Library 非空条目结构仍待验证。探查工具不会保存原始响应、个人字段值或认证信息，也不会自动批量探测；业务缓存仅保存显示需要的字段。

## 结构

```text
app/src/main/java/uk/ac/warwick/plus/
  auth/       官方登录 WebView、严格限定 origin 的 cookie jar
  data/       API、JSON 解析、Room、课表 Repository
  ui/         Compose 页面、时区处理、ViewModel
```

Room 声明使用 Java 注解处理，业务与 UI 使用 Kotlin；因此不需要额外 KSP 或 kapt 插件。当前仅有一个 app module，后续按照实际功能边界拆分。

Room schema 2 新增 `moduleName`，schema 3 新增 Coursework，schema 4 新增 Messages / Modules / Library 缓存；通过显式 1→2→3→4 迁移保留旧课表与同步状态。五类数据各自原子替换、独立记录同步时间；单接口失败保留旧缓存，切换账户清除全部数据。

## 认证与数据边界

- WebView CookieManager 是唯一的 cookie 存储来源；不将凭据导出到电脑、DataStore、日志或 Git。
- OkHttp 只向 `https://my.warwick.ac.uk:443` 发 cookie，禁止自动跳转；认证失败返回登录状态。
- GET 的 CSRF 信息来自 `/user/info`，仅接受预期的 `Csrf-Token` header 名。
- 登录 WebView 无 JavaScript bridge，关闭文件访问、明文/混合内容，证书错误直接取消，不自动授予网页权限。
- 外部地点链接通过 Custom Tabs 打开，不使用原生认证 cookie jar。
- Room 只保存五类页面展示需要的字段和账户归属；不保存教师邮箱等额外字段。系统备份已关闭。
- 切换账户后先清除旧账户数据库，再读取新账户数据。
- More 的退出仅清除本应用 WebView 的 cookie、WebStorage、缓存和五类 Room 数据；取消/等待现有同步，不调用学校退出或更改账户接口，系统浏览器会话保留。退出失败时要求重试后再登录。

## 当前限制

Web 接口是内部协议，没有稳定性承诺。新安装应用的 WebView 会话与桌面浏览器、其他应用和 Custom Tabs 的 cookie 分离，需要在手机上独立登录。后台同步、系统提醒和多账户尚未实现。Coursework 是近期聚合列表，完整历史、源系统覆盖与提交状态尚未验证；Library 非空借阅/欠费字段、Modules 公告正文未验证。消息每次刷新取最近 100 条，旧记录手动分页，最多保存 500 条；刷新成功会重置旧分页。通知没有可靠紧急程度字段，因此首页仅提供紧凑入口。

当前账号 cookie 在应用私有 WebView 数据中保存。不要将调试包作为已完成的公共发行版本；卸载或通过系统清除此应用数据可移除本地会话和缓存，不需要修改学校账户。

接入进度见 [docs/api-progress.md](docs/api-progress.md)，接口证据见 [docs/protocol.md](docs/protocol.md)，验收记录见 [docs/validation.md](docs/validation.md)，课表布局与实现见 [docs/schedule-design.md](docs/schedule-design.md)。每轮验证完成后提交 Git；提交情况会在交付回复中列出。
