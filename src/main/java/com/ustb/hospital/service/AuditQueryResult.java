package com.ustb.hospital.service;
import com.ustb.hospital.entity.AuditLog;
import java.util.List;
public final class AuditQueryResult {
    private final List<AuditLog> rows; private final long total; private final int page, pageSize;
    public AuditQueryResult(List<AuditLog> rows, long total, int page, int pageSize) { this.rows=rows; this.total=total; this.page=page; this.pageSize=pageSize; }
    public List<AuditLog> getRows() { return rows; } public long getTotal() { return total; } public int getPage() { return page; } public int getPageSize() { return pageSize; }
}
