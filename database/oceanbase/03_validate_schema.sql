-- Read-only validation for the final OceanBase MySQL-compatible business schema.
-- Every anomaly result must be empty (or anomaly_count = 0).
-- This file does not import data or repair discrepancies.
USE hospt;

-- All 21 final-schema tables must be present. Legacy tables remain retained.
SELECT expected.table_name, CASE WHEN actual.table_name IS NULL THEN 'MISSING' ELSE 'PRESENT' END AS validation_status
FROM (
    SELECT 'departments' AS table_name UNION ALL SELECT 'professional_titles'
    UNION ALL SELECT 'doctors' UNION ALL SELECT 'patients' UNION ALL SELECT 'admins'
    UNION ALL SELECT 'dates' UNION ALL SELECT 'schedule_slots' UNION ALL SELECT 'appointments'
    UNION ALL SELECT 'medical_records' UNION ALL SELECT 'medical_orders' UNION ALL SELECT 'audit_logs'
    UNION ALL SELECT 'balance_transactions' UNION ALL SELECT 'announcement'
    UNION ALL SELECT 'news' UNION ALL SELECT 'menu' UNION ALL SELECT 'orders'
    UNION ALL SELECT 'account_security' UNION ALL SELECT 'login_throttle'
    UNION ALL SELECT 'notifications' UNION ALL SELECT 'clinic_leave' UNION ALL SELECT 'queue_history'
) expected
LEFT JOIN information_schema.tables actual
  ON actual.table_schema = 'hospt' AND actual.table_name = expected.table_name
ORDER BY expected.table_name;

SELECT table_name, column_name, column_type, is_nullable, column_default, extra
FROM information_schema.columns
WHERE table_schema = 'hospt'
  AND ((table_name = 'schedule_slots' AND column_name IN ('capacity','occupied_count','next_sequence','slot_status'))
       OR (table_name = 'appointments' AND column_name IN ('schedule_slot_id','previous_appointment_id','status','version','appointment_sequence','queue_status','checked_in_at','called_at','active_patient_slot_key'))
       OR (table_name = 'medical_records' AND column_name IN ('record_status', 'version', 'finalized_at'))
       OR (table_name = 'balance_transactions' AND column_name IN ('transaction_type', 'amount', 'balance_after', 'appointment_id'))
       OR (table_name = 'announcement' AND column_name IN ('status', 'published_at', 'updated_at', 'creator_admin_id'))
       OR table_name IN ('account_security','login_throttle','notifications','clinic_leave','queue_history'))
ORDER BY table_name, ordinal_position;

SELECT table_name, index_name, non_unique, seq_in_index, column_name
FROM information_schema.statistics
WHERE table_schema = 'hospt'
  AND ((table_name = 'appointments' AND index_name IN ('uk_appointments_active_patient_slot','uk_appointments_slot_sequence'))
       OR (table_name = 'schedule_slots' AND index_name = 'uk_schedule_slots_doctor_date_period')
       OR (table_name = 'audit_logs' AND index_name = 'idx_audit_logs_created_id')
       OR (table_name = 'balance_transactions' AND index_name IN ('idx_balance_transactions_patient_created', 'idx_balance_transactions_appointment'))
       OR (table_name = 'announcement' AND index_name = 'idx_announcement_status_published')
       OR (table_name = 'notifications' AND index_name IN ('uq_notification_event_recipient','idx_notification_recipient_unread','idx_notification_recipient_created'))
       OR (table_name IN ('account_security','login_throttle') AND index_name='PRIMARY')
       OR (table_name = 'clinic_leave' AND index_name='idx_clinic_leave_doctor_dates')
       OR (table_name = 'queue_history' AND index_name='idx_queue_history_appointment'))
ORDER BY table_name, index_name, seq_in_index;

SELECT table_name, constraint_name, referenced_table_name
FROM information_schema.key_column_usage
WHERE table_schema = 'hospt'
  AND referenced_table_name IS NOT NULL
ORDER BY table_name, constraint_name, ordinal_position;

-- Both results must be empty: the v1 single-appointment-per-slot guard is obsolete.
SELECT table_name,column_name FROM information_schema.columns
WHERE table_schema='hospt' AND table_name='appointments' AND column_name='active_slot_key';
SELECT table_name,index_name FROM information_schema.statistics
WHERE table_schema='hospt' AND table_name='appointments' AND index_name='uk_appointments_active_slot';
SELECT table_name,constraint_name,constraint_type FROM information_schema.table_constraints
WHERE constraint_schema='hospt' AND constraint_type IN ('CHECK','FOREIGN KEY')
ORDER BY table_name,constraint_name;

-- Fresh final-DDL initialization does not need migration history.
-- On an upgraded database, separately review SELECT * FROM hospt.extension_schema_progress;
-- Migration 20261028-five-extensions must have a COMPLETE row.
SELECT 'extension_schema_progress' AS table_name,
       CASE WHEN COUNT(*)=0 THEN 'ABSENT: valid for fresh final DDL. Investigate if upgrading' ELSE 'PRESENT: review COMPLETE phase' END AS validation_status
FROM information_schema.tables WHERE table_schema='hospt' AND table_name='extension_schema_progress';

-- Every following result must be empty, except the explicit anomaly_count result.
SELECT s.schedule_slot_id,s.capacity,s.occupied_count,s.next_sequence,
       (SELECT COUNT(*) FROM appointments a WHERE a.schedule_slot_id=s.schedule_slot_id AND a.status IN ('BOOKED','IN_PROGRESS','COMPLETED','NO_SHOW')) AS actual_occupied
FROM schedule_slots s
WHERE s.capacity NOT BETWEEN 1 AND 1000 OR s.occupied_count NOT BETWEEN 0 AND s.capacity
   OR s.next_sequence<0 OR s.slot_status NOT IN ('AVAILABLE','CLOSED')
   OR s.occupied_count<>(SELECT COUNT(*) FROM appointments a WHERE a.schedule_slot_id=s.schedule_slot_id AND a.status IN ('BOOKED','IN_PROGRESS','COMPLETED','NO_SHOW'))
   OR s.next_sequence<(SELECT COALESCE(MAX(a.appointment_sequence),0) FROM appointments a WHERE a.schedule_slot_id=s.schedule_slot_id);
SELECT a.appointment_id FROM appointments a LEFT JOIN schedule_slots s ON s.schedule_slot_id=a.schedule_slot_id
WHERE s.schedule_slot_id IS NULL OR a.doctor_id<>s.doctor_id OR a.appointment_date<>s.schedule_date OR a.appointment_time<>s.slot_period
   OR a.appointment_sequence IS NULL OR a.appointment_sequence<1;
SELECT schedule_slot_id,appointment_sequence,COUNT(*) AS duplicate_count FROM appointments
GROUP BY schedule_slot_id,appointment_sequence HAVING COUNT(*)>1;
SELECT patient_id,schedule_slot_id,COUNT(*) AS duplicate_count FROM appointments
WHERE status IN ('BOOKED','IN_PROGRESS','COMPLETED','NO_SHOW') GROUP BY patient_id,schedule_slot_id HAVING COUNT(*)>1;
SELECT appointment_id,status,queue_status FROM appointments
WHERE status NOT IN ('BOOKED','RESCHEDULED','CANCELLED','IN_PROGRESS','COMPLETED','NO_SHOW')
   OR queue_status NOT IN ('NOT_CHECKED_IN','WAITING','CALLED','SKIPPED','IN_PROGRESS','DONE','REMOVED','NO_SHOW')
   OR (status='BOOKED' AND queue_status NOT IN ('NOT_CHECKED_IN','WAITING','CALLED','SKIPPED'))
   OR (status='IN_PROGRESS' AND queue_status<>'IN_PROGRESS') OR (status='COMPLETED' AND queue_status<>'DONE')
   OR (status IN ('CANCELLED','RESCHEDULED') AND queue_status<>'REMOVED') OR (status='NO_SHOW' AND queue_status<>'NO_SHOW');
SELECT doctor_id,COUNT(*) AS active_encounters FROM appointments
WHERE status='IN_PROGRESS' OR (status='BOOKED' AND queue_status='CALLED') GROUP BY doctor_id HAVING COUNT(*)>1;
SELECT r.medical_record_id FROM medical_records r LEFT JOIN appointments a ON a.appointment_id=r.appointment_id
WHERE a.appointment_id IS NULL OR r.doctor_id<>a.doctor_id OR r.patient_id<>a.patient_id
   OR r.record_status NOT IN ('DRAFT','FINAL') OR r.version<0
   OR (r.record_status='FINAL' AND a.status<>'COMPLETED');
SELECT o.medical_order_id FROM medical_orders o LEFT JOIN medical_records r ON r.medical_record_id=o.medical_record_id
WHERE r.medical_record_id IS NULL;
SELECT COUNT(*) AS anomaly_count FROM audit_logs WHERE created_at IS NULL;
SELECT transaction_id FROM balance_transactions
WHERE amount=0 OR balance_after<0 OR transaction_type NOT IN ('RECHARGE','APPOINTMENT_DEBIT','REFUND')
   OR (transaction_type='RECHARGE' AND amount<0) OR (transaction_type='APPOINTMENT_DEBIT' AND amount>0);

-- Polymorphic role/account references are enforced by services, not foreign keys.
-- Security/notification history can outlive a deletable unused account.
SELECT account_role,account_id FROM account_security
WHERE account_role NOT IN ('PATIENT','DOCTOR','ADMIN') OR account_id<1 OR failed_attempts<0 OR session_version<0;
SELECT source_ip FROM login_throttle WHERE attempts<0 OR window_start IS NULL;
SELECT id FROM notifications WHERE recipient_role NOT IN ('PATIENT','DOCTOR','ADMIN') OR recipient_id<1 OR created_at IS NULL;
SELECT event_key,recipient_role,recipient_id,COUNT(*) AS duplicate_count FROM notifications
GROUP BY event_key,recipient_role,recipient_id HAVING COUNT(*)>1;
SELECT leave_id FROM clinic_leave WHERE start_date>end_date OR status NOT IN ('PENDING','APPROVED','REJECTED','WITHDRAWN','REVOKED');
SELECT h.id FROM queue_history h LEFT JOIN appointments a ON a.appointment_id=h.appointment_id
WHERE a.appointment_id IS NULL OR h.to_status NOT IN ('NOT_CHECKED_IN','WAITING','CALLED','SKIPPED','IN_PROGRESS','DONE','REMOVED','NO_SHOW');
