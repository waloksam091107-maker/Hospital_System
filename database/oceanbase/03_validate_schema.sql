-- Run as root@test. All result sets must be reviewed before importing application data.
USE hospt;

SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'hospt'
  AND table_name IN ('departments', 'professional_titles', 'doctors', 'patients', 'admins', 'dates',
                     'schedule_slots', 'appointments', 'medical_records', 'medical_orders', 'audit_logs')
ORDER BY table_name;

SELECT table_name, column_name, column_type, is_nullable, column_default, extra
FROM information_schema.columns
WHERE table_schema = 'hospt'
  AND ((table_name = 'appointments' AND column_name IN ('schedule_slot_id', 'previous_appointment_id', 'status', 'version', 'active_slot_key'))
       OR (table_name = 'medical_records' AND column_name IN ('record_status', 'version', 'finalized_at')))
ORDER BY table_name, ordinal_position;

SELECT table_name, index_name, non_unique, seq_in_index, column_name
FROM information_schema.statistics
WHERE table_schema = 'hospt'
  AND ((table_name = 'appointments' AND index_name = 'uk_appointments_active_slot')
       OR (table_name = 'audit_logs' AND index_name = 'idx_audit_logs_created_id'))
ORDER BY table_name, index_name, seq_in_index;

SELECT table_name, constraint_name, referenced_table_name
FROM information_schema.key_column_usage
WHERE table_schema = 'hospt'
  AND referenced_table_name IS NOT NULL
ORDER BY table_name, constraint_name, ordinal_position;
