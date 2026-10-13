package com.ustb.hospital.servlet;

import com.ustb.hospital.service.DepartmentsServiceImpl;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/portal/departments/create")
public class PortalDepartmentCreateServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req,resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("utf-8");
        //1.接收参数
        String deptPid = req.getParameter("deptPid");
        String departmentName = req.getParameter("departmentName");
        String departmentDescription = req.getParameter("departmentDescription");
        if (departmentDescription == null) {
            departmentDescription = req.getParameter("departmentDiscription");
        }



        //2.调用业务
        DepartmentsServiceImpl service=new DepartmentsServiceImpl();
        try {
            service.save(Integer.parseInt(deptPid),departmentName,departmentDescription);
            //没有异常 说明 保存成功  要开始页面导航了
            //重定向
            resp.sendRedirect("/departments/list");  //-> list servlet


        } catch (Exception e) {
            e.printStackTrace();
            resp.sendRedirect("/portal/departments/create-page");

        }

        //3.页面导航

    }
}
