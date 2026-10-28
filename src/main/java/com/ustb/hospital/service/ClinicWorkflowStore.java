package com.ustb.hospital.service;

import org.apache.ibatis.session.SqlSession;
import java.sql.*;
import java.util.*;

/** Small parameterised JDBC boundary. It never owns or commits a business transaction. */
public final class ClinicWorkflowStore {
    private ClinicWorkflowStore(){}
    public static int update(SqlSession s,String sql,Object...args)throws SQLException{
        try(PreparedStatement p=statement(s,sql,args)){return p.executeUpdate();}
    }
    public static long insert(SqlSession s,String sql,Object...args)throws SQLException{
        try(PreparedStatement p=s.getConnection().prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)){
            bind(p,args);p.executeUpdate();try(ResultSet r=p.getGeneratedKeys()){if(!r.next())throw new SQLException("Missing generated key");return r.getLong(1);}
        }
    }
    public static List<Map<String,Object>> rows(SqlSession s,String sql,Object...args)throws SQLException{
        try(PreparedStatement p=statement(s,sql,args);ResultSet r=p.executeQuery()){
            List<Map<String,Object>> list=new ArrayList<>();ResultSetMetaData m=r.getMetaData();
            while(r.next()){Map<String,Object> row=new LinkedHashMap<>();for(int n=1;n<=m.getColumnCount();n++){Object v=r.getObject(n);if(v instanceof java.util.Date)v=r.getString(n);row.put(camel(m.getColumnLabel(n).toLowerCase(Locale.ROOT)),v);}list.add(row);}return list;
        }
    }
    public static Map<String,Object> one(SqlSession s,String sql,Object...args)throws SQLException{List<Map<String,Object>> r=rows(s,sql,args);return r.isEmpty()?null:r.get(0);}
    public static int number(Map<String,Object> r,String key){Object v=r.get(key);return v==null?0:((Number)v).intValue();}
    public static String str(Map<String,Object> r,String key){Object v=r.get(key);return v==null?null:v.toString();}
    public static boolean lockDoctor(SqlSession s,int id)throws SQLException{return one(s,"SELECT doctor_id FROM doctors WHERE doctor_id=? FOR UPDATE",id)!=null;}
    public static boolean leaveBlocked(SqlSession s,int doctor,String date,String period)throws SQLException{
        // Comma-delimited canonical periods are selected in Java to avoid dialect-specific FIND_IN_SET.
        for(Map<String,Object> r:rows(s,"SELECT periods FROM clinic_leave WHERE doctor_id=? AND status='APPROVED' AND start_date<=? AND end_date>=?",doctor,date,date))
            if(Arrays.asList(str(r,"periods").split(",")).contains(period))return true;
        return false;
    }
    public static void queueHistory(SqlSession s,int id,String from,String to,AuditContext a)throws SQLException{
        update(s,"INSERT INTO queue_history(appointment_id,from_status,to_status,actor_role,actor_id) VALUES(?,?,?,?,?)",id,from,to,a.getActorRole(),a.getActorId());
    }
    private static PreparedStatement statement(SqlSession s,String sql,Object...args)throws SQLException{PreparedStatement p=s.getConnection().prepareStatement(sql);bind(p,args);return p;}
    private static void bind(PreparedStatement p,Object[] args)throws SQLException{for(int n=0;n<args.length;n++)p.setObject(n+1,args[n]);}
    private static String camel(String name){StringBuilder b=new StringBuilder();boolean upper=false;for(char c:name.toCharArray()){if(c=='_')upper=true;else{b.append(upper?Character.toUpperCase(c):c);upper=false;}}return b.toString();}
}
