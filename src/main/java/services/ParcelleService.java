package services;

import entities.Parcelle;
import exceptions.ValidationException;
import interfaces.IService;
import utils.MyConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ParcelleService implements IService<Parcelle> {
    private Connection connection;

    public ParcelleService() {
        connection = MyConnection.getInstance().getConn();
    }

    private void validate(Parcelle p) throws ValidationException {
        if (p.getNom() == null || p.getNom().trim().isEmpty()) {
            throw new ValidationException("Le nom de la parcelle est obligatoire.");
        }
        if (p.getNom().length() < 3) {
            throw new ValidationException("Le nom de la parcelle doit contenir au moins 3 caractères.");
        }
        if (p.getSurface() <= 0) {
            throw new ValidationException("La surface doit être strictement positive.");
        }
        if (p.getLatitude() < -90 || p.getLatitude() > 90) {
            throw new ValidationException("La latitude doit être comprise entre -90 et 90.");
        }
        if (p.getLongitude() < -180 || p.getLongitude() > 180) {
            throw new ValidationException("La longitude doit être comprise entre -180 et 180.");
        }
        if (p.getTypeSol() == null || p.getTypeSol().trim().isEmpty()) {
            throw new ValidationException("Le type de sol est obligatoire.");
        }
    }

    @Override
    public void ajouter(Parcelle p) throws SQLException {
        try {
            validate(p);
        } catch (ValidationException e) {
            // Rethrow as a runtime exception or handle specifically. 
            // In a better design, we'd change the interface signature, 
            // but here I'll wrap it or just let it propagate if I update IService.
            // Actually, I'll update IService to allow throwing Exception or use Runtime.
            throw new RuntimeException(e.getMessage());
        }

        String query = "INSERT INTO parcelle (nom, surface, latitude, longitude, type_sol, user_id) VALUES (?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.setString(1, p.getNom());
        ps.setDouble(2, p.getSurface());
        ps.setDouble(3, p.getLatitude());
        ps.setDouble(4, p.getLongitude());
        ps.setString(5, p.getTypeSol());
        ps.setInt(6, p.getUserId());
        ps.executeUpdate();
    }

    @Override
    public void modifier(Parcelle p) throws SQLException {
        try {
            validate(p);
        } catch (ValidationException e) {
            throw new RuntimeException(e.getMessage());
        }

        String query = "UPDATE parcelle SET nom=?, surface=?, latitude=?, longitude=?, type_sol=? WHERE id=?";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.setString(1, p.getNom());
        ps.setDouble(2, p.getSurface());
        ps.setDouble(3, p.getLatitude());
        ps.setDouble(4, p.getLongitude());
        ps.setString(5, p.getTypeSol());
        ps.setInt(6, p.getId());
        ps.executeUpdate();
    }

    @Override
    public void supprimer(int id) throws SQLException {
        // Supprimer toutes les cultures liées avant de supprimer la parcelle
        CultureService cultureService = new CultureService();
        List<entities.Culture> cultures = cultureService.getByParcelle(id);
        for (entities.Culture c : cultures) {
            cultureService.supprimer(c.getId());
        }

        String query = "DELETE FROM parcelle WHERE id=?";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    @Override
    public List<Parcelle> afficher() throws SQLException {
        List<Parcelle> list = new ArrayList<>();
        String query = "SELECT * FROM parcelle";
        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(query);
        while (rs.next()) {
            list.add(new Parcelle(
                    rs.getInt("id"),
                    rs.getString("nom"),
                    rs.getDouble("surface"),
                    rs.getDouble("latitude"),
                    rs.getDouble("longitude"),
                    rs.getString("type_sol"),
                    rs.getInt("user_id")));
        }
        return list;
    }

    public List<Parcelle> afficherByUser(int userId) throws SQLException {
        List<Parcelle> list = new ArrayList<>();
        String query = "SELECT * FROM parcelle WHERE user_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Parcelle(
                            rs.getInt("id"),
                            rs.getString("nom"),
                            rs.getDouble("surface"),
                            rs.getDouble("latitude"),
                            rs.getDouble("longitude"),
                            rs.getString("type_sol"),
                            rs.getInt("user_id")));
                }
            }
        }
        return list;
    }
}