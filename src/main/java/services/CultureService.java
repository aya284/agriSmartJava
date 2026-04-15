package services;

import entities.Culture;
import interfaces.IService;
import utils.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CultureService implements IService<Culture> {
    private Connection conn = MyConnection.getInstance().getConn();

    @Override
    public void ajouter(Culture c) throws SQLException {
        String query = "INSERT INTO culture (type_culture, variete, date_plantation, date_recolte_prevue, statut, parcelle_id) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, c.getTypeCulture());
            ps.setString(2, c.getVariete());
            ps.setDate(3, Date.valueOf(c.getDatePlantation()));
            ps.setDate(4, Date.valueOf(c.getDateRecoltePrevue()));
            ps.setString(5, c.getStatut());
            ps.setInt(6, c.getParcelleId());
            ps.executeUpdate();
        }
    }

    @Override
    public void modifier(Culture c) throws SQLException {
        String query = "UPDATE culture SET type_culture=?, variete=?, date_plantation=?, date_recolte_prevue=?, statut=? WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, c.getTypeCulture());
            ps.setString(2, c.getVariete());
            ps.setDate(3, Date.valueOf(c.getDatePlantation()));
            ps.setDate(4, Date.valueOf(c.getDateRecoltePrevue()));
            ps.setString(5, c.getStatut());
            ps.setInt(6, c.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void supprimer(int id) throws SQLException {
        // Supprimer d'abord les consommations associées pour restituer le stock et éviter l'erreur de contrainte
        ConsommationService consommationService = new ConsommationService();
        List<entities.Consommation> consommations = consommationService.getByCulture(id);
        for (entities.Consommation cons : consommations) {
            consommationService.supprimer(cons.getId());
        }

        String query = "DELETE FROM culture WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public List<Culture> afficher() throws SQLException {
        List<Culture> list = new ArrayList<>();
        String query = "SELECT * FROM culture";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        }
        return list;
    }

    public List<Culture> getByParcelle(int parcelleId) throws SQLException {
        List<Culture> list = new ArrayList<>();
        String query = "SELECT * FROM culture WHERE parcelle_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, parcelleId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSet(rs));
                }
            }
        }
        return list;
    }

    private Culture mapResultSet(ResultSet rs) throws SQLException {
        return new Culture(
                rs.getInt("id"),
                rs.getString("type_culture"),
                rs.getString("variete"),
                rs.getDate("date_plantation").toLocalDate(),
                rs.getDate("date_recolte_prevue").toLocalDate(),
                rs.getString("statut"),
                rs.getInt("parcelle_id")
        );
    }
}
