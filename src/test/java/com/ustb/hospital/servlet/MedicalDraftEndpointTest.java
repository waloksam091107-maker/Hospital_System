package com.ustb.hospital.servlet;

import com.ustb.hospital.entity.MedicalOrder;
import com.ustb.hospital.service.*;
import com.ustb.hospital.utils.SessionIdentity;
import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import javax.servlet.http.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import static org.junit.Assert.*;

public class MedicalDraftEndpointTest {
    @Test public void draftEndpointRequiresDoctorAndCsrf() throws Exception {
        String descriptor = new String(Files.readAllBytes(Paths.get("src/main/webapp/WEB-INF/web.xml")), StandardCharsets.UTF_8);
        assertTrue(descriptor.contains("<filter-name>csrf</filter-name><url-pattern>/medical-records/draft</url-pattern>"));
        assertEquals(401, request(null, null).status);
        assertEquals(403, request("PATIENT", null).status);
    }
    @Test public void draftReturnsVersionAndConflict409() throws Exception {
        Response success = request("DOCTOR", null);
        assertEquals(200, success.status); assertTrue(success.body.toString().contains("\"version\":0"));
        assertTrue(success.body.toString().contains("\"recordStatus\":\"DRAFT\""));
        Response conflict = request("DOCTOR", "0");
        assertEquals(409, conflict.status); assertTrue(conflict.body.toString().contains("VERSION_CONFLICT"));
    }
    private Response request(String role, String version) throws Exception {
        HttpSession session = role == null ? null : (HttpSession) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{HttpSession.class}, (p,m,a) -> {
            if ("getAttribute".equals(m.getName())) { if (SessionIdentity.CURRENT_USER_ROLE.equals(a[0])) return role; if (SessionIdentity.CURRENT_DOCTOR_ID.equals(a[0])) return 1; } return value(m.getReturnType());
        });
        HttpServletRequest req = (HttpServletRequest) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{HttpServletRequest.class}, (p,m,a) -> {
            if ("getSession".equals(m.getName())) return session;
            if ("getPathInfo".equals(m.getName())) return "/draft";
            if ("getMethod".equals(m.getName())) return "POST";
            if ("getServletPath".equals(m.getName())) return "/medical-records";
            if ("getParameter".equals(m.getName())) { if ("appointmentId".equals(a[0])) return "1"; if ("version".equals(a[0])) return version; }
            return value(m.getReturnType());
        });
        Response result = new Response();
        HttpServletResponse resp = (HttpServletResponse) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{HttpServletResponse.class}, (p,m,a) -> { if ("setStatus".equals(m.getName())) result.status = (Integer)a[0]; if ("getWriter".equals(m.getName())) return new PrintWriter(result.body,true); return value(m.getReturnType()); });
        MedicalRecordsServiceImpl fake = new MedicalRecordsServiceImpl() {
            public MedicalRecordOperationResult saveDraft(int doctor, int appointment, String diagnosis, String plan, String advice, List<MedicalOrder> orders, Integer v, AuditContext audit) {
                return v == null ? MedicalRecordOperationResult.success("saved", 8L, "DRAFT", 0) : MedicalRecordOperationResult.failure("VERSION_CONFLICT", "changed");
            }
        };
        Constructor<MedicalRecordsServlet> constructor = MedicalRecordsServlet.class.getDeclaredConstructor(MedicalRecordsServiceImpl.class);
        constructor.setAccessible(true); MedicalRecordsServlet servlet = constructor.newInstance(fake);
        servlet.doPost(req,resp); return result;
    }
    private static Object value(Class<?> t) { if (!t.isPrimitive()) return null; if (t == boolean.class) return false; if (t == long.class) return 0L; return 0; }
    private static class Response { int status = 200; StringWriter body = new StringWriter(); }
}
