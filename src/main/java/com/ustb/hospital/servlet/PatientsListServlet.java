package com.ustb.hospital.servlet;

import com.github.pagehelper.PageInfo;
import com.ustb.hospital.service.DepartmentsServiceImpl;
import com.ustb.hospital.service.PatientsServiceImpl;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/list_patient")
public class PatientsListServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // 设置请求和响应的字符编码
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("text/html;charset=UTF-8");

        String pageNum=req.getParameter("pageNum");
        String pageSize=req.getParameter("pageSize");
        int i=1;
        if(pageNum!=null && pageNum!=""){
            i=Integer.parseInt(pageNum);
        }


        PatientsServiceImpl service =new PatientsServiceImpl();
        PageInfo pageInfo=service.queryAll(i,5); ////

        req.setAttribute("pageInfo",pageInfo);
        req.getRequestDispatcher("/patient/list.jsp").forward(req,resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doGet(req, resp);
    }
}
