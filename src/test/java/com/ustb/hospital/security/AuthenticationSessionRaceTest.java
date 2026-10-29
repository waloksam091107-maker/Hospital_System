package com.ustb.hospital.security;

import com.ustb.hospital.filter.AuthenticationFilter;
import com.ustb.hospital.service.AccountSecurityService;
import com.ustb.hospital.service.AuditContext;
import com.ustb.hospital.service.AuditService;
import com.ustb.hospital.utils.SessionIdentity;
import org.junit.Test;

import javax.servlet.http.*;
import java.io.*;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.Assert.*;

public class AuthenticationSessionRaceTest {
    @Test public void concurrentGuestRequestsBothReturnUnauthorizedWhenSessionIsInvalidated() throws Exception {
        AtomicBoolean invalid = new AtomicBoolean();
        CountDownLatch invalidators = new CountDownLatch(2);
        HttpSession session = session(Collections.emptyMap(), invalid, (key, count) -> { }, () -> {
            invalidators.countDown();
            if (!invalidators.await(5, TimeUnit.SECONDS)) throw new AssertionError("requests did not overlap");
        });
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<Integer>> results = new ArrayList<>();
            for (int i = 0; i < 2; i++) results.add(pool.submit(() -> denied(session, rejectingAccounts())));
            for (Future<Integer> result : results) assertEquals(401, result.get(10, TimeUnit.SECONDS).intValue());
        } finally { pool.shutdownNow(); }
    }

    @Test public void invalidationDuringAnyIdentityVersionOrAuditReadReturnsUnauthorized() throws Exception {
        Object[][] interruptions = {
            {SessionIdentity.CURRENT_USER_ROLE, 1}, {SessionIdentity.CURRENT_USER_ROLE, 2},
            {SessionIdentity.CURRENT_PATIENT_ID, 1},
            {SessionIdentity.ACCOUNT_SESSION_VERSION, 1}, {SessionIdentity.CURRENT_USER_ROLE, 3},
            {SessionIdentity.CURRENT_USER_ROLE, 4}, {SessionIdentity.CURRENT_USER_ROLE, 5},
            {SessionIdentity.CURRENT_USER_ROLE, 6}, {SessionIdentity.CURRENT_PATIENT_ID, 2}
        };
        for (Object[] interruption : interruptions) {
            AtomicBoolean invalid = new AtomicBoolean();
            HttpSession session = session(authenticatedAttributes(), invalid, (key, count) -> {
                if (interruption[0].equals(key) && interruption[1].equals(count)) invalid.set(true);
            }, () -> { });
            assertEquals(Arrays.toString(interruption), 401, denied(session, rejectingAccounts()));
        }
    }

    @Test public void accountValidationFailureStillReturnsServiceUnavailable() throws Exception {
        HttpSession session = session(authenticatedAttributes(), new AtomicBoolean(), (key, count) -> { }, () -> { });
        AccountSecurityService unavailable = new AccountSecurityService() {
            @Override public AccountState validateSession(String role, int id, long version) { throw new IllegalStateException("database unavailable"); }
        };
        assertEquals(503, denied(session, unavailable));
    }

    private static Map<String,Object> authenticatedAttributes() {
        Map<String,Object> values = new HashMap<>();
        values.put(SessionIdentity.CURRENT_USER_ROLE, "PATIENT");
        values.put(SessionIdentity.CURRENT_PATIENT_ID, 7);
        values.put(SessionIdentity.ACCOUNT_SESSION_VERSION, 1L);
        return values;
    }
    private static AccountSecurityService rejectingAccounts() {
        return new AccountSecurityService() {
            @Override public AccountState validateSession(String role, int id, long version) {
                assertEquals("PATIENT", role);
                assertEquals(7, id);
                assertEquals(1L, version);
                return null;
            }
        };
    }
    private static int denied(HttpSession session, AccountSecurityService accounts) throws Exception {
        Map<String,Object> requestAttributes = new HashMap<>();
        HttpServletRequest request = (HttpServletRequest) Proxy.newProxyInstance(AuthenticationSessionRaceTest.class.getClassLoader(), new Class[]{HttpServletRequest.class}, (proxy, method, args) -> {
            switch (method.getName()) {
                case "getSession": return session;
                case "getServletPath": return "/notifications";
                case "getPathInfo": return "/unread-count";
                case "getMethod": return "GET";
                case "getAttribute": return requestAttributes.get(args[0]);
                case "setAttribute": requestAttributes.put((String) args[0], args[1]); return null;
                default: return defaultValue(method.getReturnType());
            }
        });
        int[] status = {200};
        StringWriter body = new StringWriter();
        HttpServletResponse response = (HttpServletResponse) Proxy.newProxyInstance(AuthenticationSessionRaceTest.class.getClassLoader(), new Class[]{HttpServletResponse.class}, (proxy, method, args) -> {
            if ("setStatus".equals(method.getName())) { status[0] = (Integer) args[0]; return null; }
            if ("getWriter".equals(method.getName())) return new PrintWriter(body);
            return defaultValue(method.getReturnType());
        });
        AuditService audit = new AuditService() {
            @Override public void recordFailure(AuditContext context, String action, String type, String id) { }
        };
        new AuthenticationFilter(audit, accounts).doFilter(request, response, (q, s) -> fail("invalid session reached servlet"));
        assertTrue(body.toString(), body.toString().contains(status[0] == 503 ? "SECURITY_UNAVAILABLE" : "LOGIN_REQUIRED"));
        return status[0];
    }
    private interface ReadBoundary { void before(String key, Integer count); }
    private interface InvalidationBoundary { void before() throws Exception; }
    private static HttpSession session(Map<String,Object> attrs, AtomicBoolean invalid, ReadBoundary reads, InvalidationBoundary invalidates) {
        Map<String,Integer> counts = new HashMap<>();
        return (HttpSession) Proxy.newProxyInstance(AuthenticationSessionRaceTest.class.getClassLoader(), new Class[]{HttpSession.class}, (proxy, method, args) -> {
            if ("getAttribute".equals(method.getName())) {
                synchronized (counts) {
                    String key = (String) args[0]; int count = counts.getOrDefault(key, 0) + 1; counts.put(key, count);
                    reads.before(key, count);
                    if (invalid.get()) throw new IllegalStateException("session already invalidated");
                    return attrs.get(key);
                }
            }
            if ("invalidate".equals(method.getName())) {
                invalidates.before();
                if (!invalid.compareAndSet(false, true)) throw new IllegalStateException("session already invalidated");
            }
            return defaultValue(method.getReturnType());
        });
    }
    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == long.class) return 0L;
        return 0;
    }
}
