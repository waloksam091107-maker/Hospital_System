package com.ustb.hospital.web;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/** Ensures a patient booking request can pass the application's CSRF boundary. */
public class BookingPageSecurityContractTest {
    @Test
    public void bookingPageGetsCsrfTokenSendsItAndReadsDoctorNameFromServer() throws Exception {
        String page = new String(Files.readAllBytes(Paths.get(
                "src", "main", "webapp", "hospro", "book-appointment.html")), StandardCharsets.UTF_8);

        assertTrue("预约页必须先获取 CSRF 令牌", page.contains("appUrl('/security/csrf')"));
        assertTrue("预约请求必须携带 CSRF 请求头", page.contains("'X-CSRF-Token': csrfToken"));
        assertTrue("预约页必须从服务端获取医生姓名，不能信任 URL 中的姓名", page.contains("appUrl('/doctors/details')"));
        assertTrue("预约页应显示服务端返回的医生姓名", page.contains("$('#docname').text(doctor.name)"));
    }
}
