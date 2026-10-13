package com.ustb.hospital.entity;

public class Dates {
    private int dateId;
    private int doctorId;
    private String doctorName;
    private String Date;
    /**
     * Database column {@code shang}: morning appointment availability.
     * {@code "1"} means available; {@code "0"} means unavailable.
     */
    private String shang;

    /**
     * Database column {@code xia}: afternoon appointment availability.
     * {@code "1"} means available; {@code "0"} means unavailable.
     */
    private String xia;

    /**
     * Database column {@code wan}: evening appointment availability.
     * {@code "1"} means available; {@code "0"} means unavailable.
     */
    private String wan;

    public int getDateId() {
        return dateId;
    }

    public void setDateId(int dateId) {
        this.dateId = dateId;
    }

    public int getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(int doctorId) {
        this.doctorId = doctorId;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public String getShang() {
        return shang;
    }

    public void setShang(String shang) {
        this.shang = shang;
    }

    public String getXia() {
        return xia;
    }

    public void setXia(String xia) {
        this.xia = xia;
    }

    public String getWan() {
        return wan;
    }

    public void setWan(String wan) {
        this.wan = wan;
    }

    public String getDate() {
        return Date;
    }

    public void setDate(String date) {
        Date = date;
    }

    @Override
    public String toString() {
        return "Dates{" +
                "dateId=" + dateId +
                ", doctorId=" + doctorId +
                ", doctorName='" + doctorName + '\'' +
                ", Date='" + Date + '\'' +
                ", shang='" + shang + '\'' +
                ", xia='" + xia + '\'' +
                ", wan='" + wan + '\'' +
                '}';
    }
}
