package com.ustb.hospital.servlet;

import com.ustb.hospital.service.AuditService;
import com.ustb.hospital.service.AuditQuery;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/** Administrators may inspect metadata-only audit events. */
@WebServlet({"/audit-logs", "/audit-logs/*"})
public class AuditLogsServlet extends HttpServlet {
    private final AuditService service;
    public AuditLogsServlet() { this(new AuditService()); }
    AuditLogsServlet(AuditService service) { this.service = service; }
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!AppointmentServletSupport.requireAdmin(request, response)) return;
        try {
            if ("/search".equals(request.getPathInfo())) {
                AuditQuery query = new AuditQuery(request.getParameter("start"), request.getParameter("end"),
                        request.getParameter("role"), request.getParameter("actorId"), request.getParameter("action"),
                        request.getParameter("outcome"), request.getParameter("page"), request.getParameter("pageSize"));
                AppointmentServletSupport.writeJson(response, service.search(query));
            } else if (request.getPathInfo() == null) {
                Integer limit = AppointmentServletSupport.parsePositiveInt(request.getParameter("limit"));
                AppointmentServletSupport.writeJson(response, service.recent(limit == null ? 50 : limit.intValue()));
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                AppointmentServletSupport.writeJson(response, java.util.Collections.singletonMap("message", "接口不存在。"));
            }
        } catch (IllegalArgumentException invalid) {
            AppointmentServletSupport.writeBadRequest(response, invalid.getMessage());
        } catch (RuntimeException error) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            AppointmentServletSupport.writeJson(response, java.util.Collections.singletonMap("message", "审计日志查询失败，请稍后重试。"));
        }
    }
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        AppointmentServletSupport.writeMethodNotAllowed(response);
    }
}
