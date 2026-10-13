package com.ustb.hospital.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.PageInfo;
import com.ustb.hospital.service.DoctorsServiceImpl;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/portal/doctors")
public class PortalDoctorListServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
}
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("utf-8");
        resp.setContentType("text/html;charset=utf-8");
        String pageNum=req.getParameter("pageNum");
        String pageSize=req.getParameter("pageSize");
        int i = 1;
        if(pageNum!=null && pageNum!=" "){
            i = Integer.parseInt(pageNum);
        }

        String jobNumber = req.getParameter("jobNumber");
        String docName = req.getParameter("docName");
        String deptstrId = req.getParameter("deptId");
        if (deptstrId == null || deptstrId.isEmpty()) {
            deptstrId = "-1"; // 将空或null字符串转换为 "-1" 字符串
        }// 将字符串转换为整数
        int deptId = Integer.parseInt(deptstrId);
        DoctorsServiceImpl service = new DoctorsServiceImpl();
        PageInfo pageInfo = service.queryByPage(i,5,jobNumber,docName,deptId);
        ObjectMapper objectMapper=new ObjectMapper();
        String jsonStr=objectMapper.writeValueAsString(pageInfo);
        resp.getWriter().write(jsonStr);
    }

}
