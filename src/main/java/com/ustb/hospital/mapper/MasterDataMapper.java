package com.ustb.hospital.mapper;

import com.ustb.hospital.entity.*;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/** Protected administrator queries; entity names are selected by XML choose, never interpolated. */
public interface MasterDataMapper {
    List<Doctors> searchDoctors(@Param("keyword") String keyword,@Param("limit") int limit,@Param("offset") long offset);
    List<Patients> searchPatients(@Param("keyword") String keyword,@Param("limit") int limit,@Param("offset") long offset);
    List<Departments> searchDepartments(@Param("keyword") String keyword,@Param("limit") int limit,@Param("offset") long offset);
    List<ProfessionalTitles> searchTitles(@Param("keyword") String keyword,@Param("limit") int limit,@Param("offset") long offset);
    long countDoctors(@Param("keyword") String keyword);
    long countPatients(@Param("keyword") String keyword);
    long countDepartments(@Param("keyword") String keyword);
    long countTitles(@Param("keyword") String keyword);
    List<Departments> departmentOptions();
    List<ProfessionalTitles> titleOptions();
    Integer lockEntity(@Param("entity") String entity,@Param("id") int id);
    int countDoctorDates(@Param("id") int id);
    int countDoctorRecords(@Param("id") int id);
    int countDoctorOrders(@Param("id") int id);
    int countPatientRecords(@Param("id") int id);
    int countPatientTransactions(@Param("id") int id);
    int countPatientOrders(@Param("id") int id);
}
