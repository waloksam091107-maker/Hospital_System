package com.ustb.hospital.servlet;

import com.ustb.hospital.entity.Doctors;
import com.ustb.hospital.service.DoctorsServiceImpl;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/portal/doctors/profile/edit")
public class DoctorProfileEditPageServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("utf-8");
        //响应乱码
        resp.setContentType("text/html;charset=utf-8");
        int id = Integer.parseInt(req.getParameter("doctorId"));
        DoctorsServiceImpl service = new DoctorsServiceImpl();
        Doctors doctor = service.queryById(id);
        //转发 重定向
        req.setAttribute("doctor", doctor);
        req.getRequestDispatcher("/hospro/doc/doctor-profile-edit.jsp").forward(req, resp);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }
}
