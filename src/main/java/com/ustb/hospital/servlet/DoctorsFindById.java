package com.ustb.hospital.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ustb.hospital.entity.Doctors;
import com.ustb.hospital.service.DoctorsServiceImpl;

import javax.print.Doc;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/doc/findById")
public class DoctorsFindById extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String doctorId_ = req.getParameter("doctorId");
        int doctorId=Integer.parseInt(doctorId_);
        DoctorsServiceImpl service=new DoctorsServiceImpl();
        Doctors doctors=service.findById(doctorId);
        ObjectMapper objectMapper=new ObjectMapper();
        String jsonStr=objectMapper.writeValueAsString(doctors);

        resp.setContentType("application/json;charset=UTF-8");
        resp.getWriter().write(jsonStr);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doGet(req,resp);
    }
}
