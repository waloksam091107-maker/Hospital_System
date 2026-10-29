package com.ustb.hospital.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
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

/** Exercise the real public read mappers and physical pool on success and SQL failure. */
@RunWith(Parameterized.class)
public class PublicReadSessionLifecycleTest {
    @Parameterized.Parameters(name="{0}.{1} {3}") public static Collection<Object[]> methods() {
        return Arrays.asList(new Object[][] {
            {MenuServiceImpl.class,"queryByPage",new Class<?>[]{int.class,String.class},new Object[]{1,"-1"},"menu"},
            {MenuServiceImpl.class,"queryByPage",new Class<?>[]{int.class,String.class},new Object[]{1,"Lunch"},"menu"},
            {MenuServiceImpl.class,"queryCategories",new Class<?>[0],new Object[0],"menu"},
            {NewsServiceImpl.class,"queryAll",new Class<?>[0],new Object[0],"news"},
            {NewsServiceImpl.class,"queryNew",new Class<?>[0],new Object[0],"news"},
            {NewsServiceImpl.class,"queryById",new Class<?>[]{int.class},new Object[]{1},"news"},
            {NewsServiceImpl.class,"queryByTitle",new Class<?>[]{String.class},new Object[]{"News"},"news"},
            {NewsServiceImpl.class,"getNewsById",new Class<?>[]{int.class},new Object[]{1},"news"},
            {NewsServiceImpl.class,"query",new Class<?>[]{int.class,int.class,int.class,String.class,String.class,String.class,String.class,String.class},new Object[]{1,4,-1,null,null,null,null,null},"news"},
            {DoctorsServiceImpl.class,"queryById",new Class<?>[]{int.class},new Object[]{1},"doctors"},
            {AnnouncementsServiceImpl.class,"queryAll",new Class<?>[0],new Object[0],"announcement"}
        });
    }
    private final Class<?> service;
    private final String method,table;
    private final Class<?>[] types;
    private final Object[] args;
    private SqlSessionFactory previous,factory;
    private PooledDataSource pool;
    private Field factoryField;
    public PublicReadSessionLifecycleTest(Class<?> service,String method,Class<?>[] types,Object[] args,String table){this.service=service;this.method=method;this.types=types;this.args=args;this.table=table;}
    @Before public void isolatedPooledDatabase() throws Exception {
        Properties p=new Properties();p.setProperty("db.driver","org.h2.Driver");
        p.setProperty("db.url","jdbc:h2:mem:public_sessions_"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1");p.setProperty("db.username","sa");p.setProperty("db.password","");
        try(InputStream config=Resources.getResourceAsStream("mybatis-config.xml")){factory=new SqlSessionFactoryBuilder().build(config,p);}
        pool=(PooledDataSource)factory.getConfiguration().getEnvironment().getDataSource();
        try(SqlSession s=factory.openSession(true);Statement st=s.getConnection().createStatement()) {
            st.execute("CREATE TABLE menu(id INT PRIMARY KEY,name VARCHAR(100),price DECIMAL(10,2),fenlei VARCHAR(100))");
            st.execute("INSERT INTO menu VALUES(1,'Meal',12.00,'Lunch')");
            st.execute("CREATE TABLE news(news_id INT PRIMARY KEY,title VARCHAR(100),content VARCHAR(100),creation_time VARCHAR(30),creator VARCHAR(30),news_img VARCHAR(100))");
            st.execute("INSERT INTO news VALUES(1,'News','Content','2026-10-29','Admin','image.png')");
            st.execute("CREATE TABLE announcement(announcement_id INT PRIMARY KEY,title VARCHAR(100),content VARCHAR(100),creation_time VARCHAR(30),creator VARCHAR(30),status VARCHAR(20),published_at TIMESTAMP,updated_at TIMESTAMP,creator_admin_id INT)");
            st.execute("INSERT INTO announcement VALUES(1,'Announcement','Content','2026-10-29','Admin','PUBLISHED',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,1)");
            st.execute("CREATE TABLE departments(department_id INT PRIMARY KEY,department_name VARCHAR(100))");st.execute("INSERT INTO departments VALUES(1,'Department')");
            st.execute("CREATE TABLE professional_titles(id INT PRIMARY KEY,title_name VARCHAR(100))");st.execute("INSERT INTO professional_titles VALUES(1,'Doctor')");
            st.execute("CREATE TABLE doctors(doctor_id INT PRIMARY KEY,job_number VARCHAR(30),password VARCHAR(100),password_hash VARCHAR(100),name VARCHAR(100),avatar VARCHAR(100),phone VARCHAR(30),email VARCHAR(100),introduction VARCHAR(100),registration_fee VARCHAR(30),entry_date VARCHAR(30),department_id INT,professional_title_id INT)");
            st.execute("INSERT INTO doctors(doctor_id,job_number,name,department_id,professional_title_id) VALUES(1,'doctor-1','Doctor',1,1)");
        }
        factoryField=MybatisUtil.class.getDeclaredField("sqlSessionFactory");factoryField.setAccessible(true);
        previous=(SqlSessionFactory)factoryField.get(null);factoryField.set(null,factory);
        assertEquals(0,pool.getPoolState().getActiveConnectionCount());
    }
    @After public void cleanup() throws Exception {
        PageHelper.clearPage();if(factoryField!=null)factoryField.set(null,previous);if(pool!=null)pool.forceCloseAll();
    }
    @Test public void repeatedSuccessfulReadsReturnConnectionToPool() throws Exception {
        for(int n=0;n<12;n++) {
            Object result=invoke();assertNotNull("Mapped result remains available after closing the session",result);
            if(result instanceof List)assertFalse(((List<?>)result).isEmpty());
            if(result instanceof PageInfo)assertFalse(((PageInfo<?>)result).getList().isEmpty());
            assertEquals("Public read must release its physical connection",0,pool.getPoolState().getActiveConnectionCount());
        }
    }
    @Test public void failedReadReturnsConnectionToPool() throws Exception {
        try(SqlSession s=factory.openSession(true);Statement st=s.getConnection().createStatement()){st.execute("DROP TABLE "+table);}
        try{invoke();fail("Missing table must fail the actual mapper query");}catch(InvocationTargetException expected){assertNotNull(expected.getCause());}
        assertEquals("Failed public read must release its physical connection",0,pool.getPoolState().getActiveConnectionCount());
    }
    private Object invoke()throws Exception{return service.getMethod(method,types).invoke(service.getConstructor().newInstance(),args);}
}
