package com.ustb.hospital.servlet;

import org.junit.Test;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Guards the state-changing appointment loop endpoints against accidental GET mutations. */
public class AppointmentMutationHttpMethodTest {
    @Test
    public void stateChangingEndpointsRejectGetRequests() throws Exception {
        HttpServlet[] servlets = {
                new AppointmentBookingServlet(),
                new AppointmentsUpdateStatusServlet(),
                new AppointmentsEditServlet(),
                new AppointmentStartServlet(),
                new AppointmentCompletionServlet(),
                new AppointmentsDeleteServlet(),
                new DatesAddServlet(),
                new DatesUpdateServlet(),
                new DateDeleteServlet()
        };

        for (HttpServlet servlet : servlets) {
            AtomicInteger status = new AtomicInteger();
            AtomicReference<String> allow = new AtomicReference<String>();
            StringWriter body = new StringWriter();
            HttpServletRequest request = (HttpServletRequest) Proxy.newProxyInstance(
                    getClass().getClassLoader(), new Class<?>[]{HttpServletRequest.class}, (proxy, method, arguments) -> null);
            HttpServletResponse response = (HttpServletResponse) Proxy.newProxyInstance(
                    getClass().getClassLoader(), new Class<?>[]{HttpServletResponse.class}, (proxy, method, arguments) -> {
                        if ("setStatus".equals(method.getName())) status.set(((Integer) arguments[0]).intValue());
                        if ("setHeader".equals(method.getName()) && "Allow".equals(arguments[0])) allow.set((String) arguments[1]);
                        if ("getWriter".equals(method.getName())) return new PrintWriter(body);
                        return null;
                    });

            Method doGet = servlet.getClass().getDeclaredMethod("doGet", HttpServletRequest.class, HttpServletResponse.class);
            doGet.setAccessible(true);
            doGet.invoke(servlet, request, response);

            String name = servlet.getClass().getSimpleName();
            assertEquals(name + " 的 GET 必须返回 405", HttpServletResponse.SC_METHOD_NOT_ALLOWED, status.get());
            assertEquals(name + " 应声明仅允许 POST", "POST", allow.get());
            assertTrue(name + " 应返回结构化错误", body.toString().contains("METHOD_NOT_ALLOWED"));
        }
    }
}
