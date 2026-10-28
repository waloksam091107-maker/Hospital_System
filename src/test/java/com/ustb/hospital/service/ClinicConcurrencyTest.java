package com.ustb.hospital.service;

import com.ustb.hospital.utils.AppointmentOperationResult;
import org.apache.ibatis.session.SqlSession;
import org.junit.Before;
import org.junit.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.junit.Assert.*;

/** Adversarial transactions against the disposable H2/MySQL-mode SQL fixture. */
public class ClinicConcurrencyTest {
    private ClinicWorkflowTest f;

    @Before public void setup() throws Exception {
        f = new ClinicWorkflowTest();
        f.setup();
    }

    @Test public void concurrentCallNextCommitsExactlyOneCalledPatientAndNotification() throws Exception {
        f.ok(f.book(1, f.day)); f.ok(f.book(2, f.day));
        f.ok(f.queue("check-in", "PATIENT", 1, 1));
        f.ok(f.queue("check-in", "PATIENT", 2, 2));
        List<Callable<AppointmentOperationResult>> calls = new ArrayList<>();
        for (int i = 0; i < 8; i++) calls.add(() -> f.queue("call-next", "DOCTOR", 1, 0));
        int success = 0;
        for (AppointmentOperationResult result : race(calls)) {
            if (result.isSuccessful()) success++;
            else assertEquals("a losing concurrent call must see the doctor busy", "DOCTOR_BUSY", result.getCode());
        }
        assertEquals(1, success);
        assertEquals("1", f.scalar("SELECT COUNT(*) FROM appointments WHERE queue_status='CALLED'"));
        assertEquals("1", f.scalar("SELECT COUNT(*) FROM appointments WHERE queue_status='WAITING'"));
        assertEquals("1", f.scalar("SELECT COUNT(*) FROM notifications WHERE notification_type='QUEUE_CALLED'"));
        assertEquals("1", f.scalar("SELECT COUNT(*) FROM queue_history WHERE to_status='CALLED'"));
    }

    @Test public void approveRacingBookingEitherRetainsAndNotifiesOrRejectsAfterFreeze() throws Exception {
        for (int iteration = 0; iteration < 10; iteration++) {
            if (iteration > 0) { f = new ClinicWorkflowTest(); f.setup(); }
            f.ok(f.book(1, f.day));
            f.ok(f.leave("apply", "DOCTOR", 1, 0, f.day, f.day, "AM", "医生请假"));
            List<Callable<AppointmentOperationResult>> work = new ArrayList<>();
            work.add(() -> f.leave("approve", "ADMIN", 1, 1, null, null, null, "同意"));
            work.add(() -> f.book(2, f.day));
            List<AppointmentOperationResult> outcomes = race(work);
            f.ok(outcomes.get(0));
            AppointmentOperationResult booking = outcomes.get(1);
            if (!booking.isSuccessful()) assertEquals("SLOT_UNAVAILABLE", booking.getCode());
            String expected = booking.isSuccessful() ? "2" : "1";
            assertEquals("APPROVED", f.scalar("SELECT status FROM clinic_leave WHERE leave_id=1"));
            assertEquals("BOOKED", f.scalar("SELECT status FROM appointments WHERE patient_id=1"));
            assertEquals(expected, f.scalar("SELECT COUNT(*) FROM appointments WHERE status='BOOKED'"));
            assertEquals(expected, f.scalar("SELECT occupied_count FROM schedule_slots WHERE schedule_slot_id=1"));
            assertEquals(expected, f.scalar("SELECT COUNT(*) FROM notifications WHERE notification_type='LEAVE_APPROVED' AND recipient_role='PATIENT'"));
            assertEquals("each retained appointment needs its own stop notice", "0", f.scalar("SELECT COUNT(*) FROM appointments a WHERE NOT EXISTS (SELECT 1 FROM notifications n WHERE n.recipient_role='PATIENT' AND n.recipient_id=a.patient_id AND n.notification_type='LEAVE_APPROVED' AND n.target_id=CAST(a.appointment_id AS VARCHAR))"));
            assertEquals(booking.isSuccessful() ? "90.00" : "100.00", f.scalar("SELECT balance FROM patients WHERE patient_id=2"));
            assertEquals("SLOT_UNAVAILABLE", f.book(3, f.day).getCode());
            assertEquals("100.00", f.scalar("SELECT balance FROM patients WHERE patient_id=3"));
            assertEquals(expected, f.scalar("SELECT COUNT(*) FROM appointments"));
        }
    }

    @Test public void capacityCannotDropBelowOccupancyOrBeEditedByAnotherDoctor() throws Exception {
        f.sql("INSERT INTO doctors VALUES(2,'其他医生',10)");
        f.ok(f.book(1, f.day)); f.ok(f.book(2, f.day));
        assertEquals("SCHEDULE_IN_USE", capacity(1, 1, 1).getCode());
        assertEquals("FORBIDDEN", capacity(2, 1, 5).getCode());
        assertEquals("INVALID_REQUEST", capacity(1, 1, 0).getCode());
        assertEquals("INVALID_REQUEST", capacity(1, 1, 1001).getCode());
        assertEquals("2", f.scalar("SELECT capacity FROM schedule_slots WHERE schedule_slot_id=1"));
        assertEquals("2", f.scalar("SELECT occupied_count FROM schedule_slots WHERE schedule_slot_id=1"));
        f.ok(capacity(1, 1, 2));
        f.ok(capacity(1, 1, 3));
        f.ok(f.book(3, f.day));
        assertEquals("3", f.scalar("SELECT occupied_count FROM schedule_slots WHERE schedule_slot_id=1"));
        assertEquals("3", f.scalar("SELECT COUNT(*) FROM appointments"));
    }

    @Test public void skippedPatientRejoinsTailEvenWhenClockAndCheckinTimesAreEqual() throws Exception {
        f.ok(f.book(1, f.day)); f.ok(f.book(2, f.day));
        f.ok(f.queue("check-in", "PATIENT", 1, 1)); f.ok(f.queue("check-in", "PATIENT", 2, 2));
        assertEquals("1", f.scalar("SELECT COUNT(DISTINCT checked_in_at) FROM appointments"));
        f.ok(f.queue("call-next", "DOCTOR", 1, 0));
        assertEquals("CALLED", f.scalar("SELECT queue_status FROM appointments WHERE appointment_id=1"));
        f.ok(f.queue("skip", "DOCTOR", 1, 1));
        f.ok(f.queue("requeue", "DOCTOR", 1, 1));
        f.ok(f.queue("call-next", "DOCTOR", 1, 0));
        assertEquals("already waiting patient must be called before the restored patient", "CALLED", f.scalar("SELECT queue_status FROM appointments WHERE appointment_id=2"));
        assertEquals("WAITING", f.scalar("SELECT queue_status FROM appointments WHERE appointment_id=1"));
        assertEquals("1", f.scalar("SELECT appointment_sequence FROM appointments WHERE appointment_id=1"));
        assertEquals("2", f.scalar("SELECT appointment_sequence FROM appointments WHERE appointment_id=2"));
    }

    @Test public void noShowBeginsAtExactPeriodEndAndNeverBlamesWaitingPatients() throws Exception {
        f.ok(capacity(1, 1, 3));
        f.ok(f.book(1, f.day)); f.ok(f.book(2, f.day)); f.ok(f.book(3, f.day));
        f.ok(f.queue("check-in", "PATIENT", 2, 2));
        f.ok(f.queue("call-next", "DOCTOR", 1, 0));
        f.ok(f.queue("skip", "DOCTOR", 1, 2));
        f.ok(f.queue("check-in", "PATIENT", 3, 3));
        at("2030-05-06T03:59:59.999Z");
        assertEquals("INVALID_STATE", f.queue("no-show", "DOCTOR", 1, 1).getCode());
        assertEquals("INVALID_STATE", f.queue("no-show", "DOCTOR", 1, 2).getCode());
        at("2030-05-06T04:00:00Z");
        f.ok(f.queue("no-show", "DOCTOR", 1, 1));
        f.ok(f.queue("no-show", "DOCTOR", 1, 2));
        assertEquals("INVALID_STATE", f.queue("no-show", "DOCTOR", 1, 3).getCode());
        assertEquals("WAITING", f.scalar("SELECT queue_status FROM appointments WHERE appointment_id=3"));
        assertEquals("BOOKED", f.scalar("SELECT status FROM appointments WHERE appointment_id=3"));
        assertEquals("3", f.scalar("SELECT occupied_count FROM schedule_slots WHERE schedule_slot_id=1"));
        assertEquals("270.00", f.scalar("SELECT SUM(balance) FROM patients"));
        assertEquals("2", f.scalar("SELECT COUNT(*) FROM notifications WHERE notification_type='QUEUE_NO_SHOW'"));
        assertEquals("INVALID_STATE", f.queue("no-show", "DOCTOR", 1, 1).getCode());
        assertEquals("2", f.scalar("SELECT COUNT(*) FROM queue_history WHERE to_status='NO_SHOW'"));
    }

    @Test public void stoppedClinicPatientCannotBeMarkedNoShowAfterPeriodEnd() throws Exception {
        f.ok(f.book(1, f.day));
        f.ok(f.leave("apply", "DOCTOR", 1, 0, f.day, f.day, "AM", "医生请假"));
        f.ok(f.leave("approve", "ADMIN", 1, 1, null, null, null, "同意"));
        at("2030-05-06T04:00:00Z");
        assertFalse("clinic cancellation must not be recorded as patient absence", f.queue("no-show", "DOCTOR", 1, 1).isSuccessful());
        assertEquals("BOOKED", f.scalar("SELECT status FROM appointments WHERE appointment_id=1"));
        assertEquals("0", f.scalar("SELECT COUNT(*) FROM notifications WHERE notification_type='QUEUE_NO_SHOW'"));
    }

    @Test public void doctorCanWithdrawOwnPendingLeaveWithoutAnExtraReviewReason() throws Exception {
        f.ok(f.leave("apply", "DOCTOR", 1, 0, f.day, f.day, "AM", "申请原因已填写"));
        f.ok(f.leave("withdraw", "DOCTOR", 1, 1, null, null, null, ""));
        assertEquals("WITHDRAWN", f.scalar("SELECT status FROM clinic_leave WHERE leave_id=1"));
        assertEquals("0", f.scalar("SELECT COUNT(*) FROM notifications WHERE notification_type='LEAVE_APPROVED'"));
        f.ok(f.book(1, f.day));
    }

    @Test public void requeueUsesStableOrderEvenWhenDatabaseStoresWholeSeconds() throws Exception {
        f.sql("ALTER TABLE appointments ALTER COLUMN checked_in_at TIMESTAMP(0)");
        skippedPatientRejoinsTailEvenWhenClockAndCheckinTimesAreEqual();
    }

    @Test public void concurrentReschedulesCreateOneReplacementAndChargeOnlyOnce() throws Exception {
        f.ok(f.book(1,f.day));
        f.sql("INSERT INTO schedule_slots(doctor_id,schedule_date,slot_period,slot_status,capacity) VALUES(1,'2030-05-08','AM','AVAILABLE',1)");
        List<Callable<AppointmentOperationResult>> work=new ArrayList<>();
        work.add(()->f.action("reschedule",1,1,"2030-05-07","AM"));
        work.add(()->f.action("reschedule",1,1,"2030-05-08","AM"));
        int success=0;for(AppointmentOperationResult r:race(work))if(r.isSuccessful())success++;
        assertEquals(1,success);
        assertEquals("2",f.scalar("SELECT COUNT(*) FROM appointments"));
        assertEquals("RESCHEDULED",f.scalar("SELECT status FROM appointments WHERE appointment_id=1"));
        assertEquals("0",f.scalar("SELECT occupied_count FROM schedule_slots WHERE schedule_slot_id=1"));
        assertEquals("1",f.scalar("SELECT SUM(occupied_count) FROM schedule_slots"));
        assertEquals("90.00",f.scalar("SELECT balance FROM patients WHERE patient_id=1"));
        assertEquals("1",f.scalar("SELECT COUNT(*) FROM balance_transactions"));
    }

    @Test public void cancellationRacingStartHasOneConsistentTerminalOutcome() throws Exception {
        f.ok(f.book(1,f.day));f.ok(f.queue("check-in","PATIENT",1,1));f.ok(f.queue("call-next","DOCTOR",1,0));
        List<Callable<AppointmentOperationResult>> work=new ArrayList<>();
        work.add(()->f.action("cancel",1,1,null,null));work.add(()->f.queue("start","DOCTOR",1,1));
        int success=0;for(AppointmentOperationResult r:race(work))if(r.isSuccessful())success++;
        assertEquals(1,success);
        String status=f.scalar("SELECT status FROM appointments WHERE appointment_id=1");
        assertTrue(status.equals("CANCELLED")||status.equals("IN_PROGRESS"));
        assertEquals(status.equals("CANCELLED")?"0":"1",f.scalar("SELECT occupied_count FROM schedule_slots WHERE schedule_slot_id=1"));
        assertEquals(status.equals("CANCELLED")?"REMOVED":"IN_PROGRESS",f.scalar("SELECT queue_status FROM appointments WHERE appointment_id=1"));
    }

    private AppointmentOperationResult capacity(int doctor, long slot, int capacity) throws Exception {
        return (AppointmentOperationResult) f.service.getClass().getMethod("capacity", int.class, long.class, int.class, AuditContext.class)
                .invoke(f.service, doctor, slot, capacity, f.audit("DOCTOR", doctor));
    }

    private void at(String instant) throws Exception {
        f.service = f.service.getClass().getConstructor(Supplier.class, AuditService.class, Clock.class)
                .newInstance((Supplier<SqlSession>) () -> f.factory.openSession(false), new AuditService(),
                        Clock.fixed(Instant.parse(instant), ZoneId.of("Asia/Shanghai")));
    }

    private List<AppointmentOperationResult> race(List<Callable<AppointmentOperationResult>> calls) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(calls.size());
        CountDownLatch ready = new CountDownLatch(calls.size()), start = new CountDownLatch(1);
        try {
            List<Future<AppointmentOperationResult>> futures = new ArrayList<>();
            for (Callable<AppointmentOperationResult> call : calls) futures.add(pool.submit(() -> {
                ready.countDown();
                if (!start.await(10, TimeUnit.SECONDS)) throw new AssertionError("race start timed out");
                return call.call();
            }));
            assertTrue("all racing transactions must reach the start barrier", ready.await(10, TimeUnit.SECONDS));
            start.countDown();
            List<AppointmentOperationResult> result = new ArrayList<>();
            for (Future<AppointmentOperationResult> future : futures) result.add(future.get(20, TimeUnit.SECONDS));
            return result;
        } finally { start.countDown(); pool.shutdownNow(); }
    }
}
