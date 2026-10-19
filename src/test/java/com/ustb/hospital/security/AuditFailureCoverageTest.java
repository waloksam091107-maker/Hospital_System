package com.ustb.hospital.security;

import com.ustb.hospital.filter.*;
import com.ustb.hospital.service.*;
import org.junit.Test;
import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import static org.junit.Assert.*;

public class AuditFailureCoverageTest {
    @Test public void authAndCsrfDenialAuditOnce() throws Exception {
        for (boolean authenticated : new boolean[]{false,true}) {
            FakeAudit audit = new FakeAudit(); Map<String,Object> attrs = new HashMap<String,Object>();
            HttpServletRequest req = request(authenticated, attrs); HttpServletResponse resp = response();
            AuditFilter outer = AuditFilter.class.getConstructor(AuditService.class).newInstance(audit);
            AuthenticationFilter auth = AuthenticationFilter.class.getConstructor(AuditService.class).newInstance(audit);
            CsrfFilter csrf = CsrfFilter.class.getConstructor(AuditService.class).newInstance(audit);
            outer.doFilter(req,resp,(r,s)->auth.doFilter(r,s,(r2,s2)->csrf.doFilter(r2,s2,(r3,s3)->fail("rejected request reached servlet"))));
            assertEquals(1, audit.failures);
        }
    }
    @Test public void uncaughtExceptionIsFailureNotSuccess() throws Exception {
        FakeAudit audit = new FakeAudit(); AuditFilter outer = AuditFilter.class.getConstructor(AuditService.class).newInstance(audit);
        try { outer.doFilter(request(true,new HashMap<String,Object>()),response(),(r,s)->{ throw new ServletException("secret must not enter audit"); }); fail("exception expected"); }
        catch (ServletException expected) { assertEquals(1,audit.failures); }
    }
    @Test public void csrfBeforeAuthenticationStillRejectsAnonymousOnce() throws Exception {
        FakeAudit audit = new FakeAudit(); HttpServletRequest req = request(false,new HashMap<String,Object>());
        HttpServletResponse resp = response(); CsrfFilter csrf = new CsrfFilter(audit); AuthenticationFilter auth = new AuthenticationFilter(audit);
        csrf.doFilter(req,resp,(r,s)->auth.doFilter(r,s,(r2,s2)->fail("anonymous request reached servlet")));
        assertEquals(1,audit.failures);
    }
    @Test public void rejectedActionMatchesSuccessAndRetainsFullServletPath() throws Exception {
        FakeAudit audit = new FakeAudit(); Map<String,Object> attrs = new HashMap<>();
        HttpServletRequest req = (HttpServletRequest) Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{HttpServletRequest.class},(p,m,a)->{
            if ("getMethod".equals(m.getName())) return "POST"; if ("getServletPath".equals(m.getName())) return "/medical-records";
            if ("getPathInfo".equals(m.getName())) return "/draft"; if ("getAttribute".equals(m.getName())) return attrs.get(a[0]);
            if ("setAttribute".equals(m.getName())) { attrs.put((String)a[0],a[1]); return null; } return value(m.getReturnType());
        });
        new AuditFilter(audit).doFilter(req,response(),(r,s)->((HttpServletResponse)s).setStatus(403));
        assertEquals("MEDICAL_RECORD_DRAFT", audit.action);
        assertEquals("/medical-records/draft", audit.context.getRequestPath());
    }
    private static class FakeAudit extends AuditService { int failures; String action; AuditContext context; public void recordFailure(AuditContext c, String a, String t, String i) { failures++; action=a; context=c; assertFalse(a.contains("secret")); } }
    private HttpServletRequest request(boolean authenticated, Map<String,Object> attrs) {
        HttpSession session = authenticated ? (HttpSession) Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{HttpSession.class},(p,m,a)-> { if ("getAttribute".equals(m.getName()) && "currentUserRole".equals(a[0])) return "PATIENT"; return value(m.getReturnType()); }) : null;
        return (HttpServletRequest) Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{HttpServletRequest.class},(p,m,a)->{
            if ("getSession".equals(m.getName())) return session; if ("getMethod".equals(m.getName())) return "POST"; if ("getServletPath".equals(m.getName())) return "/appointments/book";
            if ("getAttribute".equals(m.getName())) return attrs.get(a[0]); if ("setAttribute".equals(m.getName())) { attrs.put((String)a[0],a[1]); return null; }
            return value(m.getReturnType());
        });
    }
    private HttpServletResponse response() { return (HttpServletResponse) Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{HttpServletResponse.class},(p,m,a)-> { if ("getWriter".equals(m.getName())) return new PrintWriter(new StringWriter()); return value(m.getReturnType()); }); }
    private static Object value(Class<?> t) { if (!t.isPrimitive()) return null; if (t==boolean.class) return false; if (t==long.class) return 0L; return 0; }
}
