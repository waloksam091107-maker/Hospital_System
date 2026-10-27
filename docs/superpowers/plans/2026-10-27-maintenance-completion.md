# 医院挂号系统六项完善 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans for sequential work and superpowers:dispatching-parallel-agents for the independent owned domains below. Steps use checkbox syntax for tracking.

**Goal:** 补齐排班、后台维护、删除限制、病历体验、Docker 运维和实际行为测试。
**Architecture:** 沿用 Servlet → Service → MyBatis。默认 H2 行为测试验证查询和事务，独立 OceanBase 测试验证数据库兼容性；不触碰真实业务数据。
**Tech Stack:** Java 8 source / JDK 17、JUnit 4、MyBatis、PageHelper、OceanBase、Docker Compose、PowerShell、现有 HTML/JS。
**Spec:** `docs/superpowers/specs/2026-10-27-maintenance-completion-design.md`（用户已确认）

## Global Constraints
- 仅修改当前目录；不创建其他 worktree；不执行 git add/commit/push，不发布 Docker Hub。
- 业务数据保留，禁止清库、删卷、向真实库写测试数据；新增数据库结构须提供增量升级。
- 默认分页 10，上限 100，关键字最多 100 字；角色、CSRF、审计和事务不退化。
- 短信邮件、退款、叫号、整套旧版清理和实验报告不在范围。
- 用户已授权实施，本计划后连续执行，不再为内部实施方式重复请求确认。

## Review Focus
1. 有历史引用的时段不能删除重建；并发预订不能留下已关闭但有效的预约。
2. 只有非零余额、没有流水的迁移患者同样不能删除。
3. 下拉选项必须完整，分页第一页不能代替全部科室/职称。
4. 新病历列表不能泄漏其他患者或草稿，旧数组响应不能被破坏。
5. 测试脚本中的 USE hospt 和 Docker 默认卷名不能绕过隔离。

## Task 1 — 排班（独立负责人：schedule agent）
**Files:** `service/DatesServiceImpl.java`, `mapper/DatesMapper.java`, `mapper/ScheduleSlotsMapper.java` 及对应 XML、医生排班页、`DatesScheduleMaintenanceTest`。
**Interfaces:** 继续使用现有 addDate/editForDoctor/deleteForDoctor；在自己的 Mapper 中增加锁定、历史/状态检查方法，不修改 AppointmentsMapper。
- [x] 在独立 H2 中写测试：取消历史允许原地调整且主键不变、有效占用不可关闭、越权不可改、历史日期不可移动、历史撤班只关闭、无历史物理删除、非法/过去日期拒绝、审计失败回滚。
- [x] 运行并确认失败来自现有规则。
- [x] 原地更新时段；行锁和条件更新；保留历史，统一撤班语义并更新页面提示。额外测试两事务预订/关闭同一号源。
- [x] 运行针对性测试，报告命令和结果；不得提交。

## Task 2 — 后台查询与校验（root）
**Files:** `AdminMasterDataService`, 新 `MasterDataQuery`, 四个 `PortalAdmin*Servlet`、四个基础数据 Mapper 和 XML、`hospro/admin-master-data.html`。
**Interfaces:** 列表仍含 items，增加 pageNum/pageSize/total/pages；`options=true` 返回受管理员保护的完整精简选项；保留旧无参 Service 方法供旧测试调用。
- [x] 写 `AdminMasterDataMaintenanceTest`，验证组合条件、稳定分页、无匹配、非法条件、完整选项、联系方式及日期校验。
- [x] 先运行失败测试，再实现参数对象、参数化查询和分页 Service；前端添加筛选分页，下拉选项独立加载。
- [x] 修复科室说明回填、成功消息消失和请求失败时输入丢失；不开放身份/余额编辑。
- [x] 运行针对性行为测试；页面交互以浏览器或真实 JS 执行验证。

## Task 3 — 删除保护（root，与 Task 2 顺序执行）
**Files:** 同后台 Service/Mapper、`PortalServletSupport`、Task 2 的行为测试。
- [x] 写余额非零但无流水、余额零但有流水、dates-only、病历、旧 orders 引用、下级科室和职称引用测试；无引用正常删除及审计回滚测试。
- [x] 运行失败测试；事务锁定待删除对象，分项检测并返回 HAS_REFERENCES/409；处理删除并发 FK 异常，保留历史。
- [x] 页面删除后维持筛选并修正空页，运行针对性测试。

## Task 4 — 病历与医嘱（独立负责人：clinical agent）
**Files:** `MedicalRecordsServiceImpl`, `MedicalRecordsMapper` 和 XML、新增 `MedicalRecordQuery`、`MedicalRecordsServlet`、`hospro/medical-records.html`、`doctor-schedule/encounter.html`、自己的测试文件。
**Interfaces:** 新 `/medical-records/patient/search` 分页入口；旧 `/patient` 数组和单条 API 保留。患者 ID 只来自 Session。
- [x] 先测试 FINAL/患者隔离、日期医生筛选、分页、空结果、批量医嘱，验证失败后实现。
- [x] 返回列表项目及页码元数据，加入医生与预约信息；一页批量读取医嘱。
- [x] 页面筛选分页及空/错状态；草稿修改跟踪、离开提醒、定稿确认、保存状态和失败/冲突保留输入；不保存病历正文到浏览器存储。
- [x] 运行自己的行为测试和页面 JS 检查；不修改后台/排班文件。

## Task 5 — Docker 与隔离测试设施（独立负责人：deployment agent）
**Files:** `Dockerfile`、Compose 文件、独立上传存储工具/头像 Servlet（不碰其他 Servlet）、就绪 Servlet、`scripts/oceanbase` 新脚本、自己的测试、部署文档。
**Interfaces:** `HOSPITAL_UPLOAD_DIR` 为外部持久目录，保留 uploads/avatars URL；公开就绪接口只返回 ready 状态；镜像配置由 `HOSPITAL_APP_IMAGE` 指定，不发布镜像。
- [x] 先写上传目录持久路径、非法路径拒绝和就绪响应等行为测试，再实现。
- [x] 固定可验证的 OceanBase 镜像，应用重启策略/健康检查和上传卷；新增镜像拉取配置，记录旧头像迁移步骤。
- [x] 实现带隔离前缀验证的 OceanBase 测试脚本与集成测试；不要执行硬编码 USE hospt 脚本，不碰真实库。
- [x] 新增备份/恢复脚本和明确目标保护，检查 PowerShell 5.1 编码/语法、Compose config、脚本拒绝危险输入。
- [x] Docker 未启动时如实记录，能安全启动 Docker Desktop 则尝试，不重建生产服务、不删卷。

## Task 6 — 整合、回归和文档（root）
- [x] 核对各域修改范围、接口兼容性和所有审核重点；读改动而非只相信报告。
- [x] 完整运行 `mvn test`，并 `mvn -DskipTests package`；检查每个失败/跳过原因。
- [x] Docker 可用时在隔离 Compose project/测试数据库验证；不可用则保留可复现命令并明确未验证。
- [x] 更新 README、接口说明和测试清单；记录每项行为与真实验证结果。
- [x] 独立全量审阅，修复实质问题后重跑相关及全部测试。
- [x] `git diff --check`、状态检查；汇报修改、测试和剩余环境操作，保持未提交。

## Progress / rulings
- 2026-10-27：用户确认设计；直接在用户指定目录执行，用户不允许提交，覆盖技能中的 worktree/commit 默认步骤。
- 排班、病历和部署具有独立文件所有权，可按并行技能执行；后台维护与删除由 root 顺序负责。

- 最终结果：默认 Maven 196 项、0失败/错误、12环境跳过；独立 OceanBase 6项全部通过；浏览器19项及排班JS行为通过；运维44项安全检查通过。
- 审阅修复：新增子科室与删除父科室锁定同一父行；H2双事务测试用force commit提交原生JDBC删除以正确验证串行化；恢复脚本采用受控dump词法/语句检查，拒绝跨库与可执行表达式。
- 实测：Dockerfile实际构建hospital-system:maintenance-local成功，独立18081 HTTP注册/登录/CSRF/头像上传及重启重建SHA256一致，错误数据库连接返回503；备份恢复16表行数/SHA一致。
- 未进行：真实业务库写入、生产容器替换、远程镜像发布、Git提交。测试资源停止但卷保留。
