# 原型验收

## 0.15.0：Me 网格与账户邮箱

2026-10-06。Me 使用 16dp 横向内收、紧凑姓名/账号、真实邮箱/复制、简化 Sign out。App 和 Websites 分别三列圆角方格，图标、短标签和整项点击；字体缩放 >1.3 或可用宽度 <280dp 改两列，方格最小高度等于列宽，字体放大可自然增高。原登录/退出恢复、确认框、原生服务路由和外部 HTTPS / Custom Tabs 处理保留；没有正常 Signed in 状态或账户阴影。

邮箱协议依据公开前端 account tile 组件确认，不从姓名/用户名构造。新增 account.content.email 的只读解析、独立 id=6 同步状态、缓存、资源重试和 Data status；完整进度六项、单 Account 更新一项，账号变更与退出清除邮箱。复制仅显式点击时写入剪贴板，图标短暂变为勾，不读取剪贴板或在日志记录地址。

assembleDebug 和 31 项聚焦 JVM 检查通过（TimetableStateTest 6、SyncRetryTest 10、FeedStateTest 5、CourseworkTest 10）。只新增一个复杂恢复检查：账户读取失败两次重试后保留邮箱，单独重试不重刷其他资源，退出清空邮箱；已有账号切换检查补充邮箱立即清空，既有五项进度断言适配六项。没有为网格/复制/间距编写 UI 或单元测试。既有设备缓存检查的迁移注册/假 API 仅为 schema 5 / 新接口做兼容更新，未运行它们或 UI Test，未启动模拟器。

临时 SQLite 检查从导出的 1/2/3/4 schema 建表、写入虚构行，执行源码中的迁移 SQL 到 5：所有表列定义与 schema 5 匹配，旧行保留，旧同步状态邮箱默认空字符串。该检查不替代完整 Android Room 的设备迁移测试。实际物理手机覆盖升级后正常启动并完成账户缓存，读取确认 user_version=5；因此本机现有缓存的升级路径已正常打开。未运行 Lint、登录态浏览器抓取、手机截图或自动点击复制/退出。

指定物理手机 10AG4S2KQJ0066R，adb install -r 成功，versionCode 23 / versionName 0.15.0；MainActivity 冷启动 Status: ok（529ms）。未卸载或清除数据。仅以 run-as / sqlite3 -readonly 查询 schema 版本、账户状态数量/邮箱是否非空和课表行数：schema 5，id=6 状态 1 条、非空邮箱 1 条，课表 186 条。不输出私人字段或 cookie，不读取系统剪贴板，不自动复制或退出。该结果确认原生 account envelope / email 解析与持久化成功，不替代邮箱展示和 UI 操作验收。

手动复核：邮箱是否正确、点复制后在用户选择的位置粘贴核对；三个原生服务、Settings / Data status、八个外部链接与返回；网格空间和大字体、切主题、断网后缓存邮箱；Sign out 确认取消保留登录（真正退出只在用户愿意重新登录时测试）。

## 0.14.1：首页对齐与标题内容间距

2026-10-06。Next 整卡改为三行 Column，首行日期/时间占满剩余宽度并 TextAlign.End；末行地点占剩余宽度，20dp 箭头位于右侧，不再占用整卡右侧中间区域。课程名/代码仍同一行，长名称允许两行，日期时间跨日内容允许换行；整卡点击详情与 Lake 配色保留。

标题和 All 最小高度 32→28dp，采用 labelMedium / SemiBold 与 onSurfaceVariant，不再额外淡化透明度；Today / Deadlines 首行最小高度 56dp、上/下内边距 2/8dp，后续行保留 64dp / 10dp；空状态上/下内边距 2/10dp。Today 左侧时间原先 align(Top) 且默认列内左对齐，现在与右侧内容块整体垂直居中；宽度改为与 Deadlines 一样的 48sp 转 dp，水平居中、行间距 2dp，起始时间 primary / SemiBold。Schedule 共用课程行同步居中但保持原行高和内边距。字号放大时自然增高，不新增日期或额外卡片。

assembleDebug 成功，git diff --check 通过。本轮简单样式调整没有新增测试或运行单元/UI Test、模拟器、Lint、学校接口 smoke、手机截图；视觉效果由物理手机手动验收。首轮指定物理手机 10AG4S2KQJ0066R 的 adb install -r 返回 INSTALL_FAILED_ABORTED / User rejected permissions；用户授权重试后覆盖安装成功，确认 versionCode 22 / versionName 0.14.1，MainActivity 冷启动 Status: ok（383ms）。未卸载或清除数据，保留登录与缓存。此次成功启动不替代用户的实际布局验收。

手动重点：Next 日期贴右、箭头在最下方右侧且整卡可进入详情；Today / Deadlines 的标题到首行是否紧凑、左列中心是否一致；长课程名称、换行地点/状态、无课状态和较大字体下时间/内容的居中表现。Schedule 的日期跳转、冲突/跨日提示和列表详情仍可正常使用。

## 0.14.0：Lake 主卡片、紧凑标题与常用服务入口

2026-10-06。先修正 Lake 的 Now / Next：使用已有 primaryContainer #D6E8F1 和 onPrimaryContainer #183743，不改其他主题或全局按钮颜色。各首页分区标题最小高度从 48dp 减至 32dp；Deadlines 的 All 改为紧凑可点击 Row，避免默认 Material 按钮撑高标题；foundation clickable 默认的最小触摸范围扩展与无障碍操作标签保留，大字体自然增高。标题、内容行仍共享单层背景，未加边框。

两处修正完成后 assembleDebug 和 3 项既有 AppearancePaletteTest 通过（五个固定主题共 75 组配色对比不低于 4.5）。之后新增 Next 下方一行 Moodle / Email / Tabula / Library 快捷入口，四个 64dp 最小高度的图标/短标签项，与 Me 共用链接配置，沿用 safeExternalUrl / Custom Tabs 与失败 Snackbar，不增加 API 或自动网络读取。入口与 Next 间距 12dp，其余主要区块仍为 24dp。最终 assembleDebug 成功，git diff --check 通过；没有新增测试或运行 UI Test、模拟器、Lint、学校登录态 smoke 或手机截图。

仅公开只读查看外部页面：Email 转 Outlook，Tabula 为未登录页面，Library 为账户跳转页；Moodle 的 web 抓取工具报错，手机浏览器可达性待手动确认。没有传递应用 cookie、提交表单、登录学校账户或操作邮箱/借阅。以上不等于四个服务登录流程已通过。

明确指定物理手机 10AG4S2KQJ0066R，adb install -r 覆盖安装成功，确认 versionCode 21 / versionName 0.14.0；MainActivity 冷启动 Status: ok（542ms）。未卸载或清除数据，保留登录和缓存；没有在手机自动打开外部链接。请手动重点看 Lake 的主卡片是否柔和且仍可辨识、Today / Deadlines 标题是否紧凑；分别点击 All、课程和作业，确认原详情及列表路由正常；四个快捷入口的图标/标签、浏览器跳转、返回原页面和较大字体的表现。

## 0.13.0：首页单层分区与主次信息

2026-10-06。落实已选的单层分区容器方案，本轮暂不加入常用服务快捷入口。Now / Next 标签收进三行主卡片，使用现有五个固定主题的 primary / onPrimary 强调组合；正在进行时不再重复 Happening now，时间、课程名/代码、地点及整卡详情入口保留。未加载或没有后续课程使用普通背景和简短文字，不将空状态做成主强调卡片。

Today、Deadlines 的标题和内容分别共用一个 14dp 圆角背景。透明课程/截止日期行、单行空状态不再嵌套卡片；同组行间浅分隔线内收 12dp。区块间距 24dp，组内标题使用较轻的 labelMedium，标题区统一容纳数量/操作。Deadlines 的 All 与标题同行，仍进入 Coursework / Upcoming；Recently passed 收进同组，仍进入 Past。课表源恢复入口在主卡片附近，作业源恢复在 Deadlines 内，仅原有失败/登录状态出现；没有正常状态的额外错误卡片。

assembleDebug、3 项既有 AppearancePaletteTest 检查通过，其中五个主题共 75 组关键前景/底色对比度不低于 4.5；git diff --check 通过。没有新增简单样式测试或编写/运行 UI Test，没有启动模拟器、运行 Lint、真实学校接口 smoke 或手机截图验收。数据排序/筛选、全部 Today 课程、最近 3 条未来截止日期、英国时区、详情与原站链接、恢复、认证及 Room schema 4 均不变。

明确指定物理手机 10AG4S2KQJ0066R，adb install -r 覆盖安装成功，确认 versionCode 20 / versionName 0.13.0；MainActivity 冷启动 Status: ok（542ms）。未卸载或清除数据，保留登录和缓存。用户手动复核五个主题下主卡片权重、Today / Deadlines 标题归属、区块间距、课程/作业详情、All / Recently passed 跳转、无课/无截止日期状态以及大字体表现。

## 0.12.3：短标签与直接选中底色

2026-10-06。移除 Tab 背景 animateColorAsState，选中底色直接在透明/primaryContainer 间切换，不再插值经过灰暗中间色；selectable 的 indication=null 继续保留。底部标签改为 Home / Classes / Tasks / Me，文字统一 12sp、FontWeight.Normal，不因选中改变字宽；Classes / Tasks 页面标题仍为 Schedule / Coursework deadlines，路由与功能不变。Tab 外/内横向 padding 均从 4dp 减至 2dp，释放标签宽度，保留图标/文字共用 18dp 圆角高亮、轮廓/实心图标、完整点击区域、64dp 最小高度、底栏两侧 20dp 内收和一次安全区。

assembleDebug 成功，git diff --check 通过。本轮简单 UI 调整没有新增测试、运行单元/UI 测试或启动模拟器；未运行 Lint、真实学校接口 smoke 或截图验收。首轮安装被拒绝，用户解锁后明确指定物理手机 10AG4S2KQJ0066R，adb install -r 成功；确认 versionCode 19 / versionName 0.12.3，MainActivity 冷启动 Status: ok（534ms），未卸载或清除数据。用户手动确认快速切换时不再先灰后亮、长按无遮罩、四个短标签可完整阅读及大字体表现。

## 0.12.2：Tab 整体高亮与无按压遮罩

2026-10-06。仅在底部 Tab 的 selectable 移除 indication，保留 interactionSource、Role.Tab、选中语义和完整点击区域。按住不再绘制深色水波纹/遮罩，点击仍切换页面。选中底色改为图标与文字共用的 18dp 圆角容器，160ms 颜色过渡；图标仍为未选中轮廓/选中实心，选中图标和文字共用 onPrimaryContainer，未选中共用 onSurfaceVariant。每个 Tab 内侧各 4dp 留白，底栏普通字体下仍为最小 64dp，并保留原 20dp 两侧内收和一次系统安全区；大字体自然增高。

assembleDebug 成功，git diff --check 通过。本轮简单 UI 调整没有新增测试、运行单元/UI 测试或启动模拟器；未运行 Lint、真实接口 smoke 或截图验收。物理手机 10AG4S2KQJ0066R 的前两次安装被拒绝，用户确认后第三次 adb install -r 覆盖安装成功，确认 versionCode 18 / versionName 0.12.2；MainActivity 冷启动 Status: ok（577ms），没有卸载或清除数据。用户手动复核长按无深色遮罩、点击切页、图标与标签整体高亮、Coursework 标签及大字体表现。

## 0.12.1：顶部对齐与导航语义

2026-10-06。原标题 Row 按子内容高度测量并垂直居中，Schedule 的日历按钮比双行标题高，导致品牌文字较其他页下移；服务/外观页的 Back 也有同类差异。所有页现在共用最小 48dp 内容高度和上下各 2dp 外边距，标题保持单行省略，品牌/进度文字和标题的对齐不再受日历、Today 或 Back 控件有无影响；大字体允许整体自然增高。

底部四个标签使用统一 24dp 矢量图标：房屋、带日期格的日历、作业清单、人物。未选中轮廓、选中实心，选中使用 56 × 32dp 圆角底色与稍粗标签；普通字体下底栏内容仍为 64dp，保留两侧 20dp 内收和一次系统安全区。More 的底部标签及主页面标题更名为 Me，保留账户、设置、数据状态、服务入口及登录恢复标记；内部路由、缓存与认证不变。

assembleDebug 成功，git diff --check 通过。本轮简单 UI 调整没有新增测试、运行单元/UI 测试或启动模拟器；没有截图验收、Lint 或真实学校接口 smoke。明确指定物理手机 10AG4S2KQJ0066R，adb install -r 成功，确认 versionCode 17 / versionName 0.12.1；MainActivity 冷启动 Status: ok（542ms），未卸载或清除数据。用户手动复核四页品牌字样的位置、Schedule 日历/Today、Me 服务/外观 Back、导航选中样式和大字体表现。

## 0.12.0：Coursework 信息密度与连续列表

2026-10-06。移除顶部两行说明、重复的分类标题/计数与过去作业说明；时区、接口范围及提交状态说明留在详情。搜索改为 24dp 圆角、普通字体下 48dp 视觉高度，左侧搜索图标、输入后的清除按钮；大字体允许增高。仅保留等宽 Upcoming / Past 分类，默认 Upcoming，Upcoming 按截止时间升序、Past 降序。

条目按独立 LazyColumn 行渲染，共用连续背景，只有列表首尾圆角；移除独立大卡片及行间大空隙。左列数字 / days、当天 Today、过去 Passed；右列最多两行标题和准确截止日期/时间（d MMM · HH:mm，不同年加 yyyy，英国时区）。整行点击及箭头进入原详情，完整标题与原站入口保留。空分类使用 No upcoming deadlines / No past deadlines，未加载仍独立区分；搜索无结果可 Clear search，保持分类、不请求网络。首页 View all coursework 进入 Upcoming，Recently passed 进入 Past；旧 All / Next 7 days 保存状态按 Upcoming 展示。

assembleDebug 与 20 项既有 JVM 检查通过（CourseworkTest 10、FeedParserTest 10）。仅更新既有筛选检查中被移除的 Next 7 days 断言，改为 Upcoming 包含全部未来条目；没有新增测试、编写/运行 UI Test 或启动模拟器。未运行 Lint、真实学校接口 smoke 或截图验收；用户手动复核列表密度、长标题/大字体、搜索与清除、分类排序、首页入口、点击详情和下拉刷新。

首轮 USB 安装被手机拒绝；用户解锁后已明确指定物理手机 10AG4S2KQJ0066R，adb install -r 覆盖安装成功，确认 versionCode 16 / versionName 0.12.0。MainActivity 冷启动 Status: ok（566ms）。保留登录与缓存，没有卸载或清除应用数据；视觉和交互待用户手动验收。

## 0.11.0：空状态、独立恢复与按需状态详情

2026-10-06。各页面区分未加载、学校返回空列表与筛选为空；Messages / Modules 可 Clear search，Coursework 可 Clear filters，不触发请求。首页仍保留紧凑空状态。失败/登录过期时仅在对应内容附近显示轻量恢复行，缓存可读；重试中隐藏该行，正常状态不增加额外提示或空白列表项。登录过期在 More 入口增加小标记与无障碍说明。

More → Data status 按需查看五类状态、各自缓存更新时间（英国日期/时区）、单资源操作。Timetable / Coursework 新增单资源重试，成功不重报其他资源旧失败，进度为一项。消息历史分页失败单独记忆，重试保留游标；正常下拉仍刷新最新消息，旧游标不可用时回退普通刷新。首页 Recently passed 统计近七天缓存内已过截止日期，进入 Coursework / Past；说明过去不代表未提交，All 入口与账户切换重置筛选。学校接口、认证、Room schema 4、已有缓存和重试预算不变。

构建及 30 项限定 JVM 检查通过（SyncRetryTest 9、TimetableStateTest 6、FeedStateTest 5、CourseworkTest 10）。仅新增一项独立课表/作业恢复检查，验证请求范围、单资源进度、不重报另一资源旧错误，以及失败后恢复；既有分页检查增加历史失败标记的设置/清除断言。没有新增 UI Test 或简单布局测试，没有运行 UI Test 或启动模拟器。未运行 Lint、真实学校接口 smoke 或截图验收；视觉和手动操作待用户复核。

明确指定物理手机 10AG4S2KQJ0066R，adb install -r 覆盖安装成功，确认 versionCode 15 / versionName 0.11.0；MainActivity 冷启动 Status: ok（616ms）。保留登录和缓存，没有卸载或清除数据。

## 0.10.0：实际阶段进度与单一刷新指示

2026-10-06。常规刷新移除 PullToRefreshBox 的旋转指示，保留下拉手势。顶部改为确定进度条，原品牌小字位置显示当前数据源/任务序号或重试次数，不增加新行。

全量以五个资源任务等权计数：账户验证回调推进半项，数据读取/解析/缓存完成或失败处理完毕结算整项；单服务与更早消息为一项。重试与重复验证不重复计数，等待期间不自行前进。全部任务已处理不等于全部成功，最终有失败时用错误色和原有一次短提示；登录过期/提前停止不补齐未执行任务。200ms 动画只平滑真实值变化，结束最多停留 250ms 后隐藏，不延迟业务完成；退出清除进度，页面重建不重播已结束进度。初始本地缓存读取不伪装为网络百分比。

assembleDebug 及 19 项限定 JVM 检查通过（SyncRetryTest 8、TimetableStateTest 6、FeedStateTest 5）。在既有重试测试文件只新增两项状态检查，验证真实阶段/等待停留、重复认证与重试不重复计数、部分失败/缓存保留、认证停止不填满、单服务范围与退出清除进度；不是 UI Test 或简单样式断言。原有认证、账户切换、重试、分页游标与取消检查继续通过。未运行 UI Test、启动模拟器、Lint 或真实接口 smoke；真机动画观感、下拉触发和窄屏/大字体顶部文案由用户手动复核。

明确指定物理手机 10AG4S2KQJ0066R，adb install -r 覆盖安装成功，确认 versionCode 14 / versionName 0.10.0；MainActivity 冷启动 Status: ok（371ms）。保留登录与缓存，没有卸载或清除数据。

## 0.9.1：两行倒计时与英文月份

2026-10-06。首页 Deadlines 左列改为居中的数字 / days 两行，单位按用户要求固定为 days；列宽从 72sp 对应宽度缩至 48sp 对应宽度，随系统字体缩放。右列日期固定使用 Locale.UK 的英文月份缩写：当年 d MMM（6 Oct），下一年及以后 d MMM yyyy（6 Jan 2027）。课程/作业标题、整行详情、剩余自然日计算、准确截止时间与 API/缓存逻辑保持不变。

assembleDebug 成功（28 秒），git diff --check 通过。按项目约定，本轮简单样式调整没有新增或修改测试，没有运行单元测试或 UI Test，也没有启动模拟器。真机的两列对齐、字体缩放和显示效果待用户手动反馈。

明确指定物理手机 10AG4S2KQJ0066R，adb install -r 覆盖安装成功，确认 versionCode 13 / versionName 0.9.1；MainActivity 冷启动 Status: ok（392ms）。保留登录和缓存，没有卸载或清除数据。

## 0.9.0：首页紧凑列表与 NOW / NEXT

2026-10-04。Today 与 Schedule 共用课程行，同一天共用底色与分隔线，保留时间、课程/条目代码、地点、Now/Next、冲突及跨午夜提示；没课仍为单行 No classes today。主卡片正在进行时标为 NOW，否则为 NEXT；全日项只进入当天列表，不占用主卡片。重叠课程全部保留，主卡片按开始时间、id 稳定选择。

Deadlines 最多三项，共用列表底色，左列 n days（单数 1 day、当天未过期 0 days），右列一行原始标题与一行 dd-MM；仅下一年及以后追加年份。英国自然日计算天数，详情仍显示准确时间、完整标题与原站入口。倒计时列宽随系统字号调整，长标题省略，整行点击且带详情箭头和无障碍操作说明。未加载和无返回条目使用不同单行文案，不推断提交状态。

assembleDebug 成功，17 项既有 JVM 检查通过（TimetablePresentationTest 4、SchedulePresentationTest 3、CourseworkTest 10）。没有新增/修改测试、编写或运行 UI Test，也没有启动模拟器。本轮未运行 Lint、真实学校接口 smoke 或截图验收；布局观感、大字体、当天/跨年日期及 NOW 转换由用户在物理手机手动复核。认证、Room schema 4、学校接口与缓存协议不变。

明确指定物理手机 10AG4S2KQJ0066R，adb install -r 成功覆盖安装；vivo V2502A 确认 versionCode 12 / versionName 0.9.0，MainActivity 冷启动 Status: ok（374ms）。保留登录与缓存，没有卸载或清除数据。

## 0.8.1：以卡片颜色区分背景

2026-10-04。按用户反馈移除 0.8.0 新增的所有卡片边框及相关 palette 字段，仅调整 Forest / Rosewood 的普通、空状态和 NEXT 底色，并调亮次要文字；其余主题、渐变、导航、认证、缓存和重试不变。

构建成功，仅运行既有 AppearancePaletteTest 配色检查通过，关键文字/底色组合继续满足 4.5:1；没有新增测试、编写或运行 UI Test，也没有启动模拟器。用户的开发验证约定记录在 AGENTS.md。adb 明确指定物理手机覆盖安装成功，vivo V2502A 确认 versionCode 11 / versionName 0.8.1，冷启动 Status: ok（594ms），保留登录与缓存。最终卡片观感由用户在手机上复核。

## 0.8.0：卡片层次、紧凑导航与有界重试

日期：2026-10-04。Forest / Rosewood 提亮普通、空状态与 NEXT 卡片，并在卡片边缘使用各自主题的 1dp 细边框；三个偏浅主题与渐变/纹理缓存算法不变。所有页顶部统一为 16sp 标题、上下 2dp 边距。底部 Tab 普通字号内容高度 64dp、左右内收 20dp，大字体允许增高；系统安全区仅留一次，保留状态栏/摄像头与手势或系统导航避让。

- 网络 IO、HTTP 408 / 5xx 对失败资源按 2 秒、5 秒间隔各重试一次，最多三次同步尝试，成功资源不重刷；每次资源尝试包含原有账户验证。登录过期、格式错误和其他 HTTP 状态（包括 429）不自动重试，退出取消重试，同步中下拉不会产生并发请求。
- 重试中不产生错误卡片，缓存继续展示。最后失败汇总为一次短暂 Snackbar，带 Retry；登录过期提示带 Sign in，More 保留登录入口。更早消息的自动/手动重试保留同一游标，不变成刷新第一页。未加载、确实为空、退出失败及本地缓存仍按原状态区分；所有列表与顶部不常驻同步失败文字。
- 最终 assembleDebug、assembleDebugAndroidTest、56 项 JVM 测试、Lint 通过（0 errors / 17 warnings）。新增六项重试测试：真实虚拟时间间隔/次数、成功资源不重复、失败缓存保留、通知消费与新一轮预算、认证/格式/400/429 不重试、退出取消、历史游标、成功服务刷新不重报旧课表失败。既有 palette 的 75 组文字/底色对比度验收继续通过。
- 第一轮指定模拟器共 32 项 UI 测试，30 项通过，包括短暂提示消失、针对单个 feed/历史分页的 Retry、More 登录入口、窄屏大字体导航和原有服务/课表/主题检查。两项失败为新密度测试的首页标题高度上限，以及旧测试寻找已移除顶部 Sign in 按钮：随后把标题上下边距从 4dp 调至 2dp，并将旧登录测试改从 More 进入；编译与上述最终 JVM/Lint 验证完成。最终 UI 复跑前 emulator-5556 已断开，未完成复跑，不将初轮结果报告为最终 32 项全部通过；最终真机观感及交互由用户复核。
- 已明确指定物理手机 10AG4S2KQJ0066R，adb install -r 成功覆盖安装，vivo V2502A 确认 versionCode 10 / versionName 0.8.0；MainActivity 冷启动 Status: ok（716ms）。保留既有登录和缓存。没有在手机执行 UI/认证清理测试或截取手机画面。
- 未新增接口、改变认证、Room schema 4 或业务缓存；没有重跑真实学校接口 smoke，不把模拟器虚构数据测试当作本轮学校接口新证据。

手机手动复核：Forest / Rosewood 的 NEXT、空状态与普通卡片边缘；顶部留白、Tab 高度和两端位置；四个 Tab/详情/日期选择继续可用；临时断网下拉后缓存仍可读，重试耗尽只出现短暂提示；恢复网络后下拉更新，不再常驻错误卡片。更大系统字体下 Tab 仍可点，系统栏与内容不重叠。

## 0.7.0：连续日期课表列表

日期：2026-10-04。Schedule 改为连续日期分组列表，默认从今天开始；今天无课时保留 Today 小标题和单行 No classes today。手动选定的空日期也保留分组，其他空日期跳过。移除旧周导航、七日选择器和 Day / Week 控件。

- 同日课程共用列表底色，左列开始/结束时间，右列课程名与代码/地点；整行点击进入原有详情，箭头提示可点击。Now / Next、冲突与跨日延续仅在适用课程出现，全日项不标记 Now / Next。
- 日期图标默认输入 dd/MM/yyyy，可切换日历；支持缓存内的历史记录和返回 Today，不额外请求 API。切页、刷新和 Activity 保存状态恢复保留日期与滚动位置，正常冷启动回到今天；未同步时仍显示加载/重试状态，不把未加载误报为无课。
- 最终 assembleDebug、assembleDebugAndroidTest、50 项 JVM 测试与 Lint 通过（0 errors / 17 warnings）。新增纯函数测试覆盖空起点、日期/时间排序、跳过空日期、历史、跨午夜/英国夏令时、午夜结束与 UTC 日期组件转换。
- 指定 emulator-5556 / Pixel 10a，课表、首页、服务与外观共 28 项 UI 测试通过，runner OK (28 tests)，127.738 秒。包含日期输入/取消/返回今天、下拉刷新、切页/数据更新/保存状态恢复保留位置，以及长标题/地点、冲突与 320dp / 1.8 倍字体。五套颜色的原生课表截图已检查，普通课程行高度不超过 88dp；截图仅使用虚构账户和课程。
- 随后加强刷新测试，实际删除滚动位置之前的一整个日期分组，验证当前课程仍可见；为大字体截图夹具加上系统栏边距。仅重跑 Schedule 的 4 项 UI 测试，全部通过（28.770 秒），并重新检查截图；生产代码与 APK 未变化。
- API、认证、Room schema 4 与缓存协议不变；没有重跑真实接口 smoke 或认证清理/数据库迁移设备测试，不把本轮模拟器截图当作学校接口新证据。
- 手机首次 USB 覆盖安装被拒绝（INSTALL_FAILED_ABORTED: User rejected permissions）；用户解锁并授权重试后安装成功。vivo V2502A 确认 versionCode 9 / versionName 0.7.0，MainActivity 冷启动 Status: ok（590ms）。既有会话与缓存保留；没有卸载、清除数据或截取手机画面，手机实际布局等待用户手动复核。

手动复核：今天无课的单行提示；连续日期与课程行密度；点击课程各处均进入完整详情；日期按钮查看历史/未来、取消选择、返回 Today；滚动后切到 Home 再回来、下拉刷新后位置与课程保持正常；五套配色和较大系统字体下时间/地点可读。

## 0.6.1：移除常态诊断文字，规划课表布局

日期：2026-10-04。已移除首页、Schedule、Coursework、Messages、Modules、Library 的 Saved on this device / Last updated 等保存页脚；More 移除五类同步时间列表与账户 Checked 时间，已登录状态缩短为 Signed in。Developer tools 仅从 More 进入。

- 没有改变 API、认证、Room schema 4、缓存时间的存储或同步策略。未加载、同步失败、登录过期、重试与本地退出确认保持原有行为；不再按正常缓存年龄常驻显示底部说明。
- 构建、47 项 JVM 测试及 Lint 完成（0 errors / 17 warnings）。指定 emulator-5556，首页/课表/服务 20 项既有 UI 测试通过，runner `OK (20 tests)`，66.217 秒。只调整现有探查入口测试为从 More 进入；没有新增重复实现的删除文字测试。
- 已检查虚构数据的首页、课表、Coursework、More、Messages、Modules、Library 原生截图，确认页脚清理和主要内容/入口保留。未重跑真实登录 smoke 或数据/认证清理设备测试；上述协议无改动，旧证据不作为本轮新接口验收。
- Schedule 本轮仅规划新布局。连续日期列表和按周列表两份交互草案已检查日期选择、返回 Today、切周、整行详情、对话框背景不可操作以及 320px 宽度；没有横向溢出或脚本错误。课程/时间使用用户截图中 2026-10-05 至 2026-10-09 的记录，未重新请求学校 API。
- `adb install -r` 成功覆盖安装到 vivo V2502A；确认 versionCode 8 / versionName 0.6.1，MainActivity 启动 `Status: ok`。没有卸载、清除数据或截取手机画面，既有会话与缓存保留。

课表实施建议与验收边界见 [schedule-design.md](schedule-design.md)。手机的清理效果仍需用户手动确认。

## 0.6.0：固定颜色主题与外观设置

日期：2026-10-04。五套完整固定配色 Forest / Lake / Heather / Sand / Rosewood，共用布局和渲染流程。默认 Forest，细纹理关闭；不提供明暗模式或跟随系统开关。

- More → Settings → Appearance，主题与 Fine texture 选择立即生效，SharedPreferences 保存主题 ID/纹理开关；未知 ID 回退 Forest。主页面、详情、底部导航和登录工具栏共用 palette，官方登录网页内容不修改。
- 三层非线性椭圆径向渐变与固定颗粒在后台生成，1024px 长边、16px 短边量化，按主题/纹理/尺寸档最多缓存两张。1080×2400 比例位图为 464×1024、1,900,544 字节；两张约 3.63MiB，不含仍显示的淘汰位图、GPU 纹理和小颗粒图。验证重复请求复用同一位图、第三键淘汰、纹理生成确定性。
- 最终 `assembleDebug`、`assembleDebugAndroidTest`、47 项 JVM 测试、Lint 完成；Lint 为 0 errors / 17 warnings，包含原有提示及新增 SharedPreferences/Bitmap KTX 建议。
- 指定 Pixel 10a / API 37 模拟器，33 项普通设备测试通过，真实登录 smoke 跳过，runner `OK (34 tests)`，96.302 秒。设备测试仅用虚构数据、独立测试数据库/偏好，不在登录手机执行认证清理测试。
- 验证五色立即切换与跨页面保留、偏好重建、系统 night 配置不替换颜色、1.8 倍字体设置可滚动、返回 More/课程详情及设置滑动不触发网络刷新。原有缓存/迁移/认证/课表/服务交互测试通过。
- 75 组关键 Material 前景/底色组合满足 4.5:1；带纹理的五套原生背景按 32px 网格抽样，正文/次要文字/链接/错误文字均满足 4.5:1。检查发现并修复透明 Scaffold 下深色主题招呼语继承黑色，以及浅色背景上错误文字对比度不足；新增实际文字颜色与原生背景校验。
- 已检查五套首页及普通/大字体设置页原生截图，均为虚构课程和账户。确认渐变可见、文字与系统栏图标可读；截图等平台点击涟漪结束后保存。手机视觉效果、重启持久化和滚动流畅性等待用户手动复核，尚未做帧时间或 GPU 内存测量。
- 首次 USB 安装被手机拒绝；用户解锁并授权重试后 `adb install -r` 成功。vivo V2502A 确认 versionCode 7 / versionName 0.6.0，MainActivity 启动 `Status: ok`。没有卸载、清除应用数据或修改学校账户。认证/API、Room schema 4 与业务缓存不变，本轮没有执行真实接口 smoke 或增加接口数量证据。

手动复核：在 Appearance 轮流选择五色，查看首页/课表/Coursework/详情的阅读效果；打开/关闭细纹理；退出并重开确认设置保留；下拉刷新与原有页面跳转继续正常。

## 0.5.1：首页对齐与点击提示

日期：2026-10-04。用户反馈 0.5.0 密度改善，但 NEXT 文字偏上；这来自末行 TextButton 的最小触控高度。移除该独立按钮，整张卡片作为唯一点击区域，右侧 20dp 箭头居中，保留点击反馈和无障碍 `View class details` 操作标签。三行文字上下各 12dp 留白，课程名/代码按基线对齐。

- Today 在已同步且当天无课时只显示 `No classes today`，不显示第二段解释；未取得课表数据仍保留独立加载状态。
- Deadlines 与 View all coursework 同行；窄屏/大字体保留完整标题，右侧操作可换行。
- 最终构建、44 项 JVM 测试、Lint 通过（0 errors / 15 warnings）。模拟器初轮 20 项首页/课表/服务 UI 测试通过；调整大字体标题宽度后，最终首页/课表 11 项再次通过。未更改认证、API、数据库，没有重跑真实登录 smoke 或 cookie 清理测试。
- 位置验收验证 NEXT 内容上下留白差不超过 1dp、箭头垂直居中、普通字体卡片高度不超过 100dp；单行无课卡片不超过 56dp，Deadlines 与跳转入口同行。验证卡片与箭头实际触摸都打开完整详情，无障碍点击标签保留；320dp、1.8 倍字体下仍可进入详情和 Coursework。
- 已检查浅色、深色和窄屏大字体模拟器截图，全部使用虚构课程和账户。手机视觉效果等待用户复核，没有截取手机上的其他应用。
- 首次 `adb install -r` 被手机拒绝；用户要求重试后覆盖安装成功。已确认 vivo V2502A 上 versionCode 6 / versionName 0.5.1，Activity 启动 `Status: ok`。保留既有登录数据与缓存，未卸载或清除数据；手机实际布局等待用户手动复核。

手动复核：NEXT 上下留白与箭头位置；点击卡片不同位置均进入详情；Today 无课仅一行；Deadlines 右侧入口仍能进入 Coursework；下拉刷新保持正常。

## 0.5.0：首页密度第一轮

日期：2026-10-04。根据用户提供的官方首页与 0.4.0 首页截图，只调整首页上方布局和共用顶部刷新入口，不更改 API、数据库或认证协议。

- 招呼语 24sp→16sp，取显示名称的第一个非空白词作为短名字；无名字时不添加逗号。首页头部纵向边距 16dp→8dp，内容左右 16dp、顶端 4dp，区块间距 12dp。
- 移除顶部 Refresh、首页 View messages 和当前日期/时区说明区；正常手动刷新统一下拉触发，错误恢复的 Retry/登录入口仍保留，Messages 仍在 More。
- UP NEXT 改为 NEXT。卡片日期/时间同一行，模块名/原课表代码同一行，地点/详情按钮同一行；标题 24sp→16sp、内边距 24dp→水平 14dp/垂直 10dp，标题最多两行，完整标题/地点在详情中显示。大字体下详情按钮可换行，保留触摸范围；跨日结束时间包含结束日期。
- 构建、44 项 JVM 测试、Lint 通过；Lint 0 errors / 15 warnings。Pixel 10a / API 37 模拟器 29 项普通测试通过，真实会话 smoke test 跳过，runner `OK (30 tests)`。
- 新增首页空间验收：普通字体的 NEXT 卡片不超过 160dp，日期/时间、名称/代码和地点/按钮实际位置符合行布局，无课日的作业卡片首屏可见；深浅色截图已检查。320dp 窄布局、1.8 倍字体验证代码/详情入口可见，打开详情后完整长标题可读。原首页与 Messages 的下拉刷新按当前页面分发，不触发更早分页。
- 所有设备自动化仅在 emulator-5556、独立虚构数据上运行，不在登录手机运行会清 cookie 的测试。
- 已覆盖安装到 vivo V2502A 并确认 versionCode 5 / versionName 0.5.0，Activity 冷启动成功；保留既有登录数据与缓存。手机实际布局由用户继续手动验收，本文视觉检查来自模拟器，不记录其他应用画面。

手动复核：查看短名字、NEXT 三行及首屏信息密度；点击整张卡片/详情按钮均可打开详情；下拉刷新后原课表和截止日期仍正常。0.4.0 已收到用户“测试都正常”的反馈；0.5.0 用户反馈“看起来好不少”，并指出 NEXT 纵向对齐需要进一步改进，已由 0.5.1 调整。

## 0.4.0：账户、Messages、UI、Library / Modules

日期：2026-10-04。构建验收时测试手机暂不可用；随后已覆盖安装到 vivo V2502A（versionCode 4），保留既有数据，用户反馈“目前测试都正常，没有发现异常”。没有新增逐接口数量记录，未知非空结构仍待验证。

- `assembleDebug`、`assembleDebugAndroidTest`、44 项 JVM 测试和 Lint 成功；Lint 为 0 errors、15 warnings（依赖版本、SSO 所需 JavaScript、备份规则和 Uri KTX 建议），不是无警告构建。
- Pixel 10a / API 37 模拟器 27 项普通测试通过（18 项 UI，9 项缓存/认证/迁移），真实会话 smoke test 默认跳过；完整 runner 为 `OK (28 tests)`。测试明确指定本轮启动的 emulator-5556，没有使用会卸载应用的 connected Gradle 流程。
- JVM 新增验证通知与模块 envelope、HTML 与原站链接、网站 read 时间、更早分页参数、重复/无效响应、Library 未知结构回退、英国日期筛选及重叠课程；状态测试覆盖独立 feed 失败/重试、401 停止后续请求、退出时取消等待同步、失败退出阻止重新刷新、更早页显式请求。
- Room 3→4 与 1→2→3→4 升级验证保留现有课表/截止日期/账户状态；新增三类缓存原子替换、重开持久化、失败回滚、不同数据互不清除。消息分页验证合并去重、拒绝完全重复页和旧账户游标；本地退出验证清除五类数据。
- WebView cookie 清理测试仅在无现有 MyWarwick cookie 的模拟器运行，使用虚构 cookie；普通手机或已有登录的模拟器跳过这项，避免测试清除真实会话。
- UI 覆盖 More 账户/登录/退出确认与取消、Messages 纯文本/完整详情/显式外部链接/搜索/手动分页、Modules 代码/学年/Moodle、Library 服务说明/账户链接、下拉手势、顶部刷新按当前页面分发、1.8 倍字体的长标题/筛选/可滚动详情。修复刷新 lambda 没有实际调用回调的问题，手势与按钮回归通过。
- 已检查虚构数据的 More、Messages、Modules、Library 和大字体 Coursework 模拟器截图。HTML 图片占位符已移除；筛选/详情点击收起搜索键盘，长标题可换行，详情可滚动。
- 登录态浏览器只读验证新接口：Messages 69 条、Modules 1 条、Library 0 条，HTTP 200；before 确认读更早消息，since 确认读更新消息。新增原生真实登录、学校非空 Library 借阅结构、外部网站实际登录和 MFA 回跳仍需手机复核。没有用模拟器虚构数据代替真实接口验收。
- 未调用写接口、签发 timetable token、标记已读或更改学校账户。More 退出只清除本应用会话，系统浏览器会话保留。

### 手动安装与复核

1. 使用 0.4.0 debug APK 覆盖安装（`adb -s <手机序列号> install -r <apk路径>`，或手机直接打开 APK），保留现有应用数据；不要先卸载或清除数据。包名与签名方式沿用此前版本。
2. 打开应用：确认旧课表和 Coursework 保留；刷新或完成官方登录后，查看 More 的账户与五类同步时间。手机日期显示按 Warwick 时区。
3. 查看 Messages：列表/搜索/详情与原站按钮；消息不足 100 条时不出现“更早记录”按钮，查看不更改网站已读状态。
4. 查看 Modules 与 Library：核对模块代码/学年/Moodle 入口；若借阅非空，核对实际字段和源站，当前不承诺到期日/欠费字段。
5. 下拉刷新、顶部刷新、Coursework 四种筛选、课表重叠提示、较大系统字体；断网刷新后缓存应保留，恢复后可刷新。
6. **最后**测试 More 退出：确认前取消不退出，确认后本应用数据与会话清除；重新登录。这个步骤会主动移除应用缓存，系统浏览器不会随之退出。

## 0.3.0：Coursework

日期：2026-10-04。

- 构建、29 项 JVM 测试与 Lint 通过；Lint 为 0 errors、14 warnings，包含已有版本/备份/JavaScript 提示和新增链接的 KTX 建议。
- Pixel 10a / API 37 模拟器 14 项测试通过（9 项 UI、5 项缓存/认证/迁移）；真实登录 smoke test 默认跳过。Runner 为 `OK (15 tests)`，成功 14、跳过 1。
- 新增验证：真实 `+01` / `Z` 日期、夏令时、整批拒绝无效/重复条目、官方 HTTPS 链接范围；单接口失败保留缓存、课表解析失败时继续更新 Coursework、第二接口会话过期、账户切换清除两类 UI、刷新期间防止重复请求。
- Room 2→3 及 1→2→3 升级验证保留原课表与账户状态。Coursework 有独立同步时间，课表刷新不清除截止日期，Coursework 替换失败原子回滚。
- UI 验证首页进入 Coursework、详情与显式原站点击、仅保留 Coursework 缓存时仍能查看数据、有效空列表不暗示“已提交”。首页变长后，调试入口测试改为先滚动查找；复核全部通过。
- 0.3.0 最终构建已覆盖安装到 vivo V2502A；保留原 SSO 会话与缓存。原生日志确认 `events=143`、`items=3`，无需重新登录。终止应用后再次打开复核缓存和同步。
- 已检查模拟器浅色 Coursework 列表/详情以及手机深色首页。手机真实源站详情的浏览器登录和作业提交未测试；测试使用注入回调检查原站链接，仅验证显式点击触发。
- 本轮未注册 token、修改学校账户、提交作业或执行写接口。“提醒”目前是首页截止日期展示，尚无系统通知/后台同步。

接口接入与展示进度见 [api-progress.md](api-progress.md)。

## 0.2.0：课表与接口探查

日期：2026-10-04；POC 首次登录验证于 2026-10-03 完成。本轮版本 0.2.0。

## 自动化

- `assembleDebug`、`testDebugUnitTest`、`lintDebug` 和 `assembleDebugAndroidTest` 成功。Lint 为 0 errors、13 warnings（依赖/目标版本提示、SSO 所需 JavaScript、备份规则和 KTX 建议），不是无警告构建。
- JVM 19 项测试通过：9 项协议解析、6 项同步/认证状态、4 项日期展示与探查摘要。
- 状态测试验证启动先显示缓存、并发刷新合并、离线失败保留缓存、恢复后清除错误、会话过期保留课表、重新登录后更新、有效空课表、账户切换立即移除旧数据。
- Pixel 10a / API 37 模拟器上，9 项设备测试通过：6 项 Compose 交互、3 项缓存/认证/迁移；另一个需要真实登录的 smoke test 默认跳过。Runner 输出 `OK (10 tests)`，实际成功 9、跳过 1。
- 交互覆盖官方登录入口、已认证但解析失败、缓存课表、日期/周切换、完整名称与详情、过期会话的缓存显示、手动触发探查。
- 数据测试验证重开数据库保留数据、替换失败回滚、cookie 精确 origin 限制，以及 Room 1→2 保留旧课表/账户同步状态并添加 `moduleName`。
- 使用直接安装和指定设备 instrumentation，未使用会自动卸载应用的 Gradle connected 测试流程。普通设备测试仅处理独立测试数据库和虚构课程。
- UI 测试库显式使用 Espresso 3.7.0，修复旧传递依赖在新 Android 上调用已移除反射接口导致的失败。相关官方说明：https://developer.android.com/jetpack/androidx/releases/test#espresso-3.7.0

## 实际登录结果

用户完成官方登录后，首版遇到 `DateTimeParseException`：课表时间具有 `[Europe/London]` 区域后缀，旧解析器只接受 offset。修复为 `ZonedDateTime`，并分别保存认证状态与课表同步状态。

2026-10-03 修复包通过覆盖安装保留现有登录数据。原生日志连续两次确认 `Native timetable sync succeeded; events=143`，用户确认“这次解析成功了”。因此已验证官方 WebView SSO → CookieManager → 原生 OkHttp → 解析与 Room → Compose 课表链路，无需再次登录。

本轮手机为 vivo V2502A / Android 16。0.2.0 覆盖安装，始终未卸载或清除数据；停止进程后冷启动仍显示课表，随后原生同步再次成功读取 143 条。Room 1→2 升级完成，首页实际显示完整课程名称与原条目代码，浅色模拟器和深色手机截图均已检查。

原生只读 Coursework / Library 检查使用现有会话，通过并分别返回 3 条与 0 条、HTTP 200。手机期间也出现过 TLS/Socket 连接中断；实际页面保留缓存，前台冷启动后恢复同步，没有要求再次登录。不得把这些连接错误当作 cookie 过期。

真实网络 smoke test 不属于上述 9 项可控设备测试：较早的两接口检查成功，最后一次扩展检查遇到连接中断；尝试由测试启动 Activity 的复核未能完成，已终止并恢复普通应用，测试源码回到纯只读检查。没有将该复核记为通过。最终普通客户端的课表同步已成功（143 条），Coursework/Library 结论来自先前成功的原生检查。

离线和真实会话过期使用可控状态/交互测试验证。未通过删除学校 cookie 或更改账户制造真实会话失效；未切换手机全局网络来影响其他应用。尚未验证后台长期会话和后台同步，本轮仅支持前台使用。

## 需要实际登录的验收

1. 在测试手机打开 MyWarwick+，点击 `Sign in with Warwick`。
2. 用户自行完成官方 SSO/MFA；不要向聊天提供账号密码、验证码或 cookie。
3. 登录页面自动关闭，原生首页显示课表。
4. 调试日志仅记录 `Native timetable sync succeeded; events=...`，不记录认证/个人响应。
5. 终止应用后重开，确认已缓存课表。
6. 手机离线时刷新：保留课表并提示连接失败；恢复网络后可重新刷新。

实际登录和冷启动已完成上述第 1–5 步。第 6 步的状态转换由自动化测试覆盖，并观察到真实连接失败与恢复；未将手机手动切到全局离线模式。
