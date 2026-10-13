package com.ustb.hospital.servlet;

import com.ustb.hospital.service.AdminsServiceImpl;
import com.ustb.hospital.service.DoctorsServiceImpl;
import com.ustb.hospital.service.PatientsServiceImpl;

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
        String name = req.getParameter("fname");
        String password = req.getParameter("password");
        String roleCode = req.getParameter("roleCode");
        if (roleCode == null) {
            roleCode = req.getParameter("typeId");
        }

        if (roleCode != null) {
            if (roleCode.equals("1")) {
                DoctorsServiceImpl service = new DoctorsServiceImpl();
                // 处理医生的逻辑
                service.updatePassword(name,password);  //传过去工号和password(name是工号！！！)
                resp.sendRedirect(req.getContextPath() + "/hospro/index.html");

            } else if (roleCode.equals("2")) {
                //处理患者的逻辑
                PatientsServiceImpl service =new PatientsServiceImpl();
                service.updatePassword(name,password);
                resp.sendRedirect(req.getContextPath() + "/hospro/index.html");

            } else if (roleCode.equals("3")) {
                AdminsServiceImpl service=new AdminsServiceImpl();
                service.updatePassword(name,password);
                resp.sendRedirect(req.getContextPath() + "/hospro/index.html");

            } else {
                // 其他情况，可以考虑默认处理或者错误处理
                resp.sendRedirect("/hospro/index.html");
            }
        } else {
            // 如果没有传递 typeId 参数，可以考虑默认处理或者错误处理
            resp.sendRedirect("/hospro/index.html");
        }



    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req,resp);
    }
}
