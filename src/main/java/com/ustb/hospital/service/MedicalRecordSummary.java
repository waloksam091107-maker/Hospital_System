package com.ustb.hospital.service;

import com.ustb.hospital.entity.MedicalRecord;

/** Extra display fields for the new list API; legacy record JSON stays unchanged. */
public class MedicalRecordSummary extends MedicalRecord {
    private String doctorName;
    private String appointmentDate;
    private String appointmentTime;
    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String value) { doctorName = value; }
    public String getAppointmentDate() { return appointmentDate; }
    public void setAppointmentDate(String value) { appointmentDate = value; }
    public String getAppointmentTime() { return appointmentTime; }
    public void setAppointmentTime(String value) { appointmentTime = value; }
}
