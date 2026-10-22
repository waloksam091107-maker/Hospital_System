package com.ustb.hospital.database;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Protects the deployed OceanBase schema from drifting away from the application table contract. */
public class OceanBaseSchemaContractTest {
    @Test
    public void containsTheFinalBusinessSchemaWithoutMySqlOnlyMigrationSyntax() throws Exception {
        Path schema = Paths.get("database", "oceanbase", "01_hospt_schema.sql");
        Path departmentMapper = Paths.get("src", "main", "resources", "com", "ustb", "hospital", "mapper", "DepartmentsMapper.xml");
        assertTrue("OceanBase final schema must be provided for deployment.", Files.isRegularFile(schema));
        assertTrue("Department mapper must be provided for deployment.", Files.isRegularFile(departmentMapper));

        String ddl = new String(Files.readAllBytes(schema), StandardCharsets.UTF_8).toLowerCase();
        String mapper = new String(Files.readAllBytes(departmentMapper), StandardCharsets.UTF_8).toLowerCase();
        String[] requiredTables = {
                "departments", "professional_titles", "doctors", "patients", "admins", "dates",
                "schedule_slots", "appointments", "medical_records", "medical_orders", "audit_logs"
        };
        for (String table : requiredTables) {
            assertTrue("Missing table " + table, ddl.contains("create table if not exists " + table));
        }

        assertTrue(ddl.contains("active_slot_key"));
        assertTrue(ddl.contains("uk_appointments_active_slot"));
        assertTrue(ddl.contains("record_status"));
        assertTrue(ddl.contains("version int not null default 0"));
        assertTrue(ddl.contains("finalized_at"));
        assertTrue(ddl.contains("idx_audit_logs_created_id"));
        assertTrue("The production MySQL schema and its historical dumps use the legacy department_discription column spelling.",
                ddl.contains("department_discription"));
        assertFalse("A differently named department column would reject complete-insert history dumps.",
                ddl.contains("department_description varchar(500)"));
        assertTrue("The mapper result map must read the same legacy production column as the migrated schema.",
                mapper.contains("column=\"department_discription\" property=\"departmentdescription\""));

        assertFalse(ddl.contains("engine=innodb"));
        assertFalse(ddl.contains("prepare "));
        assertFalse(ddl.contains(" after "));
        assertFalse(ddl.contains("datadir"));
    }
}
