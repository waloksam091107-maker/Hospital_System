package com.ustb.hospital.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.ustb.hospital.entity.Doctors;
import com.ustb.hospital.mapper.DepartmentsMapper;
import com.ustb.hospital.mapper.DoctorsMapper;
import com.ustb.hospital.security.PasswordService;
import com.ustb.hospital.utils.MybatisUtil;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.SqlSession;

import java.sql.SQLException;
import java.util.List;

public class DoctorsServiceImpl {
    public void updatePassword(String jobNumber,String password){
        SqlSession sqlSession = MybatisUtil.getSqlSession();
        DoctorsMapper mapper = sqlSession.getMapper(DoctorsMapper.class);
        try { mapper.updatePassword(jobNumber, PasswordService.hash(password)); }
        finally { sqlSession.close(); }
    }

    public void upgradePasswordHash(int doctorId, String passwordHash) {
        SqlSession session = MybatisUtil.getSqlSession();
        try { session.getMapper(DoctorsMapper.class).upgradePasswordHash(doctorId, passwordHash); }
        finally { session.close(); }
    }

    public Doctors selectByJobNumber(String jobNumber){
        SqlSession sqlSession = MybatisUtil.getSqlSession();
        try {
            return sqlSession.getMapper(DoctorsMapper.class).selectByJobNumber(jobNumber);
        } finally { sqlSession.close(); }
    }

    public Doctors findById(int doctorId){
        SqlSession sqlSession = MybatisUtil.getSqlSession();
        try {
            return sqlSession.getMapper(DoctorsMapper.class).findById(doctorId);
        } finally { sqlSession.close(); }
    }
    public PageInfo<Doctors> queryByDeptIdToPage(int pageNum,int pageSize,int departmentId){
        SqlSession sqlSession = MybatisUtil.getSqlSession();
        try {
            DoctorsMapper mapper = sqlSession.getMapper(DoctorsMapper.class);
            PageHelper.startPage(pageNum,pageSize);
            List<Doctors> list = mapper.selectAll("","",departmentId);
            return new PageInfo<Doctors>(list);
        } finally {
            sqlSession.close();
        }
    }

    public void delById(int doctorId){
        SqlSession sqlSession = MybatisUtil.getSqlSession();
        DoctorsMapper mapper = sqlSession.getMapper(DoctorsMapper.class);
        mapper.delById(doctorId);
    }
    public PageInfo<Doctors> queryByPage(int pageNum,int pageSize,String jobNumber,String name,int deptId){
        SqlSession sqlSession = MybatisUtil.getSqlSession();
        try {
            DoctorsMapper mapper = sqlSession.getMapper(DoctorsMapper.class);
            PageHelper.startPage(pageNum,pageSize);
        List<Doctors> list = mapper.selectAll(jobNumber,name,deptId);
            return new PageInfo<Doctors>(list);
        } finally {
            sqlSession.close();
        }
    }
    public void update(int doctorId, String password, String name, String avatar, String phone, String email, String introduction, String registrationFee, String entryDate , int departmentId, int professionalTitleId){
        SqlSession sqlSession = MybatisUtil.getSqlSession();
        DoctorsMapper mapper = sqlSession.getMapper(DoctorsMapper.class);
        mapper.update(doctorId,password,name,avatar,phone,email,introduction,registrationFee,entryDate,departmentId,professionalTitleId);
    }

    public int save( String password,  String name, String avatar,  String phone, String email, String introduction,  String registrationFee, String entryDate,int departmentId, int professionalTitleId ){

        SqlSession sqlSession = MybatisUtil.getSqlSession();
        DoctorsMapper mapper = sqlSession.getMapper(DoctorsMapper.class);
        int doctorId=0;
        String jobNumber = mapper.getMaxJobNumber();

        doctorId= mapper.getMaxDoctorId()+1;
        int jobNumber_=Integer.parseInt(jobNumber )+1;
        jobNumber = Integer.toString(jobNumber_);


        mapper.save( doctorId,   jobNumber,   PasswordService.hash(password),   name,  avatar,   phone,  email,  introduction,   registrationFee, entryDate, departmentId,  professionalTitleId);
        return jobNumber_;  //返回工号


    }
    public void createDoctor(String docName,String avatar,String password,String phone,String email,String introduction,String registrationFee,int deptId,String entryDate,int professionalTitles){
        SqlSession sqlSession=MybatisUtil.getSqlSession();
        DoctorsMapper mapper=sqlSession.getMapper(DoctorsMapper.class);
        try {
            Doctors doctorsNew = new Doctors();

            int maxId = mapper.getMaxdoctorId();
            String maxJobNumberStr = mapper.getMaxJobNumber();
            int maxJobNumberInt = Integer.parseInt(maxJobNumberStr);
            int newJobNumberInt = maxJobNumberInt + 1;
            String newJobNumberStr = String.valueOf(newJobNumberInt);


            doctorsNew.setDoctorId(maxId+1);
            doctorsNew.setJobNumber(newJobNumberStr);
            doctorsNew.setName(docName);
            doctorsNew.setAvatar(avatar);
            doctorsNew.setPassword(null);
            doctorsNew.setPasswordHash(PasswordService.hash(password));
            doctorsNew.setPhone(phone);
            doctorsNew.setEmail(email);
            doctorsNew.setIntroduction(introduction);
            doctorsNew.setRegistrationFee(registrationFee);
            doctorsNew.setDepartmentId(deptId);
            doctorsNew.setEntryDate(entryDate);
            doctorsNew.setProfessionalTitleId(professionalTitles);
            // 插入新的医生记录
            mapper.insert(doctorsNew);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
        }
    }
    public void delete(int doctorId) throws Exception {
SqlSession sqlSession = null;
        try {
            sqlSession = MybatisUtil.getSqlSession();
            DoctorsMapper mapper = sqlSession.getMapper(DoctorsMapper.class);
            Doctors doctor = mapper.selectById(doctorId);
            if (doctor == null) {

                throw new Exception("要删除的患者不存在");
            }
// 删除操作
            mapper.delete(doctorId);
sqlSession.commit();

        } catch (SQLException e) {
            e.printStackTrace();
            throw new Exception("删除失败，数据库操作异常");
        } finally {
            // 关闭SqlSession
            if (sqlSession != null) {
                sqlSession.close();
            }
        }
    }
    public Doctors queryById(int doctorId){
        SqlSession sqlSession = MybatisUtil.getSqlSession();
        DoctorsMapper mapper = sqlSession.getMapper(DoctorsMapper.class);
        Doctors doctor = mapper.selectById(doctorId);
        return doctor;
    }
    public void updateDoctorProfile(int doctorId,String phone,String registrationFee,String professionalTitleId){
        //调用工具类，获取会话对象sqlSession
        SqlSession sqlSession = MybatisUtil.getSqlSession();
        //调用mapper
        DoctorsMapper mapper = sqlSession.getMapper(DoctorsMapper.class);
        //调用方法
mapper.updateById(doctorId,phone,registrationFee, professionalTitleId);
sqlSession.commit();
        sqlSession.close();
    }
    public void updateDoctorProfileDetails(int doctorId, String phone,String email,String introduction,String registrationFee,String entryDate,int departmentId, String professionalTitleId){
        //调用工具类，获取会话对象sqlSession
        SqlSession sqlSession = MybatisUtil.getSqlSession();
        //调用mapper
        DoctorsMapper mapper = sqlSession.getMapper(DoctorsMapper.class);
        //调用方法
        //doctorId,phone,email,introduction,registrationFee, entryDate,departmentId,professionalTitleId
mapper.updateDoctorProfileDetails(doctorId,phone,email,introduction,registrationFee, entryDate,departmentId,professionalTitleId);
sqlSession.commit();
        sqlSession.close();
    }
    public Doctors getDoctorById(int doctorId) {
        SqlSession sqlSession = MybatisUtil.getSqlSession();
        try {
            return sqlSession.getMapper(DoctorsMapper.class).selectById(doctorId);
        } finally {
            sqlSession.close();
        }
    }
    public void updateAvatar(int doctorId, String avatar) {
SqlSession sqlSession = MybatisUtil.getSqlSession();
DoctorsMapper mapper = sqlSession.getMapper(DoctorsMapper.class);
        try {
            mapper.updateAvatar(doctorId, avatar);
sqlSession.commit();
        } catch (Exception e) {
            sqlSession.rollback();
            throw new RuntimeException("更新头像失败：" + e.getMessage());
        } finally {
            sqlSession.close();
        }
    }
}
