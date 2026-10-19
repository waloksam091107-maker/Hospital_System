package com.ustb.hospital.service;

import com.ustb.hospital.utils.SessionIdentity;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/** Metadata captured from the authenticated server-side session, never the request body. */
public final class AuditContext {
    private final String actorRole;
    private final Integer actorId;
    private final String requestMethod;
    private final String requestPath;
    private final String remoteAddress;

    public AuditContext(String actorRole, Integer actorId, String requestMethod, String requestPath, String remoteAddress) {
        this.actorRole = validRole(actorRole) ? actorRole : null;
        this.actorId = this.actorRole != null && actorId != null && actorId.intValue() > 0 ? actorId : null;
        this.requestMethod = matching(requestMethod, "GET|POST|PUT|PATCH|DELETE", 10);
        this.requestPath = matching(requestPath, "/[A-Za-z0-9_./-]*", 255);
        this.remoteAddress = matching(remoteAddress, "[0-9a-fA-F:.%]+", 64);
    }

    public static AuditContext fromRequest(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        String role = session == null ? null : (String) session.getAttribute(SessionIdentity.CURRENT_USER_ROLE);
        Object actor = null;
        if (session != null && SessionIdentity.ROLE_DOCTOR.equals(role)) actor = session.getAttribute(SessionIdentity.CURRENT_DOCTOR_ID);
        else if (session != null && SessionIdentity.ROLE_PATIENT.equals(role)) actor = session.getAttribute(SessionIdentity.CURRENT_PATIENT_ID);
        else if (session != null && SessionIdentity.ROLE_ADMIN.equals(role)) actor = session.getAttribute(SessionIdentity.CURRENT_ADMIN_ID);
        String path = request.getServletPath();
        if (path != null && request.getPathInfo() != null) path += request.getPathInfo();
        return new AuditContext(role, actor instanceof Integer ? (Integer) actor : null,
                request.getMethod(), path, request.getRemoteAddr());
    }

    public String getActorRole() { return actorRole; }
    public Integer getActorId() { return actorId; }
    public String getRequestMethod() { return requestMethod; }
    public String getRequestPath() { return requestPath; }
    public String getRemoteAddress() { return remoteAddress; }

    private static boolean validRole(String role) {
        return SessionIdentity.ROLE_DOCTOR.equals(role) || SessionIdentity.ROLE_PATIENT.equals(role) || SessionIdentity.ROLE_ADMIN.equals(role);
    }

    private static String matching(String value, String pattern, int maxLength) {
        return value != null && value.length() <= maxLength && value.matches(pattern) ? value : null;
    }
}
