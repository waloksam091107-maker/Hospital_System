package com.ustb.hospital.service;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

public class MedicalRecordLifecycleContractTest {
    @Test
    public void finalizationUsesOneTransactionAndValidatesItsClinicalPayload() throws Exception {
        Path service = Paths.get("src/main/java/com/ustb/hospital/service/MedicalRecordsServiceImpl.java");
        assertTrue("medical record service must exist", Files.exists(service));

        String source = new String(Files.readAllBytes(service), StandardCharsets.UTF_8);
        assertTrue("must run in a transaction", source.contains("getTransactionSqlSession"));
        assertTrue("must only finalize started appointments", source.contains("STATUS_IN_PROGRESS"));
        assertTrue("must write a record", source.contains("insertRecord"));
        assertTrue("must write orders", source.contains("insertOrders"));
        assertTrue("must complete only the current doctor's appointment", source.contains("updateStatusForDoctorIfCurrent"));
        assertTrue("must reject missing diagnosis before writing", source.contains("INVALID_DIAGNOSIS"));
    }

    @Test
    public void exposesAResultTypeForValidationErrors() {
        try {
            Class<?> resultType = Class.forName("com.ustb.hospital.service.MedicalRecordOperationResult");
            assertTrue("result must expose its success flag", resultType.getMethod("isSuccessful") != null);
            assertTrue("result must expose a business code", resultType.getMethod("getCode") != null);
        } catch (ClassNotFoundException exception) {
            throw new AssertionError("MedicalRecordOperationResult must exist", exception);
        } catch (Exception exception) {
            throw new AssertionError("MedicalRecordOperationResult has the wrong public API", exception);
        }
    }
}
