# 前五项扩展：功能、接口与使用说明

本说明对应本工作区完成的五项扩展：站内通知、账号安全、多号源与预约序号、停诊请假、签到候诊。原有账号、余额、预约、病历、医嘱、流水、审计和头像继续保留。部署前必须完成[已有库增量升级](../../database/migrations/20261028_five_extensions.md)。

## 1. 功能与页面入口

下表对应需求图片中本次实施的前五项；图片中其余扩展不属于本次交付范围。

| 需求项 | 已实现内容 | 页面与使用角色 |
| --- | --- | --- |
| 站内通知 | 本人通知分页、未读数量、单条/全部已读、相关业务入口 | `/notifications.html`，患者、医生、管理员 |
| 账号停用与登录保护 | 启停、临时密码重置、解除锁定、会话撤销、强制改密、服务端退出 | `/hospro/account-security.html`，管理员管理医生和患者；所有角色接受登录保护 |
| 多号源与预约序号 | 时段容量、占用量、余号、单调递增预约号、取消释放、改期原子处理 | `/clinic-workflow.html` 中医生容量管理；原预约确认、患者和医生预约列表展示 |
| 停诊与请假 | 医生申请/撤回，管理员批准/拒绝/撤销，影响数量、冻结及恢复通知 | `/clinic-workflow.html`，医生和管理员 |
| 签到、候诊与过号 | 患者签到，医生叫下一位、开始接诊、过号、恢复候诊、人工确认爽约 | `/clinic-workflow.html`，患者和医生；病历仍使用原接诊页面 |

路径相对于应用上下文，例如应用部署为 `/ustbhospital` 时，门诊页面为 `/ustbhospital/clinic-workflow.html`。主要页面提供“门诊服务”“站内通知”等入口；管理员可进入“账号安全管理”。页面根据服务器会话显示角色功能，不能通过修改 URL 中的角色或账号编号取得其他身份。

## 2. 默认值与通用边界

| 项目 | 当前规则 |
| --- | --- |
| 业务时区 | `Asia/Shanghai`，与宿主机时区独立 |
| 时段 | `AM`、`PM`、`EVENING` |
| 时段结束 | 上午 12:00、下午 17:00、晚间 21:00；到结束时刻即按已结束处理 |
| 结束时间配置 | JVM 系统属性 `clinic.AM.end`、`clinic.PM.end`、`clinic.EVENING.end`，值如 `12:00`；修改后应同步核对页面提示 |
| 容量 | 新建排班每时段默认 10；升级的旧号源保持 1；可设为 1—1000，不能低于已占用量 |
| 登录失败 | 同一账号 15 分钟内失败 5 次，锁定 15 分钟；管理员账号同样适用 |
| 来源限流 | 同一来源 IP 每 15 分钟最多 60 次登录尝试，包括不存在的用户名；取 `getRemoteAddr()`，不采信客户端转发头 |
| 密码 | 新密码至少 8 个字符，UTF-8 编码不超过 72 字节；BCrypt 保存，旧账号密码不因升级被重置 |
| 停诊范围 | 当日或未来，起止日期均包含在内，最多 31 天；原因/审核意见 1—500 字 |
| 列表上限 | 门诊号源、候诊查询最多 300 条；停诊列表最多 200 条 |

日期必须为有效的 `YYYY-MM-DD`，例如 `2030-02-30` 会被拒绝。新预约和改期目标必须是尚未结束的时段。签到当前按“预约当天、结束时间之前”判断，没有新增时段开始时间限制。

受保护接口同时检查登录、账号启用状态及会话版本；写操作使用 POST 和 `X-CSRF-Token`。通过 `GET /security/csrf` 获取当前会话的 `token`，不能复用其他会话的令牌。身份、归属及角色以 Session 为准，客户端提供的对象编号只用于定位待校验的业务对象。

## 3. 患者操作流程

1. 登录后在医生详情及预约页面查看时段、容量和余号，选择尚未结束的可预约时段。预约成功后记下预约号；业务预约编号和预约号分别保留，不应把它们当作候诊人数。
2. 在“我的预约”或门诊服务页面查看候诊状态。预约当天、对应时段结束前，患者可以为自己的待接诊预约签到。未来日期、停诊范围、取消/改期旧记录、接诊中及终态预约均不能签到。
3. 签到后等待叫号。已叫号会产生本人站内通知；过号后由医生恢复到候诊队尾。恢复不改变原预约号。
4. 开始接诊前可以取消或改至同一医生的其他可预约时段。取消释放原占用量；改期在一个事务中释放原时段并占用新时段，新预约取得新号码，原记录保留为已改期。改期失败保留原预约，不重复扣挂号费。
5. 医生定稿病历后，在原病历页面查看自己的正式病历与医嘱，并收到完成通知；草稿仍不向患者开放。

停诊审批通过时，原待接诊预约保留，患者收到改期/取消提示。系统不自动分配其他医生、不批量取消预约、不自动退款。医生账号被停用时，其已有待接诊患者同样收到提示，可联系医院协调或自行改期、取消。

## 4. 医生操作流程

### 4.1 排班、容量和序号

管理员按原后台流程新增医生后，医生登录创建排班。新建排班默认每时段容量 10；仅新增医生不会自动生成排班。已有排班经升级保留容量 1，可由医生进入门诊服务页面，选择日期，为自己的时段保存新容量。容量不能小于当前占用量。

`capacity` 表示本次门诊可接诊人数，`occupiedCount` 表示有效占用量；待接诊、接诊中、已完成和爽约都占用本次额度。取消及改期旧记录释放额度。每次成功占用取得递增的 `appointmentSequence`，号码取消后不回收，因此号码可能大于容量。

开放/关闭、容量满额和停诊冻结分别判断。取消不会打开手工关闭的号源，也不会解除停诊。撤销停诊后仍须满足账号启用、排班开放、未过期和有余号等条件。后台预订与排班/停诊/候诊变更共用医生行锁，容量更新及唯一约束共同防止超售和同一患者重复持有同一时段的有效预约。

### 4.2 停诊申请

医生在门诊服务页面填写日期范围、一个或多个时段及原因，提交后状态为 `PENDING`，此时不会冻结号源。可撤回自己的待审核申请。同一医生重叠日期与时段已有待审核或已批准申请时，不能重复申请。

管理员批准后状态为 `APPROVED`，覆盖范围内停止接受新预约、签到、叫号和开始接诊；以后新增的覆盖范围内排班同样受冻结规则限制。原 `BOOKED` 预约保留；已候诊/已叫号的受影响记录退出当前队列并记录历史，恢复接诊后需重新签到。如果范围内已有接诊中预约，批准被拒绝，应缩小范围或等本次接诊完成。

### 4.3 候诊、叫号与病历

医生按日期和时段查看自己的队列，点击“叫下一位”。同一医生最多存在一名已叫号或接诊中患者，并发点击也只能成功叫出一位。未签到患者不阻塞已签到患者。

候诊到达与恢复事件写入 `queue_history`，按持久化的入队顺序选取下一位，配合签到时间、预约号及预约主键稳定排序。过号恢复会追加新的入队事件，排到现有候诊者之后；数据库时间戳精度较低也不依赖毫秒差判断队尾。

只有 `CALLED` 患者可以开始接诊，然后进入原病历草稿、医嘱和定稿流程。定稿在同一事务完成病历、医嘱、预约 `COMPLETED`、候诊 `DONE`、候诊历史、审计与通知。旧直接完成接口不能绕过病历。

已叫号但未到场可标为 `SKIPPED`；当天未结束且未停诊时可恢复 `WAITING`。时段结束后，医生仅能将未签到或过号且尚未接诊者确认为 `NO_SHOW`。正常 `WAITING` 患者、停诊造成无法接诊的患者不能标记爽约。爽约为终态，不重新放号、不退款；没有后台定时批量爽约任务。

| 预约主状态 | 候诊状态 | 含义 |
| --- | --- | --- |
| `BOOKED` | `NOT_CHECKED_IN` / `WAITING` / `CALLED` / `SKIPPED` | 尚未开始接诊，候诊细分状态独立保存 |
| `IN_PROGRESS` | `IN_PROGRESS` | 已开始接诊 |
| `COMPLETED` | `DONE` | 病历已定稿完成 |
| `CANCELLED` / `RESCHEDULED` | `REMOVED` | 原预约保留，已移出候诊队列 |
| `NO_SHOW` | `NO_SHOW` | 医生在符合条件时确认爽约 |

## 5. 管理员操作流程

管理员在门诊服务页面查看待审核申请及受影响的排班/预约数量，填写审核意见后批准或拒绝；可填写原因撤销已批准的停诊。医生没有审批权限，审批由管理员会话执行。撤销只移除本次冻结，不覆盖排班原来的开闭配置，也不恢复过期或满额号源。

账号安全管理页面可在医生/患者之间切换，分页查看状态并执行以下操作：

| 操作 | 结果 |
| --- | --- |
| 停用 | 阻止登录，递增会话版本，已有登录在后续受保护请求失效；历史数据和预约保留 |
| 启用 | 恢复账号资格；旧会话仍失效，不自动打开关闭或停诊中的号源 |
| 重置密码 | 管理员填写临时密码，数据库只写 BCrypt 哈希；撤销旧会话，要求下次登录先改密；不会自动启用停用账号 |
| 解除锁定 | 清除账号失败计数和短时锁定；来源 IP 限流仍独立生效 |

成功登录会清除该账号失败计数；锁定到期可以重试。用户不存在、密码错误、停用或锁定使用相同登录失败提示。临时密码用户可取得 CSRF 令牌、修改密码或退出，其他受保护业务请求返回 `PASSWORD_CHANGE_REQUIRED`。本人修改密码成功后也撤销全部旧会话，需要重新登录。

退出通过 `POST /auth/logout` 销毁服务端 Session 和其中的 CSRF 状态。旧替代登录及不安全敏感接口已停用或纳入保护，不能用旧入口绕过账号控制。本次没有新增管理员账号创建/删除管理。

## 6. 站内通知与事务一致性

通知按“接收角色 + 接收账号”隔离，同号患者和医生互不串消息。通知页面默认每页 10 条、最大 100 条，按创建时间及主键倒序显示，支持本人单条已读和全部已读。页面把标题、正文和姓名作为文本显示。通知详情跳转后，目标接口仍执行权限检查。

| 业务事件 | 通知接收者 |
| --- | --- |
| 预约成功、改期、取消 | 相关患者和医生 |
| 停诊申请提交 | 管理员 |
| 停诊撤回、批准、拒绝、撤销 | 相关医生；批准/撤销同时通知覆盖范围内的相关待接诊患者 |
| 医生账号停用 | 该医生已有待接诊预约的患者 |
| 签到、叫号、过号、恢复候诊、开始接诊、爽约状态变更 | 该预约患者 |
| 病历定稿完成 | 该预约患者 |

业务产生的通知、对应数据变更及成功审计在同一数据库事务提交。通知写入失败时业务回滚，避免“成功预约但没有相应消息”等部分结果。事件键与接收角色/账号组成唯一约束，重复写入不会产生同一事件的重复消息。通知只包含必要操作提示，不包含密码、验证码、完整身份证号或病历正文。

当前发送渠道只有站内通知；没有短信、邮件、浏览器推送或外部消息发送，也没有补发历史业务通知。

## 7. 接口速查

以下 GET/POST 均在应用上下文下。表单写入使用 `application/x-www-form-urlencoded`，JSON 响应通常包含 `success/code/message`。认证失效为 401，越权或 CSRF 拒绝为 403，参数无效通常为 400，业务状态/容量冲突为 409；数据库或事务故障返回 500/503，应按响应正文提示处理。

### 7.1 通知

| 方法与路径 | 参数与结果 | 角色 |
| --- | --- | --- |
| GET `/notifications/list` | `page=1&pageSize=10`；返回 `items/total/page/pageSize/role` | 当前登录人 |
| GET `/notifications/unread-count` | 返回 `unreadCount` | 当前登录人 |
| POST `/notifications/read` | `id`；仅能标记本人消息 | 当前登录人 |
| POST `/notifications/read-all` | 返回本次 `updated` 数量 | 当前登录人 |

`items` 包括 `id/type/title/body/targetType/targetId/createdAt/readAt`，不接受客户端指定接收角色或接收人来切换归属。

### 7.2 容量与预约

| 方法与路径 | 参数与结果 | 角色 |
| --- | --- | --- |
| GET `/clinic/slots` | `doctorId`、可选 `date`；医生省略 doctorId 时查本人；返回 `success/list` | 已登录账号 |
| POST `/clinic/slots` | `action=capacity&slotId=...&capacity=...` | 医生，仅本人号源 |
| POST `/appointments/book` | `doctorId/appointment_date/appointment_time` | 患者 |
| POST `/appointments/cancel` | `appointmentId` | 患者，仅本人预约 |
| POST `/appointments/reschedule` | `appointmentId/appointmentDate/appointmentTime` | 患者，仅本人预约 |
| GET `/appointments/patient`、`/appointments/doctor` | 沿用 `pageNum/pageSize`；预约记录增加 `appointmentSequence/queueStatus` | 分别为本人患者/医生 |

号源 `list` 的字段包括 `scheduleSlotId/doctorId/scheduleDate/slotPeriod/slotStatus/capacity/occupiedCount/nextSequence/remaining/leaveBlocked/doctorEnabled`。`remaining` 已结合关闭、停诊、账号和过期条件计算，不能仅凭 `capacity - occupiedCount` 推断当前可预约量。查询结果与随后预订可能竞争，最终以预订事务结果为准。

### 7.3 停诊

| 方法与路径 | 参数 | 角色 |
| --- | --- | --- |
| GET `/clinic/leave` | 返回 `success/list`；每项附 `affectedAppointments/affectedSlots` | 医生查本人，管理员查全部 |
| POST `/clinic/leave` | `action=apply`，`startDate/endDate/periods/reason`；多时段如 `AM,PM` | 医生 |
| POST `/clinic/leave` | `action=withdraw&id=...`，可选 `reason` | 医生，仅本人待审核申请 |
| POST `/clinic/leave` | `action=approve` 或 `reject`，`id/reason` | 管理员 |
| POST `/clinic/leave` | `action=revoke&id=...&reason=...` | 管理员，仅已批准申请 |

停诊项包含 `leaveId/doctorId/startDate/endDate/periods/reason/status/reviewNote/createdAt`。已处理申请的重复操作不重复批准或重复生成消息。

### 7.4 候诊与接诊

| 方法与路径 | 参数 | 角色 |
| --- | --- | --- |
| GET `/clinic/queue` | 可选 `date/period`，返回 `success/list` | 患者仅本人，医生仅本人队列 |
| POST `/clinic/queue` | `action=check-in&appointmentId=...` | 患者 |
| POST `/clinic/queue` | `action=call-next&date=YYYY-MM-DD&period=AM` | 医生 |
| POST `/clinic/queue` | `action=skip`、`requeue`、`no-show` 或 `start`，`appointmentId` | 医生 |
| POST `/appointments/start` | `appointmentId`；沿用原入口，同样仅允许已叫号预约 | 医生 |
| POST `/medical-records/draft`、`/medical-records/finalize` | 沿用原病历字段与草稿版本控制 | 医生 |

队列项含预约字段、`appointmentSequence/queueStatus/checkedInAt/calledAt/patientName/leaveBlocked`。签到、叫号、取消/改期、停诊审批和开始接诊通过同一医生锁串行处理，失败保留原状态，不留下悬空叫号。

### 7.5 账号与会话

| 方法与路径 | 参数与结果 | 角色 |
| --- | --- | --- |
| POST `/auth/login` | `user/password/roleCode`，roleCode 为医生 1、患者 2、管理员 3；也兼容 `typeId` | 公开登录入口 |
| GET `/portal/admin/account-security/list` | `role=DOCTOR` 或 `PATIENT`，`page=1&pageSize=20`；最大每页 100；返回 `success/items/page/pageSize` | 管理员 |
| POST `/portal/admin/account-security/enable`、`disable`、`unlock` | `role/accountId` | 管理员 |
| POST `/portal/admin/account-security/reset` | `role/accountId/temporaryPassword` | 管理员 |
| POST `/password/reset` | `currentPassword/newPassword`，操作本人账号；成功后重新登录 | 当前登录人 |
| GET `/security/csrf` | 返回当前会话 `token` | 当前登录人 |
| POST `/auth/logout` | 无业务编号，须提交 CSRF 令牌 | 当前登录人 |

账号 `items` 只返回 `id/name/enabled/mustChangePassword/lockedUntil` 等管理状态，不返回密码或哈希。

## 8. 数据库升级与部署前提

源码交付不会自动升级真实数据库、替换运行中的业务容器或发布镜像。Docker Hub 现有历史标签 `ivan091107/hospital-system:v1.0.1-lsy-chl` 未被本次更新或覆盖，不能用该历史镜像验证本工作区的新增功能。

空库初始化使用 `database/oceanbase/01_hospt_schema.sql`。已有库必须按[增量升级说明](../../database/migrations/20261028_five_extensions.md)执行 `ExtensionSchemaUpgrade`：先在独立测试库演练并备份核对，在维护窗口停止全部应用写入，完成预检、字段/约束和数据回填，确认 `extension_schema_progress` 的 `COMPLETE` 阶段及业务对账后再启动新程序。不能删除旧表、重导演示数据，不能让旧单号源应用与新应用同时写同一业务库。

升级保留原账号和业务主键，旧号源容量为 1，不重置旧密码、不补发历史通知。遇到无法推断的数据异常，升级器停止并给出清单；OceanBase DDL 可能分步提交，失败时继续保持停写，按升级说明排查并重试。

仅在维护升级完成、连接配置及持久化卷核对完成后，才按现有部署配置本地构建并启动新应用，例如：

```powershell
docker compose --env-file .env.oceanbase -f docker-compose.oceanbase.yml up -d --build app
```

该命令不替代增量升级。不要执行 `down -v` 删除业务数据库或头像卷。本次工作区没有代用户执行 Git 提交、推送或镜像发布。

## 9. 可重复验证

在项目目录执行完整 Java 验证并构建 WAR：

```powershell
mvn -B verify
```

页面交互使用真实 Chromium/Edge 和受控 HTTP 响应，不修改真实业务库。需要 Node 能解析 `playwright` 包，并安装 Edge，或设置 `PLAYWRIGHT_BROWSER_PATH`：

```powershell
node tests/five-extensions-ui.cjs
node tests/account-security-ui.cjs
node tests/notification-center-ui.cjs
```

默认 Java 测试包含独立 H2 事务、并发、权限和回归；默认跳过显式启用的 MySQL/OceanBase 测试，因此默认验证成功不能代替真实 OceanBase 升级或并发验证。实际结果应查本次运行的 `target/surefire-reports` 和对应日志，而非仅凭本说明中的命令判断。

真实 OceanBase 基线测试使用现有隔离工具准备随机数据库和受限账号：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/oceanbase/run-isolated-tests.ps1 -StartIsolatedContainer -PrepareOnly
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/oceanbase/run-isolated-tests.ps1 -RunPrepared -KeepRunning
```

隔离工具验证 Compose 项目、端口、随机 `hospital_test_<32位十六进制>` 库名及 `hospital_test_runner@test`，拒绝业务端口；本地会话文件保存在忽略的 `.hospital-local/`。第二条命令运行既有基线集成测试，要求准备好的库仍为空。

新增真实升级验证使用单独新建的空隔离数据库，按[升级说明](../../database/migrations/20261028_five_extensions.md)配置 `HOSPITAL_TEST_DB_*` 后执行：

```powershell
mvn -B '-Dhospital.test.oceanbase.upgrade=true' '-Dtest=ExtensionSchemaUpgradeOceanBaseTest' test
```

新增门诊实际锁和业务事务测试 `ClinicOceanBaseTest` 则使用已通过空库初始化基线的隔离数据库，在明确配置相同受限连接的终端执行：

```powershell
mvn -B '-Dhospital.test.oceanbase.clinic=true' '-Dtest=ClinicOceanBaseTest' test
```

升级测试和基线初始化都要求独立空库，不能把同一已写入的库轮流交给二者初始化。隔离环境使用人员需按对应脚本/测试要求分别准备，并核对库名和受限账号；不将测试连接改为真实 `hospt` 数据库。

### 9.1 真实容器端到端测试

在上述隔离数据库已完成基线初始化、容器仍运行且未占用 18081 端口时，可准备虚构账号并启动本地测试应用。此流程仅使用 `hospital_test_` 随机测试项目，不读取真实业务账号，密码只写入忽略的 `.hospital-local/`。

```powershell
docker build -t hospital-system:five-extensions-local .
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/oceanbase/prepare-five-extensions-http.ps1 -SessionFile .hospital-local/five-extensions-test-session.json
python tests/five-extensions-http.py
node tests/five-extensions-live-ui.cjs
```

`SessionFile` 须指向本次 `run-isolated-tests.ps1` 生成的会话文件；使用其默认文件时传 `.hospital-local/oceanbase-test-session.json`。HTTP 脚本会在虚构数据上执行预约扣费、改期、候诊、爽约、病历定稿、停诊各审核分支、账号启停、锁定解锁和改密；真实页面脚本随后使用其中保留的待签到测试预约完成签到到病历定稿并操作审批与启停。因此每份 fixture 的两条测试命令按顺序各运行一次；重复验证需先停止该次测试应用，再重新准备新的虚构账号。它不会自动清理测试库或数据卷。

测试容器名记录在 `.hospital-local/five-extensions-runtime.json`，测试完成后只停止其中记录且标签 `hospital.five-extensions.test=true` 的容器，再停止对应独立 OceanBase 项目。不要停止或删除业务 Compose 项目。

端到端脚本使用当天晚间时段，请在北京时间 21:00 前执行，避免把已结束时段的正确拒绝误判为故障；纯 Java 时段边界测试使用固定时钟，不受执行时间影响。本轮测试结果见[五项扩展验收记录](../testing/five-extensions-acceptance.md)。
