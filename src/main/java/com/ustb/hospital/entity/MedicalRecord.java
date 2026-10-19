package com.ustb.hospital.entity;

import java.util.ArrayList;
import java.util.List;

/** A draft or finalized clinical record associated with one appointment. */
public class MedicalRecord {
    private long medicalRecordId;
    private int appointmentId;
    private int doctorId;
    private int patientId;
    private String diagnosis;
    private String treatmentPlan;
    private String medicalAdvice;
    private String createdAt;
    private String updatedAt;
    private String recordStatus;
    private int version;
    private String finalizedAt;
    private List<MedicalOrder> orders = new ArrayList<MedicalOrder>();

    public long getMedicalRecordId() { return medicalRecordId; }
    public void setMedicalRecordId(long medicalRecordId) { this.medicalRecordId = medicalRecordId; }
    public int getAppointmentId() { return appointmentId; }
    public void setAppointmentId(int appointmentId) { this.appointmentId = appointmentId; }
    public int getDoctorId() { return doctorId; }
    public void setDoctorId(int doctorId) { this.doctorId = doctorId; }
    public int getPatientId() { return patientId; }
    public void setPatientId(int patientId) { this.patientId = patientId; }
    public String getDiagnosis() { return diagnosis; }
    public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }
    public String getTreatmentPlan() { return treatmentPlan; }
    public void setTreatmentPlan(String treatmentPlan) { this.treatmentPlan = treatmentPlan; }
    public String getMedicalAdvice() { return medicalAdvice; }
    public void setMedicalAdvice(String medicalAdvice) { this.medicalAdvice = medicalAdvice; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    public String getRecordStatus() { return recordStatus; }
    public void setRecordStatus(String recordStatus) { this.recordStatus = recordStatus; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
    public String getFinalizedAt() { return finalizedAt; }
    public void setFinalizedAt(String finalizedAt) { this.finalizedAt = finalizedAt; }
    public List<MedicalOrder> getOrders() { return orders; }
    public void setOrders(List<MedicalOrder> orders) { this.orders = orders == null ? new ArrayList<MedicalOrder>() : orders; }
}
