# 安全功能补全人工验收清单

以下步骤在已执行 OceanBase 增量迁移、Tomcat 已部署最新 WAR 的前提下进行。每项写操作都应先在浏览器获得有效登录会话；不要把数据库密码、身份证号或病历正文复制到审计日志截图中。

1. 以未登录浏览器访问 `/ustbhospital/portal/admin/doctors`，预期 HTTP 401；以患者或医生会话访问，预期 HTTP 403。
2. 管理员打开 `hospro/admin-master-data.html`：新建科室、职称和医生，确认医生工号自动生成；尝试删除有排班/预约的医生、有下级/医生的科室或被医生使用的职称，预期 HTTP 409 和中文提示。
3. 患者打开 `hospro/register-page.html` 注册，随后登录并打开 `portal-profile.html`；修改自己的联系方式和头像。尝试在请求中伪造其他用户编号，预期不会修改其他账户。
4. 患者打开 `patients-pay.html` 进行小额模拟充值，确认余额和 `balance_transactions` 出现 `RECHARGE`；创建预约后确认出现 `APPOINTMENT_DEBIT`，取消预约后确认不产生退款记录。
5. 医生创建排班、患者预约、医生开始接诊并保存草稿病历/医嘱，最后定稿；用另一名医生或患者访问该病历，预期为 403 或无数据。
6. 管理员打开 `admin-announcements.html` 新建草稿；在 `news.html` 确认草稿不可见。发布后可见，下线后再次不可见。检查审计日志包含对应 `ANNOUNCEMENT_*` 动作但没有公告正文。
7. 管理员打开 `audit-logs.html`，核对登录、预约、接诊、病历、余额、基础数据和公告的成功/失败元数据；页面不应显示密码、身份证号、病历正文或医嘱正文。
8. 访问旧写路径（例如 `/news/add`、`/save`、`/delete`）并确认返回 410；正常的新页面不应再调用这些路径。
