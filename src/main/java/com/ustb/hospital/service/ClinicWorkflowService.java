package com.ustb.hospital.service;

import com.ustb.hospital.utils.*;
import org.apache.ibatis.session.SqlSession;
import java.math.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.function.Supplier;
import static com.ustb.hospital.service.ClinicWorkflowStore.*;

/** Serialises clinic mutations by doctor, keeping quota, money, audit and messages atomic. */
public class ClinicWorkflowService {
    private final Supplier<SqlSession> sessions;private final AuditService audits;private final ClinicTime time;
    public ClinicWorkflowService(){this(MybatisUtil::getTransactionSqlSession,new AuditService(),Clock.system(ClinicTime.ZONE));}
    public ClinicWorkflowService(Supplier<SqlSession> sessions,AuditService audits,Clock clock){this.sessions=sessions;this.audits=audits;this.time=new ClinicTime(clock);}
    private interface Work{String run(SqlSession s)throws Exception;}
    private static final class Rejected extends RuntimeException{final String code;Rejected(String c,String m){super(m);code=c;}}
    private static void require(boolean v,String c,String m){if(!v)throw new Rejected(c,m);}
    private AppointmentOperationResult transaction(Work work){
        try(SqlSession s=sessions.get()){try{s.getConnection().setTransactionIsolation(java.sql.Connection.TRANSACTION_READ_COMMITTED);String message=work.run(s);s.commit(true);return AppointmentOperationResult.success(message);}
            catch(Rejected e){s.rollback(true);return AppointmentOperationResult.failure(e.code,e.getMessage());}
            catch(IllegalArgumentException e){s.rollback(true);return AppointmentOperationResult.failure("INVALID_REQUEST",e.getMessage());}
            catch(Exception e){s.rollback(true);java.util.logging.Logger.getLogger(getClass().getName()).log(java.util.logging.Level.WARNING,"Clinic transaction failed (no credentials or medical text)",e);return AppointmentOperationResult.failure("OPERATION_FAILED","操作未完成，请刷新后重试。");}}
    }
    public AppointmentOperationResult book(int patient,int doctor,String day,String requested,AuditContext audit){return transaction(s->{
        String period=period(requested);time.date(day);require(!time.expired(day,period),"SLOT_UNAVAILABLE","该时段已结束。");
        require(lockDoctor(s,doctor),"NOT_FOUND","医生不存在。");
        Map<String,Object> p=one(s,"SELECT balance FROM patients WHERE patient_id=? FOR UPDATE",patient);require(p!=null,"NOT_FOUND","患者不存在。");
        require(AccountSecurityService.isEnabled(s,"DOCTOR",doctor)&&AccountSecurityService.isEnabled(s,"PATIENT",patient),"ACCOUNT_DISABLED","账号已停用，不能预约。");
        Map<String,Object> slot=one(s,"SELECT * FROM schedule_slots WHERE doctor_id=? AND schedule_date=? AND slot_period=? FOR UPDATE",doctor,day,period);
        require(slot!=null,"SLOT_UNAVAILABLE","所选时段没有排班。");int slotId=number(slot,"scheduleSlotId");
        require(one(s,"SELECT appointment_id FROM appointments WHERE patient_id=? AND schedule_slot_id=? AND status IN ('BOOKED','IN_PROGRESS','COMPLETED','NO_SHOW')",patient,slotId)==null,"DUPLICATE_BOOKING","您已预约该时段。");
        reserve(s,slot,doctor,day,period);
        Map<String,Object> d=one(s,"SELECT registration_fee FROM doctors WHERE doctor_id=?",doctor);
        BigDecimal fee=new BigDecimal(str(d,"registrationFee")).setScale(2,RoundingMode.UNNECESSARY);require(fee.signum()>=0,"INVALID_FEE","挂号费无效。");
        BigDecimal balance=new BigDecimal(str(p,"balance"));require(balance.compareTo(fee)>=0,"INSUFFICIENT_BALANCE","余额不足，未创建预约。");
        if(fee.signum()>0)require(update(s,"UPDATE patients SET balance=balance-? WHERE patient_id=? AND balance>=?",fee,patient,fee)==1,"INSUFFICIENT_BALANCE","余额不足。");
        int id=createAppointment(s,patient,doctor,day,period,slotId,number(slot,"nextSequence")+1,null);
        if(fee.signum()>0)update(s,"INSERT INTO balance_transactions(patient_id,appointment_id,transaction_type,amount,balance_after,note) VALUES(?,?,'APPOINTMENT_DEBIT',?,?,?)",patient,id,fee.negate(),balance.subtract(fee),"预约挂号扣费");
        audits.writeSuccess(s,audit,"APPOINTMENTS_BOOK","APPOINTMENT",""+id);appointmentMessages(s,id,patient,doctor,"BOOK","预约成功","您的预约号为 "+(number(slot,"nextSequence")+1)+"，请在预约当天签到候诊。");return "预约成功。";
    });}
    private void reserve(SqlSession s,Map<String,Object> slot,int doctor,String day,String period)throws Exception{
        require("AVAILABLE".equals(str(slot,"slotStatus"))&&number(slot,"occupiedCount")<number(slot,"capacity")&&!leaveBlocked(s,doctor,day,period),"SLOT_UNAVAILABLE","该时段已满、关闭或停诊。");
        require(update(s,"UPDATE schedule_slots SET occupied_count=occupied_count+1,next_sequence=next_sequence+1 WHERE schedule_slot_id=? AND occupied_count<capacity AND slot_status='AVAILABLE'",number(slot,"scheduleSlotId"))==1,"SLOT_UNAVAILABLE","时段已满。");
    }
    private int createAppointment(SqlSession s,int patient,int doctor,String day,String period,int slotId,int seq,Integer previous)throws Exception{
        return (int)insert(s,"INSERT INTO appointments(patient_id,doctor_id,appointment_date,appointment_time,schedule_slot_id,appointment_sequence,previous_appointment_id,status,queue_status) VALUES(?,?,?,?,?,?,?,'BOOKED','NOT_CHECKED_IN')",patient,doctor,day,period,slotId,seq,previous);
    }
    public AppointmentOperationResult patientAction(String action,int patient,int id,String day,String requested,AuditContext audit){return transaction(s->{
        Map<String,Object> initial=one(s,"SELECT doctor_id,patient_id FROM appointments WHERE appointment_id=?",id);require(initial!=null,"NOT_FOUND","预约不存在。");require(number(initial,"patientId")==patient,"FORBIDDEN","不能操作其他患者的预约。");
        int doctor=number(initial,"doctorId");lockDoctor(s,doctor);Map<String,Object> a=one(s,"SELECT * FROM appointments WHERE appointment_id=? FOR UPDATE",id);
        require("BOOKED".equals(str(a,"status")),"INVALID_STATE","只有尚未开始接诊的预约可以取消或改期。");int oldSlot=number(a,"scheduleSlotId");require(oldSlot>0,"SCHEDULE_MISMATCH","预约缺少排班关联，请联系管理员。");
        if("reschedule".equals(action)){
            String period=period(requested);time.date(day);require(!time.expired(day,period),"SLOT_UNAVAILABLE","新时段已结束。");
            require(!(day.equals(str(a,"appointmentDate"))&&period.equals(str(a,"appointmentTime"))),"NO_CHANGE","新时段与原预约相同。");
            require(AccountSecurityService.isEnabled(s,"DOCTOR",doctor),"ACCOUNT_DISABLED","医生账号已停用。");
            Map<String,Object> slot=one(s,"SELECT * FROM schedule_slots WHERE doctor_id=? AND schedule_date=? AND slot_period=? FOR UPDATE",doctor,day,period);require(slot!=null,"SLOT_UNAVAILABLE","新时段没有排班。");
            require(one(s,"SELECT appointment_id FROM appointments WHERE patient_id=? AND schedule_slot_id=? AND status IN ('BOOKED','IN_PROGRESS','COMPLETED','NO_SHOW')",patient,number(slot,"scheduleSlotId"))==null,"DUPLICATE_BOOKING","您已预约该时段。");
            reserve(s,slot,doctor,day,period);int created=createAppointment(s,patient,doctor,day,period,number(slot,"scheduleSlotId"),number(slot,"nextSequence")+1,id);
            update(s,"UPDATE appointments SET status='RESCHEDULED',queue_status='REMOVED',version=version+1 WHERE appointment_id=?",id);
            audits.writeSuccess(s,audit,"APPOINTMENTS_RESCHEDULE_NEW","APPOINTMENT",""+created);appointmentMessages(s,created,patient,doctor,"RESCHEDULE","预约已改期","原预约 "+id+" 已改至 "+day+" "+period+"，请按新预约签到。");
        }else if("cancel".equals(action)){
            update(s,"UPDATE appointments SET status='CANCELLED',queue_status='REMOVED',cancelled_at=?,version=version+1 WHERE appointment_id=?",time.timestamp(),id);appointmentMessages(s,id,patient,doctor,"CANCEL","预约已取消","该预约已取消，当前系统取消预约不自动退款。");
        }else throw new IllegalArgumentException("操作类型无效。");
        require(update(s,"UPDATE schedule_slots SET occupied_count=occupied_count-1 WHERE schedule_slot_id=? AND occupied_count>0",oldSlot)==1,"SCHEDULE_MISMATCH","原时段占用数据异常。");
        queueHistory(s,id,str(a,"queueStatus"),"REMOVED",audit);audits.writeSuccess(s,audit,"APPOINTMENTS_"+action.toUpperCase(Locale.ROOT),"APPOINTMENT",""+id);return "cancel".equals(action)?"预约已取消，当前系统取消预约不自动退款。":"预约已改期。";
    });}
    public List<Map<String,Object>> slots(int doctor,String date){
        try(SqlSession s=sessions.get()){if(date!=null&&!date.isEmpty())time.date(date);boolean enabled=AccountSecurityService.isEnabled(s,"DOCTOR",doctor);
            List<Map<String,Object>> list=date==null||date.isEmpty()?rows(s,"SELECT * FROM schedule_slots WHERE doctor_id=? AND schedule_date>=? ORDER BY schedule_date, schedule_slot_id LIMIT 300",doctor,time.today().toString()):rows(s,"SELECT * FROM schedule_slots WHERE doctor_id=? AND schedule_date=? ORDER BY schedule_slot_id",doctor,date);
            for(Map<String,Object> row:list){boolean blocked=leaveBlocked(s,doctor,str(row,"scheduleDate"),str(row,"slotPeriod"));row.put("leaveBlocked",blocked);row.put("doctorEnabled",enabled);boolean open=enabled&&!blocked&&"AVAILABLE".equals(str(row,"slotStatus"))&&!time.expired(str(row,"scheduleDate"),str(row,"slotPeriod"));row.put("remaining",open?Math.max(0,number(row,"capacity")-number(row,"occupiedCount")):0);}return list;
        }catch(SQLException e){throw new IllegalStateException("号源查询失败。",e);}
    }
    public AppointmentOperationResult capacity(int doctor,long slotId,int value,AuditContext audit){return transaction(s->{
        require(value>=1&&value<=1000,"INVALID_REQUEST","容量必须为 1—1000。");lockDoctor(s,doctor);Map<String,Object> slot=one(s,"SELECT * FROM schedule_slots WHERE schedule_slot_id=? FOR UPDATE",slotId);
        require(slot!=null&&number(slot,"doctorId")==doctor,"FORBIDDEN","不能修改其他医生的号源。");require(!time.date(str(slot,"scheduleDate")).isBefore(time.today()),"PAST_DATE","不能修改历史号源。");
        require(value>=number(slot,"occupiedCount"),"SCHEDULE_IN_USE","容量不能小于已占用数量。");update(s,"UPDATE schedule_slots SET capacity=? WHERE schedule_slot_id=?",value,slotId);audits.writeSuccess(s,audit,"SCHEDULE_CAPACITY","SCHEDULE_SLOT",""+slotId);return "时段容量已更新。";
    });}
    private static String period(String p){String value=AppointmentSlot.fromRequestValue(p);if(value==null)throw new IllegalArgumentException("时段无效。");return value;}
    private void appointmentMessages(SqlSession s,int id,int patient,int doctor,String event,String title,String body){
        NotificationService.enqueue(s,"PATIENT",patient,"appointment:"+id+":"+event,event,title,body,"APPOINTMENT",""+id);
        NotificationService.enqueue(s,"DOCTOR",doctor,"appointment:"+id+":"+event,event,title,"预约编号 "+id+"："+body,"APPOINTMENT",""+id);
    }

    public List<Map<String,Object>> leaves(String role,int actor){
        require("DOCTOR".equals(role)||"ADMIN".equals(role),"FORBIDDEN","无权查看停诊申请。");
        try(SqlSession s=sessions.get()){
            List<Map<String,Object>> list="ADMIN".equals(role)?rows(s,"SELECT * FROM clinic_leave ORDER BY leave_id DESC LIMIT 200"):rows(s,"SELECT * FROM clinic_leave WHERE doctor_id=? ORDER BY leave_id DESC LIMIT 200",actor);
            for(Map<String,Object> l:list){int count=0,slotCount=0;for(Map<String,Object> a:rows(s,"SELECT appointment_time FROM appointments WHERE doctor_id=? AND appointment_date>=? AND appointment_date<=? AND status='BOOKED'",number(l,"doctorId"),str(l,"startDate"),str(l,"endDate")))if(contains(str(l,"periods"),str(a,"appointmentTime")))count++;
                for(Map<String,Object> a:rows(s,"SELECT slot_period FROM schedule_slots WHERE doctor_id=? AND schedule_date>=? AND schedule_date<=?",number(l,"doctorId"),str(l,"startDate"),str(l,"endDate")))if(contains(str(l,"periods"),str(a,"slotPeriod")))slotCount++;
                l.put("affectedAppointments",count);l.put("affectedSlots",slotCount);}return list;
        }catch(SQLException e){throw new IllegalStateException("停诊查询失败。",e);}
    }
    public AppointmentOperationResult leaveAction(String action,String role,int actor,long id,String from,String to,String periods,String reason,AuditContext audit){return transaction(s->{
        String note=reason;
        boolean apply="apply".equals(action),withdraw="withdraw".equals(action);require(apply||withdraw?"DOCTOR".equals(role):"ADMIN".equals(role),"FORBIDDEN","当前身份不能执行此操作。");
        if (withdraw && (note==null||note.trim().isEmpty())) note="医生撤回申请";
        require(note!=null&&!note.trim().isEmpty()&&note.trim().length()<=500,"INVALID_REQUEST","请填写 1—500 字的原因或审核意见。");
        if(apply){
            LocalDate start=time.date(from),end=time.date(to);require(!start.isBefore(time.today())&&!end.isBefore(start)&&!end.isAfter(start.plusDays(30)),"INVALID_REQUEST","停诊范围须为当日或未来，且不超过 31 天。");
            String normalized=normalizePeriods(periods);require(lockDoctor(s,actor),"NOT_FOUND","医生不存在。");
            for(Map<String,Object> old:rows(s,"SELECT periods FROM clinic_leave WHERE doctor_id=? AND status IN ('PENDING','APPROVED') AND start_date<=? AND end_date>=?",actor,to,from))for(String p:normalized.split(","))require(!contains(str(old,"periods"),p),"OVERLAPPING_LEAVE","该日期时段已有待审核或已批准的申请。");
            long created=insert(s,"INSERT INTO clinic_leave(doctor_id,start_date,end_date,periods,reason,status) VALUES(?,?,?,?,?,'PENDING')",actor,from,to,normalized,note.trim());
            for(Map<String,Object> admin:rows(s,"SELECT admin_id FROM admins"))NotificationService.enqueue(s,"ADMIN",number(admin,"adminId"),"leave:"+created+":PENDING","LEAVE_PENDING","有新的停诊申请","医生 "+actor+" 申请 "+from+" 至 "+to+" 停诊，请审核。","LEAVE",""+created);
            audits.writeSuccess(s,audit,"LEAVE_APPLY","LEAVE",""+created);return "停诊申请已提交，审核前正常接诊。";
        }
        Map<String,Object> initial=one(s,"SELECT doctor_id FROM clinic_leave WHERE leave_id=?",id);require(initial!=null,"NOT_FOUND","申请不存在。");int doctor=number(initial,"doctorId");require(!withdraw||actor==doctor,"FORBIDDEN","只能撤回自己的申请。");lockDoctor(s,doctor);
        Map<String,Object> l=one(s,"SELECT * FROM clinic_leave WHERE leave_id=? FOR UPDATE",id);String old=str(l,"status"),next;
        if(withdraw){require("PENDING".equals(old),"INVALID_STATE","只能撤回待审核申请。");next="WITHDRAWN";}
        else if("approve".equals(action)||"reject".equals(action)){require("PENDING".equals(old),"INVALID_STATE","该申请已经处理。");next="approve".equals(action)?"APPROVED":"REJECTED";}
        else if("revoke".equals(action)){require("APPROVED".equals(old),"INVALID_STATE","只能撤销已批准停诊。");next="REVOKED";}else throw new IllegalArgumentException("操作类型无效。");
        List<Map<String,Object>> affected=rows(s,"SELECT * FROM appointments WHERE doctor_id=? AND appointment_date>=? AND appointment_date<=? AND status IN ('BOOKED','IN_PROGRESS') ORDER BY appointment_id FOR UPDATE",doctor,str(l,"startDate"),str(l,"endDate"));
        if("APPROVED".equals(next))for(Map<String,Object> a:affected)if(contains(str(l,"periods"),str(a,"appointmentTime")))require(!"IN_PROGRESS".equals(str(a,"status")),"IN_PROGRESS_EXISTS","范围内有接诊中预约，不能批准停诊。");
        update(s,"UPDATE clinic_leave SET status=?,review_note=? WHERE leave_id=?",next,note.trim(),id);
        if("APPROVED".equals(next)||"REVOKED".equals(next))for(Map<String,Object> a:affected)if(contains(str(l,"periods"),str(a,"appointmentTime"))){
            if("APPROVED".equals(next)&&("CALLED".equals(str(a,"queueStatus"))||"WAITING".equals(str(a,"queueStatus")))){update(s,"UPDATE appointments SET queue_status='NOT_CHECKED_IN',checked_in_at=NULL,called_at=NULL,version=version+1 WHERE appointment_id=?",number(a,"appointmentId"));queueHistory(s,number(a,"appointmentId"),str(a,"queueStatus"),"NOT_CHECKED_IN",audit);}
            NotificationService.enqueue(s,"PATIENT",number(a,"patientId"),"leave:"+id+":"+next+":"+number(a,"appointmentId"),"LEAVE_"+next,"APPROVED".equals(next)?"医生停诊通知":"停诊已撤销","预约 "+number(a,"appointmentId")+("APPROVED".equals(next)?" 所在时段停诊，预约保留，请自行改期或取消；取消不自动退款。":" 的停诊已撤销，请查看预约并按时签到。"),"APPOINTMENT",""+number(a,"appointmentId"));}
        NotificationService.enqueue(s,"DOCTOR",doctor,"leave:"+id+":"+next,"LEAVE_"+next,"停诊申请状态变化","申请 "+id+" 当前状态："+next,"LEAVE",""+id);
        audits.writeSuccess(s,audit,"LEAVE_"+next,"LEAVE",""+id);return "停诊申请已处理，预约历史保留。";
    });}
    private String normalizePeriods(String text) {
        if (text == null || text.isEmpty() || text.length() > 40)
            throw new IllegalArgumentException("请选择有效的停诊时段。");
        // Preserve empty fields: String.split without a negative limit discards
        // trailing commas and can turn a delimiter-only input into an empty set.
        Set<String> values = new LinkedHashSet<>(Arrays.asList(text.split(",", -1)));
        for (String period : values) {
            if (!time.validPeriod(period)) throw new IllegalArgumentException("时段无效。");
        }
        return String.join(",", values);
    }
    private static boolean contains(String periods,String period){return Arrays.asList(periods.split(",")).contains(period);}

    public List<Map<String,Object>> queue(String role,int actor,String date,String requested){
        require("DOCTOR".equals(role)||"PATIENT".equals(role),"FORBIDDEN","无权访问候诊队列。");
        if(date!=null&&!date.isEmpty())time.date(date);if(requested!=null&&!requested.isEmpty())period(requested);
        try(SqlSession s=sessions.get()){
            StringBuilder sql=new StringBuilder("SELECT a.*,p.name AS patient_name FROM appointments a JOIN patients p ON p.patient_id=a.patient_id WHERE a.").append("DOCTOR".equals(role)?"doctor_id":"patient_id").append("=?");List<Object> args=new ArrayList<>();args.add(actor);
            if(date!=null&&!date.isEmpty()){sql.append(" AND a.appointment_date=?");args.add(date);}if(requested!=null&&!requested.isEmpty()){sql.append(" AND a.appointment_time=?");args.add(period(requested));}
            sql.append(" ORDER BY CASE WHEN a.status IN ('BOOKED','IN_PROGRESS') THEN 0 ELSE 1 END,CASE WHEN a.queue_status='CALLED' THEN 0 WHEN a.queue_status='IN_PROGRESS' THEN 1 WHEN a.queue_status='WAITING' THEN 2 ELSE 3 END,COALESCE((SELECT MAX(q.id) FROM queue_history q WHERE q.appointment_id=a.appointment_id AND q.to_status='WAITING'),0),a.checked_in_at,a.appointment_sequence,a.appointment_id LIMIT 300");List<Map<String,Object>> list=rows(s,sql.toString(),args.toArray());
            for(Map<String,Object> a:list)a.put("leaveBlocked",leaveBlocked(s,number(a,"doctorId"),str(a,"appointmentDate"),str(a,"appointmentTime")));return list;
        }catch(SQLException e){throw new IllegalStateException("候诊查询失败。",e);}
    }
    public AppointmentOperationResult queueAction(String action,String role,int actor,int id,String date,String requested,AuditContext audit){return transaction(s->{
        int appointmentId=id;
        boolean check="check-in".equals(action);require(check?"PATIENT".equals(role):"DOCTOR".equals(role),"FORBIDDEN","当前身份不能操作候诊。");
        Map<String,Object> a=null;int doctor;
        if("call-next".equals(action)){doctor=actor;lockDoctor(s,doctor);
            require(one(s,"SELECT appointment_id FROM appointments WHERE doctor_id=? AND (queue_status='CALLED' OR status='IN_PROGRESS')",doctor)==null,"DOCTOR_BUSY","请先处理已叫号或接诊中的患者。");
            time.date(date);String p=period(requested);require(time.date(date).equals(time.today())&&!time.expired(date,p),"INVALID_STATE","只能叫当日尚未结束时段的患者。");require(!leaveBlocked(s,doctor,date,p),"SLOT_UNAVAILABLE","该时段停诊。");
            a=one(s,"SELECT * FROM appointments WHERE doctor_id=? AND appointment_date=? AND appointment_time=? AND status='BOOKED' AND queue_status='WAITING' ORDER BY COALESCE((SELECT MAX(q.id) FROM queue_history q WHERE q.appointment_id=appointments.appointment_id AND q.to_status='WAITING'),0),checked_in_at,appointment_sequence,appointment_id LIMIT 1 FOR UPDATE",doctor,date,p);require(a!=null,"EMPTY_QUEUE","当前没有候诊患者。");appointmentId=number(a,"appointmentId");
        }else{
            Map<String,Object> initial=one(s,"SELECT doctor_id,patient_id FROM appointments WHERE appointment_id=?",appointmentId);require(initial!=null,"NOT_FOUND","预约不存在。");doctor=number(initial,"doctorId");require(check?number(initial,"patientId")==actor:doctor==actor,"FORBIDDEN","预约不属于当前账号。");lockDoctor(s,doctor);a=one(s,"SELECT * FROM appointments WHERE appointment_id=? FOR UPDATE",appointmentId);
        }
        require("BOOKED".equals(str(a,"status")),"INVALID_STATE","该预约不能进行候诊操作。");String old=str(a,"queueStatus"),next;
        if(check){require(time.canCheckIn(str(a,"appointmentDate"),str(a,"appointmentTime")),"INVALID_STATE","仅可在预约当天、时段结束前签到。");require(!leaveBlocked(s,doctor,str(a,"appointmentDate"),str(a,"appointmentTime")),"SLOT_UNAVAILABLE","该时段停诊，请改期或取消。");require("NOT_CHECKED_IN".equals(old),"INVALID_STATE","已签到或当前状态不能重复签到。");next="WAITING";update(s,"UPDATE appointments SET checked_in_at=? WHERE appointment_id=?",time.timestamp(),appointmentId);}
        else if("call-next".equals(action)){next="CALLED";update(s,"UPDATE appointments SET called_at=? WHERE appointment_id=?",time.timestamp(),appointmentId);}
        else if("skip".equals(action)){require("CALLED".equals(old),"INVALID_STATE","只能将已叫号患者标为过号。");next="SKIPPED";}
        else if("requeue".equals(action)){require("SKIPPED".equals(old)&&time.canCheckIn(str(a,"appointmentDate"),str(a,"appointmentTime"))&&!leaveBlocked(s,doctor,str(a,"appointmentDate"),str(a,"appointmentTime")),"INVALID_STATE","只能恢复当日未结束且未停诊的过号预约。");next="WAITING";update(s,"UPDATE appointments SET checked_in_at=?,called_at=NULL WHERE appointment_id=?",time.timestamp(),appointmentId);}
        else if("no-show".equals(action)){require(!leaveBlocked(s,doctor,str(a,"appointmentDate"),str(a,"appointmentTime")),"SLOT_UNAVAILABLE","停诊造成无法就诊，不能标记患者爽约。");require(time.expired(str(a,"appointmentDate"),str(a,"appointmentTime"))&&("NOT_CHECKED_IN".equals(old)||"SKIPPED".equals(old)),"INVALID_STATE","只有时段结束后未签到或过号患者可确认为爽约。");next="NO_SHOW";update(s,"UPDATE appointments SET status='NO_SHOW' WHERE appointment_id=?",appointmentId);}
        else if("start".equals(action)){require("CALLED".equals(old)&&!leaveBlocked(s,doctor,str(a,"appointmentDate"),str(a,"appointmentTime")),"INVALID_STATE","请先叫号，停诊时不能接诊。");require(one(s,"SELECT appointment_id FROM appointments WHERE doctor_id=? AND status='IN_PROGRESS'",doctor)==null,"DOCTOR_BUSY","已有患者接诊中。");next="IN_PROGRESS";update(s,"UPDATE appointments SET status='IN_PROGRESS',started_at=? WHERE appointment_id=?",time.timestamp(),appointmentId);}
        else throw new IllegalArgumentException("操作类型无效。");
        update(s,"UPDATE appointments SET queue_status=?,version=version+1 WHERE appointment_id=?",next,appointmentId);queueHistory(s,appointmentId,old,next,audit);audits.writeSuccess(s,audit,"QUEUE_"+action.replace('-','_').toUpperCase(Locale.ROOT),"APPOINTMENT",""+appointmentId);
        NotificationService.enqueue(s,"PATIENT",number(a,"patientId"),"queue:"+appointmentId+":"+(number(a,"version")+1),"QUEUE_"+next,"候诊状态已更新","预约 "+appointmentId+" 当前状态："+next+("CALLED".equals(next)?"，请前往诊室。":"。"),"APPOINTMENT",""+appointmentId);return "候诊状态已更新。";
    });}
    /** Called from the medical-record transaction: final record, queue, audit, message commit together. */
    public static void finishEncounter(SqlSession s,int id,int patient,int doctor,AuditContext audit)throws Exception{
        Map<String,Object> a=one(s,"SELECT queue_status FROM appointments WHERE appointment_id=?",id);String old=str(a,"queueStatus");update(s,"UPDATE appointments SET queue_status='DONE' WHERE appointment_id=?",id);queueHistory(s,id,old,"DONE",audit);
        NotificationService.enqueue(s,"PATIENT",patient,"appointment:"+id+":FINAL","MEDICAL_RECORD_FINAL","就诊记录已完成","预约 "+id+" 的就诊记录和医嘱已完成，可到就诊记录页面查看。","MEDICAL_RECORD",""+id);
    }
}
