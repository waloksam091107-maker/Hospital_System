package com.ustb.hospital.service;

import java.time.*;
import java.time.format.*;

/** All clinic deadlines use the business zone, independent of host/container timezone. */
public final class ClinicTime {
    public static final ZoneId ZONE=ZoneId.of("Asia/Shanghai");
    private final Clock clock;
    public ClinicTime(Clock clock){this.clock=clock.withZone(ZONE);}
    public static ClinicTime system(){return new ClinicTime(Clock.system(ZONE));}
    public LocalDate today(){return LocalDate.now(clock);}
    public java.sql.Timestamp timestamp(){return java.sql.Timestamp.valueOf(LocalDateTime.now(clock));}
    public LocalDate date(String value){
        if(value==null||!value.matches("\\d{4}-\\d{2}-\\d{2}"))throw new IllegalArgumentException("日期格式应为 YYYY-MM-DD。");
        try{LocalDate day=LocalDate.parse(value,DateTimeFormatter.ISO_LOCAL_DATE.withResolverStyle(ResolverStyle.STRICT));if(day.getYear()<1)throw new IllegalArgumentException();return day;}
        catch(DateTimeException e){throw new IllegalArgumentException("请选择有效日期。");}
    }
    public LocalTime end(String period){return LocalTime.parse(System.getProperty("clinic."+period+".end","AM".equals(period)?"12:00":"PM".equals(period)?"17:00":"21:00"));}
    public boolean validPeriod(String p){return "AM".equals(p)||"PM".equals(p)||"EVENING".equals(p);}
    public boolean expired(String date,String p){if(!validPeriod(p))throw new IllegalArgumentException("时段无效。");return !LocalDateTime.now(clock).isBefore(date(date).atTime(end(p)));}
    public boolean canCheckIn(String date,String p){return date(date).equals(today())&&!expired(date,p);}
}
