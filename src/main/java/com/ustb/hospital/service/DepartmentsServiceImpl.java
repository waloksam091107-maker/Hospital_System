package com.ustb.hospital.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.ustb.hospital.entity.Departments;
import com.ustb.hospital.mapper.DepartmentsMapper;
import com.ustb.hospital.utils.MybatisUtil;
import org.apache.ibatis.session.SqlSession;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DepartmentsServiceImpl {
    public List<Departments> queryAllSecond(){
        try (SqlSession sqlSession = MybatisUtil.getSqlSession()) {
            return sqlSession.getMapper(DepartmentsMapper.class).selectFirstDept(2);
        }
    }

    public PageInfo queryDepartmentsByName(String departmentName){
        try (SqlSession sqlSession = MybatisUtil.getSqlSession()) {
            DepartmentsMapper mapper=sqlSession.getMapper(DepartmentsMapper.class);
            PageHelper.startPage(1,5);
            Departments dept=mapper.selectByName(departmentName);
            List<Departments> list=new ArrayList<>();
            list.add(dept);
            return new PageInfo(list);
        }
    }

    public List<Departments> querySecondByPid(int deptPid){
        try (SqlSession sqlSession = MybatisUtil.getSqlSession()) {
            return sqlSession.getMapper(DepartmentsMapper.class).selectByPid(deptPid);
        }
    }
    public void save(int deptPid,String deptName,String deptDesc) throws Exception{
        try {
            //验证科室名是否可用
            SqlSession sqlSession = MybatisUtil.getSqlSession();
            DepartmentsMapper mapper=sqlSession.getMapper(DepartmentsMapper.class);
            Departments departments=mapper.selectByName(deptName);
            if(departments==null){
                Departments departmentsNew = new Departments();
                departmentsNew.setDepartmentId(mapper.getMaxId()+1);
                departmentsNew.setDepartmentName(deptName);
                departmentsNew.setDepartmentDescription(deptDesc);

                //判断保存的是一级还是二级
                if(deptPid==-1){
                    departmentsNew.setDepartmentLevel(1);
                    departmentsNew.setDepartmentPath("|"+departmentsNew.getDepartmentId()+"|");


                }else{
                    //当前保存的是二级科室
                    Departments parentDepartment = mapper.selectById(deptPid);
                    if(parentDepartment==null){
                        //父类不存在（并发问题）！！！！（异常） 要抛出 给用户
                        throw new Exception("父科室不存在");



                    }else{
                        departmentsNew.setDepartmentPid(deptPid);
                        departmentsNew.setDepartmentLevel(parentDepartment.getDepartmentLevel()+1);
                        departmentsNew.setDepartmentPath("|"+deptPid+"||"+departmentsNew.getDepartmentId()+"|");
                    }

                }

                mapper.insert(departmentsNew);
}else{
                //科室名重复，不能保存（异常）
                throw new Exception("科室名称已经存在");


            }
        } catch (SQLException e) {
            e.printStackTrace();

        } finally {
        }


    }



    public List<Departments> queryByPid(int departmentPid){
        try (SqlSession sqlSession = MybatisUtil.getSqlSession()) {
            return sqlSession.getMapper(DepartmentsMapper.class).selectByPid(departmentPid);
        }
    }
    public void delById(int departmentId){
        SqlSession sqlSession = MybatisUtil.getSqlSession();
        DepartmentsMapper mapper=sqlSession.getMapper(DepartmentsMapper.class);
        //
        mapper.delById(departmentId);
    }
    public void updateById(int departmentId,String departmentName,String departmentDescription){
        SqlSession sqlSession = MybatisUtil.getSqlSession();
        DepartmentsMapper mapper=sqlSession.getMapper(DepartmentsMapper.class);
        mapper.updateById(departmentId,departmentName,departmentDescription);
    }

    public void insertDept(String departmentName,int departmentPid,int departmentLevel,String departmentPath,String departmentDescription){
        SqlSession sqlSession = MybatisUtil.getSqlSession();
        DepartmentsMapper mapper=sqlSession.getMapper(DepartmentsMapper.class);
        mapper.insertDept(departmentName,departmentPid,departmentLevel,departmentPath,departmentDescription);
    }
    public List<Departments> queryFirstDept(){
        try (SqlSession sqlSession = MybatisUtil.getSqlSession()) {
            return sqlSession.getMapper(DepartmentsMapper.class).selectFirstDept(1);
        }
    }


    public PageInfo<Departments> queryAll(int pageNum,int pageSize){
        try (SqlSession sqlSession = MybatisUtil.getSqlSession()) {
            DepartmentsMapper mapper=sqlSession.getMapper(DepartmentsMapper.class);
            PageHelper.startPage(pageNum,pageSize);
            return new PageInfo<>(mapper.selectFirstDept(1));
        }
    }

    public PageInfo<Departments> querySecondLevelDepartmentsPage(int pageNum,int pageSize){
        try (SqlSession sqlSession = MybatisUtil.getSqlSession()) {
            DepartmentsMapper mapper=sqlSession.getMapper(DepartmentsMapper.class);
            PageHelper.startPage(pageNum,pageSize);
            return new PageInfo<>(mapper.selectAll());
        }
    }

    public List<Departments> queryAllSecondLevelDepartments(){
        try (SqlSession sqlSession = MybatisUtil.getSqlSession()) {
            return sqlSession.getMapper(DepartmentsMapper.class).selectAll();
        }
    }
    public PageInfo<Departments> queryByNameWithPage(int pageNum, int pageSize, String deptName) {
        try (SqlSession sqlSession = MybatisUtil.getSqlSession()) {
            DepartmentsMapper mapper=sqlSession.getMapper(DepartmentsMapper.class);
            PageHelper.startPage(pageNum,pageSize);
            return new PageInfo<>(mapper.selectByNameWithPage(deptName));
        }
    }
    public int DepartmentHasChildren(int departmentId) {
        try (SqlSession sqlSession = MybatisUtil.getSqlSession()) {
            List<Departments> children=sqlSession.getMapper(DepartmentsMapper.class).selectByParentId(departmentId);
            return !children.isEmpty() ? 1 : 0;
        }
    }
    public void delete(int departmentId) throws Exception {
        SqlSession sqlSession = null;
        try {
            sqlSession = MybatisUtil.getSqlSession();
            DepartmentsMapper mapper = sqlSession.getMapper(DepartmentsMapper.class);
            // 检查要删除的部门是否存在
            Departments department = mapper.selectById(departmentId);
            if (department == null) {
                throw new Exception("要删除的科室不存在");
            }
            // 检查是否有子部门，如果有则不能删除
            List<Departments> children = (List<Departments>) mapper.selectByParentId(departmentId);
            if (children != null && !children.isEmpty()) {
                throw new Exception("该科室下存在子科室，不能删除");
            }
            // 删除操作
            mapper.delete(departmentId);
sqlSession.commit();

        } catch (SQLException e) {
            e.printStackTrace();
            throw new Exception("删除失败，数据库操作异常");
        } finally {
            // 关闭SqlSession
            if (sqlSession != null) {
                sqlSession.close();
            }
        }
    }
    public List<Departments> querySecondDept(){
        try (SqlSession sqlSession = MybatisUtil.getSqlSession()) {
            return sqlSession.getMapper(DepartmentsMapper.class).selectFirstDept(2);
        }
    }

}
