package com.ustb.hospital.mapper;

import com.ustb.hospital.entity.Admins;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AdminsMapper {
    void updatePassword(String name,String password);
    int upgradePasswordHash(@Param("adminId") int adminId, @Param("passwordHash") String passwordHash);
    Admins selectForPasswordUpdate(@Param("adminId") int adminId);


    void save(@Param("adminId") int adminId,@Param("username") String username,@Param("password") String password,@Param("name") String name,@Param("avatar") String avatar,@Param("phone") String phone,@Param("email") String email);
    int getMaxId();
    //比对信息，判断登录
    Admins selectByUsername(String username);
    public List<Admins> selectAll(@Param("username") String username, @Param("name") String name, @Param("phone") String phone);
    Admins selectById(int adminId);
    void updateAvatar(@Param("adminId") int patientId, @Param("avatar") String avatar);
    public int updateById(@Param("adminId")int adminId,@Param("username")String username,@Param("phone")String phone, @Param("email")String email);





}
