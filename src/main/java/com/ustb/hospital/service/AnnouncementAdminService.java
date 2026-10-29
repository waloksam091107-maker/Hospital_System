package com.ustb.hospital.service;

import com.ustb.hospital.entity.Announcements;
import com.ustb.hospital.mapper.AnnouncementsMapper;
import com.ustb.hospital.utils.AppointmentOperationResult;
import com.ustb.hospital.utils.MybatisUtil;
import org.apache.ibatis.session.SqlSession;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Supplier;

/** Administrator-only announcement maintenance; public readers use the existing list endpoint. */
public class AnnouncementAdminService {
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final Supplier<SqlSession> transactions;
    private final Supplier<SqlSession> reads;
    private final AuditService audit;

    public AnnouncementAdminService() {
        this(MybatisUtil::getTransactionSqlSession, MybatisUtil::getSqlSession, new AuditService());
    }

    public AnnouncementAdminService(Supplier<SqlSession> transactions, Supplier<SqlSession> reads, AuditService audit) {
        this.transactions = transactions;
        this.reads = reads;
        this.audit = audit;
    }

    public List<Announcements> list() {
        try (SqlSession session = reads.get()) {
            return session.getMapper(AnnouncementsMapper.class).selectAll();
        }
    }

    public AppointmentOperationResult create(String title, String content, AuditContext context) {
        if (!valid(title, 255) || !valid(content, 10000)) return failure(context, "ANNOUNCEMENT_CREATE", "INVALID_REQUEST", "公告标题或正文不符合要求。");
        SqlSession session = null;
        try {
            session = transactions.get();
            Announcements announcement = new Announcements();
            announcement.setTitle(title.trim());
            announcement.setContent(content.trim());
            announcement.setCreationTime(DISPLAY_TIME.format(LocalDateTime.now()));
            announcement.setCreator("管理员");
            announcement.setStatus("DRAFT");
            announcement.setUpdatedAt(announcement.getCreationTime());
            announcement.setCreatorAdminId(context == null ? null : context.getActorId());
            if (session.getMapper(AnnouncementsMapper.class).insertPortalAdmin(announcement) != 1 || announcement.getAnnouncementId() == null) throw new IllegalStateException();
            audit.writeSuccess(session, context, "ANNOUNCEMENT_CREATE", "ANNOUNCEMENT", String.valueOf(announcement.getAnnouncementId()));
            session.commit();
            return AppointmentOperationResult.success("公告草稿已保存，发布后将在门户显示。");
        } catch (RuntimeException exception) {
            return rollback(session, context, "ANNOUNCEMENT_CREATE", "CREATE_FAILED", "公告草稿保存失败，请稍后重试。");
        } finally { close(session); }
    }

    public AppointmentOperationResult update(int id, String title, String content, AuditContext context) {
        if (id <= 0 || !valid(title, 255) || !valid(content, 10000)) return failure(context, "ANNOUNCEMENT_UPDATE", "INVALID_REQUEST", "公告编号、标题或正文不符合要求。");
        SqlSession session = null;
        try {
            session = transactions.get();
            AnnouncementsMapper mapper = session.getMapper(AnnouncementsMapper.class);
            if (mapper.selectById(id) == null) return rollback(session, context, "ANNOUNCEMENT_UPDATE", "NOT_FOUND", "公告不存在。");
            Announcements announcement = new Announcements(); announcement.setAnnouncementId(id); announcement.setTitle(title.trim()); announcement.setContent(content.trim()); announcement.setUpdatedAt(DISPLAY_TIME.format(LocalDateTime.now()));
            if (mapper.updatePortalAdmin(announcement) != 1) throw new IllegalStateException();
            audit.writeSuccess(session, context, "ANNOUNCEMENT_UPDATE", "ANNOUNCEMENT", String.valueOf(id));
            session.commit();
            return AppointmentOperationResult.success("公告已保存。");
        } catch (RuntimeException exception) {
            return rollback(session, context, "ANNOUNCEMENT_UPDATE", "UPDATE_FAILED", "公告保存失败，请稍后重试。");
        } finally { close(session); }
    }

    public AppointmentOperationResult delete(int id, AuditContext context) {
        if (id <= 0) return failure(context, "ANNOUNCEMENT_DELETE", "INVALID_REQUEST", "公告编号无效。");
        SqlSession session = null;
        try {
            session = transactions.get();
            AnnouncementsMapper mapper = session.getMapper(AnnouncementsMapper.class);
            if (mapper.selectById(id) == null) return rollback(session, context, "ANNOUNCEMENT_DELETE", "NOT_FOUND", "公告不存在。");
            if (mapper.deletePortalAdmin(id) != 1) throw new IllegalStateException();
            audit.writeSuccess(session, context, "ANNOUNCEMENT_DELETE", "ANNOUNCEMENT", String.valueOf(id));
            session.commit();
            return AppointmentOperationResult.success("公告已删除。");
        } catch (RuntimeException exception) {
            return rollback(session, context, "ANNOUNCEMENT_DELETE", "DELETE_FAILED", "公告删除失败，请稍后重试。");
        } finally { close(session); }
    }

    public AppointmentOperationResult changeStatus(int id, boolean publish, AuditContext context) {
        if (id <= 0) return failure(context, "ANNOUNCEMENT_STATUS", "INVALID_REQUEST", "公告编号无效。");
        SqlSession session = null;
        try {
            session = transactions.get();
            AnnouncementsMapper mapper = session.getMapper(AnnouncementsMapper.class);
            if (mapper.selectById(id) == null) return rollback(session, context, "ANNOUNCEMENT_STATUS", "NOT_FOUND", "公告不存在。");
            String now = DISPLAY_TIME.format(LocalDateTime.now());
            if (mapper.updateStatus(id, publish ? "PUBLISHED" : "OFFLINE", publish ? now : null) != 1) throw new IllegalStateException();
            audit.writeSuccess(session, context, publish ? "ANNOUNCEMENT_PUBLISH" : "ANNOUNCEMENT_OFFLINE", "ANNOUNCEMENT", String.valueOf(id));
            session.commit();
            return AppointmentOperationResult.success(publish ? "公告已发布。" : "公告已下线。");
        } catch (RuntimeException exception) {
            return rollback(session, context, "ANNOUNCEMENT_STATUS", "UPDATE_FAILED", "公告状态保存失败，请稍后重试。");
        } finally { close(session); }
    }

    private static boolean valid(String value, int maximum) { return value != null && !value.trim().isEmpty() && value.trim().length() <= maximum; }
    private AppointmentOperationResult rollback(SqlSession session, AuditContext context, String action, String code, String message) { if (session != null) session.rollback(); return failure(context, action, code, message); }
    private AppointmentOperationResult failure(AuditContext context, String action, String code, String message) { if (context != null) audit.recordFailure(context, action, "ANNOUNCEMENT", null); return AppointmentOperationResult.failure(code, message); }
    private static void close(SqlSession session) { if (session != null) session.close(); }
}
