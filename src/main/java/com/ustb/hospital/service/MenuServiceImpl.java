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
        SqlSession sqlSession = MybatisUtil.getSqlSession();
        MenuMapper mapper = sqlSession.getMapper(MenuMapper.class);
        PageHelper.startPage(pageNum,2);
        if("-1".equals(fenlei)){
            //全选
            List<Menu> list=mapper.selectAll();
            PageInfo<Menu> pageInfo=new PageInfo<>(list);
            return pageInfo;

        }else{
            List<Menu> list=mapper.selectByClass(fenlei);
            PageInfo<Menu> pageInfo=new PageInfo<>(list);
            return pageInfo;
        }







    }

    public List<String> queryCategories() {
        SqlSession sqlSession = MybatisUtil.getSqlSession();
        MenuMapper mapper = sqlSession.getMapper(MenuMapper.class);
        return mapper.selectCategories();
    }
}
