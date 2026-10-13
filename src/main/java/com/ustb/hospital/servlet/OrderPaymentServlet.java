package com.ustb.hospital.servlet;

import com.ustb.hospital.service.OrdersServiceImpl;
import com.ustb.hospital.service.PatientsServiceImpl;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/orders/payment")
public class OrderPaymentServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String patientId = req.getParameter("patientId");
        String status = req.getParameter("status");
        String orderId=req.getParameter("orderId");

        PatientsServiceImpl service=new PatientsServiceImpl();
        String balanceText = service.getBalById(Integer.parseInt(patientId));
        double balance = 0;
        double number = 0;
        String paidStatus="";

        try {
            balance = Double.parseDouble(balanceText);
            String numberStr = status.substring(3); // 获取"待缴费30"中的"30"
            paidStatus="已缴费"+numberStr;
            number = Double.parseDouble(numberStr); // 将"30"转换为数字30
        } catch (NumberFormatException e) {
            return;
        }

        if (balance >= number) {
            double remainingBalance = balance - number;
            String remainingBalanceText = String.valueOf(remainingBalance);

            service.updateBal(Integer.parseInt(patientId),remainingBalanceText);
            OrdersServiceImpl ordersServlet=new OrdersServiceImpl();
            ordersServlet.updateOrderStatus(Integer.parseInt(orderId),paidStatus);
            //缴费成功
            resp.getWriter().write("1");

        } else {

            resp.getWriter().write("0");
        }




    }
}
