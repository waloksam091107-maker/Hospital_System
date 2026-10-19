package com.ustb.hospital.filter;

import com.ustb.hospital.service.AuditContext;
import com.ustb.hospital.service.AuditActions;
import com.ustb.hospital.service.AuditService;
import javax.servlet.http.HttpServletRequest;
import java.util.Locale;

/** One failure event per request, regardless of annotation filter ordering. */
final class AuditFailureRecorder {
    private static final String MARKER = AuditFailureRecorder.class.getName() + ".RECORDED";
    static void record(HttpServletRequest request, AuditService service) {
        if (Boolean.TRUE.equals(request.getAttribute(MARKER))) return;
        request.setAttribute(MARKER, Boolean.TRUE);
        String path = request.getServletPath();
        String suffix = request.getPathInfo();
        if (path == null) path = "";
        if (suffix != null) path += suffix;
        String action = AuditActions.forPath(path);
        String appointmentId = AuditService.safeTargetId(request.getParameter("appointmentId"));
        String scheduleId = AuditService.safeTargetId(request.getParameter("dateId"));
        service.recordFailure(AuditContext.fromRequest(request), action,
                appointmentId != null ? "APPOINTMENT" : scheduleId != null ? "SCHEDULE" : null,
                appointmentId != null ? appointmentId : scheduleId);
    }
    private AuditFailureRecorder() { }
}
