package com.ustb.hospital.web;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/** Verifies that the doctor-side appointment completion flow has a reachable page. */
public class DoctorAppointmentPageTest {
    @Test
    public void doctorAppointmentPageIsReachableFromScheduleAndUsesSecuredEndpoints() throws Exception {
        Path webRoot = Paths.get("src", "main", "webapp", "doctor-schedule");
        Path appointmentPage = webRoot.resolve("appointments.html");
        assertTrue("医生预约列表页面应存在", Files.isRegularFile(appointmentPage));

        String schedulePage = new String(Files.readAllBytes(webRoot.resolve("date.html")), StandardCharsets.UTF_8);
        String appointmentPageHtml = new String(Files.readAllBytes(appointmentPage), StandardCharsets.UTF_8);
        assertTrue("排班页应提供预约记录入口", schedulePage.contains("appointments.html"));
        assertTrue("排班页不应保留不存在的 appointment.html 入口", !schedulePage.contains("href=\"appointment.html\""));
        assertTrue("医生预约页应查询当前医生的预约", appointmentPageHtml.contains("/appointments/doctor"));
        assertTrue("医生预约页应提供填写就诊记录入口", appointmentPageHtml.contains("encounter.html?appointmentId="));
        assertTrue("完成接诊不得绕过就诊记录", !appointmentPageHtml.contains("/appointments/complete"));
    }

    @Test
    public void legacyScheduleSidebarRoutesAppointmentRecordsToTheCanonicalDoctorPage() throws Exception {
        Path legacySchedulePage = Paths.get("src", "main", "webapp", "hospro", "date.html");
        String schedulePage = new String(Files.readAllBytes(legacySchedulePage), StandardCharsets.UTF_8);

        assertTrue("旧排班入口的预约记录必须跳转至可用的医生预约页",
                schedulePage.contains("href=\"../doctor-schedule/appointments.html\"><i class=\"fas fa-wheelchair\"></i>预约记录"));
    }

    @Test
    public void doctorWorkflowPagesUseOneFriendlyApiErrorContract() throws Exception {
        Path webRoot = Paths.get("src", "main", "webapp", "doctor-schedule");
        for (String pageName : new String[]{"date.html", "appointments.html", "encounter.html"}) {
            String page = new String(Files.readAllBytes(webRoot.resolve(pageName)), StandardCharsets.UTF_8);
            assertTrue(pageName + " 应加载统一的医生端接口错误提示工具", page.contains("doctor-api.js"));
        }

        Path helper = webRoot.resolve("doctor-api.js");
        assertTrue("医生端接口错误提示工具应存在", Files.isRegularFile(helper));
        String source = new String(Files.readAllBytes(helper), StandardCharsets.UTF_8);
        assertTrue(source.contains("function doctorApiErrorMessage"));
        assertTrue(source.contains("xhr.status === 401"));
        assertTrue(source.contains("xhr.status === 403"));
        assertTrue(source.contains("xhr.status === 404"));
        assertTrue(source.contains("xhr.status === 409"));
        assertTrue(source.contains("请先登录医生账号。"));
        assertTrue(source.contains("当前账号无权执行此操作。"));
    }
}
