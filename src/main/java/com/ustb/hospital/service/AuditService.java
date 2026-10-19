package com.ustb.hospital.service;

import com.ustb.hospital.entity.AuditLog;
import com.ustb.hospital.mapper.AuditLogsMapper;
import com.ustb.hospital.utils.MybatisUtil;
import com.ustb.hospital.utils.SessionIdentity;
import org.apache.ibatis.session.SqlSession;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.logging.Logger;

/** Best-effort append-only audit writer; clinical and credential content is intentionally omitted. */
public class AuditService {
    private static final Logger LOGGER = Logger.getLogger(AuditService.class.getName());
    private final Supplier<SqlSession> sessionSupplier;

    public AuditService() { this(MybatisUtil::getSqlSession); }

    public AuditService(Supplier<SqlSession> sessionSupplier) {
        if (sessionSupplier == null) throw new IllegalArgumentException("An audit session supplier is required.");
        this.sessionSupplier = sessionSupplier;
    }

    /** Caller owns the transaction; an insertion failure must abort the business operation. */
    public void writeSuccess(SqlSession session, AuditContext context, String action, String targetType, String targetId) {
        if (session == null) throw new IllegalArgumentException("An existing transaction is required.");
        AuditLog log = event(context, action, targetType, targetId, "SUCCESS");
        if (session.getMapper(AuditLogsMapper.class).insert(log) != 1) {
            throw new IllegalStateException("Audit event was not inserted.");
        }
    }

    /** Failed operations are recorded independently after the business transaction rolls back. */
    public void recordFailure(AuditContext context, String action, String targetType, String targetId) {
        try {
            AuditLog log = event(context, action, targetType, targetId, "FAILURE");
            try (SqlSession session = sessionSupplier.get()) {
                if (session.getMapper(AuditLogsMapper.class).insert(log) != 1) throw new IllegalStateException("Audit event was not inserted.");
                session.commit();
            }
        } catch (RuntimeException exception) {
            LOGGER.severe("Unable to persist a security audit failure event.");
        }
    }

    private static AuditLog event(AuditContext context, String action, String targetType, String targetId, String outcome) {
        if (context == null || action == null || !action.matches("[A-Z0-9_-]{1,80}")) {
            throw new IllegalArgumentException("Invalid audit metadata.");
        }
        if (targetType != null && !targetType.matches("[A-Z0-9_]{1,80}")) throw new IllegalArgumentException("Invalid audit target.");
        if (targetId != null && safeTargetId(targetId) == null) throw new IllegalArgumentException("Invalid audit target.");
        AuditLog log = new AuditLog();
        log.setActorRole(context.getActorRole());
        log.setActorId(context.getActorId());
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setOutcome(outcome);
        log.setRequestMethod(context.getRequestMethod());
        log.setRequestPath(context.getRequestPath());
        log.setRemoteAddress(context.getRemoteAddress());
        return log;
    }
    public void record(HttpServletRequest request, String action, String targetType, String targetId, String outcome) {
        try {
            AuditLog log = new AuditLog();
            HttpSession session = request.getSession(false);
            String role = session == null ? null : (String) session.getAttribute(SessionIdentity.CURRENT_USER_ROLE);
            log.setActorRole(role);
            log.setActorId(actorId(session, role));
            log.setAction(action);
            log.setTargetType(targetType);
            log.setTargetId(safeTargetId(targetId));
            log.setOutcome(outcome);
            log.setRequestMethod(request.getMethod());
            log.setRequestPath(request.getServletPath());
            log.setRemoteAddress(request.getRemoteAddr());
            SqlSession sqlSession = MybatisUtil.getSqlSession();
            try { sqlSession.getMapper(AuditLogsMapper.class).insert(log); }
            finally { sqlSession.close(); }
        } catch (RuntimeException ignored) {
            LOGGER.severe("Unable to persist an authentication audit event.");
        }
    }

    public List<AuditLog> recent(int limit) {
        int boundedLimit = limit < 1 ? 50 : Math.min(limit, 200);
        try (SqlSession sqlSession = sessionSupplier.get()) { return sqlSession.getMapper(AuditLogsMapper.class).selectRecent(boundedLimit); }
    }

    public AuditQueryResult search(AuditQuery query) {
        try (SqlSession session = sessionSupplier.get()) {
            AuditLogsMapper mapper = session.getMapper(AuditLogsMapper.class);
            return new AuditQueryResult(mapper.search(query), mapper.countSearch(query), query.getPage(), query.getPageSize());
        }
    }

    public static String safeTargetId(String value) {
        return value != null && value.matches("[0-9]{1,15}") ? value : null;
    }

    private static Integer actorId(HttpSession session, String role) {
        if (session == null || role == null) return null;
        Object value = SessionIdentity.ROLE_DOCTOR.equals(role) ? session.getAttribute(SessionIdentity.CURRENT_DOCTOR_ID)
                : SessionIdentity.ROLE_PATIENT.equals(role) ? session.getAttribute(SessionIdentity.CURRENT_PATIENT_ID)
                : SessionIdentity.ROLE_ADMIN.equals(role) ? session.getAttribute(SessionIdentity.CURRENT_ADMIN_ID) : null;
        return value instanceof Integer ? (Integer) value : null;
    }
}
