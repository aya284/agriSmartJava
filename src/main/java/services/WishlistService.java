package services;

import entities.WishlistItem;
import interfaces.IService;
import utils.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class WishlistService implements IService<WishlistItem> {
    private final Connection conn = MyConnection.getInstance().getConn();

    public WishlistService() {
        try {
            ensureWishlistTable();
        } catch (SQLException ignored) {
            // Best effort initialization. CRUD methods throw explicit SQL errors.
        }
    }

    @Override
    public void ajouter(WishlistItem item) throws SQLException {
        ensureWishlistTable();
        String req = "INSERT INTO wishlist_item (user_id, produit_id, created_at) VALUES (?, ?, NOW()) " +
                "ON DUPLICATE KEY UPDATE created_at = created_at";
        try (PreparedStatement ps = conn.prepareStatement(req)) {
            ps.setInt(1, item.getUserId());
            ps.setInt(2, item.getProduitId());
            ps.executeUpdate();
        }
    }

    @Override
    public List<WishlistItem> afficher() throws SQLException {
        ensureWishlistTable();
        List<WishlistItem> list = new ArrayList<>();
        String req = "SELECT * FROM wishlist_item";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(req)) {
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        }
        return list;
    }

    @Override
    public void modifier(WishlistItem item) throws SQLException {
        ensureWishlistTable();
        String req = "UPDATE wishlist_item SET user_id=?, produit_id=? WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(req)) {
            ps.setInt(1, item.getUserId());
            ps.setInt(2, item.getProduitId());
            ps.setInt(3, item.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void supprimer(int id) throws SQLException {
        ensureWishlistTable();
        String req = "DELETE FROM wishlist_item WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(req)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public void supprimerByUserAndProduit(int userId, int produitId) throws SQLException {
        ensureWishlistTable();
        String req = "DELETE FROM wishlist_item WHERE user_id=? AND produit_id=?";
        try (PreparedStatement ps = conn.prepareStatement(req)) {
            ps.setInt(1, userId);
            ps.setInt(2, produitId);
            ps.executeUpdate();
        }
    }

    public List<WishlistItem> getByUser(int userId) throws SQLException {
        ensureWishlistTable();
        List<WishlistItem> list = new ArrayList<>();
        String req = "SELECT * FROM wishlist_item WHERE user_id=? ORDER BY created_at DESC";
        try (PreparedStatement ps = conn.prepareStatement(req)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSet(rs));
                }
            }
        }
        return list;
    }

    public boolean exists(int userId, int produitId) throws SQLException {
        ensureWishlistTable();
        String req = "SELECT COUNT(*) FROM wishlist_item WHERE user_id=? AND produit_id=?";
        try (PreparedStatement ps = conn.prepareStatement(req)) {
            ps.setInt(1, userId);
            ps.setInt(2, produitId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    public int countAll() throws SQLException {
        ensureWishlistTable();
        String req = "SELECT COUNT(*) FROM wishlist_item";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(req)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        }
    }

    private WishlistItem mapResultSet(ResultSet rs) throws SQLException {
        WishlistItem item = new WishlistItem();
        item.setId(rs.getInt("id"));
        item.setUserId(rs.getInt("user_id"));
        item.setProduitId(rs.getInt("produit_id"));
        Timestamp ca = rs.getTimestamp("created_at");
        if (ca != null) item.setCreatedAt(ca.toLocalDateTime());
        return item;
    }

    private void ensureWishlistTable() throws SQLException {
        String ddl = "CREATE TABLE IF NOT EXISTS wishlist_item (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "user_id INT NOT NULL," +
                "produit_id INT NOT NULL," +
                "created_at DATETIME NOT NULL," +
                "UNIQUE KEY uq_wishlist_user_produit (user_id, produit_id)," +
                "INDEX idx_wishlist_user (user_id)," +
                "INDEX idx_wishlist_produit (produit_id)" +
                ")";
        try (Statement st = conn.createStatement()) {
            st.execute(ddl);
        }
    }
}

