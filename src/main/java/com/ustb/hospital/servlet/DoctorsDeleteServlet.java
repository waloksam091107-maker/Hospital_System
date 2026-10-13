package com.ustb.hospital.servlet;

import com.ustb.hospital.mapper.DoctorsMapper;
import com.ustb.hospital.service.DoctorsServiceImpl;
import com.ustb.hospital.utils.MybatisUtil;
import org.apache.ibatis.session.SqlSession;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/doctors/delete")

public class DoctorsDeleteServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req,resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        int doctorId = Integer.parseInt(req.getParameter("doctorId"));
        SqlSession sqlSession = MybatisUtil.getSqlSession();
        DoctorsMapper mapper = sqlSession.getMapper(DoctorsMapper.class);
        // 调用删除逻辑
        DoctorsServiceImpl service =new DoctorsServiceImpl();
        try {
            service.delete(doctorId);
            resp.sendRedirect("/hospro/doctors.jsp");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
