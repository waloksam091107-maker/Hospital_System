# OceanBase 保留数据迁移设计

## 1. 目标

将医院挂号系统的运行数据库从本机 MySQL 8.0 迁移为 Docker 中部署的 OceanBase Community Edition（MySQL 兼容模式），并保证已有业务数据得到保留。迁移完成后，应用后端默认通过 `HOSPITAL_DB_*` 环境变量连接 OceanBase，MySQL 仅作为可回退的原始数据源，不再作为应用运行库。

需要保留的数据范围包括账户、科室和职称、排班、预约及其状态链、病历草稿/定稿、医嘱和审计日志，以及源库中存在的其他业务表。迁移不得通过复制 MySQL 数据目录实现。

## 2. 已知约束

- 现有项目位于 `C:\Users\ivanr\Desktop\Hospital_System\Hospital_System`，WAR 名称为 `ustbhospital.war`，运行容器必须使用 Tomcat 9。
- 源库为本机运行的 MySQL 8.0；访问凭据不写入源码、迁移脚本、Compose 文件或 Git。
- 应用使用 MyBatis；连接参数由 `MybatisUtil` 从 `HOSPITAL_DB_URL`、`HOSPITAL_DB_USERNAME`、`HOSPITAL_DB_PASSWORD` 和 `HOSPITAL_DB_DRIVER` 获取。
- 现有 Mapper 中有 `LIMIT/OFFSET`、`FOR UPDATE` 和少量显式 ``hospt`` 库名。目标 OceanBase 数据库继续使用 `hospt` 作为数据库名，以避免无必要的 Mapper 改动。
- 不执行 Git commit；`.env`、导出的 SQL 和本地凭据必须忽略。
- OceanBase 单机 Docker 仅用于课程项目、验收与开发演示，不作为生产高可用部署方案。

## 3. 迁移方式与理由

采用“先创建目标结构、再导出数据、再导入数据、最后切换连接”的逻辑迁移：

1. 保持 MySQL 服务与数据不变；启动 OceanBase MySQL 兼容模式的独立 Docker 容器和持久化卷。
2. 基于项目当前结构创建 OceanBase 专用 DDL，而不是直接运行 MySQL 的全部迁移脚本。
3. 从 MySQL 以一致性快照导出结构清单和数据；保留主键值、密码哈希、外键关联、预约状态、版本号和审计时间。
4. 以外键依赖顺序导入 OceanBase；导入失败时停止，不自动清理 MySQL 或覆盖源库。
5. 对每张已迁移表的行数、主键范围、关键关联和业务状态进行对账；通过后才让 Tomcat 使用 OceanBase 的连接配置。

该路径允许在切换失败时恢复到原 MySQL 环境变量，无需从备份还原。

## 4. OceanBase 兼容适配

### 4.1 Java 与 MyBatis

- 运行目标使用 OceanBase 的 MySQL 兼容租户。
- 默认保留 MySQL JDBC 协议与 MyBatis 的 `mysql` 分页方言；必要时仅替换为 OceanBase Connector/J 和 `jdbc:oceanbase` URL，不修改业务接口。
- 所有 JDBC 配置均由环境变量传递；Docker Compose 不保存真实密码。

### 4.2 DDL 和迁移脚本

新增 `database/oceanbase/`，保留原 `database/schema/` 与 `database/migrations/` 不动。OceanBase 目录提供：

- 可重复执行的基础结构脚本；
- 为当前实际源表生成的兼容数据导入脚本或导入说明；
- 数据库校验 SQL；
- Docker 初始化脚本。

MySQL 专用的 `ENGINE=InnoDB`、字符集/排序规则、列顺序 `AFTER`、动态 `PREPARE` 迁移和 MySQL 特定的检查约束处理不直接沿用。预约的“有效号源唯一性”保留为独立可验证的唯一性约束；若目标版本不支持原生成列写法，则以普通 `active_slot_key` 字段和业务更新逻辑维持同一规则。

### 4.3 数据迁移

迁移脚本在读取源库前先获取 `SHOW TABLES`、每表记录数和主键范围，形成迁移清单。数据导出使用 `mysqldump --single-transaction --no-create-info` 或等价的一致性读取；导入按以下依赖顺序执行：

1. `departments`、`professional_titles`；
2. `admins`、`doctors`、`patients`；
3. `dates`、`schedule_slots`；
4. `appointments`；
5. `medical_records`、`medical_orders`；
6. `audit_logs`；
7. 源库实际存在的独立内容表，例如公告、新闻或其他未建立外键的业务表。

身份密码字段、BCrypt 哈希、余额、预约状态、病历定稿状态和审计记录按原值迁移；不以演示数据覆盖现有行。

## 5. 容器与切换设计

- Docker Compose 包含 `oceanbase`、`app` 及一次性迁移/校验工具服务；应用不再启动 MySQL 容器。
- OceanBase 使用命名卷保存目标数据，默认暴露 MySQL 协议端口供宿主验证；应用通过 Compose 网络连接目标容器。
- `.env` 保存 OceanBase 账号、密码和源 MySQL 连接信息，且被 `.gitignore` 排除。
- 切换前应用保持停止或维护状态；导入和对账期间不得对源库执行写入。切换后只允许应用连接 OceanBase。
- 若对账、启动或回归测试失败，恢复原有 MySQL 环境变量并启动旧 Tomcat 配置；不删除源 MySQL 数据。

## 6. 验收标准

1. `docker compose ps` 显示 OceanBase 和应用均正常运行，应用日志无数据库连接或字符集错误。
2. 应用容器数据库连接地址指向 OceanBase 服务，而不是 `127.0.0.1:3306` 或 MySQL 容器。
3. 每张迁移表的源/目标行数一致；预约、病历、医嘱与审计日志的关键外键均无孤儿记录。
4. 所有既有账号可登录；BCrypt 密码验证不受迁移影响。
5. 预约创建、取消、改期、医生接诊、病历草稿/定稿、医嘱录入、患者病历查询和管理员审计查询均可通过。
6. MySQL 原库在整个过程后仍可读取，作为可回退数据源。

## 7. 需要人工提供的输入

实际导出时需要由系统所有者在本机安全地输入 MySQL 只读或管理员连接凭据。凭据不写入聊天、源码、Dockerfile、Compose、迁移脚本或 Git；若命令提示密码，将由所有者直接在终端输入。
