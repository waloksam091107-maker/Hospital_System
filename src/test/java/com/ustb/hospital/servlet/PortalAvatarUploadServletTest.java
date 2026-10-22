package com.ustb.hospital.servlet;

import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PortalAvatarUploadServletTest {
    @Test public void onlyAcceptedImageSignaturesPassTheirMatchingFormat() throws Exception {
        Path png = Files.createTempFile("avatar", ".png");
        Path jpeg = Files.createTempFile("avatar", ".jpg");
        Path text = Files.createTempFile("avatar", ".png");
        try {
            Files.write(png, new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a});
            Files.write(jpeg, new byte[] {(byte) 0xff, (byte) 0xd8, (byte) 0xff, 0x00});
            Files.write(text, "not an image".getBytes("UTF-8"));
            assertTrue(PortalAvatarUploadServlet.hasExpectedImageSignature(png, ".png"));
            assertTrue(PortalAvatarUploadServlet.hasExpectedImageSignature(jpeg, ".jpg"));
            assertFalse(PortalAvatarUploadServlet.hasExpectedImageSignature(text, ".png"));
        } finally { Files.deleteIfExists(png); Files.deleteIfExists(jpeg); Files.deleteIfExists(text); }
    }
}
