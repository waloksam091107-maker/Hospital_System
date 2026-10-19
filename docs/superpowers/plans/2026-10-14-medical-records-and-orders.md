# 就诊记录与医嘱 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将接诊完成变成可追溯的就诊记录与医嘱事务，而非孤立的状态更新。

**Architecture:** 新建 `medical_records`/`medical_orders`，由 `MedicalRecordsServiceImpl` 在一个 MyBatis 事务内校验预约归属、写入记录与医嘱并完成预约。Servlet 仅从登录会话取得角色身份，静态页面经 JSON 接口读写。

**Tech Stack:** Java 8, Servlet 3.1, MyBatis 3.2, Jackson, MySQL 8, jQuery.

**Spec:** `docs/specs/2026-10-14-medical-records-and-orders.md`

## Global Constraints

- 不执行真实 MySQL 迁移或 Tomcat 部署；迁移脚本与源码一并交付。
- 不接受客户端传入的医生/患者 ID 作为授权依据。
- 正式记录以诊断非空为最低条件，预约只允许从 `IN_PROGRESS` 转为 `COMPLETED`。

## Review Focus

- 重复提交同一预约时，唯一键和状态条件必须保持记录与预约一致。
- 医嘱项目为空、项目字段超长和数量非法必须得到确定的业务错误。
- 患者读取他人记录与医生读取他人预约均必须被拒绝。
- 记录写入中间失败时不能留下已完成预约或孤立医嘱。
- 医生端旧“完成接诊”按钮不得继续调用直接完成接口。

### Task 1: 迁移、实体和 Mapper

**Files:**
- Create: `database/migrations/20261014_medical_records_and_orders.sql`
- Create: `src/main/java/com/ustb/hospital/entity/MedicalRecord.java`
- Create: `src/main/java/com/ustb/hospital/entity/MedicalOrder.java`
- Create: `src/main/java/com/ustb/hospital/mapper/MedicalRecordsMapper.java`
- Create: `src/main/resources/com/ustb/hospital/mapper/MedicalRecordsMapper.xml`
- Modify: `src/main/resources/mybatis-config.xml`
- Test: `src/test/java/com/ustb/hospital/service/MedicalRecordSchemaContractTest.java`

**Interfaces:** Produces `MedicalRecordsMapper.insertRecord`, `insertOrders`, `selectForDoctor`, `selectForPatient` and `selectByAppointmentForUpdate`.

- [ ] Write a failing schema contract test asserting appointment uniqueness, FK links and mapper registration.
- [ ] Run `mvn -Dtest=MedicalRecordSchemaContractTest test`; expect failure because schema and mapper do not exist.
- [ ] Add the migration, entities, Mapper/XML and MyBatis registration.
- [ ] Run the contract test and `mvn test`; expect pass.
- [ ] Commit with `feat: add medical record persistence`.

### Task 2: 事务化提交服务

**Files:**
- Create: `src/main/java/com/ustb/hospital/service/MedicalRecordsServiceImpl.java`
- Create: `src/main/java/com/ustb/hospital/utils/MedicalRecordOperationResult.java`
- Test: `src/test/java/com/ustb/hospital/service/MedicalRecordLifecycleContractTest.java`

**Interfaces:** Consumes Task 1 Mapper and `AppointmentsMapper.selectByIdForUpdate/updateStatusForDoctorIfCurrent`; produces `finalizeEncounter(int doctorId, int appointmentId, String diagnosis, String treatmentPlan, String advice, List<MedicalOrder>)`.

- [ ] Write a failing lifecycle contract test for diagnosis validation, status precondition, ownership and atomic completion contract.
- [ ] Run `mvn -Dtest=MedicalRecordLifecycleContractTest test`; expect failure because service is absent.
- [ ] Implement minimal validation and one-transaction write/rollback service.
- [ ] Run its test and `mvn test`; expect pass.
- [ ] Commit with `feat: finalize appointments with medical records`.

### Task 3: 授权 Servlet 和页面

**Files:**
- Create: `src/main/java/com/ustb/hospital/servlet/MedicalRecordsServlet.java`
- Create: `src/main/webapp/doctor-schedule/encounter.html`
- Create: `src/main/webapp/hospro/medical-records.html`
- Modify: `src/main/java/com/ustb/hospital/filter/AuthenticationFilter.java`
- Modify: `src/main/java/com/ustb/hospital/filter/CsrfFilter.java`
- Modify: `src/main/java/com/ustb/hospital/servlet/AppointmentCompletionServlet.java`
- Modify: `src/main/webapp/doctor-schedule/appointments.html`
- Test: `src/test/java/com/ustb/hospital/servlet/MedicalRecordsEndpointContractTest.java`
- Test: `src/test/java/com/ustb/hospital/web/MedicalRecordPageContractTest.java`

**Interfaces:** Consumes Task 2 service; exposes GET `/medical-records/doctor`, GET `/medical-records/patient` and POST `/medical-records/finalize`.

- [ ] Write failing endpoint/page contract tests for role-specific routes, CSRF protection and replacement of direct completion UI.
- [ ] Run the focused tests; expect missing servlet/pages or old direct-complete reference failure.
- [ ] Implement JSON requests/responses, role checks and the minimal doctor/patient views.
- [ ] Run focused tests and `mvn clean package`; expect pass and WAR generation.
- [ ] Commit with `feat: add encounter pages and medical record api`.
