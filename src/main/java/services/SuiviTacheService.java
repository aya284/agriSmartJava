package services;

import entities.SuiviTache;
import utils.MyConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class SuiviTacheService {
    private final Connection conn = MyConnection.getInstance().getConn();

    public void add(SuiviTache suivi) throws SQLException {
        String insertSql = "INSERT INTO suivi_tache (id_tache, date, rendement, problemes, solution) VALUES (?, ?, ?, ?, ?)";
        String updateSql = "UPDATE task SET statut = 'a_valider' WHERE id_task = ?";

        // Insert into suivi_tache
        try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
            insertStmt.setInt(1, suivi.getTaskId());
            insertStmt.setTimestamp(2, Timestamp.valueOf(suivi.getDate()));
            insertStmt.setString(3, suivi.getRendement());
            insertStmt.setString(4, suivi.getProblemes());
            insertStmt.setString(5, suivi.getSolution());
            insertStmt.executeUpdate();
        }

        // Update task status
        try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
            updateStmt.setInt(1, suivi.getTaskId());
            updateStmt.executeUpdate();
        }
    }

    public List<SuiviTache> getAll() throws SQLException {
        List<SuiviTache> suivis = new ArrayList<>();
        String sql = "SELECT st.id_suivi, st.id_tache, st.date, st.rendement, st.problemes, st.solution, t.titre " +
                     "FROM suivi_tache st " +
                     "JOIN task t ON st.id_tache = t.id_task";
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                SuiviTache suivi = new SuiviTache(
                        rs.getInt("id_suivi"),
                        rs.getInt("id_tache"),
                        rs.getTimestamp("date").toLocalDateTime(),
                        rs.getString("rendement"),
                        rs.getString("problemes"),
                        rs.getString("solution"),
                        "complete",
                        rs.getString("titre")
                );
                suivis.add(suivi);
            }
        }
        return suivis;
    }

    public List<SuiviTache> getByTask(int taskId) throws SQLException {
        List<SuiviTache> suivis = new ArrayList<>();
        String sql = "SELECT st.id_suivi, st.id_tache, st.date, st.rendement, st.problemes, st.solution, t.titre " +
                     "FROM suivi_tache st " +
                     "JOIN task t ON st.id_tache = t.id_task " +
                     "WHERE st.id_tache = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, taskId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    SuiviTache suivi = new SuiviTache(
                            rs.getInt("id_suivi"),
                            rs.getInt("id_tache"),
                            rs.getTimestamp("date").toLocalDateTime(),
                            rs.getString("rendement"),
                            rs.getString("problemes"),
                            rs.getString("solution"),
                            "complete",
                            rs.getString("titre")
                    );
                    suivis.add(suivi);
                }
            }
        }
        return suivis;
    }

    public void delete(int idSuivi) throws SQLException {
        String sql = "DELETE FROM suivi_tache WHERE id_suivi = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idSuivi);
            stmt.executeUpdate();
        }
    }
}





