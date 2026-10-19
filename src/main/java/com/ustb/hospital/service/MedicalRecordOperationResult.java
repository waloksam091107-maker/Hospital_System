package com.ustb.hospital.service;

/** A safe, client-facing result for clinical record operations. */
public final class MedicalRecordOperationResult {
    private final boolean successful;
    private final String code;
    private final String message;
    private final Long medicalRecordId;
    private final String recordStatus;
    private final Integer version;

    private MedicalRecordOperationResult(boolean successful, String code, String message, Long medicalRecordId, String recordStatus, Integer version) {
        this.successful = successful;
        this.code = code;
        this.message = message;
        this.medicalRecordId = medicalRecordId;
        this.recordStatus = recordStatus;
        this.version = version;
    }

    public static MedicalRecordOperationResult success(String message) {
        return new MedicalRecordOperationResult(true, "OK", message, null, null, null);
    }

    public static MedicalRecordOperationResult success(String message, long id, String status, int version) {
        return new MedicalRecordOperationResult(true, "OK", message, Long.valueOf(id), status, Integer.valueOf(version));
    }

    public static MedicalRecordOperationResult failure(String code, String message) {
        return new MedicalRecordOperationResult(false, code, message, null, null, null);
    }

    public boolean isSuccessful() { return successful; }
    public String getCode() { return code; }
    public String getMessage() { return message; }
    public Long getMedicalRecordId() { return medicalRecordId; }
    public String getRecordStatus() { return recordStatus; }
    public Integer getVersion() { return version; }
}
