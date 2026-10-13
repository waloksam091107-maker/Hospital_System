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

@WebServlet("/orders/doctor")
public class OrdersFindByDocId extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req,resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("text/html;charset=UTF-8");

        String pageNum=req.getParameter("pageNum");
        String pageSize=req.getParameter("pageSize");
        int i=1;
        if(pageNum!=null && pageNum!=""){
            i=Integer.parseInt(pageNum);
        }
String doctorId = req.getParameter("doctorId");
        OrdersServiceImpl service=new OrdersServiceImpl();
        PageInfo pageInfo=service.findByDocId(i,5,Integer.parseInt(doctorId));
//ajax返回
        ObjectMapper objectmapper = new ObjectMapper();
        String jsonStr = objectmapper.writeValueAsString(pageInfo);


        resp.getWriter().write(jsonStr);
    }
}
