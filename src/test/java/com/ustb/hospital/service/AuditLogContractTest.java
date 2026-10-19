package com.ustb.hospital.service;

import org.junit.Test;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class AuditLogContractTest {
    @Test
    public void auditServiceOnlyAllowsSafeOpaqueTargetIds() throws Exception {
        Class<?> service;
        try {
            service = Class.forName("com.ustb.hospital.service.AuditService");
        } catch (ClassNotFoundException exception) {
            throw new AssertionError("AuditService must exist", exception);
        }
        Method sanitize = service.getMethod("safeTargetId", String.class);
        assertEquals("123", sanitize.invoke(null, "123"));
        assertNull(sanitize.invoke(null, "诊断：发热三天"));
        assertNull(sanitize.invoke(null, "123456789012345678"));
    }

    @Test
    public void registersAppendOnlyAuditMapperWithoutClinicalFields() throws Exception {
        Path entity = Paths.get("src/main/java/com/ustb/hospital/entity/AuditLog.java");
        Path mapper = Paths.get("src/main/java/com/ustb/hospital/mapper/AuditLogsMapper.java");
        Path config = Paths.get("src/main/resources/mybatis-config.xml");
        assertTrue(Files.exists(entity));
        assertTrue(Files.exists(mapper));
        String entitySource = new String(Files.readAllBytes(entity), StandardCharsets.UTF_8).toLowerCase();
        assertTrue(!entitySource.contains("diagnosis") && !entitySource.contains("password") && !entitySource.contains("idcard"));
        assertTrue(new String(Files.readAllBytes(config), StandardCharsets.UTF_8).contains("AuditLogsMapper.xml"));
    }
}
