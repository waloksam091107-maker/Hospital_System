-- New table only; no backfill or modification of historical business data.
CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT NOT NULL AUTO_INCREMENT,
    recipient_role VARCHAR(16) NOT NULL,
    recipient_id INT NOT NULL,
    event_key VARCHAR(191) NOT NULL,
    notification_type VARCHAR(40) NOT NULL,
    title VARCHAR(200) NOT NULL,
    body VARCHAR(2000) NOT NULL,
    target_type VARCHAR(40) NULL,
    target_id VARCHAR(100) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMP NULL DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_notification_event_recipient (event_key, recipient_role, recipient_id),
    KEY idx_notification_recipient_unread (recipient_role, recipient_id, read_at, id),
    KEY idx_notification_recipient_created (recipient_role, recipient_id, created_at, id)
) DEFAULT CHARSET=utf8mb4;
