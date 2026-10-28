package com.ustb.hospital.support;

import java.sql.*;

/** Extends disposable legacy fixtures with the current schema; never uses application connections. */
public final class ExtensionTestSchema {
    public static void install(Connection connection) throws SQLException {
        try (Statement s=connection.createStatement()) {
            s.execute("CREATE TABLE IF NOT EXISTS account_security(account_role VARCHAR(20),account_id INT,enabled BOOLEAN DEFAULT TRUE,session_version BIGINT DEFAULT 0,must_change_password BOOLEAN DEFAULT FALSE,failed_attempts INT DEFAULT 0,failure_window_start TIMESTAMP,locked_until TIMESTAMP,PRIMARY KEY(account_role,account_id))");
            s.execute("CREATE TABLE IF NOT EXISTS notifications(id BIGINT AUTO_INCREMENT PRIMARY KEY,recipient_role VARCHAR(20),recipient_id INT,event_key VARCHAR(191),notification_type VARCHAR(40),title VARCHAR(200),body VARCHAR(2000),target_type VARCHAR(40),target_id VARCHAR(100),created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,read_at TIMESTAMP,UNIQUE(event_key,recipient_role,recipient_id))");
            s.execute("CREATE TABLE IF NOT EXISTS clinic_leave(leave_id BIGINT AUTO_INCREMENT PRIMARY KEY,doctor_id INT,start_date DATE,end_date DATE,periods VARCHAR(40),reason VARCHAR(500),status VARCHAR(20),review_note VARCHAR(500),created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            s.execute("CREATE TABLE IF NOT EXISTS queue_history(id BIGINT AUTO_INCREMENT PRIMARY KEY,appointment_id INT,from_status VARCHAR(25),to_status VARCHAR(25),actor_role VARCHAR(20),actor_id INT,created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            if (exists(connection,"appointments")) {
                addColumn(connection,s,"appointments","appointment_sequence","INT");
                addColumn(connection,s,"appointments","queue_status","VARCHAR(25) DEFAULT 'NOT_CHECKED_IN'");
                addColumn(connection,s,"appointments","checked_in_at","TIMESTAMP NULL");
                addColumn(connection,s,"appointments","called_at","TIMESTAMP NULL");
                addColumn(connection,s,"appointments","updated_at","TIMESTAMP DEFAULT CURRENT_TIMESTAMP");
            }
            if (exists(connection,"schedule_slots")) {
                addColumn(connection,s,"schedule_slots","capacity","INT DEFAULT 10");
                addColumn(connection,s,"schedule_slots","occupied_count","INT DEFAULT 0");
                addColumn(connection,s,"schedule_slots","next_sequence","INT DEFAULT 0");
            }
        }
    }
    private static boolean exists(Connection c,String table)throws SQLException {try(ResultSet r=c.getMetaData().getTables(c.getCatalog(),null,null,new String[]{"TABLE"})){while(r.next())if(table.equalsIgnoreCase(r.getString("TABLE_NAME")))return true;return false;}}
    private static void addColumn(Connection c,Statement s,String table,String column,String declaration)throws SQLException {
        try(ResultSet r=c.getMetaData().getColumns(c.getCatalog(),null,null,null)){while(r.next())if(table.equalsIgnoreCase(r.getString("TABLE_NAME"))&&column.equalsIgnoreCase(r.getString("COLUMN_NAME")))return;}
        s.execute("ALTER TABLE "+table+" ADD COLUMN "+column+" "+declaration);
    }
    private ExtensionTestSchema(){}
}
