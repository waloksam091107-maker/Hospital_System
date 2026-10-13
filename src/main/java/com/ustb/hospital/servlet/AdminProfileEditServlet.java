package com.ustb.hospital.servlet;

import com.ustb.hospital.service.AdminsServiceImpl;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/admins/profile/update")
public class AdminProfileEditServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //请求乱码
        req.setCharacterEncoding("UTF-8");
        //响应乱码
        resp.setContentType("text/html;charset=UTF-8");
        int adminId = Integer.parseInt(req.getParameter("adminId"));
        String username = req.getParameter("username");
        String phone = req.getParameter("phone");
        String email = req.getParameter("email");
        AdminsServiceImpl service = new AdminsServiceImpl();
try {
            service.update(adminId,username,phone,email);
            //修改成功
resp.sendRedirect("/hospro/adminProfile.html?adminId=" + adminId);
        } catch (Exception e) {
            e.printStackTrace();
            resp.sendRedirect("/admintoEdit?adminId=" + adminId);
        }

    }
}
