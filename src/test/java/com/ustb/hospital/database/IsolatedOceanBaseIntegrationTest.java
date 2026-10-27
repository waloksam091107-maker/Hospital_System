package com.ustb.hospital.database;

import com.ustb.hospital.service.*;
import com.ustb.hospital.entity.*;
import com.ustb.hospital.mapper.MasterDataMapper;
import com.ustb.hospital.utils.AppointmentOperationResult;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.apache.ibatis.session.*;
import org.junit.*;
import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.Assert.*;

/** Opt-in real OceanBase test. Requires the restricted account created by the isolated wrapper. */
public class IsolatedOceanBaseIntegrationTest {
    private static SqlSessionFactory factory;
    private static String schema;

    @BeforeClass public static void createReviewedSchemaInRestrictedRandomDatabase() throws Exception {
        Assume.assumeTrue("Run scripts/oceanbase/run-isolated-tests.ps1 explicitly", Boolean.getBoolean("hospital.test.oceanbase"));
        schema = required("HOSPITAL_TEST_DB_NAME");
        assertTrue("A random isolated schema is mandatory", schema.matches("hospital_test_[a-f0-9]{32}"));
        String url = required("HOSPITAL_TEST_DB_URL");
        assertTrue("Only the local isolated endpoint may be used", url.matches("jdbc:oceanbase://127\\.0\\.0\\.1:[0-9]{4,5}/" + schema + "\\?.*"));
        int port = Integer.parseInt(url.substring(url.indexOf("127.0.0.1:") + 10, url.indexOf('/' , url.indexOf("127.0.0.1:") + 10)));
        assertTrue("Business ports forbidden", port != 2881 && port != 3306 && port != 8080 && port <= 65535);
        assertEquals("Only the restricted test account is allowed", "hospital_test_runner@test", required("HOSPITAL_TEST_DB_USERNAME"));
        Class.forName("com.oceanbase.jdbc.Driver");
        Properties properties = new Properties();
        properties.setProperty("db.driver", "com.oceanbase.jdbc.Driver"); properties.setProperty("db.url", url);
        properties.setProperty("db.username", required("HOSPITAL_TEST_DB_USERNAME")); properties.setProperty("db.password", required("HOSPITAL_TEST_DB_PASSWORD"));
        try (InputStream config = Resources.getResourceAsStream("mybatis-config.xml")) { factory = new SqlSessionFactoryBuilder().build(config, properties); }
        try (SqlSession session = factory.openSession(true); Statement query = session.getConnection().createStatement()) {
            try (ResultSet result = query.executeQuery("SELECT DATABASE(), VERSION()")) {
                assertTrue(result.next()); assertEquals(schema, result.getString(1));
                assertTrue("Must run on OceanBase", result.getString(2).toLowerCase(Locale.ROOT).contains("oceanbase"));
            }
            try (ResultSet result = query.executeQuery("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE()")) {
                assertTrue(result.next()); assertEquals("Refuse to initialize a nonempty target", 0, result.getInt(1));
            }
            try { query.executeQuery("SELECT * FROM hospt.patients LIMIT 0"); fail("Test account has business database access"); } catch (SQLException inaccessible) { }
            String source = new String(Files.readAllBytes(Paths.get("database/oceanbase/01_hospt_schema.sql")), StandardCharsets.UTF_8);
            ScriptRunner runner = new ScriptRunner(session.getConnection()); runner.setLogWriter(null); runner.setErrorLogWriter(null); runner.setStopOnError(true);
            runner.runScript(new StringReader(IsolatedOceanBaseSql.rewriteSchema(source, schema)));
        }
    }

    @Test public void bookingPersistsGeneratedKeyDebitLedgerAndMatchingAudit() throws Exception {
        Fixture fixture = fixture();
        AppointmentsServiceImpl service = appointments();
        assertTrue(service.bookAppointment(fixture.patient, fixture.doctor, fixture.date, "AM", patient(fixture.patient)).isSuccessful());
        assertEquals("90.00", scalar("SELECT balance FROM patients WHERE patient_id=" + fixture.patient));
        assertEquals("1", scalar("SELECT COUNT(*) FROM balance_transactions WHERE patient_id=" + fixture.patient + " AND transaction_type='APPOINTMENT_DEBIT'"));
        String appointment = scalar("SELECT appointment_id FROM appointments WHERE patient_id=" + fixture.patient);
        assertEquals(appointment, scalar("SELECT target_id FROM audit_logs WHERE action='APPOINTMENTS_BOOK' AND actor_id=" + fixture.patient));
        assertEquals("BOOKED", scalar("SELECT slot_status FROM schedule_slots WHERE doctor_id=" + fixture.doctor));
    }

    @Test public void failedAuditRollsBackBalanceAndLedgerOnOceanBase() throws Exception {
        Fixture fixture = fixture();
        AuditService failing = new AuditService(() -> factory.openSession(true)) {
            @Override public void writeSuccess(SqlSession session, AuditContext context, String action, String type, String id) { throw new IllegalStateException("Injected audit failure"); }
        };
        BalanceService service = new BalanceService(() -> factory.openSession(false), () -> factory.openSession(true), failing);
        assertFalse(service.recharge(fixture.patient, new BigDecimal("12.34"), patient(fixture.patient)).isSuccessful());
        assertEquals("100.00", scalar("SELECT balance FROM patients WHERE patient_id=" + fixture.patient));
        assertEquals("0", scalar("SELECT COUNT(*) FROM balance_transactions WHERE patient_id=" + fixture.patient));
    }

    @Test public void concurrentBookingsProduceExactlyOneAppointmentAndDebit() throws Exception {
        Fixture fixture = fixture();
        int other = insert("INSERT INTO patients(name,balance) VALUES('" + UUID.randomUUID() + "',100)");
        ExecutorService workers = Executors.newFixedThreadPool(2); CountDownLatch start = new CountDownLatch(1);
        try {
            Future<AppointmentOperationResult> first = workers.submit(() -> { start.await(); return appointments().bookAppointment(fixture.patient, fixture.doctor, fixture.date, "AM", patient(fixture.patient)); });
            Future<AppointmentOperationResult> second = workers.submit(() -> { start.await(); return appointments().bookAppointment(other, fixture.doctor, fixture.date, "AM", patient(other)); });
            start.countDown();
            assertTrue(first.get(30, TimeUnit.SECONDS).isSuccessful() ^ second.get(30, TimeUnit.SECONDS).isSuccessful());
        } finally { workers.shutdownNow(); }
        assertEquals("1", scalar("SELECT COUNT(*) FROM appointments WHERE doctor_id=" + fixture.doctor));
        assertEquals("1", scalar("SELECT COUNT(*) FROM balance_transactions WHERE patient_id IN (" + fixture.patient + "," + other + ")"));
        assertEquals("190.00", scalar("SELECT SUM(balance) FROM patients WHERE patient_id IN (" + fixture.patient + "," + other + ")"));
    }

    @Test public void masterDataQueriesEscapeLiteralWildcardsMapForeignKeysAndLockRows() throws Exception {
        Fixture fixture = fixture();
        int literal = insert("INSERT INTO patients(name,balance) VALUES('literal_%_" + UUID.randomUUID() + "',0)");
        AdminMasterDataService service = new AdminMasterDataService(() -> factory.openSession(false), () -> factory.openSession(true), new AuditService(() -> factory.openSession(true)));
        Map<String,Object> result = service.search("patients", "%", 1, 10);
        assertEquals(1L, ((Number)result.get("total")).longValue());
        assertEquals(literal, ((Patients)((List<?>)result.get("items")).get(0)).getPatientId());
        try (SqlSession session = factory.openSession(false)) {
            MasterDataMapper mapper = session.getMapper(MasterDataMapper.class);
            assertEquals(Integer.valueOf(fixture.doctor), mapper.lockEntity("doctors", fixture.doctor));
            List<Doctors> doctors = mapper.searchDoctors(null, 100, 0);
            Doctors doctor = null; for (Doctors item : doctors) if (item.getDoctorId() == fixture.doctor) doctor = item;
            assertNotNull(doctor); assertTrue(doctor.getDepartmentId() > 0); assertTrue(doctor.getProfessionalTitleId() > 0);
            assertFalse(mapper.searchDepartments(null, 1, 0).isEmpty()); assertFalse(mapper.searchTitles(null, 1, 0).isEmpty());
            assertNotEquals(mapper.searchPatients(null, 1, 0).get(0).getPatientId(), mapper.searchPatients(null, 1, 1).get(0).getPatientId());
            assertTrue(mapper.departmentOptions().size() >= mapper.searchDepartments(null, 1, 0).size());
            session.rollback();
        }
    }

    @Test public void cancelledHistoryKeepsSlotIdsDuringEditAndWithdrawal() throws Exception {
        Fixture fixture = fixture(); String date = LocalDate.parse(fixture.date).plusDays(1).toString();
        DatesServiceImpl schedules = new DatesServiceImpl(() -> factory.openSession(false), new AuditService(() -> factory.openSession(true)));
        assertTrue(schedules.addDate(fixture.doctor,"测试医生",date,"1","0","0").isSuccessful());
        int dateId = Integer.parseInt(scalar("SELECT date_id FROM dates WHERE doctor_id=" + fixture.doctor));
        String slot = scalar("SELECT schedule_slot_id FROM schedule_slots WHERE doctor_id=" + fixture.doctor + " AND schedule_date='" + date + "' AND slot_period='AM'");
        int appointment = insert("INSERT INTO appointments(patient_id,doctor_id,appointment_date,appointment_time,schedule_slot_id,status) VALUES(" + fixture.patient + "," + fixture.doctor + ",'" + date + "','AM'," + slot + ",'CANCELLED')");
        assertTrue(schedules.editForDoctor(fixture.doctor,dateId,date,"0","1","0").isSuccessful());
        assertEquals(slot, scalar("SELECT schedule_slot_id FROM appointments WHERE appointment_id=" + appointment));
        assertEquals("3", scalar("SELECT COUNT(*) FROM schedule_slots WHERE doctor_id=" + fixture.doctor + " AND schedule_date='" + date + "'"));
        assertTrue(schedules.deleteForDoctor(fixture.doctor,dateId).isSuccessful());
        assertEquals("1", scalar("SELECT COUNT(*) FROM dates WHERE date_id=" + dateId));
        assertEquals("3", scalar("SELECT COUNT(*) FROM schedule_slots WHERE doctor_id=" + fixture.doctor + " AND schedule_date='" + date + "' AND slot_status='CLOSED'"));
        assertEquals("ALREADY_CLOSED", schedules.deleteForDoctor(fixture.doctor,dateId).getCode());
    }

    @Test public void clinicalDraftFinalizationAndPatientPageAreAtomicAndIsolated() throws Exception {
        Fixture fixture = fixture();
        int appointment = insert("INSERT INTO appointments(patient_id,doctor_id,appointment_date,appointment_time,status) VALUES(" + fixture.patient + "," + fixture.doctor + ",'" + fixture.date + "','AM','IN_PROGRESS')");
        AuditContext doctor = new AuditContext("DOCTOR",fixture.doctor,"POST","/medical-records/draft","127.0.0.1");
        MedicalRecordsServiceImpl clinical = new MedicalRecordsServiceImpl(() -> factory.openSession(false), () -> factory.openSession(true), new AuditService(() -> factory.openSession(true)));
        MedicalOrder order = new MedicalOrder(); order.setItemName("测试医嘱"); order.setQuantity(1);
        assertTrue(clinical.saveDraft(fixture.doctor,appointment,"初诊","诊疗计划",null,Collections.singletonList(order),null,doctor).isSuccessful());
        MedicalRecordQuery query = new MedicalRecordQuery(fixture.date,fixture.date,String.valueOf(fixture.doctor),"1","10");
        assertEquals(0, clinical.searchForPatient(fixture.patient, query).getTotal());
        AuditService failing = new AuditService(() -> factory.openSession(true)) {
            @Override public void writeSuccess(SqlSession session, AuditContext context, String action, String type, String id) { throw new IllegalStateException("Injected audit failure"); }
        };
        MedicalRecordsServiceImpl failed = new MedicalRecordsServiceImpl(() -> factory.openSession(false), () -> factory.openSession(true), failing);
        assertFalse(failed.finalizeEncounter(fixture.doctor,appointment,"定稿","诊疗计划",null,Collections.<MedicalOrder>emptyList(),0,doctor).isSuccessful());
        assertEquals("DRAFT",scalar("SELECT record_status FROM medical_records WHERE appointment_id=" + appointment));
        assertEquals("IN_PROGRESS",scalar("SELECT status FROM appointments WHERE appointment_id=" + appointment));
        assertTrue(clinical.finalizeEncounter(fixture.doctor,appointment,"定稿","诊疗计划",null,Collections.singletonList(order),0,doctor).isSuccessful());
        MedicalRecordPage page = clinical.searchForPatient(fixture.patient, query);
        assertEquals(1, page.getTotal()); assertEquals(1, page.getItems().get(0).getOrders().size());
        assertEquals("COMPLETED",scalar("SELECT status FROM appointments WHERE appointment_id=" + appointment));
        assertEquals(0, clinical.searchForPatient(fixture.patient + 100000, query).getTotal());
    }

    private static String required(String key) { String value = System.getenv(key); if (value == null || value.trim().isEmpty()) throw new IllegalStateException("Missing isolated test configuration: " + key); return value; }
    private AppointmentsServiceImpl appointments() { return new AppointmentsServiceImpl(() -> factory.openSession(false), new AuditService(() -> factory.openSession(true))); }
    private AuditContext patient(int id) { return new AuditContext("PATIENT", id, "POST", "/appointments/book", "127.0.0.1"); }
    private Fixture fixture() throws Exception {
        String unique = UUID.randomUUID().toString();
        int department = insert("INSERT INTO departments(department_name) VALUES('测试科室" + unique + "')");
        int title = insert("INSERT INTO professional_titles(title_name) VALUES('测试职称" + unique + "')");
        int doctor = insert("INSERT INTO doctors(job_number,name,registration_fee,department_id,professional_title_id) VALUES('" + unique + "','测试医生',10," + department + "," + title + ")");
        int patient = insert("INSERT INTO patients(name,balance) VALUES('测试患者" + unique + "',100)");
        String date = LocalDate.now().plusDays(20).toString();
        insert("INSERT INTO schedule_slots(doctor_id,schedule_date,slot_period,slot_status) VALUES(" + doctor + ",'" + date + "','AM','AVAILABLE')");
        return new Fixture(doctor, patient, date);
    }
    private int insert(String sql) throws Exception {
        try (SqlSession session = factory.openSession(true); Statement query = session.getConnection().createStatement()) {
            query.executeUpdate(sql, Statement.RETURN_GENERATED_KEYS);
            try (ResultSet keys = query.getGeneratedKeys()) { assertTrue(keys.next()); return keys.getInt(1); }
        }
    }
    private String scalar(String sql) throws Exception {
        try (SqlSession session = factory.openSession(true); Statement query = session.getConnection().createStatement(); ResultSet result = query.executeQuery(sql)) { assertTrue(result.next()); return result.getString(1); }
    }
    private static final class Fixture { final int doctor, patient; final String date; Fixture(int doctor, int patient, String date) { this.doctor=doctor; this.patient=patient; this.date=date; } }
}
