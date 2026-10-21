package com.ustb.hospital.servlet;

import com.ustb.hospital.service.DepartmentsServiceImpl;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet("/departments/has-children")
public class DepartmentHasChildrenServlet extends HttpServlet {


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");

        String deptId = req.getParameter("deptId");
        if (deptId == null) {
            resp.getWriter().write("{\"error\":\"Missing deptId parameter\"}");
            return;
        }

        DepartmentsServiceImpl service = new DepartmentsServiceImpl();
        int hasChildren = service.DepartmentHasChildren(Integer.parseInt(deptId));

        // 使用PrintWriter来发送JSON格式的响应
        PrintWriter out = resp.getWriter();
        out.write(String.valueOf(hasChildren));
        out.flush();
    }
}
