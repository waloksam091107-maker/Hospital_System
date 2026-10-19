package com.ustb.hospital.database;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.Assert.*;

/** Runs DDL only in the explicitly enabled, independently initialized local test server. */
public class ClinicalDraftMigrationTest {
    private static final Path MIGRATION = Paths.get("database/migrations/20261017_clinical_drafts_and_audit_query.sql");
    private Connection connection;

    @Before
    public void setup() throws Exception {
        Assume.assumeTrue("MySQL integration requires -Dhospital.test.mysql=true", Boolean.getBoolean("hospital.test.mysql"));
        assertTrue("incremental migration must exist", Files.isRegularFile(MIGRATION));
        connection = DriverManager.getConnection("jdbc:mysql://127.0.0.1:13307/?serverTimezone=Asia/Shanghai", "root", "");
        try (Statement statement = connection.createStatement(); ResultSet actual = statement.executeQuery("SELECT @@port, @@datadir")) {
            assertTrue(actual.next());
            assertEquals(13307, actual.getInt(1));
            String dataDir = actual.getString(2).replace('\\', '/').toLowerCase();
            assertTrue("refuse to touch a non-isolated server", dataDir.contains("/.superpowers/sdd/2026-10-17-clinical-records-audit-completion/mysql-data/"));
        }
        String schema = "hospital_draft_test_" + UUID.randomUUID().toString().replace("-", "");
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE `" + schema + "` CHARACTER SET utf8mb4");
            statement.execute("USE `" + schema + "`");
            statement.execute("CREATE TABLE medical_records (medical_record_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, appointment_id INT NOT NULL UNIQUE, doctor_id INT NOT NULL, patient_id INT NOT NULL, diagnosis VARCHAR(500) NOT NULL, treatment_plan TEXT NULL, medical_advice TEXT NULL, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            statement.execute("CREATE TABLE audit_logs (audit_log_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, actor_role VARCHAR(20), actor_id INT, action VARCHAR(80), outcome VARCHAR(20), created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            statement.execute("INSERT INTO medical_records (appointment_id,doctor_id,patient_id,diagnosis,treatment_plan) VALUES (1,1,1,'original diagnosis',NULL)");
        }
    }

    @After
    public void close() throws Exception {
        if (connection != null) connection.close();
    }

    @Test
    public void migrationKeepsHistoricalRecordFinal() throws Exception {
        migrate();
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery("SELECT diagnosis, treatment_plan, record_status, version, finalized_at FROM medical_records WHERE appointment_id=1")) {
            assertTrue(result.next());
            assertEquals("original diagnosis", result.getString(1));
            assertNull(result.getString(2));
            assertEquals("FINAL", result.getString(3));
            assertEquals(0, result.getInt(4));
            assertNull(result.getTimestamp(5));
        }
    }

    @Test
    public void migrationCanRunTwiceWithoutDataLoss() throws Exception {
        migrate();
        migrate();
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery("SELECT COUNT(*), MIN(diagnosis) FROM medical_records")) {
            assertTrue(result.next());
            assertEquals(1, result.getInt(1));
            assertEquals("original diagnosis", result.getString(2));
        }
    }

    @Test
    public void emptyDiagnosisDraftCanBeStored() throws Exception {
        migrate();
        try (Statement statement = connection.createStatement()) {
            statement.execute("INSERT INTO medical_records (appointment_id,doctor_id,patient_id,diagnosis,record_status) VALUES (2,1,1,NULL,'DRAFT')");
            try (ResultSet result = statement.executeQuery("SELECT diagnosis,record_status FROM medical_records WHERE appointment_id=2")) {
                assertTrue(result.next());
                assertNull(result.getString(1));
                assertEquals("DRAFT", result.getString(2));
            }
        }
    }

    private void migrate() throws Exception {
        String sql = new String(Files.readAllBytes(MIGRATION), StandardCharsets.UTF_8).replaceAll("(?m)^\\s*--.*$", "");
        try (Statement statement = connection.createStatement()) {
            for (String part : sql.split(";")) {
                if (!part.trim().isEmpty()) statement.execute(part.trim());
            }
        }
    }
}
