package services;

import interfaces.IService;
import utils.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import entities.Offre;

public class OffreService implements IService<Offre> {
    private final Connection connection = MyConnection.getInstance().getConn();

    @Override
    public void ajouter(Offre offre) throws SQLException {
        // Updated SQL to match DB column names and added date_fin
        String sql = "INSERT INTO offre (title, type_poste, type_contrat, description, lieu, statut, date_debut, date_fin, salaire, is_active, statut_validation, agriculteur_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, offre.getTitle());
            ps.setString(2, offre.getType_poste());
            ps.setString(3, offre.getType_contrat());
            ps.setString(4, offre.getDescription());
            ps.setString(5, offre.getLieu());
            ps.setString(6, offre.getStatut());
            ps.setTimestamp(7, Timestamp.valueOf(offre.getDate_debut()));
            ps.setTimestamp(8, offre.getDate_fin() != null ? Timestamp.valueOf(offre.getDate_fin()) : null);
            ps.setDouble(9, offre.getSalaire());
            ps.setBoolean(10, offre.getIs_active());
            ps.setString(11, offre.getStatut_validation());
            ps.setInt(12, offre.getAgriculteur_id());

            ps.executeUpdate();
            System.out.println("Offre ajoutée avec succès !");
        }
    }

    @Override
    public List<Offre> afficher() throws SQLException {
        List<Offre> offres = new ArrayList<>();
        String sql = "SELECT * FROM offre";

        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                Offre o = new Offre();
                o.setId(rs.getLong("id"));
                o.setTitle(rs.getString("title"));
                o.setType_poste(rs.getString("type_poste"));
                o.setType_contrat(rs.getString("type_contrat"));
                o.setDescription(rs.getString("description"));
                o.setLieu(rs.getString("lieu"));
                o.setStatut(rs.getString("statut"));

                // Handling LocalDateTime conversion safely
                Timestamp debut = rs.getTimestamp("date_debut");
                if (debut != null) o.setDate_debut(debut.toLocalDateTime());

                Timestamp fin = rs.getTimestamp("date_fin");
                if (fin != null) o.setDate_fin(fin.toLocalDateTime());

                o.setSalaire(rs.getDouble("salaire"));
                o.setIs_active(rs.getBoolean("is_active"));
                o.setStatut_validation(rs.getString("statut_validation"));
                o.setAgriculteur_id(rs.getInt("agriculteur_id"));

                offres.add(o);
            }
        }
        return offres;
    }

    @Override
    public void modifier(Offre offre) throws SQLException {
        // Updated to match your entity field names and underscore column names
        String sql = "UPDATE offre SET title=?, type_poste=?, type_contrat=?, description=?, lieu=?, statut=?, date_debut=?, date_fin=?, salaire=?, is_active=? WHERE id=?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, offre.getTitle());
            ps.setString(2, offre.getType_poste());
            ps.setString(3, offre.getType_contrat());
            ps.setString(4, offre.getDescription());
            ps.setString(5, offre.getLieu());
            ps.setString(6, offre.getStatut());
            ps.setTimestamp(7, Timestamp.valueOf(offre.getDate_debut()));
            ps.setTimestamp(8, offre.getDate_fin() != null ? Timestamp.valueOf(offre.getDate_fin()) : null);
            ps.setDouble(9, offre.getSalaire());
            ps.setBoolean(10, offre.getIs_active());
            ps.setLong(11, offre.getId());

            ps.executeUpdate();
            System.out.println("Offre modifiée !");
        }
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM offre WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("Offre supprimée !");
        }
    }
    public void updateValidationStatus(long id, String newStatus) throws SQLException {
        String sql = "UPDATE offre SET statut_validation = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }
}
