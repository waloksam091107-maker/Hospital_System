package com.ustb.hospital.servlet;

import com.ustb.hospital.service.DoctorsServiceImpl;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/legacy/doctors/delete")
public class DoctorsDelServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req,resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String doctorId_ = req.getParameter("doctorId");
        int doctorId=Integer.parseInt(doctorId_);
        DoctorsServiceImpl service=new DoctorsServiceImpl();

        service.delById(doctorId);
        req.getRequestDispatcher("/doc/list.html").forward(req,resp);

    }
}
