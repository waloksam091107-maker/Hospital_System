# OceanBase 保留数据迁移 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将现有 MySQL 业务数据迁移至 Docker 中的 OceanBase MySQL 兼容租户，并使医院挂号系统在 OceanBase 上构建、启动和通过核心业务回归验证。

**Architecture:** 保留 Servlet、Service、Mapper 和 MyBatis 分层。增加 OceanBase JDBC 驱动和默认连接配置，创建与 MySQL 源脚本并存的 OceanBase 最终态 DDL、数据导出/导入/对账工具，以及由 OceanBase + Tomcat 组成的 Compose 拓扑。迁移只读取 MySQL 数据，OceanBase 验证通过前应用不切换。

**Tech Stack:** Java 8、Maven、Tomcat 9、MyBatis、OceanBase Connector/J、OceanBase Community Edition（MySQL 兼容模式）、Docker Compose、PowerShell、MySQL 8.0 客户端工具。

**Spec:** `docs/superpowers/specs/2026-10-21-oceanbase-data-preserving-migration-design.md`

## Global Constraints

- 仅修改 `C:\Users\ivanr\Desktop\Hospital_System\Hospital_System`。
- 不删除、清空、覆盖或修改现有 MySQL `hospt` 数据；源库只读导出。
- 不执行 Git commit、push、reset、rebase 或远程仓库配置。
- 密码只保存在被 Git 忽略的 `.env` 或交互式终端输入中，不写入源码、迁移脚本、Compose、日志或测试输出。
- OceanBase 使用 MySQL 兼容租户 `test`，业务数据库继续命名为 `hospt`，业务账号为 `hospital_app@test`。
- Docker Desktop 必须启动且至少向 Linux 容器分配 2 个 CPU、8GB 内存；镜像仅用于课程项目/测试环境。
- 应用最终运行时数据库连接必须为 OceanBase JDBC 驱动 `com.oceanbase.jdbc.Driver` 和 `jdbc:oceanbase:` URL。

## Review Focus

- 有效预约的唯一占号约束必须保持：同一医生、日期、时段不能同时存在两个 `BOOKED`、`IN_PROGRESS` 或 `COMPLETED` 预约。
- 导入必须保留已有主键、BCrypt 密码哈希、余额、预约链 `previous_appointment_id`、病历版本和审计时间，不能以演示数据覆盖。
- 所有外键依赖表必须按正确次序导入，且迁移后不存在孤儿预约、病历、医嘱或排班记录。
- 应用连接不得回落到默认的 MySQL `127.0.0.1:3306`；Docker 网络中的 OceanBase 启动完成前 Tomcat 不得尝试连接。
- 密钥、MySQL 密码、OceanBase 密码、导出的 SQL 文件及 OceanBase 数据卷映射目录均不得进入 Git 变更。

---

### Task 1: OceanBase JDBC 默认连接与本地秘密隔离

**Files:**
- Modify: `pom.xml`
- Modify: `src/main/java/com/ustb/hospital/utils/MybatisUtil.java`
- Modify: `database/database.env.example`
- Modify: `README.md`
- Modify: `.gitignore`
- Modify: `src/test/java/com/ustb/hospital/utils/MybatisConfigurationTest.java`
- Create: `src/test/java/com/ustb/hospital/utils/OceanBaseDefaultConfigurationTest.java`

**Interfaces:**
- Consumes: `HOSPITAL_DB_DRIVER`、`HOSPITAL_DB_URL`、`HOSPITAL_DB_USERNAME`、`HOSPITAL_DB_PASSWORD`。
- Produces: 运行时默认 OceanBase JDBC 配置；显式传入 MySQL/H2 测试配置时仍可覆盖默认值。

- [ ] **Step 1: 编写失败配置测试**

新增 `OceanBaseDefaultConfigurationTest`，使用反射调用 `MybatisUtil.databaseProperties()`，断言未设置环境/JVM 属性时：驱动是 `com.oceanbase.jdbc.Driver`、URL 以 `jdbc:oceanbase://` 开头、用户名为 `hospital_app@test`；设置 `hospital.db.*` 系统属性时应优先返回覆盖值。

- [ ] **Step 2: 运行测试确认当前默认 MySQL 配置不满足目标**

Run: `mvn -Dtest=OceanBaseDefaultConfigurationTest test`
Expected: FAIL，断言显示当前默认驱动或 URL 为 MySQL。

- [ ] **Step 3: 实现最小连接配置改动**

在 `pom.xml` 增加 `com.oceanbase:oceanbase-client:2.4.2`；修改 `MybatisUtil` 的默认驱动、URL 和用户名为 OceanBase MySQL 租户配置，同时保留 JVM 属性优先级。更新 `database.env.example`、README 和 `.gitignore`，忽略 `.env`、`database/oceanbase/.local/`、`database/oceanbase/export/` 和本地 OceanBase 持久化目录。

- [ ] **Step 4: 运行配置与 Mapper 解析测试**

Run: `mvn -Dtest=OceanBaseDefaultConfigurationTest,MybatisConfigurationTest test`
Expected: PASS。

### Task 2: OceanBase 最终态结构与约束校验脚本

**Files:**
- Create: `database/oceanbase/01_hospt_schema.sql`
- Create: `database/oceanbase/02_create_app_user.sql.template`
- Create: `database/oceanbase/03_validate_schema.sql`
- Create: `database/oceanbase/README.md`
- Create: `src/test/java/com/ustb/hospital/database/OceanBaseSchemaContractTest.java`

**Interfaces:**
- Consumes: `hospt` 数据库、OceanBase MySQL 兼容租户和现有实体/Mapper 的表字段契约。
- Produces: 覆盖当前业务最终态的结构：基础账号、排班、预约、病历、医嘱、审计表及索引/外键/预约唯一性约束。

- [ ] **Step 1: 编写失败的 DDL 契约测试**

新增 `OceanBaseSchemaContractTest`，读取 `01_hospt_schema.sql`，断言其中包含业务所需的 11 个核心表、`appointments` 的状态/号源唯一性设计、`medical_records.record_status/version/finalized_at`、`audit_logs` 查询索引，以及无 `ENGINE=InnoDB`、`PREPARE`、`AFTER` 或 MySQL 数据目录语句。

- [ ] **Step 2: 运行 DDL 契约测试确认目标文件缺失**

Run: `mvn -Dtest=OceanBaseSchemaContractTest test`
Expected: FAIL，目标 DDL 文件不存在。

- [ ] **Step 3: 实现 OceanBase 最终态 DDL**

将 MySQL 基础表和迁移后的最终字段合并为一次性 OceanBase 建表脚本。保留应用实际依赖的名称、主键和外键；使用 OceanBase MySQL 模式兼容的 `AUTO_INCREMENT`、时间戳、检查约束和索引。先尝试保留 `active_slot_key` 的生成列唯一索引；若实际 OceanBase 启动验证不支持，则改为与同样唯一约束等价的普通列/触发器并在后续 Task 5 为状态迁移补齐维护逻辑。

- [ ] **Step 4: 添加最小权限账号模板与结构验证 SQL**

在 `02_create_app_user.sql.template` 中使用占位符而非密码，定义 `hospital_app` 的创建和 `hospt.*` 最小业务权限；在 `03_validate_schema.sql` 中检查表、关键列、外键、索引和生成列/等价约束。

- [ ] **Step 5: 运行 DDL 契约测试**

Run: `mvn -Dtest=OceanBaseSchemaContractTest test`
Expected: PASS。

### Task 3: Docker Compose、应用镜像与健康等待

**Files:**
- Create: `Dockerfile`
- Create: `docker-compose.oceanbase.yml`
- Create: `.env.oceanbase.example`
- Create: `scripts/oceanbase/wait-oceanbase.ps1`
- Create: `src/test/java/com/ustb/hospital/deploy/OceanBaseComposeContractTest.java`

**Interfaces:**
- Consumes: OceanBase `oceanbase/oceanbase-ce` 单机镜像、项目 WAR、`.env` 中的本地秘密。
- Produces: `oceanbase` 和 `app` 服务；应用等待 `boot success!` 后才启动，并以 `jdbc:oceanbase://oceanbase:2881/hospt` 访问目标库。

- [ ] **Step 1: 为部署文件写失败静态契约测试**

新增 `OceanBaseComposeContractTest`，断言 Compose 存在 `oceanbase`、`app` 服务和持久化卷；应用环境中有 OceanBase URL/驱动，未引用 MySQL 镜像；`.env.oceanbase.example` 不包含真实密码；Dockerfile 使用 Tomcat 9，WAR 上下文为 `/ustbhospital`。

- [ ] **Step 2: 运行部署契约测试确认当前文件缺失**

Run: `mvn -Dtest=OceanBaseComposeContractTest test`
Expected: FAIL，Compose/Dockerfile 不存在。

- [ ] **Step 3: 实现容器文件与启动等待**

创建多阶段 Maven→Tomcat 9 Dockerfile；Compose 将 `database/oceanbase/01_hospt_schema.sql` 挂载到 OceanBase 初始化目录，设置 `OB_TENANT_PASSWORD`、`OB_DATABASE=hospt` 和本地卷；`app` 服务先调用 `wait-oceanbase.ps1` 或等价健康等待步骤，再启动 Tomcat。只暴露应用 8080 和为宿主验证保留的 OceanBase 2881；不启动 MySQL 容器。

- [ ] **Step 4: 运行部署契约与 Compose 配置检查**

Run: `mvn -Dtest=OceanBaseComposeContractTest test`
Expected: PASS。

Run: `docker compose --env-file .env.oceanbase -f docker-compose.oceanbase.yml config`
Expected: 成功展开服务配置且输出中不含实际密码。

### Task 4: 只读导出、可重复导入和数据对账工具

**Files:**
- Create: `scripts/oceanbase/migrate-from-mysql.ps1`
- Create: `scripts/oceanbase/verify-migration.ps1`
- Create: `scripts/oceanbase/migration-manifest.sql`
- Create: `src/test/java/com/ustb/hospital/deploy/OceanBaseMigrationScriptContractTest.java`

**Interfaces:**
- Consumes: 交互式 MySQL 源库密码、OceanBase `.env` 配置、当前源库 `SHOW TABLES` 结果。
- Produces: 被忽略的导出目录中的表清单、数据 SQL、源/目标行数与完整性报告；不向源 MySQL 执行 DDL/DML。

- [ ] **Step 1: 为迁移安全边界编写失败测试**

新增 `OceanBaseMigrationScriptContractTest`，静态检查迁移脚本只使用 `mysqldump --single-transaction --no-create-info` 和只读元数据/SELECT；禁止 `DROP DATABASE`、`TRUNCATE`、`DELETE FROM`、MySQL `ALTER/UPDATE/INSERT`。断言验收脚本比较源/目标表行数、预约/病历/医嘱/审计外键完整性及主键范围。

- [ ] **Step 2: 运行迁移脚本契约测试确认文件缺失**

Run: `mvn -Dtest=OceanBaseMigrationScriptContractTest test`
Expected: FAIL，迁移脚本不存在。

- [ ] **Step 3: 实现交互式只读导出与导入**

`migrate-from-mysql.ps1` 通过 `Read-Host -AsSecureString` 提示源 MySQL 密码，不回显、不持久化；先生成实际表清单和源行数清单，再导出数据（保留表顺序与主键），最后通过 OceanBase `obclient` 导入。目标导入失败时记录失败表并停止，不接触 MySQL。脚本在导入完成后创建 `hospital_app@test` 并授予 `hospt` 的最小业务权限。

- [ ] **Step 4: 实现对账脚本**

`verify-migration.ps1` 比较每张实际源表的行数、每张表的 MIN/MAX 主键；额外执行预约→排班、病历→预约、医嘱→病历、审计时间非空等完整性检查；对差异返回非零退出码。

- [ ] **Step 5: 运行迁移脚本契约测试**

Run: `mvn -Dtest=OceanBaseMigrationScriptContractTest test`
Expected: PASS。

### Task 5: 启动 OceanBase、迁移真实数据并切换应用

**Files:**
- Modify: `.env.oceanbase`（本地、Git 忽略）
- Generated, Git ignored: `database/oceanbase/.local/*`
- Generated, Git ignored: `database/oceanbase/export/*`

**Interfaces:**
- Consumes: 已启动的 Docker Desktop、用户在本地终端安全输入的 MySQL 密码、OceanBase 容器。
- Produces: 已导入实际 MySQL 数据的 OceanBase `hospt`，以及使用 `hospital_app@test` 连接的 Tomcat 应用。

- [ ] **Step 1: 生成本地 OceanBase 密钥文件**

生成高熵 OceanBase 租户和应用账号密码，写入 `.env.oceanbase`；该文件只在本地存在并被 `.gitignore` 排除。启动前确认 Docker Desktop 至少提供 2 CPU、8GB 内存。

- [ ] **Step 2: 启动并验证 OceanBase 引导完成**

Run: `docker compose --env-file .env.oceanbase -f docker-compose.oceanbase.yml up -d oceanbase`
Expected: 日志最终包含 `boot success!`；`obclient` 能以 MySQL 兼容租户连接 `hospt`。

- [ ] **Step 3: 停止旧 Tomcat 写入并执行数据迁移**

停止当前连接 MySQL 的应用服务，运行 `scripts/oceanbase/migrate-from-mysql.ps1`。当 PowerShell 提示时，由用户直接输入 MySQL 源库密码；脚本完成后产生源/目标清单，不回显密码。

- [ ] **Step 4: 执行对账并处理兼容错误**

Run: `scripts/oceanbase/verify-migration.ps1`
Expected: 所有源/目标行数、主键范围和完整性检查通过。若生成列或约束在目标版本不兼容，保留源库不动，修改 Task 2 DDL 并重新初始化 OceanBase 目标卷后重试。

- [ ] **Step 5: 构建并启动应用容器**

Run: `docker compose --env-file .env.oceanbase -f docker-compose.oceanbase.yml up -d --build app`
Expected: Tomcat 成功部署 `ustbhospital.war`，并且日志显示应用可连接 OceanBase。

### Task 6: OceanBase 上的回归与回退验证

**Files:**
- Create: `docs/operations/oceanbase-migration-runbook.md`
- Modify: `README.md`

**Interfaces:**
- Consumes: OceanBase 上已迁移数据的应用实例、既有测试帐户和管理员审计页。
- Produces: 可复现的验证、停止、重新启动和回退步骤。

- [ ] **Step 1: 编写回归/回退运行手册**

记录登录、预约、取消、改期、医生接诊、病历草稿/定稿、医嘱、患者病历和管理员审计的验证路径；明确 MySQL 回退仅切换环境变量/旧 Tomcat 配置，不能删除 OceanBase 或源数据。

- [ ] **Step 2: 运行自动化测试与 WAR 构建**

Run: `mvn test`
Expected: 全部可运行测试通过；依赖真实数据库的集成测试如环境不满足须明确 skipped，不能静默失败。

Run: `mvn package -DskipTests`
Expected: 生成 `target/ustbhospital.war`。

- [ ] **Step 3: 进行实际服务验收**

验证应用页面 `http://localhost:8080/ustbhospital/hospro/login.html`；确认容器内 `HOSPITAL_DB_URL` 为 OceanBase URL；检查审计日志产生新记录并在 OceanBase 中可查询。

- [ ] **Step 4: 审查安全范围与未提交改动**

Run: `git diff --check; git status --short`
Expected: 无空白错误；无 `.env`、导出 SQL、凭据或 OceanBase 数据目录处于 Git 跟踪状态；不执行提交。

## Self-Review

- Spec coverage: Task 1–3 迁移应用与目标基础设施；Task 4 实现数据保全；Task 5 完成受控切换；Task 6 验收和回退。
- Step scan: 每个步骤都给出唯一目标文件、接口或可执行验证命令；真实凭据仅在 Task 5 的交互步骤需要人工输入。
- Review Focus: 预约唯一性属于 Task 2/Task 5；数据原值和外键完整性属于 Task 4/5；连接目标和启动时序属于 Task 3/5；本地秘密隔离属于 Task 1/4/6。
- Proportion: 计划明确迁移边界和验收，不复制将要实现的 SQL 或 PowerShell 正文。
