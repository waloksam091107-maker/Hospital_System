package com.ustb.hospital.servlet;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/** Retired plaintext GET login endpoint; use /auth/login instead. */
@WebServlet("/login")
public class AdminsLoginServlet extends HttpServlet {
    @Override protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException { gone(response); }
    @Override protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException { gone(response); }
    private static void gone(HttpServletResponse response) throws IOException { response.sendError(HttpServletResponse.SC_GONE, "旧登录入口已停用，请使用 /auth/login。"); }
}
