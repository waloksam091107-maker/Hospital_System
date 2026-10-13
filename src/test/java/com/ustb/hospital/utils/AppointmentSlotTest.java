package com.ustb.hospital.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class AppointmentSlotTest {
    @Test
    public void normalizesSupportedLegacyAndCanonicalValues() {
        assertEquals(AppointmentSlot.AM, AppointmentSlot.fromRequestValue("09.30am"));
        assertEquals(AppointmentSlot.PM, AppointmentSlot.fromRequestValue("AFTERNOON"));
        assertEquals(AppointmentSlot.EVENING, AppointmentSlot.fromRequestValue("evening"));
    }

    @Test
    public void rejectsUnsupportedValues() {
        assertNull(AppointmentSlot.fromRequestValue("midnight"));
        assertNull(AppointmentSlot.fromRequestValue(null));
    }
}
