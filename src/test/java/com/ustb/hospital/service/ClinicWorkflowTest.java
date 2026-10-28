package com.ustb.hospital.service;

import com.ustb.hospital.utils.AppointmentOperationResult;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.*;
import org.junit.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.function.Supplier;
import static org.junit.Assert.*;

/** Actual SQL transactions against a disposable database, never business data. */
public class ClinicWorkflowTest {
    SqlSessionFactory factory; Object service;
    final String day="2030-05-06";
    @Before public void setup() throws Exception {
        Properties p=new Properties(); p.setProperty("db.driver","org.h2.Driver");
        p.setProperty("db.url","jdbc:h2:mem:clinic_"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000");
        p.setProperty("db.username","sa");p.setProperty("db.password","");
        factory=new SqlSessionFactoryBuilder().build(Resources.getResourceAsReader("mybatis-config.xml"),p);
        sql("CREATE TABLE doctors(doctor_id INT PRIMARY KEY,name VARCHAR(100),registration_fee DECIMAL(12,2))");
        sql("CREATE TABLE patients(patient_id INT PRIMARY KEY,name VARCHAR(100),balance DECIMAL(12,2))");
        sql("CREATE TABLE admins(admin_id INT PRIMARY KEY)");
        sql("INSERT INTO doctors VALUES(1,'医生',10); INSERT INTO patients VALUES(1,'患者1',100),(2,'患者2',100),(3,'患者3',100); INSERT INTO admins VALUES(1)");
        sql("CREATE TABLE account_security(account_role VARCHAR(20),account_id INT,enabled BOOLEAN DEFAULT TRUE,session_version BIGINT DEFAULT 0,must_change_password BOOLEAN DEFAULT FALSE,failed_attempts INT DEFAULT 0,failure_window_start TIMESTAMP,locked_until TIMESTAMP,PRIMARY KEY(account_role,account_id))");
        sql("CREATE TABLE schedule_slots(schedule_slot_id BIGINT AUTO_INCREMENT PRIMARY KEY,doctor_id INT,schedule_date DATE,slot_period VARCHAR(20),slot_status VARCHAR(20),capacity INT DEFAULT 10,occupied_count INT DEFAULT 0,next_sequence INT DEFAULT 0,created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,UNIQUE(doctor_id,schedule_date,slot_period))");
        sql("INSERT INTO schedule_slots(doctor_id,schedule_date,slot_period,slot_status,capacity) VALUES(1,'"+day+"','AM','AVAILABLE',2),(1,'2030-05-07','AM','AVAILABLE',1)");
        sql("CREATE TABLE appointments(appointment_id INT AUTO_INCREMENT PRIMARY KEY,patient_id INT,doctor_id INT,appointment_date DATE,appointment_time VARCHAR(20),schedule_slot_id BIGINT,previous_appointment_id INT,status VARCHAR(20),appointment_sequence INT,queue_status VARCHAR(25) DEFAULT 'NOT_CHECKED_IN',checked_in_at TIMESTAMP,called_at TIMESTAMP,started_at TIMESTAMP,completed_at TIMESTAMP,cancelled_at TIMESTAMP,updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,version INT DEFAULT 0,UNIQUE(schedule_slot_id,appointment_sequence))");
        sql("CREATE TABLE balance_transactions(transaction_id BIGINT AUTO_INCREMENT PRIMARY KEY,patient_id INT,appointment_id INT,transaction_type VARCHAR(40),amount DECIMAL(12,2) CHECK(amount<>0),balance_after DECIMAL(12,2),note VARCHAR(255))");
        sql("CREATE TABLE audit_logs(audit_log_id BIGINT AUTO_INCREMENT PRIMARY KEY,actor_role VARCHAR(20),actor_id INT,action VARCHAR(80),target_type VARCHAR(80),target_id VARCHAR(80),outcome VARCHAR(20),request_method VARCHAR(10),request_path VARCHAR(255),remote_address VARCHAR(64),created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        sql("CREATE TABLE notifications(id BIGINT AUTO_INCREMENT PRIMARY KEY,recipient_role VARCHAR(20),recipient_id INT,event_key VARCHAR(191),notification_type VARCHAR(40),title VARCHAR(200),body VARCHAR(2000),target_type VARCHAR(40),target_id VARCHAR(100),created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,read_at TIMESTAMP,UNIQUE(event_key,recipient_role,recipient_id))");
        sql("CREATE TABLE clinic_leave(leave_id BIGINT AUTO_INCREMENT PRIMARY KEY,doctor_id INT,start_date DATE,end_date DATE,periods VARCHAR(40),reason VARCHAR(500),status VARCHAR(20),review_note VARCHAR(500),created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        sql("CREATE TABLE queue_history(id BIGINT AUTO_INCREMENT PRIMARY KEY,appointment_id INT,from_status VARCHAR(25),to_status VARCHAR(25),actor_role VARCHAR(20),actor_id INT,created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        Class<?> type;try{type=Class.forName("com.ustb.hospital.service.ClinicWorkflowService");}catch(ClassNotFoundException e){throw new AssertionError("多号源、停诊和候诊事务服务尚未实现",e);}
        service=type.getConstructor(Supplier.class,AuditService.class,Clock.class).newInstance((Supplier<SqlSession>)()->factory.openSession(false),new AuditService(),Clock.fixed(Instant.parse("2030-05-06T01:00:00Z"),ZoneId.of("Asia/Shanghai")));
    }
    @Test public void capacitySequenceCancellationAndZeroFee() throws Exception {
        ok(book(1,day)); ok(book(2,day)); assertEquals("SLOT_UNAVAILABLE",book(3,day).getCode());
        assertEquals("DUPLICATE_BOOKING",book(1,day).getCode());ok(action("cancel",1,1,null,null));ok(book(3,day));
        assertEquals("3",scalar("SELECT MAX(appointment_sequence) FROM appointments")); assertEquals("2",scalar("SELECT occupied_count FROM schedule_slots WHERE schedule_slot_id=1"));
        sql("UPDATE doctors SET registration_fee=0");ok(book(1,"2030-05-07"));assertEquals("3",scalar("SELECT COUNT(*) FROM balance_transactions"));
    }
    @Test public void notificationsFailureRollsBackMoneyAndQuota() throws Exception {
        sql("DROP TABLE notifications");assertFalse(book(1,day).isSuccessful());assertEquals("100.00",scalar("SELECT balance FROM patients WHERE patient_id=1"));assertEquals("0",scalar("SELECT occupied_count FROM schedule_slots WHERE schedule_slot_id=1"));
    }
    @Test public void expiredDatesAndDisabledDoctorsRejectBookings() throws Exception {
        assertEquals("INVALID_REQUEST",book(1,"2030-02-30").getCode());assertEquals("SLOT_UNAVAILABLE",book(1,"2030-05-05").getCode());
        sql("INSERT INTO account_security(account_role,account_id,enabled) VALUES('DOCTOR',1,FALSE)");assertEquals("ACCOUNT_DISABLED",book(1,day).getCode());
    }
    @Test public void leaveKeepsAppointmentsAndPreventsNewBookingThenRevokes() throws Exception {
        ok(book(1,day));ok(leave("apply","DOCTOR",1,0,day,day,"AM","请假"));
        ok(leave("approve","ADMIN",1,1,null,null,null,"同意")); assertEquals("BOOKED",scalar("SELECT status FROM appointments"));
        assertEquals("SLOT_UNAVAILABLE",book(2,day).getCode());assertEquals("1",scalar("SELECT COUNT(*) FROM notifications WHERE notification_type='LEAVE_APPROVED' AND recipient_role='PATIENT'"));
        ok(leave("revoke","ADMIN",1,1,null,null,null,"恢复"));ok(book(2,day));
    }
    @Test public void checkinCallSkipRequeueAndStartAreScoped() throws Exception {
        ok(book(1,day));ok(book(2,day));assertFalse(queue("start","DOCTOR",1,1).isSuccessful());
        assertEquals("FORBIDDEN",queue("check-in","PATIENT",2,1).getCode());ok(queue("check-in","PATIENT",1,1));ok(queue("check-in","PATIENT",2,2));
        ok(queue("call-next","DOCTOR",1,0));assertEquals("DOCTOR_BUSY",queue("call-next","DOCTOR",1,0).getCode());
        ok(queue("skip","DOCTOR",1,1));ok(queue("call-next","DOCTOR",1,0));ok(queue("requeue","DOCTOR",1,1));ok(queue("start","DOCTOR",1,2));
        assertEquals("IN_PROGRESS",scalar("SELECT status FROM appointments WHERE appointment_id=2"));assertEquals("WAITING",scalar("SELECT queue_status FROM appointments WHERE appointment_id=1"));
        ok(leave("apply","DOCTOR",1,0,day,day,"AM","请假"));assertEquals("IN_PROGRESS_EXISTS",leave("approve","ADMIN",1,1,null,null,null,"同意").getCode());
    }
    @Test public void rescheduleFailureKeepsOriginalAndClosedSlotStaysClosed() throws Exception {
        ok(book(1,day));ok(book(2,"2030-05-07"));assertFalse(action("reschedule",1,1,"2030-05-07","AM").isSuccessful());assertEquals("BOOKED",scalar("SELECT status FROM appointments WHERE appointment_id=1"));
        sql("UPDATE schedule_slots SET slot_status='CLOSED' WHERE schedule_slot_id=1");ok(action("cancel",1,1,null,null));assertEquals("CLOSED",scalar("SELECT slot_status FROM schedule_slots WHERE schedule_slot_id=1"));
    }
    @Test public void concurrentBookingDoesNotOversell() throws Exception {
        java.util.concurrent.ExecutorService pool=java.util.concurrent.Executors.newFixedThreadPool(3);try{
            java.util.List<java.util.concurrent.Future<AppointmentOperationResult>> results=new ArrayList<>();
            for(int n=1;n<=3;n++){final int id=n;results.add(pool.submit(()->book(id,day)));}
            int success=0;for(java.util.concurrent.Future<AppointmentOperationResult> r:results)if(r.get().isSuccessful())success++;
            assertEquals(2,success);assertEquals("2",scalar("SELECT COUNT(*) FROM appointments"));
        }finally{pool.shutdownNow();}
    }
    AppointmentOperationResult book(int p,String d)throws Exception{return (AppointmentOperationResult)service.getClass().getMethod("book",int.class,int.class,String.class,String.class,AuditContext.class).invoke(service,p,1,d,"AM",audit("PATIENT",p));}
    AppointmentOperationResult action(String a,int p,int id,String date,String period)throws Exception{return (AppointmentOperationResult)service.getClass().getMethod("patientAction",String.class,int.class,int.class,String.class,String.class,AuditContext.class).invoke(service,a,p,id,date,period,audit("PATIENT",p));}
    AppointmentOperationResult queue(String a,String role,int who,int id)throws Exception{return (AppointmentOperationResult)service.getClass().getMethod("queueAction",String.class,String.class,int.class,int.class,String.class,String.class,AuditContext.class).invoke(service,a,role,who,id,day,"AM",audit(role,who));}
    AppointmentOperationResult leave(String a,String role,int who,long id,String from,String to,String periods,String reason)throws Exception{return (AppointmentOperationResult)service.getClass().getMethod("leaveAction",String.class,String.class,int.class,long.class,String.class,String.class,String.class,String.class,AuditContext.class).invoke(service,a,role,who,id,from,to,periods,reason,audit(role,who));}
    AuditContext audit(String r,int id){return new AuditContext(r,id,"POST","/clinic/test",null);}
    void ok(AppointmentOperationResult r){assertTrue(r.getCode()+": "+r.getMessage(),r.isSuccessful());}
    void sql(String text)throws Exception{try(SqlSession s=factory.openSession(true);Statement st=s.getConnection().createStatement()){st.execute(text);}}
    String scalar(String text)throws Exception{try(SqlSession s=factory.openSession(true);Statement st=s.getConnection().createStatement();ResultSet r=st.executeQuery(text)){r.next();return r.getString(1);}}
}
