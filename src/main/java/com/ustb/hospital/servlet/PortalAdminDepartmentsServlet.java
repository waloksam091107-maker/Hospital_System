package com.ustb.hospital.servlet;

import com.ustb.hospital.entity.Departments;
import com.ustb.hospital.service.AdminMasterDataService;
import com.ustb.hospital.service.AuditContext;
import com.ustb.hospital.utils.AppointmentOperationResult;
import com.ustb.hospital.utils.SessionIdentity;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/portal/admin/departments")
public class PortalAdminDepartmentsServlet extends HttpServlet {
    private final AdminMasterDataService service;
    public PortalAdminDepartmentsServlet() { this(new AdminMasterDataService()); }
    PortalAdminDepartmentsServlet(AdminMasterDataService service) { this.service = service; }
    @Override protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException { if (PortalServletSupport.requireRole(request, response, SessionIdentity.ROLE_ADMIN) == null) return; PortalServletSupport.writeMasterDataPage(request,response,service,"departments"); }
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (PortalServletSupport.requireRole(request, response, SessionIdentity.ROLE_ADMIN) == null) return;
        String action = request.getParameter("action");
        AuditContext context = AuditContext.fromRequest(request);
        AppointmentOperationResult result;
        if ("delete".equals(action)) {
            Integer id = PortalServletSupport.parsePositiveInt(request.getParameter("departmentId"));
            result = id == null ? AppointmentOperationResult.failure("INVALID_REQUEST", "科室编号无效。") : service.deleteDepartment(id.intValue(), context);
        } else if ("create".equals(action) || "update".equals(action)) {
            Integer id = PortalServletSupport.parsePositiveInt(request.getParameter("departmentId"));
            String parentText = request.getParameter("departmentPid");
            Integer parentId = PortalServletSupport.parsePositiveInt(parentText);
            if ("update".equals(action) && id == null) {
                result=AppointmentOperationResult.failure("INVALID_REQUEST","科室编号无效，未进行保存。");
            } else if (parentText != null && !parentText.trim().isEmpty() && parentId == null) {
                result = AppointmentOperationResult.failure("INVALID_REQUEST", "上级科室编号无效。");
            } else {
                Departments department = new Departments();
                department.setDepartmentId("create".equals(action) ? 0 : id.intValue());
                department.setDepartmentName(request.getParameter("departmentName"));
                department.setDepartmentDescription(request.getParameter("departmentDescription"));
                if ("create".equals(action)) department.setDepartmentPid(parentId);
                result = service.saveDepartment(department, context);
            }
        } else {
            result = AppointmentOperationResult.failure("INVALID_REQUEST", "不支持的操作。");
        }
        PortalServletSupport.writeOperationResult(response, result);
    }
}
