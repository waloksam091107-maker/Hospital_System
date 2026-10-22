-- Additive migration for the patient balance ledger. It never rewrites historical balances.
USE hospt;

CREATE TABLE IF NOT EXISTS balance_transactions (
    transaction_id BIGINT NOT NULL AUTO_INCREMENT,
    patient_id INT NOT NULL,
    appointment_id INT NULL,
    transaction_type VARCHAR(32) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    balance_after DECIMAL(12,2) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    note VARCHAR(255) NULL,
    PRIMARY KEY (transaction_id),
    KEY idx_balance_transactions_patient_created (patient_id, created_at),
    KEY idx_balance_transactions_appointment (appointment_id),
    CONSTRAINT fk_balance_transactions_patient FOREIGN KEY (patient_id) REFERENCES patients(patient_id),
    CONSTRAINT fk_balance_transactions_appointment FOREIGN KEY (appointment_id) REFERENCES appointments(appointment_id),
    CONSTRAINT chk_balance_transactions_type CHECK (transaction_type IN ('RECHARGE', 'APPOINTMENT_DEBIT', 'REFUND')),
    CONSTRAINT chk_balance_transactions_amount CHECK (amount <> 0)
);

-- Announcement lifecycle extension. Existing rows are preserved and treated as already published.
SET @schema_name := DATABASE();
SET @has_announcement_status := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=@schema_name AND table_name='announcement' AND column_name='status');
SET @add_announcement_status := IF(@has_announcement_status=0, 'ALTER TABLE announcement ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT ''PUBLISHED''', 'SELECT 1');
PREPARE announcement_status_statement FROM @add_announcement_status; EXECUTE announcement_status_statement; DEALLOCATE PREPARE announcement_status_statement;
SET @has_announcement_published_at := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=@schema_name AND table_name='announcement' AND column_name='published_at');
SET @add_announcement_published_at := IF(@has_announcement_published_at=0, 'ALTER TABLE announcement ADD COLUMN published_at VARCHAR(50) NULL', 'SELECT 1');
PREPARE announcement_published_statement FROM @add_announcement_published_at; EXECUTE announcement_published_statement; DEALLOCATE PREPARE announcement_published_statement;
SET @has_announcement_updated_at := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=@schema_name AND table_name='announcement' AND column_name='updated_at');
SET @add_announcement_updated_at := IF(@has_announcement_updated_at=0, 'ALTER TABLE announcement ADD COLUMN updated_at VARCHAR(50) NULL', 'SELECT 1');
PREPARE announcement_updated_statement FROM @add_announcement_updated_at; EXECUTE announcement_updated_statement; DEALLOCATE PREPARE announcement_updated_statement;
SET @has_announcement_creator_admin := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=@schema_name AND table_name='announcement' AND column_name='creator_admin_id');
SET @add_announcement_creator_admin := IF(@has_announcement_creator_admin=0, 'ALTER TABLE announcement ADD COLUMN creator_admin_id INT NULL', 'SELECT 1');
PREPARE announcement_creator_statement FROM @add_announcement_creator_admin; EXECUTE announcement_creator_statement; DEALLOCATE PREPARE announcement_creator_statement;
UPDATE announcement SET status='PUBLISHED', published_at=COALESCE(published_at, creation_time), updated_at=COALESCE(updated_at, creation_time) WHERE status IS NULL OR status='';
SET @has_announcement_index := (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=@schema_name AND table_name='announcement' AND index_name='idx_announcement_status_published');
SET @add_announcement_index := IF(@has_announcement_index=0, 'ALTER TABLE announcement ADD INDEX idx_announcement_status_published (status, published_at)', 'SELECT 1');
PREPARE announcement_index_statement FROM @add_announcement_index; EXECUTE announcement_index_statement; DEALLOCATE PREPARE announcement_index_statement;
