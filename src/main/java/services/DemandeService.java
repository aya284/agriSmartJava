package services;

import entities.Demande;
import interfaces.IService;
import utils.MyConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DemandeService implements IService<Demande> {
    private final Connection connection = MyConnection.getInstance().getConn();

    @Override
    public void ajouter(Demande d) throws SQLException {
        String sql = "INSERT INTO demande (nom, prenom, phone_number, date_postulation, date_modification, cv, lettre_motivation, statut, score, users_id, offre_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, d.getNom());
            ps.setString(2, d.getPrenom());
            ps.setString(3, d.getPhone_number());
            ps.setTimestamp(4, Timestamp.valueOf(d.getDate_postulation()));
            ps.setTimestamp(5, Timestamp.valueOf(d.getDate_modification()));
            ps.setString(6, d.getCv());
            ps.setString(7, d.getLettre_motivation());
            ps.setString(8, d.getStatut());
            ps.setInt(9, d.getScore());
            ps.setInt(10, d.getUsers_id());
            ps.setInt(11, d.getOffre_id());
            ps.executeUpdate();
        }
    }

    @Override
    public List<Demande> afficher() throws SQLException {
        List<Demande> list = new ArrayList<>();
        String sql = "SELECT * FROM demande";
        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Demande d = new Demande(
                        rs.getLong("id"), rs.getString("nom"), rs.getString("prenom"),
                        rs.getString("phone_number"), rs.getTimestamp("date_postulation").toLocalDateTime(),
                        rs.getTimestamp("date_modification").toLocalDateTime(), rs.getString("cv"),
                        rs.getString("lettre_motivation"), rs.getString("statut"),
                        rs.getInt("score"), rs.getInt("users_id"), rs.getInt("offre_id")
                );
                list.add(d);
            }
        }
        return list;
    }

    @Override
    public void modifier(Demande d) throws SQLException {
        String sql = "UPDATE demande SET nom=?, prenom=?, phone_number=?, date_modification=?, cv=?, lettre_motivation=?, statut=? WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, d.getNom());
            ps.setString(2, d.getPrenom());
            ps.setString(3, d.getPhone_number());
            ps.setTimestamp(4, Timestamp.valueOf(d.getDate_modification()));
            ps.setString(5, d.getCv());
            ps.setString(6, d.getLettre_motivation());
            ps.setString(7, d.getStatut());
            ps.setLong(8, d.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM demande WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}