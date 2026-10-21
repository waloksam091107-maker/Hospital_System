package com.ustb.hospital.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.PageInfo;
import com.ustb.hospital.entity.Departments;
import com.ustb.hospital.service.DepartmentsServiceImpl;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@WebServlet("/first")
public class DepartmentFirstListServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("utf-8");
//响应乱码
        resp.setContentType("application/json;charset=UTF-8");
        DepartmentsServiceImpl service = new DepartmentsServiceImpl();
        List<Departments> list = service.queryFirstDept();
//ajax返回
        ObjectMapper objectmapper = new ObjectMapper();
        String jsonStr = objectmapper.writeValueAsString(list);
        resp.getWriter().write(jsonStr);


    }
    //dopost 另一个方法
}
