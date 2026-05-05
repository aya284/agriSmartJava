package entities;

import java.time.LocalDateTime;

/**
 * Represents a product review with a 1-5 star rating and optional comment.
 * One review per user per product is enforced.
 */
public class Review {

    private int id;
    private int produitId;
    private int userId;
    private int rating; // 1-5
    private String comment;
    private LocalDateTime createdAt;
    private String userName; // Joined from user table for display

    public Review() {
        this.createdAt = LocalDateTime.now();
    }

    public Review(int produitId, int userId, int rating, String comment) {
        this();
        this.produitId = produitId;
        this.userId = userId;
        this.rating = rating;
        this.comment = comment;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getProduitId() { return produitId; }
    public void setProduitId(int produitId) { this.produitId = produitId; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = Math.max(1, Math.min(5, rating)); }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    /** Returns filled stars string, e.g. "★★★★☆" for rating 4 */
    public String getStarsDisplay() {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 5; i++) {
            sb.append(i <= rating ? "★" : "☆");
        }
        return sb.toString();
    }
}
