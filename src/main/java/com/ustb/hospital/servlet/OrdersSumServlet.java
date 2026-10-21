package com.ustb.hospital.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ustb.hospital.entity.Appointments;
import com.ustb.hospital.entity.Orders;
import com.ustb.hospital.service.AppointmentsServiceImpl;
import com.ustb.hospital.service.OrdersServiceImpl;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@WebServlet("/admins/sum")
public class OrdersSumServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("utf-8");
        resp.setContentType("application/json;charset=UTF-8");

        String date = req.getParameter("date");
        if (date == null) {
            resp.getWriter().write("日期参数为空");
return;
        }

OrdersServiceImpl orderService = new OrdersServiceImpl();
        AppointmentsServiceImpl appointmentsService = new AppointmentsServiceImpl();

        List<Appointments> listApp = appointmentsService.queryByDate(date);
ObjectMapper mapper = new ObjectMapper();
        List<Orders> list = new ArrayList<>();
        if (listApp == null || listApp.isEmpty()) {
            resp.getWriter().write(mapper.writeValueAsString(list));
            return;
        }
        Set<Integer> processedDoctorIds = new HashSet<>();
        for (Appointments app : listApp) {
            int doctorId = app.getDoctorId();
            if (!processedDoctorIds.contains(doctorId)) {
                List<Orders> listOrder = orderService.queryByDoctorId(doctorId);
                if (listOrder != null && !listOrder.isEmpty()) {
                    list.addAll(listOrder);
                }
                processedDoctorIds.add(doctorId);
            }
        }
        resp.getWriter().write(mapper.writeValueAsString(list));
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }
}
