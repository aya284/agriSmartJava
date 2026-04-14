package services;

import entities.CartItem;
import entities.Commande;
import interfaces.IService;
import utils.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CommandeService implements IService<Commande> {
    public enum OrderActor {
        BUYER,
        SELLER,
        ADMIN,
        SYSTEM
    }

    private final Connection conn = MyConnection.getInstance().getConn();
    private final OrderActionAuditService orderActionAuditService = new OrderActionAuditService();

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
        if (c == null || c.getId() <= 0) {
            throw new SQLException("Commande invalide.");
        }

        String nextStatus = normalizeStatus(c.getStatut());
        boolean previousAutoCommit = conn.getAutoCommit();

        try {
            conn.setAutoCommit(false);
            OrderSnapshot current = getOrderSnapshotForUpdate(c.getId());
            ensureTransitionAllowed(current.status, nextStatus);

            String req = "UPDATE commande SET statut=?, mode_paiement=?, " +
                    "adresse_livraison=?, montant_total=?, payment_ref=?, paid_at=?, email_sent_at=?, updated_at=NOW() WHERE id=?";
            try (PreparedStatement ps = conn.prepareStatement(req)) {
                ps.setString(1, nextStatus);
                ps.setString(2, c.getModePaiement());
                ps.setString(3, c.getAdresseLivraison());
                ps.setDouble(4, c.getMontantTotal());
                ps.setString(5, c.getPaymentRef());
                ps.setTimestamp(6, toTimestamp(c.getPaidAt()));
                ps.setTimestamp(7, toTimestamp(c.getEmailSentAt()));
                ps.setInt(8, c.getId());
                ps.executeUpdate();
            }

            if (!current.status.equals(nextStatus)) {
                orderActionAuditService.logAction(c.getId(), current.status, nextStatus, 0, "SYSTEM", "Edit commande");
            }

            conn.commit();
        } catch (SQLException ex) {
            conn.rollback();
            throw ex;
        } finally {
            conn.setAutoCommit(previousAutoCommit);
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
        updateStatut(id, statut, 0, OrderActor.SYSTEM, "Mise a jour systeme");
    }

    public void updateStatut(int id, String statut, int actorUserId, OrderActor actor, String reason) throws SQLException {
        if (id <= 0) {
            throw new SQLException("Commande invalide.");
        }
        String nextStatus = normalizeStatus(statut);
        OrderActor effectiveActor = actor == null ? OrderActor.SYSTEM : actor;
        boolean previousAutoCommit = conn.getAutoCommit();

        try {
            conn.setAutoCommit(false);
            OrderSnapshot snapshot = getOrderSnapshotForUpdate(id);
            ensureTransitionAllowed(snapshot.status, nextStatus);

            if (!isActorAllowed(snapshot, nextStatus, actorUserId, effectiveActor)) {
                throw new SQLException("Action non autorisee pour cet utilisateur.");
            }

            if (!snapshot.status.equals(nextStatus)) {
                String updateReq = "UPDATE commande SET statut=?, updated_at=NOW() WHERE id=?";
                try (PreparedStatement ps = conn.prepareStatement(updateReq)) {
                    ps.setString(1, nextStatus);
                    ps.setInt(2, id);
                    ps.executeUpdate();
                }
                orderActionAuditService.logAction(id, snapshot.status, nextStatus, actorUserId, effectiveActor.name(), safe(reason));
            }
            conn.commit();
        } catch (SQLException ex) {
            conn.rollback();
            throw ex;
        } finally {
            conn.setAutoCommit(previousAutoCommit);
        }
    }

    public Set<String> getAllowedTransitions(String currentStatus, OrderActor actor) {
        String normalizedCurrent = normalizeStatus(currentStatus);
        OrderActor effectiveActor = actor == null ? OrderActor.SYSTEM : actor;
        Set<String> allowed = new LinkedHashSet<>();
        for (String candidate : getAllowedNextStatuses(normalizedCurrent)) {
            if (isActorAllowed(normalizedCurrent, candidate, effectiveActor)) {
                allowed.add(candidate);
            }
        }
        return allowed;
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

    public List<Commande> getRecentSinceDays(int days) throws SQLException {
        int safeDays = Math.max(1, Math.min(days, 3650));
        List<Commande> list = new ArrayList<>();
        String req = "SELECT * FROM commande WHERE created_at >= DATE_SUB(NOW(), INTERVAL ? DAY) ORDER BY created_at ASC";
        try (PreparedStatement ps = conn.prepareStatement(req)) {
            ps.setInt(1, safeDays);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSet(rs));
                }
            }
        }
        return list;
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
        String lockProduit = "SELECT quantite_stock, prix, is_promotion, promotion_price, banned, vendeur_id " +
                "FROM produit WHERE id=? FOR UPDATE";
        String updateStock = "UPDATE produit SET quantite_stock = quantite_stock - ?, updated_at=NOW() WHERE id = ?";

        Map<Integer, Integer> quantitiesByProduitId = new HashMap<>();
        for (CartItem item : cartItems) {
            if (item == null || item.getProduit() == null || item.getProduit().getId() <= 0) {
                throw new SQLException("Panier invalide: produit manquant.");
            }
            int produitId = item.getProduit().getId();
            int qty = Math.max(1, item.getQuantite());
            quantitiesByProduitId.put(produitId, quantitiesByProduitId.getOrDefault(produitId, 0) + qty);
        }

        Map<Integer, Double> unitPriceByProduitId = new HashMap<>();
        double total = 0.0;
        boolean previousAutoCommit = conn.getAutoCommit();

        try {
            conn.setAutoCommit(false);
            int commandeId;

            try (PreparedStatement psLock = conn.prepareStatement(lockProduit)) {
                for (Map.Entry<Integer, Integer> entry : quantitiesByProduitId.entrySet()) {
                    int produitId = entry.getKey();
                    int quantiteDemandee = entry.getValue();

                    psLock.setInt(1, produitId);
                    try (ResultSet rs = psLock.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException("Le produit #" + produitId + " n'existe plus.");
                        }

                        if (rs.getBoolean("banned")) {
                            throw new SQLException("Le produit #" + produitId + " est indisponible.");
                        }

                        int vendeurId = rs.getInt("vendeur_id");
                        if (vendeurId > 0 && vendeurId == clientId) {
                            throw new SQLException("Vous ne pouvez pas commander votre propre produit (#" + produitId + ").");
                        }

                        int stock = rs.getInt("quantite_stock");
                        if (quantiteDemandee > stock) {
                            throw new SQLException("Stock insuffisant pour le produit #" + produitId +
                                    " (stock: " + stock + ", demande: " + quantiteDemandee + ").");
                        }

                        double unitPrice = rs.getBoolean("is_promotion") && rs.getDouble("promotion_price") > 0
                                ? rs.getDouble("promotion_price")
                                : rs.getDouble("prix");

                        unitPriceByProduitId.put(produitId, unitPrice);
                        total += unitPrice * quantiteDemandee;
                    }
                }
            }

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
                for (Map.Entry<Integer, Integer> entry : quantitiesByProduitId.entrySet()) {
                    int produitId = entry.getKey();
                    int quantite = entry.getValue();
                    psStock.setInt(1, quantite);
                    psStock.setInt(2, produitId);
                    int updated = psStock.executeUpdate();
                    if (updated == 0) {
                        throw new SQLException("Stock insuffisant pour le produit #" + produitId + ".");
                    }

                    psItem.setInt(1, commandeId);
                    psItem.setInt(2, produitId);
                    psItem.setInt(3, quantite);
                    psItem.setDouble(4, unitPriceByProduitId.getOrDefault(produitId, 0.0));
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

    private OrderSnapshot getOrderSnapshotForUpdate(int orderId) throws SQLException {
        String req = "SELECT id, statut, client_id FROM commande WHERE id=? FOR UPDATE";
        try (PreparedStatement ps = conn.prepareStatement(req)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("Commande #" + orderId + " introuvable.");
                }
                return new OrderSnapshot(
                        rs.getInt("id"),
                        normalizeStatus(rs.getString("statut")),
                        rs.getInt("client_id")
                );
            }
        }
    }

    private void ensureTransitionAllowed(String currentStatus, String nextStatus) throws SQLException {
        if (currentStatus.equals(nextStatus)) {
            return;
        }
        Set<String> allowed = getAllowedNextStatuses(currentStatus);
        if (!allowed.contains(nextStatus)) {
            throw new SQLException("Transition invalide: " + currentStatus + " -> " + nextStatus + ".");
        }
    }

    private Set<String> getAllowedNextStatuses(String status) {
        String current = normalizeStatus(status);
        Set<String> next = new LinkedHashSet<>();
        switch (current) {
            case "en_attente" -> {
                next.add("confirmee");
                next.add("annulee");
            }
            case "confirmee" -> {
                next.add("livree");
                next.add("annulee");
            }
            case "livree", "annulee" -> {
                // Etat terminal: aucune transition autorisee.
            }
            default -> throw new IllegalArgumentException("Statut non supporte: " + current);
        }
        return next;
    }

    private boolean isActorAllowed(OrderSnapshot snapshot, String nextStatus, int actorUserId, OrderActor actor) throws SQLException {
        if (actor == OrderActor.ADMIN || actor == OrderActor.SYSTEM) {
            return true;
        }
        return switch (actor) {
            case BUYER -> actorUserId > 0
                    && actorUserId == snapshot.clientId
                    && isActorAllowed(snapshot.status, nextStatus, actor);
            case SELLER -> actorUserId > 0
                    && isSellerLinkedToOrder(snapshot.id, actorUserId)
                    && isActorAllowed(snapshot.status, nextStatus, actor);
            default -> false;
        };
    }

    private boolean isActorAllowed(String currentStatus, String nextStatus, OrderActor actor) {
        if (currentStatus.equals(nextStatus)) {
            return true;
        }
        return switch (actor) {
            case BUYER -> "annulee".equals(nextStatus) && ("en_attente".equals(currentStatus) || "confirmee".equals(currentStatus));
            case SELLER -> ("en_attente".equals(currentStatus) && "confirmee".equals(nextStatus))
                    || ("confirmee".equals(currentStatus) && ("livree".equals(nextStatus) || "annulee".equals(nextStatus)));
            case ADMIN, SYSTEM -> true;
        };
    }

    private boolean isSellerLinkedToOrder(int orderId, int sellerId) throws SQLException {
        String req = "SELECT 1 FROM commande_item ci " +
                "JOIN produit p ON p.id = ci.produit_id " +
                "WHERE ci.commande_id = ? AND p.vendeur_id = ? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(req)) {
            ps.setInt(1, orderId);
            ps.setInt(2, sellerId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private String normalizeStatus(String value) throws SQLException {
        String status = safe(value).toLowerCase();
        if (!Set.of("en_attente", "confirmee", "livree", "annulee").contains(status)) {
            throw new SQLException("Statut de commande non supporte: " + value);
        }
        return status;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static final class OrderSnapshot {
        private final int id;
        private final String status;
        private final int clientId;

        private OrderSnapshot(int id, String status, int clientId) {
            this.id = id;
            this.status = status;
            this.clientId = clientId;
        }
    }
}
