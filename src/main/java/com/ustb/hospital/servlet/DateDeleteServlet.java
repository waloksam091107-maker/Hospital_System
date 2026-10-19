package com.ustb.hospital.servlet;

import com.ustb.hospital.service.DatesServiceImpl;
import com.ustb.hospital.utils.AppointmentOperationResult;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/schedules/delete")
public class DateDeleteServlet extends HttpServlet {
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Integer doctorId = AppointmentServletSupport.requireDoctor(request, response);
        Integer dateId = AppointmentServletSupport.parsePositiveInt(request.getParameter("dateId"));
        if (doctorId == null) return;
        if (dateId == null) { AppointmentServletSupport.writeBadRequest(response, "排班编号无效。"); return; }
        AppointmentOperationResult result = new DatesServiceImpl().deleteForDoctor(doctorId.intValue(), dateId.intValue(), com.ustb.hospital.service.AuditContext.fromRequest(request));
        AppointmentServletSupport.writeResult(response, result);
    }
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException { AppointmentServletSupport.writeMethodNotAllowed(response); }
}
