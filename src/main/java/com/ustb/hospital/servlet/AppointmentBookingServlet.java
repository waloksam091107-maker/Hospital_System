package com.ustb.hospital.servlet;

import com.ustb.hospital.service.AppointmentsServiceImpl;
import com.ustb.hospital.utils.AppointmentOperationResult;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/appointments/book")
public class AppointmentBookingServlet extends HttpServlet {
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Integer patientId = AppointmentServletSupport.requirePatient(request, response);
        if (patientId == null) return;
        Integer doctorId = AppointmentServletSupport.parsePositiveInt(request.getParameter("doctorId"));
        if (doctorId == null) { AppointmentServletSupport.writeBadRequest(response, "医生编号无效。"); return; }
        AppointmentOperationResult result = new AppointmentsServiceImpl().bookAppointment(patientId.intValue(), doctorId.intValue(), request.getParameter("appointment_date"), request.getParameter("appointment_time"));
        AppointmentServletSupport.writeResult(response, result);
    }
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException { doPost(request, response); }
}
