package com.ustb.hospital.service;

import com.ustb.hospital.entity.BalanceTransaction;
import com.ustb.hospital.utils.AppointmentOperationResult;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.Assert.*;

/** Verifies a patient recharge changes balance, ledger and audit as one transaction. */
public class BalanceServiceTest {
    private SqlSessionFactory factory;
    private BalanceService service;
    private final AuditContext patient = new AuditContext("PATIENT", 1, "POST", "/portal/balance/recharge", "127.0.0.1");

    @Before public void setup() throws Exception {
        Properties properties = new Properties();
        properties.setProperty("db.driver", "org.h2.Driver");
        properties.setProperty("db.url", "jdbc:h2:mem:balance_" + UUID.randomUUID().toString().replace("-", "") + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        properties.setProperty("db.username", "sa"); properties.setProperty("db.password", "");
        try (InputStream input = Resources.getResourceAsStream("mybatis-config.xml")) { factory = new SqlSessionFactoryBuilder().build(input, properties); }
        try (SqlSession session = factory.openSession(true); Statement statement = session.getConnection().createStatement()) {
            statement.execute("CREATE TABLE patients (patient_id INT PRIMARY KEY, id_card_number VARCHAR(50), password VARCHAR(255), password_hash VARCHAR(255), name VARCHAR(100), avatar VARCHAR(255), phone VARCHAR(50), email VARCHAR(255), balance DECIMAL(12,2) NOT NULL)");
            statement.execute("INSERT INTO patients(patient_id, balance) VALUES(1, 8.50), (2, 3.00)");
            statement.execute("CREATE TABLE balance_transactions (transaction_id BIGINT AUTO_INCREMENT PRIMARY KEY, patient_id INT NOT NULL, appointment_id INT, transaction_type VARCHAR(32) NOT NULL, amount DECIMAL(12,2) NOT NULL, balance_after DECIMAL(12,2) NOT NULL, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, note VARCHAR(255))");
            statement.execute("CREATE TABLE audit_logs (audit_log_id BIGINT AUTO_INCREMENT PRIMARY KEY, actor_role VARCHAR(20), actor_id INT, action VARCHAR(80), target_type VARCHAR(80), target_id VARCHAR(80), outcome VARCHAR(20), request_method VARCHAR(10), request_path VARCHAR(255), remote_address VARCHAR(64), created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        }
        Supplier<SqlSession> transactions = () -> factory.openSession(false);
        Supplier<SqlSession> reads = () -> factory.openSession(true);
        service = new BalanceService(transactions, reads, new AuditService(reads));
    }

    @Test public void rechargeUpdatesTheCurrentPatientsBalanceLedgerAndAuditTogether() throws Exception {
        AppointmentOperationResult result = service.recharge(1, new BigDecimal("12.50"), patient);
        assertTrue(result.isSuccessful());
        assertEquals("21.00", scalar("SELECT balance FROM patients WHERE patient_id = 1"));
        List<BalanceTransaction> transactions = service.recentTransactions(1, 10);
        assertEquals(1, transactions.size());
        assertEquals("RECHARGE", transactions.get(0).getTransactionType());
        assertEquals(new BigDecimal("12.50"), transactions.get(0).getAmount());
        assertEquals(new BigDecimal("21.00"), transactions.get(0).getBalanceAfter());
        assertEquals("1", scalar("SELECT COUNT(*) FROM audit_logs WHERE action = 'BALANCE_RECHARGE' AND outcome = 'SUCCESS'"));
    }

    @Test public void rechargeRejectsInvalidAmountWithoutChangingBalanceOrLedger() throws Exception {
        assertEquals("INVALID_AMOUNT", service.recharge(1, new BigDecimal("0.001"), patient).getCode());
        assertEquals("INVALID_AMOUNT", service.recharge(1, new BigDecimal("10000.01"), patient).getCode());
        assertEquals("8.50", scalar("SELECT balance FROM patients WHERE patient_id = 1"));
        assertEquals("0", scalar("SELECT COUNT(*) FROM balance_transactions"));
    }

    private String scalar(String sql) throws Exception {
        try (SqlSession session = factory.openSession(true); ResultSet result = session.getConnection().createStatement().executeQuery(sql)) {
            assertTrue(result.next()); return result.getString(1);
        }
    }
}
