package com.ustb.hospital.service;

import com.ustb.hospital.security.PasswordService;
import com.ustb.hospital.utils.AppointmentOperationResult;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.*;
import org.junit.*;
import java.io.*;
import java.lang.reflect.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import javax.servlet.http.*;
import java.util.concurrent.atomic.AtomicBoolean;
import com.ustb.hospital.filter.AuthenticationFilter;
import com.ustb.hospital.utils.SessionIdentity;
import static org.junit.Assert.*;

public class AccountSecurityServiceTest {
    private SqlSessionFactory factory;
    private Clock clock = Clock.fixed(Instant.parse("2026-10-28T00:00:00Z"), ZoneOffset.UTC);
    private final AuditContext admin = new AuditContext("ADMIN",1,"POST","/portal/admin/account-security/disable","127.0.0.1");
    @Before public void setup() throws Exception {
        Properties p = new Properties(); p.setProperty("db.driver","org.h2.Driver");
        p.setProperty("db.url","jdbc:h2:mem:accounts_"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=20000"); p.setProperty("db.username","sa"); p.setProperty("db.password","");
        try(InputStream in=Resources.getResourceAsStream("mybatis-config.xml")){factory=new SqlSessionFactoryBuilder().build(in,p);}
        sql("CREATE TABLE patients(patient_id INT PRIMARY KEY,name VARCHAR(100),password VARCHAR(255),password_hash VARCHAR(60))",
            "CREATE TABLE doctors(doctor_id INT PRIMARY KEY,job_number VARCHAR(50),name VARCHAR(100),password VARCHAR(255),password_hash VARCHAR(60))",
            "CREATE TABLE admins(admin_id INT PRIMARY KEY,username VARCHAR(100),password VARCHAR(255),password_hash VARCHAR(60))",
            "INSERT INTO patients VALUES(1,'patient','old-password',NULL)","INSERT INTO doctors VALUES(1,'doctor','Doctor','old-password',NULL)","INSERT INTO admins VALUES(1,'admin','old-password',NULL)",
            "CREATE TABLE account_security(account_role VARCHAR(20),account_id INT,enabled BOOLEAN DEFAULT TRUE,session_version BIGINT DEFAULT 0,must_change_password BOOLEAN DEFAULT FALSE,failed_attempts INT DEFAULT 0,failure_window_start TIMESTAMP NULL,locked_until TIMESTAMP NULL,PRIMARY KEY(account_role,account_id))",
            "CREATE TABLE login_throttle(source_ip VARCHAR(64) PRIMARY KEY,attempts INT DEFAULT 0,window_start TIMESTAMP NOT NULL)",
            "CREATE TABLE appointments(appointment_id INT PRIMARY KEY,doctor_id INT,patient_id INT,status VARCHAR(20))",
            "CREATE TABLE notifications(notification_id BIGINT AUTO_INCREMENT PRIMARY KEY,recipient_role VARCHAR(20),recipient_id INT,event_key VARCHAR(191),notification_type VARCHAR(40),title VARCHAR(200),body VARCHAR(2000),target_type VARCHAR(40),target_id VARCHAR(100),created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,read_at TIMESTAMP NULL,UNIQUE(recipient_role,recipient_id,event_key))",
            "CREATE TABLE audit_logs(audit_log_id BIGINT AUTO_INCREMENT PRIMARY KEY,actor_role VARCHAR(20),actor_id INT,action VARCHAR(80),target_type VARCHAR(80),target_id VARCHAR(80),outcome VARCHAR(20),request_method VARCHAR(10),request_path VARCHAR(255),remote_address VARCHAR(64),created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
    }
    private Object service() throws Exception { Class<?> c; try{c=Class.forName("com.ustb.hospital.service.AccountSecurityService");}catch(ClassNotFoundException e){throw new AssertionError("Account security service must enforce persisted access controls",e);} return c.getConstructor(Supplier.class,AuditService.class,Clock.class).newInstance((Supplier<SqlSession>)()->factory.openSession(false),new AuditService(()->factory.openSession(true)),clock); }
    private Object call(Object service,String name,Class<?>[] types,Object...args) throws Exception {try{return service.getClass().getMethod(name,types).invoke(service,args);}catch(InvocationTargetException e){throw new AssertionError(e.getCause());}}
    private Object login(String role,String name,String pass,String ip) throws Exception {return call(service(),"authenticate",new Class[]{String.class,String.class,String.class,String.class},role,name,pass,ip);}
    private boolean success(Object login) throws Exception{return (Boolean)login.getClass().getMethod("isSuccessful").invoke(login);}
    private AppointmentOperationResult enable(boolean value) throws Exception {return (AppointmentOperationResult)call(service(),"setEnabled",new Class[]{String.class,int.class,boolean.class,AuditContext.class},"PATIENT",1,value,admin);}
    @Test public void disablingRevokesOldSessionAndReenableDoesNotResurrectIt() throws Exception {
        assertTrue(success(login("PATIENT","patient","old-password","127.0.0.1")));
        assertTrue(enable(false).isSuccessful()); assertFalse(success(login("PATIENT","patient","old-password","127.0.0.1")));
        assertEquals("1",scalar("SELECT session_version FROM account_security WHERE account_role='PATIENT'"));
        assertTrue(enable(true).isSuccessful());assertEquals("1",scalar("SELECT session_version FROM account_security WHERE account_role='PATIENT'"));
    }
    @Test public void fiveConcurrentFailuresLockAccountAndExpiryAllowsLogin() throws Exception {
        Object svc=service(); ExecutorService pool=Executors.newFixedThreadPool(5); List<Future<?>> jobs=new ArrayList<>();
        try{for(int n=0;n<5;n++){final int i=n;jobs.add(pool.submit(()->{try{assertFalse(success(call(svc,"authenticate",new Class[]{String.class,String.class,String.class,String.class},"PATIENT","patient","wrong","127.0.0."+i)));}catch(Exception e){throw new RuntimeException(e);}}));}for(Future<?> f:jobs)f.get();}finally{pool.shutdownNow();}
        assertEquals("5",scalar("SELECT failed_attempts FROM account_security WHERE account_role='PATIENT'"));
        assertFalse(success(login("PATIENT","patient","old-password","127.0.0.8")));
        clock=Clock.fixed(clock.instant().plusSeconds(901),ZoneOffset.UTC);assertTrue(success(login("PATIENT","patient","old-password","127.0.0.8")));
        assertEquals("0",scalar("SELECT failed_attempts FROM account_security WHERE account_role='PATIENT'"));
    }
    @Test public void resetHashesTemporaryPasswordRequiresChangeAndKeepsDisabled() throws Exception {
        assertTrue(enable(false).isSuccessful());
        AppointmentOperationResult result=(AppointmentOperationResult)call(service(),"resetPassword",new Class[]{String.class,int.class,String.class,AuditContext.class},"PATIENT",1,"temporary-password",admin);
        assertTrue(result.isSuccessful());assertNull(scalar("SELECT password FROM patients"));assertTrue(PasswordService.matches("temporary-password",scalar("SELECT password_hash FROM patients"),null));
        assertEquals("FALSE",scalar("SELECT enabled FROM account_security"));assertEquals("TRUE",scalar("SELECT must_change_password FROM account_security"));
        assertEquals("2",scalar("SELECT session_version FROM account_security"));assertTrue(enable(true).isSuccessful());assertTrue(success(login("PATIENT","patient","temporary-password","127.0.0.1")));
    }
    @Test public void sourceThrottleAlsoLimitsUnknownAccounts() throws Exception {
        for(int i=0;i<61;i++)assertFalse(success(login("PATIENT","missing"+i,"bad","127.0.0.1")));
        assertFalse(success(login("PATIENT","patient","old-password","127.0.0.1")));assertTrue(success(login("PATIENT","patient","old-password","127.0.0.2")));
    }
    @Test public void administrativeWritesRequireAdminAndAuditFailureRollsBack() throws Exception {
        AppointmentOperationResult r=(AppointmentOperationResult)call(service(),"setEnabled",new Class[]{String.class,int.class,boolean.class,AuditContext.class},"PATIENT",1,false,new AuditContext("PATIENT",1,"POST","/x","127.0.0.1")); assertFalse(r.isSuccessful());
        sql("DROP TABLE audit_logs");assertFalse(enable(false).isSuccessful());assertEquals("0",scalar("SELECT COUNT(*) FROM account_security WHERE enabled=FALSE"));
    }
    @Test public void utf8PasswordLimitPreventsBcryptTruncation() {
        try{PasswordService.validateNewPassword(String.join("",Collections.nCopies(25,"密")));fail("75 UTF-8 bytes must be rejected");}catch(IllegalArgumentException expected){}
        PasswordService.validateNewPassword(String.join("",Collections.nCopies(24,"密")));
    }
    @Test public void selfPasswordChangeClearsTemporaryRequirementAndRevokesAllOldVersions()throws Exception {
        Object svc=service();assertTrue(success(login("PATIENT","patient","old-password","127.0.0.1")));
        assertNotNull(call(svc,"validateSession",new Class[]{String.class,int.class,long.class},"PATIENT",1,0L));
        call(svc,"resetPassword",new Class[]{String.class,int.class,String.class,AuditContext.class},"PATIENT",1,"temporary-password",admin);
        assertNull(call(svc,"validateSession",new Class[]{String.class,int.class,long.class},"PATIENT",1,0L));
        PasswordChangeService change=new PasswordChangeService(()->factory.openSession(false),new AuditService(()->factory.openSession(true)));
        assertTrue(change.change("PATIENT",1,"temporary-password","replacement-password",new AuditContext("PATIENT",1,"POST","/password/reset","127.0.0.1")).isSuccessful());
        assertNull(call(svc,"validateSession",new Class[]{String.class,int.class,long.class},"PATIENT",1,1L));
        assertEquals("FALSE",scalar("SELECT must_change_password FROM account_security WHERE account_role='PATIENT'"));
        assertTrue(success(login("PATIENT","patient","replacement-password","127.0.0.2")));
    }
    @Test public void disabledAccountCannotChangePasswordAndUnlockAllowsRetry()throws Exception {
        for(int i=0;i<5;i++)assertFalse(success(login("PATIENT","patient","wrong","127.0.0.1")));
        assertFalse(success(login("PATIENT","patient","old-password","127.0.0.2")));
        assertTrue(((AppointmentOperationResult)call(service(),"unlock",new Class[]{String.class,int.class,AuditContext.class},"PATIENT",1,admin)).isSuccessful());
        assertTrue(success(login("PATIENT","patient","old-password","127.0.0.2")));assertTrue(enable(false).isSuccessful());
        PasswordChangeService change=new PasswordChangeService(()->factory.openSession(false),new AuditService(()->factory.openSession(true)));
        assertFalse(change.change("PATIENT",1,"old-password","replacement-password",new AuditContext("PATIENT",1,"POST","/password/reset","127.0.0.1")).isSuccessful());
        assertTrue(PasswordService.matches("old-password",scalar("SELECT password_hash FROM patients"),scalar("SELECT password FROM patients")));
    }
    @Test public void filterEnforcesTemporaryPasswordAndRevokedVersionsOnEveryRequest()throws Exception {
        AccountSecurityService svc=(AccountSecurityService)service();
        assertTrue(svc.resetPassword("PATIENT",1,"temporary-password",admin).isSuccessful());
        assertEquals(403,filtered(svc,"/appointments/book",1L,false));
        assertEquals(200,filtered(svc,"/password/reset",1L,true));
        assertEquals(200,filtered(svc,"/security/csrf",1L,true));
        assertEquals(200,filtered(svc,"/auth/logout",1L,true));
        assertTrue(enable(false).isSuccessful());assertEquals(401,filtered(svc,"/appointments/book",1L,false));
        assertTrue(enable(true).isSuccessful());assertEquals(401,filtered(svc,"/appointments/book",1L,false));
    }
    @Test public void disablingDoctorPreservesAppointmentsAndNotifiesOnlyPendingPatientsOnce()throws Exception {
        sql("INSERT INTO appointments VALUES(1,1,2,'BOOKED'),(2,1,3,'IN_PROGRESS'),(3,1,4,'COMPLETED')");
        AccountSecurityService svc=(AccountSecurityService)service();assertTrue(svc.setEnabled("DOCTOR",1,false,admin).isSuccessful());
        assertEquals("1",scalar("SELECT COUNT(*) FROM notifications"));assertEquals("2",scalar("SELECT recipient_id FROM notifications"));
        assertEquals("BOOKED",scalar("SELECT status FROM appointments WHERE appointment_id=1"));assertEquals("3",scalar("SELECT COUNT(*) FROM appointments"));
        assertTrue(svc.setEnabled("DOCTOR",1,false,admin).isSuccessful());assertEquals("1",scalar("SELECT COUNT(*) FROM notifications"));
        try(SqlSession s=factory.openSession(false)){assertFalse(AccountSecurityService.isEnabled(s,"DOCTOR",1));}
    }
    @Test public void doctorDisableNotificationFailureRollsBackAccountStatus()throws Exception {
        sql("INSERT INTO appointments VALUES(1,1,2,'BOOKED')","DROP TABLE notifications");
        AccountSecurityService svc=(AccountSecurityService)service();assertFalse(svc.setEnabled("DOCTOR",1,false,admin).isSuccessful());
        assertEquals("0",scalar("SELECT COUNT(*) FROM account_security WHERE enabled=FALSE"));
        assertEquals("BOOKED",scalar("SELECT status FROM appointments WHERE appointment_id=1"));
    }
    private int filtered(AccountSecurityService svc,String path,long version,boolean expectedChain)throws Exception {
        Map<String,Object> attributes=new HashMap<>();AtomicBoolean invalidated=new AtomicBoolean(),chained=new AtomicBoolean();
        HttpSession session=(HttpSession)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{HttpSession.class},(p,m,a)->{
            if("getAttribute".equals(m.getName()))return attributes.get(a[0]);if("setAttribute".equals(m.getName())){attributes.put((String)a[0],a[1]);return null;}if("removeAttribute".equals(m.getName())){attributes.remove(a[0]);return null;}if("invalidate".equals(m.getName())){invalidated.set(true);attributes.clear();return null;}return primitive(m.getReturnType());});
        SessionIdentity.establish(session,"PATIENT",1,"Patient",version,true);
        HttpServletRequest req=(HttpServletRequest)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{HttpServletRequest.class},(p,m,a)->{
            if("getSession".equals(m.getName()))return session;if("getServletPath".equals(m.getName()))return path;if("getMethod".equals(m.getName()))return "POST";if("getRemoteAddr".equals(m.getName()))return "127.0.0.1";return primitive(m.getReturnType());});
        int[] status={200};HttpServletResponse resp=(HttpServletResponse)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{HttpServletResponse.class},(p,m,a)->{if("setStatus".equals(m.getName())){status[0]=(Integer)a[0];return null;}if("getWriter".equals(m.getName()))return new PrintWriter(new StringWriter());return primitive(m.getReturnType());});
        new AuthenticationFilter(new AuditService(()->factory.openSession(true)),svc).doFilter(req,resp,(r,s)->chained.set(true));assertEquals(expectedChain,chained.get());if(status[0]==401)assertTrue(invalidated.get());return status[0];
    }
    private static Object primitive(Class<?> type){if(!type.isPrimitive())return null;if(type==boolean.class)return false;if(type==long.class)return 0L;return 0;}
    private void sql(String... statements)throws Exception{try(SqlSession s=factory.openSession(true);Statement st=s.getConnection().createStatement()){for(String q:statements)st.execute(q);}}
    private String scalar(String q)throws Exception{try(SqlSession s=factory.openSession(true);ResultSet r=s.getConnection().createStatement().executeQuery(q)){assertTrue(r.next());return r.getString(1);}}
}
