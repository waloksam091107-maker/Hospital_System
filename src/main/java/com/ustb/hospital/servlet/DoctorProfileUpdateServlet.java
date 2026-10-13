package com.ustb.hospital.servlet;

import com.ustb.hospital.service.DoctorsServiceImpl;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/portal/doctors/profile")
public class DoctorProfileUpdateServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //请求乱码
        req.setCharacterEncoding("UTF-8");
        //响应乱码
        resp.setContentType("text/html;charset=UTF-8");
        int doctorId = Integer.parseInt(req.getParameter("doctorId"));
        String phone = req.getParameter("phone");
        String email = req.getParameter("email");
        String introduction = req.getParameter("introduction");
        String registrationFee = req.getParameter("registrationFee");
        String entryDate = req.getParameter("entryDate");
        String departmentIdStr = req.getParameter("departmentId");
        String professionalTitleId = req.getParameter("professionalTitleId");

        DoctorsServiceImpl service = new DoctorsServiceImpl();
        try {
            //doctorId,phone,email,introduction,registrationFee, entryDate,departmentId,professionalTitleId
            service.updateDoctorProfileDetails(doctorId, phone,email,introduction, registrationFee,entryDate,Integer.parseInt(departmentIdStr), professionalTitleId);
            //修改成功
            resp.sendRedirect("/hospro/doctor-profile.html?doctorId=" + doctorId);
        } catch (Exception e) {
            e.printStackTrace();
            resp.sendRedirect("/portal/doctors/profile/edit?doctorId=" + doctorId);
        }
    }
}
