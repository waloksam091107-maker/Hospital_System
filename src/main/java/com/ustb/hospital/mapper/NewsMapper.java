package com.ustb.hospital.mapper;

import com.ustb.hospital.entity.News;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface NewsMapper {
    List<News> selectAll();
    List<News> selectNew();
    News selectById(int newsId);
    List<News> selectByTitle(String newsTitle);
    void insert(News news);
    void update(@Param("newsId") int newsId, @Param("title") String title, @Param("creationTime") String creationTime, @Param("creator") String creator, @Param("content") String content, @Param("newsImg") String newsImg);
    void delete(int newsId);
    int getMaxId();

    List<News> select(@Param("newsId") int newsId, @Param("title") String title, @Param("creationTime") String creationTime, @Param("creator") String creator, @Param("content") String content, @Param("newsImg") String newsImg);
}
