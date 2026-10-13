package com.ustb.hospital.servlet;

import com.ustb.hospital.entity.Admins;
import com.ustb.hospital.entity.Doctors;
import com.ustb.hospital.entity.Patients;
import com.ustb.hospital.service.AdminsServiceImpl;
import com.ustb.hospital.service.DoctorsServiceImpl;
import com.ustb.hospital.service.PatientsServiceImpl;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/password/change")
public class PasswordChangeVerificationServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // 获取表单数据
        req.setCharacterEncoding("utf-8");
        String user = req.getParameter("user");
        String oldPassword = req.getParameter("jiupass");
        String newPassword = req.getParameter("password");
        String confirmPassword = req.getParameter("cpassword");
        String roleCode = req.getParameter("roleCode");
        if (roleCode == null) {
            roleCode = req.getParameter("typeId");
        }

        if (newPassword != null && newPassword.equals(confirmPassword)) {
            if (roleCode != null) {
                if (roleCode.equals("1")) {
                    // 处理医生的逻辑
                    DoctorsServiceImpl service = new DoctorsServiceImpl();
                    Doctors doctors = service.selectByJobNumber(user);

                    if (doctors == null) {
resp.sendRedirect(req.getContextPath() + "/hospro/login.html?typeId=1");
                    } else if (!doctors.getPassword().equals(oldPassword)) {
resp.sendRedirect(req.getContextPath() + "/hospro/login.html?typeId=1");
                    } else {
                        // 修改密码
                        service.updatePassword(user, newPassword);
resp.getWriter().write("1");
                    }
                } else if (roleCode.equals("2")) {
                    // 处理患者的逻辑
                    PatientsServiceImpl service = new PatientsServiceImpl();
                    Patients patients = service.selectByName(user);

                    if (patients == null) {
resp.sendRedirect(req.getContextPath() + "/hospro/login.html?typeId=2");
                    } else if (!patients.getPassword().equals(oldPassword)) {
resp.sendRedirect(req.getContextPath() + "/hospro/login.html?typeId=2");
                    } else {
                        // 修改密码
                        service.updatePassword(user, newPassword);
                        resp.getWriter().write("1");
                    }
                } else if (roleCode.equals("3")) {
                    // 处理管理员的逻辑
                    AdminsServiceImpl service = new AdminsServiceImpl();
                    Admins admins = service.queryByUsername(user);

                    if (admins == null) {
resp.sendRedirect(req.getContextPath() + "/hospro/login.html?typeId=3");
                    } else if (!admins.getPassword().equals(oldPassword)) {
resp.sendRedirect(req.getContextPath() + "/hospro/login.html?typeId=3");
                    } else {
                        // 修改密码
                        service.updatePassword(user, newPassword);
                        resp.getWriter().write("1");
                    }
                } else {
resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid typeId");
                }
            } else {
resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "typeId is required");
            }
        } else {
resp.sendRedirect(req.getContextPath() + "/hospro/change-password.html"); // 或者返回修改密码页面
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req,resp);
    }
}
