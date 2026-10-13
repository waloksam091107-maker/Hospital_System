package com.ustb.hospital.mapper;

import com.ustb.hospital.entity.Orders;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface OrdersMapper {

    public void updateOrderStatus(@Param("orderId") int orderId, @Param("paidStatus") String paidStatus);

    public void save(@Param("orderId") int orderId, @Param("doctorId") int doctorId, @Param("doctorName") String doctorName, @Param("patientId") int patientId, @Param("patientName") String patientName, @Param("orderType") String orderType, @Param("orderDetails") String orderDetails,@Param("status") String status);

    public int getMaxOrderId();
    public void delById(int orderId);
    public List<Orders> findByDocId(int doctorId);
    public List<Orders> findByPatId(int PatientId);
    public List<Orders> selectByDoctorId(int doctorId);

    public void updateOrderDetails(@Param("orderId") int orderId, @Param("orderType") String orderType, @Param("orderDetails") String orderDetails);
}
