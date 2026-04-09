package services;

import entities.Produit;
import interfaces.IService;
import utils.MyConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ProduitService implements IService<Produit> {
    private final Connection conn = MyConnection.getInstance().getConn();

    @Override
    public void ajouter(Produit p) throws SQLException {
        String req = "INSERT INTO produit (nom, description, type, prix, categorie, " +
                "quantite_stock, image, is_promotion, promotion_price, " +
                "location_address, location_start, location_end, created_at, updated_at, banned, vendeur_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW(), ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(req)) {
            ps.setString(1, p.getNom());
            ps.setString(2, p.getDescription());
            ps.setString(3, p.getType());
            ps.setDouble(4, p.getPrix());
            ps.setString(5, p.getCategorie());
            ps.setInt(6, p.getQuantiteStock());
            ps.setString(7, p.getImage());
            ps.setBoolean(8, p.isPromotion());
            ps.setDouble(9, p.getPromotionPrice());
            ps.setString(10, p.getLocationAddress());
            ps.setTimestamp(11, toTimestamp(p.getLocationStart()));
            ps.setTimestamp(12, toTimestamp(p.getLocationEnd()));
            ps.setBoolean(13, p.isBanned());
            ps.setInt(14, p.getVendeurId());
            ps.executeUpdate();
        }
    }

    @Override
    public List<Produit> afficher() throws SQLException {
        List<Produit> list = new ArrayList<>();
        String req = "SELECT * FROM produit WHERE banned = false";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(req)) {
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        }
        return list;
    }

    @Override
    public void modifier(Produit p) throws SQLException {
        String req = "UPDATE produit SET nom=?, description=?, type=?, prix=?, " +
                "categorie=?, quantite_stock=?, is_promotion=?, " +
                "promotion_price=?, location_address=?, location_start=?, location_end=?, updated_at=NOW() WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(req)) {
            ps.setString(1, p.getNom());
            ps.setString(2, p.getDescription());
            ps.setString(3, p.getType());
            ps.setDouble(4, p.getPrix());
            ps.setString(5, p.getCategorie());
            ps.setInt(6, p.getQuantiteStock());
            ps.setBoolean(7, p.isPromotion());
            ps.setDouble(8, p.getPromotionPrice());
            ps.setString(9, p.getLocationAddress());
            ps.setTimestamp(10, toTimestamp(p.getLocationStart()));
            ps.setTimestamp(11, toTimestamp(p.getLocationEnd()));
            ps.setInt(12, p.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String req = "DELETE FROM produit WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(req)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public List<Produit> rechercher(String keyword) throws SQLException {
        List<Produit> list = new ArrayList<>();
        String req = "SELECT * FROM produit WHERE (nom LIKE ? OR categorie LIKE ?) AND banned = false";
        try (PreparedStatement ps = conn.prepareStatement(req)) {
            String kw = "%" + keyword + "%";
            ps.setString(1, kw);
            ps.setString(2, kw);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSet(rs));
                }
            }
        }
        return list;
    }

    public List<Produit> getByType(String type) throws SQLException {
        List<Produit> list = new ArrayList<>();
        String req = "SELECT * FROM produit WHERE type=? AND banned=false";
        try (PreparedStatement ps = conn.prepareStatement(req)) {
            ps.setString(1, type);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSet(rs));
                }
            }
        }
        return list;
    }

    public int countAll() throws SQLException {
        String req = "SELECT COUNT(*) FROM produit WHERE banned=false";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(req)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        }
    }

    private Produit mapResultSet(ResultSet rs) throws SQLException {
        Produit p = new Produit();
        p.setId(rs.getInt("id"));
        p.setNom(rs.getString("nom"));
        p.setDescription(rs.getString("description"));
        p.setType(rs.getString("type"));
        p.setPrix(rs.getDouble("prix"));
        p.setCategorie(rs.getString("categorie"));
        p.setQuantiteStock(rs.getInt("quantite_stock"));
        p.setImage(rs.getString("image"));
        p.setPromotion(rs.getBoolean("is_promotion"));
        p.setPromotionPrice(rs.getDouble("promotion_price"));
        p.setLocationAddress(rs.getString("location_address"));
        Timestamp ls = rs.getTimestamp("location_start");
        if (ls != null) {
            p.setLocationStart(ls.toLocalDateTime());
        }
        Timestamp le = rs.getTimestamp("location_end");
        if (le != null) {
            p.setLocationEnd(le.toLocalDateTime());
        }
        Timestamp ca = rs.getTimestamp("created_at");
        if (ca != null) {
            p.setCreatedAt(ca.toLocalDateTime());
        }
        Timestamp ua = rs.getTimestamp("updated_at");
        if (ua != null) {
            p.setUpdatedAt(ua.toLocalDateTime());
        }
        p.setBanned(rs.getBoolean("banned"));
        p.setVendeurId(rs.getInt("vendeur_id"));
        return p;
    }

    private Timestamp toTimestamp(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }
}

