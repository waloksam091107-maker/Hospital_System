package com.ustb.hospital.servlet;

import com.ustb.hospital.service.DoctorsServiceImpl;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/docEdit")
public class DoctorsEditServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //请求乱码
        req.setCharacterEncoding("UTF-8");
        //响应乱码
        resp.setContentType("text/html;charset=UTF-8");
        int doctorId = Integer.parseInt(req.getParameter("doctorId"));
        String phone = req.getParameter("phone");
        String registrationFee = req.getParameter("registrationFee");
        String professionalTitleId = req.getParameter("professionalTitleId");


        DoctorsServiceImpl service = new DoctorsServiceImpl();
try {
            service.updateDoctorProfile(doctorId,phone,registrationFee,professionalTitleId);
            //修改成功
resp.sendRedirect("/hospro/doctors.jsp?msg=修改成功");
        } catch (Exception e) {
            e.printStackTrace();
            resp.sendRedirect("/doctors/edit-page?doctorId=" + doctorId);
        }

}}
