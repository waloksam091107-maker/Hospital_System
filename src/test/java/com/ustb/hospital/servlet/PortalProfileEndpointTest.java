package com.ustb.hospital.servlet;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/** Endpoint contract for registration and self-service profile routes. */
public class PortalProfileEndpointTest {
    @Test
    public void safePortalEndpointsExistWithoutReenablingLegacyWrites() throws Exception {
        String registration = read("src/main/java/com/ustb/hospital/servlet/PortalPatientRegistrationServlet.java");
        String registrationCsrf = read("src/main/java/com/ustb/hospital/servlet/PortalRegistrationCsrfServlet.java");
        String profile = read("src/main/java/com/ustb/hospital/servlet/PortalProfileServlet.java");
        String avatar = read("src/main/java/com/ustb/hospital/servlet/PortalAvatarUploadServlet.java");
        String legacy = read("src/main/java/com/ustb/hospital/filter/LegacyUnsafeWritesDisabledFilter.java");

        assertTrue(registration.contains("@WebServlet(\"/portal/accounts/patient-register\")"));
        assertTrue(registrationCsrf.contains("@WebServlet(\"/portal/accounts/csrf\")"));
        assertTrue(registration.contains("CsrfTokenService.verify(request)"));
        assertTrue(profile.contains("@WebServlet(\"/portal/profile\")"));
        assertTrue(avatar.contains("@WebServlet(\"/portal/profile/avatar\")"));
        assertTrue(avatar.contains("@MultipartConfig"));
        assertTrue(avatar.contains("hasExpectedImageSignature"));
        assertTrue(profile.contains("SessionIdentity.getCurrentRole"));
        assertTrue(legacy.contains("\"/accounts/register\""));
        assertTrue(legacy.contains("\"/patients/profile\""));
    }

    private String read(String relativePath) throws Exception {
        return new String(Files.readAllBytes(Paths.get(relativePath)), StandardCharsets.UTF_8);
    }
}
