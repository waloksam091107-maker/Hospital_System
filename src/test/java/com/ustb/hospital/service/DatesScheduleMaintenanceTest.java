package com.ustb.hospital.service;

import com.ustb.hospital.mapper.ScheduleSlotsMapper;
import com.ustb.hospital.utils.AppointmentOperationResult;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.*;
import org.junit.Before;
import org.junit.Test;
import java.io.InputStream;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.Assert.*;

/** Disposable database tests: real mapper SQL, foreign keys and independent transactions. */
public class DatesScheduleMaintenanceTest {
    private SqlSessionFactory factory;
    private DatesServiceImpl service;
    private final String future = LocalDate.now().plusDays(10).toString();
    private final AuditContext audit = new AuditContext("DOCTOR", 1, "POST", "/schedules/update", "127.0.0.1");

    @Before public void setup() throws Exception {
        Properties p = new Properties();
        p.setProperty("db.driver", "org.h2.Driver");
        p.setProperty("db.url", "jdbc:h2:mem:schedule_" + UUID.randomUUID().toString().replace("-", "") + ";MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000");
        p.setProperty("db.username", "sa"); p.setProperty("db.password", "");
        try (InputStream in = Resources.getResourceAsStream("mybatis-config.xml")) { factory = new SqlSessionFactoryBuilder().build(in, p); }
        sql("CREATE TABLE dates(date_id INT AUTO_INCREMENT PRIMARY KEY, doctor_id INT, doctor_name VARCHAR(80), date VARCHAR(10), shang VARCHAR(1), xia VARCHAR(1), wan VARCHAR(1), UNIQUE(doctor_id,date))");
        sql("CREATE TABLE schedule_slots(schedule_slot_id BIGINT AUTO_INCREMENT PRIMARY KEY, doctor_id INT, schedule_date VARCHAR(10), slot_period VARCHAR(10), slot_status VARCHAR(20), created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, UNIQUE(doctor_id,schedule_date,slot_period))");
        sql("CREATE TABLE appointments(appointment_id INT AUTO_INCREMENT PRIMARY KEY, doctor_id INT, patient_id INT, appointment_date VARCHAR(10), appointment_time VARCHAR(10), schedule_slot_id BIGINT REFERENCES schedule_slots(schedule_slot_id), status VARCHAR(20))");
        sql("CREATE TABLE audit_logs(audit_log_id BIGINT AUTO_INCREMENT PRIMARY KEY, actor_role VARCHAR(20), actor_id INT, action VARCHAR(80), target_type VARCHAR(80), target_id VARCHAR(80), outcome VARCHAR(20), request_method VARCHAR(10), request_path VARCHAR(255), remote_address VARCHAR(64), created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        service = new DatesServiceImpl(() -> factory.openSession(false), new AuditService(() -> factory.openSession(true)));
        assertTrue(service.addDate(1, "医生", future, "1", "1", "0", audit).isSuccessful());
    }

    @Test public void cancelledAndRescheduledHistoryAllowInPlaceAdjustment() throws Exception {
        history("CANCELLED", "AM"); history("RESCHEDULED", "PM");
        String original = scalar("SELECT schedule_slot_id FROM schedule_slots WHERE slot_period='AM'");
        assertTrue(edit(future, "0", "1", "1").isSuccessful());
        assertEquals(original, scalar("SELECT schedule_slot_id FROM schedule_slots WHERE slot_period='AM'"));
        assertEquals("CLOSED", scalar("SELECT slot_status FROM schedule_slots WHERE slot_period='AM'"));
        assertEquals("AVAILABLE", scalar("SELECT slot_status FROM schedule_slots WHERE slot_period='EVENING'"));
        assertEquals("2", scalar("SELECT COUNT(*) FROM appointments"));
    }
    @Test public void occupiedPeriodCannotBeClosedButOtherPeriodsCanChange() throws Exception {
        history("BOOKED", "AM");
        assertEquals("SCHEDULE_IN_USE", edit(future, "0", "1", "0").getCode());
        assertTrue(edit(future, "1", "0", "1").isSuccessful());
        assertEquals("BOOKED", scalar("SELECT slot_status FROM schedule_slots WHERE slot_period='AM'"));
        assertEquals("CLOSED", scalar("SELECT slot_status FROM schedule_slots WHERE slot_period='PM'"));
    }
    @Test public void completedAndInProgressPeriodsRemainImmutable() throws Exception {
        history("COMPLETED", "AM"); history("IN_PROGRESS", "PM");
        assertEquals("SCHEDULE_IN_USE", edit(future, "0", "1", "0").getCode());
        assertEquals("SCHEDULE_IN_USE", edit(future, "1", "0", "0").getCode());
        assertTrue(edit(future, "1", "1", "1").isSuccessful());
    }
    @Test public void anyHistoryPreventsMovingDate() throws Exception {
        history("CANCELLED", "AM");
        assertEquals("HISTORY_DATE_LOCKED", edit(LocalDate.now().plusDays(11).toString(), "1", "1", "0").getCode());
        assertEquals(future, scalar("SELECT date FROM dates"));
    }
    @Test public void noHistoryAllowsMovingDateWithoutRebuildingSlots() throws Exception {
        String original = scalar("SELECT schedule_slot_id FROM schedule_slots WHERE slot_period='AM'");
        String next = LocalDate.now().plusDays(11).toString();
        assertTrue(edit(next, "0", "1", "1").isSuccessful());
        assertEquals(next, scalar("SELECT schedule_date FROM schedule_slots WHERE schedule_slot_id=" + original));
        assertEquals("3", scalar("SELECT COUNT(*) FROM schedule_slots"));
    }
    @Test public void historicalWithdrawalClosesSourcesAndRetainsReferences() throws Exception {
        history("CANCELLED", "AM");
        AppointmentOperationResult result = service.deleteForDoctor(1, 1, audit);
        assertTrue(result.getMessage(), result.isSuccessful());
        assertTrue(result.getMessage().contains("历史记录保留"));
        assertEquals("1", scalar("SELECT COUNT(*) FROM dates"));
        assertEquals("3", scalar("SELECT COUNT(*) FROM schedule_slots WHERE slot_status='CLOSED'"));
        assertEquals("1", scalar("SELECT COUNT(*) FROM appointments"));
        assertEquals("ALREADY_CLOSED", service.deleteForDoctor(1, 1, audit).getCode());
    }
    @Test public void completedHistoryAllowsWithdrawalButKeepsItsOccupiedSlot() throws Exception {
        history("COMPLETED", "AM");
        assertTrue(service.deleteForDoctor(1, 1, audit).isSuccessful());
        assertEquals("BOOKED", scalar("SELECT slot_status FROM schedule_slots WHERE slot_period='AM'"));
        assertEquals("0", scalar("SELECT shang FROM dates"));
        assertEquals("2", scalar("SELECT COUNT(*) FROM schedule_slots WHERE slot_status='CLOSED'"));
    }
    @Test public void activeAppointmentRejectsWithdrawal() throws Exception {
        history("IN_PROGRESS", "AM");
        assertEquals("SCHEDULE_IN_USE", service.deleteForDoctor(1, 1, audit).getCode());
        assertEquals("AVAILABLE", scalar("SELECT slot_status FROM schedule_slots WHERE slot_period='PM'"));
    }
    @Test public void noHistoryWithdrawalPhysicallyDeletesSchedule() throws Exception {
        assertTrue(service.deleteForDoctor(1, 1, audit).isSuccessful());
        assertEquals("0", scalar("SELECT COUNT(*) FROM dates")); assertEquals("0", scalar("SELECT COUNT(*) FROM schedule_slots"));
        assertEquals("NOT_FOUND", service.deleteForDoctor(1, 1, audit).getCode());
    }
    @Test public void otherDoctorCannotChangeOrWithdrawSchedule() throws Exception {
        assertEquals("FORBIDDEN", service.editForDoctor(2, 1, future, "0", "0", "0", audit).getCode());
        assertEquals("FORBIDDEN", service.deleteForDoctor(2, 1, audit).getCode());
        assertEquals("1", scalar("SELECT shang FROM dates"));
    }
    @Test public void impossibleAndPastDatesAreRejectedForCreateAndEdit() {
        for (String date : Arrays.asList("2030-02-30", "2030-13-01", "2030-2-01")) {
            assertEquals("INVALID_DATE", service.addDate(1, "医生", date, "1", "1", "1", audit).getCode());
            assertEquals("INVALID_DATE", edit(date, "1", "1", "0").getCode());
        }
        String past = LocalDate.now().minusDays(1).toString();
        assertEquals("PAST_DATE", service.addDate(1, "医生", past, "1", "1", "1", audit).getCode());
        assertEquals("PAST_DATE", edit(past, "1", "1", "0").getCode());
    }
    @Test public void pastScheduleCannotBeModifiedMovedOrWithdrawn() throws Exception {
        String past = LocalDate.now().minusDays(1).toString();
        sql("UPDATE dates SET date='" + past + "'"); sql("UPDATE schedule_slots SET schedule_date='" + past + "'");
        assertEquals("PAST_DATE", edit(past, "0", "0", "0").getCode());
        assertEquals("PAST_DATE", edit(future, "1", "1", "0").getCode());
        assertEquals("PAST_DATE", service.deleteForDoctor(1, 1, audit).getCode());
    }
    @Test public void auditFailureRollsBackHistoricalEditAndWithdrawal() throws Exception {
        history("CANCELLED", "AM"); sql("DROP TABLE audit_logs");
        assertEquals("SCHEDULE_UPDATE_FAILED", edit(future, "0", "1", "1").getCode());
        assertEquals("AVAILABLE", scalar("SELECT slot_status FROM schedule_slots WHERE slot_period='AM'"));
        assertEquals("1", scalar("SELECT shang FROM dates"));
        assertEquals("SCHEDULE_DELETE_FAILED", service.deleteForDoctor(1, 1, audit).getCode());
        assertEquals("AVAILABLE", scalar("SELECT slot_status FROM schedule_slots WHERE slot_period='AM'"));
    }
    @Test public void auditFailureRollsBackPhysicalDeletionAndDateMove() throws Exception {
        sql("DROP TABLE audit_logs");
        assertEquals("SCHEDULE_DELETE_FAILED", service.deleteForDoctor(1, 1, audit).getCode());
        assertEquals("1", scalar("SELECT COUNT(*) FROM dates")); assertEquals("3", scalar("SELECT COUNT(*) FROM schedule_slots"));
        assertEquals("SCHEDULE_UPDATE_FAILED", edit(LocalDate.now().plusDays(11).toString(), "0", "0", "1").getCode());
        assertEquals(future, scalar("SELECT date FROM dates"));
        assertEquals(future, scalar("SELECT schedule_date FROM schedule_slots WHERE slot_period='AM'"));
    }
    @Test public void todayIsAllowedButDuplicateDateCannotMoveAnotherSchedule() {
        String today = LocalDate.now().toString();
        assertTrue(service.addDate(1, "医生", today, "1", "1", "0", audit).isSuccessful());
        assertEquals("DUPLICATE_DATE", edit(today, "1", "1", "0").getCode());
        assertTrue(service.editForDoctor(1, 2, today, "0", "0", "0", audit).isSuccessful());
    }
    @Test public void bookingThatOwnsSlotLockWinsBeforeWithdrawal() throws Exception {
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try (SqlSession booking = factory.openSession(false)) {
            assertEquals(1, booking.getMapper(ScheduleSlotsMapper.class).reserveAvailableSlot(1, future, "AM"));
            CountDownLatch entered = new CountDownLatch(1);
            Future<AppointmentOperationResult> withdraw = pool.submit(() -> { entered.countDown(); return service.deleteForDoctor(1, 1, audit); });
            assertTrue(entered.await(2, TimeUnit.SECONDS));
            try { withdraw.get(200, TimeUnit.MILLISECONDS); fail("withdrawal must wait for the booking slot lock"); } catch (TimeoutException expected) { }
            try (Statement s = booking.getConnection().createStatement()) {
                s.execute("INSERT INTO appointments(doctor_id,patient_id,appointment_date,appointment_time,schedule_slot_id,status) SELECT 1,1,schedule_date,slot_period,schedule_slot_id,'BOOKED' FROM schedule_slots WHERE slot_period='AM'");
            }
            booking.commit();
            assertEquals("SCHEDULE_IN_USE", withdraw.get(5, TimeUnit.SECONDS).getCode());
            assertEquals("BOOKED", scalar("SELECT slot_status FROM schedule_slots WHERE slot_period='AM'"));
        } finally { pool.shutdownNow(); }
    }
    @Test public void withdrawalLockPreventsConcurrentBookingFromReopeningClosedSource() throws Exception {
        history("CANCELLED", "AM");
        CountDownLatch changed = new CountDownLatch(1), finish = new CountDownLatch(1);
        AuditService gatedAudit = new AuditService(() -> factory.openSession(true)) {
            public void writeSuccess(SqlSession session, AuditContext context, String action, String type, String id) {
                changed.countDown();
                try { if (!finish.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("test timeout"); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException(e); }
                super.writeSuccess(session, context, action, type, id);
            }
        };
        DatesServiceImpl gated = new DatesServiceImpl(() -> factory.openSession(false), gatedAudit);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<AppointmentOperationResult> withdraw = pool.submit(() -> gated.deleteForDoctor(1, 1, audit));
            assertTrue("withdrawal must reach its transactional audit", changed.await(2, TimeUnit.SECONDS));
            Future<Integer> booking = pool.submit(() -> { try (SqlSession s = factory.openSession(false)) { int count = s.getMapper(ScheduleSlotsMapper.class).reserveAvailableSlot(1, future, "AM"); s.commit(); return count; } });
            try { booking.get(200, TimeUnit.MILLISECONDS); fail("booking must wait for withdrawal"); } catch (TimeoutException expected) { }
            finish.countDown(); assertTrue(withdraw.get(5, TimeUnit.SECONDS).isSuccessful()); assertEquals(Integer.valueOf(0), booking.get(5, TimeUnit.SECONDS));
            assertEquals("CLOSED", scalar("SELECT slot_status FROM schedule_slots WHERE slot_period='AM'"));
        } finally { finish.countDown(); pool.shutdownNow(); }
    }
    private AppointmentOperationResult edit(String date, String am, String pm, String evening) { return service.editForDoctor(1, 1, date, am, pm, evening, audit); }
    private void history(String status, String period) throws Exception {
        sql("INSERT INTO appointments(doctor_id,patient_id,appointment_date,appointment_time,schedule_slot_id,status) SELECT 1,1,schedule_date,slot_period,schedule_slot_id,'" + status + "' FROM schedule_slots WHERE slot_period='" + period + "'");
        if (Arrays.asList("BOOKED", "IN_PROGRESS", "COMPLETED").contains(status)) sql("UPDATE schedule_slots SET slot_status='BOOKED' WHERE slot_period='" + period + "'");
    }
    private void sql(String sql) throws Exception { try (SqlSession s = factory.openSession(true); Statement st = s.getConnection().createStatement()) { st.execute(sql); } }
    private String scalar(String sql) throws Exception { try (SqlSession s = factory.openSession(true); Statement st = s.getConnection().createStatement(); ResultSet r = st.executeQuery(sql)) { assertTrue(r.next()); return r.getString(1); } }
}
