# 病历草稿与审计补全 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在保留现有挂号功能和数据的基础上，补齐病历草稿及完成规则、管理员审计查询和关键业务成功日志的事务一致性。

**Architecture:** 先建立可测试的审计上下文与同事务写入能力，再以增量迁移扩展病历状态，在服务端事务中实现草稿与正式提交；随后覆盖预约、排班、账号等写入路径，最后补齐管理员查询页面和发布验证。审计失败回滚业务，拒绝和失败事件单独记录并在日志库不可用时告警。

**Tech Stack:** Java 8、Servlet 3.1、MyBatis 3.2.2、JUnit 4、H2（快速隔离测试）、MySQL 8.0.19（兼容性及真实事务验证）、Tomcat 9、Maven WAR、原生 JavaScript。

**Spec:** `docs/superpowers/specs/2026-10-17-clinical-records-audit-completion-design.md`

## Global Constraints

- 不重建或清空数据库，不改写旧迁移，不删除既有数据，不覆盖既有未提交改动；不操作用户现用数据库、WAR 或 Tomcat。
- 每个行为先写测试、运行并观察预期失败，再实现、运行并观察通过；保留用户原有测试并在最后运行完整测试集。
- 不在审计日志、审计页面或测试输出中输出密码、哈希、Token、身份证号、病历或医嘱正文；医生和患者的授权病历页面仍须显示相应临床内容。
- 关键数据库写操作与成功审计必须共用非自动提交的 `SqlSession`；事务回滚时失败日志由独立连接记录。
- 认证、资源归属、状态、CSRF 和并发冲突均在服务端强制；保留 `/audit-logs?limit=...` 既有返回契约。
- 真实 MySQL 集成测试只能使用显式指定、独立且可识别的测试库，不默认使用 `hospt`；不得凭空声称 H2 等同 MySQL。
- 不主动提交 Git、不对现用数据库执行迁移、不部署或重启服务；先交付脚本、构建结果和验证报告。经验证的隔离测试库可执行测试所需迁移。

## Review Focus

1. 草稿被患者通过列表、详情或医嘱间接读取：任务 3 分别验证这些路径返回不可访问，历史 `FINAL` 仍可读取。
2. 两个医生窗口对同一草稿连续保存或提交：任务 3 和 4 验证版本冲突为 409、数据库及页面未提交内容不被覆盖。
3. 认证或 CSRF 过滤器提前返回、异常未设置 HTTP 状态：任务 5 验证失败日志记录一次，不误记成功。
4. 新增预约或排班主键产生后审计插入失败：任务 2、5 验证业务变更和成功日志一起回滚，重试成功后的目标编号为实际主键。
5. 迁移中断或重复执行、历史病历无诊疗方案：任务 1 和 7 验证迁移重跑不丢数据、历史保持可见，发布文档注明 DDL 回退限制。

## File Structure

- 增量迁移：`database/migrations/20261017_clinical_drafts_and_audit_query.sql`。
- 审计及权限：`entity/AuditLog.java`、`mapper/AuditLogsMapper.java` 与对应 XML、`service/AuditService.java`、新增 `service/AuditContext.java`，以及相关过滤器、Servlet 和业务 Service。
- 病历：`entity/MedicalRecord.java`、`mapper/MedicalRecordsMapper.java` 与对应 XML、`service/MedicalRecordsServiceImpl.java`、`service/MedicalRecordOperationResult.java`、`servlet/MedicalRecordsServlet.java`、`doctor-schedule/encounter.html`。
- 查询：新增 `service/AuditQuery.java` 和 `service/AuditQueryResult.java`，扩展 `servlet/AuditLogsServlet.java` 与 `hospro/audit-logs.html`。
- 测试：在现有 `src/test/java/com/ustb/hospital/{database,service,servlet,security,web}` 下新增对应 JUnit 测试；在 `tests/` 下扩展 Node UI 测试；新增隔离数据库测试工具及操作文档。

---

### Task 1: 增量迁移与历史数据兼容

**Files:**
- Create: `database/migrations/20261017_clinical_drafts_and_audit_query.sql`
- Create: `src/test/java/com/ustb/hospital/database/ClinicalDraftMigrationTest.java`
- Modify: `src/main/java/com/ustb/hospital/entity/MedicalRecord.java`
- Modify: `src/main/resources/com/ustb/hospital/mapper/MedicalRecordsMapper.xml`

**Interfaces:**
- Consumes: 现有 `medical_records`、`medical_orders`、`audit_logs` 表。
- Produces: `MedicalRecord.getRecordStatus()/setRecordStatus(String)`、`getVersion()/setVersion(int)`、`getFinalizedAt()/setFinalizedAt(String)`；列 `record_status`、`version`、`finalized_at`。

- [ ] 新增 `migrationKeepsHistoricalRecordFinal`、`migrationCanRunTwiceWithoutDataLoss` 和 `emptyDiagnosisDraftCanBeStored`；分别断言旧病历为 `FINAL` 且原内容不变、两次执行后结构与数据一致、`DRAFT` 允许空诊断。
- [ ] 运行 `mvn -Dtest=ClinicalDraftMigrationTest test`，确认缺少迁移/字段导致预期失败；MySQL 方言测试不能用 H2 代替。
- [ ] 实现 MySQL 8.0.19 兼容、通过 `information_schema` 检查的增量迁移：`record_status VARCHAR(10) NOT NULL DEFAULT 'FINAL'`、`version INT NOT NULL DEFAULT 0`、`finalized_at TIMESTAMP NULL`，`diagnosis` 允许 `NULL`；保留旧唯一约束及数据，不伪造旧时间。新增查询索引前检查是否存在。
- [ ] 运行 `mvn -Dtest=ClinicalDraftMigrationTest test` 和隔离 MySQL 迁移测试，确认通过；若隔离 MySQL 未配置则报告真实测试未运行，不连接现用库。

### Task 2: 审计事务接口及失败告警

**Files:**
- Create: `src/main/java/com/ustb/hospital/service/AuditContext.java`
- Modify: `src/main/java/com/ustb/hospital/service/AuditService.java`
- Modify: `src/main/java/com/ustb/hospital/mapper/AuditLogsMapper.java`、对应 XML
- Create: `src/test/java/com/ustb/hospital/service/AuditTransactionTest.java`

**Interfaces:**
- Consumes: 现有 `AuditLog`、`AuditLogsMapper.insert(AuditLog)`、`SqlSession`。
- Produces: 不可变 `AuditContext(String actorRole, Integer actorId, String requestMethod, String requestPath, String remoteAddress)`；`AuditService.writeSuccess(SqlSession session, AuditContext context, String action, String targetType, String targetId)`，写失败抛出异常且不自行提交；`AuditService.recordFailure(AuditContext context, String action, String targetType, String targetId)`，独立记录，失败时输出脱敏服务端错误。

- [ ] 新增 `successAuditRollsBackWithBusiness`、`failedAuditInsertPreventsCommit`、`failureAuditSurvivesBusinessRollback`、`failureSinkLogsNoSensitivePayload`：断言同事务回滚无业务变更和成功事件；独立失败事件可保存；日志库不可用时告警但不输出秘密。
- [ ] 运行 `mvn -Dtest=AuditTransactionTest test`，确认失败来自缺少新接口或事务行为。
- [ ] 实现上下文白名单和上述两个写入接口；目标 ID 仅允许受控业务标识；事务接口不捕获并吞掉写入异常、不打开第二连接、不调用 `commit()`。
- [ ] 运行 `mvn -Dtest=AuditTransactionTest,AuditLogContractTest test`，确认通过；验证 H2 测试使用独立库。

### Task 3: 病历草稿与正式提交服务

**Files:**
- Modify: `src/main/java/com/ustb/hospital/mapper/MedicalRecordsMapper.java`、对应 XML
- Modify: `src/main/java/com/ustb/hospital/service/MedicalRecordsServiceImpl.java`、`MedicalRecordOperationResult.java`
- Create: `src/test/java/com/ustb/hospital/service/MedicalDraftLifecycleTest.java`

**Interfaces:**
- Consumes: 任务 1 病历字段和任务 2 `AuditContext` / `writeSuccess`。
- Produces: `MedicalRecordOperationResult saveDraft(int doctorId, int appointmentId, String diagnosis, String treatmentPlan, String medicalAdvice, List<MedicalOrder> orders, Integer expectedVersion, AuditContext audit)`；`finalizeEncounter` 增加 `expectedVersion` 与 `audit` 参数，结果携带病历编号、状态和版本。首次创建传 `expectedVersion=null`，初始版本 `0`；更新传当前版本并递增。

- [ ] 新增 `draftSaveDoesNotCompleteAppointment`、`patientCannotReadDraftOrOtherPatientRecord`、`doctorCannotEditOtherDoctorsAppointment`、`staleDraftCannotOverwriteNewerVersion`、`finalRequiresDiagnosisAndTreatmentPlan`、`finalizationAndAuditRollbackTogether`、`finalRecordCannotBeOverwritten`；断言状态、版本、医嘱、返回码和日志与设计一致。
- [ ] 运行 `mvn -Dtest=MedicalDraftLifecycleTest test`，确认未实现草稿及新校验导致预期失败。
- [ ] 在单事务中锁定预约和病历，校验归属与 `IN_PROGRESS`，按版本写入或替换草稿及其医嘱；患者查询的列表与详情 SQL 都加入 `record_status='FINAL'`；完成提交时同事务保存 `FINAL`、医嘱、`COMPLETED` 与成功审计。
- [ ] 明确空草稿直提、已有草稿缺失/过期版本、空白诊断/诊疗方案、`FINAL` 重复提交和无效医嘱的返回码；删除或限制任何可绕过正式病历的直接完成路径。
- [ ] 运行 `mvn -Dtest=MedicalDraftLifecycleTest,MedicalRecordLifecycleContractTest test`，确认通过；使用真实事务验证保存/提交故障不会留下部分数据。

### Task 4: 草稿接口、CSRF 和医生页面

**Files:**
- Modify: `src/main/java/com/ustb/hospital/servlet/MedicalRecordsServlet.java`、`src/main/java/com/ustb/hospital/filter/CsrfFilter.java`
- Modify: `src/main/webapp/doctor-schedule/encounter.html`
- Create: `src/test/java/com/ustb/hospital/servlet/MedicalDraftEndpointTest.java`
- Modify: `tests/doctor-encounter-ui.cjs`

**Interfaces:**
- Consumes: 任务 3 `saveDraft`、扩展后的 `finalizeEncounter`。
- Produces: `POST /medical-records/draft` 和扩展的 `POST /medical-records/finalize`；草稿成功返回 `success`、`code`、`message`、`medicalRecordId`、`recordStatus`、`version`；版本冲突返回 HTTP 409。

- [ ] 新增 `draftEndpointRequiresDoctorAndCsrf`、`draftReturnsVersionAndConflict409`；UI 测试断言草稿按钮允许空诊断、重新打开恢复内容与医嘱、冲突后保留输入并提示。
- [ ] 运行 `mvn -Dtest=MedicalDraftEndpointTest test` 与 `node tests/doctor-encounter-ui.cjs`，确认预期失败。
- [ ] 实现 Session 身份、CSRF、参数/版本校验；页面获取并提交 Token，独立绑定草稿/正式按钮，恢复保存的数据，并在服务错误或冲突时保留表单输入。
- [ ] 运行上述测试及 `mvn -Dtest=MedicalRecordsEndpointContractTest,MedicalRecordPageContractTest test`，确认通过。

### Task 5: 关键动作事务审计与入口盘点

**Files:**
- Modify: `src/main/java/com/ustb/hospital/service/AppointmentsServiceImpl.java`、`DatesServiceImpl.java`、账号相关 Service 与对应 Mapper
- Modify: `src/main/java/com/ustb/hospital/mapper/AppointmentsMapper.java`、对应 XML 和各业务 Servlet
- Modify: `src/main/java/com/ustb/hospital/filter/AuditFilter.java`、`AuthenticationFilter.java`、`CsrfFilter.java`，必要时调整 `WEB-INF/web.xml` 的过滤顺序
- Modify: `src/main/java/com/ustb/hospital/servlet/LoginServlet.java`、`PasswordResetServlet.java` 及实际存在的退出、管理员敏感写入口
- Create: `src/test/java/com/ustb/hospital/service/BusinessAuditAtomicityTest.java`、`src/test/java/com/ustb/hospital/security/AuditFailureCoverageTest.java`
- Create: `docs/operations/audited-entrypoints.md`

**Interfaces:**
- Consumes: 任务 2 `AuditContext` / `writeSuccess` / `recordFailure`；任务 3 医疗成功事务。
- Produces: 业务 Servlet 从服务端 Session 构建 `AuditContext` 并传入对应写 Service；新增预约以映射实体和数据库生成主键获得真实 ID；`docs/operations/audited-entrypoints.md` 列出路径、角色、操作、审计方式和旧入口处置。

- [ ] 先枚举实际 `@WebServlet`、旧写路径、过滤器和 Service 调用；标记数据库事务可用与纯 Session 操作。对不可安全支持的旧写路径，保留原只读功能并拒绝绕过路径，不新增无权限写入口。
- [ ] 新增 `bookingAuditFailureRollsBackSlotAndAppointment`、`rescheduleAuditFailureRollsBackBothAppointmentsAndSlots`、`scheduleAuditFailureRollsBackSchedule`、`passwordAuditFailureRollsBackPasswordChange`、`authAndCsrfDenialAuditOnce`、`uncaughtExceptionIsFailureNotSuccess`、`successfulCreateUsesGeneratedTargetId`；使用显式故障注入和数据库断言，不靠源码字符串冒充事务验证。
- [ ] 运行 `mvn -Dtest=BusinessAuditAtomicityTest,AuditFailureCoverageTest test`，确认缺失事务及失败覆盖造成预期失败。
- [ ] 预约、取消、改期、开始接诊、排班增改删及改密等数据库更新都使用同一 `SqlSession` 的 `writeSuccess`；生成主键使用 MyBatis `useGeneratedKeys` 回填实体。改期记录可关联新旧编号，不存临床正文。每个业务成功事件只写一次，避免 `AuditFilter` 重复成功记录。
- [ ] 明确固定过滤器顺序并验证未登录、角色拒绝、CSRF 拒绝、业务失败与异常均可记录失败；失败日志连接故障时写脱敏告警。登录成功/失败、旧密码升级和实际存在的退出入口说明会话和数据库事务的保证边界。
- [ ] 运行 `mvn -Dtest=BusinessAuditAtomicityTest,AuditFailureCoverageTest,AppointmentLifecycleContractTest,SecurityBoundaryContractTest test`；对受影响的旧入口执行角色、CSRF 和可用性回归。

### Task 6: 管理员筛选与分页查询

**Files:**
- Create: `src/main/java/com/ustb/hospital/service/AuditQuery.java`、`AuditQueryResult.java`
- Modify: `src/main/java/com/ustb/hospital/service/AuditService.java`、`mapper/AuditLogsMapper.java`、对应 XML
- Modify: `src/main/java/com/ustb/hospital/servlet/AuditLogsServlet.java`、`filter/AuthenticationFilter.java`
- Modify: `src/main/webapp/hospro/audit-logs.html`
- Create: `src/test/java/com/ustb/hospital/servlet/AuditQueryEndpointTest.java`、`src/test/java/com/ustb/hospital/service/AuditQueryTest.java`、`tests/audit-query-ui.cjs`

**Interfaces:**
- Consumes: 现有 `audit_logs` 和任务 2 的安全日志数据。
- Produces: `GET /audit-logs/search` 参数 `start`、`end`（`yyyy-MM-dd HH:mm:ss`，起始包含、结束不包含）、`role`、`actorId`、`action`、`outcome`、`page`、`pageSize`；返回 `{rows,total,page,pageSize}`，默认第 1 页、20 条，单页最多 100 条。`AuditService.search(AuditQuery query)` 返回 `AuditQueryResult`；旧 `GET /audit-logs?limit=...` 继续返回原有列表。

- [ ] 新增 `filtersCanBeCombinedAndPaginationStable`、`invalidRangeOrPageReturns400`、`anonymous401AndWrongRole403`、`auditPageRequiresAdmin`、`queryFailureIsNotEmptySuccess`、`legacyLimitStillReturnsList`；UI 测试断言筛选、清空、翻页、空结果和错误状态，以及转义含 HTML 的安全元数据。
- [ ] 运行 `mvn -Dtest=AuditQueryTest,AuditQueryEndpointTest test` 与 `node tests/audit-query-ui.cjs`，确认预期失败。
- [ ] 使用 MyBatis 参数化条件查询与数量查询，按 `created_at DESC, audit_log_id DESC` 稳定排序；既能检索历史动作也能检索新动作。服务端校验输入，明确区分空结果与 5xx；新旧查询路径与审计页面均由服务端限制为管理员访问。
- [ ] 实现页面筛选和分页，用文本节点或安全转义渲染；运行上述测试及审计已有契约测试，确认通过。

### Task 7: 全量回归、隔离 MySQL 验证和交付文档

**Files:**
- Create: `docs/operations/clinical-drafts-audit-upgrade.md`
- Create: `src/test/java/com/ustb/hospital/database/ClinicalAuditMysqlIntegrationTest.java`
- Modify: 按实际情况补充 `tests/` 页面行为测试及必要测试配置。

**Interfaces:**
- Consumes: 任务 1–6 迁移、接口与入口清单。
- Produces: 可复现的升级前检查、增量迁移、重复执行验证、故障恢复与人工验收步骤；隔离 MySQL 集成测试仅在显式指定专用测试连接且通过库名安全检查后启用。

- [ ] 在 MySQL 8.0.19 隔离库测试旧数据迁移、迁移重跑、草稿并发、事务审计故障回滚、管理员筛选及真实生成主键。若缺少隔离库，标记跳过的具体测试和原因，不使用用户现用库。
- [ ] 运行 `mvn test`、`node tests/doctor-encounter-ui.cjs`、`node tests/department-search-ui.cjs`、`node tests/patient-appointment-ui.cjs`、`node tests/audit-query-ui.cjs`；记录每项真实结果，包括未通过或未运行项。
- [ ] 运行 `mvn package`，记录构建的 `target/ustbhospital.war`；只在工作区生成，不覆盖 Tomcat 部署。
- [ ] 编写升级文档：环境与测试库隔离、备份建议、SQL 执行顺序及校验、MySQL DDL 非事务性、空诊断草稿导致旧约束无法直接恢复、恢复方案、接口和权限矩阵、端到端测试及已知限制。
- [ ] 核对设计每条验收标准与真实测试证据，检查 `git diff` 和未跟踪文件是否保留用户修改；未验证的项目明确标注，不将计划或构建成功等同完整上线验收。
