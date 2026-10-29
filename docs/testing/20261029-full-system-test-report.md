# 现有功能修复与部署验收记录

日期：2026-10-29，时区 Asia/Shanghai。

修改目录：`C:\Users\ivanr\Desktop\Hospital_System\Hospital_System`。开始时与 `Hospital_system_new\Hospital_system` 同为 `f922406`，2297 个受版本控制文件去除换行格式差异后内容一致。本次只修改指定目录，修改留在 Git 工作区，没有暂存、提交或推送，也没有发布 Docker Hub。

## 1. 修复的问题

| 范围 | 问题与处理 |
|---|---|
| 首页与导航 | 修复不存在的角色组件导致初始化中断、旧身份接口及失效菜单；按服务端会话显示身份和菜单，URL 参数不能切换身份。首页显示实际已发布公告，内容以文本输出。消息入口进入现有站内通知。 |
| 登录与注册 | 登录失败显示统一提示；选择患者身份时，下拉组件与实际选项一致；注册网络失败可见且可重试。找回密码页说明管理员临时密码重置流程，移除不能工作的邮件表单。 |
| 资料与头像 | 统一旧医生资料入口；排班页不依赖 URL 中的医生编号；空头像使用有效图片；患者和医生顶部显示真实会话姓名，移除虚构模板姓名。 |
| 模拟充值 | 修复异步请求成功后读取已失效事件对象导致的报错；修复重复点击产生多次请求；成功后刷新余额和流水，失败保留输入。 |
| 基础数据 | 后台按链接选中对应数据分类；保留医生、患者、科室层级、职称的查询、分页、维护及引用删除保护。旧患者列表增加管理员权限，限制页码和条数，阻止普通账号读取其他患者资料。 |
| 公告 | 修复重置按钮遮蔽表单 reset 方法、成功提示立即被清空、创建草稿却提示已经发布的问题；验证草稿、修改、发布、下线、删除全过程。 |
| 医生与号源 | 修复重复解析 JSON、空图片、旧滚动插件与 jQuery 不兼容。医生详情显示真实资料和日期时段；预约确认页显示服务端挂号费，移除无用途的银行卡/CVV 模板字段，防止重复点击。 |
| 预约与排班 | 保留容量、预约号、取消回补、同医生改期不重复扣费、历史保留及排班调整规则。实际点击验证从科室选择到取消预约全过程。预约成功页使用有效入口，不再声称发送确认邮件。 |
| 候诊与停诊 | 活跃预约排在历史记录前，避免被 300 条上限挤掉；患者默认显示当天全部时段，选择筛选条件后生效；不再显示已经无法执行的恢复候诊按钮；已完成记录不被停诊提示覆盖；原因长度与服务端 500 字规则一致。 |
| 病历与医嘱 | 通知中的病历链接读取对应预约的正式病历，不再替换为列表第一页；失败明确显示错误并重试同一记录。保留草稿、刷新恢复、医嘱增删、必填、版本冲突、定稿及本人读取规则。 |
| 收入报表 | 修复退役接口和旧订单状态文本统计。管理员按北京时间查询挂号扣费流水，模拟充值不计入收入，取消未退款的扣费仍记在原扣费日；验证跨午夜边界。 |
| 会话并发 | 未登录页面并发请求可能重复销毁同一 Session，造成 500。现在失效会话的读取和销毁返回正常未登录结果；账号验证服务故障仍返回 503，不吞掉业务异常。 |
| 登录保护时间 | OceanBase JDBC 在 UTC JVM 下默认读取和写入时区不一致，会把限流时间反复推后。安全时间字段统一显式使用上海时区读写，验证 899 秒仍锁定、900 秒到期，保留每来源 60 次及账号失败 5 次限制。 |
| 连接池 | 科室等查询未关闭 SqlSession，连续浏览后等待约 20 秒。修复科室、菜单、新闻、医生查询和公告读取的会话释放；成功和 SQL 失败都归还连接。 |
| 地图与运行环境 | 未配置地图服务时显示说明，不再在页面加载时抛异常；Docker 明确使用上海时区，镜像命名可配置，头像位于持久卷。 |

## 2. 已验证的功能

- 患者注册、三种角色登录、登录失败提示、退出、临时密码强制修改、账号停用及旧会话失效、短时锁定及解锁。
- 本人资料修改、头像格式/大小/内容校验与下载、模拟充值、余额和扣费流水。
- 科室与医生查询、医生详情、排班创建/调整/撤销、容量和剩余号源。
- 预约、重复及满额拒绝、并发防超售、取消、改期、预约号不复用、失败事务回滚。
- 停诊申请/撤回/审批/拒绝/撤销、影响预约保留、冻结号源和患者通知。
- 当日签到、候诊、叫号、过号、恢复候诊、开始接诊、爽约条件与状态限制。
- 病历草稿、医嘱、保存及刷新恢复、必填规则、定稿、不可覆盖正式记录、患者查询筛选/分页和通知定位。
- 站内通知分页、未读、单条/全部已读、本人权限和随业务事务提交。
- 医生、患者、科室和职称维护，搜索、分页、关联选项、删除引用保护及非零余额保护。
- 公告完整生命周期、公开读取、文本安全；审计角色/动作/结果/日期筛选、分页及敏感内容限制。
- 每日挂号收入、日期校验、资金口径和权限边界。
- 首页、菜单、返回链接、常用公共页面、管理页面及手机布局。

## 3. 测试结果与证据

| 验证 | 结果 | 证据 |
|---|---|---|
| 默认 Maven 和 WAR | 315 项中通过 298 项，跳过 17 项，失败 0、错误 0 | `target/full-system-final-verify.log` |
| 真实 OceanBase 基线 | 6 项通过 | `target/full-system-oceanbase-baseline.log` |
| 真实旧结构升级与数据保留 | 1 项通过 | `target/full-system-oceanbase-upgrade.log` |
| 真实候诊、容量并发、停诊、病历事务 | 1 项通过 | `target/full-system-oceanbase-clinic.log` |
| 真实 OceanBase、UTC JVM 安全时间 | 3 项通过；另带 11 项既有安全测试通过 | `target/full-system-oceanbase-security-time.log` |
| 最终镜像真实扩展 HTTP | 168 个断言通过 | `target/full-system-final-http.log` |
| 最终镜像真实门诊点击流程 | 三角色页面、签到到病历定稿、通知、容量、停诊审批及账号启停通过 | `target/full-system-final-clinic-live.log` |
| 真实自助与后台维护 | 146 项检查通过 | `target/full-system-portal-live.log` |
| 真实收入报表与权限 | 27 项检查通过 | `target/full-system-income-live.log` |
| 实际预约入口全流程 | 选科室、选医生、号源、费用、预约、取消及容量回补通过 | `target/full-system-booking-live.log` |
| 桌面/手机实际页面 | 33 条路由各检查两种尺寸，共 66 页；脚本错误、失败资源、破图、页面溢出均为 0 | `target/full-system-ui/page-smoke.json`、`final-summary.json` |
| 并发匿名请求 | 8 个新访客会话、64 项检查通过，均返回正确 401，无 500 | `target/full-system-final-browser.log` |
| 页面回归 | 14 组浏览器脚本和 16 项 JavaScript 用例通过；顶部身份另有失败前/修复后记录 | `target/full-system-final-browser.log`、`target/full-system-header-green.log` |
| 连接池回归 | 默认 Maven 中包含 44 项；264 次成功查询及 22 次 SQL 失败后，活动连接数均回到 0 | `DepartmentsSessionLifecycleTest`、`PublicReadSessionLifecycleTest` |
| PowerShell 安全检查 | 运维 44 项与 Windows PowerShell 5.1 升级保护通过 | `target/full-system-operations-safety.log`、`target/full-system-upgrade-safety.log` |
| 已部署业务环境 | 就绪、入口、公开页和未登录权限边界 16 项只读检查通过，无页面异常 | `target/full-system-deployment-readonly.log` |

默认跳过的 17 项不算通过：其中 11 项 OceanBase 用例已显式启用另行执行，6 项旧 MySQL 环境测试未运行。目标数据库是 OceanBase。

功能写入、模拟扣费、故障注入和并发测试全部使用随机 `hospital_test_` 测试库及虚构账号。页面巡检对匿名访问预期的 401 检查 JSON 错误码及隐藏账号导航，不将 401 当作页面故障，也不忽略 500。日志保留排错时的失败记录，最终结果见各最终日志和汇总 JSON。

## 4. 原业务数据与本地部署

已有业务应用停写后，先导出备份及 SHA256，再运行 `upgrade-five-extensions.ps1 -TargetDatabase hospt -ConfirmTarget hospt -MaintenanceMode`。没有重新导入 MySQL、演示种子或清空数据卷。

- 原有 16 张表记录数保持一致：患者 1、医生 1、管理员 1、预约 12、病历 2、医嘱 1、排班 4、号源 12、既有审计 104，其余业务表记录数同样保持。
- 18 条保留校验涵盖账号凭据/头像、余额、预约字段、病历和医嘱关联/内容，升级前后一致；患者余额合计仍为 30.00。
- 容量与实际占用不一致为 0，缺失预约号为 0，升级 `COMPLETE` 标记为 1。初始化账号安全记录 3 条，不补发历史通知。
- 原业务账号没有重置密码，没有使用真实患者执行测试扣费。部署后的未登录权限检查会按原规则追加失败访问审计，原审计记录保留。
- 原部署无上传头像文件；新版使用 `hospital_system_hospital-uploads` 持久卷。隔离测试验证容器替换后头像原始字节仍一致。

备份位于本机被 Git 忽略的 `database/oceanbase/export/backups/`；校验依据位于被忽略的 `.hospital-local/`。不要将这些数据加入 Git 或镜像。

当前本地镜像：`hospital-system:local`，同内容别名 `hospital-system:verified-local`。最终镜像 ID：`sha256:2824165a098422d012dcf412d99194f5c153d770c9170277adea976af480f48a`。

业务 Compose 项目仍为 `hospital_system`，继续使用原 OceanBase 数据卷；数据库镜像实际摘要已与 Compose 固定版本核对一致。应用容器 `hospital_system-app-1` 使用上述新镜像，8080 端口提供服务。

```powershell
cd C:\Users\ivanr\Desktop\Hospital_System\Hospital_System
docker compose --project-name hospital_system --env-file .env.oceanbase -f docker-compose.oceanbase.yml ps
curl.exe --fail http://localhost:8080/ustbhospital/health/ready
```

登录地址：`http://localhost:8080/ustbhospital/hospro/login.html`。

以后从本目录重新构建并仅更新应用：

```powershell
docker compose --project-name hospital_system --env-file .env.oceanbase -f docker-compose.oceanbase.yml build app
docker compose --project-name hospital_system --env-file .env.oceanbase -f docker-compose.oceanbase.yml up -d --no-deps --no-build --pull never app
```

构建和启动不会自动推送 Docker Hub。已发布的 `ivan091107/hospital-system:v1.0.1-lsy-chl` 本次未改变，待人工验收后再决定发布标签。

## 5. 验证范围

本轮已执行的现有功能场景全部通过，没有保留已知的功能阻塞。测试不能证明所有可能输入和运行环境永远无缺陷。短信/邮件自助验证码及外部支付不在本次范围；找回密码使用已有管理员重置流程，充值仍是系统内模拟充值。取消不自动退款、停诊保留预约并通知患者处理等现行业务规则没有改变。
