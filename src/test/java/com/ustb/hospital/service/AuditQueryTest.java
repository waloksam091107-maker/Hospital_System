package com.ustb.hospital.service;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.*;
import org.junit.Before;
import org.junit.Test;
import java.io.InputStream;
import java.sql.Statement;
import java.util.*;
import java.util.function.Supplier;
import static org.junit.Assert.*;

public class AuditQueryTest {
    private AuditService service;
    @Before public void setup() throws Exception {
        Properties p = new Properties(); p.setProperty("db.driver","org.h2.Driver"); p.setProperty("db.url","jdbc:h2:mem:query_"+UUID.randomUUID().toString().replace("-","")+";MODE=MySQL;DB_CLOSE_DELAY=-1"); p.setProperty("db.username","sa"); p.setProperty("db.password","");
        SqlSessionFactory factory; try (InputStream in=Resources.getResourceAsStream("mybatis-config.xml")) { factory=new SqlSessionFactoryBuilder().build(in,p); }
        try (SqlSession s=factory.openSession(true); Statement sql=s.getConnection().createStatement()) {
            sql.execute("CREATE TABLE audit_logs(audit_log_id BIGINT AUTO_INCREMENT PRIMARY KEY, actor_role VARCHAR(20), actor_id INT, action VARCHAR(80), target_type VARCHAR(80), target_id VARCHAR(80), outcome VARCHAR(20), request_method VARCHAR(10), request_path VARCHAR(255), remote_address VARCHAR(64), created_at TIMESTAMP)");
            sql.execute("INSERT INTO audit_logs(actor_role,actor_id,action,outcome,created_at) VALUES ('PATIENT',1,'_APPOINTMENTS_BOOK','SUCCESS','2030-01-01 10:00:00'),('PATIENT',1,'_APPOINTMENTS_BOOK','SUCCESS','2030-01-01 10:00:00'),('DOCTOR',1,'APPOINTMENTS_START','SUCCESS','2030-01-01 11:00:00'),('PATIENT',2,'_APPOINTMENTS_BOOK','FAILURE','2030-01-01 12:00:00')");
        }
        service = new AuditService(() -> factory.openSession(true));
    }
    @Test public void filtersCanBeCombinedAndPaginationStable() throws Exception {
        Object result = search("2030-01-01 00:00:00","2030-01-02 00:00:00","PATIENT","1","_APPOINTMENTS_BOOK","SUCCESS","1","1");
        assertEquals(2L, ((Number)result.getClass().getMethod("getTotal").invoke(result)).longValue());
        List<?> first=(List<?>)result.getClass().getMethod("getRows").invoke(result); assertEquals(1,first.size());
        Object page2=search("2030-01-01 00:00:00","2030-01-02 00:00:00","PATIENT","1","_APPOINTMENTS_BOOK","SUCCESS","2","1");
        List<?> second=(List<?>)page2.getClass().getMethod("getRows").invoke(page2);
        assertTrue(((Number)first.get(0).getClass().getMethod("getAuditLogId").invoke(first.get(0))).longValue() > ((Number)second.get(0).getClass().getMethod("getAuditLogId").invoke(second.get(0))).longValue());
    }
    @Test public void invalidRangeOrPageRejected() throws Exception {
        for (String[] values : new String[][]{{"2030-01-02 00:00:00","2030-01-01 00:00:00",null,null,null,null,"1","20"},{null,null,null,null,null,null,"0","20"},{null,null,null,null,null,null,"1","101"},{null,null,"EVIL",null,null,null,"1","20"}}) {
            try { query(values); fail("invalid query accepted"); } catch (java.lang.reflect.InvocationTargetException expected) { assertTrue(expected.getCause() instanceof IllegalArgumentException); }
        }
    }
    @Test public void canonicalActionsAlsoFindKnownHistoricalAliases() {
        AuditQuery query = new AuditQuery(null,null,"PATIENT","2","APPOINTMENTS_BOOK","FAILURE","1","20");
        AuditQueryResult result = service.search(query);
        assertEquals(1, result.getTotal());
        assertEquals("_APPOINTMENTS_BOOK", result.getRows().get(0).getAction());
    }
    private Object search(String... args) throws Exception { Object query=query(args); return AuditService.class.getMethod("search",query.getClass()).invoke(service,query); }
    private Object query(String... args) throws Exception { Class<?> c=Class.forName("com.ustb.hospital.service.AuditQuery"); return c.getConstructor(String.class,String.class,String.class,String.class,String.class,String.class,String.class,String.class).newInstance((Object[])args); }
}
