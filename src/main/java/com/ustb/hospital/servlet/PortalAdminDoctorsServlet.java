package com.ustb.hospital.servlet;

import com.ustb.hospital.entity.Doctors;
import com.ustb.hospital.service.AdminMasterDataService;
import com.ustb.hospital.service.AuditContext;
import com.ustb.hospital.utils.AppointmentOperationResult;
import com.ustb.hospital.utils.SessionIdentity;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/portal/admin/doctors")
public class PortalAdminDoctorsServlet extends HttpServlet {
    private final AdminMasterDataService service;
    public PortalAdminDoctorsServlet() { this(new AdminMasterDataService()); }
    PortalAdminDoctorsServlet(AdminMasterDataService service) { this.service = service; }
    @Override protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (PortalServletSupport.requireRole(request, response, SessionIdentity.ROLE_ADMIN) == null) return;
        PortalServletSupport.writeJson(response, java.util.Collections.<String, Object>singletonMap("items", service.doctors()));
    }
    @Override protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (PortalServletSupport.requireRole(request, response, SessionIdentity.ROLE_ADMIN) == null) return;
        String action = request.getParameter("action"); AppointmentOperationResult result;
        if ("delete".equals(action)) { Integer id = PortalServletSupport.parsePositiveInt(request.getParameter("doctorId")); result = id == null ? AppointmentOperationResult.failure("INVALID_REQUEST", "医生编号无效。") : service.deleteDoctor(id.intValue(), AuditContext.fromRequest(request)); }
        else {
            Doctors doctor = doctor(request); if (doctor == null) result = AppointmentOperationResult.failure("INVALID_REQUEST", "医生编号、科室或职称无效。");
            else if ("create".equals(action)) result = service.createDoctor(doctor, request.getParameter("password"), AuditContext.fromRequest(request));
            else if ("update".equals(action)) result = service.updateDoctor(doctor, AuditContext.fromRequest(request));
            else result = AppointmentOperationResult.failure("INVALID_REQUEST", "不支持的操作。");
        }
        PortalServletSupport.writeOperationResult(response, result);
    }
    private static Doctors doctor(HttpServletRequest request) { Integer department = PortalServletSupport.parsePositiveInt(request.getParameter("departmentId")); Integer title = PortalServletSupport.parsePositiveInt(request.getParameter("professionalTitleId")); if (department == null || title == null) return null; Doctors value = new Doctors(); Integer id = PortalServletSupport.parsePositiveInt(request.getParameter("doctorId")); value.setDoctorId(id == null ? 0 : id.intValue()); value.setName(request.getParameter("name")); value.setPhone(request.getParameter("phone")); value.setEmail(request.getParameter("email")); value.setIntroduction(request.getParameter("introduction")); value.setRegistrationFee(request.getParameter("registrationFee")); value.setEntryDate(request.getParameter("entryDate")); value.setDepartmentId(department.intValue()); value.setProfessionalTitleId(title.intValue()); return value; }
}
