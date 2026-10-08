# MyWarwick+

独立的 Android 学生客户端，使用 Kotlin + Jetpack Compose，通过官方 WebView SSO 登录并复用 MyWarwick 只读接口。不是 University of Warwick 官方应用。

**[下载首个 Beta](https://github.com/Nook001/MyWarwickPlus/releases/tag/v0.26.0-beta.1)**：0.26.0-beta.1，Android 9+，需要 Warwick 账号。安装签名 APK，首次使用需登录；当前为 Pre-release。下载页包含该版本说明，下一版本草稿见 [发布说明](docs/release-notes.md)。

## 功能

| 页面 | 内容 |
| --- | --- |
| Home | Now/Next、今日剩余课程或明日预览、截止日期、公交/打印余额半宽摘要、消息、底部公开 Events、网站入口 |
| Classes | 连续日程、日期跳转、跨日/冲突提示、课程与地点详情 |
| Tasks | Coursework 搜索、Upcoming/Past、准确期限、原站链接 |
| Inbox | 消息搜索、来源筛选、完整详情、最新页刷新和显式更早分页 |
| Me | 邮箱复制、主题设置、Library/Modules、服务入口和本地退出 |

固定颜色主题、静态渐变背景、离线缓存和真实任务阶段进度。时间按 Europe/London 显示。当前无后台同步、系统通知、作业提交或消息已读写回。

开发候选为 0.27.0-beta.1：首页增加 Buses/Print/Events 摘要及独立缓存，保留现有 UI 调整；浏览器真实批次结构已确认，原生功能验收状态见 API 表。公交不宣称实时预测，打印充值与活动报名通过原站进行。

## 文档

| 类型 | 入口 |
| --- | --- |
| 架构与 UI 标准 | [架构与组件规范](docs/architecture.md) |
| 数据接口 | [API 接入、响应结构与认证](docs/api-progress.md) |
| 开发与质量 | [构建、手机验证、性能采集与待办](docs/development.md) |
| 发行维护 | [签名备份、打包和发布](docs/release.md) |
| 用户发布说明 | [当前 Beta 说明](docs/release-notes.md) |

Android Studio 打开仓库，使用 JDK 21 和 SDK 37.0；具体命令见开发文档。版本/依赖以 Gradle 为准，开发纪律见 [AGENTS.md](AGENTS.md)。文档维护当前行为与必要流程，旧评分、方案和逐版过程通过 Git 历史查阅。

## 隐私与反馈

[隐私说明](PRIVACY.md) · [MIT 许可证](LICENSE) · [第三方许可](THIRD_PARTY_NOTICES.md)

数据直接在设备与学校间传输，没有项目中转服务器。内部接口可能变化，第三方发行允许范围尚未得到学校确认；个人账号可以访问不等于官方授权。

通过 [GitHub Issues](https://github.com/Nook001/MyWarwickPlus/issues)反馈，附应用/Android 版本与复现步骤；请移除姓名、邮箱、学号、私人消息和 cookie/token，不上传完整 HAR 或数据库。
