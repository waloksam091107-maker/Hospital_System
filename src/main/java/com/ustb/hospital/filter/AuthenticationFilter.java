package com.ustb.hospital.filter;

import com.ustb.hospital.utils.SessionIdentity;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/** Requires an authenticated session for the new appointment API surface. */
public class AuthenticationFilter implements Filter {
    private final com.ustb.hospital.service.AuditService auditService;
    private final com.ustb.hospital.service.AccountSecurityService accounts;
    public AuthenticationFilter() { this(new com.ustb.hospital.service.AuditService()); }
    public AuthenticationFilter(com.ustb.hospital.service.AuditService auditService) { this(auditService,new com.ustb.hospital.service.AccountSecurityService()); }
    public AuthenticationFilter(com.ustb.hospital.service.AuditService auditService, com.ustb.hospital.service.AccountSecurityService accounts) { this.auditService = auditService; this.accounts=accounts; }
    public void init(FilterConfig filterConfig) { }

    public void doFilter(javax.servlet.ServletRequest request, javax.servlet.ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        HttpSession session = httpRequest.getSession(false);
        Integer id=SessionIdentity.getCurrentUserId(httpRequest);
        Object version=session==null?null:session.getAttribute(SessionIdentity.ACCOUNT_SESSION_VERSION);
        com.ustb.hospital.service.AccountSecurityService.AccountState state=null;
        try { if(id!=null && version instanceof Long) state=accounts.validateSession(SessionIdentity.getCurrentRole(httpRequest),id,((Long)version).longValue()); }
        catch(RuntimeException unavailable) {
            httpResponse.setStatus(503);httpResponse.setContentType("application/json;charset=UTF-8");httpResponse.getWriter().write("{\"success\":false,\"code\":\"SECURITY_UNAVAILABLE\",\"message\":\"账号验证暂时不可用，请稍后重试。\"}");return;
        }
        if (state == null) {
            AuditFailureRecorder.record(httpRequest, auditService);
            if(session!=null)session.invalidate();
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            httpResponse.setContentType("application/json;charset=UTF-8");
            httpResponse.getWriter().write("{\"success\":false,\"code\":\"LOGIN_REQUIRED\",\"message\":\"请先登录。\"}");
            return;
        }
        String path=httpRequest.getServletPath();
        if(state.mustChangePassword && !"/password/reset".equals(path) && !"/security/csrf".equals(path) && !"/auth/logout".equals(path)) {
            AuditFailureRecorder.record(httpRequest,auditService);httpResponse.setStatus(403);httpResponse.setContentType("application/json;charset=UTF-8");
            httpResponse.getWriter().write("{\"success\":false,\"code\":\"PASSWORD_CHANGE_REQUIRED\",\"message\":\"请先修改临时密码。\"}");return;
        }
        chain.doFilter(request, response);
    }

    public void destroy() { }
}
