package com.ustb.hospital.security;

import org.junit.Test;

import javax.servlet.annotation.WebFilter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SecurityBoundaryContractTest {
    @Test
    public void requiresLoginAndCsrfForScheduleAndPasswordWrites() throws Exception {
        Path authentication = Paths.get("src/main/webapp/WEB-INF/web.xml");
        Path csrf = Paths.get("src/main/java/com/ustb/hospital/filter/CsrfFilter.java");
        String authenticationSource = new String(Files.readAllBytes(authentication), StandardCharsets.UTF_8);
        String csrfSource = new String(Files.readAllBytes(csrf), StandardCharsets.UTF_8);
        assertTrue(authenticationSource.contains("/schedules/*"));
        assertTrue(authenticationSource.contains("/password/reset"));
        assertTrue(authenticationSource.contains("<filter-name>csrf</filter-name><url-pattern>/schedules/*</url-pattern>"));
        assertTrue(authenticationSource.contains("<filter-name>csrf</filter-name><url-pattern>/password/reset</url-pattern>"));
        assertTrue("safe GET requests must continue to reach list endpoints", csrfSource.contains("isUnsafeMethod"));
    }

    @Test
    public void disablesUnsafeLegacyOrderSurface() throws Exception {
        Class<?> filter;
        try {
            filter = Class.forName("com.ustb.hospital.filter.LegacyOrdersDisabledFilter");
        } catch (ClassNotFoundException exception) {
            throw new AssertionError("LegacyOrdersDisabledFilter must exist", exception);
        }
        WebFilter mapping = filter.getAnnotation(WebFilter.class);
        assertNotNull(mapping);
        assertEquals("/orders/*", mapping.urlPatterns()[0]);
    }
}
