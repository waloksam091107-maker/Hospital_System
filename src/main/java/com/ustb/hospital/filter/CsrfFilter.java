package com.ustb.hospital.filter;

import com.ustb.hospital.security.CsrfTokenService;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/** Rejects authenticated writes that do not present the session-bound CSRF token. */
public class CsrfFilter implements Filter {
    private final com.ustb.hospital.service.AuditService auditService;
    public CsrfFilter() { this(new com.ustb.hospital.service.AuditService()); }
    public CsrfFilter(com.ustb.hospital.service.AuditService auditService) { this.auditService = auditService; }
    public void init(FilterConfig filterConfig) { }

    public void doFilter(javax.servlet.ServletRequest request, javax.servlet.ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        javax.servlet.http.HttpSession session = httpRequest.getSession(false);
        if (isUnsafeMethod(httpRequest.getMethod()) && session != null &&
                session.getAttribute(com.ustb.hospital.utils.SessionIdentity.CURRENT_USER_ROLE) != null && !CsrfTokenService.verify(httpRequest)) {
            AuditFailureRecorder.record(httpRequest, auditService);
            httpResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
            httpResponse.setContentType("application/json;charset=UTF-8");
            httpResponse.getWriter().write("{\"success\":false,\"code\":\"CSRF_REJECTED\",\"message\":\"请求校验失败，请刷新页面后重试。\"}");
            return;
        }
        chain.doFilter(request, response);
    }

    private static boolean isUnsafeMethod(String method) {
        return "POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)
                || "PATCH".equalsIgnoreCase(method) || "DELETE".equalsIgnoreCase(method);
    }

    public void destroy() { }
}
