package com.ustb.hospital.servlet;

import com.ustb.hospital.service.AnnouncementAdminService;
import com.ustb.hospital.service.AuditContext;
import com.ustb.hospital.utils.AppointmentOperationResult;
import com.ustb.hospital.utils.SessionIdentity;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

@WebServlet("/portal/admin/announcements")
public class PortalAdminAnnouncementsServlet extends HttpServlet {
    private final AnnouncementAdminService service;

    public PortalAdminAnnouncementsServlet() { this(new AnnouncementAdminService()); }
    PortalAdminAnnouncementsServlet(AnnouncementAdminService service) { this.service = service; }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (PortalServletSupport.requireRole(request, response, SessionIdentity.ROLE_ADMIN) == null) return;
        PortalServletSupport.writeJson(response, Collections.<String, Object>singletonMap("items", service.list()));
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (PortalServletSupport.requireRole(request, response, SessionIdentity.ROLE_ADMIN) == null) return;
        String action = request.getParameter("action");
        AuditContext context = AuditContext.fromRequest(request);
        AppointmentOperationResult result;
        if ("create".equals(action)) {
            result = service.create(request.getParameter("title"), request.getParameter("content"), context);
        } else if ("update".equals(action)) {
            Integer id = PortalServletSupport.parsePositiveInt(request.getParameter("announcementId"));
            result = id == null ? AppointmentOperationResult.failure("INVALID_REQUEST", "公告编号无效。") : service.update(id.intValue(), request.getParameter("title"), request.getParameter("content"), context);
        } else if ("delete".equals(action)) {
            Integer id = PortalServletSupport.parsePositiveInt(request.getParameter("announcementId"));
            result = id == null ? AppointmentOperationResult.failure("INVALID_REQUEST", "公告编号无效。") : service.delete(id.intValue(), context);
        } else if ("publish".equals(action) || "offline".equals(action)) {
            Integer id = PortalServletSupport.parsePositiveInt(request.getParameter("announcementId"));
            result = id == null ? AppointmentOperationResult.failure("INVALID_REQUEST", "公告编号无效。") : service.changeStatus(id.intValue(), "publish".equals(action), context);
        } else {
            result = AppointmentOperationResult.failure("INVALID_REQUEST", "不支持的操作。");
        }
        PortalServletSupport.writeOperationResult(response, result);
    }
}
