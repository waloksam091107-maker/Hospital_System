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
    public AuthenticationFilter() { this(new com.ustb.hospital.service.AuditService()); }
    public AuthenticationFilter(com.ustb.hospital.service.AuditService auditService) { this.auditService = auditService; }
    public void init(FilterConfig filterConfig) { }

    public void doFilter(javax.servlet.ServletRequest request, javax.servlet.ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        HttpSession session = httpRequest.getSession(false);
        if (session == null || session.getAttribute(SessionIdentity.CURRENT_USER_ROLE) == null) {
            AuditFailureRecorder.record(httpRequest, auditService);
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            httpResponse.setContentType("application/json;charset=UTF-8");
            httpResponse.getWriter().write("{\"success\":false,\"code\":\"LOGIN_REQUIRED\",\"message\":\"请先登录。\"}");
            return;
        }
        chain.doFilter(request, response);
    }

    public void destroy() { }
}
