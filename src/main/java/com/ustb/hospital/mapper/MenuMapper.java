package com.ustb.hospital.mapper;

import com.ustb.hospital.entity.Doctors;
import com.ustb.hospital.entity.Menu;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface MenuMapper {
    public void delById(String id);
    public List<Menu> selectAll();
    public List<Menu> selectByClass(String fenlei);
    public void updateById(@Param("id") String id,@Param("price") String price,@Param("fenlei") String fenlei);
}
