# 挂号系统稳定性与功能完善 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复当前可达挂号流程的编码、路由、输入校验、页面交互和权限反馈问题，并以自动化测试保证回归稳定。

**Architecture:** 保留 Servlet + Service + Mapper + MyBatis 的分层。公共 HTTP 响应由过滤器与既有 `AppointmentServletSupport` 等辅助类统一；页面只通过应用上下文构造 API/导航地址。以患者、医生、管理员三条端到端流程为边界修复可达模板，历史页面不删除，但不再作为主业务入口。

**Tech Stack:** Java 8、Servlet 3.1、MyBatis、Jackson、JSP/HTML/CSS/JavaScript、Maven、JUnit 4。

**Spec:** `docs/superpowers/specs/2026-10-20-hospital-system-stabilization.md`

## Global Constraints

- 仅修改 `C:\Users\ivanr\Desktop\Hospital_system_new\Hospital_system`。
- 不执行 Git commit、push、reset、rebase 或修改远程配置。
- 所有 JSON 响应为 `application/json;charset=UTF-8`；所有输入在进入 Service 前校验。
- 保持既有预约状态机、病历定稿约束、认证/CSRF/审计边界及数据库兼容性。
- 不用删除遗留页面或接口替代修复；仅处理主流程可达的遗留入口和模板占位跳转。

## Review Focus

- 直接请求 `/find` 时未提交 `keyword`，应返回 400 JSON，不能得到 500 或乱码。
- 响应过滤器不得在下游 Servlet 已写入二进制内容后覆盖 Content-Type；HTML 和文件上传须维持原行为。
- 根路径不是 `/ustbhospital` 时，患者页面的 API 与跳转链接仍需保留应用上下文。
- 未登录、角色越权和 CSRF 缺失分别保持 401、403 和 403，且错误体 UTF-8 可读。
- 预约已取消、已改期、无排班和医生非本人预约等状态，页面必须展示业务提示而不是静默失败或原始 JSON。

---

### Task 1: 统一 UTF-8 与搜索接口输入契约

**Files:**
- Modify: `src/main/java/com/ustb/hospital/filter/EncodingFilter.java`
- Modify: `src/main/java/com/ustb/hospital/servlet/SearchServlet.java`
- Modify: `src/test/java/com/ustb/hospital/filter/EncodingFilterTest.java`
- Create: `src/test/java/com/ustb/hospital/servlet/SearchServletContractTest.java`

**Interfaces:**
- Consumes: Servlet `Filter`、`HttpServletRequest`、`HttpServletResponse` 与 Jackson `ObjectMapper`。
- Produces: 所有下游响应在未被显式定义时具有 UTF-8；`GET /find?keyword=...` 的 JSON 搜索响应以及无效关键字的 400 JSON 错误体。

- [ ] **Step 1: 为响应编码补充失败测试**

在 `EncodingFilterTest` 中断言过滤链执行前，响应字符编码为 UTF-8；若 Servlet 已明确设置 `application/json;charset=UTF-8`，过滤器不改变其类型。

- [ ] **Step 2: 运行编码测试确认现状不满足新增断言**

Run: `mvn -Dtest=EncodingFilterTest test`
Expected: FAIL，现有过滤器未设置响应编码。

- [ ] **Step 3: 为搜索缺参和 UTF-8 JSON 编写失败测试**

在 `SearchServletContractTest` 中用 mock request/response 验证：空白/缺失 `keyword` 返回 HTTP 400、`application/json;charset=UTF-8` 和 `INVALID_ARGUMENT`；合法关键字仍从 Service 返回 JSON。

- [ ] **Step 4: 实现最小公共修复**

在 `EncodingFilter.doFilter` 链调用前设置 `request.setCharacterEncoding("UTF-8")` 与 `response.setCharacterEncoding("UTF-8")`，不强制覆盖 Content-Type。修改 `SearchServlet.doGet`，将空白 `keyword` 作为 400 JSON 返回，正常分支使用 `application/json;charset=UTF-8`。

- [ ] **Step 5: 运行本任务测试**

Run: `mvn -Dtest=EncodingFilterTest,SearchServletContractTest test`
Expected: PASS。

### Task 2: 患者发现与预约页面的可达路由修复

**Files:**
- Modify: `src/main/webapp/hospro/index.html`
- Modify: `src/main/webapp/hospro/doctor-directory.html`
- Modify: `src/main/webapp/hospro/book-appointment.html`
- Modify: `src/main/webapp/hospro/appointment-records.html`
- Modify: `src/test/java/com/ustb/hospital/web/CoreRegistrationNavigationTest.java`
- Modify: `src/test/java/com/ustb/hospital/web/PatientAppointmentEntryTest.java`
- Modify: `src/test/java/com/ustb/hospital/web/PatientRescheduleInteractionContractTest.java`

**Interfaces:**
- Consumes: `/find`、`/doc/findByDeptId`、`/appointments/*`、`/medical-records/patient` 与 `/security/csrf`。
- Produces: 以应用上下文为基础的科室搜索、医生目录、预约登记、改期/取消与病历查看页面导航；页面显示统一错误提示。

- [ ] **Step 1: 为主流程中的占位/归档跳转写失败静态契约**

扩展 `CoreRegistrationNavigationTest` 与 `PatientAppointmentEntryTest`：断言患者入口和提交控件不导航到 `archive/index.html`、`index.html?roleCode` 或 `javascript:void(0)`；断言 API 地址通过应用上下文构造。

- [ ] **Step 2: 为预约状态提示写失败页面契约**

扩展 `PatientRescheduleInteractionContractTest`：断言无排班、已取消、已改期和 CSRF/权限错误均映射为中文可读提示；时段选择包含 AM/PM/EVENING 三个显式选项，不依赖滚动选择器。

- [ ] **Step 3: 修复患者页面中的真实可达链接与错误处理**

统一四个页面的 `appContext/appUrl` 使用；将可达的模板链接指向患者中心、医生目录或业务页面；提交失败时解析 JSON `message` 并保留当前页面上下文；保留现有预约 API 的 HTTP 方法和 CSRF 请求头。

- [ ] **Step 4: 运行患者 Web 契约测试**

Run: `mvn -Dtest=CoreRegistrationNavigationTest,PatientAppointmentEntryTest,PatientRescheduleInteractionContractTest,AppointmentAvailabilityContractTest,BookingPageSecurityContractTest test`
Expected: PASS。

### Task 3: 医生排班、接诊与病历页面闭环

**Files:**
- Modify: `src/main/webapp/doctor-schedule/date.html`
- Modify: `src/main/webapp/doctor-schedule/appointments.html`
- Modify: `src/main/webapp/doctor-schedule/encounter.html`
- Modify: `src/main/java/com/ustb/hospital/servlet/DoctorAppointmentDetailServlet.java`（仅当 Task 3 测试证明响应或归属校验缺口时）
- Modify: `src/test/java/com/ustb/hospital/web/DoctorAppointmentPageTest.java`
- Modify: `src/test/java/com/ustb/hospital/web/MedicalRecordPageContractTest.java`
- Modify: `src/test/java/com/ustb/hospital/servlet/DoctorAppointmentDetailTest.java`

**Interfaces:**
- Consumes: `/schedules/*`、`/appointments/doctor`、`/appointments/start`、`/medical-records/doctor`、`/medical-records/draft`、`/medical-records/finalize`。
- Produces: 医生仅可操作本人待接诊预约；病历加载、草稿保存、定稿与医嘱表单的可读状态反馈。

- [ ] **Step 1: 为医生端路由和状态提示补充失败页面测试**

在 `DoctorAppointmentPageTest` 断言操作链接携带预约编号且不使用占位导航；在 `MedicalRecordPageContractTest` 断言页面将 401/403/404/409 映射为业务文字，不显示原始 JSON。

- [ ] **Step 2: 为医生预约详情错误边界补充失败 Servlet 测试**

在 `DoctorAppointmentDetailTest` 覆盖非法 `appointmentId`、非本人预约、已结束预约三种路径，分别断言 400、403 或 409 与 UTF-8 JSON。

- [ ] **Step 3: 实现医生端最小修复**

修复三个页面的应用上下文、空状态、时段/预约状态显示与错误信息；若测试暴露 Servlet 漏洞，使用 `AppointmentServletSupport` 的统一响应方法补齐校验，而不复制 JSON 拼接逻辑。

- [ ] **Step 4: 运行医生端测试**

Run: `mvn -Dtest=DoctorAppointmentPageTest,MedicalRecordPageContractTest,DoctorAppointmentDetailTest,MedicalDraftEndpointTest,MedicalRecordsEndpointContractTest,MedicalDraftLifecycleTest test`
Expected: PASS。

### Task 4: 管理员审计入口、遗留 JSON 端点和可达模板治理

**Files:**
- Modify: `src/main/webapp/hospro/audit-logs.html`
- Modify: `src/main/java/com/ustb/hospital/servlet/DoctorsSaveServlet.java`
- Modify: `src/main/java/com/ustb/hospital/servlet/MenuCategoryServlet.java`
- Modify: `src/main/java/com/ustb/hospital/servlet/MenuListServlet.java`
- Modify: 由静态扫描证明可从主菜单到达且含占位跳转的页面
- Modify: `src/test/java/com/ustb/hospital/servlet/AuditQueryEndpointTest.java`
- Create: `src/test/java/com/ustb/hospital/web/ReachablePageLinkContractTest.java`

**Interfaces:**
- Consumes: `/audit-logs`、`/audit-logs/query`（如存在）、管理员 Session 与前端 fetch/jQuery。
- Produces: 管理员可读审计列表及筛选/空状态；遗留 JSON Servlet 标准 Content-Type；可达页面无归档占位跳转。

- [ ] **Step 1: 为审计页面和遗留 JSON 类型建立失败测试**

在 `AuditQueryEndpointTest` 断言未登录 401、非管理员 403、管理员成功 200 的 Content-Type 均为 UTF-8 JSON；新增 `ReachablePageLinkContractTest` 仅扫描主菜单可达页面，断言无 `archive/index.html`、空 `href` 或无用途 `javascript:void(0)` 导航。

- [ ] **Step 2: 运行这些测试确认遗留问题**

Run: `mvn -Dtest=AuditQueryEndpointTest,ReachablePageLinkContractTest test`
Expected: FAIL，至少暴露不规范 JSON 类型或可达占位链接。

- [ ] **Step 3: 实现管理员和遗留入口修复**

将审计页请求、错误和空结果统一为可读界面；将被测试覆盖的 JSON Servlet 改为 `application/json;charset=UTF-8`；逐个将主菜单可达页面中的占位链接替换为实际路由，无法提供业务功能的控件改为禁用且标注“暂未开放”。

- [ ] **Step 4: 运行管理员与安全回归测试**

Run: `mvn -Dtest=AuditQueryEndpointTest,AuditQueryTest,SecurityBoundaryContractTest,AuditFailureCoverageTest,ReachablePageLinkContractTest test`
Expected: PASS。

### Task 5: 全量验证与交付前检查

**Files:**
- Modify: 仅 Task 1–4 确认需要的源文件和测试文件。

**Interfaces:**
- Consumes: 全部项目测试、Maven WAR 打包。
- Produces: 可由用户自行审查、提交的未提交工作区；`target/ustbhospital.war` 构建产物。

- [ ] **Step 1: 运行完整测试套件**

Run: `mvn test`
Expected: 所有可运行测试通过；需要真实 MySQL 的集成测试明确显示 skipped，而非失败。

- [ ] **Step 2: 构建 WAR**

Run: `mvn package -DskipTests`
Expected: `target/ustbhospital.war` 生成且 Maven BUILD SUCCESS。

- [ ] **Step 3: 审查修改范围**

Run: `git diff --check; git status --short`
Expected: 无空白错误；改动仅在本仓库，且没有提交、推送或改写 Git 历史。

## Self-Review

- Spec coverage: Task 1 覆盖编码和输入契约；Task 2 覆盖患者发现/预约；Task 3 覆盖医生排班/接诊/病历；Task 4 覆盖管理员审计和遗留模板；Task 5 覆盖完整构建验证。
- Type consistency: 使用现有 Servlet URL 与 `AppointmentServletSupport`，不引入与既有实体冲突的新接口。
- Review Focus coverage: 五项风险分别在 Task 1（缺参、编码）、Task 2（上下文、预约状态）、Task 3/4（401/403/CSRF 和页面呈现）中有明确测试。
- Proportion: 计划只确定任务边界、精确文件、接口与可验证命令，不复制实现代码。
