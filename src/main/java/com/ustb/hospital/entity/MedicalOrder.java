package com.ustb.hospital.entity;

/** A prescribed item belonging to one immutable medical record. */
public class MedicalOrder {
    private long medicalOrderId;
    private long medicalRecordId;
    private String itemName;
    private String dosage;
    private String frequency;
    private Integer quantity;
    private String instructionText;
    private String createdAt;

    public long getMedicalOrderId() { return medicalOrderId; }
    public void setMedicalOrderId(long medicalOrderId) { this.medicalOrderId = medicalOrderId; }
    public long getMedicalRecordId() { return medicalRecordId; }
    public void setMedicalRecordId(long medicalRecordId) { this.medicalRecordId = medicalRecordId; }
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public String getDosage() { return dosage; }
    public void setDosage(String dosage) { this.dosage = dosage; }
    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public String getInstructionText() { return instructionText; }
    public void setInstructionText(String instructionText) { this.instructionText = instructionText; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
