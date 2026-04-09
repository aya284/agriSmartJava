package services;

import entities.MarketplaceMessage;
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

public class MarketplaceMessageService implements IService<MarketplaceMessage> {
    private final Connection conn = MyConnection.getInstance().getConn();

    @Override
    public void ajouter(MarketplaceMessage message) throws SQLException {
        String req = "INSERT INTO marketplace_message " +
                "(conversation_id, sender_id, content, created_at, is_read, read_at, audio_path) " +
                "VALUES (?, ?, ?, NOW(), ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(req)) {
            ps.setInt(1, message.getConversationId());
            ps.setInt(2, message.getSenderId());
            ps.setString(3, message.getContent());
            ps.setBoolean(4, message.isRead());
            ps.setTimestamp(5, toTimestamp(message.getReadAt()));
            ps.setString(6, message.getAudioPath());
            ps.executeUpdate();
        }
    }

    @Override
    public List<MarketplaceMessage> afficher() throws SQLException {
        List<MarketplaceMessage> messages = new ArrayList<>();
        String req = "SELECT * FROM marketplace_message ORDER BY created_at DESC";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(req)) {
            while (rs.next()) {
                messages.add(mapResultSet(rs));
            }
        }
        return messages;
    }

    @Override
    public void modifier(MarketplaceMessage message) throws SQLException {
        String req = "UPDATE marketplace_message SET conversation_id=?, sender_id=?, content=?, " +
                "is_read=?, read_at=?, audio_path=? WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(req)) {
            ps.setInt(1, message.getConversationId());
            ps.setInt(2, message.getSenderId());
            ps.setString(3, message.getContent());
            ps.setBoolean(4, message.isRead());
            ps.setTimestamp(5, toTimestamp(message.getReadAt()));
            ps.setString(6, message.getAudioPath());
            ps.setInt(7, message.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String req = "DELETE FROM marketplace_message WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(req)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public int countAll() throws SQLException {
        String req = "SELECT COUNT(*) FROM marketplace_message";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(req)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        }
    }

    private MarketplaceMessage mapResultSet(ResultSet rs) throws SQLException {
        MarketplaceMessage message = new MarketplaceMessage();
        message.setId(rs.getInt("id"));
        message.setConversationId(rs.getInt("conversation_id"));
        message.setSenderId(rs.getInt("sender_id"));
        message.setContent(rs.getString("content"));
        message.setRead(rs.getBoolean("is_read"));
        message.setAudioPath(rs.getString("audio_path"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            message.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp readAt = rs.getTimestamp("read_at");
        if (readAt != null) {
            message.setReadAt(readAt.toLocalDateTime());
        }

        return message;
    }

    private Timestamp toTimestamp(java.time.LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }
}
