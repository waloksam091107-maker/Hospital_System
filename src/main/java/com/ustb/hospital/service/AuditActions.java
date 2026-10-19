package com.ustb.hospital.service;

import java.util.*;

/** Canonical business actions and unambiguous action names used by older audit filters. */
public final class AuditActions {
    private static final Map<String, String> PATHS;
    static {
        Map<String, String> paths = new LinkedHashMap<>();
        paths.put("/appointments/book", "APPOINTMENTS_BOOK");
        paths.put("/appointments/cancel", "APPOINTMENTS_CANCEL");
        paths.put("/appointments/reschedule", "APPOINTMENTS_RESCHEDULE");
        paths.put("/appointments/start", "APPOINTMENTS_START");
        paths.put("/schedules/create", "SCHEDULES_CREATE");
        paths.put("/schedules/update", "SCHEDULES_UPDATE");
        paths.put("/schedules/delete", "SCHEDULES_DELETE");
        paths.put("/medical-records/draft", "MEDICAL_RECORD_DRAFT");
        paths.put("/medical-records/finalize", "MEDICAL_RECORD_FINALIZE");
        paths.put("/password/reset", "PASSWORD_RESET");
        PATHS = Collections.unmodifiableMap(paths);
    }
    public static String forPath(String path) {
        String canonical = PATHS.get(path);
        if (canonical != null) return canonical;
        String derived = legacy(path);
        return derived.matches("[A-Z0-9_-]{1,80}") ? derived : "REQUEST_REJECTED";
    }
    public static List<String> aliases(String action) {
        if (action == null) return Collections.emptyList();
        for (Map.Entry<String, String> entry : PATHS.entrySet()) {
            String old = legacy(entry.getKey());
            if (action.equals(entry.getValue()) || action.equals(old)) return Arrays.asList(entry.getValue(), old);
        }
        return Collections.singletonList(action);
    }
    private static String legacy(String path) { return path == null ? "" : path.replace('/', '_').toUpperCase(Locale.ROOT); }
    private AuditActions() { }
}
