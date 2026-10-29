package com.ustb.hospital.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.PageInfo;
import com.ustb.hospital.service.PatientsServiceImpl;
import com.ustb.hospital.utils.SessionIdentity;

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
        if (PortalServletSupport.requireRole(req, resp, SessionIdentity.ROLE_ADMIN) == null) return;
        req.setCharacterEncoding("utf-8");
        resp.setContentType("application/json;charset=UTF-8");
        int page, size;
        try {
            page = req.getParameter("pageNum") == null ? 1 : Integer.parseInt(req.getParameter("pageNum"));
            size = req.getParameter("pageSize") == null ? 5 : Integer.parseInt(req.getParameter("pageSize"));
            if (page <= 0 || size <= 0 || size > 100) throw new IllegalArgumentException();
        } catch (IllegalArgumentException invalid) {
            PortalServletSupport.writeJsonError(resp, 400, "INVALID_REQUEST", "页码必须大于 0，每页记录数应为 1 至 100。");
            return;
        }
        String idCardNumber = req.getParameter("idCardNumber");
        String name = req.getParameter("name");
        String phone = req.getParameter("phone");
        PatientsServiceImpl service = new PatientsServiceImpl();
        PageInfo pageInfo=service.queryByPage(page,size,idCardNumber,name,phone);

        ObjectMapper objectMapper=new ObjectMapper();
        String jsonStr=objectMapper.writeValueAsString(pageInfo);
        resp.getWriter().write(jsonStr);
    }
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doGet(req,resp);
    }
}
