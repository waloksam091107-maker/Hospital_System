package com.ustb.hospital.database;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

/** Resumable maintenance-window upgrade. DDL is intentionally never claimed atomic. */
public final class ExtensionSchemaUpgrade {
    private static final String MIGRATION="20261028-five-extensions";
    private static final String ACTIVE="'BOOKED','IN_PROGRESS','COMPLETED','NO_SHOW'";
    private static final String STATUSES="'BOOKED','RESCHEDULED','CANCELLED','IN_PROGRESS','COMPLETED','NO_SHOW'";
    private ExtensionSchemaUpgrade() { }

    /** Requires a dedicated, autocommit connection while all application writers are stopped. */
    public static void upgrade(Connection connection) throws SQLException {
        Objects.requireNonNull(connection,"connection");
        if(!connection.getAutoCommit()) throw new SQLException("Upgrade requires a dedicated autocommit maintenance connection.");
        List<String> failures=preflight(connection);
        if(!failures.isEmpty()) throw new SQLException("Upgrade preflight rejected; no changes made: "+String.join("; ",failures));
        execute(connection,"CREATE TABLE IF NOT EXISTS extension_schema_progress (migration_key VARCHAR(64) NOT NULL,phase VARCHAR(40) NOT NULL,completed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,PRIMARY KEY(migration_key,phase))");
        if(done(connection,"COMPLETE")) { verify(connection); return; }
        if(!done(connection,"NEW_TABLES")) { newTables(connection); mark(connection,"NEW_TABLES"); }
        if(!done(connection,"COLUMNS")) { columns(connection); mark(connection,"COLUMNS"); }
        if(!done(connection,"BACKFILL")) backfill(connection);
        if(!done(connection,"NEW_GUARDS")) { guards(connection); mark(connection,"NEW_GUARDS"); }
        if(!done(connection,"RELEASE_OLD_GUARD")) {
            // The two replacement unique indexes and all validation must exist first.
            if(!uniqueGuard(connection,"uk_appointments_active_patient_slot","active_patient_slot_key") || !uniqueGuard(connection,"uk_appointments_slot_sequence","schedule_slot_id,appointment_sequence")) throw new SQLException("Replacement appointment guards are missing or incompatible.");
            if(index(connection,"appointments","uk_appointments_active_slot")) execute(connection,h2(connection)?"DROP INDEX uk_appointments_active_slot":"DROP INDEX uk_appointments_active_slot ON appointments");
            if(column(connection,"appointments","active_slot_key")) execute(connection,"ALTER TABLE appointments DROP COLUMN active_slot_key");
            mark(connection,"RELEASE_OLD_GUARD");
        }
        verify(connection); mark(connection,"COMPLETE");
    }

    /** Read-only diagnostics suitable for checking old or partially upgraded data. */
    public static List<String> preflight(Connection c) throws SQLException {
        List<String> errors=new ArrayList<>();
        for(String name:new String[]{"doctors","patients","admins","dates","appointments","schedule_slots"}) if(table(c,name)==null) errors.add("Missing table "+name);
        if(!errors.isEmpty()) return errors;
        collect(c,errors,"Appointment missing slot: ","SELECT a.appointment_id FROM appointments a LEFT JOIN schedule_slots s ON s.schedule_slot_id=a.schedule_slot_id WHERE a.schedule_slot_id IS NULL OR s.schedule_slot_id IS NULL");
        collect(c,errors,"Appointment/slot mismatch: ","SELECT a.appointment_id FROM appointments a JOIN schedule_slots s ON s.schedule_slot_id=a.schedule_slot_id WHERE a.doctor_id<>s.doctor_id OR a.appointment_date<>s.schedule_date OR a.appointment_time<>s.slot_period OR a.doctor_id IS NULL OR a.appointment_date IS NULL OR a.appointment_time IS NULL");
        collect(c,errors,"Unknown appointment status: ","SELECT appointment_id FROM appointments WHERE status IS NULL OR status NOT IN ("+STATUSES+")");
        collect(c,errors,"Unknown slot period/status: ","SELECT schedule_slot_id FROM schedule_slots WHERE slot_period IS NULL OR slot_period NOT IN ('AM','PM','EVENING') OR slot_status IS NULL OR slot_status NOT IN ('AVAILABLE','CLOSED','BOOKED')");
        collect(c,errors,"Duplicate active patient/slot: ","SELECT schedule_slot_id FROM appointments WHERE status IN ("+ACTIVE+") GROUP BY patient_id,schedule_slot_id HAVING COUNT(*)>1");
        String capacity=column(c,"schedule_slots","capacity")?"s.capacity":"1";
        collect(c,errors,"Slot occupancy exceeds historical/current capacity: ","SELECT s.schedule_slot_id FROM schedule_slots s JOIN appointments a ON a.schedule_slot_id=s.schedule_slot_id AND a.status IN ("+ACTIVE+") GROUP BY s.schedule_slot_id,"+capacity+" HAVING COUNT(*)>"+capacity);
        if(column(c,"schedule_slots","capacity")) collect(c,errors,"Invalid slot capacity: ","SELECT schedule_slot_id FROM schedule_slots WHERE capacity IS NULL OR capacity<1 OR capacity>1000");
        if(column(c,"appointments","appointment_sequence")) {
            collect(c,errors,"Invalid appointment sequence: ","SELECT appointment_id FROM appointments WHERE appointment_sequence IS NOT NULL AND appointment_sequence<1");
            collect(c,errors,"Duplicate slot sequence: ","SELECT schedule_slot_id FROM appointments WHERE appointment_sequence IS NOT NULL GROUP BY schedule_slot_id,appointment_sequence HAVING COUNT(*)>1");
        }
        for(String[] guard:new String[][]{{"uk_appointments_active_patient_slot","active_patient_slot_key"},{"uk_appointments_slot_sequence","schedule_slot_id,appointment_sequence"}})
            if(index(c,"appointments",guard[0]) && !uniqueGuard(c,guard[0],guard[1])) errors.add("Incompatible replacement index: "+guard[0]);
        return errors;
    }

    private static void columns(Connection c) throws SQLException {
        add(c,"schedule_slots","capacity","INT NOT NULL DEFAULT 1");
        add(c,"schedule_slots","occupied_count","INT NOT NULL DEFAULT 0");
        add(c,"schedule_slots","next_sequence","INT NOT NULL DEFAULT 0");
        add(c,"appointments","appointment_sequence","INT NULL");
        add(c,"appointments","queue_status","VARCHAR(25) NOT NULL DEFAULT 'NOT_CHECKED_IN'");
        add(c,"appointments","checked_in_at","TIMESTAMP NULL");
        add(c,"appointments","called_at","TIMESTAMP NULL");
    }

    private static void backfill(Connection c) throws SQLException {
        c.setAutoCommit(false);
        try {
            // Assign only unassigned rows, in stable historical primary-key order.
            Map<Long,Integer> sequences=new HashMap<>();
            try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT schedule_slot_id,MAX(appointment_sequence) FROM appointments GROUP BY schedule_slot_id")) { while(r.next()) sequences.put(r.getLong(1),r.getInt(2)); }
            List<long[]> missing=new ArrayList<>();
            try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT appointment_id,schedule_slot_id FROM appointments WHERE appointment_sequence IS NULL ORDER BY schedule_slot_id,appointment_id")) { while(r.next()) missing.add(new long[]{r.getLong(1),r.getLong(2)}); }
            try(PreparedStatement p=c.prepareStatement("UPDATE appointments SET appointment_sequence=? WHERE appointment_id=? AND appointment_sequence IS NULL")) {
                for(long[] row:missing) { int sequence=sequences.getOrDefault(row[1],0)+1;sequences.put(row[1],sequence);p.setInt(1,sequence);p.setLong(2,row[0]);p.addBatch(); } p.executeBatch();
            }
            execute(c,"UPDATE appointments SET queue_status=CASE WHEN status='IN_PROGRESS' THEN 'IN_PROGRESS' WHEN status='COMPLETED' THEN 'DONE' WHEN status IN ('CANCELLED','RESCHEDULED') THEN 'REMOVED' WHEN status='NO_SHOW' THEN 'NO_SHOW' ELSE 'NOT_CHECKED_IN' END WHERE queue_status='NOT_CHECKED_IN'");
            List<long[]> slots=new ArrayList<>();
            try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT s.schedule_slot_id,s.next_sequence,(SELECT COUNT(*) FROM appointments a WHERE a.schedule_slot_id=s.schedule_slot_id AND a.status IN ("+ACTIVE+")) FROM schedule_slots s")) {while(r.next())slots.add(new long[]{r.getLong(1),r.getLong(2),r.getLong(3)});}
            try(PreparedStatement p=c.prepareStatement("UPDATE schedule_slots SET occupied_count=?,next_sequence=? WHERE schedule_slot_id=?")) {
                for(long[] row:slots) {p.setLong(1,row[2]);p.setLong(2,Math.max(row[1],sequences.getOrDefault(row[0],0)));p.setLong(3,row[0]);p.addBatch();}p.executeBatch();
            }
            execute(c,"UPDATE schedule_slots SET slot_status=CASE WHEN EXISTS (SELECT 1 FROM dates d WHERE d.doctor_id=schedule_slots.doctor_id AND d.`date`=schedule_slots.schedule_date AND ((schedule_slots.slot_period='AM' AND d.shang='0') OR (schedule_slots.slot_period='PM' AND d.xia='0') OR (schedule_slots.slot_period='EVENING' AND d.wan='0'))) THEN 'CLOSED' ELSE 'AVAILABLE' END WHERE slot_status='BOOKED'");
            for(String[] account:new String[][]{{"DOCTOR","doctors","doctor_id"},{"PATIENT","patients","patient_id"},{"ADMIN","admins","admin_id"}}) {
                execute(c,"INSERT INTO account_security(account_role,account_id) SELECT '"+account[0]+"',a."+account[2]+" FROM "+account[1]+" a WHERE NOT EXISTS (SELECT 1 FROM account_security x WHERE x.account_role='"+account[0]+"' AND x.account_id=a."+account[2]+")");
            }
            mark(c,"BACKFILL");c.commit();
        } catch(SQLException | RuntimeException failure) {c.rollback();throw failure;}
        finally {c.setAutoCommit(true);}
    }

    private static void guards(Connection c) throws SQLException {
        if(!column(c,"appointments","active_patient_slot_key")) execute(c,"ALTER TABLE appointments ADD COLUMN active_patient_slot_key VARCHAR(128) GENERATED ALWAYS AS (CASE WHEN status IN ("+ACTIVE+") THEN CONCAT(patient_id,'|',schedule_slot_id) ELSE NULL END)"+(h2(c)?"":" STORED"));
        unique(c,"appointments","uk_appointments_active_patient_slot","active_patient_slot_key");
        unique(c,"appointments","uk_appointments_slot_sequence","schedule_slot_id,appointment_sequence");
        execute(c,h2(c)?"ALTER TABLE appointments ALTER COLUMN appointment_sequence SET NOT NULL":"ALTER TABLE appointments MODIFY COLUMN appointment_sequence INT NOT NULL");
        execute(c,"ALTER TABLE schedule_slots ALTER COLUMN capacity SET DEFAULT 10");
        check(c,"schedule_slots","chk_schedule_slots_capacity","capacity BETWEEN 1 AND 1000 AND occupied_count BETWEEN 0 AND capacity AND next_sequence>=0");
        check(c,"schedule_slots","chk_schedule_slots_status_v2","slot_status IN ('AVAILABLE','CLOSED')");
        check(c,"appointments","chk_appointments_status_v2","status IN ("+STATUSES+")");
        dropCheck(c,"appointments","chk_appointments_status");dropCheck(c,"schedule_slots","chk_schedule_slots_status");
    }

    private static void verify(Connection c) throws SQLException {
        if(!uniqueGuard(c,"uk_appointments_active_patient_slot","active_patient_slot_key") || !uniqueGuard(c,"uk_appointments_slot_sequence","schedule_slot_id,appointment_sequence") || index(c,"appointments","uk_appointments_active_slot")) throw new SQLException("Upgrade guard verification failed; leave application writers stopped.");
        for(String name:new String[]{"notifications","account_security","login_throttle","clinic_leave","queue_history"}) if(table(c,name)==null)throw new SQLException("Upgrade table verification failed: "+name);
        try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT COUNT(*) FROM appointments WHERE appointment_sequence IS NULL OR appointment_sequence<1")) {r.next();if(r.getLong(1)!=0)throw new SQLException("Unassigned historical appointment sequence remains.");}
        try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT COUNT(*) FROM schedule_slots s WHERE occupied_count<>(SELECT COUNT(*) FROM appointments a WHERE a.schedule_slot_id=s.schedule_slot_id AND a.status IN ("+ACTIVE+")) OR next_sequence<(SELECT COALESCE(MAX(appointment_sequence),0) FROM appointments a WHERE a.schedule_slot_id=s.schedule_slot_id)")) {r.next();if(r.getLong(1)!=0)throw new SQLException("Upgrade occupancy/sequence reconciliation failed.");}
    }

    private static void newTables(Connection c) throws SQLException {
        try(InputStream input=ExtensionSchemaUpgrade.class.getResourceAsStream("/db/five-extensions-tables.sql")) {
            if(input==null)throw new SQLException("Missing packaged upgrade tables resource.");
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] buffer=new byte[4096];int length;
            while((length=input.read(buffer))>=0)bytes.write(buffer,0,length);
            String source=new String(bytes.toByteArray(),StandardCharsets.UTF_8).replaceAll("(?m)^\\s*--[^\\r\\n]*","");
            for(String sql:source.split(";"))if(!sql.trim().isEmpty())execute(c,sql);
        }catch(IOException failure){throw new SQLException("Cannot read packaged upgrade tables resource.",failure);}
    }
    private static void collect(Connection c,List<String> errors,String label,String sql)throws SQLException {try(Statement s=c.createStatement();ResultSet r=s.executeQuery(sql)){while(r.next())errors.add(label+r.getString(1));}}
    private static void add(Connection c,String table,String name,String ddl)throws SQLException{if(!column(c,table,name))execute(c,"ALTER TABLE "+table+" ADD COLUMN "+name+" "+ddl);}
    private static void unique(Connection c,String table,String name,String columns)throws SQLException{if(!index(c,table,name))execute(c,"CREATE UNIQUE INDEX "+name+" ON "+table+"("+columns+")");}
    private static void check(Connection c,String table,String name,String condition)throws SQLException{if(!constraint(c,table,name))execute(c,"ALTER TABLE "+table+" ADD CONSTRAINT "+name+" CHECK ("+condition+")");}
    private static void dropCheck(Connection c,String table,String name)throws SQLException{if(constraint(c,table,name))execute(c,"ALTER TABLE "+table+(h2(c)?" DROP CONSTRAINT ":" DROP CHECK ")+name);}
    private static boolean done(Connection c,String phase)throws SQLException{try(PreparedStatement p=c.prepareStatement("SELECT phase FROM extension_schema_progress WHERE migration_key=? AND phase=?")){p.setString(1,MIGRATION);p.setString(2,phase);try(ResultSet r=p.executeQuery()){return r.next();}}}
    private static void mark(Connection c,String phase)throws SQLException{try(PreparedStatement p=c.prepareStatement("INSERT INTO extension_schema_progress(migration_key,phase) SELECT ?,? WHERE NOT EXISTS(SELECT 1 FROM extension_schema_progress WHERE migration_key=? AND phase=?)")){p.setString(1,MIGRATION);p.setString(2,phase);p.setString(3,MIGRATION);p.setString(4,phase);p.executeUpdate();}}
    private static boolean h2(Connection c)throws SQLException{return c.getMetaData().getDatabaseProductName().equalsIgnoreCase("H2");}
    private static String table(Connection c,String name)throws SQLException{try(ResultSet r=c.getMetaData().getTables(c.getCatalog(),null,null,new String[]{"TABLE"})){while(r.next())if(name.equalsIgnoreCase(r.getString("TABLE_NAME")))return r.getString("TABLE_NAME");return null;}}
    private static boolean column(Connection c,String table,String name)throws SQLException{String actual=table(c,table);if(actual==null)return false;try(ResultSet r=c.getMetaData().getColumns(c.getCatalog(),null,actual,null)){while(r.next())if(name.equalsIgnoreCase(r.getString("COLUMN_NAME")))return true;return false;}}
    private static boolean index(Connection c,String table,String name)throws SQLException{String actual=table(c,table);if(actual==null)return false;try(ResultSet r=c.getMetaData().getIndexInfo(c.getCatalog(),null,actual,false,false)){while(r.next())if(name.equalsIgnoreCase(r.getString("INDEX_NAME")))return true;return false;}}
    private static boolean uniqueGuard(Connection c,String name,String expected)throws SQLException{
        String actual=table(c,"appointments");if(actual==null)return false;
        SortedMap<Integer,String> columns=new TreeMap<>();
        try(ResultSet r=c.getMetaData().getIndexInfo(c.getCatalog(),null,actual,false,false)){
            while(r.next())if(name.equalsIgnoreCase(r.getString("INDEX_NAME"))){if(r.getBoolean("NON_UNIQUE"))return false;String column=r.getString("COLUMN_NAME");if(column==null)return false;columns.put(r.getInt("ORDINAL_POSITION"),column.toLowerCase(Locale.ROOT));}
        }
        return expected.equals(String.join(",",columns.values()));
    }
    private static boolean constraint(Connection c,String table,String name)throws SQLException{try(PreparedStatement p=c.prepareStatement("SELECT CONSTRAINT_NAME FROM information_schema.table_constraints WHERE CONSTRAINT_SCHEMA=? AND LOWER(TABLE_NAME)=? AND LOWER(CONSTRAINT_NAME)=?")){p.setString(1,h2(c)?c.getSchema():c.getCatalog());p.setString(2,table.toLowerCase(Locale.ROOT));p.setString(3,name.toLowerCase(Locale.ROOT));try(ResultSet r=p.executeQuery()){return r.next();}}}
    private static void execute(Connection c,String sql)throws SQLException{try(Statement s=c.createStatement()){s.execute(sql);}}

    /** Credentials live only in the child process environment, never command arguments. */
    public static void main(String[] arguments) {
        try {
            if(arguments.length!=0)throw new IllegalArgumentException("Use reviewed upgrade wrapper; command-line credentials/options are forbidden.");
            String target=required("HOSPITAL_UPGRADE_TARGET"),confirm=required("HOSPITAL_UPGRADE_CONFIRM_TARGET");
            if(!target.matches("[A-Za-z][A-Za-z0-9_]{0,63}") || !target.equals(confirm) || !"true".equals(required("HOSPITAL_UPGRADE_MAINTENANCE")))throw new IllegalArgumentException("Explicit matching target and stopped application writers are required.");
            String url=required("HOSPITAL_UPGRADE_DB_URL");
            if(!url.startsWith("jdbc:oceanbase://") || !url.matches("jdbc:oceanbase://[^/]+/"+target+"(?:\\?.*)?"))throw new IllegalArgumentException("JDBC URL must select the confirmed OceanBase database.");
            Class.forName("com.oceanbase.jdbc.Driver");
            try(Connection c=DriverManager.getConnection(url,required("HOSPITAL_UPGRADE_DB_USERNAME"),required("HOSPITAL_UPGRADE_DB_PASSWORD"))) {
                try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT DATABASE()")){if(!r.next() || !target.equals(r.getString(1)))throw new SQLException("Connected database does not match confirmed target.");}
                upgrade(c);System.out.println("Five-extension upgrade verified for "+target+". Keep writers stopped until the new application is ready.");
            }
        } catch(Exception failure) {
            // Do not print driver messages, URLs or stack traces that could contain credentials.
            System.err.println("Upgrade stopped. Keep application writers stopped; use preflight(Connection) and extension_schema_progress to inspect and retry the same target. "+safeDiagnostic(failure));
            System.exit(1);
        }
    }
    private static String required(String key){String value=System.getenv(key);if(value==null||value.trim().isEmpty())throw new IllegalArgumentException("Required upgrade environment is missing.");return value;}
    private static String safeDiagnostic(Exception failure){String message=failure.getMessage();return message!=null && message.startsWith("Upgrade preflight rejected;")?message:failure.getClass().getSimpleName();}
}
