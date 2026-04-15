package services;

import entities.Ressource;
import interfaces.IService;
import utils.MyConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RessourceService implements IService<Ressource> {
    private Connection connection;

    public RessourceService() {
        connection = MyConnection.getInstance().getConn();
    }

    @Override
    public void ajouter(Ressource r) throws SQLException {
        String query = "INSERT INTO ressource (nom, type, stock_restan, unite, user_id) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, r.getNom());
            ps.setString(2, r.getType());
            ps.setDouble(3, r.getStockRestant());
            ps.setString(4, r.getUnite());
            ps.setInt(5, r.getUserId());
            ps.executeUpdate();
        }
    }

    @Override
    public void modifier(Ressource r) throws SQLException {
        String query = "UPDATE ressource SET nom=?, type=?, stock_restan=?, unite=? WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, r.getNom());
            ps.setString(2, r.getType());
            ps.setDouble(3, r.getStockRestant());
            ps.setString(4, r.getUnite());
            ps.setInt(5, r.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String query = "DELETE FROM ressource WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public List<Ressource> afficher() throws SQLException {
        List<Ressource> list = new ArrayList<>();
        String query = "SELECT * FROM ressource";
        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                list.add(new Ressource(
                        rs.getInt("id"),
                        rs.getString("nom"),
                        rs.getString("type"),
                        rs.getDouble("stock_restan"),
                        rs.getString("unite"),
                        rs.getInt("user_id")));
            }
        }
        return list;
    }

    public List<Ressource> afficherByUser(int userId) throws SQLException {
        List<Ressource> list = new ArrayList<>();
        String query = "SELECT * FROM ressource WHERE user_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Ressource(
                            rs.getInt("id"),
                            rs.getString("nom"),
                            rs.getString("type"),
                            rs.getDouble("stock_restan"),
                            rs.getString("unite"),
                            rs.getInt("user_id")));
                }
            }
        }
        return list;
    }

    // ── ADMIN METHODS ──────────────────────────────────────────

    public int countRessources(String keyword, String type) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM ressource r LEFT JOIN users u ON r.user_id = u.id WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (r.nom LIKE ? OR u.first_name LIKE ? OR u.last_name LIKE ?)");
            String k = "%" + keyword.trim() + "%";
            params.add(k); params.add(k); params.add(k);
        }
        if (type != null && !type.equals("Tous")) {
            sql.append(" AND r.type = ?");
            params.add(type);
        }

        try (PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++)
                ps.setObject(i + 1, params.get(i));
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public List<Ressource> searchRessourcesPaged(String keyword, String type, int page, int pageSize) throws SQLException {
        StringBuilder sql = new StringBuilder("""
            SELECT r.*, u.first_name, u.last_name 
            FROM ressource r 
            LEFT JOIN users u ON r.user_id = u.id 
            WHERE 1=1
            """);
        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (r.nom LIKE ? OR u.first_name LIKE ? OR u.last_name LIKE ?)");
            String k = "%" + keyword.trim() + "%";
            params.add(k); params.add(k); params.add(k);
        }
        if (type != null && !type.equals("Tous")) {
            sql.append(" AND r.type = ?");
            params.add(type);
        }

        sql.append(" ORDER BY r.id DESC LIMIT ? OFFSET ?");
        params.add(pageSize);
        params.add((page - 1) * pageSize);

        List<Ressource> list = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++)
                ps.setObject(i + 1, params.get(i));
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Ressource r = new Ressource(
                        rs.getInt("id"),
                        rs.getString("nom"),
                        rs.getString("type"),
                        rs.getDouble("stock_restan"),
                        rs.getString("unite"),
                        rs.getInt("user_id")
                    );
                    String fname = rs.getString("first_name");
                    String lname = rs.getString("last_name");
                    r.setUserName((fname != null ? fname : "") + " " + (lname != null ? lname : ""));
                    list.add(r);
                }
            }
        }
        return list;
    }
}
