package com.ustb.hospital.filter;

import org.junit.Test;

import javax.servlet.FilterChain;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Ensures missing internal pages are handled before Tomcat emits its default 404 view. */
public class MissingHtmlPageFallbackTest {
    @Test
    public void missingHtmlPageRequestsHaveADedicatedFallbackFilter() throws Exception {
        Path filterSource = Paths.get("src", "main", "java", "com", "ustb", "hospital", "filter", "MissingHtmlPageRedirectFilter.java");
        assertTrue("缺失页面应有兼容处理过滤器", Files.isRegularFile(filterSource));
        String source = new String(Files.readAllBytes(filterSource), StandardCharsets.UTF_8);
        assertTrue(source.contains("@WebFilter(\"/*\")"));
        assertTrue(source.contains("endsWith(\".html\")"));
        assertTrue(source.contains("sendRedirect"));
    }

    @Test
    public void missingPatientPageRedirectsToThePatientPortalInsteadOfTomcat404() throws Exception {
        AtomicReference<String> redirect = new AtomicReference<String>();
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        HttpSession session = (HttpSession) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{HttpSession.class},
                (proxy, method, arguments) -> "getAttribute".equals(method.getName()) ? "PATIENT" : null);
        ServletContext servletContext = (ServletContext) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{ServletContext.class},
                (proxy, method, arguments) -> null);
        HttpServletRequest request = (HttpServletRequest) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{HttpServletRequest.class},
                (proxy, method, arguments) -> {
                    if ("getRequestURI".equals(method.getName())) return "/ustbhospital/hospro/patient-dashboard.html";
                    if ("getContextPath".equals(method.getName())) return "/ustbhospital";
                    if ("getServletContext".equals(method.getName())) return servletContext;
                    if ("getSession".equals(method.getName())) return session;
                    return null;
                });
        HttpServletResponse response = (HttpServletResponse) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{HttpServletResponse.class},
                (proxy, method, arguments) -> {
                    if ("sendRedirect".equals(method.getName())) redirect.set((String) arguments[0]);
                    return null;
                });
        FilterChain chain = (request1, response1) -> chainCalled.set(true);

        new MissingHtmlPageRedirectFilter().doFilter(request, response, chain);

        assertEquals("/ustbhospital/hospro/index.html", redirect.get());
        assertFalse("缺失页面不应继续交给 Tomcat 产生 404", chainCalled.get());
    }
}
