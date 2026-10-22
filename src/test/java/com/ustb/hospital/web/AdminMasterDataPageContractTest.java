package com.ustb.hospital.web;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/** Keeps the administrative UI on the protected portal APIs rather than legacy write routes. */
public class AdminMasterDataPageContractTest {
    @Test
    public void pageUsesProtectedMasterDataEndpointsAndCsrf() throws Exception {
        String page = new String(Files.readAllBytes(Paths.get("src/main/webapp/hospro/admin-master-data.html")), StandardCharsets.UTF_8);
        assertTrue(page.contains("'/portal/admin'"));
        assertTrue(page.contains("endpoint:'doctors'"));
        assertTrue(page.contains("endpoint:'patients'"));
        assertTrue(page.contains("endpoint:'departments'"));
        assertTrue(page.contains("endpoint:'professional-titles'"));
        assertTrue(page.contains("/security/csrf"));
        assertTrue(page.contains("X-CSRF-Token"));
    }
}
