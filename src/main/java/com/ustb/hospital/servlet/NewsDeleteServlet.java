package com.ustb.hospital.servlet;

import com.ustb.hospital.service.NewsServiceImpl;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/news/delete")
public class NewsDeleteServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //请求乱码
        req.setCharacterEncoding("utf-8");
        //响应乱码
        resp.setContentType("text/html;charset=utf-8");
        //1.获取请求参数
        String newsId = req.getParameter("newsId");
        NewsServiceImpl service = new NewsServiceImpl();
        boolean success = service.delete(Integer.parseInt(newsId));
        resp.setContentType("application/json;charset=utf-8");
        resp.getWriter().write("{\"success\": " + success + "}");

    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }
}
