package com.ustb.hospital.filter;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class EncodingFilter implements Filter {
    public void init(FilterConfig filterConfig)throws ServletException{

    }
    public void doFilter(ServletRequest servletRequest,ServletResponse servletResponse,FilterChain filterChain) throws IOException, ServletException {
        HttpServletResponse resp=(HttpServletResponse) servletResponse;
        HttpServletRequest req=(HttpServletRequest) servletRequest;
        req.setCharacterEncoding("utf-8");
        filterChain.doFilter(req,resp);


    }
    public void destroy(){

    }
}
