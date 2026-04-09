package services;

import entities.Commande;
import interfaces.IService;
import utils.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CommandeService implements IService<Commande> {
    private final Connection conn = MyConnection.getInstance().getConn();

    @Override
    public void ajouter(Commande c) throws SQLException {
        String req = "INSERT INTO commande (statut, mode_paiement, adresse_livraison, " +
                "montant_total, payment_ref, paid_at, email_sent_at, created_at, updated_at, client_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), NOW(), ?)";
        try (PreparedStatement ps = conn.prepareStatement(req)) {
            ps.setString(1, c.getStatut());
            ps.setString(2, c.getModePaiement());
            ps.setString(3, c.getAdresseLivraison());
            ps.setDouble(4, c.getMontantTotal());
            ps.setString(5, c.getPaymentRef());
            ps.setTimestamp(6, toTimestamp(c.getPaidAt()));
            ps.setTimestamp(7, toTimestamp(c.getEmailSentAt()));
            ps.setInt(8, c.getClientId());
            ps.executeUpdate();
        }
    }

    @Override
    public List<Commande> afficher() throws SQLException {
        List<Commande> list = new ArrayList<>();
        String req = "SELECT * FROM commande ORDER BY created_at DESC";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(req)) {
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        }
        return list;
    }

    @Override
    public void modifier(Commande c) throws SQLException {
        String req = "UPDATE commande SET statut=?, mode_paiement=?, " +
                "adresse_livraison=?, montant_total=?, payment_ref=?, paid_at=?, email_sent_at=?, updated_at=NOW() WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(req)) {
            ps.setString(1, c.getStatut());
            ps.setString(2, c.getModePaiement());
            ps.setString(3, c.getAdresseLivraison());
            ps.setDouble(4, c.getMontantTotal());
            ps.setString(5, c.getPaymentRef());
            ps.setTimestamp(6, toTimestamp(c.getPaidAt()));
            ps.setTimestamp(7, toTimestamp(c.getEmailSentAt()));
            ps.setInt(8, c.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String req = "DELETE FROM commande WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(req)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public void updateStatut(int id, String statut) throws SQLException {
        String req = "UPDATE commande SET statut=?, updated_at=NOW() WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(req)) {
            ps.setString(1, statut);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    public List<Commande> getByClient(int clientId) throws SQLException {
        List<Commande> list = new ArrayList<>();
        String req = "SELECT * FROM commande WHERE client_id=? ORDER BY created_at DESC";
        try (PreparedStatement ps = conn.prepareStatement(req)) {
            ps.setInt(1, clientId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSet(rs));
                }
            }
        }
        return list;
    }

    public int countAll() throws SQLException {
        String req = "SELECT COUNT(*) FROM commande";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(req)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        }
    }

    private Commande mapResultSet(ResultSet rs) throws SQLException {
        Commande c = new Commande();
        c.setId(rs.getInt("id"));
        c.setStatut(rs.getString("statut"));
        c.setModePaiement(rs.getString("mode_paiement"));
        c.setAdresseLivraison(rs.getString("adresse_livraison"));
        c.setMontantTotal(rs.getDouble("montant_total"));
        c.setPaymentRef(rs.getString("payment_ref"));
        c.setClientId(rs.getInt("client_id"));
        Timestamp ca = rs.getTimestamp("created_at");
        if (ca != null) {
            c.setCreatedAt(ca.toLocalDateTime());
        }
        Timestamp ua = rs.getTimestamp("updated_at");
        if (ua != null) {
            c.setUpdatedAt(ua.toLocalDateTime());
        }
        Timestamp paidAt = rs.getTimestamp("paid_at");
        if (paidAt != null) {
            c.setPaidAt(paidAt.toLocalDateTime());
        }
        Timestamp emailAt = rs.getTimestamp("email_sent_at");
        if (emailAt != null) {
            c.setEmailSentAt(emailAt.toLocalDateTime());
        }
        return c;
    }

    private Timestamp toTimestamp(java.time.LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }
}
