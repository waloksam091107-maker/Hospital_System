package com.ustb.hospital.filter;

import org.junit.Test;

import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class EncodingFilterTest {
    @Test
    public void setsCharacterEncodingWithoutOverwritingTheResponseMimeType() throws Exception {
        AtomicReference<String> requestEncoding = new AtomicReference<String>();
        AtomicReference<String> responseEncoding = new AtomicReference<String>();
        AtomicReference<String> responseContentType = new AtomicReference<String>();
        AtomicBoolean chainCalled = new AtomicBoolean(false);

        HttpServletRequest request = (HttpServletRequest) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{HttpServletRequest.class},
                (proxy, method, arguments) -> {
                    if ("setCharacterEncoding".equals(method.getName())) {
                        requestEncoding.set((String) arguments[0]);
                    }
                    return null;
                });
        HttpServletResponse response = (HttpServletResponse) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{HttpServletResponse.class},
                (proxy, method, arguments) -> {
                    if ("setCharacterEncoding".equals(method.getName())) {
                        responseEncoding.set((String) arguments[0]);
                    } else if ("setContentType".equals(method.getName())) {
                        responseContentType.set((String) arguments[0]);
                    }
                    return null;
                });
        FilterChain chain = new FilterChain() {
            @Override
            public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse) {
                chainCalled.set(true);
            }
        };

        new EncodingFilter().doFilter(request, response, chain);

        assertEquals("UTF-8", requestEncoding.get());
        assertEquals("UTF-8", responseEncoding.get());
        assertNull("A global encoding filter must not label CSS and JavaScript as HTML.",
                responseContentType.get());
        assertTrue(chainCalled.get());
    }
}
