package com.ustb.hospital.mapper;

import com.ustb.hospital.entity.Patients;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface PatientsMapper {
    void pay(@Param("idCardNumber") String idCardNumber, @Param("amount") double amount);
    Patients selectByEmail(String email);
    Patients selectByIdCardNumber(String idCardNumber);
    public Patients getpatById(int patientId);
    public void updateBal(@Param("patientId") int patientId,@Param("balance") String balance);
    int deductBalanceIfEnough(@Param("patientId") int patientId, @Param("fee") double fee);
    public Patients getBalById(int patientId);
    void updatePassword(String name,String password);
    int upgradePasswordHash(@Param("patientId") int patientId, @Param("passwordHash") String passwordHash);
    Patients selectForPasswordUpdate(@Param("patientId") int patientId);
    int getMaxId();
    void updateAvatar(@Param("patientId") int patientId, @Param("avatar") String avatar);
    public int updatePatientProfileById(@Param("patientId")int patientId,@Param("phone")String phone, @Param("balance")String balance, @Param("email")String email);

    void save(@Param("patientId") int patientId,@Param("idCardNumber") String idCardNumber,@Param("password") String password,@Param("name") String name,@Param("avatar") String avatar,@Param("phone") String phone,@Param("email") String email,@Param("balance") String balance);
    List<Patients> selectAll();

    Patients selectByName(String name);

    public List<Patients> searchPatients(@Param("idCardNumber") String idCardNumber, @Param("name") String name, @Param("phone") String phone);

    public Patients selectById(int id);
    public int delete(int id);
    public int getPatientMaxId();
    public int insertPatient(Patients patients);
    public int updateById(@Param("patientId")int patientId,@Param("phone")String phone, @Param("balance")String balance);

}
