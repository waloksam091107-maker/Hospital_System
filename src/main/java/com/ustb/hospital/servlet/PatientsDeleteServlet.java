package com.ustb.hospital.servlet;

import com.ustb.hospital.mapper.PatientsMapper;
import com.ustb.hospital.service.PatientsServiceImpl;
import com.ustb.hospital.utils.MybatisUtil;
import org.apache.ibatis.session.SqlSession;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/deletepatient")

public class PatientsDeleteServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req,resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        int patientId = Integer.parseInt(req.getParameter("patientId"));
        SqlSession sqlSession = MybatisUtil.getSqlSession();
        PatientsMapper mapper = sqlSession.getMapper(PatientsMapper.class);
        // 调用删除逻辑

        PatientsServiceImpl service =new PatientsServiceImpl();
        try {

            service.delete(patientId);
            resp.sendRedirect("/hospro/my-patients.jsp");

        } catch (Exception e) {
            throw new RuntimeException(e);

        }

    }
}
