package com.ustb.hospital.servlet;

import com.ustb.hospital.entity.Patients;
import com.ustb.hospital.service.PatientsServiceImpl;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet("/patients/pay")
public class PatientsPayServlet extends HttpServlet {
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //请求乱码
        req.setCharacterEncoding("utf-8");
        //响应乱码
        resp.setContentType("text/html;charset=utf-8");


        String idCardNumber= req.getParameter("idCardNumber");
        String password = req.getParameter("password");
        String amountStr = req.getParameter("amount");
        PatientsServiceImpl service = new PatientsServiceImpl();
        Patients patients = service.queryByIdCardNumber(idCardNumber);
        if(patients == null){
            // 账户不存在
            PrintWriter out = resp.getWriter();
            out.println("账户不存在！");
            return;
        }
        if(!patients.getPassword().equals(password)){
            // 密码错误
            PrintWriter out = resp.getWriter();
            out.println("密码错误！");
            return;
        }
        int id = patients.getPatientId();
// 转换类型
        double amount = Double.parseDouble(amountStr);


        boolean result = service.pay(idCardNumber, amount);

        // 获取PrintWriter对象用于向客户端发送响应
        PrintWriter out = resp.getWriter();

        if (result) {
            // 支付成功
            out.println("支付成功！");
            // 转到首页

            resp.sendRedirect(req.getContextPath() + "/hospro/index.html?typeId=2&patientId="+id);
        } else {
            // 支付失败
            out.println("支付失败！");
            // 转到支付页面
            resp.sendRedirect(req.getContextPath() + "/hospro/patients-pay.html?typeId=2&patientId="+id);
        }

    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }
}
