package com.ustb.hospital.servlet;

import org.junit.Test;

import javax.servlet.annotation.WebServlet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class AppointmentLifecycleServletContractTest {
    @Test
    public void exposesPostOnlyStartAppointmentEndpoint() throws Exception {
        Class<?> servletType;
        try {
            servletType = Class.forName("com.ustb.hospital.servlet.AppointmentStartServlet");
        } catch (ClassNotFoundException exception) {
            throw new AssertionError("AppointmentStartServlet must exist", exception);
        }
        WebServlet mapping = servletType.getAnnotation(WebServlet.class);
        assertNotNull(mapping);
        assertEquals("/appointments/start", mapping.value()[0]);
        assertNotNull(servletType.getDeclaredMethod("doPost", javax.servlet.http.HttpServletRequest.class,
                javax.servlet.http.HttpServletResponse.class));
        assertNotNull(servletType.getDeclaredMethod("doGet", javax.servlet.http.HttpServletRequest.class,
                javax.servlet.http.HttpServletResponse.class));
    }
}
