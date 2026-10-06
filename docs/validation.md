# 验证与交付记录

整理：2026-10-06。保留当前交付状态与必要历史证据；旧版逐项日志、预览和失败修复过程从 Git 历史查阅。此文档只记录已发生的验证，不能用构建/安装替代UI手动验收，不能用虚构数据测试补充学校接口证据。

## 最新代码：0.18.0 / versionCode 26

代码提交 c8a2724，第四批同步/缓存整理完成。执行命令：

```powershell
$env:JAVA_HOME = 'D:/Dev/JDK21.0.8'
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest --tests uk.ac.warwick.plus.RepositorySyncTest --tests uk.ac.warwick.plus.TimetableStateTest --tests uk.ac.warwick.plus.FeedStateTest --tests uk.ac.warwick.plus.SyncRetryTest
```

| 检查 | 已记录结果 / 边界 |
| --- | --- |
| Debug 构建 | assembleDebug 成功 |
| 聚焦 JVM | 23项全部通过：TimetableStateTest6、FeedStateTest5、SyncRetryTest10、RepositorySyncTest2 |
| 新增范围 | 仅两项复杂Repository编排：换账户清快照且请求前拒旧游标；取消阻塞下载后不写缓存，本地退出仍可完成 |
| Room快照 | 生成的TimetableDao_Impl.snapshot事务包装已核对；内存DAO fixture不验证真实SQLite |
| 物理部署 | 指定10AG4S2KQJ0066R，首次install -r返回INSTALL_FAILED_ABORTED / User rejected permissions；**0.18.0尚未装上**，待解锁重试，未卸载或清除数据 |
| 未执行 | UI Test / Android instrumentation、模拟器、Lint、登录态浏览器API抓取、手机截图与自动UI操作 |
| 待验收 | 真实SQLite快照、0.18.0冷启动与刷新、用户手动交互 |

0.18.0未改变API/认证源码、schema5或页面布局。状态/重试/账户隔离规则见 [architecture.md](architecture.md)。

## 历史关键证据

以下均为原交付记录的摘要，本次未重跑：

| 阶段 | 验证 / 部署 / 反馈 |
| --- | --- |
| POC / 0.2.0（2026-10-03/04） | 修复区域时区后，用户确认“这次解析成功了”；原生课表143条，冷启动/缓存正常，Coursework3条、Library0条只读检查成功。扩展smoke最后遇连接中断，未记通过；后台会话未验证 |
| 0.3.0 / 0.4.0 | Coursework及Feed缓存/迁移、解析和业务状态检查通过；0.4.0浏览器Messages69/Modules1/Library0。覆盖安装后用户“测试都正常”；不补全Library非空结构 |
| 0.5.0–0.7.0 | 当时完成构建/JVM/Lint和虚构数据模拟器布局、主题、日期交互检查；0.6.0偏好重建、固定主题/背景缓存通过，75组纯色配色及32px背景抽样达到4.5:1，非所有像素/尺寸穷尽检查。0.7.0连续课表28项设备检查及后续4项课表复核通过。均为旧版证据，不证明当前所有UI通过 |
| 0.8.0 | 构建、56项JVM、Lint通过（0 errors / 17 warnings）。初轮UI32项中30项通过，修正后模拟器断开，最终未复跑，不能记为32项全过；随后按用户约定停止日常UI测试/模拟器流程 |
| 0.8.1–0.14.1 | 密度、导航、首页分层等迭代构建与必要既有检查通过；物理手机覆盖安装/启动成功。没有以安装成功替代视觉验收；简单样式调整未新增测试 |
| 0.15.0 / schema5 | 构建与31项聚焦JVM通过；虚构SQLite迁移检查1/2/3/4→5列定义及旧行保留，非完整设备Room测试。物理升级/冷启动529ms，schema5、账户id=6状态/非空邮箱各1条、课表186条；只查数量/标志，不输出私人值，不自动复制 |
| 0.16.0 | 提交13d1448；构建与41项既有JVM通过，物理覆盖安装/冷启动543ms。用户随后明确“验收通过了”，组件迁移手动验收通过；不补充API结构 |
| 0.17.0 | 提交e4b8f2d；构建与56项聚焦既有JVM通过。未新增测试，既有断言适配共享日期逻辑、移除退役周视图检查。首次安装被拒，授权重试成功；versionCode25，冷启动394ms，保留登录/缓存。页面/跳转待手动反馈 |

历史API验证的字段、时间与语义见 [protocol.md](protocol.md)；帧时间、GPU总内存、背景生成耗时、后台长期会话及Library非空数据仍无验收结论。旧截图/日志在不跟踪的work/仅作辅助，不作为可重复的当前自动化入口。

## 手动复核清单

| 范围 | 检查点 |
| --- | --- |
| 数据恢复 | 冷启动已有内容可读；下拉进度真实、多次下拉不并发；断网失败保留课表/作业/邮箱，联网后恢复；Data status单资源更新 |
| Home / Classes | Next日期靠右/地点行末箭头与整卡详情；Today/Deadlines左列；日期跳转/取消/Today、切页后位置、全日/跨日/冲突 |
| Tasks / Feed | Upcoming/Past、搜索/清除、英文月份/精确时间；消息更早分页/失败重试与纯文本详情 |
| Me / 外观 / 链接 | 真实邮箱与显式复制、网格、大字体、五个主题/纹理持久化；首页/Me及详情来源点击后才跳浏览器，失败提示可见、返回正常 |
| 会话 | 登录失效保留缓存并提供恢复；换账户/真正Sign out仅在用户愿意重新登录时测试，不自动清理真实会话 |

## 验证纪律与本轮文档清理

按 [AGENTS.md](../AGENTS.md) 控制测试范围。物理手机为10AG4S2KQJ0066R，日常使用adb install -r保留数据；不把模拟器安装记为手机部署，不在登录手机运行会清cookie/卸载应用的测试。旧androidTest保留，未经显式要求不作为日常UI验证流程。

本轮仅改README/docs并移除已完成的一次性重构脚本；应用代码、依赖、版本与设备状态未改动。不重跑构建/测试、不部署或抓取学校接口；核对本地文档引用、Markdown结构、API覆盖及源码关键事实，执行git diff --check。已完成的计划/备选草案从工作树移除，详细历史仍可通过Git恢复。
