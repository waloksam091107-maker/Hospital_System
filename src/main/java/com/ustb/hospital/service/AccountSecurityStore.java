package com.ustb.hospital.service;

import org.apache.ibatis.session.SqlSession;
import java.sql.*;

/** JDBC statements participate in the supplied MyBatis transaction. Account row is the common lock. */
final class AccountSecurityStore {
    static String table(String role) {
        if ("PATIENT".equals(role)) return "patients";
        if ("DOCTOR".equals(role)) return "doctors";
        if ("ADMIN".equals(role)) return "admins";
        throw new IllegalArgumentException("Invalid account role");
    }
    static String idColumn(String role) { return "PATIENT".equals(role)?"patient_id":"DOCTOR".equals(role)?"doctor_id":"admin_id"; }
    static boolean exists(SqlSession s,String role,int id){
        try(PreparedStatement p=s.getConnection().prepareStatement("SELECT 1 FROM "+table(role)+" WHERE "+idColumn(role)+"=?")){p.setInt(1,id);try(ResultSet r=p.executeQuery()){return r.next();}}
        catch(SQLException e){throw new IllegalStateException("Account query failed",e);}
    }
    static Credentials account(SqlSession s,String role,Object key,boolean byLogin,boolean lock) {
        String login="PATIENT".equals(role)?"name":"DOCTOR".equals(role)?"job_number":"username";
        String display="ADMIN".equals(role)?"username":"name";
        String query="SELECT "+idColumn(role)+","+display+",password,password_hash FROM "+table(role)+" WHERE "+(byLogin?login:idColumn(role))+"=?"+(lock?" FOR UPDATE":"");
        try(PreparedStatement p=s.getConnection().prepareStatement(query)){p.setObject(1,key);try(ResultSet r=p.executeQuery()){return r.next()?new Credentials(r.getInt(1),r.getString(2),r.getString(3),r.getString(4)):null;}}
        catch(SQLException e){throw new IllegalStateException("Account query failed",e);}
    }
    static void ensure(SqlSession s,String role,int id){execute(s,"INSERT INTO account_security(account_role,account_id) SELECT ?,? WHERE NOT EXISTS(SELECT 1 FROM account_security WHERE account_role=? AND account_id=?)",role,id,role,id);}
    static AccountSecurityService.AccountState state(SqlSession s,String role,int id,boolean lock){
        try(PreparedStatement p=s.getConnection().prepareStatement("SELECT enabled,session_version,must_change_password,failed_attempts,failure_window_start,locked_until FROM account_security WHERE account_role=? AND account_id=?"+(lock?" FOR UPDATE":""))){p.setString(1,role);p.setInt(2,id);try(ResultSet r=p.executeQuery()){return r.next()?new AccountSecurityService.AccountState(r.getBoolean(1),r.getLong(2),r.getBoolean(3),r.getInt(4),r.getTimestamp(5),r.getTimestamp(6)):new AccountSecurityService.AccountState(true,0,false,0,null,null);}}
        catch(SQLException e){throw new IllegalStateException("Account control query failed",e);}
    }
    static void replacePassword(SqlSession s,String role,int id,String hash){if(execute(s,"UPDATE "+table(role)+" SET password=NULL,password_hash=? WHERE "+idColumn(role)+"=?",hash,id)!=1)throw new IllegalStateException("Account update failed");}
    static int execute(SqlSession s,String sql,Object...args){try(PreparedStatement p=s.getConnection().prepareStatement(sql)){for(int n=0;n<args.length;n++)p.setObject(n+1,args[n]);return p.executeUpdate();}catch(SQLException e){throw new IllegalStateException("Account write failed",e);}}
    static final class Credentials { final int id;final String name,legacy,hash;Credentials(int id,String name,String legacy,String hash){this.id=id;this.name=name;this.legacy=legacy;this.hash=hash;} }
    private AccountSecurityStore(){}
}
