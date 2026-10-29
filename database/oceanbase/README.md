# OceanBase 目标库文件

本目录只包含 OceanBase MySQL 兼容租户的目标结构和校验语句，不会导入演示账号或修改本机 MySQL 源库。

1. `01_hospt_schema.sql`：当前项目运行所需的最终态结构。它同时覆盖预约闭环、病历/医嘱、审计和仍被旧页面使用的新闻、公告、菜单、订单表。
2. `02_create_app_user.sql.template`：业务账号权限模板。复制到被 Git 忽略的本地文件后，才替换 `__HOSPITAL_APP_PASSWORD__`。
3. `03_validate_schema.sql`：空库初始化和真实数据导入前后的结构检查。

真实历史数据迁移必须使用 `scripts/oceanbase/migrate-from-mysql.ps1`，并在完成后执行 `scripts/oceanbase/verify-migration.ps1`。不要运行 `database/schema/hospt_minimal_demo_data.sql`，否则会把演示数据混入真实迁移目标。

多号源版本使用 `capacity`、`occupied_count` 和递增的 `appointment_sequence`。`BOOKED`、`IN_PROGRESS`、`COMPLETED`、`NO_SHOW` 占用容量；取消和改期释放容量并保留历史序号。生成列 `active_patient_slot_key` 与唯一索引防止同一患者重复占用同一号源，另一唯一索引防止同一号源序号重复。已有旧库必须停写并执行 `database/migrations/20261028_five_extensions.md` 所述的增量升级，不重复建库或导入演示数据。
