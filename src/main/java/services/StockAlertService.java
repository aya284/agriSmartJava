package services;

import entities.StockAlert;
import entities.StockAlert.AlertType;
import utils.MyConnection;

import java.sql.*;
import java.util.*;

/**
 * Service that detects low-stock and restock events by querying the database.
 * Generates in-app alerts for sellers (low stock) and wishlist users (restock).
 */
public class StockAlertService {

    private final Connection conn = MyConnection.getInstance().getConn();

    private static final int LOW_STOCK_THRESHOLD = 5;
    private static final int DEDUP_HOURS = 24;

    private volatile boolean schemaReady = false;

    /**
     * Check all products for stock alerts relevant to the given user.
     * - If user is a seller: alerts for their products with low stock
     * - If user has wishlist items: alerts for restocked products
     *
     * @param userId the current user ID
     * @return list of stock alerts for this user
     */
    public List<StockAlert> checkAlerts(int userId) throws SQLException {
        ensureSchema();
        detectAndPersistAlerts(userId);
        return getAlertsForUser(userId);
    }

    /**
     * Detect products owned by the seller that have low stock (≤ threshold).
     */
    private void checkLowStockForSeller(int userId) throws SQLException {
        String sql = "SELECT id, nom, quantite_stock FROM produit " +
                "WHERE vendeur_id = ? AND banned = FALSE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int prodId = rs.getInt("id");
                    String nom = rs.getString("nom");
                    int stock = rs.getInt("quantite_stock");

                    if (stock <= LOW_STOCK_THRESHOLD && stock >= 0) {
                        String message = stock == 0
                                ? "Rupture: \"" + nom + "\" est en rupture de stock."
                                : "Stock faible: \"" + nom + "\" n'a plus que " + stock + " unite(s) en stock.";
                        String severity = stock == 0 ? "zero" : "low";
                        persistAlertIfNeeded(AlertType.LOW_STOCK, prodId, nom, userId, message, severity);
                    } else {
                        markLowStockResolved(userId, prodId);
                    }
                }
            }
        }
    }

    /**
     * Detect products on the user's wishlist that have been restocked
     * (stock went from 0 to > 0, or increased significantly).
     */
    private void checkRestockForWishlistUser(int userId) throws SQLException {
        String sql = "SELECT p.id, p.nom, p.quantite_stock " +
                "FROM wishlist_item w " +
                "JOIN produit p ON p.id = w.produit_id " +
                "WHERE w.user_id = ? AND p.banned = FALSE AND p.quantite_stock > 0";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int prodId = rs.getInt("id");
                    String nom = rs.getString("nom");
                    int currentStock = rs.getInt("quantite_stock");

                    Integer prevStock = getSnapshotStock(prodId);
                    if (prevStock != null && prevStock == 0 && currentStock > 0) {
                        String message = "Bonne nouvelle ! \"" + nom + "\" est de nouveau en stock (" + currentStock + " disponible(s)).";
                        persistAlertIfNeeded(AlertType.RESTOCK, prodId, nom, userId, message, "restock");
                    }

                    upsertSnapshotStock(prodId, currentStock);
                }
            }
        }
    }

    /**
     * Snapshot current stock levels so we can detect changes on next check.
     * Call this when the marketplace loads to establish a baseline.
     */
    public void snapshotStockLevels() throws SQLException {
        ensureSchema();
        String sql = "SELECT id, quantite_stock FROM produit WHERE banned = FALSE";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                upsertSnapshotStock(rs.getInt("id"), rs.getInt("quantite_stock"));
            }
        }
    }

    /**
     * Check if a specific product has low stock (for immediate feedback after purchase).
     */
    public StockAlert checkProductStock(int produitId, int sellerId) throws SQLException {
        ensureSchema();
        String sql = "SELECT nom, quantite_stock FROM produit WHERE id = ? AND banned = FALSE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, produitId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int stock = rs.getInt("quantite_stock");
                    String nom = rs.getString("nom");
                    if (stock <= LOW_STOCK_THRESHOLD && stock >= 0) {
                        return new StockAlert(
                                AlertType.LOW_STOCK, produitId, nom, sellerId,
                                stock == 0
                                        ? "🔴 Rupture ! \"" + nom + "\" est en rupture de stock."
                                        : "⚠ Stock faible: \"" + nom + "\" — " + stock + " unite(s) restante(s)."
                        );
                    }
                }
            }
        }
        return null;
    }

    private void detectAndPersistAlerts(int userId) throws SQLException {
        checkLowStockForSeller(userId);
        checkRestockForWishlistUser(userId);
    }

    private List<StockAlert> getAlertsForUser(int userId) throws SQLException {
        List<StockAlert> alerts = new ArrayList<>();
        String sql = "SELECT id, alert_type, produit_id, produit_nom, target_user_id, message, is_read, created_at " +
                "FROM stock_alert WHERE target_user_id = ? ORDER BY is_read ASC, created_at DESC LIMIT 100";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    StockAlert alert = new StockAlert();
                    alert.setId(rs.getInt("id"));
                    String rawType = safe(rs.getString("alert_type")).toUpperCase(Locale.ROOT);
                    alert.setType("RESTOCK".equals(rawType) ? AlertType.RESTOCK : AlertType.LOW_STOCK);
                    alert.setProduitId(rs.getInt("produit_id"));
                    alert.setProduitNom(rs.getString("produit_nom"));
                    alert.setTargetUserId(rs.getInt("target_user_id"));
                    alert.setMessage(rs.getString("message"));
                    alert.setRead(rs.getBoolean("is_read"));
                    Timestamp created = rs.getTimestamp("created_at");
                    if (created != null) {
                        alert.setCreatedAt(created.toLocalDateTime());
                    }
                    alerts.add(alert);
                }
            }
        }
        return alerts;
    }

    private void persistAlertIfNeeded(AlertType type, int produitId, String produitNom, int targetUserId,
                                      String message, String eventKey) throws SQLException {
        if (isDuplicateRecentAlert(type, produitId, targetUserId, eventKey)) {
            return;
        }
        String sql = "INSERT INTO stock_alert(alert_type, produit_id, produit_nom, target_user_id, message, is_read, event_key, created_at) " +
                "VALUES (?, ?, ?, ?, ?, FALSE, ?, NOW())";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, type.name());
            ps.setInt(2, produitId);
            ps.setString(3, produitNom);
            ps.setInt(4, targetUserId);
            ps.setString(5, message);
            ps.setString(6, safe(eventKey));
            ps.executeUpdate();
        }
    }

    private boolean isDuplicateRecentAlert(AlertType type, int produitId, int targetUserId, String eventKey) throws SQLException {
        String sql = "SELECT 1 FROM stock_alert " +
                "WHERE alert_type = ? AND produit_id = ? AND target_user_id = ? AND event_key = ? " +
                "AND created_at >= DATE_SUB(NOW(), INTERVAL ? HOUR) LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, type.name());
            ps.setInt(2, produitId);
            ps.setInt(3, targetUserId);
            ps.setString(4, safe(eventKey));
            ps.setInt(5, DEDUP_HOURS);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private void markLowStockResolved(int userId, int produitId) throws SQLException {
        String sql = "UPDATE stock_alert SET is_read = TRUE " +
                "WHERE target_user_id = ? AND produit_id = ? AND alert_type = 'LOW_STOCK' AND is_read = FALSE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, produitId);
            ps.executeUpdate();
        }
    }

    private Integer getSnapshotStock(int produitId) throws SQLException {
        String sql = "SELECT stock_qty FROM stock_snapshot WHERE produit_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, produitId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("stock_qty");
                }
            }
        }
        return null;
    }

    private void upsertSnapshotStock(int produitId, int stockQty) throws SQLException {
        String updateSql = "UPDATE stock_snapshot SET stock_qty = ?, updated_at = NOW() WHERE produit_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
            ps.setInt(1, stockQty);
            ps.setInt(2, produitId);
            int updated = ps.executeUpdate();
            if (updated > 0) {
                return;
            }
        }

        String insertSql = "INSERT INTO stock_snapshot(produit_id, stock_qty, updated_at) VALUES (?, ?, NOW())";
        try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
            ps.setInt(1, produitId);
            ps.setInt(2, stockQty);
            ps.executeUpdate();
        }
    }

    private synchronized void ensureSchema() throws SQLException {
        if (schemaReady) {
            return;
        }
        try (Statement st = conn.createStatement()) {
            st.execute("""
                    CREATE TABLE IF NOT EXISTS stock_alert (
                      id INT AUTO_INCREMENT PRIMARY KEY,
                      alert_type VARCHAR(32) NOT NULL,
                      produit_id INT NOT NULL,
                      produit_nom VARCHAR(255),
                      target_user_id INT NOT NULL,
                      message VARCHAR(512) NOT NULL,
                      is_read BOOLEAN NOT NULL DEFAULT FALSE,
                      event_key VARCHAR(64) NOT NULL,
                      created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                      INDEX idx_stock_alert_user_created (target_user_id, created_at),
                      INDEX idx_stock_alert_lookup (alert_type, produit_id, target_user_id, event_key)
                    )
                    """);

            st.execute("""
                    CREATE TABLE IF NOT EXISTS stock_snapshot (
                      produit_id INT PRIMARY KEY,
                      stock_qty INT NOT NULL,
                      updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                    )
                    """);
        }
        schemaReady = true;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
