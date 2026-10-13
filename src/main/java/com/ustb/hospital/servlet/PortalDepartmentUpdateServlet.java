package com.ustb.hospital.servlet;

import com.ustb.hospital.service.DepartmentsServiceImpl;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/portal/departments/update")
public class PortalDepartmentUpdateServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // 设置请求和响应的字符编码
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("text/html;charset=UTF-8");

        String departmentId_=req.getParameter("id");
        int departmentId=Integer.parseInt(departmentId_);
        String departmentName=req.getParameter("name");
        String departmentDescription=req.getParameter("desc");



        DepartmentsServiceImpl service =new DepartmentsServiceImpl();
        service.updateById(departmentId,departmentName,departmentDescription);
          resp.sendRedirect("/departments/list");
    }
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doGet(req,resp);
    }
}
