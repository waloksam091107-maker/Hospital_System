package com.ustb.hospital.utils;

import java.io.IOException;
import java.nio.file.*;

/** Persistent upload root, independent of an exploded WAR and container replacement. */
public final class AvatarStorage {
    private final Path root;

    public AvatarStorage(Path root) {
        if (root == null || !root.isAbsolute()) throw new IllegalArgumentException("Upload directory must be absolute.");
        this.root = root.normalize();
    }

    public static AvatarStorage configured() {
        String configured = System.getProperty("hospital.upload.dir");
        if (configured == null || configured.trim().isEmpty()) configured = System.getenv("HOSPITAL_UPLOAD_DIR");
        Path root = configured == null || configured.trim().isEmpty()
                ? Paths.get(System.getProperty("catalina.base", System.getProperty("user.home")), "hospital-data", "uploads").toAbsolutePath()
                : Paths.get(configured.trim());
        return new AvatarStorage(root);
    }

    public Path directory() throws IOException {
        Files.createDirectories(root);
        Path directory = root.resolve("avatars");
        if (Files.isSymbolicLink(directory)) throw new IOException("Invalid avatar directory.");
        Files.createDirectories(directory);
        return directory;
    }

    public Path resolve(String filename) throws IOException {
        if (filename == null || !filename.matches("[A-Za-z0-9][A-Za-z0-9_-]{0,180}\\.(png|jpg|webp)"))
            throw new IllegalArgumentException("Invalid avatar filename.");
        Path destination = directory().resolve(filename);
        if (Files.isSymbolicLink(destination)) throw new IOException("Invalid avatar file.");
        return destination;
    }
}
