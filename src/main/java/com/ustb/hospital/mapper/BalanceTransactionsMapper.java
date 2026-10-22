package com.ustb.hospital.mapper;

import com.ustb.hospital.entity.BalanceTransaction;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BalanceTransactionsMapper {
    int insert(BalanceTransaction transaction);
    List<BalanceTransaction> selectRecentByPatient(@Param("patientId") int patientId, @Param("limit") int limit);
}
