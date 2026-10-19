# 预约闭环与规则统一：第一阶段设计

## 目标

在不删除现有 `dates` 和 `appointments` 数据的前提下，将医生排班、患者预约、取消、改期、开始/完成接诊统一为可验证的状态机，并为新接口建立基础认证、角色和 CSRF 防线。

## 现有兼容约束

- 技术栈固定为 Java 8、Servlet 3.1、MyBatis 3.2、MySQL 8 和静态 HTML/JSP。
- `dates` 暂时保留，旧页面可读取；新业务写路径改为 `schedule_slots`。
- 现有 `appointments` 保留主键和历史记录；不得用改期覆盖原记录。
- 取消预约沿用当前“不自动退款”规则，接口必须明确返回该规则。
- 任何身份必须来自 HttpSession，不能以 URL 或表单中的 `doctorId`、`patientId` 作为授权依据。

## 数据模型

### schedule_slots

`schedule_slot_id BIGINT AUTO_INCREMENT`、`doctor_id INT`、`schedule_date DATE`、`slot_period VARCHAR(20)`、`slot_status VARCHAR(20)`、`created_at DATETIME`、`updated_at DATETIME`。

- `UNIQUE(doctor_id, schedule_date, slot_period)`。
- `slot_period` 仅允许 `AM`、`PM`、`EVENING`。
- `slot_status` 仅允许 `AVAILABLE`、`BOOKED`、`CLOSED`。
- 迁移时，`dates` 中值为 `1` 的时段创建为 `AVAILABLE`；存在有效预约的时段覆盖为 `BOOKED`。

### appointments 增量字段

新增 `schedule_slot_id`、`previous_appointment_id`、`started_at`、`completed_at`、`cancelled_at`、`updated_at`、`version`。原表保留，并调整状态约束为：`BOOKED`、`RESCHEDULED`、`CANCELLED`、`IN_PROGRESS`、`COMPLETED`。

- `schedule_slot_id` 指向实际排班时段。
- 改期把旧预约置为 `RESCHEDULED`，创建带 `previous_appointment_id` 的新 `BOOKED` 预约。
- `version` 用于后续乐观锁扩展；本阶段的并发占用以 `UPDATE schedule_slots ... WHERE slot_status='AVAILABLE'` 和数据库唯一索引保证。

## 状态机与规则

```text
BOOKED --患者取消--> CANCELLED
BOOKED --患者改期--> RESCHEDULED -> 新 BOOKED
BOOKED --医生开始接诊--> IN_PROGRESS
IN_PROGRESS --后续病历阶段完成--> COMPLETED
```

- 只有 `BOOKED` 可以取消或改期。
- 只有预约所属医生可将 `BOOKED` 改为 `IN_PROGRESS`。
- 第一阶段保留现有“完成接诊”接口的兼容实现；第二阶段改为病历最终提交时完成。
- `CANCELLED`、`RESCHEDULED` 释放原 `schedule_slot`；`BOOKED`、`IN_PROGRESS`、`COMPLETED` 保持占用。
- 删除或关闭排班时段前必须不存在有效预约。

## 接口边界

- `GET /appointments/available`：患者读取可预约时段。
- `POST /appointments/book`：患者创建预约。
- `POST /appointments/cancel`：患者取消自己的预约。
- `POST /appointments/reschedule`：患者改期自己的预约。
- `POST /appointments/start`：医生开始自己的预约。
- `POST /appointments/complete`：兼容端点，仅允许医生完成自己的接诊中预约；第二阶段会替换为病历最终提交。
- `GET /appointments/patient` 与 `GET /appointments/doctor`：按会话身份读取列表。
- `POST /schedules/slots`、`POST /schedules/slots/update`、`POST /schedules/slots/close`：医生只能维护本人排班。

所有写接口必须为 POST，并要求 `X-CSRF-Token`。未登录返回 401、角色或资源归属不符返回 403、状态冲突返回 409、参数非法返回 400。

## 前端范围

- 医生预约页显示新状态，为 `BOOKED` 显示“开始接诊”，为 `IN_PROGRESS` 显示“继续接诊/完成接诊”。
- 患者预约记录显示 `RESCHEDULED` 和取消说明；仅 `BOOKED` 可显示取消、改期操作。
- 排班页按时段显示 `AVAILABLE`、`BOOKED`、`CLOSED`，不再将同一个 `0` 解释为多种业务含义。
- 通过同源 AJAX 自动携带 CSRF Token；不再使用 URL 中的身份参数作为授权来源。

## 验收

1. 两个患者并发预约同一医生、日期、时段，只有一个成功。
2. 患者不能取消、改期其他患者预约；医生不能开始或完成其他医生预约。
3. 改期后原预约为 `RESCHEDULED`，新预约保留来源关联，旧时段可再次预约。
4. 取消后原时段重新为 `AVAILABLE`；开始接诊后时段仍为 `BOOKED`。
5. 缺失会话、错误角色、缺失或错误 CSRF Token 分别得到标准拒绝响应。
6. 旧 `dates` 数据经迁移后与新时段数量及占用状态一致。
