package com.ustb.hospital.servlet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ustb.hospital.utils.AppointmentOperationResult;
import org.junit.Test;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import static org.junit.Assert.*;

/** Executes the actual response writer to verify public status and JSON behavior. */
public class ScheduleMaintenanceHttpResultTest {
    @Test public void businessConflictsReturn409AndPreserveErrorDetails() throws Exception {
        for (String code : new String[]{"SCHEDULE_IN_USE", "HISTORY_DATE_LOCKED", "ALREADY_CLOSED", "DUPLICATE_DATE", "SCHEDULE_MISMATCH"}) assertResponse(code, 409);
    }
    @Test public void malformedAndPastDateReturn400AndOwnershipReturns403() throws Exception {
        assertResponse("INVALID_DATE", 400); assertResponse("PAST_DATE", 400); assertResponse("FORBIDDEN", 403);
        assertResponse("NOT_FOUND", 404); assertResponse("SCHEDULE_UPDATE_FAILED", 500);
    }
    private void assertResponse(String code, int expected) throws Exception {
        final int[] status = {0}; StringWriter body = new StringWriter();
        HttpServletResponse response = (HttpServletResponse) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{HttpServletResponse.class}, (proxy, method, args) -> {
            if ("setStatus".equals(method.getName())) { status[0] = (Integer) args[0]; return null; }
            if ("getWriter".equals(method.getName())) return new PrintWriter(body);
            return null;
        });
        AppointmentServletSupport.writeResult(response, AppointmentOperationResult.failure(code, "排班操作未完成"));
        assertEquals(code, expected, status[0]);
        JsonNode json = new ObjectMapper().readTree(body.toString());
        assertFalse(json.path("success").asBoolean()); assertEquals(code, json.path("code").asText());
        assertEquals("排班操作未完成", json.path("message").asText());
    }
}
