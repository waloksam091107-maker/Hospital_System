package com.ustb.hospital.servlet;

import com.ustb.hospital.service.DatesServiceImpl;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/appointments/availability")
public class AppointmentAvailabilityServlet extends HttpServlet {
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (AppointmentServletSupport.requirePatient(request, response) == null) return;
        Integer doctorId = AppointmentServletSupport.parsePositiveInt(request.getParameter("doctor_id"));
        if (doctorId == null) { AppointmentServletSupport.writeBadRequest(response, "医生编号无效。"); return; }
        boolean available = new DatesServiceImpl().isSlotAvailable(doctorId.intValue(), request.getParameter("date"), request.getParameter("time"));
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("available", Boolean.valueOf(available));
        result.put("message", available ? "该时段可预约。" : "该时段不存在或已满。");
        AppointmentServletSupport.writeJson(response, result);
    }
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException { doPost(request, response); }
}
