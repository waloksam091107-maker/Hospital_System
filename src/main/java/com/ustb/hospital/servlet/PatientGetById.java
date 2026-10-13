package com.ustb.hospital.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ustb.hospital.entity.Patients;
import com.ustb.hospital.service.PatientsServiceImpl;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/order/getPatientById")
public class PatientGetById extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("utf-8");
        resp.setContentType("test/json;charset=utf-8");

        String patientId = req.getParameter("patientId");
        PatientsServiceImpl service =new PatientsServiceImpl();
        Patients patients=service.getpatById(Integer.parseInt(patientId));
        ObjectMapper objectmapper = new ObjectMapper();
        String jsonStr = objectmapper.writeValueAsString(patients);


        resp.getWriter().write(jsonStr);

    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doGet(req,resp);
    }
}
