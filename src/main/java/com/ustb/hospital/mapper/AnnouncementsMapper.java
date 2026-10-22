package com.ustb.hospital.mapper;

import com.ustb.hospital.entity.Announcements;

import java.util.List;

public interface AnnouncementsMapper {
    List<Announcements> selectAll();
    List<Announcements> selectPublished();
    Announcements selectById(int announcementId);
    int insertPortalAdmin(Announcements announcement);
    int updatePortalAdmin(Announcements announcement);
    int updateStatus(@org.apache.ibatis.annotations.Param("announcementId") int announcementId,
                     @org.apache.ibatis.annotations.Param("status") String status,
                     @org.apache.ibatis.annotations.Param("publishedAt") String publishedAt);
    int deletePortalAdmin(int announcementId);
}
