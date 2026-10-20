package com.ustb.hospital.servlet;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Guards legacy endpoints against the historical misspelled test/json response type. */
public class LegacyJsonServletContentTypeTest {
    @Test
    public void legacyReadEndpointsDeclareUtf8JsonAndFormEndpointDeclaresHtml() throws Exception {
        Path servletDirectory = Paths.get("src", "main", "java", "com", "ustb", "hospital", "servlet");
        String categories = read(servletDirectory.resolve("MenuCategoryServlet.java"));
        String list = read(servletDirectory.resolve("MenuListServlet.java"));
        String doctorSave = read(servletDirectory.resolve("DoctorsSaveServlet.java"));

        assertFalse(categories.contains("test/json"));
        assertFalse(list.contains("test/json"));
        assertFalse(doctorSave.contains("test/json"));
        assertTrue(categories.contains("application/json;charset=UTF-8"));
        assertTrue(list.contains("application/json;charset=UTF-8"));
        assertTrue(doctorSave.contains("text/html;charset=UTF-8"));
    }

    private String read(Path path) throws Exception {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }
}
