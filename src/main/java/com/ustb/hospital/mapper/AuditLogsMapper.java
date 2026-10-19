package com.ustb.hospital.mapper;

import com.ustb.hospital.entity.AuditLog;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import com.ustb.hospital.service.AuditQuery;

public interface AuditLogsMapper {
    int insert(AuditLog auditLog);
    List<AuditLog> selectRecent(@Param("limit") int limit);
    long countSearch(AuditQuery query);
    List<AuditLog> search(AuditQuery query);
}
