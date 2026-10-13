package com.ustb.hospital.servlet;

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

@WebServlet("/portal/doctors/create")
@MultipartConfig
public class PortalDoctorCreateServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("utf-8");
        //1.接收请求参数
        String docName = req.getParameter("docName");
        Part avatarPart = req.getPart("avatar");
        String avatarFileName = avatarPart.getSubmittedFileName();
        String houzhui = avatarFileName.substring(avatarFileName.lastIndexOf("."));

        //重新起一个头像名称：不能重复--随即起名
        String newname = UUID.randomUUID().toString().replaceAll("-", "");
        avatarFileName = newname + houzhui;
        avatarPart.write("D:\\hos_images\\"+avatarFileName);

        String password = req.getParameter("password");
        String phone = req.getParameter("phone");
        String email = req.getParameter("email");
        String introduction = req.getParameter("introduction");
        String registrationFee = req.getParameter("registrationFee");
        String deptId = req.getParameter("deptId");
        String entryDate = req.getParameter("entryDate");
        String professionalTitles = req.getParameter("professionalTitles");
        //2.调用业务
        DoctorsServiceImpl service= new DoctorsServiceImpl();
        try {
            service.createDoctor(docName,avatarFileName,password,phone,email,introduction,registrationFee,Integer.parseInt(deptId),entryDate,Integer.parseInt(professionalTitles));
            resp.sendRedirect("/hospro/doctors.jsp");
        } catch (Exception e) {
            e.printStackTrace();
            resp.sendRedirect("/portal/doctors/create");
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }
}
