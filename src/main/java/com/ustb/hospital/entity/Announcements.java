package com.ustb.hospital.entity;

public class Announcements {


    private Integer announcementId;
    private String title;
    private String content;
    private String creationTime;
    private String creator;
    private String status;
    private String publishedAt;
    private String updatedAt;
    private Integer creatorAdminId;

    public Integer getAnnouncementId() {
        return announcementId;
    }

    public void setAnnouncementId(Integer announcementId) {
        this.announcementId = announcementId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(String creationTime) {
        this.creationTime = creationTime;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPublishedAt() { return publishedAt; }
    public void setPublishedAt(String publishedAt) { this.publishedAt = publishedAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    public Integer getCreatorAdminId() { return creatorAdminId; }
    public void setCreatorAdminId(Integer creatorAdminId) { this.creatorAdminId = creatorAdminId; }

    @Override
    public String toString() {
        return "Announcements{" +
                "announcementId=" + announcementId +
                ", title='" + title + '\'' +
                ", content='" + content + '\'' +
                ", creationTime='" + creationTime + '\'' +
                ", creator='" + creator + '\'' +
                '}';
    }
}
