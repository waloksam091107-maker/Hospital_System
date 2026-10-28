package com.ustb.hospital.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ustb.hospital.entity.Appointments;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.Test;

import java.io.InputStream;
import java.sql.Statement;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static org.junit.Assert.*;

public class PatientAppointmentListMappingTest {
    @Test
    public void patientListReturnsRescheduleLinkWithoutLeakingOtherPatients() throws Exception {
        Properties properties = new Properties();
        properties.setProperty("db.driver", "org.h2.Driver");
        properties.setProperty("db.url", "jdbc:h2:mem:patient_list_" + UUID.randomUUID().toString().replace("-", "") + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        properties.setProperty("db.username", "sa");
        properties.setProperty("db.password", "");

        SqlSessionFactory factory;
        try (InputStream config = Resources.getResourceAsStream("mybatis-config.xml")) {
            factory = new SqlSessionFactoryBuilder().build(config, properties);
        }
        try (SqlSession session = factory.openSession(true); Statement sql = session.getConnection().createStatement()) {
            sql.execute("CREATE TABLE doctors(doctor_id INT PRIMARY KEY, name VARCHAR(100), department_id INT, professional_title_id INT)");
            sql.execute("CREATE TABLE departments(department_id INT PRIMARY KEY, department_name VARCHAR(100))");
            sql.execute("CREATE TABLE professional_titles(id INT PRIMARY KEY, title_name VARCHAR(100))");
            sql.execute("CREATE TABLE appointments(appointment_id INT PRIMARY KEY, patient_id INT, doctor_id INT, appointment_date VARCHAR(10), appointment_time VARCHAR(10), status VARCHAR(20), previous_appointment_id INT)");
            sql.execute("INSERT INTO doctors VALUES (1, 'Doctor', 1, 1)");
            sql.execute("INSERT INTO departments VALUES (1, 'Internal medicine')");
            sql.execute("INSERT INTO professional_titles VALUES (1, 'Physician')");
            sql.execute("INSERT INTO appointments VALUES (100, 1, 1, '2030-01-01', 'AM', 'RESCHEDULED', NULL), (101, 1, 1, '2030-01-02', 'AM', 'BOOKED', 100), (200, 2, 1, '2030-01-03', 'AM', 'BOOKED', 199)");
            com.ustb.hospital.support.ExtensionTestSchema.install(session.getConnection());
            sql.execute("UPDATE appointments SET appointment_sequence=7,queue_status='WAITING' WHERE appointment_id=101");

            sql.execute("INSERT INTO clinic_leave(doctor_id,start_date,end_date,periods,reason,status) VALUES(1,'2030-01-02','2030-01-02','AM','停诊','APPROVED')");
            List<Appointments> records = session.getMapper(AppointmentsMapper.class).selectByPatientIdWithDoctorDetails(1);
            assertEquals(2, records.size());
            Appointments original = records.stream().filter(a -> a.getAppointmentId() == 100).findFirst().orElseThrow(AssertionError::new);
            Appointments rescheduled = records.stream().filter(a -> a.getAppointmentId() == 101).findFirst().orElseThrow(AssertionError::new);
            assertNull(original.getPreviousAppointmentId());
            assertEquals(Integer.valueOf(100), rescheduled.getPreviousAppointmentId());
            JsonNode serialized = new ObjectMapper().valueToTree(rescheduled);
            assertEquals(100, serialized.get("previousAppointmentId").asInt());
            assertTrue("预约列表应显示已批准停诊提示", serialized.get("leaveBlocked").asBoolean());
            assertEquals(7, serialized.get("appointmentSequence").asInt());
            assertEquals("WAITING", serialized.get("queueStatus").asText());
        }
    }
}
