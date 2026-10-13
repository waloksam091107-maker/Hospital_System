package com.ustb.hospital.servlet;

import com.ustb.hospital.service.DoctorsServiceImpl;
import com.ustb.hospital.service.PatientsServiceImpl;

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
@WebServlet("/doc/save")
public class DoctorsSaveServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        req.setCharacterEncoding("utf-8");
        resp.setContentType("test/json;charset=utf-8");
        String password = req.getParameter("password");
        String name = req.getParameter("name");
        String entryDate = req.getParameter("entryDate");

        Part part =req.getPart("avatar");

        String sfn=part.getSubmittedFileName();
        String houzhui=sfn.substring(sfn.lastIndexOf("."));
        String newname= UUID.randomUUID().toString().replaceAll("-","");

        String avatar =newname+houzhui;
//保存图片
        part.write("D:\\img\\"+avatar);
        //

        String phone =req.getParameter("phone");
        String email = req.getParameter("email");
        String introduction = req.getParameter("introduction");
        String registrationFee = req.getParameter("registrationFee");
        int departmentId=13;
        int professionalTitleId=3;

        DoctorsServiceImpl service=new DoctorsServiceImpl();

        try {
            service.save(password,   name,  avatar,   phone,  email,  introduction,   registrationFee,  entryDate,departmentId,  professionalTitleId);
            req.getRequestDispatcher("/doc/list.html").forward(req,resp); //?????


        } catch (Exception e) {
            e.printStackTrace();
            resp.sendRedirect("/index.jsp");

        }


    }
}
