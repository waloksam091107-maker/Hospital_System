package com.ustb.hospital.servlet;

import com.github.pagehelper.PageInfo;
import com.ustb.hospital.entity.Appointments;
import com.ustb.hospital.service.AppointmentsServiceImpl;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/appointments/doctor")
public class AppointmentsListServlet extends HttpServlet {
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Integer doctorId = AppointmentServletSupport.requireDoctor(request, response);
        if (doctorId == null) return;
        String target = request.getParameter("appointmentId");
        Integer targetId = target == null ? null : AppointmentServletSupport.parsePositiveInt(target);
        if (target != null && targetId == null) { AppointmentServletSupport.writeBadRequest(response, "预约编号无效。"); return; }
        AppointmentsServiceImpl service = new AppointmentsServiceImpl();
        PageInfo<Appointments> page = targetId == null
                ? service.queryByDoctorId(doctorId.intValue(), AppointmentServletSupport.parsePage(request.getParameter("pageNum")), 5)
                : service.queryTarget(doctorId.intValue(), targetId.intValue(), true);
        if (targetId != null && page.getList().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            java.util.Map<String,Object> error = new java.util.HashMap<>();
            error.put("success", false); error.put("message", "预约不存在或无权查看。");
            AppointmentServletSupport.writeJson(response, error); return;
        }
        AppointmentServletSupport.writeJson(response, page);
    }
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException { doPost(request, response); }
}
