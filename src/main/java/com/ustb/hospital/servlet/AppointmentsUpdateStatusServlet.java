package com.ustb.hospital.servlet;

import com.ustb.hospital.service.AppointmentsServiceImpl;
import com.ustb.hospital.utils.AppointmentOperationResult;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/appointments/cancel")
public class AppointmentsUpdateStatusServlet extends HttpServlet {
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Integer patientId = AppointmentServletSupport.requirePatient(request, response);
        Integer appointmentId = AppointmentServletSupport.parsePositiveInt(request.getParameter("appointmentId"));
        if (patientId == null) return;
        if (appointmentId == null) { AppointmentServletSupport.writeBadRequest(response, "预约编号无效。"); return; }
        AppointmentOperationResult result = new AppointmentsServiceImpl().cancelAppointment(patientId.intValue(), appointmentId.intValue());
        AppointmentServletSupport.writeResult(response, result);
    }
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException { doPost(request, response); }
}
