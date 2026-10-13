package com.ustb.hospital.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.PageInfo;
import com.ustb.hospital.service.AdminsServiceImpl;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/admin/list")
public class AdminsListServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("utf-8");
        resp.setContentType("text/html;charset=utf-8");
        String pageNum=req.getParameter("pageNum");
        String pageSize=req.getParameter("pageSize");
        int i = 1;
        if(pageNum!=null && pageNum!=" "){
            i = Integer.parseInt(pageNum);
        }


        String username = req.getParameter("username");
        String name = req.getParameter("name");
        String phone = req.getParameter("phone");

        AdminsServiceImpl service = new AdminsServiceImpl();

        PageInfo pageInfo=service.queryByPage(i,5,username,name,phone);
        ObjectMapper objectMapper=new ObjectMapper();
        String jsonStr=objectMapper.writeValueAsString(pageInfo);
        resp.getWriter().write(jsonStr);
    }
}
