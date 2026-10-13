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

    private SessionIdentity() { }

    public static void set(HttpSession session, String role, int userId, String displayName) {
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

    public static Integer getCurrentDoctorId(HttpServletRequest request) {
        return getId(request, ROLE_DOCTOR, CURRENT_DOCTOR_ID);
    }

    public static boolean isAdmin(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session != null && ROLE_ADMIN.equals(session.getAttribute(CURRENT_USER_ROLE));
    }

    private static Integer getId(HttpServletRequest request, String expectedRole, String key) {
        HttpSession session = request.getSession(false);
        if (session == null || !expectedRole.equals(session.getAttribute(CURRENT_USER_ROLE))) {
            return null;
        }
        Object value = session.getAttribute(key);
        return value instanceof Integer ? (Integer) value : null;
    }
}
