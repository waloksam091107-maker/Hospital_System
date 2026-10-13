package com.ustb.hospital.servlet;


import com.ustb.hospital.service.NewsServiceImpl;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.IOException;

@MultipartConfig
@WebServlet("/news/edit")
public class NewsEditServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //请求乱码
        req.setCharacterEncoding("UTF-8");
        //响应乱码
        resp.setContentType("text/html;charset=UTF-8");

        String newsId = req.getParameter("newsId");
        String content = req.getParameter("content");
        String creator = req.getParameter("creator");
        String creationTime = req.getParameter("creationTime");
        String title = req.getParameter("title");
        Part part = req.getPart("newsImg");
        //获取提交的文件名
        String fileName = part.getSubmittedFileName();
String suffix = fileName.substring(fileName.lastIndexOf("."));
        //使用原有文件名
        String s = fileName.substring(0, fileName.lastIndexOf("."));
        fileName = s + suffix;
        //例如：123.jpg
        //s = 123
        //suffix = .jpg
//1.将图片名保存到数据库
        //2.将文件保存到服务器上(静态资源服务器）
        //3.“/"当前项目在tomcat服务器下的根路径
        //获取服务器下的当前项目的真实路径
        String uploadPath = this.getServletContext().getRealPath("/hos_images/");
        part.write(uploadPath + fileName);

        NewsServiceImpl service = new NewsServiceImpl();
        try {
            service.update(Integer.parseInt(newsId),  title,content, creator, creationTime, s);
            //修改成功
            resp.sendRedirect("/hospro/newsAdmins.html?msg=修改成功");
        } catch (Exception e) {

            resp.sendRedirect("/news/edit-page?newsId=" + newsId);
        }

    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }
}
