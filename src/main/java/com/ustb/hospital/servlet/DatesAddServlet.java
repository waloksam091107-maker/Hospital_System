package com.ustb.hospital.servlet;

import com.ustb.hospital.entity.Doctors;
import com.ustb.hospital.service.DatesServiceImpl;
import com.ustb.hospital.service.DoctorsServiceImpl;
import com.ustb.hospital.utils.AppointmentOperationResult;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/schedules/create")
public class DatesAddServlet extends HttpServlet {
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Integer doctorId = AppointmentServletSupport.requireDoctor(request, response);
        if (doctorId == null) return;
        Doctors doctor = new DoctorsServiceImpl().findById(doctorId.intValue());
        if (doctor == null) { AppointmentServletSupport.writeBadRequest(response, "当前医生不存在。"); return; }
        AppointmentOperationResult result = new DatesServiceImpl().addDate(doctorId.intValue(), doctor.getName(), request.getParameter("date"), request.getParameter("shang"), request.getParameter("xia"), request.getParameter("wan"));
        AppointmentServletSupport.writeResult(response, result);
    }
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException { doPost(request, response); }
}
