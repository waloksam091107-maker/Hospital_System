package com.ustb.hospital.servlet;

import com.ustb.hospital.entity.Appointments;
import com.ustb.hospital.mapper.AppointmentsMapper;
import com.ustb.hospital.service.AppointmentsServiceImpl;
import com.ustb.hospital.utils.MybatisUtil;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.ibatis.session.SqlSession;
import java.io.IOException;

/** Retained only as an administrator compatibility endpoint; patients never delete appointment history. */
@WebServlet("/appointments/delete")
public class AppointmentsDeleteServlet extends HttpServlet {
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!AppointmentServletSupport.requireAdmin(request, response)) return;
        Integer appointmentId = AppointmentServletSupport.parsePositiveInt(request.getParameter("appointmentId"));
        if (appointmentId == null) { AppointmentServletSupport.writeBadRequest(response, "预约编号无效。"); return; }
        SqlSession session = MybatisUtil.getTransactionSqlSession();
        try {
            AppointmentsMapper mapper = session.getMapper(AppointmentsMapper.class);
            Appointments appointment = mapper.selectById(appointmentId.intValue());
            if (appointment == null || AppointmentsServiceImpl.STATUS_BOOKED.equals(appointment.getStatus())) {
                session.rollback();
                AppointmentServletSupport.writeBadRequest(response, "不能直接删除不存在或未取消的预约。");
                return;
            }
            mapper.deleteAppointmentById(appointmentId.intValue());
            session.commit();
            AppointmentServletSupport.writeJson(response, java.util.Collections.singletonMap("success", Boolean.TRUE));
        } catch (Exception exception) {
            session.rollback();
            AppointmentServletSupport.writeBadRequest(response, "删除预约失败。");
        } finally { session.close(); }
    }
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException { doPost(request, response); }
}
