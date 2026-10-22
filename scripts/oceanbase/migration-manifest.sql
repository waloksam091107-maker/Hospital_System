-- Source inventory query used by the PowerShell migration and verification scripts.
SHOW TABLES;

SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'hospt'
ORDER BY table_name;
