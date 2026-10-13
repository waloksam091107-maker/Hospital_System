package com.ustb.hospital.utils;

/** Lightweight service result suitable for the legacy Servlet endpoints. */
public final class AppointmentOperationResult {
    private final boolean successful;
    private final String code;
    private final String message;

    private AppointmentOperationResult(boolean successful, String code, String message) {
        this.successful = successful;
        this.code = code;
        this.message = message;
    }

    public static AppointmentOperationResult success(String message) {
        return new AppointmentOperationResult(true, "OK", message);
    }

    public static AppointmentOperationResult failure(String code, String message) {
        return new AppointmentOperationResult(false, code, message);
    }

    public boolean isSuccessful() { return successful; }
    public String getCode() { return code; }
    public String getMessage() { return message; }
}
