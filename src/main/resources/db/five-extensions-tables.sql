-- New tables only. Existing business rows and credentials are never replaced.
CREATE TABLE IF NOT EXISTS account_security (
    account_role VARCHAR(20) NOT NULL,
    account_id INT NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    session_version BIGINT NOT NULL DEFAULT 0,
    must_change_password BOOLEAN NOT NULL DEFAULT FALSE,
    failed_attempts INT NOT NULL DEFAULT 0,
    failure_window_start TIMESTAMP NULL,
    locked_until TIMESTAMP NULL,
    PRIMARY KEY (account_role,account_id)
) DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS login_throttle (
    source_ip VARCHAR(64) NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    window_start TIMESTAMP NOT NULL,
    PRIMARY KEY (source_ip)
) DEFAULT CHARSET=utf8mb4;
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
CREATE TABLE IF NOT EXISTS clinic_leave (
    leave_id BIGINT NOT NULL AUTO_INCREMENT,
    doctor_id INT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    periods VARCHAR(40) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    review_note VARCHAR(500) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (leave_id),
    KEY idx_clinic_leave_doctor_dates (doctor_id,start_date,end_date,status),
    CONSTRAINT fk_clinic_leave_doctor FOREIGN KEY (doctor_id) REFERENCES doctors(doctor_id),
    CONSTRAINT chk_clinic_leave_status CHECK (status IN ('PENDING','APPROVED','REJECTED','WITHDRAWN','REVOKED')),
    CONSTRAINT chk_clinic_leave_dates CHECK (start_date <= end_date)
) DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS queue_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    appointment_id INT NOT NULL,
    from_status VARCHAR(25) NULL,
    to_status VARCHAR(25) NOT NULL,
    actor_role VARCHAR(20) NOT NULL,
    actor_id INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_queue_history_appointment (appointment_id,id),
    CONSTRAINT fk_queue_history_appointment FOREIGN KEY (appointment_id) REFERENCES appointments(appointment_id)
) DEFAULT CHARSET=utf8mb4;
