package com.ustb.hospital.database;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

public class AppointmentSlotMigrationContractTest {
    @Test
    public void createsSlotsBackfillsLegacySchedulesAndSupportsLifecycleStates() throws Exception {
        Path migration = Paths.get("database/migrations/20261014_appointment_slots_and_states.sql");
        assertTrue("appointment-slot migration must exist", Files.exists(migration));

        String sql = new String(Files.readAllBytes(migration), StandardCharsets.UTF_8).toUpperCase();
        assertTrue(sql.contains("CREATE TABLE") && sql.contains("SCHEDULE_SLOTS"));
        assertTrue(sql.contains("UNIQUE KEY UK_SCHEDULE_SLOTS_DOCTOR_DATE_PERIOD"));
        assertTrue(sql.contains("INSERT INTO SCHEDULE_SLOTS"));
        assertTrue(sql.contains("RESCHEDULED"));
        assertTrue(sql.contains("IN_PROGRESS"));
    }

    @Test
    public void registersScheduleSlotModelAndMapper() throws Exception {
        Path entity = Paths.get("src/main/java/com/ustb/hospital/entity/ScheduleSlot.java");
        Path mapper = Paths.get("src/main/java/com/ustb/hospital/mapper/ScheduleSlotsMapper.java");
        Path mapperXml = Paths.get("src/main/resources/com/ustb/hospital/mapper/ScheduleSlotsMapper.xml");
        Path configuration = Paths.get("src/main/resources/mybatis-config.xml");

        assertTrue("schedule slot entity must exist", Files.exists(entity));
        assertTrue("schedule slot mapper must exist", Files.exists(mapper));
        assertTrue("schedule slot mapper XML must exist", Files.exists(mapperXml));
        String xml = new String(Files.readAllBytes(configuration), StandardCharsets.UTF_8);
        assertTrue(xml.contains("com/ustb/hospital/mapper/ScheduleSlotsMapper.xml"));
    }

    @Test
    public void scheduleServiceMaintainsTheSameSlotsUsedByBooking() throws Exception {
        Path service = Paths.get("src/main/java/com/ustb/hospital/service/DatesServiceImpl.java");
        String source = new String(Files.readAllBytes(service), StandardCharsets.UTF_8);
        assertTrue("schedule writes must use schedule slots", source.contains("ScheduleSlotsMapper"));
    }
}
