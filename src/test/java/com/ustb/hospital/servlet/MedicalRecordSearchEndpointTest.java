package com.ustb.hospital.servlet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ustb.hospital.service.*;
import com.ustb.hospital.utils.SessionIdentity;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.*;
import org.junit.Before;
import org.junit.Test;
import javax.servlet.http.*;
import java.io.*;
import java.lang.reflect.Proxy;
import java.sql.Statement;
import java.util.*;
import static org.junit.Assert.*;

/** Calls the actual servlet, service and mappers; no business database is used. */
public class MedicalRecordSearchEndpointTest {
    private MedicalRecordsServlet servlet;
    @Before public void setup() throws Exception {
        Properties p = new Properties(); p.setProperty("db.driver","org.h2.Driver");
        p.setProperty("db.url","jdbc:h2:mem:clinical_endpoint_"+UUID.randomUUID().toString().replace("-","")+";MODE=MySQL;DB_CLOSE_DELAY=-1");
        p.setProperty("db.username","sa"); p.setProperty("db.password","");
        SqlSessionFactory factory;
        try(InputStream in=Resources.getResourceAsStream("mybatis-config.xml")){factory=new SqlSessionFactoryBuilder().build(in,p);}
        try(SqlSession session=factory.openSession(true); Statement s=session.getConnection().createStatement()){
            s.execute("CREATE TABLE doctors(doctor_id INT PRIMARY KEY,name VARCHAR(50))"); s.execute("INSERT INTO doctors VALUES(1,'医生')");
            s.execute("CREATE TABLE appointments(appointment_id INT PRIMARY KEY,doctor_id INT,patient_id INT,appointment_date DATE,appointment_time VARCHAR(10))");
            s.execute("INSERT INTO appointments VALUES(1,1,1,'2030-01-01','AM'),(2,1,2,'2030-01-01','AM'),(3,1,1,'2030-01-01','AM')");
            s.execute("CREATE TABLE medical_records(medical_record_id BIGINT PRIMARY KEY,appointment_id INT,doctor_id INT,patient_id INT,diagnosis VARCHAR(500),treatment_plan TEXT,medical_advice TEXT,record_status VARCHAR(10),version INT DEFAULT 0,finalized_at TIMESTAMP,created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            s.execute("INSERT INTO medical_records(medical_record_id,appointment_id,doctor_id,patient_id,record_status) VALUES(1,1,1,1,'FINAL'),(2,2,1,2,'FINAL'),(3,3,1,1,'DRAFT')");
            s.execute("CREATE TABLE medical_orders(medical_order_id BIGINT PRIMARY KEY,medical_record_id BIGINT,item_name VARCHAR(200),dosage VARCHAR(100),frequency VARCHAR(100),quantity INT,instruction_text VARCHAR(500),created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        }
        servlet=new MedicalRecordsServlet(new MedicalRecordsServiceImpl(()->factory.openSession(false),()->factory.openSession(true),new AuditService(()->factory.openSession(true))));
    }
    @Test public void sessionIdentityWinsOverPatientIdParameter() throws Exception {
        Response r=request("PATIENT","/patient/search",params("patientId","2"));
        assertEquals(200,r.status); JsonNode json=r.json();
        assertTrue(json.get("items").isArray()); assertEquals(1,json.get("total").asInt());
        assertEquals(1,json.get("items").get(0).get("patientId").asInt()); assertEquals("FINAL",json.get("items").get(0).get("recordStatus").asText());
    }
    @Test public void legacyArrayAndSingleRecordContractsStillWork() throws Exception {
        JsonNode list=request("PATIENT","/patient",Collections.<String,String>emptyMap()).json();
        assertTrue(list.isArray()); assertEquals(1,list.size());
        assertEquals(200,request("PATIENT","/patient",params("appointmentId","1")).status);
        assertEquals(404,request("PATIENT","/patient",params("appointmentId","2")).status);
        assertEquals(404,request("PATIENT","/patient",params("appointmentId","3")).status);
    }
    @Test public void roleAndInvalidQueryHaveExpectedHttpStatuses() throws Exception {
        assertEquals(401,request(null,"/patient/search",Collections.<String,String>emptyMap()).status);
        assertEquals(403,request("DOCTOR","/patient/search",Collections.<String,String>emptyMap()).status);
        assertEquals(400,request("PATIENT","/patient/search",params("dateFrom","2030-02-30")).status);
        assertEquals(400,request("PATIENT","/patient/search",params("pageNum","-1")).status);
        assertEquals(400,request("PATIENT","/patient/search",params("doctorId","1 OR 1=1")).status);
    }
    private Response request(String role,String path,Map<String,String> parameters) throws Exception {
        HttpSession session=role==null?null:(HttpSession)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{HttpSession.class},(p,m,a)->{
            if("getAttribute".equals(m.getName())) {if(SessionIdentity.CURRENT_USER_ROLE.equals(a[0]))return role;if(SessionIdentity.CURRENT_PATIENT_ID.equals(a[0]))return 1;} return value(m.getReturnType());
        });
        HttpServletRequest req=(HttpServletRequest)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{HttpServletRequest.class},(p,m,a)->{
            if("getSession".equals(m.getName()))return session;if("getPathInfo".equals(m.getName()))return path;if("getParameter".equals(m.getName()))return parameters.get(a[0]);return value(m.getReturnType());
        });
        Response result=new Response();
        HttpServletResponse resp=(HttpServletResponse)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{HttpServletResponse.class},(p,m,a)->{
            if("setStatus".equals(m.getName()))result.status=(Integer)a[0];if("getWriter".equals(m.getName()))return new PrintWriter(result.body,true);return value(m.getReturnType());
        });
        servlet.doGet(req,resp);return result;
    }
    private static Map<String,String> params(String key,String value){return Collections.singletonMap(key,value);}
    private static Object value(Class<?> t){if(!t.isPrimitive())return null;if(t==boolean.class)return false;if(t==long.class)return 0L;return 0;}
    private static class Response {int status=200;StringWriter body=new StringWriter();JsonNode json()throws Exception{return new ObjectMapper().readTree(body.toString());}}
}
