# 就诊记录与安全加固部署说明

## 部署前

1. 停止应用或进入维护窗口，并备份 `hospt` 数据库与当前 WAR。
2. 确认现有库已经执行过 `20261014_appointment_slots_and_states.sql`。
3. 使用与生产一致的账号，在预发布库先验证三份迁移；不要跳过顺序。

## 数据库迁移顺序

1. `database/migrations/20261014_appointment_slots_and_states.sql`
2. `database/migrations/20261014_medical_records_and_orders.sql`
3. `database/migrations/20261014_password_hash_and_audit.sql`

第三份迁移只增加可空 `password_hash`，不会主动删除旧密码。用户下一次以正确旧密码登录时，系统会写入 BCrypt 哈希并清空该用户的旧明文列。

## 发布与冒烟测试

1. 用 `mvn clean package` 生成 `target/ustbhospital.war`，替换 Tomcat 的 `webapps/ustbhospital.war` 后重启。
2. 医生登录后创建排班，患者预约，医生开始接诊。
3. 医生在“填写就诊记录”页填写诊断并可选添加医嘱；提交后确认预约状态为 `COMPLETED`。
4. 患者打开“就诊记录与医嘱”，确认仅能看到自己的同一记录。
5. 管理员访问 `/ustbhospital/audit-logs`，确认可查看元数据日志；日志中不应出现密码、诊断正文、医嘱正文或身份证号。
6. 分别用无 CSRF 令牌的预约、排班、病历和密码修改 POST 请求确认返回 `403`。

## 已退役接口

`/login`、`/password/change`、`/orders/*` 和 `/patients/pay` 会返回 `410 Gone`。旧医嘱必须通过 `/medical-records/finalize` 由接诊医生提交；旧充值页面不能继续使用，需在后续迭代实现会话绑定的支付流程。
