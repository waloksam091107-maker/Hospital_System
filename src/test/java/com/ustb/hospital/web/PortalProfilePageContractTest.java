package com.ustb.hospital.web;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

public class PortalProfilePageContractTest {
    @Test
    public void profileAndRegistrationPagesUseTheNewSafePortalRoutes() throws Exception {
        String profile = read("src/main/webapp/hospro/portal-profile.html");
        String registration = read("src/main/webapp/hospro/register-page.html");

        assertTrue(profile.contains("/portal/profile"));
        assertTrue(profile.contains("/portal/profile/avatar"));
        assertTrue(profile.contains("/security/csrf"));
        assertTrue(registration.contains("/portal/accounts/patient-register"));
        assertTrue(registration.contains("/portal/accounts/csrf"));
        assertTrue(registration.contains("X-CSRF-Token"));
        assertTrue(registration.contains("患者注册"));
    }

    private String read(String relativePath) throws Exception {
        return new String(Files.readAllBytes(Paths.get(relativePath)), StandardCharsets.UTF_8);
    }
}
