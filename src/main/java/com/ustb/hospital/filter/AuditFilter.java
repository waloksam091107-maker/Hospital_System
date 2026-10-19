package com.ustb.hospital.filter;

import com.ustb.hospital.service.AuditService;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;

/** Records metadata for protected mutations after the endpoint has produced its response. */
public class AuditFilter implements Filter {
    private final AuditService auditService;
    public AuditFilter() { this(new AuditService()); }
    public AuditFilter(AuditService auditService) { this.auditService = auditService; }
    public void init(FilterConfig filterConfig) { }

    public void doFilter(javax.servlet.ServletRequest request, javax.servlet.ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        StatusCaptureResponse responseWrapper = new StatusCaptureResponse((HttpServletResponse) response);
        boolean thrown = true;
        try {
            chain.doFilter(request, responseWrapper);
            thrown = false;
        } finally {
            if (thrown || responseWrapper.status >= 400) AuditFailureRecorder.record(httpRequest, auditService);
        }
    }

    private static String action(String path) { return path == null ? "WRITE" : path.replace('/', '_').toUpperCase(); }
    private static String first(String first, String second) { return first != null ? first : second; }
    private static boolean isUnsafe(String method) { return "POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method) || "PATCH".equalsIgnoreCase(method) || "DELETE".equalsIgnoreCase(method); }
    public void destroy() { }

    private static final class StatusCaptureResponse extends HttpServletResponseWrapper {
        private int status = HttpServletResponse.SC_OK;
        private StatusCaptureResponse(HttpServletResponse response) { super(response); }
        @Override public void setStatus(int statusCode) { status = statusCode; super.setStatus(statusCode); }
        @Override public void sendError(int statusCode) throws IOException { status = statusCode; super.sendError(statusCode); }
        @Override public void sendError(int statusCode, String message) throws IOException { status = statusCode; super.sendError(statusCode, message); }
        @Override public void sendRedirect(String location) throws IOException { status = HttpServletResponse.SC_FOUND; super.sendRedirect(location); }
    }
}
