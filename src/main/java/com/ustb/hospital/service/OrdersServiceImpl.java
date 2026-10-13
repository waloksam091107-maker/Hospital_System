package com.ustb.hospital.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.ustb.hospital.entity.Orders;
import com.ustb.hospital.mapper.OrdersMapper;
import com.ustb.hospital.utils.MybatisUtil;
import org.apache.ibatis.session.SqlSession;

import java.util.List;

public class OrdersServiceImpl {

    public List<Orders> queryByDoctorId(int doctorId) {
        SqlSession sqlSession = MybatisUtil.getSqlSession();
        OrdersMapper mapper = sqlSession.getMapper(OrdersMapper.class);
        List<Orders> ordersList = mapper.selectByDoctorId(doctorId);
        sqlSession.close();
        return ordersList;
    }

    public void updateOrderStatus(int orderId, String paidStatus){
        SqlSession sqlSession=MybatisUtil.getSqlSession();
        OrdersMapper mapper=sqlSession.getMapper(OrdersMapper.class);
        mapper.updateOrderStatus(orderId, paidStatus);
    }


    public void add(int doctorId,String doctorName,int patientId,String patientName,String orderType,String orderDetails,String status){
        SqlSession sqlSession=MybatisUtil.getSqlSession();
        OrdersMapper mapper=sqlSession.getMapper(OrdersMapper.class);
        int maxId=mapper.getMaxOrderId();
        maxId=maxId+1;
        mapper.save(maxId,doctorId,doctorName,patientId,patientName,orderType,orderDetails,status);
    }




    public void delById(int orderId){
        SqlSession sqlSession=MybatisUtil.getSqlSession();
        OrdersMapper mapper=sqlSession.getMapper(OrdersMapper.class);
        mapper.delById(orderId);

    }


    public PageInfo findByPatId(int pageNum,int pageSize,int doctorId){
        SqlSession sqlSession=MybatisUtil.getSqlSession();
        OrdersMapper mapper=sqlSession.getMapper(OrdersMapper.class);
        PageHelper.startPage(pageNum,pageSize); //

        List<Orders> list=mapper.findByPatId(doctorId);
        PageInfo pageInfo=new PageInfo(list);
return pageInfo;

    }
    public PageInfo findByDocId(int pageNum,int pageSize,int doctorId){
SqlSession sqlSession=MybatisUtil.getSqlSession();
OrdersMapper mapper=sqlSession.getMapper(OrdersMapper.class);
        PageHelper.startPage(pageNum,pageSize); //

        List<Orders> list=mapper.findByDocId(doctorId);
        PageInfo pageInfo=new PageInfo(list);
return pageInfo;

    }

    public void updateOrderDetails(int orderId,String orderType,String orderDetails){
        SqlSession sqlSession=MybatisUtil.getSqlSession();
        OrdersMapper mapper=sqlSession.getMapper(OrdersMapper.class);
        mapper.updateOrderDetails(orderId,orderType,orderDetails);
    }
}
