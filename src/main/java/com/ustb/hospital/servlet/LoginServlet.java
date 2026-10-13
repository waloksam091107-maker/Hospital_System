package com.ustb.hospital.servlet;

import com.ustb.hospital.entity.Admins;
import com.ustb.hospital.entity.Doctors;
import com.ustb.hospital.entity.Patients;
import com.ustb.hospital.service.AdminsServiceImpl;
import com.ustb.hospital.service.DoctorsServiceImpl;
import com.ustb.hospital.service.PatientsServiceImpl;
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
                    resp.sendRedirect(req.getContextPath() + "/hospro/login.html?typeId=1");

                }else{
                    if(!doctors.getPassword().equals(password)){  //不成立的情况
                        resp.sendRedirect(req.getContextPath() + "/hospro/login.html?typeId=1");

                    }else{
                        HttpSession session = req.getSession(true);
                        SessionIdentity.set(session, SessionIdentity.ROLE_DOCTOR, doctors.getDoctorId(), doctors.getName());
                        resp.sendRedirect(req.getContextPath() + "/hospro/index.html?roleCode=1&doctorId=" + doctors.getDoctorId());



                    }
                }

                //?
            } else if (roleCode.equals("2")) {
                PatientsServiceImpl service =new PatientsServiceImpl();
                Patients patients=service.selectByName(user);

                if(patients==null){
                    resp.sendRedirect(req.getContextPath() + "/hospro/login.html?typeId=2");

                }else{
                    if(!patients.getPassword().equals(password)){  //不成立的情况
                        resp.sendRedirect(req.getContextPath() + "/hospro/login.html?typeId=2");

                    }else{
                        HttpSession session = req.getSession(true);
                        SessionIdentity.set(session, SessionIdentity.ROLE_PATIENT, patients.getPatientId(), patients.getName());
                        resp.sendRedirect(req.getContextPath() + "/hospro/index.html?roleCode=2&patientId=" + patients.getPatientId());
                    }
                }




            } else if (roleCode.equals("3")) {
                AdminsServiceImpl service =new AdminsServiceImpl();
                Admins admins=service.queryByUsername(user);


                if(admins==null){
                    resp.sendRedirect(req.getContextPath() + "/hospro/login.html?typeId=3");

                }else{
                    if(!admins.getPassword().equals(password)){  //不成立的情况
                        resp.sendRedirect(req.getContextPath() + "/hospro/login.html?typeId=3");

                    }else{
                        HttpSession session = req.getSession(true);
                        SessionIdentity.set(session, SessionIdentity.ROLE_ADMIN, admins.getAdminId(), admins.getUsername());
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
        this.doPost(req,resp);
    }
}
