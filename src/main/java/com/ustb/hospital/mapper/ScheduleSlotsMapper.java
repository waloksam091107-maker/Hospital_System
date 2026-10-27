package com.ustb.hospital.mapper;

import com.ustb.hospital.entity.ScheduleSlot;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface ScheduleSlotsMapper {
    ScheduleSlot selectByDoctorDatePeriod(@Param("doctorId") int doctorId,
                                          @Param("scheduleDate") String scheduleDate,
                                          @Param("slotPeriod") String slotPeriod);

    int reserveAvailableSlot(@Param("doctorId") int doctorId,
                             @Param("scheduleDate") String scheduleDate,
                             @Param("slotPeriod") String slotPeriod);

    int releaseBookedSlot(@Param("scheduleSlotId") long scheduleSlotId);

    int insertSlot(@Param("doctorId") int doctorId, @Param("scheduleDate") String scheduleDate,
                   @Param("slotPeriod") String slotPeriod, @Param("slotStatus") String slotStatus);

    int deleteByDoctorAndDate(@Param("doctorId") int doctorId, @Param("scheduleDate") String scheduleDate);

    List<ScheduleSlot> selectForUpdate(@Param("doctorId") int doctorId, @Param("scheduleDate") String scheduleDate);
    int countScheduleHistory(@Param("doctorId") int doctorId, @Param("scheduleDate") String scheduleDate);
    int countActiveAppointments(@Param("doctorId") int doctorId, @Param("scheduleDate") String scheduleDate);
    int countProtectedAppointments(@Param("slot") ScheduleSlot slot);
    int updateUnoccupiedSlot(@Param("slot") ScheduleSlot slot, @Param("newDate") String newDate,
                            @Param("newStatus") String newStatus);
}
