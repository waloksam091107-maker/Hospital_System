package com.ustb.hospital.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.ustb.hospital.entity.Dates;
import com.ustb.hospital.mapper.AppointmentsMapper;
import com.ustb.hospital.mapper.DatesMapper;
import com.ustb.hospital.utils.AppointmentOperationResult;
import com.ustb.hospital.utils.AppointmentSlot;
import com.ustb.hospital.utils.MybatisUtil;
import org.apache.ibatis.session.SqlSession;

import java.util.List;
import java.util.regex.Pattern;

/** Doctor-owned schedule operations. A schedule with booked appointments cannot be changed or deleted. */
public class DatesServiceImpl {
    private static final Pattern ISO_DATE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
    public boolean isSlotAvailable(int doctorId, String date, String requestedSlot) {
        String slot = AppointmentSlot.fromRequestValue(requestedSlot);
        if (slot == null || date == null) return false;
        SqlSession sqlSession = MybatisUtil.getSqlSession();
        try {
            Dates dates = sqlSession.getMapper(DatesMapper.class).findByDoctorAndDate(doctorId, date);
            if (dates == null) return false;
            if (AppointmentSlot.AM.equals(slot)) return "1".equals(dates.getShang());
            if (AppointmentSlot.PM.equals(slot)) return "1".equals(dates.getXia());
            return "1".equals(dates.getWan());
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
        if (!isValidDate(date)) return AppointmentOperationResult.failure("INVALID_DATE", "日期格式必须为 YYYY-MM-DD。");
        SqlSession sqlSession = MybatisUtil.getTransactionSqlSession();
        try {
            DatesMapper mapper = sqlSession.getMapper(DatesMapper.class);
            if (mapper.findByDoctorAndDate(doctorId, date) != null) return rollback(sqlSession, "DUPLICATE_DATE", "该日期已经有排班。");
            Dates schedule = new Dates();
            schedule.setDoctorId(doctorId);
            schedule.setDoctorName(doctorName);
            schedule.setDate(date);
            schedule.setShang(normalizeAvailability(shang));
            schedule.setXia(normalizeAvailability(xia));
            schedule.setWan(normalizeAvailability(wan));
            mapper.insertDate(schedule);
            sqlSession.commit();
            return AppointmentOperationResult.success("排班已创建。");
        } catch (Exception exception) {
            return rollback(sqlSession, "SCHEDULE_CREATE_FAILED", "创建排班失败。");
        } finally { sqlSession.close(); }
    }

    public AppointmentOperationResult editForDoctor(int doctorId, int dateId, String date, String shang, String xia, String wan) {
        SqlSession sqlSession = MybatisUtil.getTransactionSqlSession();
        try {
            DatesMapper datesMapper = sqlSession.getMapper(DatesMapper.class);
            AppointmentsMapper appointmentsMapper = sqlSession.getMapper(AppointmentsMapper.class);
            Dates schedule = datesMapper.selectById(dateId);
            if (schedule == null) return rollback(sqlSession, "NOT_FOUND", "排班不存在。");
            if (schedule.getDoctorId() != doctorId) return rollback(sqlSession, "FORBIDDEN", "不能修改其他医生的排班。");
            if (appointmentsMapper.countReservedByDoctorAndDate(doctorId, schedule.getDate()) > 0) {
                return rollback(sqlSession, "SCHEDULE_IN_USE", "已有待接诊或已完成预约的排班不能修改。");
            }
            if (!isValidDate(date)) return rollback(sqlSession, "INVALID_DATE", "日期格式必须为 YYYY-MM-DD。");
            Dates sameDay = datesMapper.findByDoctorAndDate(doctorId, date);
            if (sameDay != null && sameDay.getDateId() != dateId) return rollback(sqlSession, "DUPLICATE_DATE", "该日期已经有排班。");
            schedule.setDate(date);
            schedule.setShang(normalizeAvailability(shang));
            schedule.setXia(normalizeAvailability(xia));
            schedule.setWan(normalizeAvailability(wan));
            if (datesMapper.updateDate(schedule) != 1) return rollback(sqlSession, "UPDATE_FAILED", "排班未更新。");
            sqlSession.commit();
            return AppointmentOperationResult.success("排班已更新。");
        } catch (Exception exception) {
            return rollback(sqlSession, "SCHEDULE_UPDATE_FAILED", "更新排班失败。");
        } finally { sqlSession.close(); }
    }

    public AppointmentOperationResult deleteForDoctor(int doctorId, int dateId) {
        SqlSession sqlSession = MybatisUtil.getTransactionSqlSession();
        try {
            DatesMapper datesMapper = sqlSession.getMapper(DatesMapper.class);
            AppointmentsMapper appointmentsMapper = sqlSession.getMapper(AppointmentsMapper.class);
            Dates schedule = datesMapper.selectById(dateId);
            if (schedule == null) return rollback(sqlSession, "NOT_FOUND", "排班不存在。");
            if (schedule.getDoctorId() != doctorId) return rollback(sqlSession, "FORBIDDEN", "不能删除其他医生的排班。");
            if (appointmentsMapper.countReservedByDoctorAndDate(doctorId, schedule.getDate()) > 0) {
                return rollback(sqlSession, "SCHEDULE_IN_USE", "已有待接诊或已完成预约的排班不能删除。");
            }
            if (datesMapper.deleteDateById(dateId) != 1) return rollback(sqlSession, "DELETE_FAILED", "排班未删除。");
            sqlSession.commit();
            return AppointmentOperationResult.success("排班已删除。");
        } catch (Exception exception) {
            return rollback(sqlSession, "SCHEDULE_DELETE_FAILED", "删除排班失败。");
        } finally { sqlSession.close(); }
    }

    private static String normalizeAvailability(String value) { return "1".equals(value) ? "1" : "0"; }
    private static boolean isValidDate(String date) { return date != null && ISO_DATE.matcher(date).matches(); }
    private static AppointmentOperationResult rollback(SqlSession session, String code, String message) {
        session.rollback();
        return AppointmentOperationResult.failure(code, message);
    }
}
