package com.ustb.hospital.mapper;

import com.ustb.hospital.entity.Departments;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface DepartmentsMapper {
    public Departments selectById(int id);
    public int getMaxId();

    //科室名称不能重复
    public Departments selectByName(String name);

    public int insert(Departments departments);
    List<Departments> selectByPid(int departmentPid);
    List<Departments> selectAll();
    List<Departments> selectFirstDept(int level);

    int insertDept(@Param("departmentName") String departmentName,@Param("departmentPid") int departmentPid,@Param("departmentLevel") int departmentLevel,@Param("departmentPath") String departmentPath,@Param("departmentDescription") String departmentDescription);

    int updateById(@Param("departmentId") int departmentId,@Param("departmentName") String departmentName ,@Param("departmentDescription") String departmentDescription);
    void delById(int departmentId);
    List<Departments> selectByNameWithPage(String deptName);
    List<Departments> selectByParentId(int id);
    public int delete(int id);

}
