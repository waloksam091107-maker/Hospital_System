package com.ustb.hospital.service;

import com.ustb.hospital.security.PasswordService;
import com.ustb.hospital.utils.*;
import org.apache.ibatis.session.SqlSession;
import java.sql.*;
import java.time.Clock;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Supplier;

/** Persisted login controls, session revocation and audited account administration. */
public class AccountSecurityService {
    private final Supplier<SqlSession> sessions;
    private final AuditService audit;
    private final Clock clock;
    public AccountSecurityService(){this(MybatisUtil::getTransactionSqlSession,new AuditService(),Clock.systemUTC());}
    public AccountSecurityService(Supplier<SqlSession> sessions,AuditService audit,Clock clock){this.sessions=sessions;this.audit=audit;this.clock=clock;}
    /** Caller locks the base account row first; current read also works under REPEATABLE READ. */
    public static boolean isEnabled(SqlSession session,String role,int id){return id>0&&AccountSecurityStore.exists(session,role,id)&&AccountSecurityStore.state(session,role,id,true).enabled;}
    public AccountState validateSession(String role,int id,long version){try(SqlSession s=sessions.get()){if(id<1||!AccountSecurityStore.exists(s,role,id))return null;AccountState state=AccountSecurityStore.state(s,role,id,true);return state.enabled&&state.version==version?state:null;}}

    public LoginResult authenticate(String role,String user,String password,String sourceIp){
        if(!validRole(role)||user==null||user.length()>100||password==null||password.length()>1024)return LoginResult.failure();
        String ip=sourceIp!=null&&sourceIp.length()<=64?sourceIp:"unknown";
        try(SqlSession s=sessions.get()){
            try{
                Timestamp now=Timestamp.from(clock.instant());
                if(!allowSource(s,ip,now)){s.commit(true);return LoginResult.failure();}
                AccountSecurityStore.Credentials account=AccountSecurityStore.account(s,role,user,true,true);
                if(account==null){s.commit(true);return LoginResult.failure();}
                AccountSecurityStore.ensure(s,role,account.id);AccountState state=AccountSecurityStore.state(s,role,account.id,true);
                if(!state.enabled||(state.lockedUntil!=null&&state.lockedUntil.after(now))){s.commit(true);return LoginResult.failure();}
                if(!PasswordService.matches(password,account.hash,account.legacy)){
                    boolean expired=state.windowStart==null||now.getTime()-state.windowStart.getTime()>=900000L||state.lockedUntil!=null;
                    int failures=expired?1:state.failures+1;Timestamp start=expired?now:state.windowStart;
                    AccountSecurityStore.execute(s,"UPDATE account_security SET failed_attempts=?,failure_window_start=?,locked_until=? WHERE account_role=? AND account_id=?",failures,start,failures>=5?new Timestamp(now.getTime()+900000L):null,role,account.id);
                    s.commit(true);return LoginResult.failure();
                }
                if(PasswordService.needsUpgrade(account.hash)&&password.getBytes(StandardCharsets.UTF_8).length<=72){
                    AccountSecurityStore.replacePassword(s,role,account.id,PasswordService.hash(password));
                    audit.writeSuccess(s,new AuditContext(role,account.id,"POST","/auth/login",ip),"PASSWORD_UPGRADE","ACCOUNT",Integer.toString(account.id));
                }
                AccountSecurityStore.execute(s,"UPDATE account_security SET failed_attempts=0,failure_window_start=NULL,locked_until=NULL WHERE account_role=? AND account_id=?",role,account.id);
                s.commit(true);return new LoginResult(true,account.id,account.name,state.version,state.mustChangePassword);
            }catch(RuntimeException error){s.rollback(true);return LoginResult.failure();}
        }catch(RuntimeException error){return LoginResult.failure();}
    }
    private boolean allowSource(SqlSession s,String ip,Timestamp now){
        AccountSecurityStore.execute(s,"INSERT INTO login_throttle(source_ip,attempts,window_start) VALUES(?,0,?) ON DUPLICATE KEY UPDATE source_ip=source_ip",ip,now);
        try(PreparedStatement p=s.getConnection().prepareStatement("SELECT attempts,window_start FROM login_throttle WHERE source_ip=? FOR UPDATE")){p.setString(1,ip);try(ResultSet r=p.executeQuery()){
            if(!r.next())throw new IllegalStateException("Throttle missing");boolean expired=now.getTime()-r.getTimestamp(2).getTime()>=900000L;int count=expired?1:r.getInt(1)+1;
            AccountSecurityStore.execute(s,"UPDATE login_throttle SET attempts=?,window_start=? WHERE source_ip=?",Math.min(count,61),expired?now:r.getTimestamp(2),ip);return count<=60;
        }}catch(SQLException e){throw new IllegalStateException("Login throttle failed",e);}
    }
    public AppointmentOperationResult setEnabled(String role,int id,boolean enabled,AuditContext context){return manage(role,id,enabled?"ENABLE":"DISABLE",null,context);}
    public AppointmentOperationResult resetPassword(String role,int id,String temporary,AuditContext context){try{PasswordService.validateNewPassword(temporary);}catch(IllegalArgumentException e){return AppointmentOperationResult.failure("INVALID_PASSWORD",e.getMessage());}return manage(role,id,"RESET",temporary,context);}
    public AppointmentOperationResult unlock(String role,int id,AuditContext context){return manage(role,id,"UNLOCK",null,context);}
    private AppointmentOperationResult manage(String role,int id,String operation,String temporary,AuditContext context){
        if(context==null||!"ADMIN".equals(context.getActorRole())||context.getActorId()==null||!("DOCTOR".equals(role)||"PATIENT".equals(role)))return AppointmentOperationResult.failure("FORBIDDEN","无权管理该账号。");
        if(id<1)return AppointmentOperationResult.failure("INVALID_REQUEST","账号编号无效。");
        try(SqlSession s=sessions.get()){
            try{
                if(AccountSecurityStore.account(s,role,id,false,true)==null){s.rollback(true);return AppointmentOperationResult.failure("NOT_FOUND","账号不存在。");}
                AccountSecurityStore.ensure(s,role,id);AccountState before=AccountSecurityStore.state(s,role,id,true);
                if("ENABLE".equals(operation))AccountSecurityStore.execute(s,"UPDATE account_security SET enabled=TRUE WHERE account_role=? AND account_id=?",role,id);
                else if("DISABLE".equals(operation)){
                    if(before.enabled){AccountSecurityStore.execute(s,"UPDATE account_security SET enabled=FALSE,session_version=session_version+1 WHERE account_role=? AND account_id=?",role,id);
                        if("DOCTOR".equals(role))notifyDoctorUnavailable(s,id,before.version+1);}
                }else if("RESET".equals(operation)){
                    AccountSecurityStore.replacePassword(s,role,id,PasswordService.hash(temporary));
                    AccountSecurityStore.execute(s,"UPDATE account_security SET session_version=session_version+1,must_change_password=TRUE,failed_attempts=0,failure_window_start=NULL,locked_until=NULL WHERE account_role=? AND account_id=?",role,id);
                }else AccountSecurityStore.execute(s,"UPDATE account_security SET failed_attempts=0,failure_window_start=NULL,locked_until=NULL WHERE account_role=? AND account_id=?",role,id);
                audit.writeSuccess(s,context,"ACCOUNT_"+operation,"ACCOUNT",Integer.toString(id));s.commit(true);return AppointmentOperationResult.success("账号设置已更新。");
            }catch(RuntimeException e){s.rollback(true);audit.recordFailure(context,"ACCOUNT_"+operation,"ACCOUNT",Integer.toString(id));return AppointmentOperationResult.failure("ACCOUNT_UPDATE_FAILED","账号设置更新失败，请稍后重试。");}
        }catch(RuntimeException e){return AppointmentOperationResult.failure("ACCOUNT_UPDATE_FAILED","账号设置更新失败，请稍后重试。");}
    }
    private void notifyDoctorUnavailable(SqlSession s,int doctorId,long version){
        try(PreparedStatement p=s.getConnection().prepareStatement("SELECT appointment_id,patient_id FROM appointments WHERE doctor_id=? AND status='BOOKED'")){p.setInt(1,doctorId);try(ResultSet r=p.executeQuery()){while(r.next())NotificationService.enqueue(s,"PATIENT",r.getInt(2),"doctor-disabled:"+doctorId+":"+version+":"+r.getInt(1),"ACCOUNT_DISABLED","医生账号暂停接诊","您预约的医生暂时无法接诊，请联系医院协调，或自行改期、取消预约。","APPOINTMENT",Integer.toString(r.getInt(1)));}}
        catch(SQLException e){throw new IllegalStateException("Account notification failed",e);}
    }
    /** Caller has already locked the base account row and owns the password transaction. */
    public static void passwordChanged(SqlSession s,String role,int id){
        AccountSecurityStore.ensure(s,role,id);AccountState state=AccountSecurityStore.state(s,role,id,true);if(!state.enabled)throw new IllegalStateException("Account disabled");
        AccountSecurityStore.execute(s,"UPDATE account_security SET session_version=session_version+1,must_change_password=FALSE,failed_attempts=0,failure_window_start=NULL,locked_until=NULL WHERE account_role=? AND account_id=?",role,id);
    }
    public List<Map<String,Object>> list(String role,int page,int size){
        if(!("DOCTOR".equals(role)||"PATIENT".equals(role))||page<1||page>100000||size<1||size>100)throw new IllegalArgumentException("Invalid query");
        String id=AccountSecurityStore.idColumn(role),table=AccountSecurityStore.table(role);List<Map<String,Object>> rows=new ArrayList<>();
        try(SqlSession s=sessions.get();PreparedStatement p=s.getConnection().prepareStatement("SELECT a."+id+",a.name,COALESCE(c.enabled,TRUE),COALESCE(c.must_change_password,FALSE),c.locked_until FROM "+table+" a LEFT JOIN account_security c ON c.account_role=? AND c.account_id=a."+id+" ORDER BY a."+id+" LIMIT ? OFFSET ?")){
            p.setString(1,role);p.setInt(2,size);p.setInt(3,(page-1)*size);try(ResultSet r=p.executeQuery()){while(r.next()){Map<String,Object> row=new LinkedHashMap<>();row.put("id",r.getInt(1));row.put("name",r.getString(2));row.put("enabled",r.getBoolean(3));row.put("mustChangePassword",r.getBoolean(4));row.put("lockedUntil",r.getTimestamp(5));rows.add(row);}}return rows;
        }catch(SQLException e){throw new IllegalStateException("Account list failed",e);}
    }
    private static boolean validRole(String role){return "PATIENT".equals(role)||"DOCTOR".equals(role)||"ADMIN".equals(role);}
    public static final class AccountState {
        public final boolean enabled,mustChangePassword;public final long version;final int failures;final Timestamp windowStart,lockedUntil;
        AccountState(boolean enabled,long version,boolean mustChangePassword,int failures,Timestamp start,Timestamp locked){this.enabled=enabled;this.version=version;this.mustChangePassword=mustChangePassword;this.failures=failures;this.windowStart=start;this.lockedUntil=locked;}
    }
    public static final class LoginResult {
        private final boolean successful;private final int id;private final String name;private final long version;private final boolean mustChangePassword;
        LoginResult(boolean successful,int id,String name,long version,boolean mustChangePassword){this.successful=successful;this.id=id;this.name=name;this.version=version;this.mustChangePassword=mustChangePassword;}
        static LoginResult failure(){return new LoginResult(false,0,null,0,false);}
        public boolean isSuccessful(){return successful;}public int getId(){return id;}public String getName(){return name;}public long getVersion(){return version;}public boolean isMustChangePassword(){return mustChangePassword;}
    }
}
