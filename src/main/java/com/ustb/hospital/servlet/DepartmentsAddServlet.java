package com.ustb.hospital.servlet;

import com.github.pagehelper.PageInfo;
import com.ustb.hospital.service.DepartmentsServiceImpl;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/add")
public class DepartmentsAddServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // 设置请求和响应的字符编码
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("text/html;charset=UTF-8");

        String departmentName = req.getParameter("departmentName");
        String departmentDescription = req.getParameter("departmentDescription");
        if (departmentDescription == null) {
            departmentDescription = req.getParameter("departmentDiscription");
        }
        String cid=req.getParameter("cid");
        DepartmentsServiceImpl service =new DepartmentsServiceImpl();
        int departmentPid = 0;
        int departmentLevel = 1;

        if(!(cid =="")){
            departmentLevel=2;
            if(cid.equals("1")){
                departmentPid=1;
            }else if(cid.equals("2")){
                departmentPid=2;
            }else if(cid.equals("3")){
                departmentPid=3;
            }else if(cid.equals("4")){
                departmentPid=4;
            }else if(cid.equals("5")){
                departmentPid=5;
            }
        }
service.insertDept(departmentName,departmentPid,departmentLevel,"path",departmentDescription);


        PageInfo pageInfo=service.queryAll(1,5); ////
        req.setAttribute("pageInfo",pageInfo);
        req.getRequestDispatcher("/dept/list.jsp").forward(req,resp);


    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doGet(req,resp);
    }
}
