# 权限、安全与审计 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 使用 BCrypt、统一的访问/CSRF 边界和脱敏审计降低挂号系统的账户与越权风险。

**Architecture:** `PasswordService` 封装哈希校验与旧密码升级；安全过滤器覆盖新的敏感接口；`AuditService` 把关键操作写入追加式 `audit_logs`。旧订单写入口被显式退役，以新医嘱接口取代。

**Tech Stack:** Java 8, Servlet 3.1, MyBatis 3.2, MySQL 8, jBCrypt.

**Spec:** `docs/specs/2026-10-14-security-and-audit.md`

## Global Constraints

- 不迁移真实数据库、不发布 WAR；只交付可审查脚本和构建产物。
- 不记录明文密码、哈希、完整病历正文、身份证号或诊断内容到审计日志。
- 迁移执行前不得将新代码部署到生产库。

## Review Focus

- 未升级的老账号只能在密码正确时升级，密码错误不得改写任何凭据。
- 过滤器白名单不能阻断登录、注册或公开医生/科室查询。
- 写请求的 CSRF 拒绝必须发生在业务写入之前。
- 审计写入失败不得掩盖原业务结果，但必须在文档中可观测。
- 已退役的旧 `/orders/*` 不能继续接受 GET 或 POST 写入。

### Task 1: BCrypt 凭据迁移与登录升级

**Files:**
- Modify: `pom.xml`
- Create: `database/migrations/20261014_password_hash_and_audit.sql`
- Create: `src/main/java/com/ustb/hospital/security/PasswordService.java`
- Modify: `src/main/java/com/ustb/hospital/entity/{Doctors,Patients,Admins}.java`
- Modify: corresponding Mapper interfaces/XML and `LoginServlet`, `AccountRegistrationServlet`, `PasswordResetServlet`
- Test: `src/test/java/com/ustb/hospital/security/PasswordServiceTest.java`

**Interfaces:** Produces `PasswordService.hash`, `matches`, `needsUpgrade`; mappers expose role-specific `upgradePasswordHash`.

- [ ] Write failing BCrypt and legacy-upgrade tests.
- [ ] Run focused test; expect missing PasswordService.
- [ ] Add dependency, migration and minimum login/registration/reset implementation.
- [ ] Run focused test and `mvn test`; expect pass.
- [ ] Commit with `feat: hash account passwords with bcrypt`.

### Task 2: 统一授权、CSRF 和旧订单退役

**Files:**
- Modify: `AuthenticationFilter.java`, `CsrfFilter.java`, `SessionIdentity.java`
- Create: `LegacyOrdersDisabledFilter.java`
- Modify: sensitive schedule/account servlet mappings as required
- Test: `src/test/java/com/ustb/hospital/servlet/SecurityBoundaryContractTest.java`

**Interfaces:** Sensitive appointment/schedule/medical routes require a session and write methods require CSRF; `/orders/*` returns 410.

- [ ] Write failing security boundary contracts.
- [ ] Run focused test; expect missing coverage/disabled filter.
- [ ] Implement only the stated route boundaries and JSON errors.
- [ ] Run focused test and `mvn test`; expect pass.
- [ ] Commit with `feat: enforce security boundary for clinical writes`.

### Task 3: 审计日志和管理员只读查询

**Files:**
- Create: `AuditLog.java`, `AuditLogsMapper.java`, `AuditLogsMapper.xml`, `AuditService.java`, `AuditLogsServlet.java`
- Modify: login, appointment, schedule and medical services/servlets to record safe metadata
- Create: `src/main/webapp/hospro/audit-logs.html`
- Test: `src/test/java/com/ustb/hospital/service/AuditLogContractTest.java`

**Interfaces:** Produces `AuditService.record(actorRole, actorId, action, targetType, targetId, outcome, request)` and GET `/audit-logs` for admins only.

- [ ] Write failing audit schema/content contracts, including redaction assertions.
- [ ] Run focused test; expect missing audit service.
- [ ] Implement append-only log, safe metadata and admin list endpoint/page.
- [ ] Run focused test and `mvn clean package`; expect WAR generation.
- [ ] Commit with `feat: audit critical registration operations`.
