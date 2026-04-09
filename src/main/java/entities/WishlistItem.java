package entities;

import java.time.LocalDateTime;

public class WishlistItem {
    private int id;
    private int userId;
    private int produitId;
    private LocalDateTime createdAt;

    public WishlistItem() {}

    public WishlistItem(int userId, int produitId) {
        this.userId = userId;
        this.produitId = produitId;
        this.createdAt = LocalDateTime.now();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public int getProduitId() { return produitId; }
    public void setProduitId(int produitId) { this.produitId = produitId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "Wishlist: User " + userId + " â†’ Produit " + produitId;
    }
}
