package services;

import entities.OrderActionAudit;
import utils.MyConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class OrderActionAuditService {
    private final Connection conn = MyConnection.getInstance().getConn();

    public OrderActionAuditService() {
        try {
            ensureAuditTable();
        } catch (SQLException ignored) {
            // Best effort schema init; failures are raised during real operations.
        }
    }

    public void logAction(int commandeId, String fromStatus, String toStatus,
                          int actorUserId, String actorRole, String reason) throws SQLException {
        ensureAuditTable();
        String req = "INSERT INTO commande_action_audit (commande_id, from_status, to_status, actor_user_id, actor_role, reason, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, NOW())";
        try (PreparedStatement ps = conn.prepareStatement(req)) {
            ps.setInt(1, commandeId);
            ps.setString(2, safe(fromStatus));
            ps.setString(3, safe(toStatus));
            ps.setInt(4, Math.max(0, actorUserId));
            ps.setString(5, safe(actorRole));
            ps.setString(6, safe(reason));
            ps.executeUpdate();
        }
    }

    public List<OrderActionAudit> getRecent(int limit) throws SQLException {
        ensureAuditTable();
        int safeLimit = Math.max(1, Math.min(limit, 300));
        String req = "SELECT id, commande_id, from_status, to_status, actor_user_id, actor_role, reason, created_at " +
                "FROM commande_action_audit ORDER BY created_at DESC LIMIT " + safeLimit;

        List<OrderActionAudit> rows = new ArrayList<>();
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(req)) {
            while (rs.next()) {
                rows.add(mapRow(rs));
            }
        }
        return rows;
    }

    private OrderActionAudit mapRow(ResultSet rs) throws SQLException {
        OrderActionAudit row = new OrderActionAudit();
        row.setId(rs.getInt("id"));
        row.setCommandeId(rs.getInt("commande_id"));
        row.setFromStatus(rs.getString("from_status"));
        row.setToStatus(rs.getString("to_status"));
        row.setActorUserId(rs.getInt("actor_user_id"));
        row.setActorRole(rs.getString("actor_role"));
        row.setReason(rs.getString("reason"));
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) {
            row.setCreatedAt(created.toLocalDateTime());
        }
        return row;
    }

    private void ensureAuditTable() throws SQLException {
        String ddl = "CREATE TABLE IF NOT EXISTS commande_action_audit (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "commande_id INT NOT NULL," +
                "from_status VARCHAR(32) NOT NULL," +
                "to_status VARCHAR(32) NOT NULL," +
                "actor_user_id INT NOT NULL," +
                "actor_role VARCHAR(32) NOT NULL," +
                "reason VARCHAR(500) NULL," +
                "created_at DATETIME NOT NULL," +
                "INDEX idx_caa_created_at (created_at)," +
                "INDEX idx_caa_commande_id (commande_id)" +
                ")";

        try (Statement st = conn.createStatement()) {
            st.execute(ddl);
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
