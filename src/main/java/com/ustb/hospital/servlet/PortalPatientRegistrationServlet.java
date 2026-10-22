package com.ustb.hospital.servlet;

import com.ustb.hospital.service.AuditContext;
import com.ustb.hospital.service.AuditService;
import com.ustb.hospital.service.PortalAccountService;
import com.ustb.hospital.security.CsrfTokenService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/** Public registration is deliberately limited to patients. */
@WebServlet("/portal/accounts/patient-register")
public class PortalPatientRegistrationServlet extends HttpServlet {
    private final PortalAccountService accounts;
    private final AuditService audit;

    public PortalPatientRegistrationServlet() { this(new PortalAccountService(), new AuditService()); }
    PortalPatientRegistrationServlet(PortalAccountService accounts, AuditService audit) { this.accounts = accounts; this.audit = audit; }

    @Override protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!CsrfTokenService.verify(request)) {
            audit.recordFailure(AuditContext.fromRequest(request), "PATIENT_REGISTER", "ACCOUNT", null);
            PortalServletSupport.writeJsonError(response, HttpServletResponse.SC_FORBIDDEN, "CSRF_REJECTED", "请求校验失败，请刷新页面后重试。");
            return;
        }
        PortalServletSupport.writeOperationResult(response, accounts.registerPatient(
                request.getParameter("name"), request.getParameter("idCardNumber"), request.getParameter("password"),
                request.getParameter("phone"), request.getParameter("email"), AuditContext.fromRequest(request)));
    }
    @Override protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        PortalServletSupport.writeMethodNotAllowed(response);
    }
}
