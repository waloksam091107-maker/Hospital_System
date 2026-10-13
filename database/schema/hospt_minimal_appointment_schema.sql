-- Fresh MySQL 8.0 schema for the minimum appointment loop.
-- Use this file only for a new local database. It does not replace the legacy migration.
CREATE DATABASE IF NOT EXISTS hospt CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS professional_titles (
    id INT NOT NULL AUTO_INCREMENT,
    title_name VARCHAR(100) NOT NULL,
    description VARCHAR(500) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_professional_titles_name (title_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS doctors (
    doctor_id INT NOT NULL AUTO_INCREMENT,
    job_number VARCHAR(50) NOT NULL,
    password VARCHAR(255) NOT NULL,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS patients (
    patient_id INT NOT NULL AUTO_INCREMENT,
    id_card_number VARCHAR(50) NULL,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    avatar VARCHAR(500) NULL,
    phone VARCHAR(50) NULL,
    email VARCHAR(255) NULL,
    balance DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    PRIMARY KEY (patient_id),
    UNIQUE KEY uk_patients_name (name),
    UNIQUE KEY uk_patients_id_card_number (id_card_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS admins (
    admin_id INT NOT NULL AUTO_INCREMENT,
    username VARCHAR(100) NOT NULL,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(100) NULL,
    avatar VARCHAR(500) NULL,
    phone VARCHAR(50) NULL,
    email VARCHAR(255) NULL,
    PRIMARY KEY (admin_id),
    UNIQUE KEY uk_admins_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS dates (
    date_id INT NOT NULL AUTO_INCREMENT,
    doctor_id INT NOT NULL,
    doctor_name VARCHAR(100) NOT NULL,
    date DATE NOT NULL,
    shang CHAR(1) NOT NULL DEFAULT '0',
    xia CHAR(1) NOT NULL DEFAULT '0',
    wan CHAR(1) NOT NULL DEFAULT '0',
    PRIMARY KEY (date_id),
    UNIQUE KEY uk_dates_doctor_date (doctor_id, date),
    CONSTRAINT fk_dates_doctor FOREIGN KEY (doctor_id) REFERENCES doctors(doctor_id),
    CONSTRAINT chk_dates_shang CHECK (shang IN ('0', '1')),
    CONSTRAINT chk_dates_xia CHECK (xia IN ('0', '1')),
    CONSTRAINT chk_dates_wan CHECK (wan IN ('0', '1'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS appointments (
    appointment_id INT NOT NULL AUTO_INCREMENT,
    patient_id INT NOT NULL,
    doctor_id INT NOT NULL,
    appointment_date DATE NOT NULL,
    appointment_time VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    active_slot_key VARCHAR(128) GENERATED ALWAYS AS (
        CASE WHEN status IN ('booked', 'completed')
             THEN CONCAT(doctor_id, '|', appointment_date, '|', appointment_time)
             ELSE NULL END
    ) STORED,
    PRIMARY KEY (appointment_id),
    UNIQUE KEY uk_appointments_active_slot (active_slot_key),
    KEY idx_appointments_patient (patient_id),
    KEY idx_appointments_doctor (doctor_id),
    CONSTRAINT fk_appointments_patient FOREIGN KEY (patient_id) REFERENCES patients(patient_id),
    CONSTRAINT fk_appointments_doctor FOREIGN KEY (doctor_id) REFERENCES doctors(doctor_id),
    CONSTRAINT chk_appointments_slot CHECK (appointment_time IN ('AM', 'PM', 'EVENING')),
    CONSTRAINT chk_appointments_status CHECK (status IN ('booked', 'cancelled', 'completed'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
