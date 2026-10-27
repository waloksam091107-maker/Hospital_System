# 2026-10-27 六项完善验收

## 环境边界

源代码目录为 `Hospital_system_new/Hospital_system`，修改基线 `5cbc67c`。不提交 Git、不发布 Docker Hub，不使用真实患者余额或真实业务库进行测试。

默认 Java 测试使用每例独立的 H2 数据库。OceanBase 集成必须通过 `scripts/oceanbase/run-isolated-tests.ps1` 显式启动，使用随机 `hospital_test_` 库、受限账号和独立端口，不能把 JDBC 地址改为业务库。浏览器脚本打开实际 HTML，但拦截接口返回固定测试数据；不能将其结果称为真实账号端到端测试。

## 业务和接口检查

| 范围 | 必须观察到的结果 | 自动验证入口 |
|---|---|---|
| 取消、改期后的排班 | 原时段 ID 不变，其他未占用时段可调整，预约历史仍能关联 | DatesScheduleMaintenanceTest |
| 有效占用及并发 | 待接诊/接诊中/已完成所占时段不被改写；预订和撤班竞争不能同时成功 | DatesScheduleMaintenanceTest、IsolatedOceanBaseIntegrationTest |
| 排班删除 | 无历史才物理删除；有历史撤班关闭开放号源；重复撤班明确报冲突 | DatesScheduleMaintenanceTest、schedule-maintenance.test.cjs |
| 日期与角色 | 非法/过去日期 400，归属越权 403，占用冲突 409 | ScheduleMaintenanceHttpResultTest |
| 管理员查询 | 关键字服务器筛选、稳定分页，百分号按字面查询，非法页码拒绝 | AdminMasterDataMaintenanceTest |
| 完整选项 | 第 25 个科室也出现在下拉框，列表使用名称，编辑回填外键 | AdminMasterDataMaintenanceTest、admin-master-data-ui.cjs |
| 科室编辑 | 原说明正常回填，保存成功提示保留；失败时输入不被清空 | admin-master-data-ui.cjs |
| 后台输入 | 管理员患者创建沿用注册格式校验，医生入职日期严格解析 | AdminMasterDataMaintenanceTest |
| 删除保护 | 预约、排班、病历、流水、旧医嘱、下级科室等引用拒绝删除；非零余额即使无流水也拒绝 | AdminMasterDataMaintenanceTest |
| 请求意图 | 无效 update 编号不能变成新增；create 中的客户端编号不能覆盖已有职称 | AdminMasterDataRequestTest |
| 事务与审计 | 审计失败时业务修改完整回滚，不留下删除、扣费或部分病历 | 多项 H2 行为测试、IsolatedOceanBaseIntegrationTest |
| 病历查询 | 仅本人 FINAL；按预约日期/医生筛选，分页稳定，空页正常；旧列表和单条接口兼容 | MedicalRecordSearchTest、MedicalRecordSearchEndpointTest |
| 医嘱读取 | 页内医嘱批量查询，空页不执行空 IN；不会混入另一患者或草稿的医嘱 | MedicalRecordSearchTest |
| 草稿编辑 | 正文及新增/删除医嘱触发未保存提醒，保存成功清除，失败和冲突保留内容/版本 | doctor-encounter-ui.cjs |
| 病历定稿 | 取消确认不发请求，保存中不能重复提交，既有必填和正式病历不可覆盖规则保留 | doctor-encounter-ui.cjs、MedicalDraftLifecycleTest |
| 上传与就绪 | 文件位于 WAR 外，非法路径拒绝，头像路由返回原始字节；数据库断开时就绪 503 | DeploymentRuntimeBehaviorTest |

## 命令

```powershell
mvn test
mvn -DskipTests package
node src/test/js/schedule-maintenance.test.cjs
node tests/admin-master-data-ui.cjs
node tests/doctor-encounter-ui.cjs
node tests/patient-medical-records-ui.cjs
powershell -ExecutionPolicy Bypass -File .\scripts\oceanbase\test-operations-safety.ps1
powershell -ExecutionPolicy Bypass -File .\scripts\oceanbase\run-isolated-tests.ps1 -StartIsolatedContainer
```

浏览器脚本需 Node.js、Playwright 和 Chromium/Edge。可在自己准备的测试工具目录安装 `playwright`，将该目录的 `node_modules` 加入 `NODE_PATH`，并用 `PLAYWRIGHT_BROWSER_PATH` 指定浏览器可执行文件；脚本默认使用 Windows Edge。安装依赖不需要在业务 WAR 中打包浏览器。

新增结构没有改变现有业务表，本次不需要清库或重导业务数据。上传目录变化需要按部署手册迁移旧头像文件。

## 本次执行记录

- 修改前基线：145 项，0 失败、0 错误，6 项 MySQL 环境测试跳过。
- 完整默认回归：196 项，0 失败、0 错误，12 项跳过（6 项旧 MySQL 环境测试及 6 项需显式启用的 OceanBase 测试）。
- 独立 OceanBase：6 项实际执行，0 失败、0 错误、0 跳过。
- WAR 已成功打包。
- 浏览器后台 4 项、医生病历 12 项、患者病历 3 项通过；排班 JavaScript 行为及语法检查通过。
- 缺审计表等故障注入会产生预期服务端日志；验收以事务回滚及测试断言为准，不应把该日志误记为业务库故障。

运维脚本 44 项校验通过，包括注释分隔、跨库查询、重命名、动态执行及合法正文处理。Docker HTTP、重建头像持久化及备份恢复的实测记录见 `docs/operations/maintenance-deployment.md`。部署当前目录不会更新远程镜像标签，旧 `v1.0.1-lsy-chl` 仍是此前发布版本。
