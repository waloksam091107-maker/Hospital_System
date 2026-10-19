package com.ustb.hospital.servlet;

import com.ustb.hospital.service.AppointmentsServiceImpl;
import com.ustb.hospital.utils.AppointmentOperationResult;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/appointments/reschedule")
public class AppointmentsEditServlet extends HttpServlet {
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Integer patientId = AppointmentServletSupport.requirePatient(request, response);
        Integer appointmentId = AppointmentServletSupport.parsePositiveInt(request.getParameter("appointmentId"));
        if (patientId == null) return;
        if (appointmentId == null) { AppointmentServletSupport.writeBadRequest(response, "预约编号无效。"); return; }
        AppointmentOperationResult result = new AppointmentsServiceImpl().rescheduleAppointment(patientId.intValue(), appointmentId.intValue(), request.getParameter("appointmentDate"), request.getParameter("appointmentTime"), com.ustb.hospital.service.AuditContext.fromRequest(request));
        AppointmentServletSupport.writeResult(response, result);
    }
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException { AppointmentServletSupport.writeMethodNotAllowed(response); }
}
