package com.ustb.hospital.service;

import com.ustb.hospital.utils.AppointmentOperationResult;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.*;
import org.junit.Before;
import org.junit.Test;
import java.io.InputStream;
import java.sql.*;
import java.util.*;
import java.util.function.Supplier;
import static org.junit.Assert.*;

/** Business writes must be atomic with their audit event, using real mapper SQL. */
public class BusinessAuditAtomicityTest {
    private SqlSessionFactory factory;
    private final AuditContext patient = new AuditContext("PATIENT", 1, "POST", "/appointments/book", "127.0.0.1");
    private final AuditContext doctor = new AuditContext("DOCTOR", 1, "POST", "/schedules/create", "127.0.0.1");
    @Before public void setup() throws Exception {
        Properties p = new Properties(); p.setProperty("db.driver","org.h2.Driver");
        p.setProperty("db.url","jdbc:h2:mem:business_" + UUID.randomUUID().toString().replace("-", "") + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        p.setProperty("db.username","sa"); p.setProperty("db.password","");
        try (InputStream in = Resources.getResourceAsStream("mybatis-config.xml")) { factory = new SqlSessionFactoryBuilder().build(in,p); }
        try (SqlSession session = factory.openSession(true); Statement s = session.getConnection().createStatement()) {
            s.execute("CREATE TABLE doctors(doctor_id INT PRIMARY KEY, registration_fee VARCHAR(20))");
            s.execute("INSERT INTO doctors VALUES(1,'10')");
            s.execute("CREATE TABLE patients(patient_id INT PRIMARY KEY, balance DECIMAL(12,2), password VARCHAR(100), password_hash VARCHAR(60))");
            s.execute("INSERT INTO patients(patient_id,balance,password) VALUES(1,100,'old-password')");
            s.execute("CREATE TABLE appointments(appointment_id INT AUTO_INCREMENT PRIMARY KEY, patient_id INT, doctor_id INT, appointment_date VARCHAR(10), appointment_time VARCHAR(10), schedule_slot_id BIGINT, previous_appointment_id INT, status VARCHAR(20), started_at TIMESTAMP, completed_at TIMESTAMP, cancelled_at TIMESTAMP, version INT DEFAULT 0)");
            s.execute("CREATE TABLE schedule_slots(schedule_slot_id BIGINT AUTO_INCREMENT PRIMARY KEY, doctor_id INT, schedule_date VARCHAR(10), slot_period VARCHAR(10), slot_status VARCHAR(20), created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            s.execute("INSERT INTO schedule_slots(doctor_id,schedule_date,slot_period,slot_status) VALUES(1,'2030-01-01','AM','AVAILABLE'),(1,'2030-01-02','PM','AVAILABLE')");
            s.execute("CREATE TABLE dates(date_id INT AUTO_INCREMENT PRIMARY KEY, doctor_id INT, doctor_name VARCHAR(100), date VARCHAR(10), shang VARCHAR(2), xia VARCHAR(2), wan VARCHAR(2))");
            s.execute("CREATE TABLE audit_logs(audit_log_id BIGINT AUTO_INCREMENT PRIMARY KEY, actor_role VARCHAR(20), actor_id INT, action VARCHAR(80), target_type VARCHAR(80), target_id VARCHAR(80), outcome VARCHAR(20), request_method VARCHAR(10), request_path VARCHAR(255), remote_address VARCHAR(64), created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        }
    }
    @Test public void bookingAuditFailureRollsBackSlotAndAppointment() throws Exception {
        dropAudit(); assertFalse(book().isSuccessful());
        assertEquals("AVAILABLE", scalar("SELECT slot_status FROM schedule_slots WHERE schedule_slot_id=1"));
        assertEquals("0", scalar("SELECT COUNT(*) FROM appointments"));
        assertEquals("100.00", scalar("SELECT balance FROM patients WHERE patient_id=1"));
    }
    @Test public void successfulCreateUsesGeneratedTargetId() throws Exception {
        assertTrue(book().isSuccessful());
        assertEquals(scalar("SELECT CAST(appointment_id AS VARCHAR) FROM appointments"), scalar("SELECT target_id FROM audit_logs"));
    }
    @Test public void rescheduleAuditFailureRollsBackBothAppointmentsAndSlots() throws Exception {
        assertTrue(book().isSuccessful()); dropAudit();
        AppointmentOperationResult r = (AppointmentOperationResult) appointmentService().getClass().getMethod("rescheduleAppointment",int.class,int.class,String.class,String.class,AuditContext.class).invoke(appointmentService(),1,1,"2030-01-02","PM",patient);
        assertFalse(r.isSuccessful()); assertEquals("1", scalar("SELECT COUNT(*) FROM appointments"));
        assertEquals("BOOKED", scalar("SELECT status FROM appointments WHERE appointment_id=1"));
        assertEquals("BOOKED", scalar("SELECT slot_status FROM schedule_slots WHERE schedule_slot_id=1"));
        assertEquals("AVAILABLE", scalar("SELECT slot_status FROM schedule_slots WHERE schedule_slot_id=2"));
    }
    @Test public void scheduleAuditFailureRollsBackSchedule() throws Exception {
        dropAudit(); Object service = datesService();
        AppointmentOperationResult r = (AppointmentOperationResult) service.getClass().getMethod("addDate",int.class,String.class,String.class,String.class,String.class,String.class,AuditContext.class).invoke(service,1,"医生","2030-01-03","1","1","1",doctor);
        assertFalse(r.isSuccessful()); assertEquals("0", scalar("SELECT COUNT(*) FROM dates"));
        assertEquals("2", scalar("SELECT COUNT(*) FROM schedule_slots"));
    }
    @Test public void passwordAuditFailureRollsBackPasswordChange() throws Exception {
        dropAudit(); Supplier<SqlSession> tx = () -> factory.openSession(false);
        Object service = Class.forName("com.ustb.hospital.service.PasswordChangeService").getConstructor(Supplier.class, AuditService.class).newInstance(tx, new AuditService(() -> factory.openSession(true)));
        AppointmentOperationResult r = (AppointmentOperationResult) service.getClass().getMethod("change",String.class,int.class,String.class,String.class,AuditContext.class).invoke(service,"PATIENT",1,"old-password","new-password",patient);
        assertFalse(r.isSuccessful()); assertEquals("old-password", scalar("SELECT password FROM patients WHERE patient_id=1"));
    }
    @Test public void legacyPasswordUpgradeRollsBackWhenAuditFails() throws Exception {
        dropAudit(); Supplier<SqlSession> tx = () -> factory.openSession(false);
        Object service = Class.forName("com.ustb.hospital.service.PasswordChangeService").getConstructor(Supplier.class, AuditService.class).newInstance(tx, new AuditService(() -> factory.openSession(true)));
        AppointmentOperationResult r = (AppointmentOperationResult) service.getClass().getMethod("upgradeLegacy",String.class,int.class,String.class,AuditContext.class).invoke(service,"PATIENT",1,"old-password",patient);
        assertFalse(r.isSuccessful()); assertEquals("old-password", scalar("SELECT password FROM patients WHERE patient_id=1"));
    }
    @Test public void legacyUpgradeFailureIsRecordedIndependently() throws Exception {
        class FailingSuccessAudit extends AuditService {
            int failures;
            public void writeSuccess(SqlSession session, AuditContext context, String action, String type, String target) { throw new IllegalStateException("simulated failure"); }
            public void recordFailure(AuditContext context, String action, String type, String target) {
                failures++; assertEquals("PASSWORD_UPGRADE", action); assertEquals("ACCOUNT", type); assertEquals("1", target);
                super.recordFailure(context, action, type, target);
            }
            FailingSuccessAudit() { super(() -> factory.openSession(true)); }
        }
        FailingSuccessAudit audit = new FailingSuccessAudit();
        PasswordChangeService service = new PasswordChangeService(() -> factory.openSession(false), audit);
        assertFalse(service.upgradeLegacy("PATIENT", 1, "old-password", patient).isSuccessful());
        assertEquals(1, audit.failures);
        assertEquals("old-password", scalar("SELECT password FROM patients WHERE patient_id=1"));
        assertEquals("FAILURE", scalar("SELECT outcome FROM audit_logs"));
    }
    private AppointmentOperationResult book() throws Exception { Object service = appointmentService(); return (AppointmentOperationResult) service.getClass().getMethod("bookAppointment",int.class,int.class,String.class,String.class,AuditContext.class).invoke(service,1,1,"2030-01-01","AM",patient); }
    private Object appointmentService() throws Exception { Supplier<SqlSession> s = () -> factory.openSession(false); return AppointmentsServiceImpl.class.getConstructor(Supplier.class, AuditService.class).newInstance(s,new AuditService(() -> factory.openSession(true))); }
    private Object datesService() throws Exception { Supplier<SqlSession> s = () -> factory.openSession(false); return DatesServiceImpl.class.getConstructor(Supplier.class, AuditService.class).newInstance(s,new AuditService(() -> factory.openSession(true))); }
    private void dropAudit() throws Exception { try (SqlSession s = factory.openSession(true)) { s.getConnection().createStatement().execute("DROP TABLE audit_logs"); } }
    private String scalar(String q) throws Exception { try (SqlSession s = factory.openSession(true); ResultSet r = s.getConnection().createStatement().executeQuery(q)) { assertTrue(r.next()); return r.getString(1); } }
}
