# 医院预约挂号管理系统：最小挂号闭环

本项目的最小业务路径是：医生登录并维护排班 → 患者登录并浏览医生/可用时段 → 创建预约 → 患者查询、取消或改期 → 医生查看本人预约并完成接诊。

## 运行条件

- JDK 8 或更高（本项目已用 JDK 17 编译）；
- Maven 3.8 或更高；
- MySQL 8.0；
- 支持 Servlet 3.1 的容器，例如 Tomcat 8.5/9（Tomcat 10 使用 Jakarta 包名，不能直接部署此 WAR）。

## 配置数据库

数据库连接不再写死在源码中。复制 `database/database.env.example` 的变量到本机环境，或在启动容器的 JVM 参数中设置：

```text
-Dhospital.db.url=jdbc:mysql://127.0.0.1:3306/hospt?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
-Dhospital.db.username=<database-user>
-Dhospital.db.password=<database-password>
```

环境变量名为 `HOSPITAL_DB_URL`、`HOSPITAL_DB_USERNAME`、`HOSPITAL_DB_PASSWORD` 和可选的 `HOSPITAL_DB_DRIVER`。JVM 参数优先。

新建本地数据库时，先执行 `database/schema/hospt_minimal_appointment_schema.sql`，再按需执行 `database/schema/hospt_minimal_demo_data.sql`。后者仅插入演示账号且使用 `INSERT IGNORE`，不会更新已有行。演示登录信息：医生工号 `D1001` / `doctor123`，患者 `patient_demo` / `patient123`，管理员 `admin` / `admin123`；仅用于本地演示，应在真实部署前删除或修改。

已有旧数据库时，不要执行完整建表脚本；在备份并运行 `database/migrations/20261011_appointment_minimum_loop.sql` 顶部的重复数据检查后，执行该迁移一次。

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

## 预约接口与规则

- 患者：`/appointments/availability`、`/appointments/book`、`/appointments/patient`、`/appointments/cancel`、`/appointments/reschedule`；
- 医生：`/schedules/mine`、`/addDate`、`/updatedate`、`/deletedate`、`/appointments/doctor`、`/appointments/complete`；
- 身份从登录 Session 读取，预约接口不信任传入的 patientId、doctorId 或 fee；
- 时段只接受 `AM`、`PM`、`EVENING`（兼容部分旧表单值）；
- 创建、占号、扣余额和插入预约在同一事务内完成。取消预约会释放号源，但当前规则不退款；
- `booked → cancelled` 仅患者本人可执行，`booked → completed` 仅对应医生可执行。
