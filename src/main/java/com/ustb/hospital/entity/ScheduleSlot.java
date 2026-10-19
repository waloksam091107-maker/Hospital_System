package com.ustb.hospital.entity;

public class ScheduleSlot {
    private long scheduleSlotId;
    private int doctorId;
    private String scheduleDate;
    private String slotPeriod;
    private String slotStatus;
    private String createdAt;
    private String updatedAt;

    public long getScheduleSlotId() { return scheduleSlotId; }
    public void setScheduleSlotId(long scheduleSlotId) { this.scheduleSlotId = scheduleSlotId; }
    public int getDoctorId() { return doctorId; }
    public void setDoctorId(int doctorId) { this.doctorId = doctorId; }
    public String getScheduleDate() { return scheduleDate; }
    public void setScheduleDate(String scheduleDate) { this.scheduleDate = scheduleDate; }
    public String getSlotPeriod() { return slotPeriod; }
    public void setSlotPeriod(String slotPeriod) { this.slotPeriod = slotPeriod; }
    public String getSlotStatus() { return slotStatus; }
    public void setSlotStatus(String slotStatus) { this.slotStatus = slotStatus; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
