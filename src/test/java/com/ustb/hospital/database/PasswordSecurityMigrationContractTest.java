package com.ustb.hospital.database;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

public class PasswordSecurityMigrationContractTest {
    @Test
    public void addsPasswordHashColumnsWithoutDiscardingExistingAccounts() throws Exception {
        Path migration = Paths.get("database/migrations/20261014_password_hash_and_audit.sql");
        assertTrue("security migration must exist", Files.exists(migration));
        String sql = new String(Files.readAllBytes(migration), StandardCharsets.UTF_8).toUpperCase();
        assertTrue(sql.contains("DOCTORS") && sql.contains("PATIENTS") && sql.contains("ADMINS"));
        assertTrue(sql.contains("PASSWORD_HASH"));
        assertTrue(sql.contains("AUDIT_LOGS"));
        assertFalse("MySQL 8.0.19 does not support ADD COLUMN IF NOT EXISTS", sql.contains("ALTER TABLE DOCTORS ADD COLUMN IF NOT EXISTS"));
        assertFalse("MySQL 8.0.19 does not support ADD COLUMN IF NOT EXISTS", sql.contains("ALTER TABLE PATIENTS ADD COLUMN IF NOT EXISTS"));
        assertFalse("MySQL 8.0.19 does not support ADD COLUMN IF NOT EXISTS", sql.contains("ALTER TABLE ADMINS ADD COLUMN IF NOT EXISTS"));
        assertTrue("migration must conditionally add columns on older MySQL versions", sql.contains("INFORMATION_SCHEMA.COLUMNS"));
    }
}
