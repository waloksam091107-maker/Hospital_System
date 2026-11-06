# 2026年11月6日系统复核与本地部署记录

本次继续检查已有工作区，没有重置或覆盖有效修改。代码基线为 `89d1c7c`，下列修复保留在工作区，未暂存、提交或推送。功能写入测试只连接随机命名的独立 OceanBase 测试库，未使用真实业务账号执行预约、充值或医疗操作。

## 修复结果

| 问题 | 处理方式 | 验证 |
|---|---|---|
| 账号安全列表切换角色时，旧请求可能覆盖新列表，旧行按钮可能携带另一角色 | 请求序号过滤过期响应；加载时清空旧行和重置表单；行按钮固定角色，校验当前请求状态 | 3 项新增请求乱序、加载失败和角色操作回归；既有账号测试及真实浏览器操作通过 |
| 手机账号安全页的表格、长姓名和重置对象导致页面横向溢出 | 滚动范围限制在表格容器内；调整容器宽度、换行及手机间距 | 390 像素页面巡检通过 |
| 管理员返回入口仍到达过时的英文统计模板，部分链接丢失部署路径 | 保留当前新版功能，统一中文管理入口；删除无数据来源的卡片；相对链接保留应用上下文；JSP 检查真实会话身份 | 4 项真实浏览器导航和访问边界测试通过，含手机布局、停诊页返回及缺失页面回退 |
| 预约通知携带目标编号，预约列表却始终读取第一页 | 两类预约列表接收正整数 `appointmentId`，按会话身份和主键共同查询；缺失或越权返回 404，非法输入返回 400；页面清除旧结果并显示错误 | 真实患者和医生最后一页目标定位、非法编号、缺失记录、跨患者拒绝及页面提示均通过 |

## 本轮实测

| 检查 | 实际结果 | 本地证据 |
|---|---|---|
| Maven 测试及 WAR 打包 | 315 项中通过 298 项、跳过 17 项，失败及错误为 0 | `target/recheck-final-maven.log` |
| OceanBase 门诊并发、病历事务和账号时间处理 | 显式启用 4 项，全通过 | `target/recheck-20261106-oceanbase-clinic-security.log` |
| OceanBase 旧库增量升级与数据保留 | 新建随机测试库，1 项通过 | `target/recheck-20261106-oceanbase-upgrade.log` |
| 前一日同一隔离环境数据库基础检查 | 6 项通过，单独记录日期，不计作今天重新执行 | `target/recheck-20261105-oceanbase.log` |
| 真实 HTTP 扩展流程 | 168 个断言通过 | `target/recheck-final-http.log` |
| 注册、三角色权限、资料头像、充值、基础数据、公告和审计 | 146 项检查通过 | `target/recheck-final-portal.log` |
| 收入报表和日期边界 | 27 项检查通过 | `target/recheck-final-income.log` |
| 签到、叫号、过号、重排、接诊、病历草稿及定稿、医嘱、通知、容量、停诊审批、账号启停 | 真实浏览器操作通过 | `target/recheck-final-clinic.log` |
| 科室到医生、确认挂号费、预约、本人记录、取消 | 真实浏览器操作通过；扣费一次、取消不自动退款、容量回补 | `target/recheck-final-booking.log` |
| Node 源码交互回归 | 23 项中通过 19 项；4 项管理员真实环境检查另行启用 | `target/recheck-final-js.log` |
| 管理员入口真实浏览器测试 | 4 项均通过，不再跳过 | `target/recheck-final-admin.log` |
| 预约通知目标定位 | 两角色目标、权限与错误提示均通过 | `target/recheck-final-target.log` |
| 既有浏览器交互回归脚本 | 54 条 PASS，脚本整体退出码为 0 | `target/recheck-final-browser.log` |
| 桌面及手机路由巡检 | 33 条路由 × 2 个尺寸，共 66 页；脚本异常、资源失败、破图、整页横向溢出均为 0 | `target/recheck-final-pages.log`、`target/full-system-ui/page-smoke.json` |
| 更新后的本地部署 | 应用和数据库均健康；22 张业务表行数及逐行内容摘要一致 | `target/recheck-final-deployment.log` |
| 8080 端口公开页面、就绪状态和匿名管理入口 | 10 项通过；未登录，未执行业务写入 | `target/recheck-final-deployed-public.log` |

默认跳过项未计入通过数。测试方法数、HTTP 断言数、浏览器检查数分别列出，不相加作为测试总数。收入测试使用重新生成的合成边界流水验证日期筛选；取消不退款由实际预约浏览器流程另行验证。短信、邮件验证码及真实支付通道没有纳入本次范围。

## 本地 Docker 状态

- 应用容器：`hospital_system-app-1`，端口 `8080`。
- 数据库容器：`hospital_system-oceanbase-1`，仍为 OceanBase，宿主机数据库端口仅绑定 `127.0.0.1`。
- 本地最新镜像：`hospital-system:local`，另保留内容相同的 `hospital-system:five-extensions-local`。
- 两个本地标签的镜像 ID：`sha256:f14008a51c3d358ee57c273403b83c10ee88cbb858196321e10b3d7b8bf7fdbb`。
- 容器内 WAR 与本地 Maven WAR 的全部应用文件内容一致，仅构建环境生成的 `META-INF/maven/org.example/ustbhospital/pom.properties` 不同；不能仅用 ZIP 文件整体摘要判断源码是否一致。
- 数据库沿用 `hospital_system_oceanbase-data` 和 `hospital_system_oceanbase-cluster`，头像沿用原持久卷。没有清库、重导 MySQL、删除卷或运行演示数据初始化。
- 部署完成后，隔离测试应用和数据库已停止，测试卷保留；正式应用和数据库保持运行。
- Docker Hub 的 `ivan091107/hospital-system:v1.0.2-lsy-chl` 未更新；本轮没有 Docker Hub 推送，不表示已发布标签包含这些修复。

访问：`http://localhost:8080/ustbhospital/hospro/login.html`。

```powershell
docker compose --project-name hospital_system --env-file .env.oceanbase -f docker-compose.oceanbase.yml ps
curl.exe --fail http://localhost:8080/ustbhospital/health/ready
```

## 实验报告同步

直接修改上一级目录中的原 `.docx`，保留已有详细功能说明、分工、教师实验要求、源码和截图。增加本轮界面状态、账号请求顺序、管理员入口和通知目标权限的说明，以及 3 段当前源码、2 张真实运行截图；代码清单统一按阅读顺序编号，合计 100 段，插图合计 37 张。目录不含负责人姓名。

历史验收结果与本轮实测分别注明日期；已发布 Docker Hub 标签与本地未发布镜像分别说明。最终目录与页码由 Word 更新，文档另经渲染检查；中间 PDF、截图和辅助脚本仅保留在本地 QA 目录，不作为另一个报告副本交付。

## 复核结论与边界

本轮已执行的功能和异常场景全部通过，没有留下已知的功能阻塞问题。该结论限定于上述测试环境和检查范围，不能据此保证任意未来数据、并发规模或外部服务均不会产生问题。真实业务数据只读校验通过，代码和报告均未进行 Git 提交。
