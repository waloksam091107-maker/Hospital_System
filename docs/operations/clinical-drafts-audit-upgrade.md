# 病历草稿与审计功能升级及验收

本说明适用于已有预约、病历和审计基础表的 MySQL 8.0.19 / Tomcat 9 环境。本次只提供增量迁移与 WAR；构建和隔离测试不会更改正在使用的数据库或 Tomcat。

## 一、升级前检查

1. 确认环境与数据库连接实际指向预期的 MySQL、端口和库；应用通过 `hospital.db.url` / `HOSPITAL_DB_URL` 等配置连接。不要把测试库连接误用于正式服务。
2. 建议对当前数据库做可恢复备份，并保留当前可用 WAR；确定维护窗口，停止新业务写入。用户可自行决定是否备份，但无备份将无法可靠恢复误操作或不兼容状态。
3. 确认之前的预约号源、病历医嘱、密码散列及审计基础迁移均已执行，并确认 `medical_records`、`medical_orders`、`audit_logs`、`appointments`、`schedule_slots` 等表存在。本脚本不是从零建库脚本。
4. 阅读同目录 `audited-entrypoints.md`。为避免未授权写操作，旧公开注册、部分资料、头像和管理员写入口暂时返回 410，升级后相关功能暂不可用；上线前应决定能否接受这个功能变化。

## 二、增量迁移

使用 MySQL 客户端登录预期实例，在 `mysql>` 提示符下执行：

```sql
SELECT DATABASE(), @@hostname, @@port, VERSION();
USE hospt;
SELECT DATABASE();
SHOW TABLES;
SHOW COLUMNS FROM medical_records;
SHOW INDEX FROM audit_logs;
```

将 `hospt` 改成实际使用的库名。确认库正确后执行本项目工作树中的脚本（路径根据实际位置调整，`SOURCE` 在 MySQL 客户端内运行）：

```sql
SOURCE C:/Users/ivanr/Desktop/Hospital_System/Hospital_System/.worktrees/appointment-closure/database/migrations/20261017_clinical_drafts_and_audit_query.sql;
```

执行期间如有 `ERROR`，不要继续部署；记录完整错误并核查实际表结构。MySQL DDL 会隐式提交，脚本前半部分即使成功也不会因后续失败自动回滚。脚本按字段与索引名称检查，可在修正原因后重新执行；重复执行不表示可以忽略结构不一致或中途错误。

检查结果：

```sql
SHOW COLUMNS FROM medical_records LIKE 'record_status';
SHOW COLUMNS FROM medical_records LIKE 'version';
SHOW COLUMNS FROM medical_records LIKE 'finalized_at';
SHOW COLUMNS FROM medical_records LIKE 'diagnosis';
SHOW INDEX FROM audit_logs WHERE Key_name = 'idx_audit_logs_created_id';
SELECT record_status, COUNT(*) FROM medical_records GROUP BY record_status;
```

前三个字段应存在；`diagnosis` 允许 NULL；索引应包含 `created_at`、`audit_log_id`；迁移前已有病历应保留为 `FINAL`。不要在日志或截图中展示密码、病历正文、身份证号或令牌。

## 三、部署与恢复边界

迁移验证通过后再将构建出的 `target/ustbhospital.war` 按 Tomcat 现有应用上下文部署，并启动或重启服务。修改部署需要另行执行；本工作不会自动替换现用 WAR。重启后可能需要重新登录，浏览器强制刷新以获得新页面。

如升级后需回退，先停止写入，评估新增草稿及其医嘱如何保留，再由备份与匹配的旧 WAR 恢复或实施经验证的前向修复。不要仅靠删除新列来回滚：已产生的空诊断草稿与旧 `NOT NULL` 约束不兼容，且删除列或恢复旧备份可能丢失升级后的业务数据。对失败的 DDL 逐条检查当前结构后再处理。

## 四、人工验收

使用独立测试账号与测试排班，不输入真实医疗信息：

1. 患者预约可用时段；医生开始接诊并打开病历。诊断和方案留空保存草稿，应成功且预约保持接诊中。患者不能读取该草稿或其医嘱。
2. 医生重新打开草稿，确认内容及医嘱恢复。两个窗口加载同一版本，先在窗口 A 保存，再从窗口 B 保存；窗口 B 应提示版本冲突并保留未提交的输入。
3. 在接诊中分别留空诊断和诊疗方案尝试正式提交，应被拒绝；两项填写并提交后，应保存病历及医嘱、预约变为已完成，患者只能查看自己的正式记录。验证其他医生及其他患者不能读取或修改。
4. 管理员打开审计页面，分别按时间、角色、账号编号、动作和结果筛选，组合条件、翻页、重置、无结果和非法参数均应正确；患者和医生访问审计接口应返回 403，未登录应返回 401。
5. 依次测试预约、取消、改期、开始接诊、排班变更、病历草稿和正式提交、修改密码，核对对应审计记录的角色、目标及成功/失败。新预约及排班成功记录应带实际目标编号。拒绝、CSRF 失败等应有一条失败事件，不应额外产生成功记录。
6. 在隔离测试环境模拟审计写入失败，确认相关业务写入回滚；不可在正式库通过删除审计表模拟。独立失败日志数据库不可用时只能产生服务器脱敏告警，不能保证失败事件落库。登录成功日志与 Session 创建不在一个数据库事务中。

功能测试通过不等于对整个旧系统、部署环境或真实医疗使用场景完成安全认证。部署后仍须在真实 Tomcat 和目标数据库执行以上端到端验收。
