package com.ustb.hospital.servlet;

import com.ustb.hospital.entity.Doctors;
import com.ustb.hospital.service.DoctorsServiceImpl;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

/** Public doctor details for the booking catalogue; passwords are excluded by @JsonIgnore. */
@WebServlet("/doctors/details")
public class DoctorDetailsServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Integer doctorId = AppointmentServletSupport.parsePositiveInt(request.getParameter("doctorId"));
        if (doctorId == null) {
            AppointmentServletSupport.writeBadRequest(response, "doctorId 必须是正整数。");
            return;
        }
        Doctors doctor = new DoctorsServiceImpl().getDoctorById(doctorId.intValue());
        if (doctor == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            AppointmentServletSupport.writeJson(response, Collections.singletonMap("message", "医生不存在。"));
            return;
        }
        AppointmentServletSupport.writeJson(response, doctor);
    }
}
