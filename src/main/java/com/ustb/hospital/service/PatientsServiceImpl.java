package com.ustb.hospital.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.ustb.hospital.entity.Patients;
import com.ustb.hospital.mapper.PatientsMapper;
import com.ustb.hospital.security.PasswordService;
import com.ustb.hospital.utils.MybatisUtil;
import org.apache.ibatis.session.SqlSession;

import java.sql.SQLException;
import java.util.List;

public class PatientsServiceImpl {

    public boolean pay(String idCardNumber, double money) {
        //调用工具类，获取会话对象sqlSession
        SqlSession sqlSession = MybatisUtil.getSqlSession();
        //调用mapper
        PatientsMapper mapper = sqlSession.getMapper(PatientsMapper.class);
        //修改患者余额
        mapper.pay(idCardNumber, money);
        //提交事务
        sqlSession.commit();
        //关闭会话
        sqlSession.close();
        return true;
    }
    public Patients queryByEmail(String email){
        //调用工具类，获取会话对象sqlSession
        SqlSession sqlSession = MybatisUtil.getSqlSession();
        //调用mapper
        PatientsMapper mapper = sqlSession.getMapper(PatientsMapper.class);
        //查询所有公告信息
        Patients patient = mapper.selectByEmail(email);
        //关闭会话
        sqlSession.close();
        return patient;
    }

    public Patients queryByIdCardNumber(String idCardNumber) {
        //调用工具类，获取会话对象sqlSession
        SqlSession sqlSession = MybatisUtil.getSqlSession();
        //调用mapper
        PatientsMapper mapper = sqlSession.getMapper(PatientsMapper.class);
        //查询所有公告信息
        Patients patient = mapper.selectByIdCardNumber(idCardNumber);
        //关闭会话
        sqlSession.close();
        return patient;
    }
    public Patients getpatById(int patientId){
        SqlSession sqlSession=MybatisUtil.getSqlSession();
        try {
            return sqlSession.getMapper(PatientsMapper.class).getpatById(patientId);
        } finally {
            sqlSession.close();
        }
    }
    public Patients getPatientById(int patientId) {
        SqlSession sqlSession = MybatisUtil.getSqlSession();
        PatientsMapper mapper = sqlSession.getMapper(PatientsMapper.class);
        try {
            return mapper.selectById(patientId);
        } finally {
            sqlSession.close();
        }
    }
    public void updateAvatar(int patientId, String avatar) {
        SqlSession sqlSession = MybatisUtil.getSqlSession();
        PatientsMapper mapper = sqlSession.getMapper(PatientsMapper.class);
        try {
            mapper.updateAvatar(patientId, avatar);
            sqlSession.commit();
        } catch (Exception e) {
            sqlSession.rollback();
            throw new RuntimeException("更新头像失败：" + e.getMessage());
        } finally {
            sqlSession.close();
        }
    }
    public void updateBal(int patientId,String balance){
        SqlSession sqlSession=MybatisUtil.getSqlSession();
        PatientsMapper mapper=sqlSession.getMapper(PatientsMapper.class);
        mapper.updateBal(patientId,balance);
    }
    public String getBalById(int patientId){
        SqlSession sqlSession=MybatisUtil.getSqlSession();
        PatientsMapper mapper=sqlSession.getMapper(PatientsMapper.class);
        Patients patients =mapper.getBalById(patientId);
        return patients.getBalance();
    }
    public void updatePassword(String name,String password){
        SqlSession sqlSession = MybatisUtil.getSqlSession();
        PatientsMapper mapper=sqlSession.getMapper(PatientsMapper.class);
        try { mapper.updatePassword(name, PasswordService.hash(password)); }
        finally { sqlSession.close(); }

    }

    public void upgradePasswordHash(int patientId, String passwordHash) {
        SqlSession session = MybatisUtil.getSqlSession();
        try { session.getMapper(PatientsMapper.class).upgradePasswordHash(patientId, passwordHash); }
        finally { session.close(); }
    }

    //                patient_id
//                        id_card_number
//                password
//                        name
//                avatar
//                        phone
//                email
//                        balance



    public void save(String idCardNumber,String password,String name,String avatar,String phone,String email,String balance){
        SqlSession sqlSession = MybatisUtil.getSqlSession();
        PatientsMapper mapper=sqlSession.getMapper(PatientsMapper.class);
        int patientId=0;
        patientId= mapper.getMaxId()+1;


        mapper.save(patientId,"",PasswordService.hash(password),name,"","","","");

    }
    public Patients selectByName(String name){
        SqlSession sqlSession = MybatisUtil.getSqlSession();
        try {
            return sqlSession.getMapper(PatientsMapper.class).selectByName(name);
        } finally { sqlSession.close(); }
    }
    public PageInfo<Patients> queryAll(int pageNum, int pageSize){
        SqlSession sqlSession = MybatisUtil.getSqlSession();
        PatientsMapper mapper=sqlSession.getMapper(PatientsMapper.class);
        PageHelper.startPage(pageNum,pageSize); //
        List<Patients> list=mapper.selectAll();
        PageInfo pageInfo=new PageInfo(list);
        return pageInfo;
    }
    public PageInfo<Patients> queryByPage(int pageNum, int pageSize, String idCardNumber, String name, String phone){
        SqlSession sqlSession = MybatisUtil.getSqlSession();
        PatientsMapper mapper = sqlSession.getMapper(PatientsMapper.class);
PageHelper.startPage(pageNum,pageSize);
List<Patients> list = mapper.searchPatients(idCardNumber,name,phone);
PageInfo<Patients> pageInfo = new PageInfo<>(list);
        return pageInfo;
    }
    public void delete(int patientId) throws Exception {
        SqlSession sqlSession = null;
        try {
            sqlSession = MybatisUtil.getSqlSession();
            PatientsMapper mapper = sqlSession.getMapper(PatientsMapper.class);
// 检查要删除的部门是否存在
            Patients patient = mapper.selectById(patientId);
if (patient == null) {
                throw new Exception("要删除的患者不存在");
            }
            // 删除操作
mapper.delete(patientId);
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
    public void update(int patientId,String phone,String balance){
        //调用工具类，获取会话对象sqlSession
        SqlSession sqlSession = MybatisUtil.getSqlSession();
        //调用mapper
        PatientsMapper mapper = sqlSession.getMapper(PatientsMapper.class);
        //调用方法
        mapper.updateById(patientId,phone,balance);
sqlSession.commit();
        sqlSession.close();
    }
    public void updatePatientProfile(int patientId,String phone,String balance,String email){
        //调用工具类，获取会话对象sqlSession
        SqlSession sqlSession = MybatisUtil.getSqlSession();
        //调用mapper
        PatientsMapper mapper = sqlSession.getMapper(PatientsMapper.class);
        //调用方法
        mapper.updatePatientProfileById(patientId,phone,balance,email);
sqlSession.commit();
        sqlSession.close();
    }
    public Patients queryById(int patientId){
        SqlSession sqlSession = MybatisUtil.getSqlSession();
        PatientsMapper mapper = sqlSession.getMapper(PatientsMapper.class);
        Patients patient = mapper.selectById(patientId);
        return patient;

    }
}
