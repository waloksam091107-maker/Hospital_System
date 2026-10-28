package com.ustb.hospital.servlet;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

@WebServlet("/auth/logout")
public class LogoutServlet extends HttpServlet {
    @Override protected void doPost(HttpServletRequest req,HttpServletResponse resp)throws IOException{
        HttpSession session=req.getSession(false);if(session!=null)session.invalidate();
        resp.setContentType("application/json;charset=UTF-8");resp.getWriter().write("{\"success\":true}");
    }
    @Override protected void doGet(HttpServletRequest req,HttpServletResponse resp)throws IOException{resp.sendError(405,"请通过 POST 退出登录。");}
}
