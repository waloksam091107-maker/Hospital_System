package com.ustb.hospital.security;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LegacyCredentialEndpointContractTest {
    @Test
    public void disablesLegacyEndpointsThatBypassSessionIdentityAndBcrypt() throws Exception {
        assertDisabled("src/main/java/com/ustb/hospital/servlet/AdminsLoginServlet.java");
        assertDisabled("src/main/java/com/ustb/hospital/servlet/PasswordChangeVerificationServlet.java");
        assertDisabled("src/main/java/com/ustb/hospital/servlet/PatientsPayServlet.java");
    }

    private static void assertDisabled(String file) throws Exception {
        Path path = Paths.get(file);
        String source = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        assertTrue(file + " must respond with Gone", source.contains("SC_GONE"));
        assertFalse(file + " must not compare a plaintext password", source.contains("getPassword().equals"));
    }
}
