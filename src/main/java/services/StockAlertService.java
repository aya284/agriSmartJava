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

    // In-memory store of previous stock levels to detect restocks
    private final Map<Integer, Integer> previousStockLevels = new HashMap<>();

    /**
     * Check all products for stock alerts relevant to the given user.
     * - If user is a seller: alerts for their products with low stock
     * - If user has wishlist items: alerts for restocked products
     *
     * @param userId the current user ID
     * @return list of stock alerts for this user
     */
    public List<StockAlert> checkAlerts(int userId) throws SQLException {
        List<StockAlert> alerts = new ArrayList<>();

        // 1. Low stock alerts for products owned by this user
        alerts.addAll(checkLowStockForSeller(userId));

        // 2. Restock alerts for products in this user's wishlist
        alerts.addAll(checkRestockForWishlistUser(userId));

        return alerts;
    }

    /**
     * Detect products owned by the seller that have low stock (≤ threshold).
     */
    private List<StockAlert> checkLowStockForSeller(int userId) throws SQLException {
        List<StockAlert> alerts = new ArrayList<>();
        String sql = "SELECT id, nom, quantite_stock FROM produit " +
                "WHERE vendeur_id = ? AND banned = FALSE AND quantite_stock <= ? AND quantite_stock > 0";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, LOW_STOCK_THRESHOLD);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int prodId = rs.getInt("id");
                    String nom = rs.getString("nom");
                    int stock = rs.getInt("quantite_stock");

                    alerts.add(new StockAlert(
                            AlertType.LOW_STOCK,
                            prodId,
                            nom,
                            userId,
                            "⚠ Stock faible: \"" + nom + "\" n'a plus que " + stock + " unite(s) en stock."
                    ));
                }
            }
        }
        return alerts;
    }

    /**
     * Detect products on the user's wishlist that have been restocked
     * (stock went from 0 to > 0, or increased significantly).
     */
    private List<StockAlert> checkRestockForWishlistUser(int userId) throws SQLException {
        List<StockAlert> alerts = new ArrayList<>();
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

                    Integer prevStock = previousStockLevels.get(prodId);
                    if (prevStock != null && prevStock == 0 && currentStock > 0) {
                        alerts.add(new StockAlert(
                                AlertType.RESTOCK,
                                prodId,
                                nom,
                                userId,
                                "✅ Bonne nouvelle ! \"" + nom + "\" est de nouveau en stock (" + currentStock + " disponible(s))."
                        ));
                    }

                    previousStockLevels.put(prodId, currentStock);
                }
            }
        }
        return alerts;
    }

    /**
     * Snapshot current stock levels so we can detect changes on next check.
     * Call this when the marketplace loads to establish a baseline.
     */
    public void snapshotStockLevels() throws SQLException {
        previousStockLevels.clear();
        String sql = "SELECT id, quantite_stock FROM produit WHERE banned = FALSE";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                previousStockLevels.put(rs.getInt("id"), rs.getInt("quantite_stock"));
            }
        }
    }

    /**
     * Check if a specific product has low stock (for immediate feedback after purchase).
     */
    public StockAlert checkProductStock(int produitId, int sellerId) throws SQLException {
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
}
