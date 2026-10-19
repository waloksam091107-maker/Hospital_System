package com.ustb.hospital.servlet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ustb.hospital.entity.MedicalOrder;
import com.ustb.hospital.entity.MedicalRecord;
import com.ustb.hospital.service.MedicalRecordOperationResult;
import com.ustb.hospital.service.MedicalRecordsServiceImpl;
import com.ustb.hospital.utils.MybatisUtil;
import com.ustb.hospital.utils.SessionIdentity;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/** Exercises the servlet and its real MyBatis SQL using an isolated in-memory database. */
public class DoctorAppointmentDetailTest {
    private Field factoryField;
    private Object previousFactory;
    private DataSource dataSource;

    @Before
    public void setupDatabase() throws Exception {
        dataSource = new UnpooledDataSource("org.h2.Driver", "jdbc:h2:mem:encounter;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        try (Connection connection = dataSource.getConnection(); Statement sql = connection.createStatement()) {
            sql.execute("DROP ALL OBJECTS");
            sql.execute("CREATE TABLE appointments (appointment_id INT PRIMARY KEY, doctor_id INT, patient_id INT, appointment_date VARCHAR(10), appointment_time VARCHAR(20), status VARCHAR(30), started_at TIMESTAMP, completed_at TIMESTAMP, version INT DEFAULT 0)");
            sql.execute("INSERT INTO appointments (appointment_id,doctor_id,patient_id,appointment_date,appointment_time,status) VALUES (6, 1, 10, '2030-01-11', 'AM', 'IN_PROGRESS'), (7, 2, 11, '2030-01-12', 'PM', 'IN_PROGRESS')");
            sql.execute("CREATE TABLE medical_records (medical_record_id BIGINT AUTO_INCREMENT PRIMARY KEY, appointment_id INT UNIQUE, doctor_id INT, patient_id INT, diagnosis VARCHAR(500), treatment_plan VARCHAR(5000), medical_advice VARCHAR(5000), record_status VARCHAR(10), version INT DEFAULT 0, finalized_at TIMESTAMP, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            sql.execute("CREATE TABLE medical_orders (medical_order_id BIGINT AUTO_INCREMENT PRIMARY KEY, medical_record_id BIGINT, item_name VARCHAR(200) CHECK (item_name <> '故障'), dosage VARCHAR(100), frequency VARCHAR(100), quantity INT, instruction_text VARCHAR(500), created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            sql.execute("CREATE TABLE audit_logs (audit_log_id BIGINT AUTO_INCREMENT PRIMARY KEY, actor_role VARCHAR(20), actor_id INT, action VARCHAR(80), target_type VARCHAR(80), target_id VARCHAR(80), outcome VARCHAR(20), request_method VARCHAR(10), request_path VARCHAR(255), remote_address VARCHAR(64), created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            connection.commit();
        }
        Configuration configuration = new Configuration(new Environment("test", new JdbcTransactionFactory(), dataSource));
        String mapperPath = "com/ustb/hospital/mapper/AppointmentsMapper.xml";
        try (InputStream input = Resources.getResourceAsStream(mapperPath)) {
            new XMLMapperBuilder(input, configuration, mapperPath, configuration.getSqlFragments()).parse();
        }
        String recordsPath = "com/ustb/hospital/mapper/MedicalRecordsMapper.xml";
        try (InputStream input = Resources.getResourceAsStream(recordsPath)) {
            new XMLMapperBuilder(input, configuration, recordsPath, configuration.getSqlFragments()).parse();
        }
        String auditPath = "com/ustb/hospital/mapper/AuditLogsMapper.xml";
        try (InputStream input = Resources.getResourceAsStream(auditPath)) {
            new XMLMapperBuilder(input, configuration, auditPath, configuration.getSqlFragments()).parse();
        }
        SqlSessionFactory factory = new SqlSessionFactoryBuilder().build(configuration);
        factoryField = MybatisUtil.class.getDeclaredField("sqlSessionFactory");
        factoryField.setAccessible(true);
        previousFactory = factoryField.get(null);
        factoryField.set(null, factory);
    }

    @After
    public void restoreFactory() throws Exception { if (factoryField != null) factoryField.set(null, previousFactory); }

    @Test
    public void ownAppointmentCanBeReadByIdRegardlessOfPagination() throws Exception {
        Response result = request("DOCTOR", 1, "6");
        assertEquals(200, result.status);
        JsonNode json = new ObjectMapper().readTree(result.body.toString());
        assertEquals(6, json.get("appointmentId").asInt());
        assertEquals("IN_PROGRESS", json.get("status").asText());
        assertEquals("2030-01-11", json.get("appointmentDate").asText());
        assertEquals("AM", json.get("appointmentTime").asText());
        assertFalse(json.has("patientId"));
    }

    @Test
    public void anotherDoctorsAppointmentAndMissingAppointmentAreNotDisclosed() throws Exception {
        Response other = request("DOCTOR", 1, "7");
        Response missing = request("DOCTOR", 1, "999");
        assertEquals(404, other.status);
        assertEquals(404, missing.status);
        assertEquals(new ObjectMapper().readTree(missing.body.toString()), new ObjectMapper().readTree(other.body.toString()));
    }

    @Test
    public void rejectsInvalidIdsAndNonDoctorSessions() throws Exception {
        assertEquals(400, request("DOCTOR", 1, "0").status);
        assertEquals(400, request("DOCTOR", 1, "abc").status);
        assertEquals(403, request("PATIENT", 1, "6").status);
        assertEquals(401, request(null, 0, "6").status);
    }

    @Test
    public void finalizationSavesOrdersAndCompletesTheAppointmentForItsPatient() {
        MedicalOrder order = new MedicalOrder();
        order.setItemName("血常规");
        order.setQuantity(1);
        MedicalRecordsServiceImpl service = new MedicalRecordsServiceImpl();
        MedicalRecordOperationResult result = service.finalizeEncounter(1, 6, "测试诊断", "诊疗方案", "医嘱说明", Collections.singletonList(order));
        assertTrue(result.getMessage(), result.isSuccessful());
        MedicalRecord record = service.findForPatient(10, 6);
        assertNotNull(record);
        assertEquals("测试诊断", record.getDiagnosis());
        assertEquals("医嘱说明", record.getMedicalAdvice());
        assertEquals(1, record.getOrders().size());
        assertEquals("血常规", record.getOrders().get(0).getItemName());
        assertNull(service.findForPatient(11, 6));
        assertFalse(service.finalizeEncounter(1, 6, "重复诊断", null, null, Collections.<MedicalOrder>emptyList()).isSuccessful());
    }

    @Test
    public void finalizationServletParsesMultipleOrdersAndPersistsThem() throws Exception {
        Map<String, String[]> parameters = new HashMap<String, String[]>();
        parameters.put("appointmentId", new String[]{"6"});
        parameters.put("diagnosis", new String[]{"测试诊断"});
        parameters.put("treatmentPlan", new String[]{"诊疗方案"});
        parameters.put("medicalAdvice", new String[]{"注意休息"});
        parameters.put("orderItem", new String[]{"血常规", "尿常规"});
        parameters.put("orderDosage", new String[]{"", ""});
        parameters.put("orderFrequency", new String[]{"一次", "一次"});
        parameters.put("orderQuantity", new String[]{"1", "2"});
        parameters.put("orderInstruction", new String[]{"空腹", "留样"});
        Response response = request("DOCTOR", 1, parameters, "MedicalRecordsServlet", "doPost", "/finalize");
        assertEquals(200, response.status);
        assertTrue(new ObjectMapper().readTree(response.body.toString()).get("success").asBoolean());
        MedicalRecord record = new MedicalRecordsServiceImpl().findForPatient(10, 6);
        assertEquals(2, record.getOrders().size());
        assertEquals("血常规", record.getOrders().get(0).getItemName());
        assertEquals("尿常规", record.getOrders().get(1).getItemName());
        assertEquals(Integer.valueOf(2), record.getOrders().get(1).getQuantity());
    }

    @Test
    public void aFailedOrderInsertRollsBackTheMedicalRecordAndAppointmentState() throws Exception {
        MedicalOrder order = new MedicalOrder();
        order.setItemName("故障");
        MedicalRecordsServiceImpl service = new MedicalRecordsServiceImpl();
        assertFalse(service.finalizeEncounter(1, 6, "测试诊断", "诊疗方案", null, Collections.singletonList(order)).isSuccessful());
        assertNull(service.findForPatient(10, 6));
        try (Connection connection = dataSource.getConnection(); Statement sql = connection.createStatement(); java.sql.ResultSet rows = sql.executeQuery("SELECT status FROM appointments WHERE appointment_id=6")) {
            assertTrue(rows.next());
            assertEquals("IN_PROGRESS", rows.getString(1));
        }
    }

    private static Response request(final String role, final int id, final String appointmentId) throws Exception {
        Map<String, String[]> parameters = new HashMap<String, String[]>();
        parameters.put("appointmentId", new String[]{appointmentId});
        return request(role, id, parameters, "DoctorAppointmentDetailServlet", "doGet", null);
    }

    private static Response request(final String role, final int id, final Map<String, String[]> parameters,
                                    String servletName, String httpMethod, final String pathInfo) throws Exception {
        final HttpSession session = role == null ? null : (HttpSession) Proxy.newProxyInstance(HttpSession.class.getClassLoader(), new Class<?>[]{HttpSession.class}, (proxy, method, args) -> {
            if ("getAttribute".equals(method.getName())) {
                if (SessionIdentity.CURRENT_USER_ROLE.equals(args[0])) return role;
                if (SessionIdentity.CURRENT_DOCTOR_ID.equals(args[0]) && "DOCTOR".equals(role)) return id;
            }
            return defaultValue(method.getReturnType());
        });
        HttpServletRequest request = (HttpServletRequest) Proxy.newProxyInstance(HttpServletRequest.class.getClassLoader(), new Class<?>[]{HttpServletRequest.class}, (proxy, method, args) -> {
            if ("getSession".equals(method.getName())) return session;
            if ("getPathInfo".equals(method.getName())) return pathInfo;
            if ("getParameterValues".equals(method.getName())) return parameters.get(args[0]);
            if ("getParameter".equals(method.getName())) {
                String[] values = parameters.get(args[0]);
                return values == null || values.length == 0 ? null : values[0];
            }
            return defaultValue(method.getReturnType());
        });
        Response result = new Response();
        HttpServletResponse response = (HttpServletResponse) Proxy.newProxyInstance(HttpServletResponse.class.getClassLoader(), new Class<?>[]{HttpServletResponse.class}, (proxy, method, args) -> {
            if ("setStatus".equals(method.getName())) result.status = (Integer) args[0];
            if ("getWriter".equals(method.getName())) return new PrintWriter(result.body, true);
            return defaultValue(method.getReturnType());
        });
        Class<?> type;
        try { type = Class.forName("com.ustb.hospital.servlet." + servletName); }
        catch (ClassNotFoundException exception) { throw new AssertionError("Doctor appointment detail endpoint is missing"); }
        Method handler = type.getDeclaredMethod(httpMethod, HttpServletRequest.class, HttpServletResponse.class);
        handler.setAccessible(true);
        try { handler.invoke(type.getDeclaredConstructor().newInstance(), request, response); }
        catch (InvocationTargetException exception) { throw new AssertionError("detail request failed", exception.getCause()); }
        return result;
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == char.class) return '\0';
        if (type == long.class) return 0L;
        if (type == float.class) return 0F;
        if (type == double.class) return 0D;
        return 0;
    }
    private static class Response { int status = 200; final StringWriter body = new StringWriter(); }
}
