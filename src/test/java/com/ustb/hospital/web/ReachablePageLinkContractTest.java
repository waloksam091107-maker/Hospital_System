package com.ustb.hospital.web;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/** The user-facing template pages retain legacy markup, so they must load the guard that neutralises dead links. */
public class ReachablePageLinkContractTest {
    @Test
    public void patientEntryPagesLoadTheLegacyNavigationGuard() throws Exception {
        Path root = Paths.get("src", "main", "webapp", "hospro");
        for (String pageName : new String[]{"index.html", "doctor-directory.html", "book-appointment.html", "appointment-records.html"}) {
            String page = new String(Files.readAllBytes(root.resolve(pageName)), StandardCharsets.UTF_8);
            assertTrue(pageName + " 必须加载旧模板导航守卫", page.contains("static/js/legacy-navigation-guard.js"));
        }
        String guard = new String(Files.readAllBytes(root.resolve("static/js/legacy-navigation-guard.js")), StandardCharsets.UTF_8);
        assertTrue(guard.contains("normalizeLegacyTemplateLinks"));
        assertTrue(guard.contains("disablePlaceholderLink"));
        assertTrue(guard.contains("archive/index.html"));
    }
}
