package com.ustb.hospital.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.ustb.hospital.entity.Appointments;
import com.ustb.hospital.entity.BalanceTransaction;
import com.ustb.hospital.entity.Doctors;
import com.ustb.hospital.entity.Patients;
import com.ustb.hospital.entity.ScheduleSlot;
import com.ustb.hospital.mapper.AppointmentsMapper;
import com.ustb.hospital.mapper.BalanceTransactionsMapper;
import com.ustb.hospital.mapper.DoctorsMapper;
import com.ustb.hospital.mapper.PatientsMapper;
import com.ustb.hospital.mapper.ScheduleSlotsMapper;
import com.ustb.hospital.utils.AppointmentOperationResult;
import com.ustb.hospital.utils.AppointmentSlot;
import com.ustb.hospital.utils.MybatisUtil;
import org.apache.ibatis.session.SqlSession;

import java.util.List;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Pattern;
import java.util.function.Supplier;

/** Appointment operations keep schedule, balance and appointment rows in one transaction. */
public class AppointmentsServiceImpl {
    public static final String STATUS_BOOKED = AppointmentStatus.BOOKED;
    public static final String STATUS_RESCHEDULED = AppointmentStatus.RESCHEDULED;
    public static final String STATUS_CANCELLED = AppointmentStatus.CANCELLED;
    public static final String STATUS_IN_PROGRESS = AppointmentStatus.IN_PROGRESS;
    public static final String STATUS_COMPLETED = AppointmentStatus.COMPLETED;
    private static final Pattern ISO_DATE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
    private final Supplier<SqlSession> transactionSessions;
    private final AuditService auditService;
    public AppointmentsServiceImpl() { this(MybatisUtil::getTransactionSqlSession, new AuditService()); }
    public AppointmentsServiceImpl(Supplier<SqlSession> sessions, AuditService auditService) { this.transactionSessions = sessions; this.auditService = auditService; }

    /** Retained for the existing order-summary screen; it does not participate in patient booking. */
    public List<Appointments> queryByDate(String date) {
        SqlSession sqlSession = MybatisUtil.getSqlSession();
        try {
            return sqlSession.getMapper(AppointmentsMapper.class).selectByDate(date);
        } finally { close(sqlSession); }
    }

    public AppointmentOperationResult bookAppointment(int patientId, int doctorId, String appointmentDate, String requestedSlot) {
        return bookAppointment(patientId, doctorId, appointmentDate, requestedSlot, synthetic("PATIENT", patientId, "/appointments/book"));
    }
    public AppointmentOperationResult bookAppointment(int patientId, int doctorId, String appointmentDate, String requestedSlot, AuditContext audit) {
        String slot = AppointmentSlot.fromRequestValue(requestedSlot);
        if (!isValidDate(appointmentDate) || slot == null) {
            return AppointmentOperationResult.failure("INVALID_REQUEST", "日期或时段无效。");
        }
        SqlSession sqlSession = null;
        try {
            sqlSession = transactionSessions.get();
            DoctorsMapper doctorsMapper = sqlSession.getMapper(DoctorsMapper.class);
            PatientsMapper patientsMapper = sqlSession.getMapper(PatientsMapper.class);
            ScheduleSlotsMapper scheduleSlotsMapper = sqlSession.getMapper(ScheduleSlotsMapper.class);
            AppointmentsMapper appointmentsMapper = sqlSession.getMapper(AppointmentsMapper.class);
            Doctors doctor = doctorsMapper.findById(doctorId);
            Patients patient = patientsMapper.selectByIdForUpdate(patientId);
            if (doctor == null || patient == null) {
                return rollback(sqlSession, "NOT_FOUND", "患者或医生不存在。");
            }
            BigDecimal registrationFee = parseRegistrationFee(doctor.getRegistrationFee());
            if (registrationFee == null) {
                return rollback(sqlSession, "INVALID_FEE", "医生挂号费配置无效。");
            }
            if (scheduleSlotsMapper.reserveAvailableSlot(doctorId, appointmentDate, slot) != 1) {
                return rollback(sqlSession, "SLOT_UNAVAILABLE", "所选时段不存在或已被占用。");
            }
            ScheduleSlot scheduleSlot = scheduleSlotsMapper.selectByDoctorDatePeriod(doctorId, appointmentDate, slot);
            if (scheduleSlot == null) {
                return rollback(sqlSession, "SCHEDULE_MISMATCH", "排班时段异常，未创建预约。");
            }
            if (patientsMapper.deductBalanceIfEnough(patientId, registrationFee) != 1) {
                return rollback(sqlSession, "INSUFFICIENT_BALANCE", "余额不足，未创建预约。");
            }
            Appointments created = newAppointment(patientId, doctorId, appointmentDate, slot, scheduleSlot.getScheduleSlotId(), null);
            if (appointmentsMapper.insertWithSlotRecord(created) != 1 || created.getAppointmentId() <= 0)
                return rollback(sqlSession, "BOOKING_FAILED", "预约未完成，请稍后重试。");
            BalanceTransaction debit = new BalanceTransaction();
            debit.setPatientId(patientId); debit.setAppointmentId(Integer.valueOf(created.getAppointmentId()));
            debit.setTransactionType("APPOINTMENT_DEBIT"); debit.setAmount(registrationFee.negate());
            debit.setBalanceAfter(currentBalance(patient).subtract(registrationFee)); debit.setNote("预约挂号扣费");
            if (sqlSession.getMapper(BalanceTransactionsMapper.class).insert(debit) != 1)
                return rollback(sqlSession, "BOOKING_FAILED", "预约未完成，请稍后重试。");
            auditService.writeSuccess(sqlSession, audit, "APPOINTMENTS_BOOK", "APPOINTMENT", Integer.toString(created.getAppointmentId()));
            sqlSession.commit();
            return AppointmentOperationResult.success("预约成功。");
        } catch (Exception exception) {
            return rollback(sqlSession, "BOOKING_FAILED", "预约未完成，请稍后重试。");
        } finally { close(sqlSession); }
    }

    public AppointmentOperationResult cancelAppointment(int patientId, int appointmentId) {
        return cancelAppointment(patientId, appointmentId, synthetic("PATIENT", patientId, "/appointments/cancel"));
    }
    public AppointmentOperationResult cancelAppointment(int patientId, int appointmentId, AuditContext audit) {
        SqlSession sqlSession = null;
        try {
            sqlSession = transactionSessions.get();
            AppointmentsMapper appointmentsMapper = sqlSession.getMapper(AppointmentsMapper.class);
            ScheduleSlotsMapper scheduleSlotsMapper = sqlSession.getMapper(ScheduleSlotsMapper.class);
            Appointments appointment = appointmentsMapper.selectByIdForUpdate(appointmentId);
            if (appointment == null) return rollback(sqlSession, "NOT_FOUND", "预约不存在。");
            if (appointment.getPatientId() != patientId) return rollback(sqlSession, "FORBIDDEN", "不能取消其他患者的预约。");
            if (!AppointmentStatus.canCancel(appointment.getStatus())) {
                return rollback(sqlSession, "INVALID_STATE", AppointmentStatus.patientMutationFailureMessage(appointment.getStatus()));
            }
            if (appointment.getScheduleSlotId() == null) {
                return rollback(sqlSession, "SCHEDULE_MISMATCH", "原预约缺少排班时段，未取消预约。");
            }
            if (appointmentsMapper.updateStatusForPatientIfCurrent(appointmentId, patientId, STATUS_BOOKED, STATUS_CANCELLED) != 1) {
                return rollback(sqlSession, "INVALID_STATE", "预约状态已变化，未取消预约。");
            }
            if (scheduleSlotsMapper.releaseBookedSlot(appointment.getScheduleSlotId().longValue()) != 1) {
                return rollback(sqlSession, "SCHEDULE_MISMATCH", "排班状态异常，未取消预约。");
            }
            auditService.writeSuccess(sqlSession, audit, "APPOINTMENTS_CANCEL", "APPOINTMENT", Integer.toString(appointmentId));
            sqlSession.commit();
            return AppointmentOperationResult.success("预约已取消。当前系统取消预约不自动退款。");
        } catch (Exception exception) {
            return rollback(sqlSession, "CANCEL_FAILED", "取消预约失败，请稍后重试。");
        } finally { close(sqlSession); }
    }

    public AppointmentOperationResult rescheduleAppointment(int patientId, int appointmentId, String newDate, String requestedSlot) {
        return rescheduleAppointment(patientId, appointmentId, newDate, requestedSlot, synthetic("PATIENT", patientId, "/appointments/reschedule"));
    }
    public AppointmentOperationResult rescheduleAppointment(int patientId, int appointmentId, String newDate, String requestedSlot, AuditContext audit) {
        String newSlot = AppointmentSlot.fromRequestValue(requestedSlot);
        if (!isValidDate(newDate) || newSlot == null) return AppointmentOperationResult.failure("INVALID_REQUEST", "新的日期或时段无效。");
        SqlSession sqlSession = null;
        try {
            sqlSession = transactionSessions.get();
            AppointmentsMapper appointmentsMapper = sqlSession.getMapper(AppointmentsMapper.class);
            ScheduleSlotsMapper scheduleSlotsMapper = sqlSession.getMapper(ScheduleSlotsMapper.class);
            Appointments appointment = appointmentsMapper.selectByIdForUpdate(appointmentId);
            if (appointment == null) return rollback(sqlSession, "NOT_FOUND", "预约不存在。");
            if (appointment.getPatientId() != patientId) return rollback(sqlSession, "FORBIDDEN", "不能修改其他患者的预约。");
            if (!AppointmentStatus.canReschedule(appointment.getStatus())) return rollback(sqlSession, "INVALID_STATE", AppointmentStatus.patientMutationFailureMessage(appointment.getStatus()));
            String oldSlot = AppointmentSlot.fromRequestValue(appointment.getAppointmentTime());
            if (oldSlot == null) return rollback(sqlSession, "INVALID_SLOT", "原预约时段无法恢复，请联系工作人员处理。");
            if (newDate.equals(appointment.getAppointmentDate()) && newSlot.equals(oldSlot)) return rollback(sqlSession, "NO_CHANGE", "新时段与原预约相同。");
            if (appointment.getScheduleSlotId() == null) return rollback(sqlSession, "SCHEDULE_MISMATCH", "原预约缺少排班时段，未改期。");
            if (scheduleSlotsMapper.reserveAvailableSlot(appointment.getDoctorId(), newDate, newSlot) != 1) return rollback(sqlSession, "SLOT_UNAVAILABLE", "该医生在所选日期或时段没有排班，或该时段已被预约。");
            ScheduleSlot newScheduleSlot = scheduleSlotsMapper.selectByDoctorDatePeriod(appointment.getDoctorId(), newDate, newSlot);
            if (newScheduleSlot == null) return rollback(sqlSession, "SCHEDULE_MISMATCH", "新排班时段异常，未改期。");
            Appointments created = newAppointment(patientId, appointment.getDoctorId(), newDate, newSlot,
                    newScheduleSlot.getScheduleSlotId(), Integer.valueOf(appointmentId));
            if (appointmentsMapper.insertWithSlotRecord(created) != 1 || created.getAppointmentId() <= 0) {
                return rollback(sqlSession, "RESCHEDULE_FAILED", "新预约未创建，原预约保持不变。");
            }
            if (appointmentsMapper.updateStatusForPatientIfCurrent(appointmentId, patientId, STATUS_BOOKED, STATUS_RESCHEDULED) != 1) {
                return rollback(sqlSession, "INVALID_STATE", "预约状态已变化，未改期。");
            }
            if (scheduleSlotsMapper.releaseBookedSlot(appointment.getScheduleSlotId().longValue()) != 1) return rollback(sqlSession, "SCHEDULE_MISMATCH", "原排班状态异常，未改期。");
            auditService.writeSuccess(sqlSession, audit, "APPOINTMENTS_RESCHEDULE", "APPOINTMENT", Integer.toString(appointmentId));
            auditService.writeSuccess(sqlSession, audit, "APPOINTMENTS_RESCHEDULE_NEW", "APPOINTMENT", Integer.toString(created.getAppointmentId()));
            sqlSession.commit();
            return AppointmentOperationResult.success("预约已改期。");
        } catch (Exception exception) {
            return rollback(sqlSession, "RESCHEDULE_FAILED", "改期失败，原预约保持不变。");
        } finally { close(sqlSession); }
    }

    public AppointmentOperationResult completeAppointment(int doctorId, int appointmentId) {
        return AppointmentOperationResult.failure("MEDICAL_RECORD_REQUIRED", "请填写并提交就诊记录后完成接诊。");
    }

    public AppointmentOperationResult startAppointment(int doctorId, int appointmentId) {
        return startAppointment(doctorId, appointmentId, synthetic("DOCTOR", doctorId, "/appointments/start"));
    }
    public AppointmentOperationResult startAppointment(int doctorId, int appointmentId, AuditContext audit) {
        SqlSession sqlSession = null;
        try {
            sqlSession = transactionSessions.get();
            AppointmentsMapper mapper = sqlSession.getMapper(AppointmentsMapper.class);
            if (mapper.updateStatusForDoctorIfCurrent(appointmentId, doctorId, STATUS_BOOKED, STATUS_IN_PROGRESS) != 1) {
                return rollback(sqlSession, "INVALID_STATE", "预约不存在、不属于当前医生，或当前不能开始接诊。");
            }
            auditService.writeSuccess(sqlSession, audit, "APPOINTMENTS_START", "APPOINTMENT", Integer.toString(appointmentId));
            sqlSession.commit();
            return AppointmentOperationResult.success("已开始接诊。");
        } catch (Exception exception) {
            return rollback(sqlSession, "START_FAILED", "开始接诊失败，请稍后重试。");
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
    private static AuditContext synthetic(String role, int id, String path) { return new AuditContext(role, Integer.valueOf(id), "POST", path, null); }
    private static Appointments newAppointment(int patient, int doctor, String date, String slot, long slotId, Integer previousId) {
        Appointments appointment = new Appointments();
        appointment.setPatientId(patient); appointment.setDoctorId(doctor); appointment.setAppointmentDate(date);
        appointment.setAppointmentTime(slot); appointment.setScheduleSlotId(Long.valueOf(slotId));
        appointment.setPreviousAppointmentId(previousId); appointment.setStatus(STATUS_BOOKED);
        return appointment;
    }
    private static int safePage(int pageNum) { return pageNum > 0 ? pageNum : 1; }
    private static BigDecimal parseRegistrationFee(String value) {
        if (value == null) return null;
        try {
            BigDecimal fee = new BigDecimal(value.trim()).setScale(2, RoundingMode.UNNECESSARY);
            return fee.signum() >= 0 ? fee : null;
        } catch (RuntimeException exception) { return null; }
    }
    private static BigDecimal currentBalance(Patients patient) {
        try { return new BigDecimal(patient.getBalance() == null ? "0" : patient.getBalance()).setScale(2, RoundingMode.UNNECESSARY); }
        catch (RuntimeException exception) { throw new IllegalStateException("患者余额数据无效。", exception); }
    }
    private static AppointmentOperationResult rollback(SqlSession session, String code, String message) {
        if (session != null) session.rollback();
        return AppointmentOperationResult.failure(code, message);
    }
    private static void close(SqlSession session) { if (session != null) session.close(); }
}
