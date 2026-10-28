package com.ustb.hospital.security;

import com.ustb.hospital.filter.AuthenticationFilter;
import com.ustb.hospital.service.AuditService;
import com.ustb.hospital.utils.SessionIdentity;
import org.junit.Test;
import javax.servlet.http.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.Assert.*;

public class AccountSecurityBoundaryTest {
    @Test public void unversionedSessionCannotEnterProtectedBusinessEndpoint() throws Exception {
        Map<String,Object> attrs=new HashMap<>();HttpSession session=session(attrs,new AtomicBoolean());SessionIdentity.set(session,"PATIENT",1,"Patient");
        int[] status={200};new AuthenticationFilter(new AuditService(){public void recordFailure(com.ustb.hospital.service.AuditContext c,String a,String t,String id){}})
                .doFilter(request(session,"/appointments/book"),response(status),(q,s)->fail("Session without authenticated account version reached protected servlet"));
        assertEquals(401,status[0]);
    }
    @Test public void logoutInvalidatesServerSessionAndRejectsGet()throws Exception{
        Class<?> type;try{type=Class.forName("com.ustb.hospital.servlet.LogoutServlet");}catch(ClassNotFoundException e){throw new AssertionError("Logout must destroy server session",e);}
        AtomicBoolean invalid=new AtomicBoolean();HttpSession session=session(new HashMap<>(),invalid);int[] status={200};
        Method post=type.getDeclaredMethod("doPost",HttpServletRequest.class,HttpServletResponse.class);post.setAccessible(true);post.invoke(type.newInstance(),request(session,"/auth/logout"),response(status));assertTrue(invalid.get());
        Method get=type.getDeclaredMethod("doGet",HttpServletRequest.class,HttpServletResponse.class);get.setAccessible(true);get.invoke(type.newInstance(),request(session,"/auth/logout"),response(status));assertEquals(405,status[0]);
    }
    private HttpSession session(Map<String,Object> attrs,AtomicBoolean invalid){return (HttpSession)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{HttpSession.class},(p,m,a)->{if(m.getName().equals("getAttribute"))return attrs.get(a[0]);if(m.getName().equals("setAttribute")){attrs.put((String)a[0],a[1]);return null;}if(m.getName().equals("removeAttribute")){attrs.remove(a[0]);return null;}if(m.getName().equals("invalidate")){invalid.set(true);attrs.clear();return null;}return value(m.getReturnType());});}
    private HttpServletRequest request(HttpSession session,String path){return (HttpServletRequest)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{HttpServletRequest.class},(p,m,a)->{if(m.getName().equals("getSession"))return session;if(m.getName().equals("getServletPath"))return path;if(m.getName().equals("getContextPath"))return "";if(m.getName().equals("getMethod"))return "POST";return value(m.getReturnType());});}
    private HttpServletResponse response(int[] status){return (HttpServletResponse)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{HttpServletResponse.class},(p,m,a)->{if(m.getName().equals("setStatus")||m.getName().equals("sendError")){status[0]=(Integer)a[0];return null;}if(m.getName().equals("getWriter"))return new PrintWriter(new StringWriter());return value(m.getReturnType());});}
    private static Object value(Class<?> t){if(!t.isPrimitive())return null;if(t==boolean.class)return false;if(t==long.class)return 0L;return 0;}
}
