package com.ustb.hospital.database;

import com.ustb.hospital.service.*;
import com.ustb.hospital.entity.MedicalOrder;
import com.ustb.hospital.utils.AppointmentOperationResult;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.*;
import org.junit.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.Assert.*;

/** Exercises actual OceanBase locks and the newly initialized schema, after the schema test. */
public class ClinicOceanBaseTest {
 @Test public void concurrencyQueueLeaveAndClinicalCompletionUseRealOceanBaseTransactions()throws Exception{
  Assume.assumeTrue(Boolean.getBoolean("hospital.test.oceanbase.clinic"));
  String name=System.getenv("HOSPITAL_TEST_DB_NAME"),url=System.getenv("HOSPITAL_TEST_DB_URL");
  assertNotNull(name);assertTrue(name.matches("hospital_test_[a-f0-9]{32}"));assertTrue(url.matches("jdbc:oceanbase://127\\.0\\.0\\.1:[0-9]{4,5}/"+name+"\\?.*"));
  assertEquals("hospital_test_runner@test",System.getenv("HOSPITAL_TEST_DB_USERNAME"));
  Properties p=new Properties();p.setProperty("db.driver","com.oceanbase.jdbc.Driver");p.setProperty("db.url",url);p.setProperty("db.username",System.getenv("HOSPITAL_TEST_DB_USERNAME"));p.setProperty("db.password",System.getenv("HOSPITAL_TEST_DB_PASSWORD"));
  SqlSessionFactory f=new SqlSessionFactoryBuilder().build(Resources.getResourceAsReader("mybatis-config.xml"),p);
  assertEquals(name,scalar(f,"SELECT DATABASE()"));assertTrue(scalar(f,"SELECT VERSION()").toLowerCase().contains("oceanbase"));
  String unique=UUID.randomUUID().toString();int dep=insert(f,"INSERT INTO departments(department_name) VALUES('clinic-"+unique+"')");int title=insert(f,"INSERT INTO professional_titles(title_name) VALUES('clinic-"+unique+"')");
  int doctor=insert(f,"INSERT INTO doctors(job_number,name,registration_fee,department_id,professional_title_id) VALUES('clinic-"+unique+"','测试医生',10,"+dep+","+title+")");
  int[] patients=new int[3];for(int i=0;i<3;i++)patients[i]=insert(f,"INSERT INTO patients(name,balance) VALUES('clinic-"+unique+"-"+i+"',100)");
  int slot=insert(f,"INSERT INTO schedule_slots(doctor_id,schedule_date,slot_period,slot_status,capacity) VALUES("+doctor+",'2030-05-06','AM','AVAILABLE',2)");
  ClinicWorkflowService s=new ClinicWorkflowService(()->f.openSession(false),new AuditService(),Clock.fixed(Instant.parse("2030-05-06T01:00:00Z"),ClinicTime.ZONE));
  ExecutorService pool=Executors.newFixedThreadPool(8);try{
   List<Future<AppointmentOperationResult>> results=new ArrayList<>();CountDownLatch start=new CountDownLatch(1);
   for(int patient:patients)results.add(pool.submit(()->{start.await();return s.book(patient,doctor,"2030-05-06","AM",audit("PATIENT",patient));}));start.countDown();
   int successes=0;for(Future<AppointmentOperationResult> r:results)if(r.get(30,TimeUnit.SECONDS).isSuccessful())successes++;assertEquals(2,successes);
   assertEquals("2",scalar(f,"SELECT occupied_count FROM schedule_slots WHERE schedule_slot_id="+slot));
   List<Map<String,Object>> queue=s.queue("DOCTOR",doctor,"2030-05-06","AM");for(Map<String,Object>a:queue)ok(s.queueAction("check-in","PATIENT",((Number)a.get("patientId")).intValue(),((Number)a.get("appointmentId")).intValue(),null,null,audit("PATIENT",((Number)a.get("patientId")).intValue())));
   results.clear();for(int i=0;i<8;i++)results.add(pool.submit(()->s.queueAction("call-next","DOCTOR",doctor,0,"2030-05-06","AM",audit("DOCTOR",doctor))));successes=0;for(Future<AppointmentOperationResult>r:results)if(r.get(30,TimeUnit.SECONDS).isSuccessful())successes++;assertEquals(1,successes);
   int called=Integer.parseInt(scalar(f,"SELECT appointment_id FROM appointments WHERE doctor_id="+doctor+" AND queue_status='CALLED'"));ok(s.queueAction("start","DOCTOR",doctor,called,null,null,audit("DOCTOR",doctor)));
   MedicalRecordsServiceImpl records=new MedicalRecordsServiceImpl(()->f.openSession(false),()->f.openSession(true),new AuditService());assertTrue(records.finalizeEncounter(doctor,called,"测试诊断","测试诊疗方案","休息",Collections.<MedicalOrder>emptyList()).isSuccessful());
   assertEquals("DONE",scalar(f,"SELECT queue_status FROM appointments WHERE appointment_id="+called));assertEquals("1",scalar(f,"SELECT COUNT(*) FROM notifications WHERE event_key='appointment:"+called+":FINAL'"));
   ok(s.leaveAction("apply","DOCTOR",doctor,0,"2030-05-06","2030-05-06","AM","请假",audit("DOCTOR",doctor)));
   long leave=Long.parseLong(scalar(f,"SELECT leave_id FROM clinic_leave WHERE doctor_id="+doctor));ok(s.leaveAction("approve","ADMIN",1,leave,null,null,null,"同意",audit("ADMIN",1)));
   assertEquals("1",scalar(f,"SELECT COUNT(*) FROM appointments WHERE doctor_id="+doctor+" AND status='BOOKED'"));int unused=Integer.parseInt(scalar(f,"SELECT patient_id FROM patients WHERE name LIKE 'clinic-"+unique+"-%' AND balance=100 LIMIT 1"));assertEquals("SLOT_UNAVAILABLE",s.book(unused,doctor,"2030-05-06","AM",audit("PATIENT",unused)).getCode());
   assertEquals("1",scalar(f,"SELECT COUNT(*) FROM notifications WHERE notification_type='LEAVE_APPROVED' AND target_id IN (SELECT CAST(appointment_id AS CHAR) FROM appointments WHERE doctor_id="+doctor+" AND status='BOOKED') AND recipient_role='PATIENT'"));
  }finally{pool.shutdownNow();}
 }
 static AuditContext audit(String r,int id){return new AuditContext(r,id,"POST","/clinic/test",null);}
 static void ok(AppointmentOperationResult r){assertTrue(r.getCode()+": "+r.getMessage(),r.isSuccessful());}
 static int insert(SqlSessionFactory f,String sql)throws Exception{try(SqlSession s=f.openSession(true);Statement st=s.getConnection().createStatement()){st.executeUpdate(sql,Statement.RETURN_GENERATED_KEYS);try(ResultSet r=st.getGeneratedKeys()){r.next();return r.getInt(1);}}}
 static String scalar(SqlSessionFactory f,String sql)throws Exception{try(SqlSession s=f.openSession(true);Statement st=s.getConnection().createStatement();ResultSet r=st.executeQuery(sql)){r.next();return r.getString(1);}}
}
