package com.ustb.hospital.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.PageInfo;
import com.ustb.hospital.entity.Departments;
import com.ustb.hospital.service.DepartmentsServiceImpl;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/departments/delete")
public class DepartmentDelServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
       this.doPost(req,resp);

    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
     try {
int departmentId = Integer.parseInt(req.getParameter("id"));
// 调用删除逻辑
        DepartmentsServiceImpl service =new DepartmentsServiceImpl();
        //判断是否可以删除
         List<Departments> list= service.queryByPid(departmentId);

         if(list.isEmpty()){
             //可以删除
             service.delById(departmentId);
             resp.getWriter().write("1");
         }else{
             //不能删除
             resp.getWriter().write("0");
         }

    } catch (Exception e) {
e.printStackTrace();
    }

    }





}
