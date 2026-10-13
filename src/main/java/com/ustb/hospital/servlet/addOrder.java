package com.ustb.hospital.servlet;

import com.ustb.hospital.service.OrdersServiceImpl;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/orders/create")
public class addOrder extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("utf-8");
        // 获取表单提交的参数
        String doctorId = req.getParameter("doctorId");
        String doctorName = req.getParameter("doctorName");
        String patientId = req.getParameter("patientId");
        String patientName = req.getParameter("patientName");
        String orderType = req.getParameter("orderType");
        String orderDetails = req.getParameter("orderDetails");
        String status = req.getParameter("status");

        // 可以在这里进行后续的处理，比如存储到数据库或者进行其他业务逻辑操作

        // 打印参数验证是否成功接收
OrdersServiceImpl service=new OrdersServiceImpl();
        service.add(Integer.parseInt(doctorId),doctorName,Integer.parseInt(patientId),patientName,orderType,orderDetails,status);
        String redirectUrl = "/hospro/docOrder.html?doctorId=" + doctorId;

        // 执行重定向
        resp.sendRedirect(redirectUrl);


    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req,resp);
    }
}
