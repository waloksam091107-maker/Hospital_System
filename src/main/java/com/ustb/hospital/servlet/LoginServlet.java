package com.ustb.hospital.servlet;

import com.ustb.hospital.security.CsrfTokenService;
import com.ustb.hospital.service.AccountSecurityService;
import com.ustb.hospital.service.AuditService;
import com.ustb.hospital.utils.SessionIdentity;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

@WebServlet("/auth/login")
public class LoginServlet extends HttpServlet {
    private final AccountSecurityService accounts;
    public LoginServlet(){this(new AccountSecurityService());}
    public LoginServlet(AccountSecurityService accounts){this.accounts=accounts;}
    @Override protected void doPost(HttpServletRequest req,HttpServletResponse resp)throws IOException{
        req.setCharacterEncoding("UTF-8");
        String code=req.getParameter("roleCode");if(code==null)code=req.getParameter("typeId");
        String role="1".equals(code)?"DOCTOR":"2".equals(code)?"PATIENT":"3".equals(code)?"ADMIN":null;
        if(role==null){resp.sendError(400,"typeId is required and must be 1, 2 or 3");return;}
        AccountSecurityService.LoginResult result=accounts.authenticate(role,req.getParameter("user"),req.getParameter("password"),req.getRemoteAddr());
        if(!result.isSuccessful()){
            new AuditService().record(req,"LOGIN","ACCOUNT",null,"FAILURE");
            resp.sendRedirect(req.getContextPath()+"/hospro/login.html?typeId="+code+"&error=login_failed");return;
        }
        HttpSession previous=req.getSession(false);if(previous!=null)previous.invalidate();
        HttpSession session=req.getSession(true);req.changeSessionId();session.setMaxInactiveInterval(30*60);
        SessionIdentity.establish(session,role,result.getId(),result.getName(),result.getVersion(),result.isMustChangePassword());
        CsrfTokenService.issue(session);new AuditService().record(req,"LOGIN","ACCOUNT",Integer.toString(result.getId()),"SUCCESS");
        resp.sendRedirect(req.getContextPath()+(result.isMustChangePassword()?"/hospro/change-password.html?required=true":"/hospro/index.html?roleCode="+code+"&"+role.toLowerCase()+"Id="+result.getId()));
    }
    @Override protected void doGet(HttpServletRequest req,HttpServletResponse resp)throws IOException{resp.sendError(405,"仅支持 POST 登录请求");}
}
