package com.ustb.hospital.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ustb.hospital.utils.AppointmentOperationResult;
import com.ustb.hospital.utils.SessionIdentity;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/** Shared parsing, authorisation and response code for appointment endpoints. */
final class AppointmentServletSupport {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private AppointmentServletSupport() { }

    static Integer requirePatient(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Integer patientId = SessionIdentity.getCurrentPatientId(request);
        if (patientId == null) writeAuthError(request, response);
        return patientId;
    }
    static Integer requireDoctor(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Integer doctorId = SessionIdentity.getCurrentDoctorId(request);
        if (doctorId == null) writeAuthError(request, response);
        return doctorId;
    }
    static boolean requireAdmin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (SessionIdentity.isAdmin(request)) return true;
        writeAuthError(request, response);
        return false;
    }
    static Integer parsePositiveInt(String value) {
        try {
            int parsed = Integer.parseInt(value);
            return parsed > 0 ? Integer.valueOf(parsed) : null;
        } catch (Exception exception) { return null; }
    }
    static int parsePage(String value) {
        Integer parsed = parsePositiveInt(value);
        return parsed == null ? 1 : parsed.intValue();
    }
    static void writeResult(HttpServletResponse response, AppointmentOperationResult result) throws IOException {
        response.setStatus(result.isSuccessful() ? HttpServletResponse.SC_OK : statusFor(result.getCode()));
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("success", Boolean.valueOf(result.isSuccessful()));
        body.put("code", result.getCode());
        body.put("message", result.getMessage());
        writeJson(response, body);
    }
    private static int statusFor(String code) {
        if ("BOOKING_FAILED".equals(code) || "CANCEL_FAILED".equals(code) || "RESCHEDULE_FAILED".equals(code)
                || "START_FAILED".equals(code) || "SCHEDULE_CREATE_FAILED".equals(code)
                || "SCHEDULE_UPDATE_FAILED".equals(code) || "SCHEDULE_DELETE_FAILED".equals(code)) return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        if ("FORBIDDEN".equals(code)) return HttpServletResponse.SC_FORBIDDEN;
        if ("NOT_FOUND".equals(code)) return HttpServletResponse.SC_NOT_FOUND;
        if ("SLOT_UNAVAILABLE".equals(code) || "INVALID_STATE".equals(code) || "NO_CHANGE".equals(code)
                || "SCHEDULE_IN_USE".equals(code) || "HISTORY_DATE_LOCKED".equals(code)
                || "ALREADY_CLOSED".equals(code) || "DUPLICATE_DATE".equals(code) || "SCHEDULE_MISMATCH".equals(code)) {
            return HttpServletResponse.SC_CONFLICT;
        }
        return HttpServletResponse.SC_BAD_REQUEST;
    }
    static void writeJson(HttpServletResponse response, Object body) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        OBJECT_MAPPER.writeValue(response.getWriter(), body);
    }
    static void writeBadRequest(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("success", Boolean.FALSE);
        body.put("code", "INVALID_REQUEST");
        body.put("message", message);
        writeJson(response, body);
    }
    static void writeMethodNotAllowed(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        response.setHeader("Allow", "POST");
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("success", Boolean.FALSE);
        body.put("code", "METHOD_NOT_ALLOWED");
        body.put("message", "该操作仅支持 POST 请求。");
        writeJson(response, body);
    }
    private static void writeAuthError(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        boolean loggedIn = session != null && session.getAttribute(SessionIdentity.CURRENT_USER_ROLE) != null;
        response.setStatus(loggedIn ? HttpServletResponse.SC_FORBIDDEN : HttpServletResponse.SC_UNAUTHORIZED);
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("success", Boolean.FALSE);
        body.put("code", loggedIn ? "FORBIDDEN" : "LOGIN_REQUIRED");
        body.put("message", loggedIn ? "当前账号无权执行此操作。" : "请先登录。");
        writeJson(response, body);
    }
}
