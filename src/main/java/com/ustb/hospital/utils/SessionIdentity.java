package com.ustb.hospital.utils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/** Centralises the session keys used for appointment authorisation. */
public final class SessionIdentity {
    public static final String ROLE_DOCTOR = "DOCTOR";
    public static final String ROLE_PATIENT = "PATIENT";
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String CURRENT_USER_ROLE = "currentUserRole";
    public static final String CURRENT_PATIENT_ID = "currentPatientId";
    public static final String CURRENT_DOCTOR_ID = "currentDoctorId";
    public static final String CURRENT_ADMIN_ID = "currentAdminId";
    public static final String CURRENT_USER_NAME = "currentUserName";
    public static final String ACCOUNT_SESSION_VERSION = "accountSessionVersion";
    public static final String MUST_CHANGE_PASSWORD = "mustChangePassword";

    private SessionIdentity() { }

    public static void set(HttpSession session, String role, int userId, String displayName) {
        session.removeAttribute(ACCOUNT_SESSION_VERSION);
        session.removeAttribute(MUST_CHANGE_PASSWORD);
        session.setAttribute(CURRENT_USER_ROLE, role);
        session.setAttribute(CURRENT_USER_NAME, displayName);
        if (ROLE_PATIENT.equals(role)) {
            session.setAttribute(CURRENT_PATIENT_ID, Integer.valueOf(userId));
            session.removeAttribute(CURRENT_DOCTOR_ID);
            session.removeAttribute(CURRENT_ADMIN_ID);
        } else if (ROLE_DOCTOR.equals(role)) {
            session.setAttribute(CURRENT_DOCTOR_ID, Integer.valueOf(userId));
            session.removeAttribute(CURRENT_PATIENT_ID);
            session.removeAttribute(CURRENT_ADMIN_ID);
        } else if (ROLE_ADMIN.equals(role)) {
            session.setAttribute(CURRENT_ADMIN_ID, Integer.valueOf(userId));
            session.removeAttribute(CURRENT_PATIENT_ID);
            session.removeAttribute(CURRENT_DOCTOR_ID);
        }
    }

    public static Integer getCurrentPatientId(HttpServletRequest request) {
        return getId(request, ROLE_PATIENT, CURRENT_PATIENT_ID);
    }

    /** Only a successfully verified login may establish an account-versioned session. */
    public static void establish(HttpSession session, String role, int id, String name, long version, boolean mustChangePassword) {
        set(session, role, id, name);
        session.setAttribute(ACCOUNT_SESSION_VERSION, Long.valueOf(version));
        session.setAttribute(MUST_CHANGE_PASSWORD, Boolean.valueOf(mustChangePassword));
    }

    public static Integer getCurrentDoctorId(HttpServletRequest request) {
        return getId(request, ROLE_DOCTOR, CURRENT_DOCTOR_ID);
    }

    public static Integer getCurrentAdminId(HttpServletRequest request) {
        return getId(request, ROLE_ADMIN, CURRENT_ADMIN_ID);
    }

    public static String getCurrentRole(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Object role = getSessionAttribute(session, CURRENT_USER_ROLE);
        return role instanceof String ? (String) role : null;
    }

    /** A concurrent logout or rejected request may invalidate the same session during a read. */
    public static Object getSessionAttribute(HttpSession session, String name) {
        if (session == null) return null;
        try { return session.getAttribute(name); }
        catch (IllegalStateException invalidated) { return null; }
    }

    /** Returns the authenticated actor id only when the role and id key agree. */
    public static Integer getCurrentUserId(HttpServletRequest request) {
        String role = getCurrentRole(request);
        if (ROLE_PATIENT.equals(role)) return getCurrentPatientId(request);
        if (ROLE_DOCTOR.equals(role)) return getCurrentDoctorId(request);
        if (ROLE_ADMIN.equals(role)) return getCurrentAdminId(request);
        return null;
    }

    public static boolean isAdmin(HttpServletRequest request) {
        return ROLE_ADMIN.equals(getCurrentRole(request));
    }

    private static Integer getId(HttpServletRequest request, String expectedRole, String key) {
        HttpSession session = request.getSession(false);
        if (!expectedRole.equals(getSessionAttribute(session, CURRENT_USER_ROLE))) {
            return null;
        }
        Object value = getSessionAttribute(session, key);
        return value instanceof Integer ? (Integer) value : null;
    }
}
