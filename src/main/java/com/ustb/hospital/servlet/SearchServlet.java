package com.ustb.hospital.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ustb.hospital.entity.Departments;
import com.ustb.hospital.service.DepartmentsServiceImpl;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@WebServlet("/find")
public class SearchServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // 设置请求和响应的字符编码
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("text/html;charset=UTF-8");


        DepartmentsServiceImpl service=new DepartmentsServiceImpl();
        List<Departments> list = service.queryAllSecondLevelDepartments();
        ObjectMapper objectMapper=new ObjectMapper();

        String keyword = req.getParameter("keyword");


        List<Departments> matchingDepartments = new ArrayList<>();
                // 遍历原始列表，找到名称包含关键字的部门对象
        for (Departments department : list) {
            if (department.getDepartmentName().contains(keyword)) {
                matchingDepartments.add(department);
            }
        }
        String jsonStr=objectMapper.writeValueAsString(matchingDepartments);
        resp.setCharacterEncoding("utf-8");
        resp.getWriter().write(jsonStr);
    }


}
