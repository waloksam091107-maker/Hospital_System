package com.ustb.hospital.web;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Keeps patient-facing appointment pages aligned with the three schedule slots accepted by the service. */
public class AppointmentSlotPageContractTest {
    @Test
    public void bookingPageSubmitsOnlyCanonicalScheduleSlots() throws Exception {
        String page = read("doctors-details.html");

        assertEquals("预约页的可选时段必须与后端排班粒度完全一致", canonicalSlots(),
                extractValues(page, "<input[^>]*name=\"appointmentTime\"[^>]*value=\"([^\"]+)\""));
        assertTrue("预约页应先检查可预约性", page.contains("/appointments/availability"));
        assertTrue("预约确认页应以 POST 创建预约", read("book-appointment.html").contains("type: 'POST'"));
    }

    @Test
    public void reschedulePageSubmitsOnlyCanonicalScheduleSlots() throws Exception {
        String page = read("patient.jsp");

        assertEquals("改期页的可选时段必须与后端排班粒度完全一致", canonicalSlots(),
                extractValues(page, "<option value=\"([^\"]+)\">(?:上午|下午|晚间)</option>"));
        assertTrue("改期页应以 POST 提交改期请求", page.contains("url: appUrl('/appointments/reschedule')") && page.contains("type: 'POST'"));
    }

    private String read(String fileName) throws Exception {
        return new String(Files.readAllBytes(Paths.get("src", "main", "webapp", "hospro", fileName)), StandardCharsets.UTF_8);
    }

    private Set<String> canonicalSlots() {
        return new HashSet<String>(Arrays.asList("AM", "PM", "EVENING"));
    }

    private Set<String> extractValues(String source, String expression) {
        Set<String> values = new HashSet<String>();
        Matcher matcher = Pattern.compile(expression).matcher(source);
        while (matcher.find()) values.add(matcher.group(1));
        return values;
    }
}
