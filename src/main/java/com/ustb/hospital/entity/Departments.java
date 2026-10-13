package com.ustb.hospital.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Departments {
    private int departmentId;
    private String departmentName;
    private Integer departmentPid;
    private int departmentLevel;
    private String departmentPath;
    private String departmentDescription;

    public int getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(int departmentId) {
        this.departmentId = departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public Integer getDepartmentPid() {
        return departmentPid;
    }

    public void setDepartmentPid(Integer departmentPid) {
        this.departmentPid = departmentPid;
    }

    public int getDepartmentLevel() {
        return departmentLevel;
    }

    public void setDepartmentLevel(int departmentLevel) {
        this.departmentLevel = departmentLevel;
    }

    public String getDepartmentPath() {
        return departmentPath;
    }

    public void setDepartmentPath(String departmentPath) {
        this.departmentPath = departmentPath;
    }

    @JsonProperty("departmentDiscription")
    public String getDepartmentDescription() {
        return departmentDescription;
    }

    @JsonProperty("departmentDiscription")
    public void setDepartmentDescription(String departmentDescription) {
        this.departmentDescription = departmentDescription;
    }

    @Override
    public String toString() {
        return "Departments{" +
                "departmentId=" + departmentId +
                ", departmentName='" + departmentName + '\'' +
                ", departmentPid=" + departmentPid +
                ", departmentLevel=" + departmentLevel +
                ", departmentPath='" + departmentPath + '\'' +
                ", departmentDescription='" + departmentDescription + '\'' +
                '}';
    }
}
