package com.ustb.hospital.security;

import org.junit.Test;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AppointmentRequestSecurityContractTest {
    @Test
    public void issuesUnpredictableTokensAndRejectsMismatches() throws Exception {
        Class<?> service = load("com.ustb.hospital.security.CsrfTokenService");
        Method newToken = service.getMethod("newToken");
        Method matches = service.getMethod("matches", String.class, String.class);
        String first = (String) newToken.invoke(null);
        String second = (String) newToken.invoke(null);
        assertTrue(first.length() >= 32);
        assertFalse(first.equals(second));
        assertTrue(((Boolean) matches.invoke(null, first, first)).booleanValue());
        assertFalse(((Boolean) matches.invoke(null, first, second)).booleanValue());
    }

    @Test
    public void protectsAppointmentRoutesWithAuthenticationAndCsrfFilters() throws Exception {
        String descriptor = new String(Files.readAllBytes(Paths.get("src/main/webapp/WEB-INF/web.xml")), StandardCharsets.UTF_8);
        assertTrue(descriptor.contains("com.ustb.hospital.filter.AuthenticationFilter"));
        assertTrue(descriptor.contains("com.ustb.hospital.filter.CsrfFilter"));
        assertTrue(descriptor.contains("<filter-name>authentication</filter-name><url-pattern>/appointments/*</url-pattern>"));
        assertTrue(descriptor.contains("<filter-name>csrf</filter-name><url-pattern>/appointments/book</url-pattern>"));
    }

    private static Class<?> load(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException exception) {
            throw new AssertionError(name + " must exist", exception);
        }
    }
}
