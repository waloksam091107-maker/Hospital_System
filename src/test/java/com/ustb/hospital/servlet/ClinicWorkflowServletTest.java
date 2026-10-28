package com.ustb.hospital.servlet;
import org.junit.Test;
import javax.servlet.http.*;
import java.io.*;
import java.lang.reflect.*;
import static org.junit.Assert.*;
public class ClinicWorkflowServletTest {
 @Test public void unauthenticatedClinicEndpointRejectsBeforeDatabase()throws Exception{
  Class<?> type;try{type=Class.forName("com.ustb.hospital.servlet.ClinicWorkflowServlet");}catch(ClassNotFoundException e){throw new AssertionError("门诊接口尚未实现",e);}
  int[] status={0};StringWriter body=new StringWriter();
  HttpServletRequest req=(HttpServletRequest)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{HttpServletRequest.class},(p,m,a)->m.getName().equals("getMethod")?"GET":null);
  HttpServletResponse resp=(HttpServletResponse)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{HttpServletResponse.class},(p,m,a)->{if(m.getName().equals("setStatus"))status[0]=(Integer)a[0];if(m.getName().equals("getWriter"))return new PrintWriter(body);return null;});
  Method method=type.getDeclaredMethod("doGet",HttpServletRequest.class,HttpServletResponse.class);method.setAccessible(true);method.invoke(type.newInstance(),req,resp);
  assertEquals(401,status[0]);assertTrue(body.toString().contains("LOGIN_REQUIRED"));
 }
}
