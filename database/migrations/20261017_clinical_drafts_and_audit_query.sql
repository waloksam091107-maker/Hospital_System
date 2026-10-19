-- Select the intended database before running. Compatible with MySQL 8.0.19.
-- DDL commits implicitly; perform the documented preflight and recovery steps.
SET @schema_name := DATABASE();

SET @column_exists := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = @schema_name AND table_name = 'medical_records' AND column_name = 'record_status');
SET @sql := IF(@column_exists = 0,
    'ALTER TABLE medical_records ADD COLUMN record_status VARCHAR(10) NOT NULL DEFAULT ''FINAL''', 'SELECT 1');
PREPARE migration_statement FROM @sql;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;

SET @column_exists := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = @schema_name AND table_name = 'medical_records' AND column_name = 'version');
SET @sql := IF(@column_exists = 0,
    'ALTER TABLE medical_records ADD COLUMN version INT NOT NULL DEFAULT 0', 'SELECT 1');
PREPARE migration_statement FROM @sql;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;

SET @column_exists := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = @schema_name AND table_name = 'medical_records' AND column_name = 'finalized_at');
SET @sql := IF(@column_exists = 0,
    'ALTER TABLE medical_records ADD COLUMN finalized_at TIMESTAMP NULL', 'SELECT 1');
PREPARE migration_statement FROM @sql;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;

SET @diagnosis_required := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = @schema_name AND table_name = 'medical_records'
      AND column_name = 'diagnosis' AND is_nullable = 'NO');
SET @sql := IF(@diagnosis_required = 1,
    'ALTER TABLE medical_records MODIFY COLUMN diagnosis VARCHAR(500) NULL', 'SELECT 1');
PREPARE migration_statement FROM @sql;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;

SET @index_exists := (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = @schema_name AND table_name = 'audit_logs' AND index_name = 'idx_audit_logs_created_id');
SET @sql := IF(@index_exists = 0,
    'ALTER TABLE audit_logs ADD INDEX idx_audit_logs_created_id (created_at, audit_log_id)', 'SELECT 1');
PREPARE migration_statement FROM @sql;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;
