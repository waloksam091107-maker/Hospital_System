package com.ustb.hospital.servlet;

import com.ustb.hospital.entity.Admins;
import com.ustb.hospital.entity.Doctors;
import com.ustb.hospital.entity.Patients;
import com.ustb.hospital.service.AdminsServiceImpl;
import com.ustb.hospital.service.DoctorsServiceImpl;
import com.ustb.hospital.service.PatientsServiceImpl;
import com.ustb.hospital.security.CsrfTokenService;
import com.ustb.hospital.security.PasswordService;
import com.ustb.hospital.service.AuditService;
import com.ustb.hospital.utils.SessionIdentity;

import javax.print.Doc;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

@WebServlet("/auth/login")

public class LoginServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("utf-8");
        resp.setContentType("text/html;charset=utf-8");

        String user = req.getParameter("user");
        String password = req.getParameter("password");
        String roleCode = req.getParameter("roleCode");
        if (roleCode == null) {
            roleCode = req.getParameter("typeId");
        }

        if (roleCode != null) {
            if (roleCode.equals("1")) {
                DoctorsServiceImpl service =new DoctorsServiceImpl();
                Doctors doctors=service.selectByJobNumber(user);

                if(doctors==null){
                    auditLoginFailure(req);
                    resp.sendRedirect(req.getContextPath() + "/hospro/login.html?typeId=1");

                }else{
                    if(!PasswordService.matches(password, doctors.getPasswordHash(), doctors.getPassword())){
                        auditLoginFailure(req);
                        resp.sendRedirect(req.getContextPath() + "/hospro/login.html?typeId=1");

                    }else{
                        if (PasswordService.needsUpgrade(doctors.getPasswordHash())) {
                            if (!upgradeLegacy(req, resp, SessionIdentity.ROLE_DOCTOR, doctors.getDoctorId(), password)) return;
                        }
                        establishSession(req, SessionIdentity.ROLE_DOCTOR, doctors.getDoctorId(), doctors.getName());
                        resp.sendRedirect(req.getContextPath() + "/hospro/index.html?roleCode=1&doctorId=" + doctors.getDoctorId());



                    }
                }

                //?
            } else if (roleCode.equals("2")) {
                PatientsServiceImpl service =new PatientsServiceImpl();
                Patients patients=service.selectByName(user);

                if(patients==null){
                    auditLoginFailure(req);
                    resp.sendRedirect(req.getContextPath() + "/hospro/login.html?typeId=2");

                }else{
                    if(!PasswordService.matches(password, patients.getPasswordHash(), patients.getPassword())){
                        auditLoginFailure(req);
                        resp.sendRedirect(req.getContextPath() + "/hospro/login.html?typeId=2");

                    }else{
                        if (PasswordService.needsUpgrade(patients.getPasswordHash())) {
                            if (!upgradeLegacy(req, resp, SessionIdentity.ROLE_PATIENT, patients.getPatientId(), password)) return;
                        }
                        establishSession(req, SessionIdentity.ROLE_PATIENT, patients.getPatientId(), patients.getName());
                        resp.sendRedirect(req.getContextPath() + "/hospro/index.html?roleCode=2&patientId=" + patients.getPatientId());
                    }
                }




            } else if (roleCode.equals("3")) {
                AdminsServiceImpl service =new AdminsServiceImpl();
                Admins admins=service.queryByUsername(user);


                if(admins==null){
                    auditLoginFailure(req);
                    resp.sendRedirect(req.getContextPath() + "/hospro/login.html?typeId=3");

                }else{
                    if(!PasswordService.matches(password, admins.getPasswordHash(), admins.getPassword())){
                        auditLoginFailure(req);
                        resp.sendRedirect(req.getContextPath() + "/hospro/login.html?typeId=3");

                    }else{
                        if (PasswordService.needsUpgrade(admins.getPasswordHash())) {
                            if (!upgradeLegacy(req, resp, SessionIdentity.ROLE_ADMIN, admins.getAdminId(), password)) return;
                        }
                        establishSession(req, SessionIdentity.ROLE_ADMIN, admins.getAdminId(), admins.getUsername());
                        resp.sendRedirect(req.getContextPath() + "/hospro/index.html?roleCode=3&adminId=" + admins.getAdminId());
                    }
                }


            } else {
                // 处理无效的 typeId
resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid typeId");
                return;
            }
        } else {
resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "typeId is required");
        }

        // 继续处理其他逻辑，或者根据处理结果跳转到不同页面





    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "仅支持 POST 登录请求");
    }

    private static void establishSession(HttpServletRequest request, String role, int userId, String name) {
        HttpSession session = request.getSession(true);
        request.changeSessionId();
        session.setMaxInactiveInterval(30 * 60);
        SessionIdentity.set(session, role, userId, name);
        CsrfTokenService.issue(session);
        new AuditService().record(request, "LOGIN", "ACCOUNT", String.valueOf(userId), "SUCCESS");
    }

    private static void auditLoginFailure(HttpServletRequest request) {
        new AuditService().record(request, "LOGIN", "ACCOUNT", null, "FAILURE");
    }
    private static boolean upgradeLegacy(HttpServletRequest request, HttpServletResponse response, String role, int id, String password) throws IOException {
        com.ustb.hospital.service.AuditContext context = new com.ustb.hospital.service.AuditContext(role, Integer.valueOf(id), "POST", "/auth/login", request.getRemoteAddr());
        com.ustb.hospital.utils.AppointmentOperationResult result = new com.ustb.hospital.service.PasswordChangeService().upgradeLegacy(role, id, password, context);
        if (result.isSuccessful()) return true;
        auditLoginFailure(request);
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "登录暂时不可用，请稍后重试。");
        return false;
    }
}
