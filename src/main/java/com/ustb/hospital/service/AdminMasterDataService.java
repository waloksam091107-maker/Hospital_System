package com.ustb.hospital.service;

import com.ustb.hospital.entity.Departments;
import com.ustb.hospital.entity.Doctors;
import com.ustb.hospital.entity.Patients;
import com.ustb.hospital.entity.ProfessionalTitles;
import com.ustb.hospital.mapper.DepartmentsMapper;
import com.ustb.hospital.mapper.DoctorsMapper;
import com.ustb.hospital.mapper.PatientsMapper;
import com.ustb.hospital.mapper.ProfessionalTitlesMapper;
import com.ustb.hospital.security.PasswordService;
import com.ustb.hospital.utils.AppointmentOperationResult;
import com.ustb.hospital.utils.MybatisUtil;
import org.apache.ibatis.session.SqlSession;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/** Administrative maintenance for reference data; all writes are transactional and audited. */
public class AdminMasterDataService {
    private final Supplier<SqlSession> transactions;
    private final Supplier<SqlSession> reads;
    private final AuditService audit;
    public AdminMasterDataService() { this(MybatisUtil::getTransactionSqlSession, MybatisUtil::getSqlSession, new AuditService()); }
    public AdminMasterDataService(Supplier<SqlSession> transactions, Supplier<SqlSession> reads, AuditService audit) { this.transactions = transactions; this.reads = reads; this.audit = audit; }

    public List<Doctors> doctors() { try (SqlSession s = reads.get()) { return s.getMapper(DoctorsMapper.class).selectAll(null, null, -1); } }
    public List<Patients> patients() { try (SqlSession s = reads.get()) { return s.getMapper(PatientsMapper.class).selectAll(); } }
    public List<Departments> departments() { try (SqlSession s = reads.get()) { return s.getMapper(DepartmentsMapper.class).selectAll(); } }
    public List<ProfessionalTitles> professionalTitles() { try (SqlSession s = reads.get()) { return s.getMapper(ProfessionalTitlesMapper.class).selectAll(); } }

    public AppointmentOperationResult createDoctor(Doctors doctor, String rawPassword, AuditContext context) {
        if (!validDoctor(doctor) || !validPassword(rawPassword)) return failure(context, "DOCTOR_CREATE", "INVALID_REQUEST", "医生信息、挂号费或初始密码不符合要求。");
        SqlSession s = null;
        try {
            s = transactions.get(); DoctorsMapper doctors = s.getMapper(DoctorsMapper.class);
            if (s.getMapper(DepartmentsMapper.class).selectById(doctor.getDepartmentId()) == null || s.getMapper(ProfessionalTitlesMapper.class).selectById(doctor.getProfessionalTitleId()) == null)
                return rollback(s, context, "DOCTOR_CREATE", "NOT_FOUND", "所属科室或职称不存在。");
            doctor.setJobNumber(nextJobNumber(doctors)); doctor.setPasswordHash(PasswordService.hash(rawPassword)); doctor.setPassword(null); doctor.setAvatar("");
            if (doctors.insertPortalAdmin(doctor) != 1 || doctor.getDoctorId() <= 0) throw new IllegalStateException("医生写入失败。");
            audit.writeSuccess(s, context, "DOCTOR_CREATE", "DOCTOR", String.valueOf(doctor.getDoctorId())); s.commit();
            return AppointmentOperationResult.success("医生已创建，工号为 " + doctor.getJobNumber() + "。");
        } catch (RuntimeException e) { return rollback(s, context, "DOCTOR_CREATE", "CREATE_FAILED", "医生创建失败，请稍后重试。"); } finally { close(s); }
    }

    public AppointmentOperationResult updateDoctor(Doctors doctor, AuditContext context) {
        if (doctor == null || doctor.getDoctorId() <= 0 || !validDoctor(doctor)) return failure(context, "DOCTOR_UPDATE", "INVALID_REQUEST", "医生信息或挂号费不符合要求。");
        SqlSession s = null;
        try { s = transactions.get(); DoctorsMapper doctors = s.getMapper(DoctorsMapper.class);
            if (doctors.findById(doctor.getDoctorId()) == null) return rollback(s, context, "DOCTOR_UPDATE", "NOT_FOUND", "医生不存在。");
            if (s.getMapper(DepartmentsMapper.class).selectById(doctor.getDepartmentId()) == null || s.getMapper(ProfessionalTitlesMapper.class).selectById(doctor.getProfessionalTitleId()) == null) return rollback(s, context, "DOCTOR_UPDATE", "NOT_FOUND", "所属科室或职称不存在。");
            if (doctors.updatePortalAdmin(doctor) != 1) return rollback(s, context, "DOCTOR_UPDATE", "UPDATE_FAILED", "医生资料保存失败。");
            audit.writeSuccess(s, context, "DOCTOR_UPDATE", "DOCTOR", String.valueOf(doctor.getDoctorId())); s.commit(); return AppointmentOperationResult.success("医生资料已保存。");
        } catch (RuntimeException e) { return rollback(s, context, "DOCTOR_UPDATE", "UPDATE_FAILED", "医生资料保存失败，请稍后重试。"); } finally { close(s); }
    }

    public AppointmentOperationResult deleteDoctor(int doctorId, AuditContext context) {
        SqlSession s = null; try { s = transactions.get(); DoctorsMapper doctors = s.getMapper(DoctorsMapper.class);
            if (doctors.findById(doctorId) == null) return rollback(s, context, "DOCTOR_DELETE", "NOT_FOUND", "医生不存在。");
            if (doctors.countAppointmentsByDoctorId(doctorId) > 0 || doctors.countScheduleSlotsByDoctorId(doctorId) > 0) return rollback(s, context, "DOCTOR_DELETE", "HAS_REFERENCES", "该医生已有预约或排班记录，不能删除。");
            if (doctors.deletePortalAdmin(doctorId) != 1) return rollback(s, context, "DOCTOR_DELETE", "DELETE_FAILED", "医生删除失败。");
            audit.writeSuccess(s, context, "DOCTOR_DELETE", "DOCTOR", String.valueOf(doctorId)); s.commit(); return AppointmentOperationResult.success("医生已删除。");
        } catch (RuntimeException e) { return rollback(s, context, "DOCTOR_DELETE", "DELETE_FAILED", "医生删除失败，请稍后重试。"); } finally { close(s); }
    }

    public AppointmentOperationResult createPatient(String name, String idCard, String password, String phone, String email, AuditContext context) {
        if (!validText(name, 100) || !validText(idCard, 50) || !validPassword(password)) return failure(context, "PATIENT_CREATE", "INVALID_REQUEST", "患者姓名、身份证号或初始密码不符合要求。");
        SqlSession s = null; try { s = transactions.get(); PatientsMapper patients = s.getMapper(PatientsMapper.class);
            if (patients.selectByName(name.trim()) != null || patients.selectByIdCardNumber(idCard.trim()) != null) return rollback(s, context, "PATIENT_CREATE", "DUPLICATE_ACCOUNT", "患者姓名或身份证号已存在。");
            Patients patient = new Patients(); patient.setName(name.trim()); patient.setIdCardNumber(idCard.trim().toUpperCase()); patient.setPasswordHash(PasswordService.hash(password)); patient.setAvatar(""); patient.setPhone(optional(phone, 50)); patient.setEmail(optional(email, 255));
            if (patients.insertPortal(patient) != 1) throw new IllegalStateException("患者写入失败。"); audit.writeSuccess(s, context, "PATIENT_CREATE", "PATIENT", String.valueOf(patient.getPatientId())); s.commit(); return AppointmentOperationResult.success("患者已创建。");
        } catch (RuntimeException e) { return rollback(s, context, "PATIENT_CREATE", "CREATE_FAILED", "患者创建失败，请稍后重试。"); } finally { close(s); }
    }

    public AppointmentOperationResult deletePatient(int patientId, AuditContext context) {
        SqlSession s = null; try { s = transactions.get(); PatientsMapper patients = s.getMapper(PatientsMapper.class);
            if (patients.selectById(patientId) == null) return rollback(s, context, "PATIENT_DELETE", "NOT_FOUND", "患者不存在。");
            if (patients.countAppointmentsByPatientId(patientId) > 0) return rollback(s, context, "PATIENT_DELETE", "HAS_REFERENCES", "该患者已有预约记录，不能删除。");
            if (patients.deletePortalAdmin(patientId) != 1) return rollback(s, context, "PATIENT_DELETE", "DELETE_FAILED", "患者删除失败。"); audit.writeSuccess(s, context, "PATIENT_DELETE", "PATIENT", String.valueOf(patientId)); s.commit(); return AppointmentOperationResult.success("患者已删除。");
        } catch (RuntimeException e) { return rollback(s, context, "PATIENT_DELETE", "DELETE_FAILED", "患者删除失败，请稍后重试。"); } finally { close(s); }
    }

    /** Administrators may correct contact details, but never replace a patient's identity or balance. */
    public AppointmentOperationResult updatePatient(int patientId, String phone, String email, AuditContext context) {
        if (patientId <= 0 || !optionalLength(phone, 50) || !optionalLength(email, 255)) {
            return failure(context, "PATIENT_UPDATE", "INVALID_REQUEST", "患者编号、联系电话或邮箱不符合要求。");
        }
        SqlSession s = null;
        try {
            s = transactions.get();
            PatientsMapper patients = s.getMapper(PatientsMapper.class);
            if (patients.selectById(patientId) == null) {
                return rollback(s, context, "PATIENT_UPDATE", "NOT_FOUND", "患者不存在。");
            }
            if (patients.updatePortalProfile(patientId, optional(phone, 50), optional(email, 255)) != 1) {
                return rollback(s, context, "PATIENT_UPDATE", "UPDATE_FAILED", "患者资料保存失败。");
            }
            audit.writeSuccess(s, context, "PATIENT_UPDATE", "PATIENT", String.valueOf(patientId));
            s.commit();
            return AppointmentOperationResult.success("患者联系方式已保存。");
        } catch (RuntimeException e) {
            return rollback(s, context, "PATIENT_UPDATE", "UPDATE_FAILED", "患者资料保存失败，请稍后重试。");
        } finally {
            close(s);
        }
    }

    public AppointmentOperationResult saveDepartment(Departments department, AuditContext context) {
        if (department == null || !validText(department.getDepartmentName(), 100) || !optionalLength(department.getDepartmentDescription(), 500)) return failure(context, "DEPARTMENT_SAVE", "INVALID_REQUEST", "科室名称或说明不符合要求。");
        SqlSession s = null; try { s = transactions.get(); DepartmentsMapper departments = s.getMapper(DepartmentsMapper.class);
            Departments sameName = departments.selectByName(department.getDepartmentName().trim());
            if (sameName != null && sameName.getDepartmentId() != department.getDepartmentId()) return rollback(s, context, "DEPARTMENT_SAVE", "DUPLICATE_ACCOUNT", "科室名称已存在。");
            if (department.getDepartmentId() <= 0) {
                department.setDepartmentName(department.getDepartmentName().trim());
                department.setDepartmentDescription(optional(department.getDepartmentDescription(), 500));
                if (department.getDepartmentPid() == null) {
                    department.setDepartmentLevel(1);
                    department.setDepartmentPath("/");
                } else {
                    Departments parent = departments.selectById(department.getDepartmentPid().intValue());
                    if (parent == null) return rollback(s, context, "DEPARTMENT_CREATE", "NOT_FOUND", "上级科室不存在。");
                    department.setDepartmentLevel(parent.getDepartmentLevel() + 1);
                    String parentPath = parent.getDepartmentPath() == null ? "/" : parent.getDepartmentPath();
                    department.setDepartmentPath(parentPath.endsWith("/") ? parentPath + parent.getDepartmentId() + "/" : parentPath + "/" + parent.getDepartmentId() + "/");
                }
                if (departments.insertPortalAdmin(department) != 1) throw new IllegalStateException();
                audit.writeSuccess(s, context, "DEPARTMENT_CREATE", "DEPARTMENT", String.valueOf(department.getDepartmentId()));
            }
            else { if (departments.selectById(department.getDepartmentId()) == null) return rollback(s, context, "DEPARTMENT_UPDATE", "NOT_FOUND", "科室不存在。"); if (departments.updateById(department.getDepartmentId(), department.getDepartmentName().trim(), optional(department.getDepartmentDescription(), 500)) != 1) throw new IllegalStateException(); audit.writeSuccess(s, context, "DEPARTMENT_UPDATE", "DEPARTMENT", String.valueOf(department.getDepartmentId())); }
            s.commit(); return AppointmentOperationResult.success("科室资料已保存。");
        } catch (RuntimeException e) { return rollback(s, context, "DEPARTMENT_SAVE", "UPDATE_FAILED", "科室资料保存失败，请稍后重试。"); } finally { close(s); }
    }

    public AppointmentOperationResult deleteDepartment(int id, AuditContext context) {
        SqlSession s = null; try { s = transactions.get(); DepartmentsMapper departments = s.getMapper(DepartmentsMapper.class);
            if (departments.selectById(id) == null) return rollback(s, context, "DEPARTMENT_DELETE", "NOT_FOUND", "科室不存在。");
            if (!departments.selectByParentId(id).isEmpty() || departments.countDoctorsByDepartmentId(id) > 0) return rollback(s, context, "DEPARTMENT_DELETE", "HAS_REFERENCES", "该科室存在下级科室或医生引用，不能删除。");
            if (departments.deletePortalAdmin(id) != 1) throw new IllegalStateException(); audit.writeSuccess(s, context, "DEPARTMENT_DELETE", "DEPARTMENT", String.valueOf(id)); s.commit(); return AppointmentOperationResult.success("科室已删除。");
        } catch (RuntimeException e) { return rollback(s, context, "DEPARTMENT_DELETE", "DELETE_FAILED", "科室删除失败，请稍后重试。"); } finally { close(s); }
    }

    public AppointmentOperationResult saveProfessionalTitle(ProfessionalTitles title, AuditContext context) {
        if (title == null || !validText(title.getTitleName(), 100) || !optionalLength(title.getDescription(), 500)) return failure(context, "TITLE_SAVE", "INVALID_REQUEST", "职称名称或说明不符合要求。");
        SqlSession s = null; try { s = transactions.get(); ProfessionalTitlesMapper titles = s.getMapper(ProfessionalTitlesMapper.class); ProfessionalTitles same = titles.selectByName(title.getTitleName().trim());
            if (same != null && same.getId() != title.getId()) return rollback(s, context, "TITLE_SAVE", "DUPLICATE_ACCOUNT", "职称名称已存在。");
            title.setTitleName(title.getTitleName().trim()); title.setDescription(optional(title.getDescription(), 500));
            if (title.getId() <= 0) { if (titles.insert(title) != 1) throw new IllegalStateException(); audit.writeSuccess(s, context, "TITLE_CREATE", "PROFESSIONAL_TITLE", String.valueOf(title.getId())); }
            else { if (titles.update(title) != 1) return rollback(s, context, "TITLE_UPDATE", "NOT_FOUND", "职称不存在。"); audit.writeSuccess(s, context, "TITLE_UPDATE", "PROFESSIONAL_TITLE", String.valueOf(title.getId())); }
            s.commit(); return AppointmentOperationResult.success("职称资料已保存。");
        } catch (RuntimeException e) { return rollback(s, context, "TITLE_SAVE", "UPDATE_FAILED", "职称资料保存失败，请稍后重试。"); } finally { close(s); }
    }

    public AppointmentOperationResult deleteProfessionalTitle(int id, AuditContext context) {
        SqlSession s = null; try { s = transactions.get(); ProfessionalTitlesMapper titles = s.getMapper(ProfessionalTitlesMapper.class);
            if (titles.selectById(id) == null) return rollback(s, context, "TITLE_DELETE", "NOT_FOUND", "职称不存在。");
            if (titles.countDoctorsByTitleId(id) > 0) return rollback(s, context, "TITLE_DELETE", "HAS_REFERENCES", "该职称仍被医生引用，不能删除。");
            if (titles.delete(id) != 1) throw new IllegalStateException(); audit.writeSuccess(s, context, "TITLE_DELETE", "PROFESSIONAL_TITLE", String.valueOf(id)); s.commit(); return AppointmentOperationResult.success("职称已删除。");
        } catch (RuntimeException e) { return rollback(s, context, "TITLE_DELETE", "DELETE_FAILED", "职称删除失败，请稍后重试。"); } finally { close(s); }
    }

    private static String nextJobNumber(DoctorsMapper doctors) { String value; do { value = "D" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(); } while (doctors.selectByJobNumber(value) != null); return value; }
    private static boolean validDoctor(Doctors doctor) { return doctor != null && validText(doctor.getName(), 100) && doctor.getDepartmentId() > 0 && doctor.getProfessionalTitleId() > 0 && nonNegativeMoney(doctor.getRegistrationFee()) != null && optionalLength(doctor.getIntroduction(), 2000) && optionalLength(doctor.getPhone(), 50) && optionalLength(doctor.getEmail(), 255); }
    private static BigDecimal nonNegativeMoney(String value) { try { BigDecimal result = new BigDecimal(value == null ? "" : value.trim()).setScale(2, RoundingMode.UNNECESSARY); return result.signum() >= 0 ? result : null; } catch (RuntimeException e) { return null; } }
    private static boolean validPassword(String value) { try { PasswordService.validateNewPassword(value); return true; } catch (IllegalArgumentException e) { return false; } }
    private static boolean validText(String value, int max) { return value != null && !value.trim().isEmpty() && value.trim().length() <= max; }
    private static boolean optionalLength(String value, int max) { return value == null || value.trim().length() <= max; }
    private static String optional(String value, int max) { return value == null ? null : (value.trim().isEmpty() ? null : value.trim()); }
    private AppointmentOperationResult rollback(SqlSession s, AuditContext context, String action, String code, String message) { if (s != null) s.rollback(); return failure(context, action, code, message); }
    private AppointmentOperationResult failure(AuditContext context, String action, String code, String message) { if (context != null) audit.recordFailure(context, action, "MASTER_DATA", null); return AppointmentOperationResult.failure(code, message); }
    private static void close(SqlSession s) { if (s != null) s.close(); }
}
