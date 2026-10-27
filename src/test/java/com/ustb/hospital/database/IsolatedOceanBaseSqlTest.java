package com.ustb.hospital.database;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import static org.junit.Assert.*;

public class IsolatedOceanBaseSqlTest {
    private final String schema = "hospital_test_0123456789abcdef0123456789abcdef";
    @Test public void reviewedSchemaSelectsOnlyTheRandomTarget() throws Exception {
        String source = new String(Files.readAllBytes(Paths.get("database/oceanbase/01_hospt_schema.sql")), StandardCharsets.UTF_8);
        String safe = IsolatedOceanBaseSql.rewriteSchema(source, schema);
        assertTrue(safe.startsWith("USE `" + schema + "`;"));
        assertFalse(safe.toLowerCase().contains("hospt"));
        assertTrue(safe.contains("CREATE TABLE IF NOT EXISTS appointments"));
    }
    @Test public void refusesOtherDatabaseSelectionsAndQualifiedDestinations() {
        for (String sql : new String[]{"USE other; CREATE TABLE x(id INT);", "CREATE DATABASE hospt;", "INSERT INTO hospt.patients VALUES(1);", "CREATE TABLE `other`.`x` (id INT);", "/*!50000 USE hospt */;"}) {
            try { IsolatedOceanBaseSql.rewriteSchema(sql, schema); fail("Unsafe SQL accepted"); } catch (IllegalArgumentException expected) { }
        }
        try { IsolatedOceanBaseSql.rewriteSchema("CREATE TABLE x(id INT)", "hospt"); fail("Business schema accepted"); } catch (IllegalArgumentException expected) { }
    }
}
