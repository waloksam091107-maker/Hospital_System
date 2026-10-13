package com.ustb.hospital.mapper;

import com.ustb.hospital.entity.Appointments;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AppointmentsMapper {
    public void insert(@Param("patientId") int patientId,@Param("doctorId") int doctorId,@Param("appointmentDate") String appointmentDate,@Param("appointmentTime") String appointmentTime,@Param("status") String status);

    List<Appointments> selectAll();
    List<Appointments> selectAllWithDoctorDetails();
    Appointments selectById(int appointmentId);
    void deleteAppointmentById(int appointmentId);
    int updateAppointment(Appointments appointments); // 修改返回类型为int
    int updateStatus(@Param("appointmentId") int appointmentId, @Param("status") String status); // 添加@Param注解
    int updateStatusIfCurrent(@Param("appointmentId") int appointmentId, @Param("doctorId") int doctorId,
                              @Param("fromStatus") String fromStatus, @Param("toStatus") String toStatus);
    int updateAppointmentSlot(@Param("appointmentId") int appointmentId, @Param("appointmentDate") String appointmentDate,
                              @Param("appointmentTime") String appointmentTime, @Param("status") String status);
    int countReservedByDoctorAndDate(@Param("doctorId") int doctorId, @Param("appointmentDate") String appointmentDate);
    List<Appointments> selectByPatientIdWithDoctorDetails(int patientId);
    Appointments selectAppointmentById( int appointmentId);
    List<Appointments> selectByDoctorId(int doctorId);
    List<Appointments> selectByDate(String date);

}
