package com.ustb.hospital.service;

import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.*;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.*;
import java.sql.*;
import java.util.*;
import java.math.BigDecimal;
import static org.junit.Assert.*;

public class IncomeReportServiceTest {
    private IncomeReportService service;
    @Before public void setup()throws Exception{
        UnpooledDataSource source=new UnpooledDataSource("org.h2.Driver","jdbc:h2:mem:income_"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1","sa","");
        SqlSessionFactory factory=new SqlSessionFactoryBuilder().build(new Configuration(new Environment("test",new JdbcTransactionFactory(),source)));
        try(Connection c=source.getConnection();Statement s=c.createStatement()){
            s.execute("CREATE TABLE patients(patient_id INT PRIMARY KEY,name VARCHAR(100))");s.execute("INSERT INTO patients VALUES(1,'患者')");
            s.execute("CREATE TABLE doctors(doctor_id INT PRIMARY KEY,name VARCHAR(100))");s.execute("INSERT INTO doctors VALUES(1,'医生')");
            s.execute("CREATE TABLE appointments(appointment_id INT PRIMARY KEY,doctor_id INT,status VARCHAR(30))");s.execute("INSERT INTO appointments VALUES(1,1,'CANCELLED')");
            s.execute("CREATE TABLE balance_transactions(transaction_id BIGINT PRIMARY KEY,appointment_id INT,patient_id INT,transaction_type VARCHAR(32),amount DECIMAL(12,2),created_at DATETIME)");
            s.execute("INSERT INTO balance_transactions VALUES(1,1,1,'APPOINTMENT_DEBIT',-12.50,'2026-10-29 00:00:00'),(2,1,1,'APPOINTMENT_DEBIT',-7.25,'2026-10-29 23:59:59'),(3,NULL,1,'RECHARGE',1000,'2026-10-29 12:00:00'),(4,1,1,'APPOINTMENT_DEBIT',-99,'2026-10-28 23:59:59'),(5,1,1,'APPOINTMENT_DEBIT',-88,'2026-10-30 00:00:00')");
            c.commit();
        }
        service=new IncomeReportService(()->factory.openSession(true));
    }
    @Test public void incomeUsesDebitLedgerDayBoundariesAndKeepsCancelledPayments(){
        Map<String,Object> report=service.report("2026-10-29");
        assertEquals(new BigDecimal("19.75"),report.get("total"));
        List<Map<String,Object>> items=(List<Map<String,Object>>)report.get("items");assertEquals(2,items.size());assertEquals("医生",items.get(0).get("doctorName"));assertEquals("患者",items.get(0).get("patientName"));
    }
    @Test public void dayWithoutDebitsHasZeroIncome(){assertEquals(new BigDecimal("0.00"),service.report("2026-10-31").get("total"));}
    @Test public void invalidDateIsRejected(){for(String date:new String[]{null,"2026-02-30","2026-1-1","2026-10-29 OR 1=1"}){try{service.report(date);fail("Invalid date accepted: "+date);}catch(IllegalArgumentException expected){}}}
}
