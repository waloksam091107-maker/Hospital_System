# 挂号系统安全功能补全 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在不改变既有 OceanBase 真实业务数据、预约—接诊—病历—医嘱主链路及旧接口 410 防护的前提下，完成患者注册、本人资料、模拟充值、管理员基础数据与公告维护，并修复医生端真实排班入口和遗留菜单问题。

**Architecture:** 保持 Servlet + Service + MyBatis Mapper + JSP/HTML 的分层。历史不安全写接口仍由禁用过滤器返回 410；所有新写操作只通过新的 `/portal/...` 入口进入。请求身份只从 `SessionIdentity` 取得，Servlet 负责方法、角色和参数边界，Service 负责事务和业务校验，Mapper 只执行持久化，页面统一先获取 CSRF Token 再提交。

**Tech Stack:** Java 8、Servlet 3.x、MyBatis、Maven、Tomcat、OceanBase CE（MySQL 兼容模式）、HTML/JSP/原生 JavaScript、Docker Compose、JUnit 5。

**Spec:** `docs/superpowers/specs/2026-10-22-secure-feature-completion-design.md`

## Global Constraints

- 仅修改 `C:\Users\ivanr\Desktop\Hospital_system_new\Hospital_system`；不修改旧目录。
- 不实现短信验证码、邮件验证码、未登录找回密码或真实支付；已登录的修改密码入口维持现状。
- 不重新启用 `/accounts/register`、`/orders/*`、`/news/*` 等已停用的历史写入口；它们必须继续返回 410。
- 所有新写接口使用 POST、会话认证、角色校验和 CSRF；禁止接受或信任请求中的操作者 ID。
- 任何成功业务写入必须与成功审计处于同一 MyBatis 事务；错误写入失败审计，但不得记录密码、身份证号、病历和医嘱正文。
- 不执行 `docker compose down -v`、数据库重置或删除 OceanBase 卷；真实数据只能增量迁移或读取验证。
- 用户自行提交 Git：本计划的所有任务完成后均不得执行 `git add`、`git commit` 或 `git push`。

## Review Focus

- 修改 Mapper 时检查 MyBatis 参数名、事务提交/回滚、会话关闭与 OceanBase/MySQL 兼容性。
- 修改页面时检查 URL 是新安全入口、写请求携带 CSRF、错误消息可读且不泄漏异常。
- 修改过滤器或 `web.xml` 时检查过滤器顺序与新路由覆盖范围，且不影响现有预约/病历接口。
- 重点回归患者与医生跨账号越权、余额并发扣减、删除被引用基础数据、草稿病历可见性和审计完整性。

---

## Task 1: 建立新安全入口的公共授权、响应与审计基础，并修复医生排班入口

**Files:**
- Create: `src/main/java/com/ustb/hospital/servlet/PortalServletSupport.java`
- Modify: `src/main/java/com/ustb/hospital/utils/SessionIdentity.java`
- Modify: `src/main/webapp/WEB-INF/web.xml`
- Modify: `src/main/webapp/hospro/index.html`
- Modify: `src/main/webapp/hospro/static/js/legacy-navigation-guard.js`
- Create: `src/test/java/com/ustb/hospital/security/PortalSecurityBoundaryContractTest.java`
- Modify: `src/test/java/com/ustb/hospital/web/CoreRegistrationNavigationTest.java`

- [x] **Step 1: 先写失败测试。** 为新门户路径定义契约：未登录 401、非目标角色 403、写请求无 CSRF 403；断言旧不安全路径仍是 410。
- [x] **Step 2: 补充通用 Servlet 支持类。** 实现 JSON/页面错误响应、`requireRole`、从会话取得唯一当前用户 ID、严格解析正整数/金额/可选字符串的助手方法；任何失败分支通过现有 `AuditService` 记录脱敏失败事件。
- [x] **Step 3: 扩展会话身份工具。** 增加取得当前管理员 ID、当前角色和登录主体的无歧义访问方法，禁止各 Servlet 自行读取不一致的 session key。
- [x] **Step 4: 配置安全链。** 在 `web.xml` 为 `/portal/profile/*`、`/portal/balance/*`、`/portal/admin/*` 增加审计、认证和 CSRF 映射；保持编码→审计→认证→CSRF 的次序及既有映射不变。公开注册路径仅允许 POST 且自行执行必要的匿名输入验证。
- [x] **Step 5: 修复真实导航。** 将医生“排班表”改为具有有效 `href` 的实际链接，并在页面初始化时补上当前医生 `doctorId`；导航保护脚本只拦截明确的空/`#` 占位链接，不能拦截此链接。
- [x] **Step 6: 运行聚焦测试。** 执行新增安全契约和 `CoreRegistrationNavigationTest`，确认测试先失败后通过。

## Task 2: 补齐 OceanBase 增量结构与余额流水领域模型

**Files:**
- Create: `database/migrations/20261022_secure_portal_and_balance_transactions.sql`
- Modify: `database/oceanbase/01_hospt_schema.sql`
- Modify: `database/oceanbase/03_validate_schema.sql`
- Create: `src/main/java/com/ustb/hospital/entity/BalanceTransaction.java`
- Create: `src/main/java/com/ustb/hospital/mapper/BalanceTransactionsMapper.java`
- Create: `src/main/resources/com/ustb/hospital/mapper/BalanceTransactionsMapper.xml`
- Modify: `src/main/java/com/ustb/hospital/mapper/PatientsMapper.java`
- Modify: `src/main/resources/com/ustb/hospital/mapper/PatientsMapper.xml`
- Create: `src/test/java/com/ustb/hospital/database/BalanceTransactionSchemaContractTest.java`

- [x] **Step 1: 先写 DDL 契约测试。** 断言增量迁移与全新 OceanBase Schema 都声明 `balance_transactions`、金额精度、交易类型约束和患者/预约查询索引，且迁移为可重复执行的安全增量。
- [x] **Step 2: 写增量迁移。** 创建余额流水表与索引，不改写、删除或重建既有预约/患者数据；按项目现有日期迁移习惯加入必要的 `ALTER`/`CREATE IF NOT EXISTS`。
- [x] **Step 3: 更新新库 Schema 与验证 SQL。** 全量建库时获得同样的表、键和索引；验证脚本检查结构存在，不读取或输出敏感字段。
- [x] **Step 4: 建立实体与 Mapper。** 定义充值、预约扣费、预留退款三类流水的最小字段；新增患者行级锁定/按主键累加余额/读取当前余额的 Mapper 方法，避免继续使用 `MAX(id)+1` 和按身份证号写余额的旧实现。
- [x] **Step 5: 运行数据库契约测试。** 确认新迁移不会破坏现有 OceanBase schema contract 和 appointment/medical-records migrations。

## Task 3: 实现患者注册、本人资料和安全头像更新

**Files:**
- Create: `src/main/java/com/ustb/hospital/service/PortalAccountService.java`
- Create: `src/main/java/com/ustb/hospital/servlet/PortalPatientRegistrationServlet.java`
- Create: `src/main/java/com/ustb/hospital/servlet/PortalProfileServlet.java`
- Create: `src/main/java/com/ustb/hospital/servlet/PortalAvatarUploadServlet.java`
- Modify: `src/main/java/com/ustb/hospital/mapper/PatientsMapper.java`
- Modify: `src/main/resources/com/ustb/hospital/mapper/PatientsMapper.xml`
- Modify: `src/main/java/com/ustb/hospital/mapper/DoctorsMapper.java`
- Modify: `src/main/resources/com/ustb/hospital/mapper/DoctorsMapper.xml`
- Modify: `src/main/java/com/ustb/hospital/mapper/AdminsMapper.java`
- Modify: `src/main/resources/com/ustb/hospital/mapper/AdminsMapper.xml`
- Create: `src/main/webapp/hospro/portal-profile.html`
- Modify: `src/main/webapp/hospro/register-page.html`
- Modify: `src/main/webapp/hospro/my-profile.html`
- Create: `src/test/java/com/ustb/hospital/service/PortalAccountServiceTest.java`
- Create: `src/test/java/com/ustb/hospital/servlet/PortalProfileEndpointTest.java`
- Create: `src/test/java/com/ustb/hospital/web/PortalProfilePageContractTest.java`

- [ ] **Step 1: 先写服务与 Servlet 失败测试。** 覆盖仅患者可匿名注册、重复用户名/身份证号 409、医生和管理员不得从公开入口创建、资料读取不暴露密码散列、跨角色/跨用户修改被拒绝。
- [ ] **Step 2: 实现患者注册服务。** 使用密码散列、数据库唯一性和事务创建患者；在冲突时返回稳定业务错误，成功时记录 `PATIENT_REGISTER` 审计且不自动伪造登录身份。
- [ ] **Step 3: 实现资料读取和更新。** `GET /portal/profile` 返回当前会话主体可展示字段，`POST /portal/profile` 仅更新当前角色被允许的字段；患者不能修改余额，医生不能修改部门/职称，管理员不能凭前端 ID 修改其他管理员资料。
- [ ] **Step 4: 实现头像上传。** 仅接收限定大小的 PNG/JPEG/WEBP；根据服务端随机文件名和角色目录保存，拒绝路径穿越、伪造 MIME 和调用方 userId；更新头像与成功审计在同一事务边界内，失败不留半成品引用。
- [ ] **Step 5: 迁移页面。** 注册页只显示患者注册语义，资料页请求新接口、带 CSRF、展示中文字段级错误；保留旧资料写入口不可达，不向页面暴露其链接。
- [ ] **Step 6: 运行聚焦测试。** 执行账户、资料、页面契约和历史写入口测试。

## Task 4: 实现患者本人模拟充值及预约扣费流水一致性

**Files:**
- Create: `src/main/java/com/ustb/hospital/service/BalanceService.java`
- Create: `src/main/java/com/ustb/hospital/servlet/PortalBalanceRechargeServlet.java`
- Modify: `src/main/java/com/ustb/hospital/service/AppointmentsServiceImpl.java`
- Modify: `src/main/java/com/ustb/hospital/mapper/AppointmentsMapper.java`
- Modify: `src/main/resources/com/ustb/hospital/mapper/AppointmentsMapper.xml`
- Modify: `src/main/webapp/hospro/patients-pay.html`
- Modify: `src/main/webapp/hospro/patOrder.html`
- Create: `src/test/java/com/ustb/hospital/service/BalanceServiceTest.java`
- Modify: `src/test/java/com/ustb/hospital/service/AppointmentLifecycleContractTest.java`
- Create: `src/test/java/com/ustb/hospital/servlet/PortalBalanceRechargeEndpointTest.java`

- [ ] **Step 1: 先写失败测试。** 验证患者只能给自己充值、金额必须为正且在单次上限内、无 CSRF 403、成功后余额与流水的 `balance_after` 一致；预约成功需产生扣费流水，余额不足不生成预约或流水。
- [ ] **Step 2: 实现余额领域服务。** 在同一 SqlSession 中锁定患者行、计算金额、更新余额、写入流水和成功审计；所有异常 rollback，审计失败记录不得包含金额以外的私密业务正文。
- [ ] **Step 3: 将预约扣费接入流水。** 在现有预约创建事务成功扣费后写 `APPOINTMENT_DEBIT`；保留“取消预约不自动退款”的既有产品规则，并明确不产生退款流水。
- [ ] **Step 4: 实现充值 Servlet 与页面。** `GET/POST /portal/balance/recharge` 只允许 PATIENT；页面写明“模拟充值，不接入真实支付”，每次调用先获取/携带 CSRF，不再链接 `/patients/pay` 历史接口。
- [ ] **Step 5: 回归预约循环。** 运行余额、预约生命周期、并发余额/号源和页面预约测试。

## Task 5: 实现管理员基础数据安全 CRUD

**Files:**
- Create: `src/main/java/com/ustb/hospital/service/AdminMasterDataService.java`
- Create: `src/main/java/com/ustb/hospital/servlet/PortalAdminDoctorsServlet.java`
- Create: `src/main/java/com/ustb/hospital/servlet/PortalAdminPatientsServlet.java`
- Create: `src/main/java/com/ustb/hospital/servlet/PortalAdminDepartmentsServlet.java`
- Create: `src/main/java/com/ustb/hospital/servlet/PortalAdminProfessionalTitlesServlet.java`
- Modify: `src/main/java/com/ustb/hospital/mapper/DoctorsMapper.java`
- Modify: `src/main/resources/com/ustb/hospital/mapper/DoctorsMapper.xml`
- Modify: `src/main/java/com/ustb/hospital/mapper/PatientsMapper.java`
- Modify: `src/main/resources/com/ustb/hospital/mapper/PatientsMapper.xml`
- Modify: `src/main/java/com/ustb/hospital/mapper/DepartmentsMapper.java`
- Modify: `src/main/resources/com/ustb/hospital/mapper/DepartmentsMapper.xml`
- Create: `src/main/java/com/ustb/hospital/mapper/ProfessionalTitlesMapper.java`
- Create: `src/main/resources/com/ustb/hospital/mapper/ProfessionalTitlesMapper.xml`
- Create: `src/main/webapp/hospro/admin-master-data.html`
- Modify: `src/main/webapp/hospro/adm_main.jsp`
- Create: `src/test/java/com/ustb/hospital/service/AdminMasterDataServiceTest.java`
- Create: `src/test/java/com/ustb/hospital/servlet/PortalAdminMasterDataEndpointTest.java`

- [ ] **Step 1: 先写安全与约束测试。** 非管理员访问全部返回 403；管理员创建医生时生成唯一工号、密码散列而非明文；删除有预约/排班引用的医生、存在下级或医生引用的科室、被医生引用的职称均返回 409。
- [ ] **Step 2: 完成 Mapper 读写接口。** 为四类资源增加按主键读、唯一性查询、受关联约束的计数/删除和更新 SQL；使用数据库自动主键或安全的 generated keys，不使用 `MAX()+1`；修复历史 Mapper 中错误表名但不开放其旧入口。
- [ ] **Step 3: 完成服务层事务。** 管理员服务统一验证引用关系、重复数据和可编辑字段；医生/患者创建、更新、停用或删除的成功操作写审计，失败操作写脱敏失败审计。
- [ ] **Step 4: 完成 Servlet 路由。** 管理端列表使用 GET，新增/修改/删除使用 POST 的明确 `action`，每次从 Session 获取 ADMIN 身份；所有写操作经过 CSRF 过滤，不读取前端 actorId。
- [ ] **Step 5: 迁移管理页面。** 管理员主页链接新的基础资料页；页面提供医生、患者、科室树、职称的列表/新增/编辑/删除与中文冲突提示，不再发往被禁用的 `/save`、`/delete` 或旧部门/医生路径。
- [ ] **Step 6: 运行聚焦测试。** 执行管理服务、Servlet、权限边界、旧写接口 410 和 mapper 配置测试。

## Task 6: 实现管理员公告维护与公开已发布公告查询

**Files:**
- Modify: `database/migrations/20261022_secure_portal_and_balance_transactions.sql`
- Modify: `database/oceanbase/01_hospt_schema.sql`
- Modify: `src/main/java/com/ustb/hospital/entity/Announcements.java`
- Modify: `src/main/java/com/ustb/hospital/mapper/AnnouncementsMapper.java`
- Modify: `src/main/resources/com/ustb/hospital/mapper/AnnouncementsMapper.xml`
- Modify: `src/main/java/com/ustb/hospital/service/AnnouncementsServiceImpl.java`
- Create: `src/main/java/com/ustb/hospital/servlet/PortalAdminAnnouncementsServlet.java`
- Modify: `src/main/java/com/ustb/hospital/servlet/AnnouncementsListServlet.java`
- Create: `src/main/webapp/hospro/admin-announcements.html`
- Modify: `src/main/webapp/hospro/news.html`
- Create: `src/test/java/com/ustb/hospital/service/AnnouncementPublishingTest.java`
- Create: `src/test/java/com/ustb/hospital/servlet/PortalAdminAnnouncementsEndpointTest.java`

- [ ] **Step 1: 先写失败测试。** 验证匿名用户只能得到已发布公告，管理员可草稿/发布/下线/编辑/删除，非管理员写入 403，旧 `/news/add`、`/news/edit`、`/news/delete` 仍为 410。
- [ ] **Step 2: 增量扩展公告数据。** 以兼容已有 `announcement` 数据为目标，增加 `status`、`published_at`、`updated_at`、`creator_admin_id` 等可空/默认字段和查询索引，并为历史记录制定一次性安全默认发布策略。
- [ ] **Step 3: 实现 Mapper 和事务服务。** 使用参数化 SQL 完成列表、详情、保存、发布状态和删除；内容存储原文但页面输出按文本安全转义，不把管理端异常传给浏览器。
- [ ] **Step 4: 实现管理与公开接口。** 管理写操作在单事务中完成业务和审计；公开列表不需要会话，详情只允许读已发布项目。
- [ ] **Step 5: 更新页面。** 管理员公告页使用新入口、CSRF 和中文提示；门户公告页只展示已发布数据，删除到旧新闻写路径的任何活动入口。
- [ ] **Step 6: 运行公告及安全回归测试。** 执行新测试、历史禁用测试与现有公共新闻/页面测试。

## Task 7: 整体回归、Docker/OceanBase 增量验证与人工验收清单

**Files:**
- Modify: `README.md`
- Modify: `docs/operations/audited-entrypoints.md`
- Create: `docs/testing/secure-feature-completion-acceptance.md`
- Modify: `src/test/java/com/ustb/hospital/deploy/OceanBaseMigrationScriptContractTest.java`
- Modify: `src/test/java/com/ustb/hospital/web/ReachablePageLinkContractTest.java`

- [ ] **Step 1: 补充可运行说明。** 在 README 标明 OceanBase 增量迁移顺序、Docker 启动命令、演示账号位置、模拟充值边界和“不要删除数据卷”的警告；不写入 `.env.oceanbase` 密码。
- [ ] **Step 2: 更新审计入口文档。** 列出新增的注册、资料、头像、余额、基础资料、公告操作及其审计动作，明确成功事务审计和失败脱敏审计。
- [ ] **Step 3: 写端到端页面链接契约。** 扫描活跃页面，确保没有可点击的 `/orders/*`、已禁用写路径或 `alert(该功能暂未开放)` 作为已承诺菜单的最终行为。
- [ ] **Step 4: 运行完整自动化验证。** 在目标目录执行 `mvn test` 和 `mvn -DskipTests package`；保存失败输出、修复根因后重跑，不能仅依据旧结果宣称成功。
- [ ] **Step 5: 验证 OceanBase 数据安全。** 使用已有 `scripts/oceanbase/verify-migration.ps1` 及新增结构验证，在不导入覆盖、不删除卷的前提下确认数据行数、主键/关系与新表均正确。
- [ ] **Step 6: 验证 Docker 运行态。** 使用现有 `docker-compose.oceanbase.yml` 对 app 做增量 `up -d --build`，检查 health/logs 和 `/ustbhospital/`；不可执行清卷命令。
- [ ] **Step 7: 完成手工验收文档。** 按角色列出注册、资料、充值、预约、排班、接诊、病历/医嘱、管理 CRUD、公告、401/403/410、审计日志的可复现步骤和预期结果。
- [ ] **Step 8: 收尾检查。** 执行 `git status --short` 仅报告工作区改动，不暂存、不提交、不推送。

