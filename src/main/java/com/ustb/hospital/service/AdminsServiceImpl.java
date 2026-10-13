package com.ustb.hospital.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.ustb.hospital.entity.Admins;
import com.ustb.hospital.mapper.AdminsMapper;
import com.ustb.hospital.mapper.PatientsMapper;
import com.ustb.hospital.utils.MybatisUtil;
import org.apache.ibatis.session.SqlSession;

import java.util.List;

public class AdminsServiceImpl {
    public void updatePassword(String name,String password){
        SqlSession sqlSession= MybatisUtil.getSqlSession();
        AdminsMapper mapper=sqlSession.getMapper(AdminsMapper.class);
        mapper.updatePassword(name,password);
    }
    public Admins queryByUsername(String username){
        SqlSession sqlSession= MybatisUtil.getSqlSession();
        try {
            return sqlSession.getMapper(AdminsMapper.class).selectByUsername(username);
        } finally {
            sqlSession.close();
        }
    }

    public void save(String username,String password,String name,String avatar,String phone,String email){
        SqlSession sqlSession = MybatisUtil.getSqlSession();
        AdminsMapper mapper=sqlSession.getMapper(AdminsMapper.class);
        int adminId=0;
        adminId= mapper.getMaxId()+1;
        mapper.save(adminId,username,password,name,avatar,phone,email);
    }
    public PageInfo<Admins> queryByPage(int pageNum, int pageSize, String username, String name, String phone){
        SqlSession sqlSession = MybatisUtil.getSqlSession();
        AdminsMapper mapper = sqlSession.getMapper(AdminsMapper.class);
        PageHelper.startPage(pageNum,pageSize);
List<Admins> list = mapper.selectAll(username,name,phone);
PageInfo<Admins> pageInfo = new PageInfo<>(list);
        return pageInfo;
    }

    public Admins queryById(int adminId){
        SqlSession sqlSession=MybatisUtil.getSqlSession();
        AdminsMapper mapper = sqlSession.getMapper(AdminsMapper.class);
        return  mapper.selectById(adminId);

    }
    public void updateAvatar(int adminId, String avatar) {
        SqlSession sqlSession = MybatisUtil.getSqlSession();
        AdminsMapper mapper = sqlSession.getMapper(AdminsMapper.class);
        try {
            mapper.updateAvatar(adminId, avatar);
            sqlSession.commit();
        } catch (Exception e) {
            sqlSession.rollback();
            throw new RuntimeException("更新头像失败：" + e.getMessage());
        } finally {
            sqlSession.close();
        }
    }
    public void update(int adminId,String username,String phone,String email){
        //调用工具类，获取会话对象sqlSession
        SqlSession sqlSession = MybatisUtil.getSqlSession();
        //调用mapper
        AdminsMapper mapper = sqlSession.getMapper(AdminsMapper.class);
        //调用方法
mapper.updateById(adminId,username,phone,email);
sqlSession.commit();
        sqlSession.close();
    }


}
