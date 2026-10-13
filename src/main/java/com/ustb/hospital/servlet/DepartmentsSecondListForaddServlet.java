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

@WebServlet("/secondDept")
public class DepartmentsSecondListForaddServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        DepartmentsServiceImpl service=new DepartmentsServiceImpl();
        List<Departments> list = service.querySecondDept();
req.setAttribute("list",list);
        req.getRequestDispatcher("/hospro/doc/add.jsp").forward(req,resp);
}
}
