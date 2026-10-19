# 第一阶段部署与验证

## 部署前

1. 停止 Tomcat 或切换到维护窗口，备份 `hospt` 数据库。
2. 在数据库副本先执行以下预检；两个查询都必须返回空集：

```sql
SELECT doctor_id, appointment_date, appointment_time, COUNT(*) AS duplicate_count
FROM appointments
WHERE UPPER(status) IN ('BOOKED', 'IN_PROGRESS', 'COMPLETED')
GROUP BY doctor_id, appointment_date, appointment_time
HAVING COUNT(*) > 1;

SELECT doctor_id, date, COUNT(*) AS duplicate_count
FROM dates
GROUP BY doctor_id, date
HAVING COUNT(*) > 1;
```

3. 确认数据库为 MySQL 8，且现有最小闭环迁移 `20261011_appointment_minimum_loop.sql` 已执行。

## 数据库迁移

在备份后的目标库执行：

```powershell
mysql -u <用户名> -p hospt < database/migrations/20261014_appointment_slots_and_states.sql
```

迁移会：

- 创建 `schedule_slots`；
- 回填 `dates` 的早、中、晚时段；
- 将有效预约对应时段标记为 `BOOKED`；
- 为预约增加时段、改期来源、时间戳和版本字段；
- 将预约状态规范为大写状态机值。

迁移后核对：

```sql
SELECT slot_status, COUNT(*) FROM schedule_slots GROUP BY slot_status;
SELECT status, COUNT(*) FROM appointments GROUP BY status;
SELECT COUNT(*) AS appointments_without_slot
FROM appointments WHERE schedule_slot_id IS NULL;
```

最后一个计数应为 0；如非 0，不要部署新 WAR，应根据备份和遗留数据补齐关联。

## 构建与部署

```powershell
mvn clean package
```

产物为 `target/ustbhospital.war`。停止 Tomcat 后，将其替换为部署目录中的 `ustbhospital.war`，清理同名展开目录，再启动 Tomcat。

## 浏览器验收

1. 医生登录并创建可预约时段。
2. 患者登录、预约一个 `AVAILABLE` 时段；重复预约相同时段必须失败。
3. 患者取消预约；时段应恢复为 `AVAILABLE`，并出现“不自动退款”提示。
4. 患者改期；旧记录为 `RESCHEDULED`，新记录为 `BOOKED`。
5. 医生只能看到自己的预约；点击“开始接诊”后状态为 `IN_PROGRESS`，才能完成接诊。
6. 在浏览器开发者工具中移除 `X-CSRF-Token` 重发写请求，应收到 HTTP 403。
7. 未登录访问 `/appointments/patient` 或 `/appointments/doctor`，应收到 HTTP 401。

## 回滚

WAR 可回滚为上一版本。数据库回滚必须使用部署前备份；本迁移包含数据状态规范化和新增外键，不能安全地仅靠删除新表恢复。
