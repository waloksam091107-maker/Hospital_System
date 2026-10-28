package com.ustb.hospital.servlet;

import com.ustb.hospital.utils.SessionIdentity;
import com.ustb.hospital.service.NotificationService;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.*;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.Test;
import javax.servlet.http.*;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.*;
import java.util.*;
import java.sql.Statement;
import static org.junit.Assert.*;

public class NotificationServletTest {
    @Test public void unauthenticatedRequestReturnsJson401BeforeDatabaseAccess() throws Exception {
        Exchange exchange=new Exchange(null,"/unread-count",Collections.emptyMap());
        exchange.call("doGet"); assertEquals(401,exchange.status); assertTrue(exchange.output.toString().contains("LOGIN_REQUIRED"));
    }
    @Test public void malformedPageIsRejectedBeforeDatabaseAccess() throws Exception {
        Map<String,String> params=new HashMap<>(); params.put("page","-1");
        Exchange exchange=new Exchange("PATIENT","/list",params);
        exchange.call("doGet"); assertEquals(400,exchange.status); assertTrue(exchange.output.toString().contains("INVALID_REQUEST"));
    }
    @Test public void readOperationsRejectGetRequests() throws Exception {
        Exchange exchange=new Exchange("PATIENT","/read-all",Collections.emptyMap());
        exchange.call("doGet"); assertEquals(405,exchange.status);
    }
    @Test public void malformedReadIdIsRejectedBeforeDatabaseAccess() throws Exception {
        Exchange exchange=new Exchange("PATIENT","/read",Collections.singletonMap("id","-2"));
        exchange.call("doPost"); assertEquals(400,exchange.status);
    }
    @Test public void clientCannotReadOrMarkAnotherRecipientsMessages() throws Exception {
        UnpooledDataSource ds=new UnpooledDataSource("org.h2.Driver","jdbc:h2:mem:notification_http_"+System.nanoTime()+";MODE=MySQL;DB_CLOSE_DELAY=-1","sa","");
        SqlSessionFactory factory=new SqlSessionFactoryBuilder().build(new Configuration(new Environment("test",new JdbcTransactionFactory(),ds)));
        try(SqlSession session=factory.openSession(true); Statement statement=session.getConnection().createStatement()) {
            statement.execute("CREATE TABLE notifications (id BIGINT AUTO_INCREMENT PRIMARY KEY,recipient_role VARCHAR(16),recipient_id INT,event_key VARCHAR(191),notification_type VARCHAR(40),title VARCHAR(200),body VARCHAR(2000),target_type VARCHAR(40),target_id VARCHAR(100),created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,read_at TIMESTAMP,UNIQUE(event_key,recipient_role,recipient_id))");
            NotificationService.enqueue(session,"PATIENT",7,"mine","BOOKING","本人通知","本人内容","APPOINTMENT","1");
            NotificationService.enqueue(session,"PATIENT",8,"others","BOOKING","他人通知","他人内容","APPOINTMENT","2");
        }
        NotificationService service=new NotificationService(()->factory.openSession(false),()->factory.openSession(true));
        Map<String,String> params=new HashMap<>(); params.put("recipientId","8");params.put("role","DOCTOR");
        Exchange list=new Exchange("PATIENT","/list",params); list.call("doGet",service);
        assertEquals(200,list.status); assertTrue(list.output.toString().contains("本人通知")); assertFalse(list.output.toString().contains("他人通知"));
        params.put("id","2"); Exchange read=new Exchange("PATIENT","/read",params); read.call("doPost",service);
        assertEquals(404,read.status); assertEquals(1L,service.countUnread("PATIENT",8));
        Exchange all=new Exchange("PATIENT","/read-all",params); all.call("doPost",service);
        assertEquals(200,all.status); assertEquals(0L,service.countUnread("PATIENT",7)); assertEquals(1L,service.countUnread("PATIENT",8));
    }
    private static class Exchange {
        final StringWriter output=new StringWriter(); int status=200;
        final HttpServletRequest request; final HttpServletResponse response;
        Exchange(String role,String path,Map<String,String> params) {
            Map<String,Object> attributes=new HashMap<>();
            if(role!=null) { attributes.put(SessionIdentity.CURRENT_USER_ROLE,role); attributes.put(SessionIdentity.CURRENT_PATIENT_ID,7); }
            HttpSession session=proxy(HttpSession.class,(p,m,a)->"getAttribute".equals(m.getName())?attributes.get(a[0]):defaultValue(m.getReturnType()));
            request=proxy(HttpServletRequest.class,(p,m,a)->{
                if("getSession".equals(m.getName())) return role==null?null:session;
                if("getPathInfo".equals(m.getName())) return path;
                if("getParameter".equals(m.getName())) return params.get(a[0]);
                return defaultValue(m.getReturnType());
            });
            response=proxy(HttpServletResponse.class,(p,m,a)->{
                if("setStatus".equals(m.getName())) status=(Integer)a[0];
                if("getWriter".equals(m.getName())) return new PrintWriter(output);
                return defaultValue(m.getReturnType());
            });
        }
        void call(String name) throws Exception {
            call(name,null);
        }
        void call(String name,NotificationService service) throws Exception {
            Class<?> type;
            try { type=Class.forName("com.ustb.hospital.servlet.NotificationServlet"); }
            catch(ClassNotFoundException e) { throw new AssertionError("NotificationServlet must expose protected own-notification APIs",e); }
            Object servlet=service==null?type.newInstance():type.getConstructor(NotificationService.class).newInstance(service);
            Method method=type.getDeclaredMethod(name,HttpServletRequest.class,HttpServletResponse.class); method.setAccessible(true); method.invoke(servlet,request,response);
        }
        @SuppressWarnings("unchecked") static <T>T proxy(Class<T> type,InvocationHandler handler) { return (T)Proxy.newProxyInstance(type.getClassLoader(),new Class[]{type},handler); }
        static Object defaultValue(Class<?> type) { if(type==boolean.class)return false; if(type==int.class)return 0; if(type==long.class)return 0L; return null; }
    }
}
