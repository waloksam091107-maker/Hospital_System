package com.ustb.hospital.service;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.Assert.*;

/** Exercises portal account writes against the real MyBatis mappings in a disposable database. */
public class PortalAccountServiceTest {
    private SqlSessionFactory factory;
    private PortalAccountService service;
    private final AuditContext anonymousRegistration = new AuditContext(null, null, "POST", "/portal/accounts/patient-register", "127.0.0.1");
    private final AuditContext patientProfile = new AuditContext("PATIENT", 1, "POST", "/portal/profile", "127.0.0.1");

    @Before public void setup() throws Exception {
        Properties properties = new Properties();
        properties.setProperty("db.driver", "org.h2.Driver");
        properties.setProperty("db.url", "jdbc:h2:mem:portal_" + UUID.randomUUID().toString().replace("-", "") + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        properties.setProperty("db.username", "sa"); properties.setProperty("db.password", "");
        try (InputStream input = Resources.getResourceAsStream("mybatis-config.xml")) { factory = new SqlSessionFactoryBuilder().build(input, properties); }
        try (SqlSession session = factory.openSession(true); Statement statement = session.getConnection().createStatement()) {
            statement.execute("CREATE TABLE patients (patient_id INT AUTO_INCREMENT PRIMARY KEY, id_card_number VARCHAR(50) NOT NULL UNIQUE, password VARCHAR(255), password_hash VARCHAR(255), name VARCHAR(100) NOT NULL UNIQUE, avatar VARCHAR(255), phone VARCHAR(50), email VARCHAR(255), balance DECIMAL(12,2) NOT NULL DEFAULT 0.00)");
            statement.execute("CREATE TABLE audit_logs (audit_log_id BIGINT AUTO_INCREMENT PRIMARY KEY, actor_role VARCHAR(20), actor_id INT, action VARCHAR(80), target_type VARCHAR(80), target_id VARCHAR(80), outcome VARCHAR(20), request_method VARCHAR(10), request_path VARCHAR(255), remote_address VARCHAR(64), created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        }
        Supplier<SqlSession> transactions = () -> factory.openSession(false);
        Supplier<SqlSession> reads = () -> factory.openSession(true);
        service = new PortalAccountService(transactions, reads, new AuditService(reads));
    }

    @Test public void patientRegistrationHashesCredentialsAndWritesOnlyMetadataAudit() throws Exception {
        assertTrue(service.registerPatient("测试患者", "110101199001011234", "SecurePass1!", "13800138000", "patient@example.com", anonymousRegistration).isSuccessful());
        assertEquals("1", scalar("SELECT COUNT(*) FROM patients"));
        assertEquals("1", scalar("SELECT COUNT(*) FROM audit_logs WHERE action = 'PATIENT_REGISTER' AND outcome = 'SUCCESS'"));
        assertEquals("1", scalar("SELECT COUNT(*) FROM patients WHERE password IS NULL AND password_hash LIKE '$2%'"));

        Map<String, Object> profile = service.readOwnProfile("PATIENT", 1);
        assertEquals("测试患者", profile.get("name"));
        assertFalse(profile.containsKey("password"));
        assertFalse(profile.containsKey("passwordHash"));
    }

    @Test public void registrationRejectsDuplicateAndProfileCannotUpdateBalance() throws Exception {
        assertTrue(service.registerPatient("测试患者", "110101199001011234", "SecurePass1!", null, null, anonymousRegistration).isSuccessful());
        assertEquals("DUPLICATE_ACCOUNT", service.registerPatient("测试患者", "110101199001011235", "SecurePass1!", null, null, anonymousRegistration).getCode());
        assertTrue(service.updateOwnProfile("PATIENT", 1, "13900139000", "new@example.com", "ignored", patientProfile).isSuccessful());
        assertEquals("0.00", scalar("SELECT balance FROM patients WHERE patient_id = 1"));
        assertEquals("13900139000", scalar("SELECT phone FROM patients WHERE patient_id = 1"));
    }

    private String scalar(String sql) throws Exception {
        try (SqlSession session = factory.openSession(true); ResultSet result = session.getConnection().createStatement().executeQuery(sql)) {
            assertTrue(result.next()); return result.getString(1);
        }
    }
}
