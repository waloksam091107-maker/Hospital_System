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
import com.ustb.hospital.utils.SessionIdentity;
import org.apache.ibatis.session.SqlSession;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/** Safe account creation and self-service profile mutations for the portal. */
public class PortalAccountService {
    private final Supplier<SqlSession> transactions;
    private final Supplier<SqlSession> reads;
    private final AuditService audit;

    public PortalAccountService() {
        this(MybatisUtil::getTransactionSqlSession, MybatisUtil::getSqlSession, new AuditService());
    }

    public PortalAccountService(Supplier<SqlSession> transactions, Supplier<SqlSession> reads, AuditService audit) {
        this.transactions = transactions;
        this.reads = reads;
        this.audit = audit;
    }

    public AppointmentOperationResult registerPatient(String name, String idCardNumber, String password,
                                                      String phone, String email, AuditContext context) {
        String normalizedName = normalize(name, 100);
        String normalizedCard = normalize(idCardNumber, 50);
        if (normalizedName == null || normalizedName.length() < 2) return failure(context, "PATIENT_REGISTER", "INVALID_NAME", "姓名长度应为 2 到 100 个字符。");
        if (normalizedCard == null || !normalizedCard.matches("[0-9Xx]{15,18}")) return failure(context, "PATIENT_REGISTER", "INVALID_ID_CARD", "身份证号格式不正确。");
        if (!validPhone(phone) || !validEmail(email)) return failure(context, "PATIENT_REGISTER", "INVALID_CONTACT", "联系电话或邮箱格式不正确。");
        try { PasswordService.validateNewPassword(password); }
        catch (IllegalArgumentException exception) { return failure(context, "PATIENT_REGISTER", "INVALID_PASSWORD", exception.getMessage()); }

        try (SqlSession session = transactions.get()) {
            PatientsMapper mapper = session.getMapper(PatientsMapper.class);
            if (mapper.selectByName(normalizedName) != null || mapper.selectByIdCardNumber(normalizedCard) != null) {
                session.rollback();
                return failure(context, "PATIENT_REGISTER", "DUPLICATE_ACCOUNT", "该患者账号或身份证号已存在。");
            }
            Patients patient = new Patients();
            patient.setName(normalizedName);
            patient.setIdCardNumber(normalizedCard.toUpperCase());
            patient.setPasswordHash(PasswordService.hash(password));
            patient.setAvatar("");
            patient.setPhone(normalizeOptional(phone, 50));
            patient.setEmail(normalizeOptional(email, 255));
            if (mapper.insertPortal(patient) != 1 || patient.getPatientId() <= 0) throw new IllegalStateException("患者账户写入失败。");
            audit.writeSuccess(session, context, "PATIENT_REGISTER", "PATIENT", String.valueOf(patient.getPatientId()));
            session.commit();
            return AppointmentOperationResult.success("注册成功，请使用患者身份登录。");
        } catch (RuntimeException exception) {
            return failure(context, "PATIENT_REGISTER", "REGISTER_FAILED", "注册暂时不可用，请稍后重试。");
        }
    }

    public Map<String, Object> readOwnProfile(String role, int userId) {
        try (SqlSession session = reads.get()) {
            Map<String, Object> result = new LinkedHashMap<String, Object>();
            result.put("role", role);
            if (SessionIdentity.ROLE_PATIENT.equals(role)) {
                Patients patient = session.getMapper(PatientsMapper.class).selectById(userId);
                if (patient == null) return null;
                result.put("id", patient.getPatientId()); result.put("name", patient.getName()); result.put("idCardNumber", patient.getIdCardNumber());
                result.put("avatar", patient.getAvatar()); result.put("phone", patient.getPhone()); result.put("email", patient.getEmail()); result.put("balance", patient.getBalance());
            } else if (SessionIdentity.ROLE_DOCTOR.equals(role)) {
                Doctors doctor = session.getMapper(DoctorsMapper.class).selectPortalProfileById(userId);
                if (doctor == null) return null;
                result.put("id", doctor.getDoctorId()); result.put("name", doctor.getName()); result.put("jobNumber", doctor.getJobNumber());
                result.put("avatar", doctor.getAvatar()); result.put("phone", doctor.getPhone()); result.put("email", doctor.getEmail()); result.put("introduction", doctor.getIntroduction());
            } else if (SessionIdentity.ROLE_ADMIN.equals(role)) {
                Admins admin = session.getMapper(AdminsMapper.class).selectPortalProfileById(userId);
                if (admin == null) return null;
                result.put("id", admin.getAdminId()); result.put("name", admin.getName()); result.put("username", admin.getUsername());
                result.put("avatar", admin.getAvatar()); result.put("phone", admin.getPhone()); result.put("email", admin.getEmail());
            } else return null;
            return result;
        }
    }

    public AppointmentOperationResult updateOwnProfile(String role, int userId, String phone, String email,
                                                        String introduction, AuditContext context) {
        if (!validPhone(phone) || !validEmail(email)) return failure(context, "INVALID_CONTACT", "联系电话或邮箱格式不正确。");
        String intro = normalizeOptional(introduction, 2000);
        if (introduction != null && introduction.trim().length() > 0 && intro == null) return failure(context, "INVALID_INTRODUCTION", "个人简介不能超过 2000 个字符。");
        try (SqlSession session = transactions.get()) {
            int updated;
            if (SessionIdentity.ROLE_PATIENT.equals(role)) updated = session.getMapper(PatientsMapper.class).updatePortalProfile(userId, normalizeOptional(phone, 50), normalizeOptional(email, 255));
            else if (SessionIdentity.ROLE_DOCTOR.equals(role)) updated = session.getMapper(DoctorsMapper.class).updatePortalProfile(userId, normalizeOptional(phone, 50), normalizeOptional(email, 255), intro);
            else if (SessionIdentity.ROLE_ADMIN.equals(role)) updated = session.getMapper(AdminsMapper.class).updatePortalProfile(userId, normalizeOptional(phone, 50), normalizeOptional(email, 255));
            else return failure(context, "FORBIDDEN", "当前账号无权执行此操作。");
            if (updated != 1) { session.rollback(); return failure(context, "NOT_FOUND", "当前账号不存在或已被删除。"); }
            audit.writeSuccess(session, context, "PROFILE_UPDATE", "ACCOUNT", String.valueOf(userId));
            session.commit();
            return AppointmentOperationResult.success("个人资料已保存。");
        } catch (RuntimeException exception) { return failure(context, "PROFILE_UPDATE_FAILED", "保存失败，请稍后重试。"); }
    }

    public AppointmentOperationResult updateOwnAvatar(String role, int userId, String avatar, AuditContext context) {
        try (SqlSession session = transactions.get()) {
            int updated;
            if (SessionIdentity.ROLE_PATIENT.equals(role)) updated = session.getMapper(PatientsMapper.class).updateAvatar(userId, avatar);
            else if (SessionIdentity.ROLE_DOCTOR.equals(role)) updated = session.getMapper(DoctorsMapper.class).updateAvatar(userId, avatar);
            else if (SessionIdentity.ROLE_ADMIN.equals(role)) updated = session.getMapper(AdminsMapper.class).updateAvatar(userId, avatar);
            else return failure(context, "FORBIDDEN", "当前账号无权执行此操作。");
            if (updated != 1) { session.rollback(); return failure(context, "NOT_FOUND", "当前账号不存在或已被删除。"); }
            audit.writeSuccess(session, context, "AVATAR_UPDATE", "ACCOUNT", String.valueOf(userId));
            session.commit();
            return AppointmentOperationResult.success("头像已更新。");
        } catch (RuntimeException exception) { return failure(context, "AVATAR_UPDATE_FAILED", "头像保存失败，请稍后重试。"); }
    }

    private AppointmentOperationResult failure(AuditContext context, String code, String message) {
        return failure(context, "PORTAL_PROFILE", code, message);
    }
    private AppointmentOperationResult failure(AuditContext context, String action, String code, String message) {
        if (context != null) audit.recordFailure(context, action, "ACCOUNT", null);
        return AppointmentOperationResult.failure(code, message);
    }
    private static String normalize(String value, int limit) { String result = normalizeOptional(value, limit); return result == null || result.isEmpty() ? null : result; }
    private static String normalizeOptional(String value, int limit) { if (value == null) return null; String result = value.trim(); return result.length() <= limit ? result : null; }
    private static boolean validPhone(String value) { return value == null || value.trim().isEmpty() || value.trim().matches("[0-9+() -]{5,50}"); }
    private static boolean validEmail(String value) { return value == null || value.trim().isEmpty() || value.trim().matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+"); }
}
