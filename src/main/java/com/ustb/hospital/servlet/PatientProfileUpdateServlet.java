package com.ustb.hospital.servlet;

import com.ustb.hospital.service.PatientsServiceImpl;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/patients/profile")

public class PatientProfileUpdateServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //请求乱码
        req.setCharacterEncoding("UTF-8");
        //响应乱码
        resp.setContentType("text/html;charset=UTF-8");
        int patientId = Integer.parseInt(req.getParameter("patientId"));
        String email = req.getParameter("email");
        String phone = req.getParameter("phone");
        String balance = req.getParameter("balance");

        PatientsServiceImpl service = new PatientsServiceImpl();
try {
            service.updatePatientProfile(patientId,phone,balance,email);
            //修改成功
///patientpr.html?patientId=6
            resp.sendRedirect("/hospro/patientpr.html?patientId=" + patientId);
        } catch (Exception e) {
            e.printStackTrace();
            resp.sendRedirect("/hospro/patient/patient-profile-edit.jsp?patientId=" + patientId);
        }

    }
}
