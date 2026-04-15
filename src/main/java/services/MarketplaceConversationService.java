package services;

import entities.MarketplaceConversation;
import interfaces.IService;
import utils.MyConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class MarketplaceConversationService implements IService<MarketplaceConversation> {
    private final Connection conn = MyConnection.getInstance().getConn();

    @Override
    public void ajouter(MarketplaceConversation conversation) throws SQLException {
        String req = "INSERT INTO marketplace_conversation (produit_id, buyer_id, seller_id, created_at, last_message_at) " +
                "VALUES (?, ?, ?, NOW(), NOW())";
        try (PreparedStatement ps = conn.prepareStatement(req)) {
            ps.setInt(1, conversation.getProduitId());
            ps.setInt(2, conversation.getBuyerId());
            ps.setInt(3, conversation.getSellerId());
            ps.executeUpdate();
        }
    }

    @Override
    public List<MarketplaceConversation> afficher() throws SQLException {
        List<MarketplaceConversation> list = new ArrayList<>();
        String req = "SELECT * FROM marketplace_conversation ORDER BY last_message_at DESC, created_at DESC";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(req)) {
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        }
        return list;
    }

    @Override
    public void modifier(MarketplaceConversation conversation) throws SQLException {
        String req = "UPDATE marketplace_conversation SET produit_id=?, buyer_id=?, seller_id=?, last_message_at=? WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(req)) {
            ps.setInt(1, conversation.getProduitId());
            ps.setInt(2, conversation.getBuyerId());
            ps.setInt(3, conversation.getSellerId());
            ps.setTimestamp(4, toTimestamp(conversation.getLastMessageAt()));
            ps.setInt(5, conversation.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String req = "DELETE FROM marketplace_conversation WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(req)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public List<MarketplaceConversation> getByUser(int userId) throws SQLException {
        List<MarketplaceConversation> list = new ArrayList<>();
        String req = "SELECT * FROM marketplace_conversation WHERE buyer_id=? OR seller_id=? " +
                "ORDER BY last_message_at DESC, created_at DESC";
        try (PreparedStatement ps = conn.prepareStatement(req)) {
            ps.setInt(1, userId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSet(rs));
                }
            }
        }
        return list;
    }

    public MarketplaceConversation findOrCreateConversation(int produitId, int buyerId, int sellerId) throws SQLException {
        MarketplaceConversation existing = findConversation(produitId, buyerId, sellerId);
        if (existing != null) {
            return existing;
        }

        String insertReq = "INSERT INTO marketplace_conversation (produit_id, buyer_id, seller_id, created_at, last_message_at) " +
                "VALUES (?, ?, ?, NOW(), NOW())";
        try (PreparedStatement ps = conn.prepareStatement(insertReq, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, produitId);
            ps.setInt(2, buyerId);
            ps.setInt(3, sellerId);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    MarketplaceConversation conversation = new MarketplaceConversation();
                    conversation.setId(keys.getInt(1));
                    conversation.setProduitId(produitId);
                    conversation.setBuyerId(buyerId);
                    conversation.setSellerId(sellerId);
                    return conversation;
                }
            }
        }

        throw new SQLException("Impossible de creer la conversation.");
    }

    public MarketplaceConversation findConversation(int produitId, int buyerId, int sellerId) throws SQLException {
        String findReq = "SELECT * FROM marketplace_conversation WHERE produit_id=? AND buyer_id=? AND seller_id=? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(findReq)) {
            ps.setInt(1, produitId);
            ps.setInt(2, buyerId);
            ps.setInt(3, sellerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSet(rs);
                }
            }
        }
        return null;
    }

    public void touchLastMessage(int conversationId) throws SQLException {
        String req = "UPDATE marketplace_conversation SET last_message_at = NOW() WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(req)) {
            ps.setInt(1, conversationId);
            ps.executeUpdate();
        }
    }

    private MarketplaceConversation mapResultSet(ResultSet rs) throws SQLException {
        MarketplaceConversation conversation = new MarketplaceConversation();
        conversation.setId(rs.getInt("id"));
        conversation.setProduitId(rs.getInt("produit_id"));
        conversation.setBuyerId(rs.getInt("buyer_id"));
        conversation.setSellerId(rs.getInt("seller_id"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            conversation.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp lastMessageAt = rs.getTimestamp("last_message_at");
        if (lastMessageAt != null) {
            conversation.setLastMessageAt(lastMessageAt.toLocalDateTime());
        }

        return conversation;
    }

    private Timestamp toTimestamp(java.time.LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }
}
