-- Optional data for a fresh local demonstration database.
-- Run after hospt_minimal_appointment_schema.sql. INSERT IGNORE preserves existing rows.
USE hospt;

INSERT IGNORE INTO departments (department_id, department_name, department_pid, department_level, department_path, department_discription)
VALUES (1, CONVERT(0xE58685E7A791 USING utf8mb4), NULL, 1, '/1', CONVERT(0xE6BC94E7A4BAE7A791E5AEA4 USING utf8mb4));
INSERT IGNORE INTO professional_titles (id, title_name, description)
VALUES (1, CONVERT(0xE4B8BBE6B2BBE58CBBE5B888 USING utf8mb4), CONVERT(0xE6BC94E7A4BAE8818CE7A7B0 USING utf8mb4));
INSERT IGNORE INTO doctors (doctor_id, job_number, password, name, registration_fee, entry_date, department_id, professional_title_id)
VALUES (1, 'D1001', 'doctor123', CONVERT(0xE6BC94E7A4BAE58CBBE7949F USING utf8mb4), 10.00, '2026-01-01', 1, 1);
INSERT IGNORE INTO patients (patient_id, id_card_number, password, name, balance)
VALUES (1, 'DEMO-ID-001', 'patient123', 'patient_demo', 100.00);
INSERT IGNORE INTO admins (admin_id, username, password, name)
VALUES (1, 'admin', 'admin123', CONVERT(0xE6BC94E7A4BAE7AEA1E79086E59198 USING utf8mb4));
