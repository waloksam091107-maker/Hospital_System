package com.ustb.hospital.database;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/** Prevents a Windows mysql client charset setting from corrupting Chinese demo data. */
public class DemoDataEncodingContractTest {
    @Test
    public void demoDataUsesExplicitUtf8LiteralsForChineseSeedValues() throws Exception {
        String sql = new String(Files.readAllBytes(Paths.get(
                "database", "schema", "hospt_minimal_demo_data.sql")), StandardCharsets.UTF_8);

        assertTrue(sql.contains("CONVERT(0xE58685E7A791 USING utf8mb4)"));
        assertTrue(sql.contains("CONVERT(0xE6BC94E7A4BAE58CBBE7949F USING utf8mb4)"));
        assertTrue(sql.contains("CONVERT(0xE4B8BBE6B2BBE58CBBE5B888 USING utf8mb4)"));
        assertTrue(sql.contains("CONVERT(0xE6BC94E7A4BAE7AEA1E79086E59198 USING utf8mb4)"));
    }
}
