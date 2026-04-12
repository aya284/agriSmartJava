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
}
