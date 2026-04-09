package services;

import entities.WishlistItem;
import interfaces.IService;
import utils.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class WishlistService implements IService<WishlistItem> {
    private final Connection conn = MyConnection.getInstance().getConn();

    @Override
    public void ajouter(WishlistItem item) throws SQLException {
        if (exists(item.getUserId(), item.getProduitId())) {
            return;
        }
        String req = "INSERT INTO wishlist_item (user_id, produit_id, created_at) VALUES (?, ?, NOW())";
        try (PreparedStatement ps = conn.prepareStatement(req)) {
            ps.setInt(1, item.getUserId());
            ps.setInt(2, item.getProduitId());
            ps.executeUpdate();
        }
    }

    @Override
    public List<WishlistItem> afficher() throws SQLException {
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
        String req = "DELETE FROM wishlist_item WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(req)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public List<WishlistItem> getByUser(int userId) throws SQLException {
        List<WishlistItem> list = new ArrayList<>();
        String req = "SELECT * FROM wishlist_item WHERE user_id=?";
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
}

