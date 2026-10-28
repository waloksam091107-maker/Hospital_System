package com.ustb.hospital.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.ustb.hospital.entity.Dates;
import com.ustb.hospital.entity.ScheduleSlot;
import com.ustb.hospital.mapper.DatesMapper;
import com.ustb.hospital.mapper.ScheduleSlotsMapper;
import com.ustb.hospital.utils.AppointmentOperationResult;
import com.ustb.hospital.utils.AppointmentSlot;
import com.ustb.hospital.utils.MybatisUtil;
import org.apache.ibatis.session.SqlSession;

import java.util.List;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

/** Schedule changes preserve appointment references and share slot locks with patient booking. */
public class DatesServiceImpl {
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT);
    private final Supplier<SqlSession> transactionSessions;
    private final AuditService auditService;
    public DatesServiceImpl() { this(MybatisUtil::getTransactionSqlSession, new AuditService()); }
    public DatesServiceImpl(Supplier<SqlSession> sessions, AuditService auditService) { this.transactionSessions = sessions; this.auditService = auditService; }
    public boolean isSlotAvailable(int doctorId, String date, String requestedSlot) {
        String period = AppointmentSlot.fromRequestValue(requestedSlot);
        if (period == null || date == null) return false;
        try {
            for (java.util.Map<String,Object> row : new ClinicWorkflowService(transactionSessions, auditService, java.time.Clock.system(ClinicTime.ZONE)).slots(doctorId, date))
                if (period.equals(row.get("slotPeriod")) && ((Number)row.get("remaining")).intValue() > 0) return true;
        } catch (IllegalArgumentException ignored) { }
        return false;
    }

    public PageInfo<Dates> queryByDoctorId(int doctorId, int pageNum, int pageSize) {
        SqlSession sqlSession = MybatisUtil.getSqlSession();
        try {
            PageHelper.startPage(pageNum > 0 ? pageNum : 1, pageSize);
            List<Dates> list = sqlSession.getMapper(DatesMapper.class).selectByDoctorId(doctorId);
            return new PageInfo<Dates>(list);
        } finally { sqlSession.close(); }
    }

    /** Legacy service name retained for existing callers. */
    public PageInfo<Dates> queryByPatientId(int doctorId, int pageNum, int pageSize) {
        return queryByDoctorId(doctorId, pageNum, pageSize);
    }

    public AppointmentOperationResult addDate(int doctorId, String doctorName, String date, String shang, String xia, String wan) {
        return addDate(doctorId, doctorName, date, shang, xia, wan, synthetic(doctorId, "/schedules/create"));
    }
    public AppointmentOperationResult addDate(int doctorId, String doctorName, String date, String shang, String xia, String wan, AuditContext audit) {
        if (!isValidDate(date)) return AppointmentOperationResult.failure("INVALID_DATE", "请选择有效日期，格式为 YYYY-MM-DD。");
        if (isPast(date)) return AppointmentOperationResult.failure("PAST_DATE", "不能新建过去日期的排班。");
        SqlSession sqlSession = transactionSessions.get();
        try {
            sqlSession.getConnection().setTransactionIsolation(java.sql.Connection.TRANSACTION_READ_COMMITTED);
            if (!ClinicWorkflowStore.lockDoctor(sqlSession, doctorId)) return rollback(sqlSession, "NOT_FOUND", "医生不存在。");
            DatesMapper mapper = sqlSession.getMapper(DatesMapper.class);
            ScheduleSlotsMapper slotsMapper = sqlSession.getMapper(ScheduleSlotsMapper.class);
            if (mapper.findByDoctorAndDate(doctorId, date) != null) return rollback(sqlSession, "DUPLICATE_DATE", "该日期已经有排班。");
            Dates schedule = new Dates();
            schedule.setDoctorId(doctorId);
            schedule.setDoctorName(doctorName);
            schedule.setDate(date);
            schedule.setShang(normalizeAvailability(shang));
            schedule.setXia(normalizeAvailability(xia));
            schedule.setWan(normalizeAvailability(wan));
            if (mapper.insertDate(schedule) != 1 || schedule.getDateId() <= 0) return rollback(sqlSession, "SCHEDULE_CREATE_FAILED", "创建排班失败。");
            insertSlots(slotsMapper, doctorId, date, schedule);
            auditService.writeSuccess(sqlSession, audit, "SCHEDULES_CREATE", "SCHEDULE", Integer.toString(schedule.getDateId()));
            sqlSession.commit();
            return AppointmentOperationResult.success("排班已创建。");
        } catch (Exception exception) {
            return rollback(sqlSession, "SCHEDULE_CREATE_FAILED", "创建排班失败。");
        } finally { sqlSession.close(); }
    }

    public AppointmentOperationResult editForDoctor(int doctorId, int dateId, String date, String shang, String xia, String wan) {
        return editForDoctor(doctorId, dateId, date, shang, xia, wan, synthetic(doctorId, "/schedules/update"));
    }
    public AppointmentOperationResult editForDoctor(int doctorId, int dateId, String date, String shang, String xia, String wan, AuditContext audit) {
        SqlSession sqlSession = transactionSessions.get();
        try {
            sqlSession.getConnection().setTransactionIsolation(java.sql.Connection.TRANSACTION_READ_COMMITTED);
            if (!ClinicWorkflowStore.lockDoctor(sqlSession, doctorId)) return rollback(sqlSession, "NOT_FOUND", "医生不存在。");
            DatesMapper datesMapper = sqlSession.getMapper(DatesMapper.class);
            ScheduleSlotsMapper slotsMapper = sqlSession.getMapper(ScheduleSlotsMapper.class);
            Dates schedule = datesMapper.selectByIdForUpdate(dateId);
            if (schedule == null) return rollback(sqlSession, "NOT_FOUND", "排班不存在。");
            if (schedule.getDoctorId() != doctorId) return rollback(sqlSession, "FORBIDDEN", "不能修改其他医生的排班。");
            String previousDate = schedule.getDate();
            if (!isValidDate(date) || !isValidDate(previousDate)) return rollback(sqlSession, "INVALID_DATE", "请选择有效日期，格式为 YYYY-MM-DD。");
            if (isPast(date) || isPast(previousDate)) return rollback(sqlSession, "PAST_DATE", "不能修改过去日期的排班，也不能移动到过去日期。");
            // Lock before reading history: booking commits its appointment before releasing this lock.
            List<ScheduleSlot> slots = slotsMapper.selectForUpdate(doctorId, previousDate);
            if (!validSlots(slots)) return rollback(sqlSession, "SCHEDULE_MISMATCH", "排班时段异常，未更新。");
            if (!previousDate.equals(date) && slotsMapper.countScheduleHistory(doctorId, previousDate) > 0)
                return rollback(sqlSession, "HISTORY_DATE_LOCKED", "有预约历史的排班不能移动日期，请在原日期调整未占用时段。");
            Dates sameDay = datesMapper.findByDoctorAndDate(doctorId, date);
            if (sameDay != null && sameDay.getDateId() != dateId) return rollback(sqlSession, "DUPLICATE_DATE", "该日期已经有排班。");
            for (ScheduleSlot slot : slots) {
                String requested = availability(slot.getSlotPeriod(), shang, xia, wan);
                String previous = availability(slot.getSlotPeriod(), schedule.getShang(), schedule.getXia(), schedule.getWan());
                boolean occupied = slotsMapper.countProtectedAppointments(slot) > 0 || "BOOKED".equals(slot.getSlotStatus());
                if (occupied) {
                    if (!previousDate.equals(date) || !previous.equals(requested))
                        return rollback(sqlSession, "SCHEDULE_IN_USE", "待接诊、接诊中或已完成预约所占时段不能修改；可调整其他时段。");
                } else if (slotsMapper.updateUnoccupiedSlot(slot, date, "1".equals(requested) ? "AVAILABLE" : "CLOSED") != 1) {
                    return rollback(sqlSession, "SCHEDULE_IN_USE", "排班时段状态已变化，请刷新后重试。");
                }
            }
            schedule.setDate(date);
            schedule.setShang(normalizeAvailability(shang));
            schedule.setXia(normalizeAvailability(xia));
            schedule.setWan(normalizeAvailability(wan));
            if (datesMapper.updateDate(schedule) != 1) return rollback(sqlSession, "UPDATE_FAILED", "排班未更新。");
            auditService.writeSuccess(sqlSession, audit, "SCHEDULES_UPDATE", "SCHEDULE", Integer.toString(dateId));
            sqlSession.commit();
            return AppointmentOperationResult.success("排班已更新。");
        } catch (Exception exception) {
            return rollback(sqlSession, "SCHEDULE_UPDATE_FAILED", "更新排班失败。");
        } finally { sqlSession.close(); }
    }

    public AppointmentOperationResult deleteForDoctor(int doctorId, int dateId) {
        return deleteForDoctor(doctorId, dateId, synthetic(doctorId, "/schedules/delete"));
    }
    public AppointmentOperationResult deleteForDoctor(int doctorId, int dateId, AuditContext audit) {
        SqlSession sqlSession = transactionSessions.get();
        try {
            sqlSession.getConnection().setTransactionIsolation(java.sql.Connection.TRANSACTION_READ_COMMITTED);
            if (!ClinicWorkflowStore.lockDoctor(sqlSession, doctorId)) return rollback(sqlSession, "NOT_FOUND", "医生不存在。");
            DatesMapper datesMapper = sqlSession.getMapper(DatesMapper.class);
            ScheduleSlotsMapper slotsMapper = sqlSession.getMapper(ScheduleSlotsMapper.class);
            Dates schedule = datesMapper.selectByIdForUpdate(dateId);
            if (schedule == null) return rollback(sqlSession, "NOT_FOUND", "排班不存在。");
            if (schedule.getDoctorId() != doctorId) return rollback(sqlSession, "FORBIDDEN", "不能删除其他医生的排班。");
            if (!isValidDate(schedule.getDate())) return rollback(sqlSession, "INVALID_DATE", "排班日期无效，未撤销。");
            if (isPast(schedule.getDate())) return rollback(sqlSession, "PAST_DATE", "不能撤销过去日期的排班。");
            List<ScheduleSlot> slots = slotsMapper.selectForUpdate(doctorId, schedule.getDate());
            if (!validSlots(slots)) return rollback(sqlSession, "SCHEDULE_MISMATCH", "排班时段异常，未撤销。");
            if (slotsMapper.countActiveAppointments(doctorId, schedule.getDate()) > 0)
                return rollback(sqlSession, "SCHEDULE_IN_USE", "仍有待接诊或接诊中预约，请先处理有效预约后再撤班。");
            boolean history = slotsMapper.countScheduleHistory(doctorId, schedule.getDate()) > 0;
            if (history) {
                boolean changed = !"0".equals(schedule.getShang()) || !"0".equals(schedule.getXia()) || !"0".equals(schedule.getWan());
                for (ScheduleSlot slot : slots) {
                    if (slotsMapper.countProtectedAppointments(slot) > 0) {
                        // Completed/no-show quota is retained, but withdrawing the day must
                        // close unused capacity too. Never delete or renumber historical slots.
                        changed |= !"CLOSED".equals(slot.getSlotStatus());
                        ClinicWorkflowStore.update(sqlSession, "UPDATE schedule_slots SET slot_status='CLOSED' WHERE schedule_slot_id=?", slot.getScheduleSlotId());
                        continue;
                    }
                    if ("BOOKED".equals(slot.getSlotStatus())) return rollback(sqlSession, "SCHEDULE_MISMATCH", "时段占用状态异常，请核对后再撤班。");
                    changed |= "AVAILABLE".equals(slot.getSlotStatus());
                    if (slotsMapper.updateUnoccupiedSlot(slot, schedule.getDate(), "CLOSED") != 1)
                        return rollback(sqlSession, "SCHEDULE_IN_USE", "排班时段状态已变化，请刷新后重试。");
                }
                if (!changed) return rollback(sqlSession, "ALREADY_CLOSED", "排班已关闭，无可撤销的开放号源；历史记录保留。");
                schedule.setShang("0"); schedule.setXia("0"); schedule.setWan("0");
                if (datesMapper.updateDate(schedule) != 1) return rollback(sqlSession, "UPDATE_FAILED", "排班未撤销。");
            } else {
                for (ScheduleSlot slot : slots) if ("BOOKED".equals(slot.getSlotStatus()))
                    return rollback(sqlSession, "SCHEDULE_IN_USE", "排班时段已被占用，请核对预约后再撤班。");
                if (slotsMapper.deleteByDoctorAndDate(doctorId, schedule.getDate()) != 3)
                    return rollback(sqlSession, "SCHEDULE_MISMATCH", "排班时段异常，未删除。");
                if (datesMapper.deleteDateById(dateId) != 1) return rollback(sqlSession, "DELETE_FAILED", "排班未删除。");
            }
            auditService.writeSuccess(sqlSession, audit, history ? "SCHEDULES_WITHDRAW" : "SCHEDULES_DELETE", "SCHEDULE", Integer.toString(dateId));
            sqlSession.commit();
            return AppointmentOperationResult.success(history ? "排班已撤销，历史记录保留。" : "排班已删除（无预约历史）。");
        } catch (Exception exception) {
            return rollback(sqlSession, "SCHEDULE_DELETE_FAILED", "删除排班失败。");
        } finally { sqlSession.close(); }
    }

    private static String normalizeAvailability(String value) { return "1".equals(value) ? "1" : "0"; }
    private static AuditContext synthetic(int id, String path) { return new AuditContext("DOCTOR", Integer.valueOf(id), "POST", path, null); }
    private static void insertSlots(ScheduleSlotsMapper mapper, int doctorId, String date, Dates schedule) {
        mapper.insertSlot(doctorId, date, AppointmentSlot.AM, "1".equals(schedule.getShang()) ? "AVAILABLE" : "CLOSED");
        mapper.insertSlot(doctorId, date, AppointmentSlot.PM, "1".equals(schedule.getXia()) ? "AVAILABLE" : "CLOSED");
        mapper.insertSlot(doctorId, date, AppointmentSlot.EVENING, "1".equals(schedule.getWan()) ? "AVAILABLE" : "CLOSED");
    }
    private static String availability(String period, String am, String pm, String evening) {
        return normalizeAvailability(AppointmentSlot.AM.equals(period) ? am : AppointmentSlot.PM.equals(period) ? pm : evening);
    }
    private static boolean validSlots(List<ScheduleSlot> slots) {
        if (slots.size() != 3) return false;
        Set<String> periods = new HashSet<String>();
        for (ScheduleSlot slot : slots) {
            if (!(AppointmentSlot.AM.equals(slot.getSlotPeriod()) || AppointmentSlot.PM.equals(slot.getSlotPeriod()) || AppointmentSlot.EVENING.equals(slot.getSlotPeriod()))
                    || !("AVAILABLE".equals(slot.getSlotStatus()) || "CLOSED".equals(slot.getSlotStatus()) || "BOOKED".equals(slot.getSlotStatus()))) return false;
            periods.add(slot.getSlotPeriod());
        }
        return periods.size() == 3;
    }
    private static boolean isValidDate(String date) {
        if (date == null || !date.matches("\\d{4}-\\d{2}-\\d{2}")) return false;
        try { return LocalDate.parse(date, ISO_DATE).getYear() >= 1; }
        catch (DateTimeParseException invalid) { return false; }
    }
    private static boolean isPast(String date) { return LocalDate.parse(date, ISO_DATE).isBefore(LocalDate.now(ClinicTime.ZONE)); }
    private static AppointmentOperationResult rollback(SqlSession session, String code, String message) {
        session.rollback();
        return AppointmentOperationResult.failure(code, message);
    }
}
