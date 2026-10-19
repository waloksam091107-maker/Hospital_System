package com.ustb.hospital.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.List;

/** Validated, bounded audit filters. Start is inclusive and end exclusive. */
public final class AuditQuery {
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss").withResolverStyle(ResolverStyle.STRICT);
    private final String start, end, role, action, outcome;
    private final Integer actorId;
    private final int page, pageSize;
    public AuditQuery(String start, String end, String role, String actorId, String action, String outcome, String page, String pageSize) {
        this.start = date(start); this.end = date(end);
        if (this.start != null && this.end != null && this.start.compareTo(this.end) >= 0) throw new IllegalArgumentException("开始时间必须早于结束时间。");
        this.role = choice(role, "DOCTOR|PATIENT|ADMIN", "角色无效。");
        this.outcome = choice(outcome, "SUCCESS|FAILURE", "结果无效。");
        this.action = choice(action, "[A-Z0-9_-]{1,80}", "动作无效。");
        String actor = clean(actorId);
        this.actorId = actor == null ? null : Integer.valueOf(number(actor, 1, Integer.MAX_VALUE, "操作人编号无效。"));
        this.page = number(page, 1, 1000000, "页码无效。");
        this.pageSize = number(pageSize, 20, 100, "每页数量无效。");
    }
    private static String clean(String s) { return s == null || s.trim().isEmpty() ? null : s.trim(); }
    private static String date(String s) { s = clean(s); if (s == null) return null; try { LocalDateTime.parse(s,FORMAT); return s; } catch (RuntimeException ex) { throw new IllegalArgumentException("时间格式必须为 yyyy-MM-dd HH:mm:ss。"); } }
    private static String choice(String s, String pattern, String message) { s = clean(s); if (s != null && !s.matches(pattern)) throw new IllegalArgumentException(message); return s; }
    private static int number(String text, int fallback, int max, String message) { text=clean(text); if(text==null)return fallback; try { if(!text.matches("[0-9]{1,10}")) throw new NumberFormatException(); int n=Integer.parseInt(text); if(n<1 || n>max) throw new NumberFormatException(); return n; } catch(NumberFormatException ex) { throw new IllegalArgumentException(message); } }
    public String getStart() { return start; } public String getEnd() { return end; } public String getRole() { return role; }
    public Integer getActorId() { return actorId; } public String getAction() { return action; } public String getOutcome() { return outcome; }
    public List<String> getActionAliases() { return AuditActions.aliases(action); }
    public int getPage() { return page; } public int getPageSize() { return pageSize; } public int getOffset() { return (page-1)*pageSize; }
}
