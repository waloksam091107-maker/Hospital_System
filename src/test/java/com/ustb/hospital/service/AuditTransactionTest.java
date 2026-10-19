package com.ustb.hospital.service;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.Assert.*;

/** Exercises actual MyBatis transactions against an isolated in-memory database. */
public class AuditTransactionTest {
    private SqlSessionFactory factory;

    @Before
    public void setup() throws Exception {
        Properties properties = new Properties();
        properties.setProperty("db.driver", "org.h2.Driver");
        properties.setProperty("db.url", "jdbc:h2:mem:audit_" + UUID.randomUUID().toString().replace("-", "") + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        properties.setProperty("db.username", "sa");
        properties.setProperty("db.password", "");
        try (InputStream config = Resources.getResourceAsStream("mybatis-config.xml")) {
            factory = new SqlSessionFactoryBuilder().build(config, properties);
        }
        try (SqlSession session = factory.openSession(true); Statement statement = session.getConnection().createStatement()) {
            statement.execute("CREATE TABLE business_records (id INT PRIMARY KEY)");
            statement.execute("CREATE TABLE audit_logs (audit_log_id BIGINT AUTO_INCREMENT PRIMARY KEY, actor_role VARCHAR(20), actor_id INT, action VARCHAR(80) NOT NULL CHECK (action <> 'REJECTED_INSERT'), target_type VARCHAR(80), target_id VARCHAR(80), outcome VARCHAR(20) NOT NULL, request_method VARCHAR(10), request_path VARCHAR(255), remote_address VARCHAR(64), created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        }
    }

    @Test
    public void successAuditRollsBackWithBusiness() throws Exception {
        Object service = service(() -> factory.openSession(true));
        try (SqlSession session = factory.openSession(false)) {
            session.getConnection().createStatement().execute("INSERT INTO business_records VALUES (7)");
            writeSuccess(service, session, "APPOINTMENTS_BOOK");
            session.rollback(true);
        }
        assertEquals(0, count("business_records"));
        assertEquals(0, count("audit_logs"));
    }

    @Test
    public void failedAuditInsertPreventsCommit() throws Exception {
        Object service = service(() -> factory.openSession(true));
        try (SqlSession session = factory.openSession(false)) {
            session.getConnection().createStatement().execute("INSERT INTO business_records VALUES (8)");
            try {
                writeSuccess(service, session, "REJECTED_INSERT");
                fail("a failed audit insert must propagate to the transaction owner");
            } catch (InvocationTargetException expected) {
                session.rollback(true);
            }
        }
        assertEquals(0, count("business_records"));
        assertEquals(0, count("audit_logs"));
    }

    @Test
    public void failureAuditSurvivesBusinessRollback() throws Exception {
        Object service = service(() -> factory.openSession(true));
        try (SqlSession session = factory.openSession(false)) {
            session.getConnection().createStatement().execute("INSERT INTO business_records VALUES (9)");
            session.rollback(true);
        }
        recordFailure(service);
        assertEquals(0, count("business_records"));
        try (SqlSession session = factory.openSession(true); ResultSet rs = session.getConnection().createStatement().executeQuery("SELECT outcome, target_id FROM audit_logs")) {
            assertTrue(rs.next());
            assertEquals("FAILURE", rs.getString(1));
            assertEquals("7", rs.getString(2));
            assertFalse(rs.next());
        }
    }

    @Test
    public void failureSinkLogsNoSensitivePayload() throws Exception {
        final String secret = "SENSITIVE_TEST_TOKEN_7462";
        Object service = service(() -> { throw new IllegalStateException(secret); });
        Logger logger = Logger.getLogger(AuditService.class.getName());
        final StringBuilder captured = new StringBuilder();
        Handler handler = new Handler() {
            public void publish(LogRecord record) { captured.append(record.getMessage()); if (record.getThrown() != null) captured.append(record.getThrown()); }
            public void flush() { }
            public void close() { }
        };
        logger.addHandler(handler);
        try { recordFailure(service); } finally { logger.removeHandler(handler); }
        assertTrue("failure to persist audit must be surfaced in server logs", captured.length() > 0);
        assertFalse("the alert must not expose database errors or secrets", captured.toString().contains(secret));
    }

    private Object context() throws Exception {
        Class<?> type;
        try { type = Class.forName("com.ustb.hospital.service.AuditContext"); }
        catch (ClassNotFoundException missing) { throw new AssertionError("transactional audit context is required"); }
        return type.getConstructor(String.class, Integer.class, String.class, String.class, String.class)
                .newInstance("PATIENT", Integer.valueOf(1), "POST", "/appointments/book", "127.0.0.1");
    }

    private Object service(Supplier<SqlSession> supplier) throws Exception {
        context();
        return AuditService.class.getConstructor(Supplier.class).newInstance(supplier);
    }

    private void writeSuccess(Object service, SqlSession session, String action) throws Exception {
        Object auditContext = context();
        service.getClass().getMethod("writeSuccess", SqlSession.class, auditContext.getClass(), String.class, String.class, String.class)
                .invoke(service, session, auditContext, action, "APPOINTMENT", "7");
    }

    private void recordFailure(Object service) throws Exception {
        Object auditContext = context();
        service.getClass().getMethod("recordFailure", auditContext.getClass(), String.class, String.class, String.class)
                .invoke(service, auditContext, "APPOINTMENTS_BOOK", "APPOINTMENT", "7");
    }

    private int count(String table) throws Exception {
        try (SqlSession session = factory.openSession(true); ResultSet result = session.getConnection().createStatement().executeQuery("SELECT COUNT(*) FROM " + table)) {
            assertTrue(result.next());
            return result.getInt(1);
        }
    }
}
