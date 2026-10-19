package com.ustb.hospital.service;

import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class AppointmentStatusTest {
    @Test
    public void patientMutationFailureExplainsTerminalAppointmentState() throws Exception {
        Method message;
        try {
            message = AppointmentStatus.class.getMethod("patientMutationFailureMessage", String.class);
        } catch (NoSuchMethodException e) {
            throw new AssertionError("患者操作遇到已取消或已改期状态时应返回明确原因", e);
        }
        assertNull(message.invoke(null, "BOOKED"));
        assertTrue(((String) message.invoke(null, "RESCHEDULED")).contains("已改期"));
        assertTrue(((String) message.invoke(null, "CANCELLED")).contains("已取消"));
        assertEquals("当前预约状态不允许此操作。", message.invoke(null, "IN_PROGRESS"));
    }
    @Test
    public void onlyBookedAppointmentsMayBeCancelledRescheduledOrStarted() throws Exception {
        Class<?> statusType = loadStatusType();
        assertTrue(invoke(statusType, "canCancel", "BOOKED"));
        assertTrue(invoke(statusType, "canReschedule", "BOOKED"));
        assertTrue(invoke(statusType, "canStart", "BOOKED"));
        assertFalse(invoke(statusType, "canCancel", "IN_PROGRESS"));
        assertFalse(invoke(statusType, "canReschedule", "COMPLETED"));
        assertFalse(invoke(statusType, "canStart", "CANCELLED"));
    }

    @Test
    public void onlyTerminalReleasedStatesFreeTheirSlot() throws Exception {
        Class<?> statusType = loadStatusType();
        assertTrue(invoke(statusType, "keepsSlotOccupied", "BOOKED"));
        assertTrue(invoke(statusType, "keepsSlotOccupied", "IN_PROGRESS"));
        assertTrue(invoke(statusType, "keepsSlotOccupied", "COMPLETED"));
        assertFalse(invoke(statusType, "keepsSlotOccupied", "CANCELLED"));
        assertFalse(invoke(statusType, "keepsSlotOccupied", "RESCHEDULED"));
    }

    private static Class<?> loadStatusType() {
        try {
            Class<?> statusType = Class.forName("com.ustb.hospital.service.AppointmentStatus");
            assertNotNull(statusType);
            return statusType;
        } catch (ClassNotFoundException exception) {
            throw new AssertionError("AppointmentStatus must define the appointment lifecycle", exception);
        }
    }

    private static boolean invoke(Class<?> statusType, String methodName, String status) throws Exception {
        Method method = statusType.getMethod(methodName, String.class);
        return ((Boolean) method.invoke(null, status)).booleanValue();
    }
}
