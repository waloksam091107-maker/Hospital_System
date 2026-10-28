package com.ustb.hospital.service;

import com.ustb.hospital.entity.MedicalOrder;
import com.ustb.hospital.entity.MedicalRecord;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.function.Supplier;
import static org.junit.Assert.*;

/** Uses actual mappers and transactions in a disposable H2 database. */
public class MedicalDraftLifecycleTest {
    private SqlSessionFactory factory;
    private MedicalRecordsServiceImpl service;
    private final AuditContext audit = new AuditContext("DOCTOR", 1, "POST", "/medical-records/draft", "127.0.0.1");

    @Before public void setup() throws Exception {
        Properties p = new Properties();
        p.setProperty("db.driver", "org.h2.Driver");
        p.setProperty("db.url", "jdbc:h2:mem:medical_" + UUID.randomUUID().toString().replace("-", "") + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        p.setProperty("db.username", "sa"); p.setProperty("db.password", "");
        try (InputStream in = Resources.getResourceAsStream("mybatis-config.xml")) { factory = new SqlSessionFactoryBuilder().build(in, p); }
        try (SqlSession session = factory.openSession(true); Statement s = session.getConnection().createStatement()) {
            s.execute("CREATE TABLE appointments (appointment_id INT PRIMARY KEY, doctor_id INT, patient_id INT, status VARCHAR(20), started_at TIMESTAMP, completed_at TIMESTAMP, version INT DEFAULT 0)");
            s.execute("INSERT INTO appointments(appointment_id,doctor_id,patient_id,status) VALUES (1,1,1,'IN_PROGRESS'),(2,2,2,'IN_PROGRESS')");
            s.execute("CREATE TABLE medical_records (medical_record_id BIGINT AUTO_INCREMENT PRIMARY KEY, appointment_id INT UNIQUE, doctor_id INT, patient_id INT, diagnosis VARCHAR(500), treatment_plan TEXT, medical_advice TEXT, record_status VARCHAR(10), version INT DEFAULT 0, finalized_at TIMESTAMP, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            s.execute("CREATE TABLE medical_orders (medical_order_id BIGINT AUTO_INCREMENT PRIMARY KEY, medical_record_id BIGINT, item_name VARCHAR(200), dosage VARCHAR(100), frequency VARCHAR(100), quantity INT, instruction_text VARCHAR(500), created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            s.execute("CREATE TABLE audit_logs (audit_log_id BIGINT AUTO_INCREMENT PRIMARY KEY, actor_role VARCHAR(20), actor_id INT, action VARCHAR(80), target_type VARCHAR(80), target_id VARCHAR(80), outcome VARCHAR(20), request_method VARCHAR(10), request_path VARCHAR(255), remote_address VARCHAR(64), created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            s.execute("CREATE TABLE doctors(doctor_id INT PRIMARY KEY)");
            s.execute("INSERT INTO doctors VALUES(1),(2)");
            com.ustb.hospital.support.ExtensionTestSchema.install(session.getConnection());
            s.execute("UPDATE appointments SET queue_status='IN_PROGRESS'");
        }
        try {
            Supplier<SqlSession> tx = () -> factory.openSession(false);
            Supplier<SqlSession> read = () -> factory.openSession(true);
            service = MedicalRecordsServiceImpl.class.getConstructor(Supplier.class, Supplier.class, AuditService.class)
                    .newInstance(tx, read, new AuditService(read));
        } catch (NoSuchMethodException missing) { throw new AssertionError("draft service must support isolated transaction testing", missing); }
    }

    @Test public void draftSaveDoesNotCompleteAppointment() throws Exception {
        MedicalRecordOperationResult result = draft(1, 1, "", "", null, orders("血常规"));
        assertTrue(result.isSuccessful()); assertEquals("DRAFT", record(1).getRecordStatus());
        assertEquals(0, record(1).getVersion()); assertEquals(1, record(1).getOrders().size());
        assertEquals("IN_PROGRESS", scalar("SELECT status FROM appointments WHERE appointment_id=1"));
        assertEquals("1", scalar("SELECT COUNT(*) FROM audit_logs"));
    }
    @Test public void patientCannotReadDraftOrOtherPatientRecord() throws Exception {
        assertTrue(draft(1, 1, "x", "y", null, Collections.<MedicalOrder>emptyList()).isSuccessful());
        assertNull(service.findForPatient(1, 1)); assertTrue(service.findAllForPatient(1).isEmpty());
        assertNull(service.findForDoctor(2, 1));
    }
    @Test public void doctorCannotEditOtherDoctorsAppointment() throws Exception {
        assertEquals("FORBIDDEN", draft(1, 2, "x", "y", null, Collections.<MedicalOrder>emptyList()).getCode());
        assertEquals("0", scalar("SELECT COUNT(*) FROM medical_records"));
    }
    @Test public void staleDraftCannotOverwriteNewerVersion() throws Exception {
        assertTrue(draft(1, 1, "a", "b", null, orders("一")).isSuccessful());
        assertTrue(draft(1, 1, "new", "plan", 0, orders("二")).isSuccessful());
        assertEquals("VERSION_CONFLICT", draft(1, 1, "old", "plan", 0, orders("三")).getCode());
        assertEquals("new", record(1).getDiagnosis()); assertEquals(1, record(1).getVersion());
        assertEquals("二", record(1).getOrders().get(0).getItemName());
    }
    @Test public void finalRequiresDiagnosisAndTreatmentPlan() throws Exception {
        assertEquals("INVALID_DIAGNOSIS", finish(" ", "plan", null).getCode());
        assertEquals("INVALID_TREATMENT_PLAN", finish("diagnosis", "  ", null).getCode());
        assertEquals("0", scalar("SELECT COUNT(*) FROM medical_records"));
    }
    @Test public void finalizationAndAuditRollbackTogether() throws Exception {
        assertTrue(draft(1, 1, "old", "old", null, orders("一")).isSuccessful());
        try (SqlSession s = factory.openSession(true)) { s.getConnection().createStatement().execute("DROP TABLE audit_logs"); }
        assertFalse(finish("new", "plan", 0).isSuccessful());
        assertEquals("old", record(1).getDiagnosis()); assertEquals("DRAFT", record(1).getRecordStatus());
        assertEquals("IN_PROGRESS", scalar("SELECT status FROM appointments WHERE appointment_id=1"));
    }
    @Test public void finalRecordCannotBeOverwritten() throws Exception {
        assertTrue(finish("diagnosis", "plan", null).isSuccessful());
        assertEquals("COMPLETED", scalar("SELECT status FROM appointments WHERE appointment_id=1"));
        assertEquals("DONE", scalar("SELECT queue_status FROM appointments WHERE appointment_id=1"));
        assertEquals("1", scalar("SELECT COUNT(*) FROM queue_history WHERE to_status='DONE'"));
        assertEquals("1", scalar("SELECT COUNT(*) FROM notifications WHERE recipient_role='PATIENT' AND recipient_id=1"));
        assertNotNull(service.findForPatient(1, 1)); assertNull(service.findForPatient(2, 1));
        assertFalse(finish("overwrite", "plan", null).isSuccessful());
        assertEquals("diagnosis", record(1).getDiagnosis());
    }
    @Test public void legacyCompletionCannotBypassMedicalRecord() {
        assertFalse(new AppointmentsServiceImpl().completeAppointment(1, 1).isSuccessful());
    }
    private MedicalRecord record(int id) { return service.findForDoctor(1, id); }
    private MedicalRecordOperationResult draft(int doctor, int appointment, String d, String p, Integer v, List<MedicalOrder> o) throws Exception {
        return (MedicalRecordOperationResult) MedicalRecordsServiceImpl.class.getMethod("saveDraft", int.class, int.class, String.class, String.class, String.class, List.class, Integer.class, AuditContext.class).invoke(service, doctor, appointment, d, p, null, o, v, audit);
    }
    private MedicalRecordOperationResult finish(String d, String p, Integer v) throws Exception {
        return (MedicalRecordOperationResult) MedicalRecordsServiceImpl.class.getMethod("finalizeEncounter", int.class, int.class, String.class, String.class, String.class, List.class, Integer.class, AuditContext.class).invoke(service, 1, 1, d, p, null, Collections.<MedicalOrder>emptyList(), v, audit);
    }
    private List<MedicalOrder> orders(String item) { MedicalOrder o = new MedicalOrder(); o.setItemName(item); return Collections.singletonList(o); }
    private String scalar(String sql) throws Exception { try (SqlSession s = factory.openSession(true); ResultSet r = s.getConnection().createStatement().executeQuery(sql)) { assertTrue(r.next()); return r.getString(1); } }
}
