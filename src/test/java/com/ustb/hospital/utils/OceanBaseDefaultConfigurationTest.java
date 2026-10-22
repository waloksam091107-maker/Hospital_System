package com.ustb.hospital.utils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Guards the database defaults used when no deployment-specific JVM properties are supplied. */
public class OceanBaseDefaultConfigurationTest {
    private static final String[] PROPERTY_NAMES = {
            "hospital.db.driver",
            "hospital.db.url",
            "hospital.db.username",
            "hospital.db.password"
    };

    private final Properties savedProperties = new Properties();

    @Before
    public void clearDatabaseSystemProperties() {
        for (String propertyName : PROPERTY_NAMES) {
            String value = System.getProperty(propertyName);
            if (value != null) {
                savedProperties.setProperty(propertyName, value);
            }
            System.clearProperty(propertyName);
        }
    }

    @After
    public void restoreDatabaseSystemProperties() {
        for (String propertyName : PROPERTY_NAMES) {
            System.clearProperty(propertyName);
            String value = savedProperties.getProperty(propertyName);
            if (value != null) {
                System.setProperty(propertyName, value);
            }
        }
    }

    @Test
    public void usesOceanBaseWhenNoRuntimeOverrideIsSupplied() throws Exception {
        Properties properties = databaseProperties();

        assertEquals("com.oceanbase.jdbc.Driver", properties.getProperty("db.driver"));
        assertTrue(properties.getProperty("db.url").startsWith("jdbc:oceanbase://"));
        assertEquals("hospital_app@test", properties.getProperty("db.username"));
    }

    @Test
    public void givesJvmPropertiesPriorityOverDeploymentDefaults() throws Exception {
        System.setProperty("hospital.db.driver", "example.Driver");
        System.setProperty("hospital.db.url", "jdbc:example://database/hospital");
        System.setProperty("hospital.db.username", "override-user");
        System.setProperty("hospital.db.password", "override-password");

        Properties properties = databaseProperties();

        assertEquals("example.Driver", properties.getProperty("db.driver"));
        assertEquals("jdbc:example://database/hospital", properties.getProperty("db.url"));
        assertEquals("override-user", properties.getProperty("db.username"));
        assertEquals("override-password", properties.getProperty("db.password"));
    }

    private Properties databaseProperties() throws Exception {
        Method method = MybatisUtil.class.getDeclaredMethod("databaseProperties");
        method.setAccessible(true);
        return (Properties) method.invoke(null);
    }
}
