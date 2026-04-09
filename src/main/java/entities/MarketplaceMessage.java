package entities;

import java.time.LocalDateTime;

public class MarketplaceMessage {
    private int id;
    private int conversationId;
    private int senderId;
    private String content;
    private LocalDateTime createdAt;
    private boolean isRead;
    private LocalDateTime readAt;
    private String audioPath;

    public MarketplaceMessage() {}

    public MarketplaceMessage(int conversationId, int senderId, String content) {
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.content = content;
        this.createdAt = LocalDateTime.now();
        this.isRead = false;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getConversationId() { return conversationId; }
    public void setConversationId(int conversationId) { this.conversationId = conversationId; }
    public int getSenderId() { return senderId; }
    public void setSenderId(int senderId) { this.senderId = senderId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
    public LocalDateTime getReadAt() { return readAt; }
    public void setReadAt(LocalDateTime readAt) { this.readAt = readAt; }
    public String getAudioPath() { return audioPath; }
    public void setAudioPath(String audioPath) { this.audioPath = audioPath; }
}
