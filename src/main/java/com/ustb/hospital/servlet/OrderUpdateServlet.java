package com.ustb.hospital.servlet;

import com.ustb.hospital.service.OrdersServiceImpl;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/orders/update")
public class OrderUpdateServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("utf-8");
        String orderId = req.getParameter("orderId");
        String patientId = req.getParameter("patientId");
        String patientName = req.getParameter("patientName");
        String orderType = req.getParameter("orderType");
        String orderDetails = req.getParameter("orderDetails");
        String doctorId = req.getParameter("doctorId");

        OrdersServiceImpl service =new OrdersServiceImpl();
        service.updateOrderDetails(Integer.parseInt(orderId),orderType,orderDetails);
        resp.sendRedirect("/hospro/docOrder.html?doctorId="+doctorId);

    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req,resp);
    }
}
