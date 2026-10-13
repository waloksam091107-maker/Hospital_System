package com.ustb.hospital.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.ustb.hospital.entity.Appointments;
import com.ustb.hospital.entity.Doctors;
import com.ustb.hospital.entity.Patients;
import com.ustb.hospital.mapper.AppointmentsMapper;
import com.ustb.hospital.mapper.DatesMapper;
import com.ustb.hospital.mapper.DoctorsMapper;
import com.ustb.hospital.mapper.PatientsMapper;
import com.ustb.hospital.utils.AppointmentOperationResult;
import com.ustb.hospital.utils.AppointmentSlot;
import com.ustb.hospital.utils.MybatisUtil;
import org.apache.ibatis.session.SqlSession;

import java.util.List;
import java.util.regex.Pattern;

/** Appointment operations keep schedule, balance and appointment rows in one transaction. */
public class AppointmentsServiceImpl {
    public static final String STATUS_BOOKED = "booked";
    public static final String STATUS_CANCELLED = "cancelled";
    public static final String STATUS_COMPLETED = "completed";
    private static final Pattern ISO_DATE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

    /** Retained for the existing order-summary screen; it does not participate in patient booking. */
    public List<Appointments> queryByDate(String date) {
        SqlSession sqlSession = MybatisUtil.getSqlSession();
        try {
            return sqlSession.getMapper(AppointmentsMapper.class).selectByDate(date);
        } finally { close(sqlSession); }
    }

    public AppointmentOperationResult bookAppointment(int patientId, int doctorId, String appointmentDate, String requestedSlot) {
        String slot = AppointmentSlot.fromRequestValue(requestedSlot);
        if (!isValidDate(appointmentDate) || slot == null) {
            return AppointmentOperationResult.failure("INVALID_REQUEST", "日期或时段无效。");
        }
        SqlSession sqlSession = null;
        try {
            sqlSession = MybatisUtil.getTransactionSqlSession();
            DoctorsMapper doctorsMapper = sqlSession.getMapper(DoctorsMapper.class);
            PatientsMapper patientsMapper = sqlSession.getMapper(PatientsMapper.class);
            DatesMapper datesMapper = sqlSession.getMapper(DatesMapper.class);
            AppointmentsMapper appointmentsMapper = sqlSession.getMapper(AppointmentsMapper.class);
            Doctors doctor = doctorsMapper.findById(doctorId);
            Patients patient = patientsMapper.selectById(patientId);
            if (doctor == null || patient == null) {
                return rollback(sqlSession, "NOT_FOUND", "患者或医生不存在。");
            }
            double registrationFee = parseRegistrationFee(doctor.getRegistrationFee());
            if (registrationFee < 0) {
                return rollback(sqlSession, "INVALID_FEE", "医生挂号费配置无效。");
            }
            if (datesMapper.reserveSlot(doctorId, appointmentDate, slot) != 1) {
                return rollback(sqlSession, "SLOT_UNAVAILABLE", "所选时段不存在或已被占用。");
            }
            if (patientsMapper.deductBalanceIfEnough(patientId, registrationFee) != 1) {
                return rollback(sqlSession, "INSUFFICIENT_BALANCE", "余额不足，未创建预约。");
            }
            appointmentsMapper.insert(patientId, doctorId, appointmentDate, slot, STATUS_BOOKED);
            sqlSession.commit();
            return AppointmentOperationResult.success("预约成功。");
        } catch (Exception exception) {
            return rollback(sqlSession, "BOOKING_FAILED", "预约未完成，请稍后重试。");
        } finally { close(sqlSession); }
    }

    public AppointmentOperationResult cancelAppointment(int patientId, int appointmentId) {
        SqlSession sqlSession = null;
        try {
            sqlSession = MybatisUtil.getTransactionSqlSession();
            AppointmentsMapper appointmentsMapper = sqlSession.getMapper(AppointmentsMapper.class);
            DatesMapper datesMapper = sqlSession.getMapper(DatesMapper.class);
            Appointments appointment = appointmentsMapper.selectById(appointmentId);
            if (appointment == null) return rollback(sqlSession, "NOT_FOUND", "预约不存在。");
            if (appointment.getPatientId() != patientId) return rollback(sqlSession, "FORBIDDEN", "不能取消其他患者的预约。");
            String slot = AppointmentSlot.fromRequestValue(appointment.getAppointmentTime());
            if (slot == null) return rollback(sqlSession, "INVALID_SLOT", "原预约时段无法恢复，请联系工作人员处理。");
            if (appointmentsMapper.updateStatusIfCurrent(appointmentId, appointment.getDoctorId(), STATUS_BOOKED, STATUS_CANCELLED) != 1) {
                return rollback(sqlSession, "INVALID_STATE", "只有已预约的记录可以取消。");
            }
            if (datesMapper.releaseSlot(appointment.getDoctorId(), appointment.getAppointmentDate(), slot) != 1) {
                return rollback(sqlSession, "SCHEDULE_MISMATCH", "排班状态异常，未取消预约。");
            }
            sqlSession.commit();
            return AppointmentOperationResult.success("预约已取消。当前系统取消预约不自动退款。");
        } catch (Exception exception) {
            return rollback(sqlSession, "CANCEL_FAILED", "取消预约失败，请稍后重试。");
        } finally { close(sqlSession); }
    }

    public AppointmentOperationResult rescheduleAppointment(int patientId, int appointmentId, String newDate, String requestedSlot) {
        String newSlot = AppointmentSlot.fromRequestValue(requestedSlot);
        if (!isValidDate(newDate) || newSlot == null) return AppointmentOperationResult.failure("INVALID_REQUEST", "新的日期或时段无效。");
        SqlSession sqlSession = null;
        try {
            sqlSession = MybatisUtil.getTransactionSqlSession();
            AppointmentsMapper appointmentsMapper = sqlSession.getMapper(AppointmentsMapper.class);
            DatesMapper datesMapper = sqlSession.getMapper(DatesMapper.class);
            Appointments appointment = appointmentsMapper.selectById(appointmentId);
            if (appointment == null) return rollback(sqlSession, "NOT_FOUND", "预约不存在。");
            if (appointment.getPatientId() != patientId) return rollback(sqlSession, "FORBIDDEN", "不能修改其他患者的预约。");
            if (!STATUS_BOOKED.equals(appointment.getStatus())) return rollback(sqlSession, "INVALID_STATE", "只有已预约的记录可以改期。");
            String oldSlot = AppointmentSlot.fromRequestValue(appointment.getAppointmentTime());
            if (oldSlot == null) return rollback(sqlSession, "INVALID_SLOT", "原预约时段无法恢复，请联系工作人员处理。");
            if (newDate.equals(appointment.getAppointmentDate()) && newSlot.equals(oldSlot)) return rollback(sqlSession, "NO_CHANGE", "新时段与原预约相同。");
            if (datesMapper.reserveSlot(appointment.getDoctorId(), newDate, newSlot) != 1) return rollback(sqlSession, "SLOT_UNAVAILABLE", "新的时段不存在或已被占用。");
            if (appointmentsMapper.updateAppointmentSlot(appointmentId, newDate, newSlot, STATUS_BOOKED) != 1) return rollback(sqlSession, "INVALID_STATE", "预约状态已变化，未改期。");
            if (datesMapper.releaseSlot(appointment.getDoctorId(), appointment.getAppointmentDate(), oldSlot) != 1) return rollback(sqlSession, "SCHEDULE_MISMATCH", "原排班状态异常，未改期。");
            sqlSession.commit();
            return AppointmentOperationResult.success("预约已改期。");
        } catch (Exception exception) {
            return rollback(sqlSession, "RESCHEDULE_FAILED", "改期失败，原预约保持不变。");
        } finally { close(sqlSession); }
    }

    public AppointmentOperationResult completeAppointment(int doctorId, int appointmentId) {
        SqlSession sqlSession = null;
        try {
            sqlSession = MybatisUtil.getTransactionSqlSession();
            AppointmentsMapper mapper = sqlSession.getMapper(AppointmentsMapper.class);
            if (mapper.updateStatusIfCurrent(appointmentId, doctorId, STATUS_BOOKED, STATUS_COMPLETED) != 1) {
                return rollback(sqlSession, "INVALID_STATE", "预约不存在、不属于当前医生，或当前不能完成接诊。");
            }
            sqlSession.commit();
            return AppointmentOperationResult.success("接诊已完成。");
        } catch (Exception exception) {
            return rollback(sqlSession, "COMPLETE_FAILED", "完成接诊失败，请稍后重试。");
        } finally { close(sqlSession); }
    }

    public PageInfo<Appointments> queryByDoctorId(int doctorId, int pageNum, int pageSize) {
        SqlSession sqlSession = MybatisUtil.getSqlSession();
        try {
            PageHelper.startPage(safePage(pageNum), pageSize);
            List<Appointments> list = sqlSession.getMapper(AppointmentsMapper.class).selectByDoctorId(doctorId);
            return new PageInfo<Appointments>(list);
        } finally { close(sqlSession); }
    }

    public PageInfo<Appointments> queryByPatientId(int patientId, int pageNum, int pageSize) {
        SqlSession sqlSession = MybatisUtil.getSqlSession();
        try {
            PageHelper.startPage(safePage(pageNum), pageSize);
            List<Appointments> list = sqlSession.getMapper(AppointmentsMapper.class).selectByPatientIdWithDoctorDetails(patientId);
            return new PageInfo<Appointments>(list);
        } finally { close(sqlSession); }
    }

    public PageInfo<Appointments> queryAll(int pageNum, int pageSize) {
        SqlSession sqlSession = MybatisUtil.getSqlSession();
        try {
            PageHelper.startPage(safePage(pageNum), pageSize);
            return new PageInfo<Appointments>(sqlSession.getMapper(AppointmentsMapper.class).selectAll());
        } finally { close(sqlSession); }
    }
    public PageInfo<Appointments> queryAllWithDoctorDetails(int pageNum, int pageSize) {
        SqlSession sqlSession = MybatisUtil.getSqlSession();
        try {
            PageHelper.startPage(safePage(pageNum), pageSize);
            return new PageInfo<Appointments>(sqlSession.getMapper(AppointmentsMapper.class).selectAllWithDoctorDetails());
        } finally { close(sqlSession); }
    }

    private static boolean isValidDate(String date) { return date != null && ISO_DATE.matcher(date).matches(); }
    private static int safePage(int pageNum) { return pageNum > 0 ? pageNum : 1; }
    private static double parseRegistrationFee(String value) {
        if (value == null) return -1;
        try {
            double fee = Double.parseDouble(value.trim());
            return fee >= 0 && !Double.isInfinite(fee) && !Double.isNaN(fee) ? fee : -1;
        } catch (NumberFormatException exception) { return -1; }
    }
    private static AppointmentOperationResult rollback(SqlSession session, String code, String message) {
        if (session != null) session.rollback();
        return AppointmentOperationResult.failure(code, message);
    }
    private static void close(SqlSession session) { if (session != null) session.close(); }
}
