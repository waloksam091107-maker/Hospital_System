package com.ustb.hospital.servlet;

import com.ustb.hospital.service.NotificationService;
import com.ustb.hospital.utils.SessionIdentity;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet("/notifications/*")
public class NotificationServlet extends HttpServlet {
    private final NotificationService notifications;
    public NotificationServlet() { this(new NotificationService()); }
    public NotificationServlet(NotificationService notifications) { this.notifications=notifications; }

    @Override protected void doGet(HttpServletRequest request,HttpServletResponse response) throws IOException {
        Integer id=identity(request,response); if(id==null) return;
        String path=path(request), role=SessionIdentity.getCurrentRole(request);
        try {
            if("/list".equals(path) || "/".equals(path)) {
                int page=positive(request.getParameter("page"),1), size=positive(request.getParameter("pageSize"),10);
                if(size>100) throw new IllegalArgumentException();
                PortalServletSupport.writeJson(response,notifications.list(role,id,page,size));
            } else if("/unread-count".equals(path)) {
                Map<String,Object> body=success(); body.put("unreadCount",notifications.countUnread(role,id)); PortalServletSupport.writeJson(response,body);
            } else if("/read".equals(path) || "/read-all".equals(path)) PortalServletSupport.writeMethodNotAllowed(response);
            else PortalServletSupport.writeJsonError(response,404,"NOT_FOUND","通知接口不存在。");
        } catch(IllegalArgumentException invalid) { invalid(response); }
        catch(RuntimeException failure) { failed(response,failure); }
    }

    @Override protected void doPost(HttpServletRequest request,HttpServletResponse response) throws IOException {
        Integer id=identity(request,response); if(id==null) return;
        String role=SessionIdentity.getCurrentRole(request), path=path(request);
        try {
            if("/read".equals(path)) {
                long messageId=Long.parseLong(request.getParameter("id")); if(messageId<=0) throw new IllegalArgumentException();
                if(!notifications.markRead(role,id,messageId)) { PortalServletSupport.writeJsonError(response,404,"NOT_FOUND","通知不存在。"); return; }
                PortalServletSupport.writeJson(response,success());
            } else if("/read-all".equals(path)) {
                Map<String,Object> body=success(); body.put("updated",notifications.markAllRead(role,id)); PortalServletSupport.writeJson(response,body);
            } else if("/list".equals(path)||"/unread-count".equals(path)||"/".equals(path)) PortalServletSupport.writeMethodNotAllowed(response);
            else PortalServletSupport.writeJsonError(response,404,"NOT_FOUND","通知接口不存在。");
        } catch(IllegalArgumentException invalid) { invalid(response); }
        catch(RuntimeException failure) { failed(response,failure); }
    }

    private static Integer identity(HttpServletRequest request,HttpServletResponse response) throws IOException {
        Integer id=SessionIdentity.getCurrentUserId(request);
        if(id==null || id<=0) PortalServletSupport.writeJsonError(response,401,"LOGIN_REQUIRED","请先登录。");
        return id!=null && id>0?id:null;
    }
    private static int positive(String value,int fallback) { if(value==null) return fallback; int number=Integer.parseInt(value); if(number<1) throw new IllegalArgumentException(); return number; }
    private static String path(HttpServletRequest request) { return request.getPathInfo()==null?"/":request.getPathInfo(); }
    private static Map<String,Object> success() { Map<String,Object> result=new LinkedHashMap<>(); result.put("success",true); return result; }
    private static void invalid(HttpServletResponse response) throws IOException { PortalServletSupport.writeJsonError(response,400,"INVALID_REQUEST","消息编号、页码或每页条数无效。"); }
    private static void failed(HttpServletResponse response,RuntimeException failure) throws IOException { Logger.getLogger(NotificationServlet.class.getName()).log(Level.SEVERE,"Notification operation failed",failure); PortalServletSupport.writeJsonError(response,500,"NOTIFICATION_FAILED","通知操作失败，请稍后重试。"); }
}
