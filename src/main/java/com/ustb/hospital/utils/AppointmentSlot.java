package com.ustb.hospital.utils;

/**
 * Canonical slot values used by both the schedule table and appointments.
 * The public pages historically submitted display times such as "09.00am";
 * converting them here prevents different textual representations from
 * representing the same schedule slot.
 */
public final class AppointmentSlot {
    public static final String AM = "AM";
    public static final String PM = "PM";
    public static final String EVENING = "EVENING";

    private AppointmentSlot() {
    }

    public static String fromRequestValue(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toUpperCase();
        if (AM.equals(normalized) || "MORNING".equals(normalized) || "SHANG".equals(normalized)
                || normalized.endsWith("AM")) {
            return AM;
        }
        if (PM.equals(normalized) || "AFTERNOON".equals(normalized) || "XIA".equals(normalized)
                || normalized.endsWith("PM")) {
            return PM;
        }
        if (EVENING.equals(normalized) || "NIGHT".equals(normalized) || "WAN".equals(normalized)) {
            return EVENING;
        }
        return null;
    }
}
