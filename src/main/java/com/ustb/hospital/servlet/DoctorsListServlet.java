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

/** Returns the paged doctor catalogue used by the patient booking pages. */
@WebServlet("/doc/list")
public class DoctorsListServlet extends HttpServlet {
    private static final int DEFAULT_PAGE_SIZE = 5;
    private static final int MAX_PAGE_SIZE = 50;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        int pageNumber = positiveOrDefault(request.getParameter("pageNum"), 1);
        int requestedPageSize = positiveOrDefault(request.getParameter("pageSize"), DEFAULT_PAGE_SIZE);
        int pageSize = Math.min(requestedPageSize, MAX_PAGE_SIZE);
        int departmentId = integerOrDefault(request.getParameter("deptId"), -1);

        PageInfo<Doctors> page = new DoctorsServiceImpl().queryByPage(
                pageNumber, pageSize, request.getParameter("jobNumber"), request.getParameter("docName"), departmentId);
        response.setContentType("application/json;charset=UTF-8");
        new ObjectMapper().writeValue(response.getWriter(), page);
    }

    private int positiveOrDefault(String value, int fallback) {
        Integer parsed = parseInteger(value);
        return parsed != null && parsed.intValue() > 0 ? parsed.intValue() : fallback;
    }

    private int integerOrDefault(String value, int fallback) {
        Integer parsed = parseInteger(value);
        return parsed == null ? fallback : parsed.intValue();
    }

    private Integer parseInteger(String value) {
        try {
            return value == null || value.trim().isEmpty() ? null : Integer.valueOf(value.trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
