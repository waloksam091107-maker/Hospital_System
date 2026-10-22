package com.ustb.hospital.servlet;

import com.ustb.hospital.service.AuditContext;
import com.ustb.hospital.service.PortalAccountService;
import com.ustb.hospital.utils.SessionIdentity;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

@WebServlet("/portal/profile")
public class PortalProfileServlet extends HttpServlet {
    private final PortalAccountService accounts;
    public PortalProfileServlet() { this(new PortalAccountService()); }
    PortalProfileServlet(PortalAccountService accounts) { this.accounts = accounts; }

    @Override protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Integer id = PortalServletSupport.requireAuthenticated(request, response);
        if (id == null) return;
        Map<String, Object> profile = accounts.readOwnProfile(SessionIdentity.getCurrentRole(request), id.intValue());
        if (profile == null) { PortalServletSupport.writeJsonError(response, HttpServletResponse.SC_NOT_FOUND, "NOT_FOUND", "当前账号不存在或已被删除。"); return; }
        PortalServletSupport.writeJson(response, profile);
    }

    @Override protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Integer id = PortalServletSupport.requireAuthenticated(request, response);
        if (id == null) return;
        PortalServletSupport.writeOperationResult(response, accounts.updateOwnProfile(SessionIdentity.getCurrentRole(request), id.intValue(),
                request.getParameter("phone"), request.getParameter("email"), request.getParameter("introduction"), AuditContext.fromRequest(request)));
    }
}
