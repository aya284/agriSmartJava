package entities;

import java.time.LocalDateTime;

/**
 * Represents a stock alert notification for either sellers (low stock)
 * or wishlist users (product restocked).
 */
public class StockAlert {

    public enum AlertType { LOW_STOCK, RESTOCK }

    private int id;
    private AlertType type;
    private int produitId;
    private String produitNom;
    private int targetUserId;
    private String message;
    private boolean read;
    private LocalDateTime createdAt;

    public StockAlert() {
        this.createdAt = LocalDateTime.now();
        this.read = false;
    }

    public StockAlert(AlertType type, int produitId, String produitNom,
                      int targetUserId, String message) {
        this();
        this.type = type;
        this.produitId = produitId;
        this.produitNom = produitNom;
        this.targetUserId = targetUserId;
        this.message = message;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public AlertType getType() { return type; }
    public void setType(AlertType type) { this.type = type; }
    public int getProduitId() { return produitId; }
    public void setProduitId(int produitId) { this.produitId = produitId; }
    public String getProduitNom() { return produitNom; }
    public void setProduitNom(String produitNom) { this.produitNom = produitNom; }
    public int getTargetUserId() { return targetUserId; }
    public void setTargetUserId(int targetUserId) { this.targetUserId = targetUserId; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
