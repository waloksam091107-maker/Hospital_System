package com.ustb.hospital.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ustb.hospital.entity.Admins;
import com.ustb.hospital.service.AdminsServiceImpl;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
@WebServlet("/admins/list-by-id")
public class AdminsListByIdServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //请求乱码
        req.setCharacterEncoding("utf-8");
        //响应乱码
        resp.setContentType("application/json;charset=UTF-8");
        String id = req.getParameter("id");
        AdminsServiceImpl service = new AdminsServiceImpl();
        Admins admin = service.queryById(Integer.parseInt(id));
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(admin);
        resp.getWriter().write(json);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }
}
