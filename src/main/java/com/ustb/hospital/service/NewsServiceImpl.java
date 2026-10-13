package com.ustb.hospital.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.ustb.hospital.entity.News;
import com.ustb.hospital.mapper.NewsMapper;
import com.ustb.hospital.utils.MybatisUtil;
import org.apache.ibatis.session.SqlSession;

import java.util.List;

public class NewsServiceImpl {
    public List<News> queryAll(){
        //调用工具类，获取会话对象sqlSession
        SqlSession sqlSession = MybatisUtil.getSqlSession();
        //调用mapper
        NewsMapper mapper = sqlSession.getMapper(NewsMapper.class);
        //查询所有公告信息
        List<News> news = mapper.selectAll();
        //关闭会话
        sqlSession.close();
        return news;
    }
    public List<News> queryNew(){
        //调用工具类，获取会话对象sqlSession
        SqlSession sqlSession = MybatisUtil.getSqlSession();
        //调用mapper
        NewsMapper mapper = sqlSession.getMapper(NewsMapper.class);
        //查询所有公告信息
        List<News> news = mapper.selectNew();
        //关闭会话
        sqlSession.close();
        return news;
    }
    public News queryById(int id){
        //调用工具类，获取会话对象sqlSession
        SqlSession sqlSession = MybatisUtil.getSqlSession();
        //调用mapper
        NewsMapper mapper = sqlSession.getMapper(NewsMapper.class);
        //查询公告信息
        News news = mapper.selectById(id);
        //关闭会话
        sqlSession.close();
        return news;
    }
    public List<News> queryByTitle(String title){
        //调用工具类，获取会话对象sqlSession
        SqlSession sqlSession = MybatisUtil.getSqlSession();
        //调用mapper
        NewsMapper mapper = sqlSession.getMapper(NewsMapper.class);
        //查询公告信息
        List<News> news = mapper.selectByTitle(title);
        //关闭会话
        sqlSession.close();
        return news;
    }

    public PageInfo<News> query(int pageNum, int pageSize, int newsId, String title, String creationTime, String creator, String content, String newsImg){
        SqlSession sqlSession = MybatisUtil.getSqlSession();
        NewsMapper mapper = sqlSession.getMapper(NewsMapper.class);
        PageHelper.startPage(pageNum, pageSize);
        PageInfo<News> pageInfo = new PageInfo<>(mapper.select(newsId,title,creationTime,creator,content,newsImg));
        return pageInfo;
    }

    public void add(String title, String content, String creator, String creationTime, String newsImg){
        SqlSession sqlSession = MybatisUtil.getSqlSession();
        NewsMapper mapper = sqlSession.getMapper(NewsMapper.class);
        News news = new News();
        int id = mapper.getMaxId() + 1;
        news.setNewsId(id);
        news.setTitle(title);
        news.setContent(content);
        news.setCreator(creator);
        news.setCreationTime(creationTime);
        news.setNewsImg(newsImg);
        mapper.insert(news);
        sqlSession.commit();
        sqlSession.close();
    }

    public void update(int id, String title, String content, String creator, String creationTime, String newsImg){
        SqlSession sqlSession = MybatisUtil.getSqlSession();
        NewsMapper mapper = sqlSession.getMapper(NewsMapper.class);
        News news = new News();
        if(title != null && !title.equals("")){
            news.setTitle(title);
        }

        if(content != null && !content.equals("")) {
            news.setContent(content);
        }

        if(creator != null && !creator.equals("")) {
            news.setCreator(creator);
        }

        if(creationTime != null && !creationTime.equals("")) {
            news.setCreationTime(creationTime);
        }

        if(newsImg != null && !newsImg.equals("")) {
            news.setNewsImg(newsImg);
        }

        news.setNewsId(id);
        mapper.update(news.getNewsId(),news.getTitle(),news.getCreationTime(),news.getCreator(),news.getContent(),news.getNewsImg());
        sqlSession.commit();
        sqlSession.close();
    }

    public boolean delete(int id){
        SqlSession sqlSession = MybatisUtil.getSqlSession();
        NewsMapper mapper = sqlSession.getMapper(NewsMapper.class);
        mapper.delete(id);
        sqlSession.commit();
        sqlSession.close();
        return true;
    }

    public News getNewsById(int id) {
        SqlSession sqlSession = MybatisUtil.getSqlSession();
        NewsMapper mapper = sqlSession.getMapper(NewsMapper.class);
        News news = mapper.selectById(id);
        sqlSession.close();
        return news;
    }

}
