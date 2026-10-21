package com.ustb.hospital.servlet;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        String patientById = read(servletDirectory.resolve("PatientGetById.java"));
        String secondDepartments = read(servletDirectory.resolve("DepartmentsSecondAllServlet.java"));
        String departmentChildren = read(servletDirectory.resolve("DepartmentHasChildrenServlet.java"));

        assertFalse(categories.contains("test/json"));
        assertFalse(list.contains("test/json"));
        assertFalse(doctorSave.contains("test/json"));
        assertFalse(patientById.contains("test/json"));
        assertFalse(secondDepartments.contains("text/json"));
        assertTrue(categories.contains("application/json;charset=UTF-8"));
        assertTrue(list.contains("application/json;charset=UTF-8"));
        assertTrue(patientById.contains("application/json;charset=UTF-8"));
        assertTrue(secondDepartments.contains("application/json;charset=UTF-8"));
        assertTrue(departmentChildren.contains("application/json;charset=UTF-8"));
        assertTrue(doctorSave.contains("text/html;charset=UTF-8"));
    }

    @Test
    public void everyServletThatSerializesJsonDeclaresUtf8Json() throws Exception {
        Path servletDirectory = Paths.get("src", "main", "java", "com", "ustb", "hospital", "servlet");
        List<Path> jsonServlets;
        try (Stream<Path> paths = Files.walk(servletDirectory)) {
            jsonServlets = paths
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> {
                        try {
                            String source = read(path);
                            return source.contains("writeValueAsString") || source.contains(".writeValue(");
                        } catch (Exception exception) {
                            throw new IllegalStateException(exception);
                        }
                    })
                    .collect(Collectors.toList());
        }

        for (Path servlet : jsonServlets) {
            assertTrue(servlet.getFileName() + " 输出 JSON 时必须声明 UTF-8 JSON 响应类型",
                    read(servlet).contains("application/json;charset=UTF-8"));
        }
    }

    private String read(Path path) throws Exception {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }
}
