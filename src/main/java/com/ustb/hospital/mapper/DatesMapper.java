package com.ustb.hospital.mapper;

import com.ustb.hospital.entity.Dates;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface DatesMapper {

    Dates findByDoctorAndDate(@Param("doctorId") int doctorId,@Param("date") String date);
    List<Dates> selectByDoctorId(int doctorId);
    List<Dates> selectAll();
    Dates selectById(int dateId);
    Dates selectByIdForUpdate(int dateId);
    int updateDate(Dates dates);
    int deleteDateById(int dateId);
    public int insertDate(Dates dates);
    int reserveSlot(@Param("doctorId") int doctorId, @Param("date") String date, @Param("slot") String slot);
    int releaseSlot(@Param("doctorId") int doctorId, @Param("date") String date, @Param("slot") String slot);
}
