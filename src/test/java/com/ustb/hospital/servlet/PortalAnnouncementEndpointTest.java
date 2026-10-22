package com.ustb.hospital.servlet;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/** Announcement writes must be restricted to the protected administrator portal. */
public class PortalAnnouncementEndpointTest {
    @Test
    public void administratorAnnouncementRouteIsProtectedAndAudited() throws Exception {
        String source = new String(Files.readAllBytes(Paths.get("src/main/java/com/ustb/hospital/servlet/PortalAdminAnnouncementsServlet.java")), StandardCharsets.UTF_8);
        assertTrue(source.contains("@WebServlet(\"/portal/admin/announcements\")"));
        assertTrue(source.contains("SessionIdentity.ROLE_ADMIN"));
        assertTrue(source.contains("AuditContext.fromRequest"));
        assertTrue(source.contains("\"publish\".equals(action)"));
        assertTrue(source.contains("\"offline\".equals(action)"));
    }
}
