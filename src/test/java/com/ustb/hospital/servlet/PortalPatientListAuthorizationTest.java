package com.ustb.hospital.servlet;

import com.ustb.hospital.utils.MybatisUtil;
import com.ustb.hospital.utils.SessionIdentity;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.*;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.*;
import javax.servlet.http.*;
import java.io.*;
import java.lang.reflect.*;
import java.sql.*;
import java.util.*;
import static org.junit.Assert.*;

/** Executes the real patient-list servlet and SQL with disposable H2 data. */
public class PortalPatientListAuthorizationTest {
    private Field field; private Object previous;
    @Before public void setup() throws Exception {
        UnpooledDataSource source=new UnpooledDataSource("org.h2.Driver","jdbc:h2:mem:patient_list_"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1","sa","");
        try(Connection c=source.getConnection();Statement s=c.createStatement()) {
            s.execute("CREATE TABLE patients(patient_id INT PRIMARY KEY,id_card_number VARCHAR(50),password VARCHAR(255),name VARCHAR(100),avatar VARCHAR(255),phone VARCHAR(50),email VARCHAR(255),balance DECIMAL(12,2))");
            s.execute("INSERT INTO patients VALUES(2,'private-identity',NULL,'另一位患者',NULL,'private-phone','private@example.test',10)");
            s.execute("CREATE TABLE audit_logs(audit_log_id BIGINT AUTO_INCREMENT PRIMARY KEY,actor_role VARCHAR(20),actor_id INT,action VARCHAR(80),target_type VARCHAR(80),target_id VARCHAR(80),outcome VARCHAR(20),request_method VARCHAR(10),request_path VARCHAR(255),remote_address VARCHAR(64),created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        }
        Configuration config=new Configuration(new Environment("test",new JdbcTransactionFactory(),source));
        com.github.pagehelper.PageInterceptor paging=new com.github.pagehelper.PageInterceptor();
        Properties pagingProperties=new Properties();pagingProperties.setProperty("helperDialect","h2");paging.setProperties(pagingProperties);config.addInterceptor(paging);
        for(String name:new String[]{"PatientsMapper","AuditLogsMapper"}) {
            String path="com/ustb/hospital/mapper/"+name+".xml";
            try(InputStream input=Resources.getResourceAsStream(path)){new XMLMapperBuilder(input,config,path,config.getSqlFragments()).parse();}
        }
        field=MybatisUtil.class.getDeclaredField("sqlSessionFactory");field.setAccessible(true);previous=field.get(null);field.set(null,new SqlSessionFactoryBuilder().build(config));
    }
    @After public void restore()throws Exception{if(field!=null)field.set(null,previous);}
    @Test public void patientCannotEnumerateOtherPatients()throws Exception{assertDenied("PATIENT",403);}
    @Test public void doctorCannotEnumerateAllPatientIdentities()throws Exception{assertDenied("DOCTOR",403);}
    @Test public void anonymousCannotEnumeratePatientIdentities()throws Exception{assertDenied(null,401);}
    @Test public void adminCanUseExistingPatientDirectory()throws Exception{
        Response result=request("ADMIN");assertEquals(200,result.status);assertTrue(result.body.toString().contains("private-identity"));
    }
    @Test public void incomeEndpointRequiresAnAdminSession()throws Exception{
        assertEquals(401,request(null,true).status);assertEquals(403,request("PATIENT",true).status);assertEquals(403,request("DOCTOR",true).status);
    }
    @Test public void incomeEndpointRejectsInvalidDate()throws Exception{assertEquals(400,request("ADMIN",true).status);}
    @Test public void invalidAdminPagingIsRejectedBeforeQuery()throws Exception{
        for(String[] values:new String[][]{{"0","5"},{"-1","5"},{"abc","5"},{"1","0"},{"1","101"},{"1","abc"}}){
            Response result=request("ADMIN",false,values[0],values[1]);assertNull("Invalid paging must not throw",result.failure);assertEquals(400,result.status);
        }
    }
    @Test public void adminPageSizeIsHonored()throws Exception{
        Response result=request("ADMIN",false,"1","3");assertNull(result.failure);assertEquals(200,result.status);assertTrue(result.body.toString().contains("\"pageSize\":3"));
    }
    private void assertDenied(String role,int status)throws Exception{
        Response result=request(role);assertEquals("Patient directory must reject role "+role,status,result.status);assertFalse(result.body.toString().contains("private-identity"));
    }
    private Response request(String role)throws Exception{
        return request(role,false);
    }
    private Response request(String role,boolean income)throws Exception{
        return request(role,income,null,null);
    }
    private Response request(String role,boolean income,String page,String size)throws Exception{
        HttpSession session=(HttpSession)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{HttpSession.class},(p,m,a)->{
            if("getAttribute".equals(m.getName())){if(SessionIdentity.CURRENT_USER_ROLE.equals(a[0]))return role;if(Arrays.asList(SessionIdentity.CURRENT_PATIENT_ID,SessionIdentity.CURRENT_DOCTOR_ID,SessionIdentity.CURRENT_ADMIN_ID).contains(a[0]))return 1;}return value(m.getReturnType());
        });
        HttpServletRequest req=(HttpServletRequest)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{HttpServletRequest.class},(p,m,a)->{
            if("getSession".equals(m.getName()))return role==null?null:session;if("getParameter".equals(m.getName()))return "pageNum".equals(a[0])?page:"pageSize".equals(a[0])?size:null;if("getMethod".equals(m.getName()))return "GET";if("getServletPath".equals(m.getName()))return "/portal/patients";return value(m.getReturnType());
        });
        Response result=new Response();HttpServletResponse resp=(HttpServletResponse)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{HttpServletResponse.class},(p,m,a)->{
            if("setStatus".equals(m.getName()))result.status=(Integer)a[0];if("getWriter".equals(m.getName()))return new PrintWriter(result.body,true);return value(m.getReturnType());
        });
        try{if(income)new PortalIncomeReportServlet().doGet(req,resp);else new PortalPatientListServlet().doGet(req,resp);}catch(RuntimeException failure){result.failure=failure;}return result;
    }
    private static Object value(Class<?> t){if(!t.isPrimitive())return null;if(t==boolean.class)return false;if(t==long.class)return 0L;return 0;}
    private static class Response{int status=200;StringWriter body=new StringWriter();Throwable failure;}
}
