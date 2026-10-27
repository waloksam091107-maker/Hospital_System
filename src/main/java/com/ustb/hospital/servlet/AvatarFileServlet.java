package com.ustb.hospital.servlet;

import com.ustb.hospital.utils.AvatarStorage;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;

/** Stable public route for non-executable image files held outside the web application. */
@WebServlet("/uploads/avatars/*")
public class AvatarFileServlet extends HttpServlet {
    private final AvatarStorage storage;
    public AvatarFileServlet() { this(AvatarStorage.configured()); }
    AvatarFileServlet(AvatarStorage storage) { this.storage = storage; }

    @Override protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = request.getPathInfo();
        Path file;
        try {
            file = storage.resolve(path != null && path.startsWith("/") ? path.substring(1) : null);
            if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) { response.setStatus(404); return; }
        } catch (IllegalArgumentException | IOException invalid) { response.setStatus(404); return; }
        String name = file.getFileName().toString();
        response.setContentType(name.endsWith(".png") ? "image/png" : name.endsWith(".jpg") ? "image/jpeg" : "image/webp");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Cache-Control", "public, max-age=86400");
        response.setContentLengthLong(Files.size(file));
        try (InputStream input = Files.newInputStream(file, LinkOption.NOFOLLOW_LINKS)) {
            byte[] buffer = new byte[8192]; int count;
            while ((count = input.read(buffer)) != -1) response.getOutputStream().write(buffer, 0, count);
        }
    }
}
