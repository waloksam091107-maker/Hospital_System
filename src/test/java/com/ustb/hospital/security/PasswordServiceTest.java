package com.ustb.hospital.security;

import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class PasswordServiceTest {
    @Test
    public void hashesAndVerifiesPasswordsWithoutReturningThePlaintext() throws Exception {
        Class<?> service;
        try {
            service = Class.forName("com.ustb.hospital.security.PasswordService");
        } catch (ClassNotFoundException exception) {
            throw new AssertionError("PasswordService must exist", exception);
        }
        Method hash = service.getMethod("hash", String.class);
        Method matches = service.getMethod("matches", String.class, String.class, String.class);
        String encoded = (String) hash.invoke(null, "correct horse battery staple");
        assertNotNull(encoded);
        assertTrue("must use BCrypt", encoded.startsWith("$2"));
        assertFalse(encoded.contains("correct horse battery staple"));
        assertTrue((Boolean) matches.invoke(null, "correct horse battery staple", encoded, null));
        assertFalse((Boolean) matches.invoke(null, "wrong", encoded, null));
    }

    @Test
    public void permitsLegacyComparisonOnlyWhenNoHashExists() throws Exception {
        Class<?> service;
        try {
            service = Class.forName("com.ustb.hospital.security.PasswordService");
        } catch (ClassNotFoundException exception) {
            throw new AssertionError("PasswordService must exist", exception);
        }
        Method matches = service.getMethod("matches", String.class, String.class, String.class);
        Method needsUpgrade = service.getMethod("needsUpgrade", String.class);
        assertTrue((Boolean) matches.invoke(null, "old-password", null, "old-password"));
        assertFalse((Boolean) matches.invoke(null, "wrong", null, "old-password"));
        assertTrue((Boolean) needsUpgrade.invoke(null, (Object) null));
    }
}
