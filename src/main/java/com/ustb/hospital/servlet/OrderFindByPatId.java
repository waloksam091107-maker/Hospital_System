package com.ustb.hospital.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.PageInfo;
import com.ustb.hospital.service.OrdersServiceImpl;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/orders/patient")
public class OrderFindByPatId extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");

        String patientId = req.getParameter("patientId");

        String pageNum=req.getParameter("pageNum");
        String pageSize=req.getParameter("pageSize");
        int i=1;
        if(pageNum!=null && pageNum!=""){
            i=Integer.parseInt(pageNum);
        }
OrdersServiceImpl service=new OrdersServiceImpl();
        PageInfo pageInfo=service.findByPatId(i,5,Integer.parseInt(patientId));
//ajax返回
        ObjectMapper objectmapper = new ObjectMapper();
        String jsonStr = objectmapper.writeValueAsString(pageInfo);


        resp.getWriter().write(jsonStr);


    }
}
