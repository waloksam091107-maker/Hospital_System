# 医院挂号系统五项扩展实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** 完成多号源、停诊、候诊、账号保护和站内通知，保留现有业务记录。
**Architecture:** 沿用 Servlet/MyBatis；新增聚焦业务的 JDBC 仓储与服务，使用现有 SqlSession 连接参与同一事务。排班、预约与停诊采用医生行作为共同锁，避免跨业务并发穿透。
**Tech Stack:** Java 8、Servlet 3.1、MyBatis、OceanBase MySQL 模式、JUnit、H2、浏览器测试。
**Spec:** ../specs/2026-10-27-five-extensions-design.md

## Global Constraints
- 仅指定 Hospital_system_new/Hospital_system 目录；不暂存、不提交、不推送 Git。
- 不重置真实数据库、不自动替换业务容器、不发布镜像。新旧数据均须可增量升级。
- 停诊保留预约并通知患者自行改期/取消；不退款；短信/邮件不做。
- 新号源默认 10、旧号源保持 1、容量 1—1000；失败 5 次/15 分钟锁定 15 分钟；Asia/Shanghai。
- 代码与证据留工作区；用户已批准设计及直接开始实现，不再次等待方法选择。

## Review Focus
- 通知必须随业务提交，接收角色相同编号不串消息（任务 1、3）。
- 启停和重置撤销旧会话，旧登录入口不得绕过（任务 2）。
- 取消不打开人工关闭号源，改期失败不丢原预约（任务 3）。
- 审批与预约/叫号共享锁，停诊不能穿透新排班（任务 4、5）。
- 迁移失败可重试，旧主键/余额/病历/审计不被覆盖（任务 6）。

### Task 1: 站内通知
Files: 新增 NotificationService、NotificationServlet、notifications.html、通知脚本及 NotificationServiceTest；独立 schema 片段。
Interfaces: `NotificationService.enqueue(SqlSession, String role, int recipientId, String eventKey, String type, String title, String body, String targetType, String targetId)`，不提交调用者事务；本人分页/未读/已读 API `/notifications/*`。
- [x] RED: 真实数据库测试角色隔离、重复事件、已读归属、分页和回滚。
- [x] GREEN: 实现事务消息和本人接口/页面，禁止消息正文插入 HTML。
- [x] VERIFY: Maven 定向通知测试及页面交互测试通过。

### Task 2: 账号保护
Files: 新增 AccountSecurityService/账户控制仓储与管理端接口；修改 LoginServlet、AuthenticationFilter、SessionIdentity、PasswordChangeService/安全入口；独立 schema 片段。
Interfaces: `AccountSecurityService.isEnabled(SqlSession, String role, int id)` 供预订检查；会话版本和 mustChangePassword 由账号模块管理。管理 `/portal/admin/account-security/*`。
- [x] RED: 启停、会话撤销、并发失败、锁到期、临时密码、密码字节边界、退出、替代入口测试。
- [x] GREEN: 实现状态/失败限流/密码控制及管理界面；敏感操作审计和通知。
- [x] VERIFY: 账号服务与 Servlet 测试；旧登录/密码回归。

### Task 3: 多号源与预约序号
Files: AppointmentsServiceImpl、DatesServiceImpl、Appointments 和预约查询 SQL、预约查询 SQL；新增 ClinicTime、ClinicWorkflowStore、容量接口和页面。
Interfaces: 医生行锁用于全部变更；容量/occupied_count/next_sequence 存于 schedule_slots，appointment_sequence/queue_status 存于 appointments。`ClinicTime` 可注入 Clock。
- [x] RED: 容量满额/重复患者/取消回补/序号不复用/改期回滚/零挂号费/过期时段测试。
- [x] GREEN: 原业务事务接入容量、序号、账号检查和通知；保持历史 ID；默认新排班容量 10。
- [x] VERIFY: 真实 SQL 并发测试及旧预约回归；容量编辑、余号和序号显示。

### Task 4: 停诊申请和审核
Files: ClinicWorkflowService/ClinicWorkflowServlet 的停诊方法、申请和审批页面；ClinicWorkflowStore；停诊 schema。
Interfaces: `/clinic/leave/*`；医生申请/撤回、管理员审批/撤销；预订/查询/候诊共用已批准冻结查询。
- [x] RED: 重叠、角色、审核幂等、接诊中拒绝、保留预约、撤销不误开放测试。
- [x] GREEN: 日期范围≤31天、时段集合校验、共享医生锁；患者和医生定向通知、审计。
- [x] VERIFY: 服务/HTTP/页面测试及预订竞争测试。

### Task 5: 签到候诊
Files: ClinicWorkflowService/ClinicWorkflowServlet 的候诊方法 和候诊页面；修改 AppointmentsServiceImpl.startAppointment、MedicalRecordsServiceImpl 定稿。
Interfaces: `/clinic/queue/*`；签到、叫下一位、过号、恢复、爽约；仅 CALLED 可开始，定稿同步 DONE。
- [x] RED: 固定时钟边界、重复签到、并发叫号、过号恢复、未签到/过号爽约资格及外人拒绝测试。
- [x] GREEN: 独立 queue_status/历史，医生最多一个 CALLED/IN_PROGRESS；取消改期移出队列。
- [x] VERIFY: 服务/HTTP/浏览器完整流程。

### Task 6: 增量升级、集成和交付
Files: database/oceanbase/01_hospt_schema.sql、database/migrations/20261028_five_extensions.md、升级脚本、隔离集成测试、部署/验收文档。
- [x] RED: 旧库迁移前后账号/预约/病历/资金/审计对账，异常数据拒绝、重试安全和并发。
- [x] GREEN: 前置检查、分阶段进度、稳定序号回填、新约束和初始化结构；安全维护窗口说明。
- [x] VERIFY: `mvn -B verify`、新增浏览器测试、独立 OceanBase 升级/并发、构建 WAR；最终独立代码审查，修复重要问题。证据记录 target/ 或忽略目录，Git 仅检查 diff/status。

## Execution ledger
2026-10-28：设计经用户批准，基线 bf1d7d9。采用任务边界分工：账号和通知各有独立文件；预约/停诊/候诊由主执行者串行整合。任何共享文件改动先协调，所有 Maven 构建串行避免 target 竞争。不使用依赖 Git 提交的完成脚本，直接保存测试日志和工作区差异。


2026-10-28 完成记录：
- 任务 1、2：通知与账号模块完成真实 H2/Servlet 和浏览器测试，业务通知接入同一 SqlSession 事务。
- 任务 3—5：容量、停诊和候诊集中到 ClinicWorkflowService/Store 与 ClinicWorkflowServlet，共享医生行锁；现有预约接口调用这些事务，保留原列表和病历接口。
- 最终审查发现整秒时间戳会丢失过号队尾的毫秒偏移，已通过整秒回归测试复现，改用持久化 queue_history 的 WAITING 记录顺序；没有依赖容器或数据库的时间小数精度。
- 任务 6：旧库升级及重复执行在真实 OceanBase 通过；默认 verify 233 通过/14 跳过，另行 OceanBase 8 通过；真实 HTTP 72 断言、实际浏览器角色页面和所有新增页面交互通过；详见 docs/testing/five-extensions-acceptance.md。
- 增量升级采用带只读预检、阶段记录和索引形状验证的 Java 升级器及 PS5.1 包装器，未使用不可重试的一次性 ALTER SQL 文件。
- Git HEAD 仍为 bf1d7d9，索引未暂存；真实业务库和业务容器未变更。源码、测试、升级和使用文档均留本工作区。
