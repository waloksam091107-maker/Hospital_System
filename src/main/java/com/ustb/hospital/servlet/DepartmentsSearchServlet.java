package com.ustb.hospital.servlet;

import com.github.pagehelper.PageInfo;
import com.ustb.hospital.service.DepartmentsServiceImpl;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/departments/search")
public class DepartmentsSearchServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("utf-8");
        resp.setContentType("text/html;charset=utf-8");
        String departmentName = req.getParameter("deptName");
        // 调用服务层方法，但注意这里我们可能需要一个支持按名字查询的方法
        DepartmentsServiceImpl service = new DepartmentsServiceImpl();
        // 假设我们有一个queryByName方法来按名字查询部门，并且它返回一个PageInfo对象（或任何你用来封装查询结果的类）
        PageInfo pageInfo = null; // 初始化为null，因为我们不确定是否有匹配的结果
        if (departmentName != null && !departmentName.trim().isEmpty()) {
pageInfo = service.queryByNameWithPage(1, 5, departmentName);
        } else {
            // 如果没有提供部门名字，可能需要返回一个空的结果集或执行其他操作
            // 这里我们简单地让pageInfo保持为null，然后在JSP中处理这种情况
        }

        // 将查询结果设置到请求属性中
        req.setAttribute("pageInfo", pageInfo);

        // 将请求转发到JSP页面
        req.getRequestDispatcher("/hospro/departmentsList.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doGet(req, resp);
    }
}
