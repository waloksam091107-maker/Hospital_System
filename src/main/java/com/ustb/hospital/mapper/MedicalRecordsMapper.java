package com.ustb.hospital.mapper;

import com.ustb.hospital.entity.MedicalOrder;
import com.ustb.hospital.entity.MedicalRecord;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface MedicalRecordsMapper {
    int insertRecord(MedicalRecord record);
    int updateDraft(@Param("record") MedicalRecord record, @Param("expectedVersion") int expectedVersion);
    int deleteOrdersByRecordId(@Param("medicalRecordId") long medicalRecordId);
    int insertOrders(@Param("orders") List<MedicalOrder> orders);
    MedicalRecord selectByAppointmentForUpdate(@Param("appointmentId") int appointmentId);
    MedicalRecord selectForDoctor(@Param("appointmentId") int appointmentId, @Param("doctorId") int doctorId);
    MedicalRecord selectForPatient(@Param("appointmentId") int appointmentId, @Param("patientId") int patientId);
    List<MedicalRecord> selectAllForPatient(@Param("patientId") int patientId);
    List<MedicalOrder> selectOrdersByRecordId(@Param("medicalRecordId") long medicalRecordId);
}
