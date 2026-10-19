package com.ustb.hospital.servlet;

import org.junit.Test;

import javax.servlet.annotation.WebServlet;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MedicalRecordsEndpointContractTest {
    @Test
    public void exposesTheMedicalRecordApiBehindOneRoleCheckedServlet() throws Exception {
        Class<?> servlet;
        try {
            servlet = Class.forName("com.ustb.hospital.servlet.MedicalRecordsServlet");
        } catch (ClassNotFoundException exception) {
            throw new AssertionError("MedicalRecordsServlet must exist", exception);
        }
        WebServlet mapping = servlet.getAnnotation(WebServlet.class);
        assertNotNull(mapping);
        assertEquals("/medical-records/*", mapping.value()[0]);
        assertNotNull(servlet.getDeclaredMethod("doGet", javax.servlet.http.HttpServletRequest.class, javax.servlet.http.HttpServletResponse.class));
        assertNotNull(servlet.getDeclaredMethod("doPost", javax.servlet.http.HttpServletRequest.class, javax.servlet.http.HttpServletResponse.class));
    }

    @Test
    public void protectsFinalizationWithAuthenticationAndCsrf() throws Exception {
        Path authentication = Paths.get("src/main/webapp/WEB-INF/web.xml");
        Path csrf = authentication;
        String authenticationSource = new String(Files.readAllBytes(authentication), StandardCharsets.UTF_8);
        String csrfSource = new String(Files.readAllBytes(csrf), StandardCharsets.UTF_8);
        assertTrue(authenticationSource.contains("<filter-name>authentication</filter-name><url-pattern>/medical-records/*</url-pattern>"));
        assertTrue(csrfSource.contains("<filter-name>csrf</filter-name><url-pattern>/medical-records/finalize</url-pattern>"));
    }
}
