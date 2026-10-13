package com.ustb.hospital.servlet;

import com.github.pagehelper.PageInfo;
import com.ustb.hospital.entity.Appointments;
import com.ustb.hospital.service.AppointmentsServiceImpl;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/appointments/doctor")
public class AppointmentsListServlet extends HttpServlet {
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Integer doctorId = AppointmentServletSupport.requireDoctor(request, response);
        if (doctorId == null) return;
        PageInfo<Appointments> page = new AppointmentsServiceImpl().queryByDoctorId(doctorId.intValue(), AppointmentServletSupport.parsePage(request.getParameter("pageNum")), 5);
        AppointmentServletSupport.writeJson(response, page);
    }
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException { doPost(request, response); }
}
