package com.ustb.hospital.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.PageInfo;
import com.ustb.hospital.service.PatientsServiceImpl;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet({"/portal/patients", "/patient/list"})
public class PortalPatientListServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("utf-8");
        resp.setContentType("application/json;charset=UTF-8");
        String pageNum=req.getParameter("pageNum");
        String pageSize=req.getParameter("pageSize");
        int i = 1;
        if(pageNum!=null && pageNum!=" "){
            i = Integer.parseInt(pageNum);
        }
        String idCardNumber = req.getParameter("idCardNumber");
        String name = req.getParameter("name");
        String phone = req.getParameter("phone");
        PatientsServiceImpl service = new PatientsServiceImpl();
        PageInfo pageInfo=service.queryByPage(i,5,idCardNumber,name,phone);

        ObjectMapper objectMapper=new ObjectMapper();
        String jsonStr=objectMapper.writeValueAsString(pageInfo);
        resp.getWriter().write(jsonStr);
    }
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doGet(req,resp);
    }
}
