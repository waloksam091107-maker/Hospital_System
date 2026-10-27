package com.ustb.hospital.service;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.*;
import org.junit.*;
import java.io.InputStream;
import java.sql.*;
import java.util.*;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.*;
import static org.junit.Assert.*;

/** Real mapper/transaction tests in a fresh database; never connects to business data. */
public class AdminMasterDataMaintenanceTest {
    private SqlSessionFactory factory;
    private AdminMasterDataService service;
    private final AuditContext admin = new AuditContext("ADMIN",1,"POST","/portal/admin/patients","127.0.0.1");
    @Before public void setup() throws Exception {
        Properties p=new Properties(); p.setProperty("db.driver","org.h2.Driver");
        p.setProperty("db.url","jdbc:h2:mem:admin_"+UUID.randomUUID().toString().replace("-","")+";MODE=MySQL;DB_CLOSE_DELAY=-1");
        p.setProperty("db.username","sa");p.setProperty("db.password","");
        try(InputStream in=Resources.getResourceAsStream("mybatis-config.xml")){factory=new SqlSessionFactoryBuilder().build(in,p);}
        sql("CREATE TABLE departments(department_id INT AUTO_INCREMENT PRIMARY KEY,department_name VARCHAR(100),department_pid INT,department_level INT,department_path VARCHAR(255),department_discription VARCHAR(500))");
        sql("CREATE TABLE professional_titles(id INT AUTO_INCREMENT PRIMARY KEY,title_name VARCHAR(100),description VARCHAR(500))");
        sql("CREATE TABLE doctors(doctor_id INT AUTO_INCREMENT PRIMARY KEY,job_number VARCHAR(50),password VARCHAR(255),password_hash VARCHAR(255),name VARCHAR(100),avatar VARCHAR(255),phone VARCHAR(50),email VARCHAR(255),introduction VARCHAR(2000),registration_fee VARCHAR(20),entry_date VARCHAR(50),department_id INT,professional_title_id INT)");
        sql("CREATE TABLE patients(patient_id INT AUTO_INCREMENT PRIMARY KEY,id_card_number VARCHAR(50),password VARCHAR(255),password_hash VARCHAR(255),name VARCHAR(100),avatar VARCHAR(255),phone VARCHAR(50),email VARCHAR(255),balance DECIMAL(12,2) DEFAULT 0 NOT NULL)");
        sql("CREATE TABLE appointments(appointment_id INT PRIMARY KEY,doctor_id INT,patient_id INT)");
        sql("CREATE TABLE dates(date_id INT PRIMARY KEY,doctor_id INT)");
        sql("CREATE TABLE schedule_slots(schedule_slot_id INT PRIMARY KEY,doctor_id INT)");
        sql("CREATE TABLE medical_records(medical_record_id INT PRIMARY KEY,doctor_id INT,patient_id INT)");
        sql("CREATE TABLE balance_transactions(transaction_id INT PRIMARY KEY,patient_id INT)");
        sql("CREATE TABLE orders(order_id INT PRIMARY KEY,doctor_id INT,patient_id INT)");
        sql("CREATE TABLE audit_logs(audit_log_id BIGINT AUTO_INCREMENT PRIMARY KEY,actor_role VARCHAR(20),actor_id INT,action VARCHAR(80),target_type VARCHAR(80),target_id VARCHAR(80),outcome VARCHAR(20),request_method VARCHAR(10),request_path VARCHAR(255),remote_address VARCHAR(64),created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        sql("INSERT INTO departments VALUES(1,'内科',NULL,1,'/','原科室说明')");
        sql("INSERT INTO professional_titles VALUES(1,'主任医师','职称说明')");
        sql("INSERT INTO doctors(doctor_id,job_number,name,department_id,professional_title_id,registration_fee) VALUES(1,'D001','医生甲',1,1,'10.00')");
        sql("INSERT INTO patients(patient_id,name,id_card_number,balance) VALUES(1,'患者甲','110101199001010011',0),(2,'患者乙','110101199001010022',12.50)");
        service=new AdminMasterDataService(()->factory.openSession(false),()->factory.openSession(true),new AuditService(()->factory.openSession(true)));
    }
    @Test public void nonzeroLegacyBalanceWithoutLedgerCannotBeDeleted() throws Exception {
        assertEquals("HAS_REFERENCES",service.deletePatient(2,admin).getCode());
        assertEquals("12.50",scalar("SELECT balance FROM patients WHERE patient_id=2"));
    }
    @Test public void eachPatientHistoryBlocksDeletion() throws Exception {
        String[] inserts={"INSERT INTO appointments VALUES(1,1,1)","INSERT INTO medical_records VALUES(1,1,1)","INSERT INTO balance_transactions VALUES(1,1)","INSERT INTO orders VALUES(1,1,1)"};
        String[] tables={"appointments","medical_records","balance_transactions","orders"};
        for(int i=0;i<inserts.length;i++) {sql(inserts[i]);assertEquals(tables[i],"HAS_REFERENCES",service.deletePatient(1,admin).getCode());assertEquals("1",scalar("SELECT COUNT(*) FROM patients WHERE patient_id=1"));sql("DELETE FROM "+tables[i]);}
    }
    @Test public void eachDoctorHistoryBlocksDeletion() throws Exception {
        String[] tables={"appointments","dates","schedule_slots","medical_records","orders"};
        String[] inserts={"INSERT INTO appointments VALUES(1,1,1)","INSERT INTO dates VALUES(1,1)","INSERT INTO schedule_slots VALUES(1,1)","INSERT INTO medical_records VALUES(1,1,1)","INSERT INTO orders VALUES(1,1,1)"};
        for(int i=0;i<tables.length;i++){sql(inserts[i]);assertEquals(tables[i],"HAS_REFERENCES",service.deleteDoctor(1,admin).getCode());sql("DELETE FROM "+tables[i]);}
    }
    @Test public void noReferencesDeletesAndAuditsButAuditFailureRollsBack() throws Exception {
        assertTrue(service.deletePatient(1,admin).isSuccessful());assertEquals("0",scalar("SELECT COUNT(*) FROM patients WHERE patient_id=1"));
        assertEquals("1",scalar("SELECT COUNT(*) FROM audit_logs WHERE action='PATIENT_DELETE' AND outcome='SUCCESS'"));
        sql("DROP TABLE audit_logs");assertFalse(service.deleteDoctor(1,admin).isSuccessful());assertEquals("1",scalar("SELECT COUNT(*) FROM doctors WHERE doctor_id=1"));
    }
    @Test public void parentDepartmentAndReferencedTitleCannotBeDeleted() throws Exception {
        assertEquals("HAS_REFERENCES",service.deleteDepartment(1,admin).getCode());
        assertEquals("HAS_REFERENCES",service.deleteProfessionalTitle(1,admin).getCode());
        sql("DELETE FROM doctors");sql("INSERT INTO departments VALUES(2,'呼吸内科',1,2,'/1/','')");
        assertEquals("HAS_REFERENCES",service.deleteDepartment(1,admin).getCode());
    }
    @Test public void administratorPatientInputUsesRegistrationRules() {
        assertFalse(service.createPatient("甲","bad","password123","12345","test@example.com",admin).isSuccessful());
        assertFalse(service.createPatient("患者丙","110101199001010033","password123","bad phone","test@example.com",admin).isSuccessful());
        assertFalse(service.createPatient("患者丙","110101199001010033","password123","12345","bad email",admin).isSuccessful());
        assertFalse(service.updatePatient(1,"not a phone","test@example.com",admin).isSuccessful());
    }
    @Test public void searchPaginatesStablyAndTreatsWildcardLiterally() throws Exception {
        sql("INSERT INTO patients(patient_id,name,id_card_number) VALUES(3,'百分%患者','3'),(4,'患者丙','4')");
        Map<String,Object> first=search("patients","患者",1,2);assertEquals(4L,((Number)first.get("total")).longValue());assertEquals(2,((List<?>)first.get("items")).size());
        Map<String,Object> second=search("patients","患者",2,2);assertEquals(2,((List<?>)second.get("items")).size());
        assertEquals(1L,((Number)search("patients","%",1,10).get("total")).longValue());
        assertEquals(0L,((Number)search("patients","没有",1,10).get("total")).longValue());
    }
    @Test public void optionsIncludeAllDepartmentsAndDoctorKeepsEditableForeignKeys() throws Exception {
        for(int i=2;i<=25;i++)sql("INSERT INTO departments VALUES("+i+",'科室"+i+"',NULL,1,'/','说明')");
        assertEquals(25,((List<?>)service.options("departments").get("items")).size());
        Map<String,Object> result=search("doctors","D001",1,10);
        com.ustb.hospital.entity.Doctors doctor=(com.ustb.hospital.entity.Doctors)((List<?>)result.get("items")).get(0);
        assertEquals(1,doctor.getDepartmentId());assertEquals(1,doctor.getProfessionalTitleId());
        assertEquals(1L,((Number)search("departments","原科室",1,10).get("total")).longValue());
    }
    @Test public void doctorInvalidDateAndContactDoNotChangeSavedData() throws Exception {
        com.ustb.hospital.entity.Doctors d=new com.ustb.hospital.entity.Doctors();d.setDoctorId(1);d.setName("医生甲");d.setDepartmentId(1);d.setProfessionalTitleId(1);d.setRegistrationFee("10.00");d.setEntryDate("2030-02-30");
        assertFalse(service.updateDoctor(d,admin).isSuccessful());d.setEntryDate("2030-02-28");d.setEmail("bad");assertFalse(service.updateDoctor(d,admin).isSuccessful());
        d.setEmail("valid@example.com");assertTrue(service.updateDoctor(d,admin).isSuccessful());
    }
    @Test public void missingObjectsReturnNotFound() {
        assertEquals("NOT_FOUND",service.deleteDoctor(99,admin).getCode());assertEquals("NOT_FOUND",service.deletePatient(99,admin).getCode());
        assertEquals("NOT_FOUND",service.deleteDepartment(99,admin).getCode());assertEquals("NOT_FOUND",service.deleteProfessionalTitle(99,admin).getCode());
    }
    @Test public void childCreationWaitsForParentDeletionAndCannotCreateOrphan() throws Exception {
        sql("DELETE FROM doctors");
        ExecutorService worker=Executors.newSingleThreadExecutor();CountDownLatch entered=new CountDownLatch(1);
        try(SqlSession deleting=factory.openSession(false)) {
            deleting.getConnection().createStatement().executeQuery("SELECT department_id FROM departments WHERE department_id=1 FOR UPDATE").close();
            AdminMasterDataService creating=new AdminMasterDataService(()->{entered.countDown();return factory.openSession(false);},()->factory.openSession(true),new AuditService(()->factory.openSession(true)));
            com.ustb.hospital.entity.Departments child=new com.ustb.hospital.entity.Departments();child.setDepartmentName("并发子科室");child.setDepartmentPid(1);
            Future<com.ustb.hospital.utils.AppointmentOperationResult> result=worker.submit(()->creating.saveDepartment(child,admin));
            assertTrue(entered.await(5,TimeUnit.SECONDS));
            try { result.get(500,TimeUnit.MILLISECONDS);fail("Child insert must wait for the parent transaction."); }
            catch(TimeoutException expected) { }
            deleting.getConnection().createStatement().executeUpdate("DELETE FROM departments WHERE department_id=1");deleting.commit(true);
            assertEquals("NOT_FOUND",result.get(5,TimeUnit.SECONDS).getCode());
            assertEquals("0",scalar("SELECT COUNT(*) FROM departments WHERE department_pid=1"));
        } finally {worker.shutdownNow();}
    }
    @Test public void invalidSearchCannotLoadAnUnboundedPage() throws Exception {
        try{search("patients","",0,1000);fail("invalid page accepted");}catch(IllegalArgumentException expected){}
    }
    @SuppressWarnings("unchecked") private Map<String,Object> search(String type,String keyword,int page,int size)throws Exception {
        try{return (Map<String,Object>)AdminMasterDataService.class.getMethod("search",String.class,String.class,int.class,int.class).invoke(service,type,keyword,page,size);}
        catch(NoSuchMethodException e){throw new AssertionError("missing server-side master-data search",e);}
        catch(InvocationTargetException e){if(e.getCause() instanceof IllegalArgumentException)throw (IllegalArgumentException)e.getCause();throw e;}
    }
    private void sql(String q)throws Exception{try(SqlSession s=factory.openSession(true);Statement st=s.getConnection().createStatement()){st.execute(q);}}
    private String scalar(String q)throws Exception{try(SqlSession s=factory.openSession(true);ResultSet r=s.getConnection().createStatement().executeQuery(q)){assertTrue(r.next());return r.getString(1);}}
}
