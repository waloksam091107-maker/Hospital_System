package com.ustb.hospital.servlet;

import com.ustb.hospital.entity.ProfessionalTitles;
import com.ustb.hospital.service.*;
import com.ustb.hospital.utils.*;
import org.junit.Test;
import javax.servlet.http.*;
import java.lang.reflect.Proxy;
import java.io.*;
import java.util.*;
import static org.junit.Assert.*;

/** Actual servlet parsing and HTTP status; persistence is isolated at its boundary. */
public class AdminMasterDataRequestTest {
    @Test public void malformedUpdateMustNotBecomeCreate() throws Exception {
        Map<String,String> params=new HashMap<String,String>();params.put("action","update");params.put("id","not-an-id");params.put("titleName","主任医师");
        Result r=post(params);assertEquals(400,r.status);assertTrue(r.body.toString().contains("INVALID_REQUEST"));
    }
    @Test public void createCannotUseClientIdToOverwriteAnExistingTitle() throws Exception {
        Map<String,String> params=new HashMap<String,String>();params.put("action","create");params.put("id","1");params.put("titleName","主任医师");
        Result r=post(params);assertEquals(200,r.status);
    }
    private Result post(Map<String,String> params)throws Exception{
        HttpSession session=(HttpSession)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{HttpSession.class},(p,m,a)->{
            if(m.getName().equals("getAttribute")){if(SessionIdentity.CURRENT_USER_ROLE.equals(a[0]))return "ADMIN";if(SessionIdentity.CURRENT_ADMIN_ID.equals(a[0]))return 1;}return primitive(m.getReturnType());});
        HttpServletRequest request=(HttpServletRequest)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{HttpServletRequest.class},(p,m,a)->{
            if(m.getName().equals("getSession"))return session;if(m.getName().equals("getParameter"))return params.get(a[0]);return primitive(m.getReturnType());});
        Result result=new Result();HttpServletResponse response=(HttpServletResponse)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{HttpServletResponse.class},(p,m,a)->{
            if(m.getName().equals("setStatus"))result.status=(Integer)a[0];if(m.getName().equals("getWriter"))return new PrintWriter(result.body,true);return primitive(m.getReturnType());});
        AdminMasterDataService isolated=new AdminMasterDataService(){@Override public AppointmentOperationResult saveProfessionalTitle(ProfessionalTitles title,AuditContext audit){
            return title.getId()==0?AppointmentOperationResult.success("created"):AppointmentOperationResult.failure("INVALID_REQUEST","unexpected update");}};
        new PortalAdminProfessionalTitlesServlet(isolated).doPost(request,response);return result;
    }
    private static Object primitive(Class<?> t){if(!t.isPrimitive())return null;if(t==boolean.class)return false;if(t==long.class)return 0L;return 0;}
    private static class Result{int status=200;StringWriter body=new StringWriter();}
}
