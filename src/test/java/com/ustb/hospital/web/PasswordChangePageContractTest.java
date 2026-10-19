package com.ustb.hospital.web;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

public class PasswordChangePageContractTest {
    @Test
    public void submitsCurrentAndNewPasswordToTheProtectedResetEndpoint() throws Exception {
        Path page = Paths.get("src/main/webapp/hospro/change-password.html");
        String html = new String(Files.readAllBytes(page), StandardCharsets.UTF_8);
        assertTrue(html.contains("../password/reset"));
        assertTrue(html.contains("name=\"currentPassword\""));
        assertTrue(html.contains("name=\"newPassword\""));
        assertTrue(html.contains("X-CSRF-Token"));
    }
}
