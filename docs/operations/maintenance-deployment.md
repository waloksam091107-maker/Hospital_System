# 排班、后台和病历完善版本：部署与运维

适用代码：当前检出的项目源码。数据库继续为 OceanBase MySQL 兼容租户，不重新导入 MySQL，不覆盖已有业务数据。源码和本地镜像修改不会自动更新 Docker Hub。

## 1. 获取项目和准备配置

```powershell
git clone https://github.com/waloksam091107-maker/Hospital_System.git
cd Hospital_System
if (-not (Test-Path .env.oceanbase)) { Copy-Item .env.oceanbase.example .env.oceanbase }
notepad .env.oceanbase
```

将两个本地占位符改为独立密码：`OB_ROOT_PASSWORD` 用于 `root@test`，`HOSPITAL_APP_PASSWORD` 用于业务账号。已有环境保留原值和原 Compose 项目名。`.env.*` 被 Git 忽略，不能通过提交配置文件共享密码。

Docker Desktop 使用 Linux 引擎，分配至少 2 CPU、8 GB 内存。数据库镜像已固定为本机验证过的 OceanBase 4.4.2.1 对应摘要，不跟随 `latest` 自动升级。应用仍使用 Tomcat 9 / Java 17，WAR 保持 `javax.servlet`。

## 2. 数据库与业务账号

```powershell
docker compose --env-file .env.oceanbase -f docker-compose.oceanbase.yml up -d oceanbase
docker compose --env-file .env.oceanbase -f docker-compose.oceanbase.yml ps
```

首次初始化可能需要数分钟。等待 OceanBase 显示 healthy，再处理业务账号。已迁移且账号可用的环境跳过以下创建步骤，直接进入第 3 节。

当前 Compose 不自动挂载或重复执行建表脚本。只有确认是全新的空业务库时，才执行下面的初始化；已有迁移数据的环境跳过此命令。此脚本不会创建可登录的业务账号，也不会导入演示患者。

```powershell
Get-Content -Raw -Encoding UTF8 .\database\oceanbase\01_hospt_schema.sql | docker compose --env-file .env.oceanbase -f docker-compose.oceanbase.yml exec -T oceanbase sh -lc 'MYSQL_PWD="$OB_TENANT_PASSWORD" obclient -h127.0.0.1 -P2881 -uroot@test --default-character-set=utf8mb4'
```

Windows PowerShell 5.1 执行包含中文的 SQL 前，在当前终端设置 `$OutputEncoding = New-Object System.Text.UTF8Encoding($false)`。脚本执行失败时先查看错误，不重复导入历史数据。随后打开租户数据库命令行：

```powershell
docker compose --env-file .env.oceanbase -f docker-compose.oceanbase.yml exec oceanbase obclient -h127.0.0.1 -P2881 -uroot@test -p
```

在提示处输入 `OB_ROOT_PASSWORD`，然后在数据库命令行执行。下方密码占位符必须替换为本机 `.env.oceanbase` 的 `HOSPITAL_APP_PASSWORD`；输入数据库命令不等同于 PowerShell 命令。

```sql
USE hospt;
CREATE USER IF NOT EXISTS hospital_app IDENTIFIED BY '替换为本地业务账号密码';
GRANT SELECT, INSERT, UPDATE, DELETE ON hospt.* TO hospital_app;
FLUSH PRIVILEGES;
```

可以参考 `database/oceanbase/02_create_app_user.sql.template`，不要把填好密码的副本加入 Git。已有账号密码与本地配置不一致时应核对配置，不盲目改动账号。已经迁移的真实库不运行演示数据脚本或重复导入。

已有旧版本若尚未执行 `20261022_secure_portal_and_balance_transactions.sql`，应先按原升级说明完成该历史增量升级。五项扩展需要容量、候诊、停诊、通知和账号保护结构，按 [五项扩展增量升级](../../database/migrations/20261028_five_extensions.md) 在停写状态升级并核对，再启动新应用。不要通过清库或重导数据获得新结构。

## 3. 从当前源码构建启动

```powershell
mvn test
mvn -DskipTests package
docker compose --env-file .env.oceanbase -f docker-compose.oceanbase.yml up -d --build app
docker compose --env-file .env.oceanbase -f docker-compose.oceanbase.yml ps
curl.exe --fail http://localhost:8080/ustbhospital/health/ready
```

登录页：`http://localhost:8080/ustbhospital/hospro/login.html`。就绪接口返回 `{"ready":true}` 表示应用可连接数据库；数据库不可用时返回 HTTP 503 和 `{"ready":false}`，不返回连接串或账号。

查看日志及停止：

```powershell
docker compose --env-file .env.oceanbase -f docker-compose.oceanbase.yml logs --tail 100 app
docker compose --env-file .env.oceanbase -f docker-compose.oceanbase.yml logs --tail 100 oceanbase
docker compose --env-file .env.oceanbase -f docker-compose.oceanbase.yml stop
```

后续更新使用同一个 Compose 项目名。源码构建默认镜像为 `hospital-system:local`，`HOSPITAL_APP_IMAGE` 可指定其他本地标签。数据库已运行且不需要调整时可执行 `docker compose --env-file .env.oceanbase -f docker-compose.oceanbase.yml up -d --build --no-deps app` 仅更新应用。更换源码目录时显式增加 `--project-name hospital_system` 以复用原项目，先核对原容器的项目标签和卷名。数据库卷和头像卷保留。不要使用 `down -v` 或卷删除命令。

## 4. 上传目录与旧头像迁移

新上传目录由 `HOSPITAL_UPLOAD_DIR` 配置。容器内使用 `/var/lib/hospital/uploads`，实际头像位于其 `avatars` 子目录，通过命名卷 `hospital-uploads` 保存。对外路径继续为 `/ustbhospital/uploads/avatars/文件名`。

升级前，从旧应用部署目录提取头像；执行期间旧应用先停止写入，避免漏掉复制期间的新上传：

```powershell
New-Item -ItemType Directory -Force .hospital-local\old-avatars | Out-Null
docker compose --env-file .env.oceanbase -f docker-compose.oceanbase.yml stop app
docker compose --env-file .env.oceanbase -f docker-compose.oceanbase.yml cp app:/usr/local/tomcat/webapps/ustbhospital/uploads/avatars/. .hospital-local/old-avatars/
```

若原头像目录不存在，先核对数据库保存的路径和原部署方式，不随意改变头像字段。对本机 Tomcat，同样复制原 `webapps/ustbhospital/uploads/avatars` 的文件到本地暂存目录。

先构建新应用，再用一次性容器把原文件复制到持久卷，最后启动应用：

```powershell
docker compose --env-file .env.oceanbase -f docker-compose.oceanbase.yml build app
docker compose --env-file .env.oceanbase -f docker-compose.oceanbase.yml run --rm --no-deps --entrypoint sh -v "${PWD}/.hospital-local/old-avatars:/migration:ro" app -c 'mkdir -p /var/lib/hospital/uploads/avatars && cp -n /migration/* /var/lib/hospital/uploads/avatars/'
docker compose --env-file .env.oceanbase -f docker-compose.oceanbase.yml up -d app
```

复制示例仅用于非空头像暂存目录。`cp -n` 不覆盖同名新文件；保留暂存副本，抽查原头像 URL 成功后再自行清理。不要使用同步删除参数。

## 5. 直接拉取已发布应用镜像

现有历史标签为 `ivan091107/hospital-system:v1.0.1-lsy-chl`，不包含这次尚未发布的工作区修改。需要先由维护者发布包含这些修改的新版本，才能让其他人拉取新功能。本次没有执行 Docker Hub 推送。

新建本地 `.env.image`，仅在本机填写实际值：

```dotenv
HOSPITAL_APP_IMAGE=替换为包含本次更新的完整镜像名及标签
HOSPITAL_DB_URL=jdbc:oceanbase://host.docker.internal:2881/hospt?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
HOSPITAL_DB_USERNAME=hospital_app@test
HOSPITAL_DB_PASSWORD=替换为本地业务密码
OCEANBASE_HOST=host.docker.internal
OCEANBASE_PORT=2881
HOSPITAL_HTTP_PORT=8080
```

上例用于 Windows Docker Desktop 访问宿主机已映射的 OceanBase 2881 端口。其他机器应改为容器可访问的真实数据库主机；容器内的 `localhost` 指向应用自身。

```powershell
docker compose --env-file .env.image -f docker-compose.image.yml pull
docker compose --env-file .env.image -f docker-compose.image.yml up -d
docker compose --env-file .env.image -f docker-compose.image.yml ps
```

此配置仅启动应用，数据库和账号必须事先准备。镜像部署和源码部署使用不同项目名时，会得到不同的头像卷；切换部署方式前需要明确复用或迁移原卷，不能将新空卷误当作头像已迁移。

## 6. 备份与恢复

脚本兼容 Windows PowerShell 5.1，保留 UTF-8 BOM。需要 MySQL 8 的 `mysqldump.exe`，以及支持 MySQL 协议的 `mysql.exe` 或 `obclient.exe`。密码由安全输入读取，也可由当前进程的 `HOSPITAL_OB_PASSWORD` 提供，不能写进命令参数、仓库或文档。

备份当前业务库是只读操作。请使用具有备份所需读取权限的本地账号，核对地址后执行：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\oceanbase\backup.ps1 -Database hospt -ServerHost 127.0.0.1 -Port 2881 -User hospital_app@test -ClientPath "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -DumpPath "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqldump.exe"
```

输出 SQL 和 `.sha256` 位于被忽略的 `database/oceanbase/export/backups/`，文件名带时间和随机值，不覆盖旧备份。备份使用一致性事务，不包含数据库创建/切换语句，去除同源外键的库名前缀；其他跨库引用会拒绝导出为恢复文件。

恢复只允许显式命名的 `hospital_restore_...` 或 `hospital_test_...` **现有空库**，不会直接覆盖 `hospt`。先由数据库管理员创建新的恢复库并仅授予该库权限。以下恢复目标和文件路径必须替换为实际值：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\oceanbase\restore.ps1 -TargetDatabase hospital_restore_20261027 -ConfirmTarget hospital_restore_20261027 -InputFile "C:\实际备份目录\备份文件.sql" -ServerHost 127.0.0.1 -Port 2881 -User 恢复专用账号@test -ClientPath "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -ValidateOnly
```

确认检查通过后，去掉 `-ValidateOnly` 才执行恢复。脚本核对 SHA256、连接目标和空库条件，支持受控的标准 mysqldump 显式建表、字面量 VALUES 插入和头尾 SET 语句；拒绝跨库 SQL、客户端命令、动态执行以及改变引号含义的 SQL 模式。它不是任意 SQL 脚本执行器。恢复后逐表核对行数、中文内容和业务关联，再单独决定是否切换业务连接；不自动切换。

## 7. 独立环境验证和本次实测

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\oceanbase\test-operations-safety.ps1
powershell -ExecutionPolicy Bypass -File .\scripts\oceanbase\run-isolated-tests.ps1 -StartIsolatedContainer
```

测试脚本启动随机 `hospital_test_` Compose 项目，默认使用 12881，创建随机测试库和受限账号。连接信息只保存在被忽略的 `.hospital-local`；不能输出、提交该文件。测试失败保留环境供检查，不删除真实库或业务卷。重复测试应准备新的空测试库，不能对已运行过的非空测试库再次初始化。

2026-10-27 实测结果：

- 默认 Maven：196 项，0 失败、0 错误、12 项环境测试跳过；6 项 OceanBase 集成另行执行全部通过。
- 实际 Dockerfile 构建成功，本地镜像为 `hospital-system:maintenance-local`，没有推送远程。
- 新镜像只在独立测试网络、18081 端口启动，真实 HTTP 完成注册、登录、CSRF、PNG 上传与下载。
- 应用重启及停止后重建，均可从原上传卷读到相同 SHA256 的头像。
- 独立错误数据库地址的应用返回 HTTP 503，正文仅为就绪状态。
- 修订后的备份恢复在独立恢复库完成，16 张表行数及备份 SHA256 一致；安全检查 44 项通过。
- 原业务容器、账号、业务数据及数据卷未重建、未覆盖。旧的 Docker Hub 标签未改变。

本次验证结束后，独立测试应用和测试 OceanBase 容器已停止，测试数据和卷保留供复查；不影响原业务容器的状态。
