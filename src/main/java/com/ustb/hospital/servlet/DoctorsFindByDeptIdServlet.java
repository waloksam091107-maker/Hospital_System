package com.ustb.hospital.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.PageInfo;
import com.ustb.hospital.entity.Doctors;
import com.ustb.hospital.service.DoctorsServiceImpl;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/** Lists doctors for a selected department in the patient booking catalogue. */
@WebServlet("/doc/findByDeptId")
public class DoctorsFindByDeptIdServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Integer departmentId = AppointmentServletSupport.parsePositiveInt(request.getParameter("departmentId"));
        if (departmentId == null) {
            AppointmentServletSupport.writeBadRequest(response, "departmentId 必须是正整数。");
            return;
        }
        int pageNumber = AppointmentServletSupport.parsePage(request.getParameter("pageNum"));
        PageInfo<Doctors> page = new DoctorsServiceImpl().queryByDeptIdToPage(pageNumber, 5, departmentId.intValue());
        response.setContentType("application/json;charset=UTF-8");
        new ObjectMapper().writeValue(response.getWriter(), page);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }
}
