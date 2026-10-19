
package com.ustb.hospital.mapper;

import com.ustb.hospital.entity.Doctors;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface DoctorsMapper {
    public void updatePassword(String jobNumber,String password);
    int upgradePasswordHash(@Param("doctorId") int doctorId, @Param("passwordHash") String passwordHash);
    Doctors selectForPasswordUpdate(@Param("doctorId") int doctorId);

    public Doctors selectByJobNumber(String jobNumber);
    public Doctors findById(int doctorId);
    public int getMaxDoctorId();
    public String getMaxJobNumber();
    //多条件分页直接查询 工号，姓名，科室id
    public List<Doctors> selectAll(@Param("jobNumber") String jobNumber, @Param("name") String name, @Param("deptId") int deptId);
    public void save(@Param("doctorId") int doctorId,@Param("jobNumber") String jobNumber,@Param("password") String password,@Param("name") String name,@Param("avatar") String avatar,@Param("phone") String phone,@Param("email") String email,@Param("introduction") String introduction,@Param("registrationFee") String registrationFee,@Param("entryDate") String entryDate ,@Param("departmentId") int departmentId,@Param("professionalTitleId") int professionalTitleId );
    public void update(@Param("doctorId") int doctorId,@Param("password") String password,@Param("name") String name,@Param("avatar") String avatar,@Param("phone") String phone,@Param("email") String email,@Param("introduction") String introduction,@Param("registrationFee") String registrationFee,@Param("entryDate") String entryDate ,@Param("departmentId") int departmentId,@Param("professionalTitleId") int professionalTitleId );
    public void delById(@Param("doctorId") int doctorId);
    public Integer getMaxdoctorId();
    public int insert(Doctors doctors);
    public Doctors selectById(int id);
    public int delete(int id);
    public int updateDoctorProfileDetails(@Param("doctorId")int doctorId,@Param("phone")String phone,@Param("email")String email,@Param("introduction")String introduction, @Param("registrationFee")String registrationFee, @Param("entryDate")String entryDate,@Param("departmentId")int departmentId, @Param("professionalTitleId")String professionalTitleId);

    public int updateById(@Param("doctorId")int doctorId,@Param("phone")String phone, @Param("registrationFee")String registrationFee,  @Param("professionalTitleId")String professionalTitleId);

    public int getDocMaxId();
    public int insertDoc(Doctors doctors);
    void updateAvatar(@Param("doctorId") int doctorId, @Param("avatar") String avatar);



}
