# 医院预约挂号管理系统：最小挂号闭环

本项目的最小业务路径是：医生登录并维护排班 → 患者登录并浏览医生/可用时段 → 创建预约 → 患者查询、取消或改期 → 医生查看本人预约并完成接诊。

## 运行条件

- JDK 8 或更高（本项目已用 JDK 17 编译）；
- Maven 3.8 或更高；
- Docker Desktop（Linux 容器至少 2 CPU、8GB 内存）和 OceanBase Community Edition（Docker Compose 部署）；
- 仅在执行一次性历史数据迁移时需要本机 MySQL 8.0 源库；
- 支持 Servlet 3.1 的容器，例如 Tomcat 8.5/9（Tomcat 10 使用 Jakarta 包名，不能直接部署此 WAR）。

## 配置数据库

数据库连接不再写死在源码中。运行目标为 OceanBase 的 MySQL 兼容租户，默认连接地址为本机 `127.0.0.1:2881`。复制 `database/database.env.example` 的变量到本机环境，或在启动容器的 JVM 参数中设置：

```text
-Dhospital.db.driver=com.oceanbase.jdbc.Driver
-Dhospital.db.url=jdbc:oceanbase://127.0.0.1:2881/hospt?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
-Dhospital.db.username=hospital_app@test
-Dhospital.db.password=<database-password>
```

环境变量名为 `HOSPITAL_DB_URL`、`HOSPITAL_DB_USERNAME`、`HOSPITAL_DB_PASSWORD` 和可选的 `HOSPITAL_DB_DRIVER`。JVM 参数优先。

OceanBase 专用的最终态 DDL、数据迁移与对账工具位于 `database/oceanbase/` 和 `scripts/oceanbase/`。已有 MySQL 业务数据时，必须使用只读导出与对账流程迁移，不能执行演示数据脚本覆盖目标数据。

## 构建与部署

在项目根目录运行：

```text
mvn clean package
```

将生成的 `target/ustbhospital.war` 部署到 Tomcat。默认上下文路径是 `/ustbhospital`，登录页面为：

```text
http://localhost:8080/ustbhospital/hospro/login.html
```

最小预约流程页面已适配默认 `/ustbhospital` 上下文。其他历史后台页面仍可能保留站点根路径，建议后续统一处理。

### Docker + OceanBase

首次迁移真实 MySQL 数据前，按 `docs/operations/oceanbase-migration-runbook.md` 依次初始化目标库、执行迁移与对账。对账通过后，在项目根目录执行：

```text
docker compose --env-file .env.oceanbase -f docker-compose.oceanbase.yml up -d --build app
```

`docker-compose.oceanbase.yml` 同时启动 OceanBase 与 Tomcat；本地 `.env.oceanbase` 被 Git 忽略，不能提交数据库密码。

### 增量迁移提醒

本版本的 `database/migrations/20261022_secure_portal_and_balance_transactions.sql` 是**增量**脚本：它创建余额流水表，并为既有 `announcement` 表补充草稿、发布和下线状态字段。执行前先完成既有 OceanBase 对账；执行时不得重建 `hospt`、覆盖导入数据或删除 Docker 数据卷。历史公告会被保留并默认标记为已发布。

## 本次安全门户功能

- 患者可通过 `register-page.html` 注册账号；资料、头像和模拟充值均只作用于当前登录主体；
- 预约扣费与余额流水在同一事务中完成。取消预约释放号源，但不自动退款；
- 管理员可在 `admin-master-data.html` 安全维护医生、患者联系信息、科室层级和职称；被预约、排班或引用的数据会拒绝删除；
- 管理员可在 `admin-announcements.html` 保存草稿、发布、下线、编辑或删除公告；`news.html` 仅显示已发布公告；
- 新写接口均使用 POST、Session 身份、角色校验与 CSRF。旧的不安全写入口仍返回 410，不应再链接或调用。

## 预约接口与规则

- 患者：`/appointments/availability`、`/appointments/book`、`/appointments/patient`、`/appointments/cancel`、`/appointments/reschedule`；
- 医生：`/schedules/mine`、`/addDate`、`/updatedate`、`/deletedate`、`/appointments/doctor`、`/appointments/complete`；
- 身份从登录 Session 读取，预约接口不信任传入的 patientId、doctorId 或 fee；
- 时段只接受 `AM`、`PM`、`EVENING`（兼容部分旧表单值）；
- 创建、占号、扣余额和插入预约在同一事务内完成。取消预约会释放号源，但当前规则不退款；
- `booked → cancelled` 仅患者本人可执行，`booked → completed` 仅对应医生可执行。
