package com.ustb.hospital.service;

import com.ustb.hospital.security.PasswordService;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.*;
import org.junit.*;
import java.io.InputStream;
import java.sql.*;
import java.time.*;
import java.util.*;
import static org.junit.Assert.*;

/** Opt-in regression against an already initialized, restricted, random OceanBase test schema. */
public class AccountSecurityOceanBaseTimestampTest {
    private static SqlSessionFactory factory;
    private static TimeZone originalZone;
    private final Instant start = Instant.parse("2026-10-29T07:06:00Z");
    private final String name = "timestamp-" + UUID.randomUUID();
    private final String ip = "timestamp-" + UUID.randomUUID();
    private int patient;

    @BeforeClass public static void connectRestrictedTestSchema() throws Exception {
        Assume.assumeTrue("Explicit isolated OceanBase opt-in required", Boolean.getBoolean("hospital.test.oceanbase"));
        String schema = required("HOSPITAL_TEST_DB_NAME"), url = required("HOSPITAL_TEST_DB_URL");
        assertTrue(schema.matches("hospital_test_[a-f0-9]{32}"));
        assertTrue(url.matches("jdbc:oceanbase://127\\.0\\.0\\.1:[0-9]{4,5}/" + schema + "\\?.*"));
        int port = Integer.parseInt(url.substring(url.indexOf("127.0.0.1:") + 10, url.indexOf('/', url.indexOf("127.0.0.1:") + 10)));
        assertTrue(port != 2881 && port != 3306 && port != 8080 && port <= 65535);
        assertEquals("hospital_test_runner@test", required("HOSPITAL_TEST_DB_USERNAME"));
        Properties p = new Properties();
        p.setProperty("db.driver", "com.oceanbase.jdbc.Driver"); p.setProperty("db.url", url);
        p.setProperty("db.username", required("HOSPITAL_TEST_DB_USERNAME")); p.setProperty("db.password", required("HOSPITAL_TEST_DB_PASSWORD"));
        try (InputStream config = Resources.getResourceAsStream("mybatis-config.xml")) { factory = new SqlSessionFactoryBuilder().build(config, p); }
        try (SqlSession s = factory.openSession(); ResultSet r = s.getConnection().createStatement().executeQuery("SELECT DATABASE(),VERSION(),@@session.time_zone")) {
            assertTrue(r.next()); assertEquals(schema, r.getString(1));
            assertTrue(r.getString(2).toLowerCase(Locale.ROOT).contains("oceanbase")); assertEquals("+08:00", r.getString(3));
        }
        originalZone = TimeZone.getDefault(); TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }
    @AfterClass public static void restoreTimezone() { if (originalZone != null) TimeZone.setDefault(originalZone); }
    @Before public void fixture() throws Exception {
        try (SqlSession s = factory.openSession(true); PreparedStatement p = s.getConnection().prepareStatement("INSERT INTO patients(name,password,password_hash) VALUES(?,NULL,?)", Statement.RETURN_GENERATED_KEYS)) {
            p.setString(1, name); p.setString(2, PasswordService.hash("correct-password")); p.executeUpdate();
            try (ResultSet r = p.getGeneratedKeys()) { assertTrue(r.next()); patient = r.getInt(1); }
        }
    }
    @After public void cleanup() throws Exception {
        if (factory == null) return;
        try (SqlSession s = factory.openSession(true)) {
            AccountSecurityStore.execute(s, "DELETE FROM login_throttle WHERE source_ip=?", ip);
            AccountSecurityStore.execute(s, "DELETE FROM account_security WHERE account_role='PATIENT' AND account_id=?", patient);
            AccountSecurityStore.execute(s, "DELETE FROM patients WHERE patient_id=?", patient);
        }
    }
    @Test public void successfulLoginsPreserveSourceWindowWallTimeAndInstant() throws Exception {
        for (int n = 0; n < 4; n++) {
            assertTrue(service(start.plusSeconds(n)).authenticate("PATIENT", name, "correct-password", ip).isSuccessful());
            assertEquals("2026-10-29 15:06:00", scalar("SELECT DATE_FORMAT(window_start,'%Y-%m-%d %H:%i:%s') FROM login_throttle WHERE source_ip='" + ip + "'"));
        }
    }
    @Test public void accountLockAndAdminListExpireAtExactlyFifteenMinutes() throws Exception {
        for (int n = 0; n < 5; n++) assertFalse(service(start).authenticate("PATIENT", name, "wrong", ip).isSuccessful());
        assertEquals("2026-10-29 15:06:00", scalar("SELECT DATE_FORMAT(failure_window_start,'%Y-%m-%d %H:%i:%s') FROM account_security WHERE account_role='PATIENT' AND account_id=" + patient));
        assertEquals("2026-10-29 15:21:00", scalar("SELECT DATE_FORMAT(locked_until,'%Y-%m-%d %H:%i:%s') FROM account_security WHERE account_role='PATIENT' AND account_id=" + patient));
        try (SqlSession s = factory.openSession()) {
            AccountSecurityService.AccountState state = AccountSecurityStore.state(s, "PATIENT", patient, false);
            assertEquals(start, state.windowStart.toInstant()); assertEquals(Instant.parse("2026-10-29T07:21:00Z"), state.lockedUntil.toInstant());
        }
        boolean found = false;
        for (int page = 1; !found; page++) {
            List<Map<String,Object>> rows = service(start).list("PATIENT", page, 100); assertFalse("Fixture must appear in account list", rows.isEmpty());
            for (Map<String,Object> row : rows) if (((Number) row.get("id")).intValue() == patient) {
                assertEquals(Instant.parse("2026-10-29T07:21:00Z"), ((Timestamp) row.get("lockedUntil")).toInstant()); found = true;
            }
        }
        assertFalse(service(start.plusSeconds(899)).authenticate("PATIENT", name, "correct-password", ip).isSuccessful());
        assertTrue(service(start.plusSeconds(900)).authenticate("PATIENT", name, "correct-password", ip).isSuccessful());
    }
    @Test public void sourceLimitRemainsSixtyAndRecoversAtExactlyFifteenMinutes() throws Exception {
        for (int n = 0; n < 59; n++) assertFalse(service(start).authenticate("PATIENT", name + "-missing", "wrong", ip).isSuccessful());
        assertTrue(service(start).authenticate("PATIENT", name, "correct-password", ip).isSuccessful());
        assertFalse(service(start).authenticate("PATIENT", name, "correct-password", ip).isSuccessful());
        assertFalse(service(start.plusSeconds(899)).authenticate("PATIENT", name, "correct-password", ip).isSuccessful());
        assertTrue(service(start.plusSeconds(900)).authenticate("PATIENT", name, "correct-password", ip).isSuccessful());
    }
    private AccountSecurityService service(Instant instant) { return new AccountSecurityService(() -> factory.openSession(false), new AuditService(() -> factory.openSession(true)), Clock.fixed(instant, ZoneOffset.UTC)); }
    private String scalar(String sql) throws Exception { try (SqlSession s = factory.openSession(); ResultSet r = s.getConnection().createStatement().executeQuery(sql)) { assertTrue(r.next()); return r.getString(1); } }
    private static String required(String key) { String value = System.getenv(key); assertNotNull("Missing " + key, value); return value; }
}
