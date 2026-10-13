package com.ustb.hospital.servlet;

import com.ustb.hospital.entity.Departments;
import com.ustb.hospital.service.DepartmentsServiceImpl;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@WebServlet("/portal/departments/edit-page")
public class PortalDepartmentEditPageServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String idParameter = req.getParameter("id");
        int id=Integer.parseInt(idParameter);
        DepartmentsServiceImpl service =new DepartmentsServiceImpl();
        List<Departments> list = service.queryFirstDept();

        req.setAttribute("id",id);
        req.setAttribute("list",list);
        req.getRequestDispatcher("/hospro/dept/update.jsp").forward(req,resp);
    }
}
