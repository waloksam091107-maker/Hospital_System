package com.ustb.hospital.service;

import com.ustb.hospital.entity.BalanceTransaction;
import com.ustb.hospital.entity.Patients;
import com.ustb.hospital.mapper.BalanceTransactionsMapper;
import com.ustb.hospital.mapper.PatientsMapper;
import com.ustb.hospital.utils.AppointmentOperationResult;
import com.ustb.hospital.utils.MybatisUtil;
import org.apache.ibatis.session.SqlSession;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/** Simulated patient recharge with an append-only balance ledger. */
public class BalanceService {
    public static final BigDecimal MAX_RECHARGE_AMOUNT = new BigDecimal("10000.00");
    private final Supplier<SqlSession> transactions;
    private final Supplier<SqlSession> reads;
    private final AuditService audit;

    public BalanceService() { this(MybatisUtil::getTransactionSqlSession, MybatisUtil::getSqlSession, new AuditService()); }
    public BalanceService(Supplier<SqlSession> transactions, Supplier<SqlSession> reads, AuditService audit) {
        this.transactions = transactions; this.reads = reads; this.audit = audit;
    }

    public AppointmentOperationResult recharge(int patientId, BigDecimal amount, AuditContext context) {
        BigDecimal recharge = normalizedAmount(amount);
        if (patientId <= 0 || recharge == null) return failure(context, "INVALID_AMOUNT", "充值金额应为 0.01 至 10000.00 元。");
        SqlSession session = null;
        try {
            session = transactions.get();
            PatientsMapper patients = session.getMapper(PatientsMapper.class);
            Patients patient = patients.selectByIdForUpdate(patientId);
            if (patient == null) return rollback(session, context, "NOT_FOUND", "当前患者不存在或已被删除。");
            BigDecimal before = balanceOf(patient);
            BigDecimal after = before.add(recharge);
            if (patients.increaseBalance(patientId, recharge) != 1) return rollback(session, context, "RECHARGE_FAILED", "充值失败，请稍后重试。");
            BalanceTransaction transaction = new BalanceTransaction();
            transaction.setPatientId(patientId); transaction.setTransactionType("RECHARGE");
            transaction.setAmount(recharge); transaction.setBalanceAfter(after); transaction.setNote("模拟充值");
            if (session.getMapper(BalanceTransactionsMapper.class).insert(transaction) != 1) throw new IllegalStateException("余额流水写入失败。");
            audit.writeSuccess(session, context, "BALANCE_RECHARGE", "PATIENT", String.valueOf(patientId));
            session.commit();
            return AppointmentOperationResult.success("模拟充值成功。");
        } catch (RuntimeException exception) {
            return rollback(session, context, "RECHARGE_FAILED", "充值失败，请稍后重试。");
        } finally { if (session != null) session.close(); }
    }

    public List<BalanceTransaction> recentTransactions(int patientId, int limit) {
        if (patientId <= 0) return Collections.emptyList();
        int boundedLimit = Math.max(1, Math.min(limit, 100));
        try (SqlSession session = reads.get()) { return session.getMapper(BalanceTransactionsMapper.class).selectRecentByPatient(patientId, boundedLimit); }
    }

    private static BigDecimal normalizedAmount(BigDecimal value) {
        if (value == null) return null;
        try {
            BigDecimal result = value.setScale(2, RoundingMode.UNNECESSARY);
            return result.signum() > 0 && result.compareTo(MAX_RECHARGE_AMOUNT) <= 0 ? result : null;
        } catch (ArithmeticException exception) { return null; }
    }

    private static BigDecimal balanceOf(Patients patient) {
        try { return new BigDecimal(patient.getBalance() == null ? "0" : patient.getBalance()).setScale(2, RoundingMode.UNNECESSARY); }
        catch (RuntimeException exception) { throw new IllegalStateException("患者余额数据无效。", exception); }
    }

    private AppointmentOperationResult rollback(SqlSession session, AuditContext context, String code, String message) {
        if (session != null) session.rollback();
        return failure(context, code, message);
    }
    private AppointmentOperationResult failure(AuditContext context, String code, String message) {
        if (context != null) audit.recordFailure(context, "BALANCE_RECHARGE", "PATIENT", null);
        return AppointmentOperationResult.failure(code, message);
    }
}
