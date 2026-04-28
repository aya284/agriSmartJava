package services;

import entities.Review;
import utils.MyConnection;

import java.sql.*;
import java.util.*;

/**
 * Service for managing product reviews and ratings.
 * Enforces one review per user per product.
 */
public class ReviewService {

    private final Connection conn = MyConnection.getInstance().getConn();

    /**
     * Initialize the review table if it doesn't exist.
     */
    public void ensureTableExists() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS review (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "produit_id INT NOT NULL, " +
                "user_id INT NOT NULL, " +
                "rating INT NOT NULL, " +
                "comment TEXT, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "UNIQUE KEY unique_review (produit_id, user_id)" +
                ")";
        try (Statement st = conn.createStatement()) {
            st.executeUpdate(sql);
        }
    }

    /**
     * Add a review. Throws if user already reviewed this product.
     */
    public void addReview(Review review) throws SQLException {
        String sql = "INSERT INTO review (produit_id, user_id, rating, comment) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, review.getProduitId());
            ps.setInt(2, review.getUserId());
            ps.setInt(3, review.getRating());
            ps.setString(4, review.getComment());
            ps.executeUpdate();
        }
    }

    /**
     * Get all reviews for a product, with user names joined.
     */
    public List<Review> getReviewsForProduct(int produitId) throws SQLException {
        List<Review> reviews = new ArrayList<>();
        String sql = "SELECT r.*, CONCAT(u.first_name, ' ', u.last_name) AS user_name " +
                "FROM review r " +
                "LEFT JOIN user u ON u.id = r.user_id " +
                "WHERE r.produit_id = ? " +
                "ORDER BY r.created_at DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, produitId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Review r = new Review();
                    r.setId(rs.getInt("id"));
                    r.setProduitId(rs.getInt("produit_id"));
                    r.setUserId(rs.getInt("user_id"));
                    r.setRating(rs.getInt("rating"));
                    r.setComment(rs.getString("comment"));
                    Timestamp ts = rs.getTimestamp("created_at");
                    if (ts != null) r.setCreatedAt(ts.toLocalDateTime());
                    r.setUserName(rs.getString("user_name"));
                    reviews.add(r);
                }
            }
        }
        return reviews;
    }

    /**
     * Get average rating for a single product.
     */
    public double getAverageRating(int produitId) throws SQLException {
        String sql = "SELECT AVG(rating) AS avg_rating FROM review WHERE produit_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, produitId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("avg_rating");
                }
            }
        }
        return 0.0;
    }

    /**
     * Get review count for a single product.
     */
    public int getReviewCount(int produitId) throws SQLException {
        String sql = "SELECT COUNT(*) AS cnt FROM review WHERE produit_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, produitId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("cnt");
            }
        }
        return 0;
    }

    /**
     * Check if user already reviewed this product.
     */
    public boolean hasUserReviewed(int userId, int produitId) throws SQLException {
        String sql = "SELECT COUNT(*) AS cnt FROM review WHERE user_id = ? AND produit_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, produitId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("cnt") > 0;
            }
        }
        return false;
    }

    /**
     * Check if user has purchased this product (required to leave a review).
     */
    public boolean hasUserPurchased(int userId, int produitId) throws SQLException {
        String sql = "SELECT COUNT(*) AS cnt FROM commande c " +
                "JOIN commande_item ci ON ci.commande_id = c.id " +
                "WHERE c.client_id = ? AND ci.produit_id = ? " +
                "AND LOWER(c.statut) IN ('confirmee', 'livree')";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, produitId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("cnt") > 0;
            }
        }
        return false;
    }

    /**
     * Bulk fetch average ratings for all products (for product card display).
     * Returns map of produitId -> average rating.
     */
    public Map<Integer, Double> getAllAverageRatings() throws SQLException {
        Map<Integer, Double> ratings = new HashMap<>();
        String sql = "SELECT produit_id, AVG(rating) AS avg_rating FROM review GROUP BY produit_id";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                ratings.put(rs.getInt("produit_id"), rs.getDouble("avg_rating"));
            }
        }
        return ratings;
    }

    /**
     * Bulk fetch review counts for all products.
     */
    public Map<Integer, Integer> getAllReviewCounts() throws SQLException {
        Map<Integer, Integer> counts = new HashMap<>();
        String sql = "SELECT produit_id, COUNT(*) AS cnt FROM review GROUP BY produit_id";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                counts.put(rs.getInt("produit_id"), rs.getInt("cnt"));
            }
        }
        return counts;
    }
}
