package com.ustb.hospital.database;

import org.apache.ibatis.jdbc.ScriptRunner;
import org.junit.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import static org.junit.Assert.*;

/** Opt-in actual legacy OceanBase upgrade. Requires a NEW restricted random test schema. */
public class ExtensionSchemaUpgradeOceanBaseTest {
    @Test public void upgradesLegacyOceanBaseAndRetainsClinicalAndFinancialRows() throws Exception {
        Assume.assumeTrue("Requires an explicitly prepared empty isolated OceanBase schema",Boolean.getBoolean("hospital.test.oceanbase.upgrade"));
        String schema=required("HOSPITAL_TEST_DB_NAME"),url=required("HOSPITAL_TEST_DB_URL");
        assertTrue(schema.matches("hospital_test_[a-f0-9]{32}"));
        assertTrue(url.matches("jdbc:oceanbase://127\\.0\\.0\\.1:[0-9]{4,5}/"+schema+"\\?.*"));
        String authority=url.substring("jdbc:oceanbase://".length(),url.indexOf('/',"jdbc:oceanbase://".length()));
        int port=Integer.parseInt(authority.substring(authority.indexOf(':')+1));
        assertTrue(port!=2881 && port!=3306 && port!=8080 && port<=65535);
        assertEquals("hospital_test_runner@test",required("HOSPITAL_TEST_DB_USERNAME"));
        Class.forName("com.oceanbase.jdbc.Driver");
        try(Connection c=DriverManager.getConnection(url,required("HOSPITAL_TEST_DB_USERNAME"),required("HOSPITAL_TEST_DB_PASSWORD"))) {
            assertEquals(schema,scalar(c,"SELECT DATABASE()"));
            assertTrue(scalar(c,"SELECT VERSION()").toLowerCase().contains("oceanbase"));
            assertEquals("Target must be newly prepared and empty","0",scalar(c,"SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE()"));
            try {scalar(c,"SELECT COUNT(*) FROM hospt.patients");fail("Restricted upgrade-test user can access business schema");}catch(SQLException inaccessible) { }
            try(InputStream input=getClass().getResourceAsStream("/database/legacy-before-five-extensions.sql")) {
                assertNotNull(input);ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] buffer=new byte[4096];int length;
                while((length=input.read(buffer))>=0)bytes.write(buffer,0,length);
                ScriptRunner runner=new ScriptRunner(c);runner.setLogWriter(null);runner.setErrorLogWriter(null);runner.setStopOnError(true);
                runner.runScript(new StringReader(IsolatedOceanBaseSql.rewriteSchema(new String(bytes.toByteArray(),StandardCharsets.UTF_8),schema)));
            }
            execute(c,"INSERT INTO departments(department_id,department_name) VALUES(1,'升级测试科室')");
            execute(c,"INSERT INTO professional_titles(id,title_name) VALUES(1,'升级测试职称')");
            execute(c,"INSERT INTO doctors(doctor_id,job_number,name,password_hash,avatar,department_id,professional_title_id) VALUES(1,'UPGRADE-D1','升级测试医生','original-doctor-hash','doctor.png',1,1)");
            execute(c,"INSERT INTO patients(patient_id,name,password_hash,avatar,balance) VALUES(1,'升级测试患者','original-patient-hash','patient.png',77.25)");
            execute(c,"INSERT INTO admins(admin_id,username,password_hash) VALUES(1,'upgrade-admin','original-admin-hash')");
            execute(c,"INSERT INTO dates(date_id,doctor_id,doctor_name,`date`,shang,xia,wan) VALUES(1,1,'升级测试医生','2030-01-01','1','0','0')");
            execute(c,"INSERT INTO schedule_slots(schedule_slot_id,doctor_id,schedule_date,slot_period,slot_status) VALUES(101,1,'2030-01-01','AM','BOOKED')");
            execute(c,"INSERT INTO appointments(appointment_id,patient_id,doctor_id,appointment_date,appointment_time,schedule_slot_id,status) VALUES(10,1,1,'2030-01-01','AM',101,'CANCELLED'),(11,1,1,'2030-01-01','AM',101,'COMPLETED')");
            execute(c,"INSERT INTO medical_records(medical_record_id,appointment_id,doctor_id,patient_id,diagnosis) VALUES(30,11,1,1,'保留历史病历')");
            execute(c,"INSERT INTO medical_orders(medical_order_id,medical_record_id,item_name) VALUES(40,30,'保留历史医嘱')");
            execute(c,"INSERT INTO balance_transactions(transaction_id,patient_id,appointment_id,transaction_type,amount,balance_after) VALUES(50,1,11,'APPOINTMENT_DEBIT',-22.75,77.25)");
            execute(c,"INSERT INTO audit_logs(audit_log_id,action,outcome) VALUES(60,'HISTORY','SUCCESS')");
            // ScriptRunner uses a transaction even on an initially autocommit connection.
            c.commit();c.setAutoCommit(true);
            ExtensionSchemaUpgrade.upgrade(c);ExtensionSchemaUpgrade.upgrade(c);
            assertEquals("77.25|original-patient-hash|patient.png",scalar(c,"SELECT CONCAT(balance,'|',password_hash,'|',avatar) FROM patients WHERE patient_id=1"));
            assertEquals("1|1|2|AVAILABLE",scalar(c,"SELECT CONCAT(capacity,'|',occupied_count,'|',next_sequence,'|',slot_status) FROM schedule_slots WHERE schedule_slot_id=101"));
            assertEquals("2|DONE",scalar(c,"SELECT CONCAT(appointment_sequence,'|',queue_status) FROM appointments WHERE appointment_id=11"));
            assertEquals("1",scalar(c,"SELECT COUNT(*) FROM medical_records WHERE medical_record_id=30 AND appointment_id=11"));
            assertEquals("1",scalar(c,"SELECT COUNT(*) FROM medical_orders WHERE medical_order_id=40 AND medical_record_id=30"));
            assertEquals("1",scalar(c,"SELECT COUNT(*) FROM balance_transactions WHERE transaction_id=50"));
            assertEquals("1",scalar(c,"SELECT COUNT(*) FROM audit_logs WHERE audit_log_id=60"));
            assertEquals("0",scalar(c,"SELECT COUNT(*) FROM notifications"));
            assertEquals("3",scalar(c,"SELECT COUNT(*) FROM account_security"));
            assertEquals("1",scalar(c,"SELECT COUNT(*) FROM extension_schema_progress WHERE migration_key='20261028-five-extensions' AND phase='COMPLETE'"));
        }
    }
    private static String required(String key){String value=System.getenv(key);if(value==null||value.trim().isEmpty())throw new IllegalStateException("Missing isolated upgrade test configuration");return value;}
    private static String scalar(Connection c,String sql)throws SQLException{try(Statement s=c.createStatement();ResultSet r=s.executeQuery(sql)){if(!r.next())throw new SQLException("Missing scalar result");return r.getString(1);}}
    private static void execute(Connection c,String sql)throws SQLException{try(Statement s=c.createStatement()){s.execute(sql);}}
}
