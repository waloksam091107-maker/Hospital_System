package com.ustb.hospital.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.PageInfo;
import com.ustb.hospital.entity.News;
import com.ustb.hospital.service.NewsServiceImpl;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/news/select")
public class NewsSelectServlet extends HttpServlet {
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //请求乱码
        req.setCharacterEncoding("utf-8");
        //响应乱码
        resp.setContentType("application/json;charset=UTF-8");
        //获取参数---都是字符串
        String pageNum = req.getParameter("pageNum");

        int i = 1;
        if(pageNum!= null&&!pageNum.equals("")){
            i = Integer.parseInt(pageNum);
        }


        String title = req.getParameter("title");
        String content = req.getParameter("content");
        String creationTime = req.getParameter("creationTime");
        String creator = req.getParameter("creator");
        String newsImg = req.getParameter("newsImg");

        NewsServiceImpl service = new NewsServiceImpl();

        PageInfo<News> pageInfo = service.query(i,4, -1, title, creationTime, creator,content, newsImg);
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonStr = objectMapper.writeValueAsString(pageInfo);
        resp.getWriter().write(jsonStr);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }
}
