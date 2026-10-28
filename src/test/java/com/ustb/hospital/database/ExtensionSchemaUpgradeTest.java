package com.ustb.hospital.database;

import org.junit.Test;
import java.lang.reflect.*;
import java.sql.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.Assert.*;

/** Actual H2 DDL/data upgrade; no business environment is contacted. */
public class ExtensionSchemaUpgradeTest {
    @Test public void upgradesWithoutChangingBusinessHistoryAndKeepsLegacyCapacityOne() throws Exception {
        try(Connection c=legacy()) {
            upgrade(c);
            assertEquals("2",scalar(c,"SELECT COUNT(*) FROM patients"));
            assertEquals("77.25|old-hash|patient.png",scalar(c,"SELECT CONCAT(balance,'|',password_hash,'|',avatar) FROM patients WHERE patient_id=1"));
            assertEquals("1",scalar(c,"SELECT COUNT(*) FROM medical_records WHERE medical_record_id=30 AND appointment_id=12"));
            assertEquals("1",scalar(c,"SELECT COUNT(*) FROM medical_orders WHERE medical_order_id=40 AND medical_record_id=30"));
            assertEquals("1",scalar(c,"SELECT COUNT(*) FROM balance_transactions WHERE transaction_id=50"));
            assertEquals("1",scalar(c,"SELECT COUNT(*) FROM audit_logs WHERE audit_log_id=60"));
            assertEquals("1|1|2|AVAILABLE",scalar(c,"SELECT CONCAT(capacity,'|',occupied_count,'|',next_sequence,'|',slot_status) FROM schedule_slots WHERE schedule_slot_id=101"));
            assertEquals("CLOSED",scalar(c,"SELECT slot_status FROM schedule_slots WHERE schedule_slot_id=102"));
            assertEquals("1|REMOVED",scalar(c,"SELECT CONCAT(appointment_sequence,'|',queue_status) FROM appointments WHERE appointment_id=10"));
            assertEquals("2|NOT_CHECKED_IN",scalar(c,"SELECT CONCAT(appointment_sequence,'|',queue_status) FROM appointments WHERE appointment_id=11"));
            assertEquals("1|DONE",scalar(c,"SELECT CONCAT(appointment_sequence,'|',queue_status) FROM appointments WHERE appointment_id=12"));
            assertEquals("0",scalar(c,"SELECT COUNT(*) FROM notifications"));
            assertEquals("4",scalar(c,"SELECT COUNT(*) FROM account_security WHERE enabled=TRUE AND session_version=0 AND must_change_password=FALSE"));
            assertFalse(index(c,"uk_appointments_active_slot"));
            assertTrue(index(c,"uk_appointments_active_patient_slot"));
            assertTrue(index(c,"uk_appointments_slot_sequence"));
        }
    }

    @Test public void completedUpgradeCanBeRepeatedWithoutResettingNewRuntimeState() throws Exception {
        try(Connection c=legacy()) {
            upgrade(c); execute(c,"UPDATE schedule_slots SET capacity=9,next_sequence=20 WHERE schedule_slot_id=101");
            execute(c,"UPDATE appointments SET queue_status='WAITING',checked_in_at=CURRENT_TIMESTAMP WHERE appointment_id=11");
            execute(c,"UPDATE account_security SET enabled=FALSE,session_version=3 WHERE account_role='DOCTOR'");
            upgrade(c);
            assertEquals("9|20",scalar(c,"SELECT CONCAT(capacity,'|',next_sequence) FROM schedule_slots WHERE schedule_slot_id=101"));
            assertEquals("WAITING",scalar(c,"SELECT queue_status FROM appointments WHERE appointment_id=11"));
            assertEquals("3",scalar(c,"SELECT session_version FROM account_security WHERE account_role='DOCTOR'"));
            execute(c,"INSERT INTO schedule_slots(schedule_slot_id,doctor_id,schedule_date,slot_period,slot_status) VALUES(103,1,'2030-01-03','AM','AVAILABLE')");
            assertEquals("10",scalar(c,"SELECT capacity FROM schedule_slots WHERE schedule_slot_id=103"));
        }
    }

    @Test public void nullSlotPreflightStopsBeforeAnySchemaOrDataMutation() throws Exception {
        try(Connection c=legacy()) {
            execute(c,"UPDATE appointments SET schedule_slot_id=NULL WHERE appointment_id=10");
            expectPreflightFailure(c,"10"); assertNoMutation(c);
        }
    }

    @Test public void mismatchedSlotAndUnknownStatusStopBeforeMutation() throws Exception {
        try(Connection c=legacy()) {
            execute(c,"UPDATE appointments SET appointment_date='2030-01-08' WHERE appointment_id=10");
            expectPreflightFailure(c,"10"); assertNoMutation(c);
        }
        try(Connection c=legacy()) {
            execute(c,"ALTER TABLE appointments DROP CONSTRAINT chk_appointments_status");
            execute(c,"UPDATE appointments SET status='UNKNOWN' WHERE appointment_id=10");
            expectPreflightFailure(c,"10"); assertNoMutation(c);
        }
    }

    @Test public void duplicatePatientAndOversubscribedLegacySlotAreNotSilentlyRepaired() throws Exception {
        try(Connection c=legacy()) {
            execute(c,"DROP INDEX uk_appointments_active_slot");
            execute(c,"INSERT INTO appointments VALUES(13,1,1,'2030-01-01','AM',101,'BOOKED',DEFAULT)");
            expectPreflightFailure(c,"101"); assertNoMutation(c);
        }
        try(Connection c=legacy()) {
            execute(c,"DROP INDEX uk_appointments_active_slot");
            execute(c,"INSERT INTO appointments VALUES(13,2,1,'2030-01-01','AM',101,'BOOKED',DEFAULT)");
            expectPreflightFailure(c,"101"); assertNoMutation(c);
        }
    }

    @Test public void ddlFailureRetainsOldGuardAndRetryCompletesWithoutRewritingHistory() throws Exception {
        try(Connection real=legacy()) {
            final boolean[] failed={false};
            Connection interrupted=(Connection)Proxy.newProxyInstance(Connection.class.getClassLoader(),new Class[]{Connection.class},(p,m,a)->{
                Object value=invoke(real,m,a);
                if("createStatement".equals(m.getName())) {
                    Statement delegate=(Statement)value;
                    return Proxy.newProxyInstance(Statement.class.getClassLoader(),new Class[]{Statement.class},(sp,sm,sa)->{
                        if(!failed[0] && sa!=null && sa.length>0 && sa[0] instanceof String && ((String)sa[0]).toLowerCase(Locale.ROOT).contains("create unique index uk_appointments_slot_sequence")) {failed[0]=true;throw new SQLException("Injected interrupted DDL");}
                        return invoke(delegate,sm,sa);
                    });
                }
                return value;
            });
            try { upgrade(interrupted); fail("Expected interrupted DDL"); } catch(SQLException expected) {assertTrue(failed[0]);}
            assertTrue("Old uniqueness must remain until both replacements exist",index(real,"uk_appointments_active_slot"));
            upgrade(real);
            assertFalse(index(real,"uk_appointments_active_slot"));
            assertEquals("3",scalar(real,"SELECT COUNT(*) FROM appointments"));
            assertEquals("2",scalar(real,"SELECT appointment_sequence FROM appointments WHERE appointment_id=11"));
        }
    }

    @Test public void misleadingReplacementIndexNameCannotRemoveOldUniqueness() throws Exception {
        try(Connection c=legacy()) {
            execute(c,"CREATE INDEX uk_appointments_active_patient_slot ON appointments(patient_id)");
            try {upgrade(c);fail("A nonunique/wrong-column index must not be accepted as the replacement guard");}
            catch(SQLException expected){assertTrue(expected.getMessage().contains("uk_appointments_active_patient_slot"));}
            assertNoMutation(c);assertTrue(index(c,"uk_appointments_active_slot"));
        }
    }

    @Test public void newEmptySchemaUsesCapacityAndPerPatientUniqueConstraints() throws Exception {
        try(Connection c=DriverManager.getConnection("jdbc:h2:mem:empty_extensions_"+System.nanoTime()+";MODE=MySQL;DB_CLOSE_DELAY=-1","sa","")) {
            String schema=new String(Files.readAllBytes(Paths.get("database/oceanbase/01_hospt_schema.sql")),StandardCharsets.UTF_8);
            schema=schema.replaceAll("(?m)^\\s*--[^\\r\\n]*","").replaceAll("(?im)^\\s*(CREATE DATABASE IF NOT EXISTS hospt|USE hospt);","").replaceAll("(?i)\\)\\s+STORED", ")");
            for(String statement:schema.split(";")) if(!statement.trim().isEmpty()) execute(c,statement);
            assertTrue("Fresh schema must expose slot capacity",column(c,"schedule_slots","capacity"));
            execute(c,"INSERT INTO departments(department_id,department_name) VALUES(1,'科室')");execute(c,"INSERT INTO professional_titles(id,title_name) VALUES(1,'职称')");
            execute(c,"INSERT INTO doctors(doctor_id,job_number,name,department_id,professional_title_id) VALUES(1,'D1','医生',1,1)");
            execute(c,"INSERT INTO patients(patient_id,name) VALUES(1,'甲'),(2,'乙')");
            execute(c,"INSERT INTO schedule_slots(schedule_slot_id,doctor_id,schedule_date,slot_period,slot_status) VALUES(101,1,'2030-01-01','AM','AVAILABLE')");
            assertEquals("10",scalar(c,"SELECT capacity FROM schedule_slots WHERE schedule_slot_id=101"));
            execute(c,"INSERT INTO appointments(patient_id,doctor_id,appointment_date,appointment_time,schedule_slot_id,status,appointment_sequence) VALUES(1,1,'2030-01-01','AM',101,'BOOKED',1),(2,1,'2030-01-01','AM',101,'NO_SHOW',2)");
            try {execute(c,"INSERT INTO appointments(patient_id,doctor_id,appointment_date,appointment_time,schedule_slot_id,status,appointment_sequence) VALUES(1,1,'2030-01-01','AM',101,'BOOKED',3)");fail("Same patient cannot hold duplicate active slot");}catch(SQLException expected) {assertEquals("23505",expected.getSQLState());}
        }
    }

    private static Connection legacy() throws Exception {
        Connection c=DriverManager.getConnection("jdbc:h2:mem:legacy_extensions_"+System.nanoTime()+";MODE=MySQL;DB_CLOSE_DELAY=-1","sa","");
        String[] sql={
            "CREATE TABLE doctors(doctor_id INT PRIMARY KEY,password_hash VARCHAR(60),avatar VARCHAR(100))",
            "CREATE TABLE patients(patient_id INT PRIMARY KEY,balance DECIMAL(12,2),password_hash VARCHAR(60),avatar VARCHAR(100))",
            "CREATE TABLE admins(admin_id INT PRIMARY KEY,password_hash VARCHAR(60))",
            "CREATE TABLE dates(date_id INT PRIMARY KEY,doctor_id INT,`date` DATE,shang CHAR(1),xia CHAR(1),wan CHAR(1))",
            "CREATE TABLE schedule_slots(schedule_slot_id BIGINT PRIMARY KEY,doctor_id INT,schedule_date DATE,slot_period VARCHAR(20),slot_status VARCHAR(20),CONSTRAINT chk_schedule_slots_status CHECK(slot_status IN('AVAILABLE','BOOKED','CLOSED')))",
            "CREATE TABLE appointments(appointment_id INT PRIMARY KEY,patient_id INT,doctor_id INT,appointment_date DATE,appointment_time VARCHAR(20),schedule_slot_id BIGINT,status VARCHAR(20),active_slot_key VARCHAR(128) AS(CASE WHEN status IN('BOOKED','IN_PROGRESS','COMPLETED') THEN CONCAT(doctor_id,'|',appointment_date,'|',appointment_time) ELSE NULL END),CONSTRAINT chk_appointments_status CHECK(status IN('BOOKED','RESCHEDULED','CANCELLED','IN_PROGRESS','COMPLETED')))",
            "CREATE UNIQUE INDEX uk_appointments_active_slot ON appointments(active_slot_key)",
            "CREATE TABLE medical_records(medical_record_id BIGINT PRIMARY KEY,appointment_id INT,diagnosis VARCHAR(100))",
            "CREATE TABLE medical_orders(medical_order_id BIGINT PRIMARY KEY,medical_record_id BIGINT,item_name VARCHAR(100))",
            "CREATE TABLE balance_transactions(transaction_id BIGINT PRIMARY KEY,patient_id INT,amount DECIMAL(12,2))",
            "CREATE TABLE audit_logs(audit_log_id BIGINT PRIMARY KEY,action VARCHAR(80))",
            "INSERT INTO doctors VALUES(1,'doctor-hash','doctor.png')","INSERT INTO patients VALUES(1,77.25,'old-hash','patient.png'),(2,100.00,'second-hash','second.png')","INSERT INTO admins VALUES(1,'admin-hash')",
            "INSERT INTO dates VALUES(1,1,'2030-01-01','1','0','0'),(2,1,'2030-01-02','0','0','0')",
            "INSERT INTO schedule_slots VALUES(101,1,'2030-01-01','AM','BOOKED'),(102,1,'2030-01-02','AM','BOOKED')",
            "INSERT INTO appointments VALUES(10,1,1,'2030-01-01','AM',101,'CANCELLED',DEFAULT),(11,1,1,'2030-01-01','AM',101,'BOOKED',DEFAULT),(12,2,1,'2030-01-02','AM',102,'COMPLETED',DEFAULT)",
            "INSERT INTO medical_records VALUES(30,12,'历史病历')","INSERT INTO medical_orders VALUES(40,30,'历史医嘱')","INSERT INTO balance_transactions VALUES(50,1,-22.75)","INSERT INTO audit_logs VALUES(60,'HISTORY')"
        };
        for(String statement:sql)execute(c,statement);return c;
    }
    private static void upgrade(Connection c) throws Exception {
        Class<?> type;
        try {type=Class.forName("com.ustb.hospital.database.ExtensionSchemaUpgrade");}catch(ClassNotFoundException missing){throw new AssertionError("ExtensionSchemaUpgrade must preserve legacy history and install resumable constraints",missing);}
        try{type.getMethod("upgrade",Connection.class).invoke(null,c);}catch(InvocationTargetException e){if(e.getCause() instanceof Exception)throw(Exception)e.getCause();throw e;}
    }
    private static void expectPreflightFailure(Connection c,String id) throws Exception {try{upgrade(c);fail("Expected preflight rejection");}catch(SQLException expected){assertTrue(expected.getMessage(),expected.getMessage().contains(id));}}
    private static void assertNoMutation(Connection c)throws Exception{assertFalse(column(c,"schedule_slots","capacity"));assertFalse(table(c,"extension_schema_progress"));assertEquals("BOOKED",scalar(c,"SELECT slot_status FROM schedule_slots WHERE schedule_slot_id=101"));}
    private static Object invoke(Object o,Method m,Object[] a)throws Throwable{try{return m.invoke(o,a);}catch(InvocationTargetException e){throw e.getCause();}}
    private static boolean table(Connection c,String name)throws Exception{try(ResultSet r=c.getMetaData().getTables(null,null,name.toUpperCase(Locale.ROOT),new String[]{"TABLE"})){return r.next();}}
    private static boolean column(Connection c,String table,String name)throws Exception{try(ResultSet r=c.getMetaData().getColumns(null,null,table.toUpperCase(Locale.ROOT),name.toUpperCase(Locale.ROOT))){return r.next();}}
    private static boolean index(Connection c,String name)throws Exception{try(ResultSet r=c.getMetaData().getIndexInfo(null,null,"APPOINTMENTS",false,false)){while(r.next())if(name.equalsIgnoreCase(r.getString("INDEX_NAME")))return true;return false;}}
    private static String scalar(Connection c,String sql)throws Exception{try(Statement s=c.createStatement();ResultSet r=s.executeQuery(sql)){assertTrue(r.next());return r.getString(1);}}
    private static void execute(Connection c,String sql)throws SQLException{try(Statement s=c.createStatement()){s.execute(sql);}}
}
