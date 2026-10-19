-- MySQL 8.0 migration: normalize appointment slots and lifecycle states.
-- Run against a backed-up database after reviewing every preflight query.
USE hospt;

-- Existing duplicate effective appointments must be resolved before this migration.
SELECT doctor_id, appointment_date, appointment_time, COUNT(*) AS duplicate_count
FROM appointments
WHERE UPPER(status) IN ('BOOKED', 'IN_PROGRESS', 'COMPLETED')
GROUP BY doctor_id, appointment_date, appointment_time
HAVING COUNT(*) > 1;

CREATE TABLE schedule_slots (
    schedule_slot_id BIGINT NOT NULL AUTO_INCREMENT,
    doctor_id INT NOT NULL,
    schedule_date DATE NOT NULL,
    slot_period VARCHAR(20) NOT NULL,
    slot_status VARCHAR(20) NOT NULL DEFAULT 'CLOSED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (schedule_slot_id),
    UNIQUE KEY uk_schedule_slots_doctor_date_period (doctor_id, schedule_date, slot_period),
    KEY idx_schedule_slots_doctor_date (doctor_id, schedule_date),
    CONSTRAINT fk_schedule_slots_doctor FOREIGN KEY (doctor_id) REFERENCES doctors(doctor_id),
    CONSTRAINT chk_schedule_slots_period CHECK (slot_period IN ('AM', 'PM', 'EVENING')),
    CONSTRAINT chk_schedule_slots_status CHECK (slot_status IN ('AVAILABLE', 'BOOKED', 'CLOSED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Backfill all legacy three-period schedule rows. A legacy 0 becomes CLOSED;
-- an effective appointment below overrides CLOSED/AVAILABLE to BOOKED.
INSERT INTO schedule_slots (doctor_id, schedule_date, slot_period, slot_status)
SELECT doctor_id, date, 'AM', CASE WHEN shang = '1' THEN 'AVAILABLE' ELSE 'CLOSED' END FROM dates
UNION ALL
SELECT doctor_id, date, 'PM', CASE WHEN xia = '1' THEN 'AVAILABLE' ELSE 'CLOSED' END FROM dates
UNION ALL
SELECT doctor_id, date, 'EVENING', CASE WHEN wan = '1' THEN 'AVAILABLE' ELSE 'CLOSED' END FROM dates;

UPDATE schedule_slots slot
INNER JOIN appointments appointment
        ON appointment.doctor_id = slot.doctor_id
       AND appointment.appointment_date = slot.schedule_date
       AND appointment.appointment_time = slot.slot_period
SET slot.slot_status = 'BOOKED'
WHERE UPPER(appointment.status) IN ('BOOKED', 'IN_PROGRESS', 'COMPLETED');

ALTER TABLE appointments
    ADD COLUMN schedule_slot_id BIGINT NULL AFTER appointment_time,
    ADD COLUMN previous_appointment_id INT NULL AFTER schedule_slot_id,
    ADD COLUMN started_at DATETIME NULL AFTER status,
    ADD COLUMN completed_at DATETIME NULL AFTER started_at,
    ADD COLUMN cancelled_at DATETIME NULL AFTER completed_at,
    ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER cancelled_at,
    ADD COLUMN version INT NOT NULL DEFAULT 0 AFTER updated_at;

UPDATE appointments appointment
INNER JOIN schedule_slots slot
        ON slot.doctor_id = appointment.doctor_id
       AND slot.schedule_date = appointment.appointment_date
       AND slot.slot_period = appointment.appointment_time
SET appointment.schedule_slot_id = slot.schedule_slot_id;

-- Convert existing lower-case values before replacing the legacy constraint.
UPDATE appointments SET status = UPPER(status);

-- The base schema and the prior minimum-loop migration both create this index.
ALTER TABLE appointments
    DROP INDEX uk_appointments_active_slot,
    DROP COLUMN active_slot_key;

SET @has_status_check := (
    SELECT COUNT(*) FROM information_schema.table_constraints
    WHERE table_schema = DATABASE()
      AND table_name = 'appointments'
      AND constraint_name = 'chk_appointments_status'
      AND constraint_type = 'CHECK'
);
SET @drop_status_check := IF(@has_status_check > 0,
    'ALTER TABLE appointments DROP CHECK chk_appointments_status',
    'SELECT 1');
PREPARE appointment_status_statement FROM @drop_status_check;
EXECUTE appointment_status_statement;
DEALLOCATE PREPARE appointment_status_statement;

ALTER TABLE appointments
    ADD CONSTRAINT fk_appointments_schedule_slot FOREIGN KEY (schedule_slot_id) REFERENCES schedule_slots(schedule_slot_id),
    ADD CONSTRAINT fk_appointments_previous FOREIGN KEY (previous_appointment_id) REFERENCES appointments(appointment_id),
    ADD CONSTRAINT chk_appointments_status CHECK (status IN ('BOOKED', 'RESCHEDULED', 'CANCELLED', 'IN_PROGRESS', 'COMPLETED')),
    ADD COLUMN active_slot_key VARCHAR(128)
        GENERATED ALWAYS AS (
            CASE WHEN status IN ('BOOKED', 'IN_PROGRESS', 'COMPLETED')
                 THEN CONCAT(doctor_id, '|', appointment_date, '|', appointment_time)
                 ELSE NULL END
        ) STORED,
    ADD UNIQUE KEY uk_appointments_active_slot (active_slot_key),
    ADD KEY idx_appointments_schedule_slot (schedule_slot_id),
    ADD KEY idx_appointments_previous (previous_appointment_id);
