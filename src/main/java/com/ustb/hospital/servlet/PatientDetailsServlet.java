package com.ustb.hospital.servlet;

import com.ustb.hospital.entity.Patients;
import com.ustb.hospital.service.PatientsServiceImpl;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

/** Returns only the logged-in patient's profile, never an ID supplied in the URL. */
@WebServlet({"/patients/details", "/patientMess", "/patients/current"})
public class PatientDetailsServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Integer patientId = AppointmentServletSupport.requirePatient(request, response);
        if (patientId == null) return;
        Patients patient = new PatientsServiceImpl().getPatientById(patientId.intValue());
        if (patient == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            AppointmentServletSupport.writeJson(response, Collections.singletonMap("message", "当前患者不存在。"));
            return;
        }
        AppointmentServletSupport.writeJson(response, patient);
    }
}
