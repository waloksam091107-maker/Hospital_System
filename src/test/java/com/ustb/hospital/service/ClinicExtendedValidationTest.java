package com.ustb.hospital.service;

import org.junit.*;
import static org.junit.Assert.*;

/** Additional acceptance boundaries found while retesting all five extensions. */
public class ClinicExtendedValidationTest {
    private ClinicWorkflowTest fixture;
    @Before public void setup()throws Exception{fixture=new ClinicWorkflowTest();fixture.setup();}

    @Test public void delimiterOnlyLeavePeriodsMustNotCreateAnUnusableRequest()throws Exception{
        assertEquals("INVALID_REQUEST",fixture.leave("apply","DOCTOR",1,0,fixture.day,fixture.day,",","请假").getCode());
        assertEquals("0",fixture.scalar("SELECT COUNT(*) FROM clinic_leave"));
        assertEquals("0",fixture.scalar("SELECT COUNT(*) FROM notifications"));
    }
    @Test public void trailingEmptyPeriodMustNotBeSilentlyDropped()throws Exception{
        assertEquals("INVALID_REQUEST",fixture.leave("apply","DOCTOR",1,0,fixture.day,fixture.day,"AM,,","请假").getCode());
        assertEquals("0",fixture.scalar("SELECT COUNT(*) FROM clinic_leave"));
    }
    @Test public void rejectedAndWithdrawnRequestsDoNotFreezeSlots()throws Exception{
        fixture.ok(fixture.leave("apply","DOCTOR",1,0,fixture.day,fixture.day,"AM","请假"));
        fixture.ok(fixture.leave("reject","ADMIN",1,1,null,null,null,"正常接诊"));
        fixture.ok(fixture.book(1,fixture.day));
        fixture.ok(fixture.leave("apply","DOCTOR",1,0,fixture.day,fixture.day,"AM","重新申请"));
        fixture.ok(fixture.leave("withdraw","DOCTOR",1,2,null,null,null,""));
        fixture.ok(fixture.book(2,fixture.day));
        assertEquals("2",fixture.scalar("SELECT occupied_count FROM schedule_slots WHERE schedule_slot_id=1"));
    }
    @Test public void overlappingPendingLeaveAndUnauthorisedReviewAreRejected()throws Exception{
        fixture.ok(fixture.leave("apply","DOCTOR",1,0,fixture.day,fixture.day,"AM,PM","请假"));
        assertEquals("OVERLAPPING_LEAVE",fixture.leave("apply","DOCTOR",1,0,fixture.day,fixture.day,"PM","请假").getCode());
        assertEquals("FORBIDDEN",fixture.leave("approve","DOCTOR",1,1,null,null,null,"自己审批").getCode());
        assertEquals("PENDING",fixture.scalar("SELECT status FROM clinic_leave"));
    }
}
