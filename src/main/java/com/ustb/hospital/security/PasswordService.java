package com.ustb.hospital.security;

import org.mindrot.jbcrypt.BCrypt;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Password hashing and the one-way transition from legacy plaintext rows. */
public final class PasswordService {
    private static final int BCRYPT_COST = 12;

    private PasswordService() { }

    public static String hash(String rawPassword) {
        validate(rawPassword);
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt(BCRYPT_COST));
    }

    /** Uses legacy comparison only if a BCrypt hash has not yet been populated. */
    public static boolean matches(String rawPassword, String passwordHash, String legacyPassword) {
        if (rawPassword == null) return false;
        if (passwordHash != null && passwordHash.trim().length() > 0) {
            try { return BCrypt.checkpw(rawPassword, passwordHash); }
            catch (IllegalArgumentException exception) { return false; }
        }
        if (legacyPassword == null) return false;
        return MessageDigest.isEqual(rawPassword.getBytes(StandardCharsets.UTF_8), legacyPassword.getBytes(StandardCharsets.UTF_8));
    }

    public static boolean needsUpgrade(String passwordHash) {
        return passwordHash == null || passwordHash.trim().length() == 0;
    }

    public static void validateNewPassword(String password) {
        if (password == null || password.length() < 8 || password.length() > 128 || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("新密码至少 8 个字符，且 UTF-8 编码不能超过 72 字节。");
        }
    }

    private static void validate(String password) {
        if (password == null || password.length() == 0 || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("密码不能为空且 UTF-8 编码不能超过 72 字节。");
        }
    }
}
