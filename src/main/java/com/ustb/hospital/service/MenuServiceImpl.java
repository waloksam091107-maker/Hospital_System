package com.ustb.hospital.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.ustb.hospital.entity.Doctors;
import com.ustb.hospital.entity.Menu;
import com.ustb.hospital.mapper.DoctorsMapper;
import com.ustb.hospital.mapper.MenuMapper;
import com.ustb.hospital.utils.MybatisUtil;
import org.apache.ibatis.session.SqlSession;

import java.util.List;

public class MenuServiceImpl {
    public void delById(String id){
        SqlSession sqlSession = MybatisUtil.getSqlSession();
        MenuMapper mapper = sqlSession.getMapper(MenuMapper.class);


        mapper.delById(id);
    }
    public void updateById(String id,String price,String fenlei){
        SqlSession sqlSession = MybatisUtil.getSqlSession();
        MenuMapper mapper = sqlSession.getMapper(MenuMapper.class);


        mapper.updateById(id,price,fenlei);
    }
    public PageInfo<Menu> queryByPage(int pageNum, String fenlei){
        try (SqlSession sqlSession = MybatisUtil.getSqlSession()) {
            MenuMapper mapper = sqlSession.getMapper(MenuMapper.class);
            PageHelper.startPage(pageNum,2);
            List<Menu> list="-1".equals(fenlei)?mapper.selectAll():mapper.selectByClass(fenlei);
            return new PageInfo<>(list);
        }
    }

    public List<String> queryCategories() {
        try (SqlSession sqlSession = MybatisUtil.getSqlSession()) {
            return sqlSession.getMapper(MenuMapper.class).selectCategories();
        }
    }
}
