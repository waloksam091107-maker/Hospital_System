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
        response.setStatus(result.isSuccessful() ? HttpServletResponse.SC_OK : HttpServletResponse.SC_BAD_REQUEST);
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("success", Boolean.valueOf(result.isSuccessful()));
        body.put("code", result.getCode());
        body.put("message", result.getMessage());
        writeJson(response, body);
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
