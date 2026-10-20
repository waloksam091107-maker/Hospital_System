package com.ustb.hospital.web;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MedicalRecordPageContractTest {
    @Test
    public void doctorAppointmentPageSendsFinalizationThroughEncounterPage() throws Exception {
        Path appointments = Paths.get("src/main/webapp/doctor-schedule/appointments.html");
        String html = new String(Files.readAllBytes(appointments), StandardCharsets.UTF_8);
        assertTrue(html.contains("encounter.html?appointmentId="));
        assertFalse("the list must not directly complete a visit", html.contains("/appointments/complete"));
    }

    @Test
    public void createsDoctorAndPatientMedicalRecordPages() throws Exception {
        Path doctorPage = Paths.get("src/main/webapp/doctor-schedule/encounter.html");
        Path patientPage = Paths.get("src/main/webapp/hospro/medical-records.html");
        assertTrue(Files.exists(doctorPage));
        assertTrue(Files.exists(patientPage));
        assertTrue(new String(Files.readAllBytes(doctorPage), StandardCharsets.UTF_8).contains("/medical-records/finalize"));
        assertTrue(new String(Files.readAllBytes(patientPage), StandardCharsets.UTF_8).contains("/medical-records/patient"));
    }

    @Test
    public void encounterPageUsesTheSharedFriendlyErrorMapperForProtectedApis() throws Exception {
        Path doctorPage = Paths.get("src/main/webapp/doctor-schedule/encounter.html");
        String html = new String(Files.readAllBytes(doctorPage), StandardCharsets.UTF_8);
        assertTrue(html.contains("doctor-api.js"));
        assertTrue(html.contains("doctorApiErrorMessage(xhr,"));
    }
}
