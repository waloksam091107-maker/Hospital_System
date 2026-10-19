package com.ustb.hospital.service;

import com.ustb.hospital.entity.Appointments;
import com.ustb.hospital.entity.MedicalOrder;
import com.ustb.hospital.entity.MedicalRecord;
import com.ustb.hospital.mapper.AppointmentsMapper;
import com.ustb.hospital.mapper.MedicalRecordsMapper;
import com.ustb.hospital.utils.MybatisUtil;
import org.apache.ibatis.session.SqlSession;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/** Writes an encounter and closes its appointment as one transaction. */
public class MedicalRecordsServiceImpl {
    private static final String STATUS_IN_PROGRESS = AppointmentStatus.IN_PROGRESS;
    private static final String STATUS_COMPLETED = AppointmentStatus.COMPLETED;
    private static final int MAX_DIAGNOSIS_LENGTH = 500;
    private static final int MAX_TEXT_LENGTH = 5000;
    private static final int MAX_ORDERS = 20;
    private final Supplier<SqlSession> transactionSessions;
    private final Supplier<SqlSession> readSessions;
    private final AuditService auditService;

    public MedicalRecordsServiceImpl() {
        this(MybatisUtil::getTransactionSqlSession, MybatisUtil::getSqlSession, new AuditService());
    }

    public MedicalRecordsServiceImpl(Supplier<SqlSession> transactions, Supplier<SqlSession> reads, AuditService auditService) {
        this.transactionSessions = transactions;
        this.readSessions = reads;
        this.auditService = auditService;
    }

    public MedicalRecordOperationResult finalizeEncounter(int doctorId, int appointmentId, String diagnosis,
                                                           String treatmentPlan, String medicalAdvice,
                                                           List<MedicalOrder> orders) {
        return finalizeEncounter(doctorId, appointmentId, diagnosis, treatmentPlan, medicalAdvice, orders, null,
                new AuditContext("DOCTOR", Integer.valueOf(doctorId), "POST", "/medical-records/finalize", null));
    }

    public MedicalRecordOperationResult saveDraft(int doctorId, int appointmentId, String diagnosis,
                                                   String treatmentPlan, String medicalAdvice, List<MedicalOrder> orders,
                                                   Integer expectedVersion, AuditContext audit) {
        return save(doctorId, appointmentId, diagnosis, treatmentPlan, medicalAdvice, orders, expectedVersion, audit, false);
    }

    public MedicalRecordOperationResult finalizeEncounter(int doctorId, int appointmentId, String diagnosis,
                                                           String treatmentPlan, String medicalAdvice, List<MedicalOrder> orders,
                                                           Integer expectedVersion, AuditContext audit) {
        return save(doctorId, appointmentId, diagnosis, treatmentPlan, medicalAdvice, orders, expectedVersion, audit, true);
    }

    private MedicalRecordOperationResult save(int doctorId, int appointmentId, String diagnosis, String treatmentPlan,
                                               String medicalAdvice, List<MedicalOrder> orders, Integer expectedVersion,
                                               AuditContext audit, boolean finalize) {
        MedicalRecordOperationResult validation = validate(diagnosis, treatmentPlan, medicalAdvice, orders, finalize);
        if (validation != null) return validation;

        SqlSession session = null;
        try {
            session = transactionSessions.get();
            AppointmentsMapper appointmentsMapper = session.getMapper(AppointmentsMapper.class);
            MedicalRecordsMapper recordsMapper = session.getMapper(MedicalRecordsMapper.class);
            Appointments appointment = appointmentsMapper.selectByIdForUpdate(appointmentId);
            if (appointment == null || appointment.getDoctorId() != doctorId) {
                return rollback(session, "FORBIDDEN", "预约不存在或不属于当前医生。");
            }
            MedicalRecord existing = recordsMapper.selectByAppointmentForUpdate(appointmentId);
            if (existing != null && !"DRAFT".equals(existing.getRecordStatus())) {
                return rollback(session, "DUPLICATE_RECORD", "该预约已经有正式就诊记录。");
            }
            if (!STATUS_IN_PROGRESS.equals(appointment.getStatus())) {
                return rollback(session, "INVALID_STATE", "只有接诊中的预约可以提交就诊记录。");
            }
            if ((existing == null && expectedVersion != null) || (existing != null &&
                    (expectedVersion == null || expectedVersion.intValue() != existing.getVersion()))) {
                return rollback(session, "VERSION_CONFLICT", "病历草稿已变化，请重新读取后再保存。");
            }

            MedicalRecord record = new MedicalRecord();
            record.setAppointmentId(appointmentId);
            record.setDoctorId(doctorId);
            record.setPatientId(appointment.getPatientId());
            record.setDiagnosis(normalizeNullable(diagnosis));
            record.setTreatmentPlan(normalizeNullable(treatmentPlan));
            record.setMedicalAdvice(normalizeNullable(medicalAdvice));
            record.setRecordStatus(finalize ? "FINAL" : "DRAFT");
            if (existing == null) {
                record.setVersion(0);
                if (recordsMapper.insertRecord(record) != 1) return rollback(session, "RECORD_SAVE_FAILED", "就诊记录保存失败。");
            } else {
                record.setMedicalRecordId(existing.getMedicalRecordId());
                record.setVersion(existing.getVersion() + 1);
                if (recordsMapper.updateDraft(record, expectedVersion.intValue()) != 1)
                    return rollback(session, "VERSION_CONFLICT", "病历草稿已变化，请重新读取后再保存。");
                recordsMapper.deleteOrdersByRecordId(record.getMedicalRecordId());
            }
            List<MedicalOrder> sanitizedOrders = sanitizeOrders(orders, record.getMedicalRecordId());
            if (!sanitizedOrders.isEmpty() && recordsMapper.insertOrders(sanitizedOrders) != sanitizedOrders.size()) {
                return rollback(session, "ORDER_SAVE_FAILED", "医嘱保存失败。");
            }
            if (finalize && appointmentsMapper.updateStatusForDoctorIfCurrent(appointmentId, doctorId,
                    STATUS_IN_PROGRESS, STATUS_COMPLETED) != 1) {
                return rollback(session, "INVALID_STATE", "预约状态已变化，未完成接诊。");
            }
            auditService.writeSuccess(session, audit, finalize ? "MEDICAL_RECORD_FINALIZE" : "MEDICAL_RECORD_DRAFT",
                    "APPOINTMENT", Integer.toString(appointmentId));
            session.commit();
            return MedicalRecordOperationResult.success(finalize ? "就诊记录和医嘱已保存，接诊已完成。" : "草稿已保存。",
                    record.getMedicalRecordId(), record.getRecordStatus(), record.getVersion());
        } catch (Exception exception) {
            return rollback(session, "FINALIZE_FAILED", "提交就诊记录失败，请稍后重试。");
        } finally {
            close(session);
        }
    }

    public MedicalRecord findForDoctor(int doctorId, int appointmentId) {
        SqlSession session = readSessions.get();
        try {
            MedicalRecordsMapper mapper = session.getMapper(MedicalRecordsMapper.class);
            return withOrders(mapper, mapper.selectForDoctor(appointmentId, doctorId));
        } finally { close(session); }
    }

    public MedicalRecord findForPatient(int patientId, int appointmentId) {
        SqlSession session = readSessions.get();
        try {
            MedicalRecordsMapper mapper = session.getMapper(MedicalRecordsMapper.class);
            return withOrders(mapper, mapper.selectForPatient(appointmentId, patientId));
        } finally { close(session); }
    }

    public List<MedicalRecord> findAllForPatient(int patientId) {
        SqlSession session = readSessions.get();
        try {
            MedicalRecordsMapper mapper = session.getMapper(MedicalRecordsMapper.class);
            List<MedicalRecord> records = mapper.selectAllForPatient(patientId);
            for (MedicalRecord record : records) withOrders(mapper, record);
            return records;
        } finally { close(session); }
    }

    private static MedicalRecordOperationResult validate(String diagnosis, String treatmentPlan, String advice,
                                                          List<MedicalOrder> orders, boolean finalize) {
        String normalizedDiagnosis = normalize(diagnosis);
        if ((finalize && (normalizedDiagnosis == null || normalizedDiagnosis.length() == 0)) ||
                (normalizedDiagnosis != null && normalizedDiagnosis.length() > MAX_DIAGNOSIS_LENGTH)) {
            return MedicalRecordOperationResult.failure("INVALID_DIAGNOSIS", "诊断不能为空且不能超过500个字符。");
        }
        if (finalize && normalizeNullable(treatmentPlan) == null) {
            return MedicalRecordOperationResult.failure("INVALID_TREATMENT_PLAN", "诊疗方案不能为空。");
        }
        if (tooLong(treatmentPlan) || tooLong(advice)) {
            return MedicalRecordOperationResult.failure("INVALID_TEXT", "诊疗方案或医嘱说明过长。");
        }
        List<MedicalOrder> safeOrders = orders == null ? Collections.<MedicalOrder>emptyList() : orders;
        if (safeOrders.size() > MAX_ORDERS) {
            return MedicalRecordOperationResult.failure("TOO_MANY_ORDERS", "单次接诊最多保存20条医嘱。");
        }
        for (MedicalOrder order : safeOrders) {
            if (order == null || normalize(order.getItemName()) == null || normalize(order.getItemName()).length() == 0
                    || normalize(order.getItemName()).length() > 200 || tooLong(order.getInstructionText(), 500)
                    || tooLong(order.getDosage(), 100) || tooLong(order.getFrequency(), 100)
                    || (order.getQuantity() != null && order.getQuantity().intValue() <= 0)) {
                return MedicalRecordOperationResult.failure("INVALID_ORDER", "医嘱项目、剂量、频次或数量无效。");
            }
        }
        return null;
    }

    private static List<MedicalOrder> sanitizeOrders(List<MedicalOrder> orders, long recordId) {
        if (orders == null || orders.isEmpty()) return Collections.emptyList();
        List<MedicalOrder> copies = new ArrayList<MedicalOrder>();
        for (MedicalOrder source : orders) {
            MedicalOrder copy = new MedicalOrder();
            copy.setMedicalRecordId(recordId);
            copy.setItemName(normalize(source.getItemName()));
            copy.setDosage(normalizeNullable(source.getDosage()));
            copy.setFrequency(normalizeNullable(source.getFrequency()));
            copy.setQuantity(source.getQuantity());
            copy.setInstructionText(normalizeNullable(source.getInstructionText()));
            copies.add(copy);
        }
        return copies;
    }

    private static MedicalRecord withOrders(MedicalRecordsMapper mapper, MedicalRecord record) {
        if (record != null) record.setOrders(mapper.selectOrdersByRecordId(record.getMedicalRecordId()));
        return record;
    }

    private static boolean tooLong(String value) { return tooLong(value, MAX_TEXT_LENGTH); }
    private static boolean tooLong(String value, int max) { return value != null && value.trim().length() > max; }
    private static String normalize(String value) { return value == null ? null : value.trim(); }
    private static String normalizeNullable(String value) { String normalized = normalize(value); return normalized == null || normalized.length() == 0 ? null : normalized; }
    private static MedicalRecordOperationResult rollback(SqlSession session, String code, String message) {
        if (session != null) session.rollback();
        return MedicalRecordOperationResult.failure(code, message);
    }
    private static void close(SqlSession session) { if (session != null) session.close(); }
}
