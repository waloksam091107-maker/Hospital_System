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
@WebServlet("/news/add")
public class NewsAddServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doGet(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //处理中文乱码
        req.setCharacterEncoding("UTF-8");

        String title = req.getParameter("title");
        String content = req.getParameter("content");
        String creator = req.getParameter("creator");
        String creationTime = req.getParameter("creationTime");
        //获取文件
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
        //2.调用业务
        NewsServiceImpl service = new NewsServiceImpl();
        try {
            service.add(title, content, creator, creationTime,s);

            //3.页面导航
            //没有异常，保存成功
            resp.sendRedirect("/hospro/newsAdmins.html");
        }
        catch (Exception e) {
            e.printStackTrace();
            resp.sendRedirect("/hospro/newsAdd.html");
        }

    }
}
