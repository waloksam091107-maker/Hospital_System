package com.ustb.hospital.service;

import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.junit.Assert.*;

/** Real sessions catch accidental autonomous commits and recipient-scope mistakes. */
public class NotificationServiceTest {
    private SqlSessionFactory factory;
    private Class<?> type;
    private Object service;

    @Before public void setup() throws Exception {
        UnpooledDataSource ds = new UnpooledDataSource("org.h2.Driver", "jdbc:h2:mem:notifications_" + System.nanoTime() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        factory = new SqlSessionFactoryBuilder().build(new Configuration(new Environment("test", new JdbcTransactionFactory(), ds)));
        try (Connection c = ds.getConnection(); Statement s = c.createStatement()) {
            s.execute("CREATE TABLE notifications (id BIGINT AUTO_INCREMENT PRIMARY KEY, recipient_role VARCHAR(16) NOT NULL, recipient_id INT NOT NULL, event_key VARCHAR(191) NOT NULL, notification_type VARCHAR(40) NOT NULL, title VARCHAR(200) NOT NULL, body VARCHAR(2000) NOT NULL, target_type VARCHAR(40), target_id VARCHAR(100), created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL, read_at TIMESTAMP NULL, UNIQUE(event_key,recipient_role,recipient_id))");
        }
        try { type = Class.forName("com.ustb.hospital.service.NotificationService"); }
        catch (ClassNotFoundException missing) { throw new AssertionError("NotificationService must implement transactional recipient-scoped notifications", missing); }
        service = type.getConstructor(Supplier.class, Supplier.class).newInstance((Supplier<SqlSession>) () -> factory.openSession(false), (Supplier<SqlSession>) () -> factory.openSession(true));
    }

    @Test public void repeatedEventIsIdempotentPerRoleAndRecipient() throws Exception {
        try (SqlSession session = factory.openSession(false)) {
            enqueue(session,"PATIENT",7,"book:1"); enqueue(session,"PATIENT",7,"book:1");
            enqueue(session,"DOCTOR",7,"book:1"); enqueue(session,"PATIENT",8,"book:1");
            session.commit(true);
        }
        assertEquals(3, count());
        assertEquals(1L, invoke("countUnread", new Class[]{String.class,int.class},"PATIENT",7));
        assertEquals(1L, invoke("countUnread", new Class[]{String.class,int.class},"DOCTOR",7));
    }

    @Test public void enqueueUsesCallerTransactionAndNeverCommits() throws Exception {
        try (SqlSession session = factory.openSession(false)) {
            enqueue(session,"PATIENT",7,"rollback");
            assertEquals(0,count());
            session.rollback(true);
        }
        assertEquals(0,count());
    }

    @Test public void sqlFailuresPropagateAndCallerCanRollbackBusinessWrite() throws Exception {
        try (SqlSession init = factory.openSession(true); Statement statement = init.getConnection().createStatement()) {
            statement.execute("CREATE TABLE business_result (id INT)");
            statement.execute("ALTER TABLE notifications ADD CHECK (recipient_id <> 7)");
        }
        try (SqlSession session = factory.openSession(false); Statement statement = session.getConnection().createStatement()) {
            statement.executeUpdate("INSERT INTO business_result VALUES(1)");
            try { enqueue(session,"PATIENT",7,"broken"); fail("Nonduplicate notification SQL error must fail the business transaction"); }
            catch (IllegalStateException expected) { assertNotNull(expected.getCause()); }
            session.rollback(true);
            try (ResultSet rows = statement.executeQuery("SELECT COUNT(*) FROM business_result")) { rows.next(); assertEquals(0,rows.getInt(1)); }
        }
    }

    @Test public void paginationAndMarkingReadRemainScopedToCurrentRoleAndId() throws Exception {
        try (SqlSession session = factory.openSession(false)) {
            enqueue(session,"PATIENT",7,"a"); enqueue(session,"PATIENT",7,"b"); enqueue(session,"DOCTOR",7,"c");
            session.commit(true);
        }
        Map<?,?> first = (Map<?,?>) invoke("list",new Class[]{String.class,int.class,int.class,int.class},"PATIENT",7,1,1);
        assertEquals(2L,first.get("total"));
        List<?> items = (List<?>)first.get("items"); assertEquals(1,items.size());
        long id = ((Number)((Map<?,?>)items.get(0)).get("id")).longValue();
        assertEquals(false,invoke("markRead",new Class[]{String.class,int.class,long.class},"DOCTOR",7,id));
        assertEquals(true,invoke("markRead",new Class[]{String.class,int.class,long.class},"PATIENT",7,id));
        assertEquals(true,invoke("markRead",new Class[]{String.class,int.class,long.class},"PATIENT",7,id));
        assertEquals(1L,invoke("countUnread",new Class[]{String.class,int.class},"PATIENT",7));
        assertEquals(1,invoke("markAllRead",new Class[]{String.class,int.class},"PATIENT",7));
        assertEquals(0L,invoke("countUnread",new Class[]{String.class,int.class},"PATIENT",7));
        assertEquals(1L,invoke("countUnread",new Class[]{String.class,int.class},"DOCTOR",7));
    }

    @Test public void rejectsInvalidRoleAndUnboundedPagination() throws Exception {
        try { invoke("list",new Class[]{String.class,int.class,int.class,int.class},"PATIENT",7,1,101); fail(); }
        catch (IllegalArgumentException expected) { }
        try { invoke("list",new Class[]{String.class,int.class,int.class,int.class},"OTHER",7,1,10); fail(); }
        catch (IllegalArgumentException expected) { }
    }

    private void enqueue(SqlSession session,String role,int id,String event) throws Exception {
        try { type.getMethod("enqueue",SqlSession.class,String.class,int.class,String.class,String.class,String.class,String.class,String.class,String.class)
                .invoke(null,session,role,id,event,"BOOKING","预约提醒","请查看预约信息。","APPOINTMENT","12"); }
        catch (InvocationTargetException error) { throwCause(error); }
    }
    private Object invoke(String method,Class<?>[] signature,Object... args) throws Exception {
        try { return type.getMethod(method,signature).invoke(service,args); }
        catch (InvocationTargetException error) { throwCause(error); return null; }
    }
    private static void throwCause(InvocationTargetException error) throws Exception { if(error.getCause() instanceof Exception) throw (Exception)error.getCause(); throw error; }
    private int count() throws Exception {
        try (SqlSession session = factory.openSession(true); Statement s=session.getConnection().createStatement(); ResultSet r=s.executeQuery("SELECT COUNT(*) FROM notifications")) { r.next(); return r.getInt(1); }
    }
}
