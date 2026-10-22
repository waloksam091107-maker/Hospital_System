# OceanBase 数据保留迁移与 Docker 部署手册

本手册把本项目从本机 MySQL `hospt` 源库迁移到 OceanBase Community Edition 的 MySQL 兼容租户。迁移脚本只读取 MySQL；不会删除、更新或改写源库。

## 1. 前置条件

- Docker Desktop 使用 Linux 容器，WSL 2 至少分配 2 CPU、8GB 内存；
- 本机 MySQL 8.0 服务可用，且保留当前 `hospt` 业务数据；
- 在项目根目录复制 `.env.oceanbase.example` 为 `.env.oceanbase`，填写只在本机保存的 OceanBase 租户密码和应用账号密码；
- 不要把 `.env.oceanbase`、`database/oceanbase/export/` 或 Docker 数据卷加入 Git。

## 2. 初始化目标库

```powershell
docker compose --env-file .env.oceanbase -f docker-compose.oceanbase.yml up -d oceanbase
docker compose --env-file .env.oceanbase -f docker-compose.oceanbase.yml ps
```

只有当 `oceanbase` 显示为 `healthy` 才能继续。首次初始化完成后，启动日志应包含 `boot success!`。初始化脚本会创建空的 `hospt` 库与目标表，尚未导入任何 MySQL 数据。

## 3. 导入真实业务数据

在项目根目录执行：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\oceanbase\migrate-from-mysql.ps1
```

脚本会在 PowerShell 安全密码提示中要求输入 MySQL 源库密码。密码只存于本次脚本进程的内存中，导出文件仅保存在被 Git 忽略的 `database/oceanbase/export/`。导入前脚本会比较源、目标表集合；目标结构缺表时会停止，不会悄悄跳过数据。

## 4. 对账

完成导入后立刻执行：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\oceanbase\verify-migration.ps1
```

再次按提示输入 MySQL 源库密码。对账会比较各业务表行数及主键范围，并检查预约、病历、医嘱的关键关联是否存在孤儿记录。所有项目通过后才能切换应用。

## 5. 构建并启动应用

```powershell
docker compose --env-file .env.oceanbase -f docker-compose.oceanbase.yml up -d --build app
docker compose --env-file .env.oceanbase -f docker-compose.oceanbase.yml ps
docker compose --env-file .env.oceanbase -f docker-compose.oceanbase.yml logs --tail=200 app
```

应用端口为 `http://localhost:8080/ustbhospital/`。Tomcat 连接 OceanBase 时使用 `hospital_app@test`，不是 `root@test`。

## 6. 回滚边界

若迁移或验收失败，先停止应用容器：

```powershell
docker compose --env-file .env.oceanbase -f docker-compose.oceanbase.yml stop app
```

此操作不会修改 MySQL 源库。只有确认需要从零重建 OceanBase 目标库且不再保留目标库导入结果时，才可执行 `docker compose ... down -v`；该命令会删除 OceanBase 目标卷，不能用于源库。
