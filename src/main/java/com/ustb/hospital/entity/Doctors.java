package com.ustb.hospital.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Date;

public class Doctors {


    private int doctorId;
    private String jobNumber;
    private String password;
    private String passwordHash;
    private String name;
    private String avatar;
    private String phone;
    private String email;
    private String introduction;
    private String registrationFee;
    private String entryDate;
    private int departmentId;
    private int professionalTitleId;
    private Departments departments;
    private ProfessionalTitles professionalTitles;
    private Appointments appointments;

    public int getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(int doctorId) {
        this.doctorId = doctorId;
    }

    public String getJobNumber() {
        return jobNumber;
    }

    public void setJobNumber(String jobNumber) {
        this.jobNumber = jobNumber;
    }

    @JsonIgnore
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @JsonIgnore
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getIntroduction() {
        return introduction;
    }

    public void setIntroduction(String introduction) {
        this.introduction = introduction;
    }

    public String getRegistrationFee() {
        return registrationFee;
    }

    public void setRegistrationFee(String registrationFee) {
        this.registrationFee = registrationFee;
    }

    public String getEntryDate() {
        return entryDate;
    }

    public void setEntryDate(String entryDate) {
        this.entryDate = entryDate;
    }

    public int getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(int departmentId) {
        this.departmentId = departmentId;
    }

    public int getProfessionalTitleId() {
        return professionalTitleId;
    }

    public void setProfessionalTitleId(int professionalTitleId) {
        this.professionalTitleId = professionalTitleId;
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
    public Appointments getAppointments() {
        return appointments;
    }

    public void setAppointments(Appointments appointments) {
        this.appointments = appointments;
    }


    @Override
    public String toString() {
        return "Doctors{" +
                "doctorId=" + doctorId +
                ", jobNumber='" + jobNumber + '\'' +
                ", name='" + name + '\'' +
                ", avatar='" + avatar + '\'' +
                ", phone='" + phone + '\'' +
                ", email='" + email + '\'' +
                ", introduction=" + introduction +
                ", registrationFee=" + registrationFee +
                ", entryDate=" + entryDate +
                ", departmentId=" + departmentId +
                ", professionalTitleId=" + professionalTitleId +
                ", departments=" + departments +
                ", professionalTitles=" + professionalTitles +
                ", appointments=" + appointments +
                '}';
    }
}
