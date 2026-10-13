-- Optional data for a fresh local demonstration database.
-- Run after hospt_minimal_appointment_schema.sql. INSERT IGNORE preserves existing rows.
USE hospt;

INSERT IGNORE INTO departments (department_id, department_name, department_pid, department_level, department_path, department_discription)
VALUES (1, '内科', NULL, 1, '/1', '演示科室');
INSERT IGNORE INTO professional_titles (id, title_name, description)
VALUES (1, '主治医师', '演示职称');
INSERT IGNORE INTO doctors (doctor_id, job_number, password, name, registration_fee, entry_date, department_id, professional_title_id)
VALUES (1, 'D1001', 'doctor123', '演示医生', 10.00, '2026-01-01', 1, 1);
INSERT IGNORE INTO patients (patient_id, id_card_number, password, name, balance)
VALUES (1, 'DEMO-ID-001', 'patient123', 'patient_demo', 100.00);
INSERT IGNORE INTO admins (admin_id, username, password, name)
VALUES (1, 'admin', 'admin123', '演示管理员');
