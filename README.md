# 医院预约挂号管理系统

患者可注册登录、维护资料、浏览医生和号源、模拟充值、预约/取消/改期及查看正式病历。医生维护排班、开始接诊、保存病历草稿并定稿，管理员维护基础数据、公告和审计日志。后端使用 OceanBase 的 MySQL 兼容租户。

## 源码与镜像

- GitHub：https://github.com/waloksam091107-maker/Hospital_System
- Docker Hub：https://hub.docker.com/r/ivan091107/hospital-system
- 已发布的历史版本：`ivan091107/hospital-system:v1.0.1-lsy-chl`。该标签不包含本工作区尚未发布的修改。本次没有上传或覆盖远程镜像。

```powershell
git clone https://github.com/waloksam091107-maker/Hospital_System.git
cd Hospital_System
```

已有源码请在对应目录更新并检查未提交修改，不要用强制同步覆盖工作区。

## 环境与本地构建

- JDK 8 或更高，已使用 JDK 17 验证；Maven 3.8 或更高。
- Tomcat 9（`javax.servlet`）；Tomcat 10 的 Jakarta API 与当前 WAR 不兼容。
- Docker Desktop Linux 引擎，OceanBase 至少按 2 CPU、8 GB 内存准备资源。

```powershell
mvn test
mvn -DskipTests package
```

生成 `target/ustbhospital.war`。数据库配置来自 `HOSPITAL_DB_DRIVER`、`HOSPITAL_DB_URL`、`HOSPITAL_DB_USERNAME`、`HOSPITAL_DB_PASSWORD`；同名 `hospital.db.*` JVM 参数优先。业务连接使用 `com.oceanbase.jdbc.Driver` 和 `jdbc:oceanbase://主机:2881/hospt?...`，不要将密码写入源码。

登录入口：`http://localhost:8080/ustbhospital/hospro/login.html`。

## Docker 启动和更新

完整配置、账号初始化、旧头像迁移、镜像启动、备份和恢复步骤见 [部署与运维说明](docs/operations/maintenance-deployment.md)。本地 `.env.oceanbase` 被 Git 忽略，禁止提交真实密码。

首次配置，在文件尚不存在时执行：

```powershell
Copy-Item .env.oceanbase.example .env.oceanbase
notepad .env.oceanbase
```

先填写本地密码。已有迁移好的业务库应保留原配置和原 Compose 项目名，不重复初始化或覆盖导入。

```powershell
docker compose --env-file .env.oceanbase -f docker-compose.oceanbase.yml up -d oceanbase
```

空库首次启动还需要按部署说明创建业务数据库账号、授予权限；**建表脚本不会自动提供可登录的业务账号**。已有真实 MySQL 数据时，按 [迁移手册](docs/operations/oceanbase-migration-runbook.md) 导入并对账，不导入演示数据。

数据库账号配置好后，构建当前工作区并启动应用：

```powershell
docker compose --env-file .env.oceanbase -f docker-compose.oceanbase.yml up -d --build app
docker compose --env-file .env.oceanbase -f docker-compose.oceanbase.yml ps
curl.exe --fail http://localhost:8080/ustbhospital/health/ready
docker compose --env-file .env.oceanbase -f docker-compose.oceanbase.yml logs --tail 100 app
```

后续更新也使用同一个目录、同一个 Compose 项目名和数据卷，执行上述 `up -d --build app`。普通停止：

```powershell
docker compose --env-file .env.oceanbase -f docker-compose.oceanbase.yml stop
```

不要使用 `down -v`，否则会删除数据库和头像卷。上传文件位于外部持久目录 `HOSPITAL_UPLOAD_DIR`（容器中为 `/var/lib/hospital/uploads`），头像 URL 继续使用 `/uploads/avatars/文件名`。升级前须按部署说明复制旧部署目录内的头像。

无需本地构建时使用 `docker-compose.image.yml`；先在本地配置中指定实际包含所需功能的 `HOSPITAL_APP_IMAGE`、数据库连接和 `OCEANBASE_HOST`，再执行：

```powershell
docker compose --env-file .env.image -f docker-compose.image.yml pull
docker compose --env-file .env.image -f docker-compose.image.yml up -d
```

此方式连接已配置好的 OceanBase，不会自动创建数据库。`.env.image` 的变量示例及主机连接方式见部署说明。

## 主要接口和业务规则

- 患者预约：`GET /appointments/availability`、`GET /appointments/patient`；`POST /appointments/book`、`/appointments/cancel`、`/appointments/reschedule`。
- 医生排班：`GET /schedules/mine`；`POST /schedules/create`、`/schedules/update`、`/schedules/delete`。
- 接诊：`GET /appointments/doctor`、`/appointments/doctor/detail`；`POST /appointments/start`。旧 `/appointments/complete` 不能绕过病历直接完成预约。
- 病历：`POST /medical-records/draft`、`/medical-records/finalize`；`GET /medical-records/doctor`、`/medical-records/patient`。定稿后预约变为 COMPLETED，患者只能读取自己的 FINAL 记录。
- 病历筛选：`GET /medical-records/patient/search?dateFrom=2030-01-01&dateTo=2030-12-31&doctorId=1&pageNum=1&pageSize=10`，可省略筛选条件。日期按预约日期筛选，返回 `items/total/pages/pageNum/pageSize`。
- 后台基础数据：`/portal/admin/doctors`、`/patients`、`/departments`、`/professional-titles`（均位于 `/portal/admin` 下）。GET 支持 `keyword/pageNum/pageSize`；POST 使用 `action=create/update/delete`。科室和职称支持 `options=true` 返回完整精简选项，不能把第一页当成全部选项。
- 后台默认每页 10、最大 100，关键字最多 100 字；按字面查询 `%`、`_`，按主键稳定排序。
- 所有写操作沿用 POST、Session 角色、CSRF；身份从服务端会话读取，不能相信 URL 中的角色或患者编号。
- 状态路径：BOOKED → IN_PROGRESS → COMPLETED；患者可以将 BOOKED 取消或改期。扣费、预约和流水在同一事务内，取消释放号源，现行规则不自动退款。

### 排班调整与数据保留

同一天内，未被有效预约占用的时段可以调整；取消、改期历史不再阻止调整，但原时段 ID 保留。BOOKED、IN_PROGRESS、COMPLETED 所占时段不可修改。有预约历史的日期不能整体移动。

无预约历史的排班可以物理删除；有历史但没有待接诊或接诊中预约时，撤班关闭可预约号源并保留历史。重复撤班返回冲突提示。过去日期不能撤销或修改，非法日期返回 400，业务冲突返回 409。

医生、患者、科室和职称删除前检查业务引用。患者余额非零，即使没有流水，也不能删除。资金流水、病历、旧医嘱、预约和排班历史不能随基础数据级联删除。

## 测试

```powershell
mvn test
node src/test/js/schedule-maintenance.test.cjs
node tests/admin-master-data-ui.cjs
node tests/doctor-encounter-ui.cjs
node tests/patient-medical-records-ui.cjs
```

浏览器脚本需要可解析的 `playwright` 包和本机 Edge，或通过 `PLAYWRIGHT_BROWSER_PATH` 指定 Chromium 浏览器。它们使用真实页面及受控 HTTP 响应，不修改真实业务库。

显式启动独立 OceanBase 测试环境并运行集成测试：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\oceanbase\run-isolated-tests.ps1 -StartIsolatedContainer
```

该脚本使用独立 Compose 项目、默认端口 12881、随机 `hospital_test_` 数据库和受限账号；不得改成真实 `hospt` 库。已有 MySQL 集成测试默认跳过，不能把默认 Maven 通过理解为真实 OceanBase 集成测试已经执行。详细验收见 [本次完善测试清单](docs/testing/maintenance-completion-acceptance.md)。
