package com.ustb.hospital.filter;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/** Retires request-parameter based legacy order writes in favour of medical records. */
@WebFilter(urlPatterns = "/orders/*")
public class LegacyOrdersDisabledFilter implements Filter {
    public void init(FilterConfig filterConfig) { }
    public void doFilter(javax.servlet.ServletRequest request, javax.servlet.ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        httpResponse.setStatus(HttpServletResponse.SC_GONE);
        httpResponse.setContentType("application/json;charset=UTF-8");
        httpResponse.getWriter().write("{\"success\":false,\"code\":\"LEGACY_ORDERS_DISABLED\",\"message\":\"旧医嘱接口已停用，请通过就诊记录提交医嘱。\"}");
    }
    public void destroy() { }
}
