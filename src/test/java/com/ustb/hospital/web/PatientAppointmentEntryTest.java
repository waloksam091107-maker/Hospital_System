package com.ustb.hospital.web;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Protects the real patient appointment entry point from template-only dashboard links. */
public class PatientAppointmentEntryTest {
    @Test
    public void newAppointmentButtonOpensTheDepartmentSelectionPage() throws Exception {
        String page = new String(Files.readAllBytes(Paths.get("src", "main", "webapp", "hospro", "patient.jsp")), StandardCharsets.UTF_8);
        assertTrue(page.contains("新增预约"));
        assertFalse("新增预约不能指向不存在的 patient-dashboard.html", page.contains("href=\"patient-dashboard.html\" class=\"theme-btn-one\""));
        assertTrue("新增预约应直接进入科室选择", page.contains("href=\"appointment-departments.html\" class=\"theme-btn-one\""));
    }

    @Test
    public void bookingPageRejectsIncompleteOrUnsupportedAppointmentParametersBeforePosting() throws Exception {
        String page = new String(Files.readAllBytes(Paths.get(
                "src", "main", "webapp", "hospro", "book-appointment.html")), StandardCharsets.UTF_8);

        assertTrue("预约提交前必须验证 URL 中的预约参数", page.contains("function buildBookingRequest()"));
        assertTrue("预约页只能接受系统定义的三个时段", page.contains("['AM', 'PM', 'EVENING']"));
        assertTrue("参数无效时必须给患者明确提示", page.contains("预约信息不完整或已失效，请返回医生页面重新选择。"));
    }
}
