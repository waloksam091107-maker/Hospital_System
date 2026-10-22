package com.ustb.hospital.entity;

import java.math.BigDecimal;

/** Immutable-in-practice account ledger entry for patient balance changes. */
public class BalanceTransaction {
    private Long transactionId;
    private int patientId;
    private Integer appointmentId;
    private String transactionType;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String createdAt;
    private String note;

    public Long getTransactionId() { return transactionId; }
    public void setTransactionId(Long transactionId) { this.transactionId = transactionId; }
    public int getPatientId() { return patientId; }
    public void setPatientId(int patientId) { this.patientId = patientId; }
    public Integer getAppointmentId() { return appointmentId; }
    public void setAppointmentId(Integer appointmentId) { this.appointmentId = appointmentId; }
    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(BigDecimal balanceAfter) { this.balanceAfter = balanceAfter; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
