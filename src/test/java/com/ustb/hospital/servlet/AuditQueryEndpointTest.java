package com.ustb.hospital.servlet;

import com.ustb.hospital.service.AuditService;
import com.ustb.hospital.utils.SessionIdentity;
import org.junit.Test;
import javax.servlet.http.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import static org.junit.Assert.*;

public class AuditQueryEndpointTest {
    @Test public void anonymous401AndWrongRole403() throws Exception { assertEquals(401, request(null,"/search",null,false).status); assertEquals(403,request("PATIENT","/search",null,false).status); }
    @Test public void invalidRangeOrPageReturns400() throws Exception { assertEquals(400,request("ADMIN","/search","0",false).status); }
    @Test public void queryFailureIsNotEmptySuccess() throws Exception { assertEquals(500,request("ADMIN","/search",null,true).status); }
    @Test public void legacyLimitStillReturnsList() throws Exception { Response r=request("ADMIN",null,null,false); assertEquals(200,r.status); assertTrue(r.body.toString().startsWith("[")); }
    @Test public void auditPageRequiresAdmin() throws Exception {
        Class<?> type=Class.forName("com.ustb.hospital.filter.AuditPageFilter");
        assertTrue(Arrays.asList(type.getAnnotation(javax.servlet.annotation.WebFilter.class).urlPatterns()).contains("/hospro/audit-logs.html"));
    }
    private Response request(String role, String path, String page, boolean fail) throws Exception {
        HttpSession session=role==null?null:(HttpSession)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{HttpSession.class},(p,m,a)-> { if("getAttribute".equals(m.getName()) && SessionIdentity.CURRENT_USER_ROLE.equals(a[0])) return role; return value(m.getReturnType()); });
        HttpServletRequest req=(HttpServletRequest)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{HttpServletRequest.class},(p,m,a)->{ if("getSession".equals(m.getName())) return session; if("getPathInfo".equals(m.getName())) return path; if("getParameter".equals(m.getName()) && "page".equals(a[0])) return page; return value(m.getReturnType()); });
        Response r=new Response(); HttpServletResponse resp=(HttpServletResponse)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{HttpServletResponse.class},(p,m,a)->{ if("setStatus".equals(m.getName()))r.status=(Integer)a[0]; if("getWriter".equals(m.getName()))return new PrintWriter(r.body,true); return value(m.getReturnType()); });
        AuditService service=new AuditService(()->{throw new IllegalStateException("database unavailable");}) { public java.util.List<com.ustb.hospital.entity.AuditLog> recent(int limit) { return Collections.emptyList(); } };
        Constructor<AuditLogsServlet> ctor=AuditLogsServlet.class.getDeclaredConstructor(AuditService.class); ctor.setAccessible(true); ctor.newInstance(service).doGet(req,resp); return r;
    }
    private static Object value(Class<?> t) { if(!t.isPrimitive())return null; if(t==boolean.class)return false; if(t==long.class)return 0L; return 0; }
    private static class Response { int status=200; StringWriter body=new StringWriter(); }
}
