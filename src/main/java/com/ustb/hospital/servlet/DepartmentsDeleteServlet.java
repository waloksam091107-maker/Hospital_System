package com.ustb.hospital.servlet;

import com.ustb.hospital.mapper.DepartmentsMapper;
import com.ustb.hospital.service.DepartmentsServiceImpl;
import com.ustb.hospital.utils.MybatisUtil;
import org.apache.ibatis.session.SqlSession;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/delete")
public class DepartmentsDeleteServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req,resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
int departmentId = Integer.parseInt(req.getParameter("deptId"));
        SqlSession sqlSession = MybatisUtil.getSqlSession();
        DepartmentsMapper mapper = sqlSession.getMapper(DepartmentsMapper.class);
        // 调用删除逻辑
        DepartmentsServiceImpl service =new DepartmentsServiceImpl();
        try {
            service.delete(departmentId);
resp.sendRedirect("/departments/list");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


    }
}
