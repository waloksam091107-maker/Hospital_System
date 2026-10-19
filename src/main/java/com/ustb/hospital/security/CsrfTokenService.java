package com.ustb.hospital.security;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/** Session-bound CSRF token support for same-origin state-changing requests. */
public final class CsrfTokenService {
    public static final String SESSION_ATTRIBUTE = "csrfToken";
    public static final String HEADER_NAME = "X-CSRF-Token";
    private static final SecureRandom RANDOM = new SecureRandom();

    private CsrfTokenService() { }

    public static String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String issue(HttpSession session) {
        String token = newToken();
        session.setAttribute(SESSION_ATTRIBUTE, token);
        return token;
    }

    public static String current(HttpSession session) {
        Object token = session == null ? null : session.getAttribute(SESSION_ATTRIBUTE);
        return token instanceof String ? (String) token : null;
    }

    public static boolean verify(HttpServletRequest request) {
        return matches(current(request.getSession(false)), request.getHeader(HEADER_NAME));
    }

    public static boolean matches(String expected, String supplied) {
        if (expected == null || supplied == null) return false;
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), supplied.getBytes(StandardCharsets.UTF_8));
    }
}
