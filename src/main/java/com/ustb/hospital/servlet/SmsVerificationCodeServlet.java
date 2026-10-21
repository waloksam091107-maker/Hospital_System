package com.ustb.hospital.servlet;

import com.aliyuncs.exceptions.ClientException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ustb.hospital.utils.SMSUtils;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/verification/sms-code")
public class SmsVerificationCodeServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String phoneNumber = req.getParameter("phoneNumber");
String randomCode = String.valueOf((int) (Math.random() * 9000 + 1000)); //4位数验证码
        String content = "{\"code\":\""+randomCode+"\"}";
        try {
            SMSUtils.send(phoneNumber,"医通","SMS_469075527",content); //发送短信
            ObjectMapper objectMapper=new ObjectMapper();
            String jsonStr=objectMapper.writeValueAsString(randomCode);
            resp.setContentType("application/json;charset=UTF-8");

            resp.getWriter().write(jsonStr);

        }
        catch (ClientException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doGet(req,resp);
    }
}
