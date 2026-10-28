package com.ustb.hospital.database;

import com.ustb.hospital.entity.MedicalOrder;
import com.ustb.hospital.service.*;
import com.ustb.hospital.utils.AppointmentOperationResult;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.*;
import org.junit.*;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.Assert.*;

/** Explicitly opt-in; aborts unless this is the independent test server's data directory. */
public class ClinicalAuditMysqlIntegrationTest {
    private SqlSessionFactory factory;
    private final AuditContext doctor = new AuditContext("DOCTOR",1,"POST","/medical-records/draft","127.0.0.1");
    private final AuditContext patient = new AuditContext("PATIENT",1,"POST","/appointments/book","127.0.0.1");
    @Before public void setup() throws Exception {
        Assume.assumeTrue(Boolean.getBoolean("hospital.test.mysql"));
        String schema="hospital_integration_"+UUID.randomUUID().toString().replace("-","");
        try (Connection c=DriverManager.getConnection("jdbc:mysql://127.0.0.1:13307/?serverTimezone=Asia/Shanghai","root",""); Statement s=c.createStatement()) {
            try(ResultSet r=s.executeQuery("SELECT @@port, @@datadir")){ assertTrue(r.next()); assertEquals(13307,r.getInt(1)); assertTrue(r.getString(2).replace('\\','/').toLowerCase().contains("/.superpowers/sdd/2026-10-17-clinical-records-audit-completion/mysql-data/")); }
            s.execute("CREATE DATABASE `"+schema+"` CHARACTER SET utf8mb4"); s.execute("USE `"+schema+"`");
            s.execute("CREATE TABLE doctors(doctor_id INT PRIMARY KEY,registration_fee VARCHAR(20)) ENGINE=InnoDB"); s.execute("INSERT INTO doctors VALUES(1,'10')");
            s.execute("CREATE TABLE patients(patient_id INT PRIMARY KEY,balance DECIMAL(12,2)) ENGINE=InnoDB"); s.execute("INSERT INTO patients VALUES(1,100)");
            s.execute("CREATE TABLE appointments(appointment_id INT AUTO_INCREMENT PRIMARY KEY,patient_id INT,doctor_id INT,appointment_date VARCHAR(10),appointment_time VARCHAR(10),schedule_slot_id BIGINT,previous_appointment_id INT,status VARCHAR(20),started_at TIMESTAMP NULL,completed_at TIMESTAMP NULL,cancelled_at TIMESTAMP NULL,version INT DEFAULT 0) ENGINE=InnoDB");
            s.execute("CREATE TABLE schedule_slots(schedule_slot_id BIGINT AUTO_INCREMENT PRIMARY KEY,doctor_id INT,schedule_date VARCHAR(10),slot_period VARCHAR(10),slot_status VARCHAR(20),created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP) ENGINE=InnoDB");
            s.execute("INSERT INTO schedule_slots(doctor_id,schedule_date,slot_period,slot_status) VALUES(1,'2030-01-01','AM','AVAILABLE')");
            s.execute("CREATE TABLE medical_records(medical_record_id BIGINT AUTO_INCREMENT PRIMARY KEY,appointment_id INT UNIQUE,doctor_id INT,patient_id INT,diagnosis VARCHAR(500) NOT NULL,treatment_plan TEXT,medical_advice TEXT,created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP) ENGINE=InnoDB");
            s.execute("CREATE TABLE medical_orders(medical_order_id BIGINT AUTO_INCREMENT PRIMARY KEY,medical_record_id BIGINT,item_name VARCHAR(200),dosage VARCHAR(100),frequency VARCHAR(100),quantity INT,instruction_text VARCHAR(500),created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP) ENGINE=InnoDB");
            s.execute("CREATE TABLE audit_logs(audit_log_id BIGINT AUTO_INCREMENT PRIMARY KEY,actor_role VARCHAR(20),actor_id INT,action VARCHAR(80),target_type VARCHAR(80),target_id VARCHAR(80),outcome VARCHAR(20),request_method VARCHAR(10),request_path VARCHAR(255),remote_address VARCHAR(64),created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP) ENGINE=InnoDB");
            String migration=new String(Files.readAllBytes(Paths.get("database/migrations/20261017_clinical_drafts_and_audit_query.sql")),StandardCharsets.UTF_8).replaceAll("(?m)^\\s*--.*$","");
            for(String part:migration.split(";")) if(!part.trim().isEmpty()) s.execute(part.trim());
            com.ustb.hospital.support.ExtensionTestSchema.install(c);
            s.execute("UPDATE schedule_slots SET capacity=1");
            s.execute("CREATE TABLE balance_transactions(transaction_id BIGINT AUTO_INCREMENT PRIMARY KEY,patient_id INT,appointment_id INT,transaction_type VARCHAR(40),amount DECIMAL(12,2),balance_after DECIMAL(12,2),note VARCHAR(255),created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP) ENGINE=InnoDB");
        }
        Properties p=new Properties(); p.setProperty("db.driver","com.mysql.cj.jdbc.Driver"); p.setProperty("db.url","jdbc:mysql://127.0.0.1:13307/"+schema+"?serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=UTF-8"); p.setProperty("db.username","root"); p.setProperty("db.password","");
        try(InputStream in=Resources.getResourceAsStream("mybatis-config.xml")){factory=new SqlSessionFactoryBuilder().build(in,p);}
    }
    @Test public void generatedAppointmentIdAndAuditFailureRollback() throws Exception {
        AppointmentsServiceImpl a=new AppointmentsServiceImpl(()->factory.openSession(false),new AuditService(()->factory.openSession(true)));
        assertTrue(a.bookAppointment(1,1,"2030-01-01","AM",patient).isSuccessful());
        assertEquals(scalar("SELECT appointment_id FROM appointments"),scalar("SELECT target_id FROM audit_logs WHERE action='APPOINTMENTS_BOOK'"));
        try(SqlSession s=factory.openSession(true)){s.getConnection().createStatement().execute("DROP TABLE audit_logs");}
        assertFalse(a.cancelAppointment(1,1,patient).isSuccessful());
        assertEquals("BOOKED",scalar("SELECT status FROM appointments")); assertEquals("AVAILABLE",scalar("SELECT slot_status FROM schedule_slots"));
        assertEquals("1",scalar("SELECT occupied_count FROM schedule_slots"));
    }
    @Test public void concurrentDraftsAndFinalizationAreAtomic() throws Exception {
        try(SqlSession s=factory.openSession(true)){s.getConnection().createStatement().execute("INSERT INTO appointments(patient_id,doctor_id,status,queue_status,appointment_sequence) VALUES(1,1,'IN_PROGRESS','IN_PROGRESS',1)");}
        MedicalRecordsServiceImpl m=new MedicalRecordsServiceImpl(()->factory.openSession(false),()->factory.openSession(true),new AuditService(()->factory.openSession(true)));
        assertTrue(m.saveDraft(1,1,"initial","plan",null,Collections.<MedicalOrder>emptyList(),null,doctor).isSuccessful());
        ExecutorService pool=Executors.newFixedThreadPool(2);
        try {
            Callable<MedicalRecordOperationResult> task=()->m.saveDraft(1,1,"updated","plan",null,Collections.<MedicalOrder>emptyList(),0,doctor);
            Future<MedicalRecordOperationResult> a=pool.submit(task),b=pool.submit(task);
            MedicalRecordOperationResult x=a.get(20,TimeUnit.SECONDS),y=b.get(20,TimeUnit.SECONDS);
            assertTrue(x.isSuccessful() ^ y.isSuccessful()); assertEquals("VERSION_CONFLICT",x.isSuccessful()?y.getCode():x.getCode());
        } finally { pool.shutdownNow(); }
        assertNull(m.findForPatient(1,1)); assertEquals("IN_PROGRESS",scalar("SELECT status FROM appointments"));
        try(SqlSession s=factory.openSession(true)){s.getConnection().createStatement().execute("DROP TABLE audit_logs");}
        assertFalse(m.finalizeEncounter(1,1,"final","plan",null,Collections.<MedicalOrder>emptyList(),1,doctor).isSuccessful());
        assertEquals("DRAFT",scalar("SELECT record_status FROM medical_records")); assertEquals("IN_PROGRESS",scalar("SELECT status FROM appointments"));
    }
    @Test public void combinedAuditQueryRunsOnMysql() throws Exception {
        AuditService audit=new AuditService(()->factory.openSession(true));
        try(SqlSession s=factory.openSession(false)){audit.writeSuccess(s,patient,"APPOINTMENTS_BOOK","APPOINTMENT","123");s.commit();}
        AuditQueryResult result=audit.search(new AuditQuery(null,null,"PATIENT","1","APPOINTMENTS_BOOK","SUCCESS","1","20"));
        assertEquals(1,result.getTotal()); assertEquals("123",result.getRows().get(0).getTargetId());
        try(SqlSession s=factory.openSession(true)){s.getConnection().createStatement().execute("INSERT INTO audit_logs(actor_role,actor_id,action,outcome) VALUES ('PATIENT',1,'_APPOINTMENTS_BOOK','FAILURE')");}
        assertEquals(1,audit.search(new AuditQuery(null,null,"PATIENT","1","APPOINTMENTS_BOOK","FAILURE","1","20")).getTotal());
    }
    private String scalar(String sql) throws Exception { try(SqlSession s=factory.openSession(true);ResultSet r=s.getConnection().createStatement().executeQuery(sql)){assertTrue(r.next());return r.getString(1);} }
}
