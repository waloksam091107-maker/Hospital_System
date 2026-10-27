package com.ustb.hospital.service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** Validated filters for the role-scoped patient search. Identity is supplied separately. */
public final class MedicalRecordQuery {
    private final String dateFrom;
    private final String dateTo;
    private final Integer doctorId;
    private final int pageNum;
    private final int pageSize;

    public MedicalRecordQuery(String dateFrom, String dateTo, String doctorId, String pageNum, String pageSize) {
        this.dateFrom = date(dateFrom);
        this.dateTo = date(dateTo);
        if (this.dateFrom != null && this.dateTo != null && this.dateFrom.compareTo(this.dateTo) > 0)
            throw new IllegalArgumentException("开始日期不能晚于结束日期。");
        this.doctorId = blank(doctorId) ? null : positive(doctorId, "医生编号");
        this.pageNum = blank(pageNum) ? 1 : positive(pageNum, "页码");
        this.pageSize = blank(pageSize) ? 10 : Math.min(100, positive(pageSize, "每页条数"));
    }

    private static String date(String text) {
        if (blank(text)) return null;
        try {
            if (!text.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) throw new DateTimeParseException("format", text, 0);
            LocalDate parsed = LocalDate.parse(text);
            if (parsed.getYear() < 1) throw new DateTimeParseException("year", text, 0);
            return parsed.toString();
        } catch (DateTimeParseException invalid) { throw new IllegalArgumentException("预约日期必须为有效的 YYYY-MM-DD 日期。"); }
    }
    private static int positive(String text, String label) {
        try {
            if (!text.matches("[0-9]{1,10}")) throw new NumberFormatException();
            int value = Integer.parseInt(text);
            if (value < 1) throw new NumberFormatException();
            return value;
        } catch (NumberFormatException invalid) { throw new IllegalArgumentException(label + "必须为正整数。"); }
    }
    private static boolean blank(String text) { return text == null || text.trim().isEmpty(); }
    public String getDateFrom() { return dateFrom; }
    public String getDateTo() { return dateTo; }
    public Integer getDoctorId() { return doctorId; }
    public int getPageNum() { return pageNum; }
    public int getPageSize() { return pageSize; }
    public long getOffset() { return (long) (pageNum - 1) * pageSize; }
}
