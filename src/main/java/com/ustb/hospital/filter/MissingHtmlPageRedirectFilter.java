package com.ustb.hospital.filter;

import com.ustb.hospital.utils.SessionIdentity;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * Keeps old template navigation from ending at Tomcat's default 404 page.
 * Only missing HTML/JSP navigation targets are redirected; missing assets and API routes keep normal handling.
 */
@WebFilter("/*")
public class MissingHtmlPageRedirectFilter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) {
        // No filter configuration is required.
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        if (!(servletRequest instanceof HttpServletRequest) || !(servletResponse instanceof HttpServletResponse)) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        String requestPath = request.getRequestURI().substring(request.getContextPath().length());
        if ("/hospro/login.html".equals(requestPath)) {
            response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Expires", "0");
        }
        if (!isHtmlPage(requestPath) || request.getServletContext().getResource(requestPath) != null) {
            filterChain.doFilter(request, response);
            return;
        }
        response.sendRedirect(request.getContextPath() + fallbackPage(request));
    }

    private boolean isHtmlPage(String requestPath) {
        return requestPath.endsWith(".html") || requestPath.endsWith(".htm") || requestPath.endsWith(".jsp");
    }

    private String fallbackPage(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        String role = session == null ? null : (String) session.getAttribute(SessionIdentity.CURRENT_USER_ROLE);
        if (SessionIdentity.ROLE_DOCTOR.equals(role)) return "/doctor-schedule/date.html";
        if (SessionIdentity.ROLE_PATIENT.equals(role)) return "/hospro/index.html";
        if (SessionIdentity.ROLE_ADMIN.equals(role)) return "/hospro/adm_main.jsp";
        return "/hospro/login.html";
    }

    @Override
    public void destroy() {
        // No resources are retained by this filter.
    }
}
