-- Run after the appointment and medical-record migrations on MySQL 8.0+.
-- Existing plaintext remains only until the account next authenticates successfully.
-- MySQL 8.0.19 does not support ADD COLUMN IF NOT EXISTS, so use
-- information_schema to make the migration safely repeatable.
SET @schema_name := DATABASE();

SET @column_exists := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = @schema_name AND table_name = 'doctors' AND column_name = 'password_hash');
SET @sql := IF(@column_exists = 0,
    'ALTER TABLE doctors ADD COLUMN password_hash VARCHAR(60) NULL AFTER password', 'SELECT 1');
PREPARE add_password_hash FROM @sql;
EXECUTE add_password_hash;
DEALLOCATE PREPARE add_password_hash;
ALTER TABLE doctors MODIFY COLUMN password VARCHAR(255) NULL;

SET @column_exists := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = @schema_name AND table_name = 'patients' AND column_name = 'password_hash');
SET @sql := IF(@column_exists = 0,
    'ALTER TABLE patients ADD COLUMN password_hash VARCHAR(60) NULL AFTER password', 'SELECT 1');
PREPARE add_password_hash FROM @sql;
EXECUTE add_password_hash;
DEALLOCATE PREPARE add_password_hash;
ALTER TABLE patients MODIFY COLUMN password VARCHAR(255) NULL;

SET @column_exists := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = @schema_name AND table_name = 'admins' AND column_name = 'password_hash');
SET @sql := IF(@column_exists = 0,
    'ALTER TABLE admins ADD COLUMN password_hash VARCHAR(60) NULL AFTER password', 'SELECT 1');
PREPARE add_password_hash FROM @sql;
EXECUTE add_password_hash;
DEALLOCATE PREPARE add_password_hash;
ALTER TABLE admins MODIFY COLUMN password VARCHAR(255) NULL;

CREATE TABLE IF NOT EXISTS audit_logs (
    audit_log_id BIGINT NOT NULL AUTO_INCREMENT,
    actor_role VARCHAR(20) NULL,
    actor_id INT NULL,
    action VARCHAR(80) NOT NULL,
    target_type VARCHAR(80) NULL,
    target_id VARCHAR(80) NULL,
    outcome VARCHAR(20) NOT NULL,
    request_method VARCHAR(10) NULL,
    request_path VARCHAR(255) NULL,
    remote_address VARCHAR(64) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (audit_log_id),
    KEY idx_audit_logs_actor_created (actor_role, actor_id, created_at),
    KEY idx_audit_logs_action_created (action, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
