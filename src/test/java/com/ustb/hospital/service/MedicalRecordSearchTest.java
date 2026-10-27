package com.ustb.hospital.service;

import com.ustb.hospital.entity.MedicalRecord;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.*;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.junit.Before;
import org.junit.Test;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.Statement;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import static org.junit.Assert.*;

/** Real queries in a disposable H2 database, including query-count regression coverage. */
public class MedicalRecordSearchTest {
    private MedicalRecordsServiceImpl service;
    private final QueryCounter counter = new QueryCounter();

    @Before public void setup() throws Exception {
        Properties p = new Properties();
        p.setProperty("db.driver", "org.h2.Driver");
        p.setProperty("db.url", "jdbc:h2:mem:record_search_" + UUID.randomUUID().toString().replace("-", "") + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        p.setProperty("db.username", "sa"); p.setProperty("db.password", "");
        SqlSessionFactory factory;
        try (InputStream in = Resources.getResourceAsStream("mybatis-config.xml")) { factory = new SqlSessionFactoryBuilder().build(in, p); }
        factory.getConfiguration().addInterceptor(counter);
        try (SqlSession session = factory.openSession(true); Statement s = session.getConnection().createStatement()) {
            s.execute("CREATE TABLE doctors(doctor_id INT PRIMARY KEY, name VARCHAR(50))");
            s.execute("INSERT INTO doctors VALUES(1,'王医生'),(2,'李医生')");
            s.execute("CREATE TABLE appointments(appointment_id INT PRIMARY KEY, doctor_id INT, patient_id INT, appointment_date DATE, appointment_time VARCHAR(10))");
            s.execute("CREATE TABLE medical_records(medical_record_id BIGINT PRIMARY KEY, appointment_id INT, doctor_id INT, patient_id INT, diagnosis VARCHAR(500), treatment_plan TEXT, medical_advice TEXT, record_status VARCHAR(10), version INT DEFAULT 0, finalized_at TIMESTAMP, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            s.execute("CREATE TABLE medical_orders(medical_order_id BIGINT PRIMARY KEY, medical_record_id BIGINT, item_name VARCHAR(200), dosage VARCHAR(100), frequency VARCHAR(100), quantity INT, instruction_text VARCHAR(500), created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            for (int id = 1; id <= 15; id++) {
                int patient = id == 14 ? 2 : 1;
                int doctor = id % 2 + 1;
                String date = id <= 6 ? "2030-01-01" : "2030-01-02";
                s.execute("INSERT INTO appointments VALUES("+id+","+doctor+","+patient+",'"+date+"','AM')");
                s.execute("INSERT INTO medical_records(medical_record_id,appointment_id,doctor_id,patient_id,diagnosis,record_status,finalized_at) VALUES("+id+","+id+","+doctor+","+patient+",'diagnosis', '"+(id == 15 ? "DRAFT" : "FINAL")+"','2030-01-03 10:00:00')");
                s.execute("INSERT INTO medical_orders(medical_order_id,medical_record_id,item_name) VALUES("+id+","+id+",'项目"+id+"')");
            }
        }
        service = new MedicalRecordsServiceImpl(() -> factory.openSession(false), () -> factory.openSession(true), new AuditService(() -> factory.openSession(true)));
        counter.sql.clear();
    }

    @Test public void defaultsIsolatePatientAndFinalRecordsAndBatchOrders() throws Exception {
        Object result = search(1, null, null, null, null, null);
        assertEquals(13L, number(result, "getTotal"));
        assertEquals(1L, number(result, "getPageNum")); assertEquals(10L, number(result, "getPageSize")); assertEquals(2L, number(result, "getPages"));
        List<MedicalRecord> records = items(result); assertEquals(10, records.size());
        assertEquals(13, records.get(0).getAppointmentId());
        for (MedicalRecord record : records) {
            assertEquals(1, record.getPatientId()); assertEquals("FINAL", record.getRecordStatus());
            assertEquals(1, record.getOrders().size()); assertEquals("项目"+record.getAppointmentId(), record.getOrders().get(0).getItemName());
        }
        assertEquals("王医生", records.get(1).getClass().getMethod("getDoctorName").invoke(records.get(1)));
        assertEquals("2030-01-02", records.get(0).getClass().getMethod("getAppointmentDate").invoke(records.get(0)));
        assertEquals("AM", records.get(0).getClass().getMethod("getAppointmentTime").invoke(records.get(0)));
        assertEquals("one count, one page, one batch order query", 3, counter.sql.size());
        assertEquals(1, counter.sql.stream().filter(sql -> sql.contains("FROM medical_orders")).count());
    }

    @Test public void combinesInclusiveAppointmentDatesAndDoctorAndStablePages() throws Exception {
        Object page = search(1, "2030-01-01", "2030-01-01", "2", "2", "2");
        assertEquals(3L, number(page, "getTotal")); assertEquals(2L, number(page, "getPages"));
        assertEquals(1, items(page).size()); assertEquals(1, items(page).get(0).getAppointmentId());
        assertEquals(3, items(search(1, "2030-01-01", "2030-01-01", "2", "1", "2")).get(1).getAppointmentId());
    }

    @Test public void emptyOrBeyondLastPageDoesNotQueryAnEmptyOrderSet() throws Exception {
        Object empty = search(1, "2031-01-01", null, null, "1", null);
        assertEquals(0, number(empty, "getTotal")); assertEquals(0, number(empty, "getPages")); assertTrue(items(empty).isEmpty());
        assertFalse(counter.sql.stream().anyMatch(sql -> sql.contains("FROM medical_orders")));
        counter.sql.clear();
        assertTrue(items(search(1, null, null, null, "99", "10")).isEmpty());
        assertFalse(counter.sql.stream().anyMatch(sql -> sql.contains("FROM medical_orders")));
    }

    @Test public void queryRejectsInvalidDatesAndPagingAndCapsPageSize() throws Exception {
        for (String[] args : new String[][] { {"2030-02-30",null,null,null,null}, {"2030-02-02","2030-01-01",null,null,null}, {null,null,"0",null,null}, {null,null,null,"0",null}, {null,null,null,"2147483648",null}, {null,null,null,null,"-1"} }) {
            try { search(1,args[0],args[1],args[2],args[3],args[4]); fail("invalid query accepted: "+Arrays.toString(args)); }
            catch (IllegalArgumentException expected) { assertNotNull(expected.getMessage()); }
        }
        assertEquals(100, number(search(1,null,null,null,null,"999"), "getPageSize"));
    }

    private Object search(int patient, String from, String to, String doctor, String page, String size) throws Exception {
        try {
            Class<?> queryType = Class.forName("com.ustb.hospital.service.MedicalRecordQuery");
            Object query = queryType.getConstructor(String.class,String.class,String.class,String.class,String.class).newInstance(from,to,doctor,page,size);
            return MedicalRecordsServiceImpl.class.getMethod("searchForPatient", int.class, queryType).invoke(service,patient,query);
        } catch (ClassNotFoundException | NoSuchMethodException missing) { throw new AssertionError("paginated patient medical-record search is missing", missing); }
        catch (InvocationTargetException wrapped) { if (wrapped.getCause() instanceof IllegalArgumentException) throw (IllegalArgumentException) wrapped.getCause(); throw wrapped; }
    }
    @SuppressWarnings("unchecked") private List<MedicalRecord> items(Object result) throws Exception { return (List<MedicalRecord>) result.getClass().getMethod("getItems").invoke(result); }
    private long number(Object result, String method) throws Exception { return ((Number)result.getClass().getMethod(method).invoke(result)).longValue(); }

    @Intercepts(@Signature(type=StatementHandler.class, method="prepare", args={Connection.class}))
    public static class QueryCounter implements Interceptor {
        final List<String> sql = new ArrayList<String>();
        public Object intercept(Invocation invocation) throws Throwable { sql.add(((StatementHandler)invocation.getTarget()).getBoundSql().getSql()); return invocation.proceed(); }
        public Object plugin(Object target) { return Plugin.wrap(target,this); }
        public void setProperties(Properties properties) { }
    }
}
