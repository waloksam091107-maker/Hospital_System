package com.ustb.hospital.service;

import com.ustb.hospital.entity.Admins;
import com.ustb.hospital.entity.Doctors;
import com.ustb.hospital.entity.Patients;
import com.ustb.hospital.mapper.AdminsMapper;
import com.ustb.hospital.mapper.DoctorsMapper;
import com.ustb.hospital.mapper.PatientsMapper;
import com.ustb.hospital.security.PasswordService;
import com.ustb.hospital.utils.AppointmentOperationResult;
import com.ustb.hospital.utils.MybatisUtil;
import org.apache.ibatis.session.SqlSession;
import java.util.function.Supplier;

/** Locks the account while changing credentials and writing its success audit. */
public final class PasswordChangeService {
    private final Supplier<SqlSession> sessions;
    private final AuditService audit;
    public PasswordChangeService() { this(MybatisUtil::getTransactionSqlSession, new AuditService()); }
    public PasswordChangeService(Supplier<SqlSession> sessions, AuditService audit) { this.sessions = sessions; this.audit = audit; }
    public AppointmentOperationResult change(String role, int id, String current, String replacement, AuditContext context) {
        try { PasswordService.validateNewPassword(replacement); }
        catch (IllegalArgumentException bad) { return AppointmentOperationResult.failure("INVALID_PASSWORD", bad.getMessage()); }
        return changeInternal(role, id, current, replacement, context, false);
    }
    public AppointmentOperationResult upgradeLegacy(String role, int id, String current, AuditContext context) {
        AppointmentOperationResult result = changeInternal(role, id, current, current, context, true);
        if (!result.isSuccessful()) audit.recordFailure(context, "PASSWORD_UPGRADE", "ACCOUNT", Integer.toString(id));
        return result;
    }
    private AppointmentOperationResult changeInternal(String role, int id, String current, String replacement, AuditContext context, boolean upgrade) {
        try (SqlSession session = sessions.get()) {
            try {
                String hash = null, old = null;
                if ("DOCTOR".equals(role)) { Doctors value = session.getMapper(DoctorsMapper.class).selectForPasswordUpdate(id); if (value != null) { hash = value.getPasswordHash(); old = value.getPassword(); } }
                else if ("PATIENT".equals(role)) { Patients value = session.getMapper(PatientsMapper.class).selectForPasswordUpdate(id); if (value != null) { hash = value.getPasswordHash(); old = value.getPassword(); } }
                else if ("ADMIN".equals(role)) { Admins value = session.getMapper(AdminsMapper.class).selectForPasswordUpdate(id); if (value != null) { hash = value.getPasswordHash(); old = value.getPassword(); } }
                if (!PasswordService.matches(current, hash, old)) { session.rollback(); return AppointmentOperationResult.failure("FORBIDDEN", "当前密码错误或未登录。"); }
                if (upgrade && !PasswordService.needsUpgrade(hash)) { session.rollback(); return AppointmentOperationResult.success("密码已是最新格式。"); }
                String newHash = PasswordService.hash(replacement);
                int changed = "DOCTOR".equals(role) ? session.getMapper(DoctorsMapper.class).upgradePasswordHash(id, newHash)
                        : "PATIENT".equals(role) ? session.getMapper(PatientsMapper.class).upgradePasswordHash(id, newHash)
                        : "ADMIN".equals(role) ? session.getMapper(AdminsMapper.class).upgradePasswordHash(id, newHash) : 0;
                if (changed != 1) throw new IllegalStateException("Account update failed.");
                if (!upgrade) AccountSecurityService.passwordChanged(session, role, id);
                audit.writeSuccess(session, context, upgrade ? "PASSWORD_UPGRADE" : "PASSWORD_RESET", "ACCOUNT", Integer.toString(id));
                session.commit(); return AppointmentOperationResult.success("密码已修改。");
            } catch (RuntimeException error) { session.rollback(); return AppointmentOperationResult.failure("PASSWORD_CHANGE_FAILED", "修改密码失败，请稍后重试。"); }
        } catch (RuntimeException error) { return AppointmentOperationResult.failure("PASSWORD_CHANGE_FAILED", "修改密码失败，请稍后重试。"); }
    }
}
