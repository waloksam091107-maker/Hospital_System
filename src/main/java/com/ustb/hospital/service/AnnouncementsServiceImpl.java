package com.ustb.hospital.service;

import com.ustb.hospital.entity.Announcements;
import com.ustb.hospital.mapper.AnnouncementsMapper;
import com.ustb.hospital.utils.MybatisUtil;
import org.apache.ibatis.session.SqlSession;

import java.util.List;

public class AnnouncementsServiceImpl {
    public List<Announcements> queryAll(){
        try (SqlSession sqlSession = MybatisUtil.getSqlSession()) {
            return sqlSession.getMapper(AnnouncementsMapper.class).selectPublished();
        }
    }
}
