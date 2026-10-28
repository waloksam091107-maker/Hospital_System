-- OceanBase Community Edition, MySQL-compatible tenant, final business schema.
-- This file creates an empty target only. Historical data is imported separately.

CREATE DATABASE IF NOT EXISTS hospt;
USE hospt;

CREATE TABLE IF NOT EXISTS departments (
    department_id INT NOT NULL AUTO_INCREMENT,
    department_name VARCHAR(100) NOT NULL,
    department_pid INT NULL,
    department_level INT NOT NULL DEFAULT 1,
    department_path VARCHAR(255) NULL,
    department_discription VARCHAR(500) NULL,
    PRIMARY KEY (department_id),
    UNIQUE KEY uk_departments_name (department_name)
);

CREATE TABLE IF NOT EXISTS professional_titles (
    id INT NOT NULL AUTO_INCREMENT,
    title_name VARCHAR(100) NOT NULL,
    description VARCHAR(500) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_professional_titles_name (title_name)
);

CREATE TABLE IF NOT EXISTS doctors (
    doctor_id INT NOT NULL AUTO_INCREMENT,
    job_number VARCHAR(50) NOT NULL,
    password VARCHAR(255) NULL,
    password_hash VARCHAR(60) NULL,
    name VARCHAR(100) NOT NULL,
    avatar VARCHAR(500) NULL,
    phone VARCHAR(50) NULL,
    email VARCHAR(255) NULL,
    introduction TEXT NULL,
    registration_fee DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    entry_date VARCHAR(30) NULL,
    department_id INT NOT NULL,
    professional_title_id INT NOT NULL,
    PRIMARY KEY (doctor_id),
    UNIQUE KEY uk_doctors_job_number (job_number),
    CONSTRAINT fk_doctors_department FOREIGN KEY (department_id) REFERENCES departments(department_id),
    CONSTRAINT fk_doctors_title FOREIGN KEY (professional_title_id) REFERENCES professional_titles(id)
);

CREATE TABLE IF NOT EXISTS patients (
    patient_id INT NOT NULL AUTO_INCREMENT,
    id_card_number VARCHAR(50) NULL,
    password VARCHAR(255) NULL,
    password_hash VARCHAR(60) NULL,
    name VARCHAR(100) NOT NULL,
    avatar VARCHAR(500) NULL,
    phone VARCHAR(50) NULL,
    email VARCHAR(255) NULL,
    balance DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    PRIMARY KEY (patient_id),
    UNIQUE KEY uk_patients_name (name),
    UNIQUE KEY uk_patients_id_card_number (id_card_number)
);

CREATE TABLE IF NOT EXISTS admins (
    admin_id INT NOT NULL AUTO_INCREMENT,
    username VARCHAR(100) NOT NULL,
    password VARCHAR(255) NULL,
    password_hash VARCHAR(60) NULL,
    name VARCHAR(100) NULL,
    avatar VARCHAR(500) NULL,
    phone VARCHAR(50) NULL,
    email VARCHAR(255) NULL,
    PRIMARY KEY (admin_id),
    UNIQUE KEY uk_admins_username (username)
);

CREATE TABLE IF NOT EXISTS dates (
    date_id INT NOT NULL AUTO_INCREMENT,
    doctor_id INT NOT NULL,
    doctor_name VARCHAR(100) NOT NULL,
    `date` DATE NOT NULL,
    shang CHAR(1) NOT NULL DEFAULT '0',
    xia CHAR(1) NOT NULL DEFAULT '0',
    wan CHAR(1) NOT NULL DEFAULT '0',
    PRIMARY KEY (date_id),
    UNIQUE KEY uk_dates_doctor_date (doctor_id, `date`),
    CONSTRAINT fk_dates_doctor FOREIGN KEY (doctor_id) REFERENCES doctors(doctor_id),
    CONSTRAINT chk_dates_shang CHECK (shang IN ('0', '1')),
    CONSTRAINT chk_dates_xia CHECK (xia IN ('0', '1')),
    CONSTRAINT chk_dates_wan CHECK (wan IN ('0', '1'))
);

CREATE TABLE IF NOT EXISTS schedule_slots (
    schedule_slot_id BIGINT NOT NULL AUTO_INCREMENT,
    doctor_id INT NOT NULL,
    schedule_date DATE NOT NULL,
    slot_period VARCHAR(20) NOT NULL,
    slot_status VARCHAR(20) NOT NULL DEFAULT 'CLOSED',
    capacity INT NOT NULL DEFAULT 10,
    occupied_count INT NOT NULL DEFAULT 0,
    next_sequence INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (schedule_slot_id),
    UNIQUE KEY uk_schedule_slots_doctor_date_period (doctor_id, schedule_date, slot_period),
    KEY idx_schedule_slots_doctor_date (doctor_id, schedule_date),
    CONSTRAINT fk_schedule_slots_doctor FOREIGN KEY (doctor_id) REFERENCES doctors(doctor_id),
    CONSTRAINT chk_schedule_slots_period CHECK (slot_period IN ('AM', 'PM', 'EVENING')),
    CONSTRAINT chk_schedule_slots_status CHECK (slot_status IN ('AVAILABLE', 'CLOSED')),
    CONSTRAINT chk_schedule_slots_capacity CHECK (capacity BETWEEN 1 AND 1000 AND occupied_count BETWEEN 0 AND capacity AND next_sequence >= 0)
);

CREATE TABLE IF NOT EXISTS appointments (
    appointment_id INT NOT NULL AUTO_INCREMENT,
    patient_id INT NOT NULL,
    doctor_id INT NOT NULL,
    appointment_date DATE NOT NULL,
    appointment_time VARCHAR(20) NOT NULL,
    schedule_slot_id BIGINT NULL,
    previous_appointment_id INT NULL,
    status VARCHAR(20) NOT NULL,
    started_at DATETIME NULL,
    completed_at DATETIME NULL,
    cancelled_at DATETIME NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version INT NOT NULL DEFAULT 0,
    appointment_sequence INT NOT NULL,
    queue_status VARCHAR(25) NOT NULL DEFAULT 'NOT_CHECKED_IN',
    checked_in_at TIMESTAMP NULL,
    called_at TIMESTAMP NULL,
    active_patient_slot_key VARCHAR(128) GENERATED ALWAYS AS (
        CASE WHEN status IN ('BOOKED', 'IN_PROGRESS', 'COMPLETED', 'NO_SHOW')
             THEN CONCAT(patient_id, '|', schedule_slot_id)
             ELSE NULL END
    ) STORED,
    PRIMARY KEY (appointment_id),
    UNIQUE KEY uk_appointments_active_patient_slot (active_patient_slot_key),
    UNIQUE KEY uk_appointments_slot_sequence (schedule_slot_id, appointment_sequence),
    KEY idx_appointments_patient (patient_id),
    KEY idx_appointments_doctor (doctor_id),
    KEY idx_appointments_schedule_slot (schedule_slot_id),
    KEY idx_appointments_previous (previous_appointment_id),
    CONSTRAINT fk_appointments_patient FOREIGN KEY (patient_id) REFERENCES patients(patient_id),
    CONSTRAINT fk_appointments_doctor FOREIGN KEY (doctor_id) REFERENCES doctors(doctor_id),
    CONSTRAINT fk_appointments_schedule_slot FOREIGN KEY (schedule_slot_id) REFERENCES schedule_slots(schedule_slot_id),
    CONSTRAINT fk_appointments_previous FOREIGN KEY (previous_appointment_id) REFERENCES appointments(appointment_id),
    CONSTRAINT chk_appointments_slot CHECK (appointment_time IN ('AM', 'PM', 'EVENING')),
    CONSTRAINT chk_appointments_status CHECK (status IN ('BOOKED', 'RESCHEDULED', 'CANCELLED', 'IN_PROGRESS', 'COMPLETED', 'NO_SHOW'))
);

CREATE TABLE IF NOT EXISTS medical_records (
    medical_record_id BIGINT NOT NULL AUTO_INCREMENT,
    appointment_id INT NOT NULL,
    doctor_id INT NOT NULL,
    patient_id INT NOT NULL,
    diagnosis VARCHAR(500) NULL,
    treatment_plan TEXT NULL,
    medical_advice TEXT NULL,
    record_status VARCHAR(10) NOT NULL DEFAULT 'FINAL',
    version INT NOT NULL DEFAULT 0,
    finalized_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (medical_record_id),
    UNIQUE KEY uk_medical_records_appointment (appointment_id),
    KEY idx_medical_records_patient_created (patient_id, created_at),
    KEY idx_medical_records_doctor_created (doctor_id, created_at),
    CONSTRAINT fk_medical_records_appointment FOREIGN KEY (appointment_id) REFERENCES appointments(appointment_id),
    CONSTRAINT fk_medical_records_doctor FOREIGN KEY (doctor_id) REFERENCES doctors(doctor_id),
    CONSTRAINT fk_medical_records_patient FOREIGN KEY (patient_id) REFERENCES patients(patient_id),
    CONSTRAINT chk_medical_records_status CHECK (record_status IN ('DRAFT', 'FINAL'))
);

CREATE TABLE IF NOT EXISTS medical_orders (
    medical_order_id BIGINT NOT NULL AUTO_INCREMENT,
    medical_record_id BIGINT NOT NULL,
    item_name VARCHAR(200) NOT NULL,
    dosage VARCHAR(100) NULL,
    frequency VARCHAR(100) NULL,
    quantity INT NULL,
    instruction_text VARCHAR(500) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (medical_order_id),
    KEY idx_medical_orders_record (medical_record_id),
    CONSTRAINT fk_medical_orders_record FOREIGN KEY (medical_record_id) REFERENCES medical_records(medical_record_id) ON DELETE CASCADE
);

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
    KEY idx_audit_logs_action_created (action, created_at),
    KEY idx_audit_logs_created_id (created_at, audit_log_id)
);

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

-- Legacy presentation and order tables remain available to the current mapper set.
CREATE TABLE IF NOT EXISTS announcement (
    announcement_id INT NOT NULL AUTO_INCREMENT,
    title VARCHAR(255) NULL,
    content TEXT NULL,
    creation_time VARCHAR(50) NULL,
    creator VARCHAR(100) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED',
    published_at VARCHAR(50) NULL,
    updated_at VARCHAR(50) NULL,
    creator_admin_id INT NULL,
    KEY idx_announcement_status_published (status, published_at),
    PRIMARY KEY (announcement_id)
);

CREATE TABLE IF NOT EXISTS news (
    news_id INT NOT NULL AUTO_INCREMENT,
    title VARCHAR(255) NULL,
    content TEXT NULL,
    creation_time VARCHAR(50) NULL,
    creator VARCHAR(100) NULL,
    news_img VARCHAR(500) NULL,
    PRIMARY KEY (news_id)
);

CREATE TABLE IF NOT EXISTS menu (
    id VARCHAR(64) NOT NULL,
    name VARCHAR(255) NULL,
    price VARCHAR(64) NULL,
    fenlei VARCHAR(100) NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS orders (
    order_id INT NOT NULL AUTO_INCREMENT,
    doctor_id INT NOT NULL,
    doctor_name VARCHAR(100) NULL,
    patient_id INT NOT NULL,
    patient_name VARCHAR(100) NULL,
    order_type VARCHAR(100) NULL,
    order_details TEXT NULL,
    status VARCHAR(50) NULL,
    PRIMARY KEY (order_id),
    KEY idx_orders_doctor (doctor_id),
    KEY idx_orders_patient (patient_id)
);

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
