package com.ustb.hospital.servlet;

import com.github.pagehelper.PageInfo;
import com.ustb.hospital.service.DepartmentsServiceImpl;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/departments/list")
public class DepartmentsListServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pageNum=req.getParameter("pageNum");
        String pageSize=req.getParameter("pageSize");
        int i =1;
        if(pageNum!=null && pageNum!=" "){
            i = Integer.parseInt(pageNum);
        }

        DepartmentsServiceImpl service=new DepartmentsServiceImpl();
        PageInfo pageInfo=service.queryAll(i,5);


        req.setAttribute("pageInfo",pageInfo);
        req.getRequestDispatcher("/hospro/departmentsList.jsp").forward(req,resp);
    }
}
