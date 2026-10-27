package com.ustb.hospital.database;

/** Fail-closed adapter for the one reviewed empty-schema file, never a general SQL rewriter. */
final class IsolatedOceanBaseSql {
    static String rewriteSchema(String sql, String database) {
        if (database == null || !database.matches("hospital_test_[a-f0-9]{32}")) throw new IllegalArgumentException("Random isolated schema required.");
        String clean = sql.replaceAll("(?m)^\\s*--[^\\r\\n]*", "");
        clean = clean.replaceAll("(?im)^\\s*CREATE DATABASE IF NOT EXISTS hospt;\\s*", "");
        clean = clean.replaceAll("(?im)^\\s*USE hospt;\\s*", "");
        if (clean.matches("(?is).*\\b(USE|DATABASE|SCHEMA)\\b.*") || clean.matches("(?is).*\\bhospt\\b.*")
                || clean.matches("(?is).*\\b(INTO|UPDATE|FROM|JOIN|TABLE|REFERENCES)\\s+(IF\\s+NOT\\s+EXISTS\\s+)?`?[A-Za-z_][A-Za-z0-9_]*`?\\s*\\..*"))
            throw new IllegalArgumentException("Schema selection or qualified destination is forbidden.");
        return "USE `" + database + "`;\n" + clean;
    }
}
