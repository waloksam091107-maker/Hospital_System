package com.ustb.hospital.mapper;

import com.ustb.hospital.entity.ProfessionalTitles;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface ProfessionalTitlesMapper {
    List<ProfessionalTitles> selectAll();
    ProfessionalTitles selectById(@Param("id") int id);
    ProfessionalTitles selectByName(@Param("titleName") String titleName);
    int insert(ProfessionalTitles title);
    int update(ProfessionalTitles title);
    int countDoctorsByTitleId(@Param("id") int id);
    int delete(@Param("id") int id);
}
