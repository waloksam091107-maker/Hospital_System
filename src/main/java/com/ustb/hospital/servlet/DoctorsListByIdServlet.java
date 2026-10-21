package com.ustb.hospital.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ustb.hospital.entity.Doctors;
import com.ustb.hospital.service.DoctorsServiceImpl;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/doctor/list")
public class DoctorsListByIdServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //请求乱码
        req.setCharacterEncoding("utf-8");
        //响应乱码
        resp.setContentType("application/json;charset=UTF-8");

        String id = req.getParameter("id");
        DoctorsServiceImpl service = new DoctorsServiceImpl();
        Doctors doctor = service.queryById(Integer.parseInt(id));
        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(doctor);
        resp.getWriter().write(json);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }
}
