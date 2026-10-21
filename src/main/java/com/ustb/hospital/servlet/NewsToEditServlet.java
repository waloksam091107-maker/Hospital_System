package com.ustb.hospital.servlet;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.ustb.hospital.entity.News;
import com.ustb.hospital.service.NewsServiceImpl;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/news/edit-page")
public class NewsToEditServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //请求乱码
        req.setCharacterEncoding("utf-8");
        //响应乱码
        resp.setContentType("application/json;charset=UTF-8");

        String id = req.getParameter("newsId");

        NewsServiceImpl service = new NewsServiceImpl();
        News news = service.getNewsById(Integer.parseInt(id));


        ObjectMapper objectMapper = new ObjectMapper();
        String jsonStr = objectMapper.writeValueAsString(news);
        resp.getWriter().write(jsonStr);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }
}
