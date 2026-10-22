package com.ustb.hospital.database;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/** Guards the additive ledger schema required by the simulated recharge flow. */
public class BalanceTransactionSchemaContractTest {
    @Test
    public void migrationAndFreshOceanBaseSchemaDeclareTheBalanceLedger() throws Exception {
        String migration = read("database/migrations/20261022_secure_portal_and_balance_transactions.sql");
        String schema = read("database/oceanbase/01_hospt_schema.sql");

        for (String ddl : new String[]{migration, schema}) {
            String normalized = ddl.toUpperCase();
            assertTrue(normalized.contains("BALANCE_TRANSACTIONS"));
            assertTrue(normalized.contains("TRANSACTION_TYPE"));
            assertTrue(normalized.contains("BALANCE_AFTER"));
            assertTrue(normalized.contains("DECIMAL(12,2)"));
            assertTrue(normalized.contains("IDX_BALANCE_TRANSACTIONS_PATIENT_CREATED"));
            assertTrue(normalized.contains("IDX_BALANCE_TRANSACTIONS_APPOINTMENT"));
        }
    }

    @Test
    public void registersLedgerMapperInMyBatisConfiguration() throws Exception {
        assertTrue(Files.isRegularFile(Paths.get("src/main/java/com/ustb/hospital/entity/BalanceTransaction.java")));
        assertTrue(Files.isRegularFile(Paths.get("src/main/java/com/ustb/hospital/mapper/BalanceTransactionsMapper.java")));
        assertTrue(Files.isRegularFile(Paths.get("src/main/resources/com/ustb/hospital/mapper/BalanceTransactionsMapper.xml")));
        assertTrue(read("src/main/resources/mybatis-config.xml").contains(
                "com/ustb/hospital/mapper/BalanceTransactionsMapper.xml"));
    }

    private String read(String path) throws Exception {
        Path file = Paths.get(path);
        return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
    }
}
