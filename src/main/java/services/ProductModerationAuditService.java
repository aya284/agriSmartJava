package services;

import entities.ProductModerationAudit;
import entities.Produit;
import utils.MyConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class ProductModerationAuditService {
    private final Connection conn = MyConnection.getInstance().getConn();

    public ProductModerationAuditService() {
        try {
            ensureAuditTable();
        } catch (SQLException ignored) {
            // Table creation best-effort; read/write operations still raise explicit errors.
        }
    }

    public void logAction(Produit produit, boolean banned, String reason, int adminId, String adminName) throws SQLException {
        ensureAuditTable();
        String req = "INSERT INTO produit_moderation_audit (produit_id, produit_nom, action_type, reason, admin_id, admin_name, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, NOW())";
        try (PreparedStatement ps = conn.prepareStatement(req)) {
            ps.setInt(1, produit == null ? 0 : produit.getId());
            ps.setString(2, produit == null ? "" : safe(produit.getNom()));
            ps.setString(3, banned ? "BAN" : "UNBAN");
            ps.setString(4, safe(reason));
            ps.setInt(5, adminId);
            ps.setString(6, safe(adminName));
            ps.executeUpdate();
        }
    }

    public List<ProductModerationAudit> getRecent(int limit) throws SQLException {
        ensureAuditTable();
        int safeLimit = Math.max(1, Math.min(limit, 200));
        String req = "SELECT id, produit_id, produit_nom, action_type, reason, admin_id, admin_name, created_at " +
                "FROM produit_moderation_audit ORDER BY created_at DESC LIMIT " + safeLimit;

        List<ProductModerationAudit> rows = new ArrayList<>();
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(req)) {
            while (rs.next()) {
                ProductModerationAudit row = new ProductModerationAudit();
                row.setId(rs.getInt("id"));
                row.setProduitId(rs.getInt("produit_id"));
                row.setProduitNom(rs.getString("produit_nom"));
                row.setActionType(rs.getString("action_type"));
                row.setReason(rs.getString("reason"));
                row.setAdminId(rs.getInt("admin_id"));
                row.setAdminName(rs.getString("admin_name"));
                Timestamp created = rs.getTimestamp("created_at");
                if (created != null) {
                    row.setCreatedAt(created.toLocalDateTime());
                }
                rows.add(row);
            }
        }
        return rows;
    }

    public List<ProductModerationAudit> getRecentSinceDays(int days) throws SQLException {
        ensureAuditTable();
        int safeDays = Math.max(1, Math.min(days, 3650));
        String req = "SELECT id, produit_id, produit_nom, action_type, reason, admin_id, admin_name, created_at " +
                "FROM produit_moderation_audit WHERE created_at >= DATE_SUB(NOW(), INTERVAL ? DAY) " +
                "ORDER BY created_at ASC";

        List<ProductModerationAudit> rows = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(req)) {
            ps.setInt(1, safeDays);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ProductModerationAudit row = new ProductModerationAudit();
                    row.setId(rs.getInt("id"));
                    row.setProduitId(rs.getInt("produit_id"));
                    row.setProduitNom(rs.getString("produit_nom"));
                    row.setActionType(rs.getString("action_type"));
                    row.setReason(rs.getString("reason"));
                    row.setAdminId(rs.getInt("admin_id"));
                    row.setAdminName(rs.getString("admin_name"));
                    Timestamp created = rs.getTimestamp("created_at");
                    if (created != null) {
                        row.setCreatedAt(created.toLocalDateTime());
                    }
                    rows.add(row);
                }
            }
        }
        return rows;
    }

    private void ensureAuditTable() throws SQLException {
        String ddl = "CREATE TABLE IF NOT EXISTS produit_moderation_audit (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "produit_id INT NOT NULL," +
                "produit_nom VARCHAR(255) NOT NULL," +
                "action_type VARCHAR(16) NOT NULL," +
                "reason VARCHAR(500) NULL," +
                "admin_id INT NOT NULL," +
                "admin_name VARCHAR(255) NULL," +
                "created_at DATETIME NOT NULL," +
                "INDEX idx_pma_created_at (created_at)," +
                "INDEX idx_pma_produit_id (produit_id)" +
                ")";

        try (Statement st = conn.createStatement()) {
            st.execute(ddl);
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
