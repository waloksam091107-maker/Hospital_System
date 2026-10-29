package com.ustb.hospital.service;

import com.ustb.hospital.utils.MybatisUtil;
import org.apache.ibatis.session.SqlSession;
import java.math.BigDecimal;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.function.Supplier;

/** Daily registration income from the immutable debit ledger. */
public class IncomeReportService {
    private static final ZoneId ZONE=ZoneId.of("Asia/Shanghai");
    private final Supplier<SqlSession> reads;
    public IncomeReportService(){this(MybatisUtil::getSqlSession);}
    public IncomeReportService(Supplier<SqlSession> reads){this.reads=reads;}
    public Map<String,Object> report(String date){
        LocalDate day;
        try{if(date==null||!date.matches("\\d{4}-\\d{2}-\\d{2}"))throw new IllegalArgumentException();day=LocalDate.parse(date);}
        catch(RuntimeException invalid){throw new IllegalArgumentException("日期必须是有效的 YYYY-MM-DD。",invalid);}
        List<Map<String,Object>> items=new ArrayList<>();BigDecimal total=new BigDecimal("0.00");
        String sql="SELECT t.transaction_id,t.appointment_id,t.patient_id,p.name AS patient_name,a.doctor_id,d.name AS doctor_name,t.amount FROM balance_transactions t LEFT JOIN appointments a ON a.appointment_id=t.appointment_id LEFT JOIN doctors d ON d.doctor_id=a.doctor_id LEFT JOIN patients p ON p.patient_id=t.patient_id WHERE t.transaction_type='APPOINTMENT_DEBIT' AND t.amount<0 AND t.created_at>=? AND t.created_at<? ORDER BY t.created_at,t.transaction_id";
        try(SqlSession session=reads.get();PreparedStatement statement=session.getConnection().prepareStatement(sql)){
            Calendar calendar=Calendar.getInstance(TimeZone.getTimeZone(ZONE));
            statement.setTimestamp(1,Timestamp.from(day.atStartOfDay(ZONE).toInstant()),calendar);
            statement.setTimestamp(2,Timestamp.from(day.plusDays(1).atStartOfDay(ZONE).toInstant()),calendar);
            try(ResultSet row=statement.executeQuery()){
                while(row.next()){
                    BigDecimal amount=row.getBigDecimal("amount").negate().setScale(2);total=total.add(amount);
                    Map<String,Object> item=new LinkedHashMap<>();item.put("transactionId",row.getLong("transaction_id"));item.put("appointmentId",row.getObject("appointment_id"));item.put("patientId",row.getObject("patient_id"));item.put("doctorId",row.getObject("doctor_id"));item.put("patientName",row.getString("patient_name"));item.put("doctorName",row.getString("doctor_name"));item.put("amount",amount);items.add(item);
                }
            }
        }catch(SQLException failure){throw new IllegalStateException("收入报表读取失败。",failure);}
        Map<String,Object> report=new LinkedHashMap<>();report.put("date",date);report.put("timezone",ZONE.getId());report.put("items",items);report.put("total",total);return report;
    }
}
