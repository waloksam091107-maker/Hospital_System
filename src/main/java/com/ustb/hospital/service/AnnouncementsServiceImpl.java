package com.ustb.hospital.service;

import com.ustb.hospital.entity.Announcements;
import com.ustb.hospital.mapper.AnnouncementsMapper;
import com.ustb.hospital.utils.MybatisUtil;
import org.apache.ibatis.session.SqlSession;

import java.util.List;

public class AnnouncementsServiceImpl {
    public List<Announcements> queryAll(){
        //调用工具类，获取会话对象sqlSession
        SqlSession sqlSession = MybatisUtil.getSqlSession();
        //调用mapper
        AnnouncementsMapper mapper = sqlSession.getMapper(AnnouncementsMapper.class);
        //查询所有公告信息
        List<Announcements> announcements = mapper.selectPublished();
        //关闭会话
        sqlSession.close();
        return announcements;
    }
}
