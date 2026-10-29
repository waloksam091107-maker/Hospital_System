package com.ustb.hospital.service;

import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/** Historical appointments must never push an actionable visit out of the queue limit. */
public class ClinicQueueVisibilityTest {
    private ClinicWorkflowTest fixture;

    @Before public void setup() throws Exception {
        fixture = new ClinicWorkflowTest();
        fixture.setup();
        StringBuilder history = new StringBuilder("INSERT INTO appointments(patient_id,doctor_id,appointment_date,appointment_time,status,queue_status,appointment_sequence) VALUES");
        for (int i = 0; i < 300; i++) {
            if (i > 0) history.append(',');
            history.append("(1,1,'2030-05-06','AM','CANCELLED','REMOVED',1)");
        }
        fixture.sql(history.toString());
        fixture.ok(fixture.book(1, fixture.day));
    }

    @Test public void patientCanFindAnUncheckedAppointmentAfterLongHistory() {
        assertActionable(((ClinicWorkflowService) fixture.service).queue("PATIENT", 1, null, null));
    }

    @Test public void doctorCanFindAnUncheckedAppointmentAfterLongHistory() {
        assertActionable(((ClinicWorkflowService) fixture.service).queue("DOCTOR", 1, fixture.day, "AM"));
    }

    private void assertActionable(List<Map<String, Object>> list) {
        assertEquals(300, list.size());
        assertEquals("the active appointment must remain visible before older cancelled visits", "BOOKED", list.get(0).get("status"));
        assertEquals(301, ((Number) list.get(0).get("appointmentId")).intValue());
    }
}
