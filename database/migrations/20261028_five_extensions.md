# 五项扩展数据库升级

本次已有库升级使用 `com.ustb.hospital.database.ExtensionSchemaUpgrade`，不提供需要手工逐句执行的盲目 ALTER 脚本。空库使用 `database/oceanbase/01_hospt_schema.sql`。两者提供相同的业务字段、消息、账号控制、停诊和候诊历史表。

升级器不删除业务记录、不修改密码、头像、余额、病历、医嘱、资金流水或审计历史，不批量补发历史通知。它需要专用的 autocommit JDBC 连接，不能嵌入业务事务。OceanBase DDL 可分步提交；失败后不能假设数据库已经回滚。

## 操作顺序

1. 先在全新隔离测试库演练并核对升级前后记录数、余额、密码哈希、头像、预约主键、病历及医嘱关联、资金流水和审计记录。保存经过验证的业务库备份与校验文件。
2. 构建并测试新 WAR。安排维护窗口，停止所有旧版和新版应用实例及其他写入任务。确认所有写入都已经停止后才传入 `-MaintenanceMode`；该开关表示操作人员的明确确认，不会自动停止或替换容器。
3. 在环境变量中配置 `HOSPITAL_UPGRADE_DB_URL`、`HOSPITAL_UPGRADE_DB_USERNAME`、`HOSPITAL_UPGRADE_DB_PASSWORD`。URL 必须明确选择目标数据库，例如 `jdbc:oceanbase://127.0.0.1:12881/hospital_test_example?serverTimezone=Asia/Shanghai`。凭证不作为 Java 命令参数或写入日志。
4. 先运行 `scripts/oceanbase/upgrade-five-extensions.ps1 -TargetDatabase hospital_test_example -ConfirmTarget hospital_test_example -MaintenanceMode -ValidateOnly`。此步骤只验证参数和环境，不连接数据库。
5. 使用相同参数去掉 `-ValidateOnly` 执行。默认使用已构建的 `target/classes` 和 WAR 目录中的 OceanBase JDBC 驱动；必要时可通过 `-RuntimeClasspath` 指定已审核的运行时目录。
6. 查询 `extension_schema_progress`，确认迁移 `20261028-five-extensions` 的 `COMPLETE` 阶段存在，并进行下面的业务核对。通过后才切换到新程序。禁止旧的单号源程序重新写入升级后的数据库。

## 预检与阶段

任何表、字段、索引或数据改动之前，预检会拒绝：缺失核心表、预约未关联号源、号源不存在、医生/日期/时段不匹配、未知状态、同一患者重复有效预约、旧号源有效占用超过 1、已存在新容量字段时占用超过其容量、无效容量以及重复或非正的已有预约序号。错误清单包含需要人工核查的预约/号源编号。不会推断、删除或改写异常记录。

阶段记录依次为 `NEW_TABLES`、`COLUMNS`、`BACKFILL`、`NEW_GUARDS`、`RELEASE_OLD_GUARD`、`COMPLETE`。DDL 通过元数据检测已完成步骤；数据回填与其阶段标记在一个事务中提交。只有新的患者/号源唯一索引和号源/序号唯一索引都存在后，才移除旧的单人号源唯一索引。

旧号源容量回填 1，新建号源默认 10。所有预约按每个号源内预约主键顺序获得稳定序号；取消和改期历史也保留序号。有效占用包含 `BOOKED`、`IN_PROGRESS`、`COMPLETED`、`NO_SHOW`。旧 `BOOKED` 号源转换为 `AVAILABLE`；对应 dates 的时段标记为 0 时转换为 `CLOSED`。既有手工 `CLOSED` 不被打开。已完成预约映射 `DONE`，接诊中映射 `IN_PROGRESS`，取消/改期历史映射 `REMOVED`，其余待接诊从 `NOT_CHECKED_IN` 开始。

## 核对与失败处理

- 业务记录总数、主键、余额和密码哈希应与备份清单一致；病历、医嘱及流水外键仍指向原编号。
- 对每个号源，`occupied_count` 应等于上述四种状态的预约数量，且不大于 `capacity`；`next_sequence` 不小于现有最大序号；全部历史预约都有正序号。
- 确认存在 `uk_appointments_active_patient_slot` 和 `uk_appointments_slot_sequence`，旧 `uk_appointments_active_slot` 已移除。消息表为空，不应出现补发历史业务通知。
- 任意失败均保持所有应用写入关闭。保存预检编号和阶段记录，修复具体原因后，对同一数据库重新运行同一升级器。不要手工清空阶段表、删除业务数据或启动旧程序。
- 若决定回退，应按现有备份/恢复流程恢复到一个明确的恢复目标，核对完整后再进行独立切换；不要将应用直接指回处于部分升级状态的数据库。

## 可重复验证

默认测试：`mvn -B -Dtest=ExtensionSchemaUpgradeTest,OceanBaseSchemaContractTest test`。

PowerShell 5.1 安全参数测试：`powershell.exe -NoProfile -ExecutionPolicy Bypass -File src/test/powershell/upgrade-five-extensions-safety.ps1`。

真实升级测试类为 `ExtensionSchemaUpgradeOceanBaseTest`，必须使用新建、空的 `hospital_test_<32位十六进制>` 数据库、非业务端口和只能访问该库的 `hospital_test_runner@test` 账号。按既有隔离容器流程准备环境，将其连接设置放入 `HOSPITAL_TEST_DB_*` 环境变量后运行 `mvn -B -Dhospital.test.oceanbase.upgrade=true -Dtest=ExtensionSchemaUpgradeOceanBaseTest test`。测试会创建旧版结构和虚构数据，升级、重复执行并核对保留情况，不会清理或访问真实业务库。
