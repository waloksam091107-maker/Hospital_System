package com.ustb.hospital.servlet;

import com.ustb.hospital.entity.Appointments;
import com.ustb.hospital.mapper.AppointmentsMapper;
import com.ustb.hospital.utils.MybatisUtil;
import org.apache.ibatis.session.SqlSession;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/** Returns only the appointment details needed for the current doctor's encounter. */
@WebServlet("/appointments/doctor/detail")
public class DoctorAppointmentDetailServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Integer doctorId = AppointmentServletSupport.requireDoctor(request, response);
        if (doctorId == null) return;
        Integer appointmentId = AppointmentServletSupport.parsePositiveInt(request.getParameter("appointmentId"));
        if (appointmentId == null) {
            AppointmentServletSupport.writeBadRequest(response, "预约编号无效。");
            return;
        }
        Appointments appointment;
        try (SqlSession session = MybatisUtil.getSqlSession()) {
            appointment = session.getMapper(AppointmentsMapper.class).selectDetailForDoctor(appointmentId, doctorId);
        }
        Map<String, Object> body = new HashMap<String, Object>();
        if (appointment == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            body.put("success", Boolean.FALSE);
            body.put("code", "NOT_FOUND");
            body.put("message", "预约不存在或无权查看。");
        } else {
            body.put("appointmentId", appointment.getAppointmentId());
            body.put("appointmentDate", appointment.getAppointmentDate());
            body.put("appointmentTime", appointment.getAppointmentTime());
            body.put("status", appointment.getStatus());
        }
        AppointmentServletSupport.writeJson(response, body);
    }
}
