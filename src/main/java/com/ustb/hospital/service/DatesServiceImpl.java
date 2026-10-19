package com.ustb.hospital.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.ustb.hospital.entity.Dates;
import com.ustb.hospital.mapper.AppointmentsMapper;
import com.ustb.hospital.mapper.DatesMapper;
import com.ustb.hospital.mapper.ScheduleSlotsMapper;
import com.ustb.hospital.utils.AppointmentOperationResult;
import com.ustb.hospital.utils.AppointmentSlot;
import com.ustb.hospital.utils.MybatisUtil;
import org.apache.ibatis.session.SqlSession;

import java.util.List;
import java.util.regex.Pattern;
import java.util.function.Supplier;

/** Doctor-owned schedule operations. A schedule with booked appointments cannot be changed or deleted. */
public class DatesServiceImpl {
    private static final Pattern ISO_DATE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
    private final Supplier<SqlSession> transactionSessions;
    private final AuditService auditService;
    public DatesServiceImpl() { this(MybatisUtil::getTransactionSqlSession, new AuditService()); }
    public DatesServiceImpl(Supplier<SqlSession> sessions, AuditService auditService) { this.transactionSessions = sessions; this.auditService = auditService; }
    public boolean isSlotAvailable(int doctorId, String date, String requestedSlot) {
        String slot = AppointmentSlot.fromRequestValue(requestedSlot);
        if (slot == null || date == null) return false;
        SqlSession sqlSession = MybatisUtil.getSqlSession();
        try {
            com.ustb.hospital.entity.ScheduleSlot scheduleSlot = sqlSession.getMapper(ScheduleSlotsMapper.class)
                    .selectByDoctorDatePeriod(doctorId, date, slot);
            return scheduleSlot != null && "AVAILABLE".equals(scheduleSlot.getSlotStatus());
        } finally { sqlSession.close(); }
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
        if (!isValidDate(date)) return AppointmentOperationResult.failure("INVALID_DATE", "日期格式必须为 YYYY-MM-DD。");
        SqlSession sqlSession = transactionSessions.get();
        try {
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
            DatesMapper datesMapper = sqlSession.getMapper(DatesMapper.class);
            ScheduleSlotsMapper slotsMapper = sqlSession.getMapper(ScheduleSlotsMapper.class);
            AppointmentsMapper appointmentsMapper = sqlSession.getMapper(AppointmentsMapper.class);
            Dates schedule = datesMapper.selectById(dateId);
            if (schedule == null) return rollback(sqlSession, "NOT_FOUND", "排班不存在。");
            if (schedule.getDoctorId() != doctorId) return rollback(sqlSession, "FORBIDDEN", "不能修改其他医生的排班。");
            String previousDate = schedule.getDate();
            if (appointmentsMapper.countByDoctorAndDate(doctorId, previousDate) > 0) {
                return rollback(sqlSession, "SCHEDULE_IN_USE", "已有预约记录的排班不能修改。");
            }
            if (!isValidDate(date)) return rollback(sqlSession, "INVALID_DATE", "日期格式必须为 YYYY-MM-DD。");
            Dates sameDay = datesMapper.findByDoctorAndDate(doctorId, date);
            if (sameDay != null && sameDay.getDateId() != dateId) return rollback(sqlSession, "DUPLICATE_DATE", "该日期已经有排班。");
            schedule.setDate(date);
            schedule.setShang(normalizeAvailability(shang));
            schedule.setXia(normalizeAvailability(xia));
            schedule.setWan(normalizeAvailability(wan));
            if (datesMapper.updateDate(schedule) != 1) return rollback(sqlSession, "UPDATE_FAILED", "排班未更新。");
            if (slotsMapper.deleteByDoctorAndDate(doctorId, previousDate) != 3) {
                return rollback(sqlSession, "SCHEDULE_MISMATCH", "排班时段异常，未更新。");
            }
            insertSlots(slotsMapper, doctorId, date, schedule);
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
            DatesMapper datesMapper = sqlSession.getMapper(DatesMapper.class);
            ScheduleSlotsMapper slotsMapper = sqlSession.getMapper(ScheduleSlotsMapper.class);
            AppointmentsMapper appointmentsMapper = sqlSession.getMapper(AppointmentsMapper.class);
            Dates schedule = datesMapper.selectById(dateId);
            if (schedule == null) return rollback(sqlSession, "NOT_FOUND", "排班不存在。");
            if (schedule.getDoctorId() != doctorId) return rollback(sqlSession, "FORBIDDEN", "不能删除其他医生的排班。");
            if (appointmentsMapper.countByDoctorAndDate(doctorId, schedule.getDate()) > 0) {
                return rollback(sqlSession, "SCHEDULE_IN_USE", "已有预约记录的排班不能删除。");
            }
            if (slotsMapper.deleteByDoctorAndDate(doctorId, schedule.getDate()) != 3) {
                return rollback(sqlSession, "SCHEDULE_MISMATCH", "排班时段异常，未删除。");
            }
            if (datesMapper.deleteDateById(dateId) != 1) return rollback(sqlSession, "DELETE_FAILED", "排班未删除。");
            auditService.writeSuccess(sqlSession, audit, "SCHEDULES_DELETE", "SCHEDULE", Integer.toString(dateId));
            sqlSession.commit();
            return AppointmentOperationResult.success("排班已删除。");
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
    private static boolean isValidDate(String date) { return date != null && ISO_DATE.matcher(date).matches(); }
    private static AppointmentOperationResult rollback(SqlSession session, String code, String message) {
        session.rollback();
        return AppointmentOperationResult.failure(code, message);
    }
}
