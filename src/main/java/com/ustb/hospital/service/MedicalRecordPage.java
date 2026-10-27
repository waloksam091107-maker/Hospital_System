package com.ustb.hospital.service;

import java.util.List;

public final class MedicalRecordPage {
    private final List<MedicalRecordSummary> items;
    private final int pageNum;
    private final int pageSize;
    private final long total;
    public MedicalRecordPage(List<MedicalRecordSummary> items, MedicalRecordQuery query, long total) {
        this.items = items; this.pageNum = query.getPageNum(); this.pageSize = query.getPageSize(); this.total = total;
    }
    public List<MedicalRecordSummary> getItems() { return items; }
    public int getPageNum() { return pageNum; }
    public int getPageSize() { return pageSize; }
    public long getTotal() { return total; }
    public long getPages() { return total == 0 ? 0 : (total - 1) / pageSize + 1; }
}
