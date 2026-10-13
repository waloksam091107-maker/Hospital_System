package com.ustb.hospital.utils;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class MybatisUtil {
    private static volatile SqlSessionFactory sqlSessionFactory;

    public static SqlSession getSqlSession(){
        return openSession(true);
    }

    /** Opens a caller-managed transaction for a multi-step business operation. */
    public static SqlSession getTransactionSqlSession(){
        return openSession(false);
    }

    private static SqlSession openSession(boolean autoCommit){
        return getSqlSessionFactory().openSession(autoCommit);
    }

    private static SqlSessionFactory getSqlSessionFactory() {
        if (sqlSessionFactory == null) {
            synchronized (MybatisUtil.class) {
                if (sqlSessionFactory == null) {
                    try (InputStream inputStream = Resources.getResourceAsStream("mybatis-config.xml")) {
                        sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream, databaseProperties());
                    } catch (IOException exception) {
                        throw new IllegalStateException("无法读取 MyBatis 配置。", exception);
                    }
                }
            }
        }
        return sqlSessionFactory;
    }

    private static Properties databaseProperties() {
        Properties properties = new Properties();
        properties.setProperty("db.driver", value("hospital.db.driver", "HOSPITAL_DB_DRIVER", "com.mysql.cj.jdbc.Driver"));
        properties.setProperty("db.url", value("hospital.db.url", "HOSPITAL_DB_URL", "jdbc:mysql://127.0.0.1:3306/hospt?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai"));
        properties.setProperty("db.username", value("hospital.db.username", "HOSPITAL_DB_USERNAME", "root"));
        properties.setProperty("db.password", value("hospital.db.password", "HOSPITAL_DB_PASSWORD", ""));
        return properties;
    }

    private static String value(String systemProperty, String environmentVariable, String defaultValue) {
        String configured = System.getProperty(systemProperty);
        if (configured == null || configured.trim().isEmpty()) configured = System.getenv(environmentVariable);
        return configured == null ? defaultValue : configured.trim();
    }
}
