package com.ustb.hospital.utils;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.Test;

import java.io.InputStream;
import java.util.Properties;

import static org.junit.Assert.assertNotNull;

/** Verifies that all mapper XML files used at application start can be parsed without a database connection. */
public class MybatisConfigurationTest {
    @Test
    public void loadsMinimumAppointmentMappings() throws Exception {
        Properties properties = new Properties();
        properties.setProperty("db.driver", "com.mysql.cj.jdbc.Driver");
        properties.setProperty("db.url", "jdbc:mysql://127.0.0.1:3306/hospt");
        properties.setProperty("db.username", "test");
        properties.setProperty("db.password", "test");
        InputStream input = Resources.getResourceAsStream("mybatis-config.xml");
        try {
            SqlSessionFactory factory = new SqlSessionFactoryBuilder().build(input, properties);
            assertNotNull(factory.getConfiguration().getMappedStatement("com.ustb.hospital.mapper.AppointmentsMapper.insert"));
            assertNotNull(factory.getConfiguration().getMappedStatement("com.ustb.hospital.mapper.AppointmentsMapper.updateStatusIfCurrent"));
            assertNotNull(factory.getConfiguration().getMappedStatement("com.ustb.hospital.mapper.DatesMapper.reserveSlot"));
            assertNotNull(factory.getConfiguration().getMappedStatement("com.ustb.hospital.mapper.DatesMapper.releaseSlot"));
            assertNotNull(factory.getConfiguration().getMappedStatement("com.ustb.hospital.mapper.PatientsMapper.deductBalanceIfEnough"));
        } finally {
            input.close();
        }
    }
}
