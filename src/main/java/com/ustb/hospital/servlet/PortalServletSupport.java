package com.ustb.hospital.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ustb.hospital.service.AuditContext;
import com.ustb.hospital.service.AuditService;
import com.ustb.hospital.service.AdminMasterDataService;
import com.ustb.hospital.utils.SessionIdentity;
import com.ustb.hospital.utils.AppointmentOperationResult;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

/** Shared session, validation and JSON helpers for new safe portal endpoints. */
public final class PortalServletSupport {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final AuditService AUDIT = new AuditService();

    private PortalServletSupport() { }

    static Integer requireRole(HttpServletRequest request, HttpServletResponse response, String requiredRole) throws IOException {
        String currentRole = SessionIdentity.getCurrentRole(request);
        Integer userId = SessionIdentity.getCurrentUserId(request);
        if (currentRole == null || userId == null) {
            writeJsonError(response, HttpServletResponse.SC_UNAUTHORIZED, "LOGIN_REQUIRED", "请先登录。");
            AUDIT.recordFailure(AuditContext.fromRequest(request), "PORTAL_ACCESS", null, null);
            return null;
        }
        if (!requiredRole.equals(currentRole)) {
            writeJsonError(response, HttpServletResponse.SC_FORBIDDEN, "FORBIDDEN", "当前账号无权执行此操作。");
            AUDIT.recordFailure(AuditContext.fromRequest(request), "PORTAL_ACCESS", null, null);
            return null;
        }
        return userId;
    }

    static Integer requireAuthenticated(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String currentRole = SessionIdentity.getCurrentRole(request);
        Integer userId = SessionIdentity.getCurrentUserId(request);
        if (currentRole == null || userId == null) {
            writeJsonError(response, HttpServletResponse.SC_UNAUTHORIZED, "LOGIN_REQUIRED", "请先登录。");
            AUDIT.recordFailure(AuditContext.fromRequest(request), "PORTAL_ACCESS", null, null);
            return null;
        }
        return userId;
    }

    static Integer parsePositiveInt(String value) {
        try {
            int parsed = Integer.parseInt(value);
            return parsed > 0 ? Integer.valueOf(parsed) : null;
        } catch (RuntimeException exception) {
            return null;
        }
    }

    static BigDecimal parsePositiveAmount(String value, BigDecimal maximum) {
        try {
            BigDecimal amount = new BigDecimal(value).setScale(2, RoundingMode.UNNECESSARY);
            if (amount.signum() <= 0 || (maximum != null && amount.compareTo(maximum) > 0)) return null;
            return amount;
        } catch (RuntimeException exception) {
            return null;
        }
    }

    static String requiredText(String value, int maximumLength) {
        if (value == null) return null;
        String result = value.trim();
        return result.isEmpty() || result.length() > maximumLength ? null : result;
    }

    static void writeJson(HttpServletResponse response, Map<String, Object> body) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        OBJECT_MAPPER.writeValue(response.getWriter(), body);
    }

    static void writeJsonError(HttpServletResponse response, int status, String code, String message) throws IOException {
        response.setStatus(status);
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("success", Boolean.FALSE);
        body.put("code", code);
        body.put("message", message);
        writeJson(response, body);
    }

    static void writeMethodNotAllowed(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        response.setHeader("Allow", "GET, POST");
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("success", Boolean.FALSE);
        body.put("code", "METHOD_NOT_ALLOWED");
        body.put("message", "该操作不支持当前请求方法。");
        writeJson(response, body);
    }

    static void writeMasterDataPage(HttpServletRequest request, HttpServletResponse response,
                                    AdminMasterDataService service, String resource) throws IOException {
        try {
            if ("true".equals(request.getParameter("options"))) {
                writeJson(response, service.options(resource));
                return;
            }
            String pageText=request.getParameter("pageNum"), sizeText=request.getParameter("pageSize");
            int page=pageText==null ? 1 : Integer.parseInt(pageText);
            int size=sizeText==null ? 10 : Integer.parseInt(sizeText);
            writeJson(response,service.search(resource,request.getParameter("keyword"),page,size));
        } catch (IllegalArgumentException invalid) {
            writeJsonError(response,HttpServletResponse.SC_BAD_REQUEST,"INVALID_REQUEST","查询参数无效：页码须大于 0，每页 1 到 100 条，关键字不超过 100 字。");
        } catch (RuntimeException failure) {
            java.util.logging.Logger.getLogger(PortalServletSupport.class.getName()).log(java.util.logging.Level.SEVERE,"Master data query failed",failure);
            writeJsonError(response,HttpServletResponse.SC_INTERNAL_SERVER_ERROR,"QUERY_FAILED","数据读取失败，请稍后重试。");
        }
    }

    static void writeOperationResult(HttpServletResponse response, AppointmentOperationResult result) throws IOException {
        response.setStatus(result.isSuccessful() ? HttpServletResponse.SC_OK : statusFor(result.getCode()));
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("success", Boolean.valueOf(result.isSuccessful()));
        body.put("code", result.getCode());
        body.put("message", result.getMessage());
        writeJson(response, body);
    }

    private static int statusFor(String code) {
        if ("FORBIDDEN".equals(code)) return HttpServletResponse.SC_FORBIDDEN;
        if ("NOT_FOUND".equals(code)) return HttpServletResponse.SC_NOT_FOUND;
        if ("DUPLICATE_ACCOUNT".equals(code) || "HAS_REFERENCES".equals(code)) return HttpServletResponse.SC_CONFLICT;
        if (code != null && code.endsWith("_FAILED")) return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        return HttpServletResponse.SC_BAD_REQUEST;
    }
}
