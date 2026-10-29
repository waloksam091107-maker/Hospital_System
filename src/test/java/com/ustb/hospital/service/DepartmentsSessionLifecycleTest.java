package com.ustb.hospital.service;

import com.github.pagehelper.PageHelper;
import com.ustb.hospital.utils.MybatisUtil;
import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import java.io.InputStream;
import java.lang.reflect.*;
import java.sql.Statement;
import java.util.*;
import static org.junit.Assert.*;

/** Real mapper queries must return their physical connection, including failed queries. */
@RunWith(Parameterized.class)
public class DepartmentsSessionLifecycleTest {
    @Parameterized.Parameters(name="{0}") public static Collection<Object[]> methods() {
        return Arrays.asList(new Object[][] {
            {"queryAllSecond", new Class<?>[0], new Object[0]},
            {"queryDepartmentsByName", new Class<?>[]{String.class}, new Object[]{"Parent"}},
            {"querySecondByPid", new Class<?>[]{int.class}, new Object[]{1}},
            {"queryByPid", new Class<?>[]{int.class}, new Object[]{1}},
            {"queryFirstDept", new Class<?>[0], new Object[0]},
            {"queryAll", new Class<?>[]{int.class,int.class}, new Object[]{1,5}},
            {"querySecondLevelDepartmentsPage", new Class<?>[]{int.class,int.class}, new Object[]{1,5}},
            {"queryAllSecondLevelDepartments", new Class<?>[0], new Object[0]},
            {"queryByNameWithPage", new Class<?>[]{int.class,int.class,String.class}, new Object[]{1,5,"Child"}},
            {"DepartmentHasChildren", new Class<?>[]{int.class}, new Object[]{1}},
            {"querySecondDept", new Class<?>[0], new Object[0]}
        });
    }
    private final String method;
    private final Class<?>[] types;
    private final Object[] args;
    private SqlSessionFactory previous, factory;
    private PooledDataSource pool;
    private Field factoryField;
    public DepartmentsSessionLifecycleTest(String method,Class<?>[] types,Object[] args) {this.method=method;this.types=types;this.args=args;}
    @Before public void isolatedPooledDatabase() throws Exception {
        Properties p = new Properties(); p.setProperty("db.driver","org.h2.Driver");
        p.setProperty("db.url","jdbc:h2:mem:dept_sessions_"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1");
        p.setProperty("db.username","sa"); p.setProperty("db.password","");
        try(InputStream config=Resources.getResourceAsStream("mybatis-config.xml")){factory=new SqlSessionFactoryBuilder().build(config,p);}
        pool=(PooledDataSource)factory.getConfiguration().getEnvironment().getDataSource();
        try(SqlSession s=factory.openSession(true);Statement st=s.getConnection().createStatement()) {
            st.execute("CREATE TABLE departments(department_id INT PRIMARY KEY,department_name VARCHAR(100),department_pid INT,department_level INT,department_path VARCHAR(100),department_discription VARCHAR(100))");
            st.execute("INSERT INTO departments VALUES(1,'Parent',NULL,1,'|1|','Parent'),(2,'Child',1,2,'|1||2|','Child')");
        }
        factoryField=MybatisUtil.class.getDeclaredField("sqlSessionFactory"); factoryField.setAccessible(true);
        previous=(SqlSessionFactory)factoryField.get(null); factoryField.set(null,factory);
        assertEquals(0,pool.getPoolState().getActiveConnectionCount());
    }
    @After public void cleanup() throws Exception {
        PageHelper.clearPage();
        if(factoryField!=null)factoryField.set(null,previous);
        if(pool!=null)pool.forceCloseAll();
    }
    @Test public void repeatedSuccessfulReadsReturnConnectionToPool() throws Exception {
        for(int n=0;n<12;n++) {
            Object result=invoke(); assertNotNull("Query still returns its mapped result",result);
            if(result instanceof List)assertFalse(((List<?>)result).isEmpty());
            if(result instanceof Integer)assertEquals(1,((Integer)result).intValue());
            assertEquals("Department read must release the pooled connection",0,pool.getPoolState().getActiveConnectionCount());
        }
    }
    @Test public void failedReadReturnsConnectionToPool() throws Exception {
        try(SqlSession s=factory.openSession(true);Statement st=s.getConnection().createStatement()){st.execute("DROP TABLE departments");}
        try {invoke();fail("Missing table must fail the real mapper query");}catch(InvocationTargetException expected){assertNotNull(expected.getCause());}
        assertEquals("Failed department read must release the pooled connection",0,pool.getPoolState().getActiveConnectionCount());
    }
    private Object invoke()throws Exception{return DepartmentsServiceImpl.class.getMethod(method,types).invoke(new DepartmentsServiceImpl(),args);}
}
