package com.ustb.hospital.service;

/** Central lifecycle rules for appointment mutations. */
public final class AppointmentStatus {
    public static final String NO_SHOW = "NO_SHOW";
    public static final String BOOKED = "BOOKED";
    public static final String RESCHEDULED = "RESCHEDULED";
    public static final String CANCELLED = "CANCELLED";
    public static final String IN_PROGRESS = "IN_PROGRESS";
    public static final String COMPLETED = "COMPLETED";

    private AppointmentStatus() {
    }

    public static boolean canCancel(String status) {
        return BOOKED.equals(status);
    }

    public static boolean canReschedule(String status) {
        return BOOKED.equals(status);
    }

    /** Explains a rejected patient action using the current persisted status. */
    public static String patientMutationFailureMessage(String status) {
        if (BOOKED.equals(status)) return null;
        if (RESCHEDULED.equals(status)) return "该预约已改期，请操作新的预约记录。";
        if (CANCELLED.equals(status)) return "该预约已取消，无法继续操作。";
        return "当前预约状态不允许此操作。";
    }

    public static boolean canStart(String status) {
        return BOOKED.equals(status);
    }

    public static boolean keepsSlotOccupied(String status) {
        return BOOKED.equals(status) || IN_PROGRESS.equals(status) || (COMPLETED.equals(status) || NO_SHOW.equals(status));
    }
}
