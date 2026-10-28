package com.ustb.hospital.filter;

import com.ustb.hospital.service.AuditService;
import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.*;
import java.io.IOException;

/** Old endpoints can mutate on GET or trust caller-supplied identities. Retire until safely redesigned. */
@WebFilter(urlPatterns = {
        "/accounts/register", "/appointments/delete",
        "/admins/details", "/admin/list", "/admins/list-by-id", "/admintoEdit", "/admins/sum",
        "/legacy/doctors/edit-page", "/doctors/edit-page",
        "/order/getPatientById", "/list_patient", "/patients/select", "/patients/edit-page", "/patienttoEdit", "/patients/profile/edit",
        "/password/email-code", "/verification/sms-code",
        "/admins/profile/update", "/admins/avatar",
        "/portal/doctors/create", "/portal/doctors/profile", "/doc/save", "/doc/update", "/docEdit",
        "/doctors/delete", "/legacy/doctors/delete", "/updateAvatar", "/doctors/avatar-upload",
        "/patients/profile", "/patientEdit", "/deletepatient", "/patients/avatar",
        "/portal/departments/create", "/portal/departments/update", "/departments/delete",
        "/departments/parent-check", "/save", "/delete", "/add", "/update",
        "/menu/del", "/menu/update", "/news/add", "/news/delete", "/news/edit"
})
public class LegacyUnsafeWritesDisabledFilter implements Filter {
    private final AuditService audit = new AuditService();
    public void init(FilterConfig config) { }
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        AuditFailureRecorder.record(req, audit);
        resp.setStatus(HttpServletResponse.SC_GONE);
        resp.setContentType("application/json;charset=UTF-8");
        resp.getWriter().write("{\"success\":false,\"code\":\"LEGACY_WRITE_DISABLED\",\"message\":\"该旧写入功能因安全原因暂时停用。\"}");
    }
    public void destroy() { }
}
