package com.ustb.hospital.web;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Keeps the patient booking page and availability endpoint on the same JSON contract. */
public class AppointmentAvailabilityContractTest {
    @Test
    public void availabilityEndpointReturnsJsonConsumedByDoctorDetailsPage() throws Exception {
        String servlet = new String(Files.readAllBytes(Paths.get("src", "main", "java", "com", "ustb", "hospital", "servlet", "AppointmentAvailabilityServlet.java")), StandardCharsets.UTF_8);
        String page = new String(Files.readAllBytes(Paths.get("src", "main", "webapp", "hospro", "doctors-details.html")), StandardCharsets.UTF_8);

        assertTrue("可预约性接口应返回 JSON", servlet.contains("AppointmentServletSupport.writeJson(response, result)"));
        assertFalse("可预约性接口不应返回与前端不兼容的 1/0 文本", servlet.contains("text/plain;charset=UTF-8"));
        assertTrue("医生详情页应按 JSON 的 available 字段判断", page.contains("if (response.available)"));
    }
}
