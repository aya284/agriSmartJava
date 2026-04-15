package services;

import entities.Consommation;
import interfaces.IService;
import utils.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ConsommationService implements IService<Consommation> {
    private Connection conn = MyConnection.getInstance().getConn();

    @Override
    public void ajouter(Consommation c) throws SQLException {
        // Début de la transaction
        conn.setAutoCommit(false);
        try {
            // 1. Vérifier le stock disponible
            String checkQuery = "SELECT stock_restan FROM ressource WHERE id = ?";
            double stockDisponible = 0;
            try (PreparedStatement psCheck = conn.prepareStatement(checkQuery)) {
                psCheck.setInt(1, c.getRessourceId());
                try (ResultSet rs = psCheck.executeQuery()) {
                    if (rs.next()) {
                        stockDisponible = rs.getDouble("stock_restan");
                    } else {
                        throw new SQLException("Ressource non trouvée.");
                    }
                }
            }

            if (stockDisponible < c.getQuantite()) {
                throw new SQLException("Stock insuffisant ! Disponible : " + stockDisponible);
            }

            // 2. Déduire le stock
            String updateQuery = "UPDATE ressource SET stock_restan = stock_restan - ? WHERE id = ?";
            try (PreparedStatement psUpdate = conn.prepareStatement(updateQuery)) {
                psUpdate.setDouble(1, c.getQuantite());
                psUpdate.setInt(2, c.getRessourceId());
                int rows = psUpdate.executeUpdate();
                System.out.println("[STOCK] Deduction successful: " + rows + " row(s) updated for ressource ID " + c.getRessourceId());
                if (rows == 0) throw new SQLException("Erreur: Le stock n'a pas pu être mis à jour.");
            }

            // 3. Insérer la consommation
            String insertQuery = "INSERT INTO consommation (quantite, date_consommation, ressource_id, culture_id) VALUES (?, ?, ?, ?)";
            try (PreparedStatement psInsert = conn.prepareStatement(insertQuery)) {
                psInsert.setDouble(1, c.getQuantite());
                psInsert.setDate(2, Date.valueOf(c.getDateConsommation()));
                psInsert.setInt(3, c.getRessourceId());
                psInsert.setInt(4, c.getCultureId());
                psInsert.executeUpdate();
            }

            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    @Override
    public List<Consommation> afficher() throws SQLException {
        List<Consommation> list = new ArrayList<>();
        String query = "SELECT * FROM consommation";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                list.add(new Consommation(
                        rs.getInt("id"),
                        rs.getDouble("quantite"),
                        rs.getDate("date_consommation").toLocalDate(),
                        rs.getInt("ressource_id"),
                        rs.getInt("culture_id")
                ));
            }
        }
        return list;
    }

    public Consommation getById(int id) throws SQLException {
        String query = "SELECT * FROM consommation WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Consommation cons = new Consommation(
                            rs.getInt("id"),
                            rs.getDouble("quantite"),
                            rs.getDate("date_consommation").toLocalDate(),
                            rs.getInt("ressource_id"),
                            rs.getInt("culture_id")
                    );
                    return cons;
                }
            }
        }
        return null;
    }

    @Override
    public void modifier(Consommation c) throws SQLException {
        Consommation old = getById(c.getId());
        if (old == null) throw new SQLException("Enregistrement introuvable.");

        conn.setAutoCommit(false);
        try {
            if (old.getRessourceId() != c.getRessourceId()) {
                // Changer de ressource : rendre à l'ancienne, prendre à la nouvelle
                String returnOld = "UPDATE ressource SET stock_restan = stock_restan + ? WHERE id = ?";
                try (PreparedStatement ps = conn.prepareStatement(returnOld)) {
                    ps.setDouble(1, old.getQuantite());
                    ps.setInt(2, old.getRessourceId());
                    ps.executeUpdate();
                }
                String deductNew = "UPDATE ressource SET stock_restan = stock_restan - ? WHERE id = ?";
                try (PreparedStatement ps = conn.prepareStatement(deductNew)) {
                    ps.setDouble(1, c.getQuantite());
                    ps.setInt(2, c.getRessourceId());
                    ps.executeUpdate();
                }
            } else {
                // Même ressource : ajuster par la différence (delta)
                double delta = c.getQuantite() - old.getQuantite();
                String adjustStock = "UPDATE ressource SET stock_restan = stock_restan - ? WHERE id = ?";
                try (PreparedStatement ps = conn.prepareStatement(adjustStock)) {
                    ps.setDouble(1, delta);
                    ps.setInt(2, c.getRessourceId());
                    int rows = ps.executeUpdate();
                    System.out.println("[STOCK] Adjustment successful: " + rows + " row(s) updated (delta: " + delta + ")");
                }
            }

            String query = "UPDATE consommation SET quantite=?, date_consommation=?, ressource_id=?, culture_id=? WHERE id=?";
            try (PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setDouble(1, c.getQuantite());
                ps.setDate(2, Date.valueOf(c.getDateConsommation()));
                ps.setInt(3, c.getRessourceId());
                ps.setInt(4, c.getCultureId());
                ps.setInt(5, c.getId());
                ps.executeUpdate();
            }
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    @Override
    public void supprimer(int id) throws SQLException {
        Consommation old = getById(id);
        if (old == null) return;

        conn.setAutoCommit(false);
        try {
            // Restituer le stock
            String returnStock = "UPDATE ressource SET stock_restan = stock_restan + ? WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(returnStock)) {
                ps.setDouble(1, old.getQuantite());
                ps.setInt(2, old.getRessourceId());
                ps.executeUpdate();
            }

            String delete = "DELETE FROM consommation WHERE id=?";
            try (PreparedStatement ps = conn.prepareStatement(delete)) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    public List<Consommation> getByCulture(int cultureId) throws SQLException {
        List<Consommation> list = new ArrayList<>();
        String query = "SELECT c.*, r.nom AS ressource_nom, r.unite " +
                       "FROM consommation c " +
                       "JOIN ressource r ON c.ressource_id = r.id " +
                       "WHERE c.culture_id = ? " +
                       "ORDER BY c.date_consommation DESC";
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, cultureId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Consommation cons = new Consommation(
                            rs.getInt("id"),
                            rs.getDouble("quantite"),
                            rs.getDate("date_consommation").toLocalDate(),
                            rs.getInt("ressource_id"),
                            rs.getInt("culture_id")
                    );
                    cons.setRessourceNom(rs.getString("ressource_nom"));
                    cons.setUnite(rs.getString("unite"));
                    list.add(cons);
                }
            }
        }
        return list;
    }
}
