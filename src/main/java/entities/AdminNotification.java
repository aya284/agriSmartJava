package entities;

import java.time.LocalDateTime;

public class AdminNotification {
    private int id;
    private String title;
    private String message;
    private String type;
    private String link;
    private boolean isRead;
    private LocalDateTime createdAt;
    private Integer relatedUserId;

    public AdminNotification() {}

    public AdminNotification(String title, String message, String type, Integer relatedUserId) {
        this.title = title;
        this.message = message;
        this.type = type;
        this.relatedUserId = relatedUserId;
        this.isRead = false;
        this.createdAt = LocalDateTime.now();
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Integer getRelatedUserId() { return relatedUserId; }
    public void setRelatedUserId(Integer relatedUserId) { this.relatedUserId = relatedUserId; }
}
