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
        return workflow().book(patientId, doctorId, appointmentDate, requestedSlot, audit);
    }

    public AppointmentOperationResult cancelAppointment(int patientId, int appointmentId) {
        return cancelAppointment(patientId, appointmentId, synthetic("PATIENT", patientId, "/appointments/cancel"));
    }
    public AppointmentOperationResult cancelAppointment(int patientId, int appointmentId, AuditContext audit) {
        return workflow().patientAction("cancel", patientId, appointmentId, null, null, audit);
    }

    public AppointmentOperationResult rescheduleAppointment(int patientId, int appointmentId, String newDate, String requestedSlot) {
        return rescheduleAppointment(patientId, appointmentId, newDate, requestedSlot, synthetic("PATIENT", patientId, "/appointments/reschedule"));
    }
    public AppointmentOperationResult rescheduleAppointment(int patientId, int appointmentId, String newDate, String requestedSlot, AuditContext audit) {
        return workflow().patientAction("reschedule", patientId, appointmentId, newDate, requestedSlot, audit);
    }

    public AppointmentOperationResult completeAppointment(int doctorId, int appointmentId) {
        return AppointmentOperationResult.failure("MEDICAL_RECORD_REQUIRED", "请填写并提交就诊记录后完成接诊。");
    }

    public AppointmentOperationResult startAppointment(int doctorId, int appointmentId) {
        return startAppointment(doctorId, appointmentId, synthetic("DOCTOR", doctorId, "/appointments/start"));
    }
    public AppointmentOperationResult startAppointment(int doctorId, int appointmentId, AuditContext audit) {
        return workflow().queueAction("start", "DOCTOR", doctorId, appointmentId, null, null, audit);
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

    private ClinicWorkflowService workflow() { return new ClinicWorkflowService(transactionSessions, auditService, java.time.Clock.system(ClinicTime.ZONE)); }

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
