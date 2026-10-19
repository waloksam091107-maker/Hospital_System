package com.ustb.hospital.servlet;

import com.ustb.hospital.service.AdminsServiceImpl;
import com.ustb.hospital.service.DoctorsServiceImpl;
import com.ustb.hospital.service.PatientsServiceImpl;
import com.ustb.hospital.entity.Admins;
import com.ustb.hospital.entity.Doctors;
import com.ustb.hospital.entity.Patients;
import com.ustb.hospital.security.PasswordService;
import com.ustb.hospital.utils.SessionIdentity;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/password/reset")
public class PasswordResetServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("utf-8");
        String currentPassword = req.getParameter("currentPassword");
        String newPassword = req.getParameter("newPassword");
        if (currentPassword == null || newPassword == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "必须提交当前密码和新密码。");
            return;
        }
        try { PasswordService.validateNewPassword(newPassword); }
        catch (IllegalArgumentException exception) { resp.sendError(HttpServletResponse.SC_BAD_REQUEST, exception.getMessage()); return; }

        com.ustb.hospital.service.AuditContext context = com.ustb.hospital.service.AuditContext.fromRequest(req);
        if (context.getActorId() == null) { forbidden(resp); return; }
        com.ustb.hospital.utils.AppointmentOperationResult result = new com.ustb.hospital.service.PasswordChangeService()
                .change(context.getActorRole(), context.getActorId().intValue(), currentPassword, newPassword, context);
        if (!result.isSuccessful()) {
            if ("FORBIDDEN".equals(result.getCode())) forbidden(resp);
            else resp.sendError("INVALID_PASSWORD".equals(result.getCode()) ? HttpServletResponse.SC_BAD_REQUEST : HttpServletResponse.SC_INTERNAL_SERVER_ERROR, result.getMessage());
            return;
        }
        resp.sendRedirect(req.getContextPath() + "/hospro/index.html");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "仅支持 POST 修改密码请求");
    }

    private static void forbidden(HttpServletResponse response) throws IOException {
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "当前密码错误或未登录。");
    }
}
