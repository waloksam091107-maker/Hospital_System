package com.ustb.hospital.servlet;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/appointments/complete")
public class AppointmentCompletionServlet extends HttpServlet {
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Integer doctorId = AppointmentServletSupport.requireDoctor(request, response);
        Integer appointmentId = AppointmentServletSupport.parsePositiveInt(request.getParameter("appointmentId"));
        if (doctorId == null) return;
        if (appointmentId == null) { AppointmentServletSupport.writeBadRequest(response, "预约编号无效。"); return; }
        response.setStatus(HttpServletResponse.SC_CONFLICT);
        java.util.Map<String, Object> body = new java.util.HashMap<String, Object>();
        body.put("success", Boolean.FALSE);
        body.put("code", "MEDICAL_RECORD_REQUIRED");
        body.put("message", "请通过就诊记录页面保存诊断和医嘱后完成接诊。");
        AppointmentServletSupport.writeJson(response, body);
    }
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException { AppointmentServletSupport.writeMethodNotAllowed(response); }
}
