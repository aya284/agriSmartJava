package entities;

import java.time.LocalDateTime;

public class MarketplaceConversation {
    private int id;
    private int produitId;
    private int buyerId;
    private int sellerId;
    private LocalDateTime createdAt;
    private LocalDateTime lastMessageAt;

    public MarketplaceConversation() {}

    public MarketplaceConversation(int produitId, int buyerId, int sellerId) {
        this.produitId = produitId;
        this.buyerId = buyerId;
        this.sellerId = sellerId;
        this.createdAt = LocalDateTime.now();
        this.lastMessageAt = LocalDateTime.now();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getProduitId() { return produitId; }
    public void setProduitId(int produitId) { this.produitId = produitId; }
    public int getBuyerId() { return buyerId; }
    public void setBuyerId(int buyerId) { this.buyerId = buyerId; }
    public int getSellerId() { return sellerId; }
    public void setSellerId(int sellerId) { this.sellerId = sellerId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getLastMessageAt() { return lastMessageAt; }
    public void setLastMessageAt(LocalDateTime lastMessageAt) { this.lastMessageAt = lastMessageAt; }
}
