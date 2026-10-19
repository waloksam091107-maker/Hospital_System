package com.ustb.hospital.utils;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

public class SmsLocalCredentialsTest {
    @Rule public final TemporaryFolder temp = new TemporaryFolder();

    private String[] load(Path path) throws Exception {
        final Method method;
        try {
            method = SMSUtils.class.getDeclaredMethod("loadCredentials", Path.class);
        } catch (NoSuchMethodException e) {
            throw new AssertionError("SMS credentials must be loaded from local configuration", e);
        }
        method.setAccessible(true);
        return (String[]) method.invoke(null, path);
    }

    @Test public void readsBothCredentialsFromLocalConfiguration() throws Exception {
        Path path = temp.newFile("sms.properties").toPath();
        Files.write(path, "accessKeyId=test-id\naccessKeySecret=test-secret\n".getBytes(StandardCharsets.UTF_8));
        assertArrayEquals(new String[] {"test-id", "test-secret"}, load(path));
    }

    @Test public void refusesBlankCredentials() throws Exception {
        Path path = temp.newFile("empty.properties").toPath();
        Files.write(path, "accessKeyId=test-id\naccessKeySecret=\n".getBytes(StandardCharsets.UTF_8));
        try {
            load(path);
            fail("blank credentials must not be accepted");
        } catch (InvocationTargetException e) {
            assertTrue(e.getCause() instanceof IllegalStateException);
        }
    }

    @Test public void reportsMissingConfigurationWithoutUsingHardcodedSecrets() throws Exception {
        try {
            load(temp.getRoot().toPath().resolve("missing.properties"));
            fail("missing configuration must not be accepted");
        } catch (InvocationTargetException e) {
            assertTrue(e.getCause() instanceof IllegalStateException);
        }
    }

    @Test public void allowsTomcatToSpecifyAnAbsoluteConfigurationPath() throws Exception {
        Method method = SMSUtils.class.getDeclaredMethod("credentialsPath");
        method.setAccessible(true);
        String previous = System.getProperty("hospital.sms.config");
        Path expected = temp.getRoot().toPath().resolve("outside-webapp.properties");
        try {
            System.setProperty("hospital.sms.config", expected.toString());
            assertEquals(expected, method.invoke(null));
        } finally {
            if (previous == null) System.clearProperty("hospital.sms.config");
            else System.setProperty("hospital.sms.config", previous);
        }
    }
}
