package com.ustb.hospital.database;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

public class MedicalRecordSchemaContractTest {
    @Test
    public void createsOneMedicalRecordPerAppointmentAndChildOrders() throws Exception {
        Path migration = Paths.get("database/migrations/20261014_medical_records_and_orders.sql");
        assertTrue("medical record migration must exist", Files.exists(migration));

        String sql = new String(Files.readAllBytes(migration), StandardCharsets.UTF_8).toUpperCase();
        assertTrue(sql.contains("CREATE TABLE") && sql.contains("MEDICAL_RECORDS"));
        assertTrue(sql.contains("UNIQUE KEY UK_MEDICAL_RECORDS_APPOINTMENT"));
        assertTrue(sql.contains("MEDICAL_ORDERS"));
        assertTrue(sql.contains("FOREIGN KEY (APPOINTMENT_ID)"));
        assertTrue(sql.contains("FOREIGN KEY (MEDICAL_RECORD_ID)"));
    }

    @Test
    public void registersMedicalRecordMapper() throws Exception {
        Path entity = Paths.get("src/main/java/com/ustb/hospital/entity/MedicalRecord.java");
        Path orderEntity = Paths.get("src/main/java/com/ustb/hospital/entity/MedicalOrder.java");
        Path mapper = Paths.get("src/main/java/com/ustb/hospital/mapper/MedicalRecordsMapper.java");
        Path mapperXml = Paths.get("src/main/resources/com/ustb/hospital/mapper/MedicalRecordsMapper.xml");
        Path configuration = Paths.get("src/main/resources/mybatis-config.xml");

        assertTrue(Files.exists(entity));
        assertTrue(Files.exists(orderEntity));
        assertTrue(Files.exists(mapper));
        assertTrue(Files.exists(mapperXml));
        String xml = new String(Files.readAllBytes(configuration), StandardCharsets.UTF_8);
        assertTrue(xml.contains("com/ustb/hospital/mapper/MedicalRecordsMapper.xml"));
    }
}
