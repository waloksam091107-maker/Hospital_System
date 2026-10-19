package com.ustb.hospital.servlet;

import com.ustb.hospital.security.CsrfTokenService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/security/csrf")
public class CsrfTokenServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String token = CsrfTokenService.current(request.getSession(false));
        if (token == null) token = CsrfTokenService.issue(request.getSession(true));
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"token\":\"" + token + "\"}");
    }
}
