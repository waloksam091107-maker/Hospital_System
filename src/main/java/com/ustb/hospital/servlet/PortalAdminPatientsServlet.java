package com.ustb.hospital.servlet;

import com.ustb.hospital.service.AdminMasterDataService;
import com.ustb.hospital.service.AuditContext;
import com.ustb.hospital.utils.AppointmentOperationResult;
import com.ustb.hospital.utils.SessionIdentity;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/portal/admin/patients")
public class PortalAdminPatientsServlet extends HttpServlet {
    private final AdminMasterDataService service;
    public PortalAdminPatientsServlet() { this(new AdminMasterDataService()); }
    PortalAdminPatientsServlet(AdminMasterDataService service) { this.service = service; }
    @Override protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException { if (PortalServletSupport.requireRole(request, response, SessionIdentity.ROLE_ADMIN) == null) return; PortalServletSupport.writeJson(response, java.util.Collections.<String, Object>singletonMap("items", service.patients())); }
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (PortalServletSupport.requireRole(request, response, SessionIdentity.ROLE_ADMIN) == null) return;
        String action = request.getParameter("action");
        AuditContext context = AuditContext.fromRequest(request);
        AppointmentOperationResult result;
        if ("create".equals(action)) {
            result = service.createPatient(request.getParameter("name"), request.getParameter("idCardNumber"),
                    request.getParameter("password"), request.getParameter("phone"), request.getParameter("email"), context);
        } else if ("update".equals(action)) {
            Integer id = PortalServletSupport.parsePositiveInt(request.getParameter("patientId"));
            result = id == null ? AppointmentOperationResult.failure("INVALID_REQUEST", "患者编号无效。")
                    : service.updatePatient(id.intValue(), request.getParameter("phone"), request.getParameter("email"), context);
        } else if ("delete".equals(action)) {
            Integer id = PortalServletSupport.parsePositiveInt(request.getParameter("patientId"));
            result = id == null ? AppointmentOperationResult.failure("INVALID_REQUEST", "患者编号无效。")
                    : service.deletePatient(id.intValue(), context);
        } else {
            result = AppointmentOperationResult.failure("INVALID_REQUEST", "不支持的操作。");
        }
        PortalServletSupport.writeOperationResult(response, result);
    }
}
