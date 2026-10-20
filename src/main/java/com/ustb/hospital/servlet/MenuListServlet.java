package com.ustb.hospital.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.PageInfo;
import com.ustb.hospital.service.MenuServiceImpl;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/menu/list")
public class MenuListServlet extends HttpServlet {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        writePage(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        writePage(req, resp);
    }

    private void writePage(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");

        Integer pageNum = AppointmentServletSupport.parsePositiveInt(req.getParameter("pageNum"));
        if (pageNum == null) {
            AppointmentServletSupport.writeBadRequest(resp, "页码必须为正整数。");
            return;
        }
        String category = req.getParameter("fenlei");
        if (category == null || category.trim().isEmpty()) category = "-1";

        PageInfo pageInfo = new MenuServiceImpl().queryByPage(pageNum.intValue(), category);
        OBJECT_MAPPER.writeValue(resp.getWriter(), pageInfo);
    }
}
