package com.ustb.hospital.web;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/** The public page must be read-only and the administrator page must use the protected API. */
public class AnnouncementPageContractTest {
    @Test
    public void announcementPagesUseSeparatedReadAndWriteRoutes() throws Exception {
        String publicPage = read("news.html");
        String adminPage = read("admin-announcements.html");
        assertTrue(publicPage.contains("/announcements/list"));
        assertTrue(!publicPage.contains("/news/add"));
        assertTrue(adminPage.contains("/portal/admin/announcements"));
        assertTrue(adminPage.contains("/security/csrf"));
        assertTrue(adminPage.contains("X-CSRF-Token"));
    }
    private static String read(String name) throws Exception {
        return new String(Files.readAllBytes(Paths.get("src/main/webapp/hospro/" + name)), StandardCharsets.UTF_8);
    }
}
