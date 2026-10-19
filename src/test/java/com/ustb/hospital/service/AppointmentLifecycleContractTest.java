package com.ustb.hospital.service;

import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.assertNotNull;

public class AppointmentLifecycleContractTest {
    @Test
    public void exposesDoctorStartTransition() throws Exception {
        Method method = AppointmentsServiceImpl.class.getMethod("startAppointment", int.class, int.class);
        assertNotNull(method);
    }
}
