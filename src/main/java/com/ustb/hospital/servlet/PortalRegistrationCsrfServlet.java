package com.ustb.hospital.servlet;

import com.ustb.hospital.security.CsrfTokenService;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/** Issues a session-bound token before an anonymous patient registration request. */
@WebServlet("/portal/accounts/csrf")
public class PortalRegistrationCsrfServlet extends HttpServlet {
    @Override protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String token = CsrfTokenService.current(request.getSession(false));
        if (token == null) token = CsrfTokenService.issue(request.getSession(true));
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("token", token);
        PortalServletSupport.writeJson(response, body);
    }

    @Override protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        PortalServletSupport.writeMethodNotAllowed(response);
    }
}
