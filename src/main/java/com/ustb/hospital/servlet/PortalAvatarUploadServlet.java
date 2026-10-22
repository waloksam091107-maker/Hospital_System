package com.ustb.hospital.servlet;

import com.ustb.hospital.service.AuditContext;
import com.ustb.hospital.service.PortalAccountService;
import com.ustb.hospital.utils.AppointmentOperationResult;
import com.ustb.hospital.utils.SessionIdentity;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@WebServlet("/portal/profile/avatar")
@MultipartConfig(maxFileSize = 2 * 1024 * 1024, maxRequestSize = 3 * 1024 * 1024)
public class PortalAvatarUploadServlet extends HttpServlet {
    private final PortalAccountService accounts;
    public PortalAvatarUploadServlet() { this(new PortalAccountService()); }
    PortalAvatarUploadServlet(PortalAccountService accounts) { this.accounts = accounts; }

    @Override protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Integer userId = PortalServletSupport.requireAuthenticated(request, response);
        if (userId == null) return;
        Part part;
        try { part = request.getPart("avatar"); }
        catch (IllegalStateException exception) { PortalServletSupport.writeJsonError(response, HttpServletResponse.SC_BAD_REQUEST, "INVALID_AVATAR", "头像文件不能超过 2MB。"); return; }
        if (part == null || part.getSize() <= 0 || part.getSize() > 2 * 1024 * 1024) { PortalServletSupport.writeJsonError(response, HttpServletResponse.SC_BAD_REQUEST, "INVALID_AVATAR", "请选择不超过 2MB 的头像文件。"); return; }
        String extension = extensionFor(part.getContentType());
        if (extension == null) { PortalServletSupport.writeJsonError(response, HttpServletResponse.SC_BAD_REQUEST, "INVALID_AVATAR", "头像仅支持 PNG、JPEG 或 WEBP 图片。"); return; }

        Path directory = avatarDirectory();
        Files.createDirectories(directory);
        String role = SessionIdentity.getCurrentRole(request).toLowerCase();
        Path destination = directory.resolve(role + "-" + userId + "-" + UUID.randomUUID().toString().replace("-", "") + extension);
        Files.copy(part.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
        if (!hasExpectedImageSignature(destination, extension)) {
            Files.deleteIfExists(destination);
            PortalServletSupport.writeJsonError(response, HttpServletResponse.SC_BAD_REQUEST, "INVALID_AVATAR", "头像文件内容不是支持的图片格式。");
            return;
        }
        String publicPath = "uploads/avatars/" + destination.getFileName().toString();
        AppointmentOperationResult result = accounts.updateOwnAvatar(SessionIdentity.getCurrentRole(request), userId.intValue(), publicPath, AuditContext.fromRequest(request));
        if (!result.isSuccessful()) Files.deleteIfExists(destination);
        if (result.isSuccessful()) {
            Map<String, Object> body = new LinkedHashMap<String, Object>();
            body.put("success", Boolean.TRUE); body.put("code", "OK"); body.put("message", result.getMessage()); body.put("avatar", publicPath);
            PortalServletSupport.writeJson(response, body);
        } else {
            PortalServletSupport.writeOperationResult(response, result);
        }
    }

    private Path avatarDirectory() {
        String resolved = getServletContext().getRealPath("/uploads/avatars");
        return resolved == null ? Paths.get(System.getProperty("java.io.tmpdir"), "ustbhospital-avatars") : Paths.get(resolved);
    }
    private static String extensionFor(String contentType) {
        if ("image/png".equalsIgnoreCase(contentType)) return ".png";
        if ("image/jpeg".equalsIgnoreCase(contentType)) return ".jpg";
        if ("image/webp".equalsIgnoreCase(contentType)) return ".webp";
        return null;
    }

    static boolean hasExpectedImageSignature(Path file, String extension) throws IOException {
        byte[] header = new byte[12];
        int read;
        try (InputStream input = Files.newInputStream(file)) { read = input.read(header); }
        if (".png".equals(extension)) return read >= 8 && header[0] == (byte) 0x89 && header[1] == 0x50 && header[2] == 0x4e && header[3] == 0x47 && header[4] == 0x0d && header[5] == 0x0a && header[6] == 0x1a && header[7] == 0x0a;
        if (".jpg".equals(extension)) return read >= 3 && header[0] == (byte) 0xff && header[1] == (byte) 0xd8 && header[2] == (byte) 0xff;
        return read >= 12 && header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F' && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P';
    }
}
