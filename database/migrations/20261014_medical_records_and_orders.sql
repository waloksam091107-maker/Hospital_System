-- Run after 20261014_appointment_slots_and_states.sql on MySQL 8.
-- A medical record is immutable clinical evidence for one completed appointment.
CREATE TABLE IF NOT EXISTS medical_records (
    medical_record_id BIGINT NOT NULL AUTO_INCREMENT,
    appointment_id INT NOT NULL,
    doctor_id INT NOT NULL,
    patient_id INT NOT NULL,
    diagnosis VARCHAR(500) NOT NULL,
    treatment_plan TEXT NULL,
    medical_advice TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (medical_record_id),
    UNIQUE KEY uk_medical_records_appointment (appointment_id),
    KEY idx_medical_records_patient_created (patient_id, created_at),
    KEY idx_medical_records_doctor_created (doctor_id, created_at),
    CONSTRAINT fk_medical_records_appointment FOREIGN KEY (appointment_id)
        REFERENCES appointments (appointment_id),
    CONSTRAINT fk_medical_records_doctor FOREIGN KEY (doctor_id)
        REFERENCES doctors (doctor_id),
    CONSTRAINT fk_medical_records_patient FOREIGN KEY (patient_id)
        REFERENCES patients (patient_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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
    CONSTRAINT fk_medical_orders_record FOREIGN KEY (medical_record_id)
        REFERENCES medical_records (medical_record_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
