package services;

import entities.CartItem;
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

    public List<Commande> getBySeller(int sellerId) throws SQLException {
        List<Commande> list = new ArrayList<>();
        String req = "SELECT DISTINCT c.* FROM commande c "
                + "JOIN commande_item ci ON ci.commande_id = c.id "
                + "JOIN produit p ON p.id = ci.produit_id "
                + "WHERE p.vendeur_id = ? "
                + "ORDER BY c.created_at DESC";
        try (PreparedStatement ps = conn.prepareStatement(req)) {
            ps.setInt(1, sellerId);
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

    public int createCommandeFromCart(int clientId, String modePaiement, String adresseLivraison,
                                      List<CartItem> cartItems) throws SQLException {
        if (cartItems == null || cartItems.isEmpty()) {
            throw new SQLException("Le panier est vide.");
        }

        String insertCommande = "INSERT INTO commande (statut, mode_paiement, adresse_livraison, montant_total, " +
                "created_at, updated_at, client_id) VALUES (?, ?, ?, ?, NOW(), NOW(), ?)";
        String insertItem = "INSERT INTO commande_item (commande_id, produit_id, quantite, prix_unitaire, created_at) " +
                "VALUES (?, ?, ?, ?, NOW())";
        String updateStock = "UPDATE produit SET quantite_stock = quantite_stock - ?, updated_at=NOW() " +
                "WHERE id = ? AND quantite_stock >= ?";

        double total = cartItems.stream().mapToDouble(CartItem::getLineTotal).sum();
        boolean previousAutoCommit = conn.getAutoCommit();

        try {
            conn.setAutoCommit(false);
            int commandeId;

            try (PreparedStatement psCommande = conn.prepareStatement(insertCommande, Statement.RETURN_GENERATED_KEYS)) {
                psCommande.setString(1, "en_attente");
                psCommande.setString(2, modePaiement);
                psCommande.setString(3, adresseLivraison);
                psCommande.setDouble(4, total);
                psCommande.setInt(5, clientId);
                psCommande.executeUpdate();

                try (ResultSet keys = psCommande.getGeneratedKeys()) {
                    if (!keys.next()) {
                        throw new SQLException("Creation de commande impossible: ID non genere.");
                    }
                    commandeId = keys.getInt(1);
                }
            }

            try (PreparedStatement psItem = conn.prepareStatement(insertItem);
                 PreparedStatement psStock = conn.prepareStatement(updateStock)) {
                for (CartItem item : cartItems) {
                    int produitId = item.getProduit().getId();
                    int quantite = item.getQuantite();

                    psStock.setInt(1, quantite);
                    psStock.setInt(2, produitId);
                    psStock.setInt(3, quantite);
                    int updated = psStock.executeUpdate();
                    if (updated == 0) {
                        throw new SQLException("Stock insuffisant pour le produit #" + produitId + ".");
                    }

                    psItem.setInt(1, commandeId);
                    psItem.setInt(2, produitId);
                    psItem.setInt(3, quantite);
                    psItem.setDouble(4, item.getUnitPrice());
                    psItem.addBatch();
                }
                psItem.executeBatch();
            }

            conn.commit();
            return commandeId;
        } catch (SQLException ex) {
            conn.rollback();
            throw ex;
        } finally {
            conn.setAutoCommit(previousAutoCommit);
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
