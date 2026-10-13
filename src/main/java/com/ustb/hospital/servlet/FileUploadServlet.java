package com.ustb.hospital.servlet;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.UUID;

import static javax.print.attribute.standard.MediaSizeName.C;

@MultipartConfig
@WebServlet("/doctors/avatar-upload")
public class FileUploadServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("utf-8");

        String docname = req.getParameter("docname");

        //获取文件
        Part part = req.getPart("docavatar");
        String sfn = part.getSubmittedFileName();

        //获取提交的文件名
        String houzhui = sfn.substring(sfn.lastIndexOf("."));
        //重新起一个头像名称  不能重复
        String newname = UUID.randomUUID().toString().replaceAll("-", "");

        sfn=newname+houzhui;

        part.write("C:\\Users\\15801\\Desktop\\pic\\"+sfn);





    }
}
