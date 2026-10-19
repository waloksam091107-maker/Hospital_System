package com.ustb.hospital.servlet;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/** Retired request-parameter password reset endpoint; use authenticated /password/reset. */
@WebServlet("/password/change")
public class PasswordChangeVerificationServlet extends HttpServlet {
    @Override protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException { gone(response); }
    @Override protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException { gone(response); }
    private static void gone(HttpServletResponse response) throws IOException { response.sendError(HttpServletResponse.SC_GONE, "旧密码修改入口已停用，请使用 /password/reset。"); }
}
