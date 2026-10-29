package com.ustb.hospital.service;

import com.ustb.hospital.mapper.AnnouncementsMapper;
import com.ustb.hospital.utils.AppointmentOperationResult;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.*;
import org.junit.Test;

import java.io.InputStream;
import java.sql.Statement;
import java.util.Properties;
import java.util.UUID;

import static org.junit.Assert.*;

/** Real announcement mapper behavior in an isolated, in-memory database. */
public class AnnouncementLifecycleTest {
    @Test public void savedDraftIsReportedAsDraftAndOnlyBecomesPublicAfterPublish() throws Exception {
        Properties properties = new Properties();
        properties.setProperty("db.driver", "org.h2.Driver");
        properties.setProperty("db.url", "jdbc:h2:mem:announcement_" + UUID.randomUUID().toString().replace("-", "") + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        properties.setProperty("db.username", "sa");
        properties.setProperty("db.password", "");
        SqlSessionFactory factory;
        try (InputStream input = Resources.getResourceAsStream("mybatis-config.xml")) { factory = new SqlSessionFactoryBuilder().build(input, properties); }
        try (SqlSession session = factory.openSession(true); Statement sql = session.getConnection().createStatement()) {
            sql.execute("CREATE TABLE announcement(announcement_id INT AUTO_INCREMENT PRIMARY KEY,title VARCHAR(255),content VARCHAR(10000),creation_time VARCHAR(50),creator VARCHAR(100),status VARCHAR(20),published_at VARCHAR(50),updated_at VARCHAR(50),creator_admin_id INT)");
            sql.execute("CREATE TABLE audit_logs(audit_log_id BIGINT AUTO_INCREMENT PRIMARY KEY,actor_role VARCHAR(20),actor_id INT,action VARCHAR(80),target_type VARCHAR(80),target_id VARCHAR(80),outcome VARCHAR(20),request_method VARCHAR(10),request_path VARCHAR(255),remote_address VARCHAR(64),created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        }
        AnnouncementAdminService service = new AnnouncementAdminService(() -> factory.openSession(false), () -> factory.openSession(true), new AuditService(() -> factory.openSession(true)));
        AuditContext admin = new AuditContext("ADMIN", 1, "POST", "/portal/admin/announcements", "127.0.0.1");
        AppointmentOperationResult created = service.create("门诊安排", "公告正文", admin);
        assertTrue(created.isSuccessful());
        try (SqlSession session = factory.openSession(true)) {
            AnnouncementsMapper mapper = session.getMapper(AnnouncementsMapper.class);
            assertEquals("DRAFT", mapper.selectById(1).getStatus());
            assertTrue(mapper.selectPublished().isEmpty());
        }
        assertTrue("A saved draft must not be reported as already published: " + created.getMessage(), created.getMessage().contains("草稿"));
        assertTrue(service.changeStatus(1, true, admin).isSuccessful());
        try (SqlSession session = factory.openSession(true)) { assertEquals(1, session.getMapper(AnnouncementsMapper.class).selectPublished().size()); }
        assertTrue(service.changeStatus(1, false, admin).isSuccessful());
        try (SqlSession session = factory.openSession(true)) {
            AnnouncementsMapper mapper = session.getMapper(AnnouncementsMapper.class);
            assertEquals("OFFLINE", mapper.selectById(1).getStatus());
            assertTrue(mapper.selectPublished().isEmpty());
        }
    }
}
