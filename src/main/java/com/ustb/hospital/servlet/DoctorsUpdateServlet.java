package com.ustb.hospital.servlet;

import com.ustb.hospital.service.DepartmentsServiceImpl;
import com.ustb.hospital.service.DoctorsServiceImpl;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.IOException;
import java.util.UUID;
@MultipartConfig
@WebServlet("/doc/update")
public class DoctorsUpdateServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req,resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("utf-8");



        String doctorId_=req.getParameter("doctorId");
        int doctorId=Integer.parseInt(doctorId_);
        String password = req.getParameter("password");
        String name = req.getParameter("name");
        Part part=req.getPart("avatar");
        String phone = req.getParameter("phone");
        String email = req.getParameter("email");
        String introduction = req.getParameter("introduction");
        String registrationFee = req.getParameter("registrationFee");
        String entryDate = req.getParameter("entryDate");
        String departmentId = req.getParameter("departmentId");
        String professionalTitleId = req.getParameter("professionalTitleId");

        String sfn=part.getSubmittedFileName();
        String houzhui=sfn.substring(sfn.lastIndexOf("."));
        String newname= UUID.randomUUID().toString().replaceAll("-","");

        String avatar =newname+houzhui;

//保存图片
        part.write("D:\\img\\"+avatar);
        //


        DoctorsServiceImpl service=new DoctorsServiceImpl();
        service.update(doctorId,password,name,avatar,phone,email,introduction,registrationFee,entryDate,Integer.parseInt(departmentId),Integer.parseInt(professionalTitleId));
        resp.sendRedirect("/doc/list.html");


    }
}
