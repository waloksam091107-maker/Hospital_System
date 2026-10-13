package com.ustb.hospital.servlet;

import com.ustb.hospital.entity.Admins;
import com.ustb.hospital.mapper.AdminsMapper;
import com.ustb.hospital.service.AdminsServiceImpl;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/login")
public class AdminsLoginServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String username=req.getParameter("username");
        String password=req.getParameter("password");

        AdminsServiceImpl adminsService=new AdminsServiceImpl();
        Admins admins=adminsService.queryByUsername(username);
        if(admins==null){
//1.转发
            req.getRequestDispatcher("/login.jsp").forward(req,resp);

        }else{
            if(!admins.getPassword().equals(password)){  //不成立的情况
//登录失败，回到登录页面
                req.getRequestDispatcher("/login.jsp").forward(req,resp);
            }else{
                req.getRequestDispatcher("/index.jsp").forward(req,resp);



            }
        }

    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doGet(req,resp);
    }
}
