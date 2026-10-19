package com.ustb.hospital.entity;

public class Appointments {
    private int appointmentId;
    private int patientId;
    private int doctorId;
    private String appointmentDate;
    private String appointmentTime;
    private Long scheduleSlotId;
    private Integer previousAppointmentId;
    private String status;
    private String startedAt;
    private String completedAt;
    private String cancelledAt;
    private String updatedAt;
    private int version;
    private Patients patients;
    public Patients getPatients() {
        return patients;
    }
    private Doctors doctors;
    private Departments departments;
    private ProfessionalTitles professionalTitles;

    public void setPatients(Patients patients) {
        this.patients = patients;
    }



    public int getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(int appointmentId) {
        this.appointmentId = appointmentId;
    }

    public int getPatientId() {
        return patientId;
    }

    public void setPatientId(int patientId) {
        this.patientId = patientId;
    }

    public int getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(int doctorId) {
        this.doctorId = doctorId;
    }

    public String getAppointmentDate() {
        return appointmentDate;
    }

    public void setAppointmentDate(String appointmentDate) {
        this.appointmentDate = appointmentDate;
    }

    public String getAppointmentTime() {
        return appointmentTime;
    }

    public void setAppointmentTime(String appointmentTime) {
        this.appointmentTime = appointmentTime;
    }

    public Long getScheduleSlotId() { return scheduleSlotId; }
    public void setScheduleSlotId(Long scheduleSlotId) { this.scheduleSlotId = scheduleSlotId; }
    public Integer getPreviousAppointmentId() { return previousAppointmentId; }
    public void setPreviousAppointmentId(Integer previousAppointmentId) { this.previousAppointmentId = previousAppointmentId; }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStartedAt() { return startedAt; }
    public void setStartedAt(String startedAt) { this.startedAt = startedAt; }
    public String getCompletedAt() { return completedAt; }
    public void setCompletedAt(String completedAt) { this.completedAt = completedAt; }
    public String getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(String cancelledAt) { this.cancelledAt = cancelledAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }

    public Doctors getDoctors() {
        return doctors;
    }

    public void setDoctors(Doctors doctors) {
        this.doctors = doctors;
    }

    public Departments getDepartments() {
        return departments;
    }

    public void setDepartments(Departments departments) {
        this.departments = departments;
    }

    public ProfessionalTitles getProfessionalTitles() {
        return professionalTitles;
    }

    public void setProfessionalTitles(ProfessionalTitles professionalTitles) {
        this.professionalTitles = professionalTitles;
    }

    @Override
    public String toString() {
        return "Appointments{" +
                "appointmentId=" + appointmentId +
                ", patientId=" + patientId +
                ", doctorId=" + doctorId +
                ", appointmentDate='" + appointmentDate + '\'' +
                ", appointmentTime='" + appointmentTime + '\'' +
                ", status='" + status + '\'' +
                ", patients=" + patients +
                ", doctors=" + doctors +
                ", departments=" + departments +
                ", professionalTitles=" + professionalTitles +
                '}';
    }
}
