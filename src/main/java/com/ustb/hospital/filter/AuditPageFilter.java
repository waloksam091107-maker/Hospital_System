package com.ustb.hospital.filter;
import com.ustb.hospital.service.AuditService;
import com.ustb.hospital.utils.SessionIdentity;
import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.*;
import java.io.IOException;

@WebFilter(urlPatterns = "/hospro/audit-logs.html")
public class AuditPageFilter implements Filter {
    public void init(FilterConfig config) { }
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req=(HttpServletRequest)request; HttpServletResponse resp=(HttpServletResponse)response;
        HttpSession session=req.getSession(false);
        if (session == null || session.getAttribute(SessionIdentity.CURRENT_USER_ROLE) == null) {
            AuditFailureRecorder.record(req,new AuditService()); resp.sendError(HttpServletResponse.SC_UNAUTHORIZED,"请先登录。"); return;
        }
        if (!SessionIdentity.isAdmin(req)) {
            AuditFailureRecorder.record(req,new AuditService()); resp.sendError(HttpServletResponse.SC_FORBIDDEN,"当前账号无权访问审计日志。"); return;
        }
        chain.doFilter(request,response);
    }
    public void destroy() { }
}
