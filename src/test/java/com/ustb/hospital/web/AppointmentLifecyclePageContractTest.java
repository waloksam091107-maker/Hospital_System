package com.ustb.hospital.web;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

public class AppointmentLifecyclePageContractTest {
    @Test
    public void doctorAndPatientPagesUseLifecycleEndpointsAndCsrfHeader() throws Exception {
        String doctor = new String(Files.readAllBytes(Paths.get("src/main/webapp/doctor-schedule/appointments.html")), StandardCharsets.UTF_8);
        String patient = new String(Files.readAllBytes(Paths.get("src/main/webapp/hospro/patient.jsp")), StandardCharsets.UTF_8);
        assertTrue(doctor.contains("/appointments/start"));
        assertTrue(doctor.contains("IN_PROGRESS"));
        assertTrue(doctor.contains("X-CSRF-Token"));
        assertTrue(patient.contains("RESCHEDULED"));
        assertTrue(patient.contains("X-CSRF-Token"));
    }
}
