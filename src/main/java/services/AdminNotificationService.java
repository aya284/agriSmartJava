package services;

import entities.AdminNotification;
import utils.MyConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AdminNotificationService {
    private final Connection conn = MyConnection.getInstance().getConn();

    public void addNotification(String title, String message, String type, Integer relatedUserId) throws SQLException {
        String sql = "INSERT INTO admin_notification (title, message, type, related_user_id, is_read, created_at) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setString(2, message);
            ps.setString(3, type);
            if (relatedUserId != null) ps.setInt(4, relatedUserId);
            else ps.setNull(4, Types.INTEGER);
            ps.setInt(5, 0); // is_read = false
            ps.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        }
    }

    public int countUnread() throws SQLException {
        String sql = "SELECT COUNT(*) FROM admin_notification WHERE is_read = 0";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public List<AdminNotification> getUnreadNotifications() throws SQLException {
        List<AdminNotification> notifications = new ArrayList<>();
        String sql = "SELECT * FROM admin_notification WHERE is_read = 0 ORDER BY created_at DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                notifications.add(mapNotification(rs));
            }
        }
        return notifications;
    }

    public void markAllAsRead() throws SQLException {
        String sql = "UPDATE admin_notification SET is_read = 1 WHERE is_read = 0";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.executeUpdate();
        }
    }

    private AdminNotification mapNotification(ResultSet rs) throws SQLException {
        AdminNotification n = new AdminNotification();
        n.setId(rs.getInt("id"));
        n.setTitle(rs.getString("title"));
        n.setMessage(rs.getString("message"));
        n.setType(rs.getString("type"));
        n.setLink(rs.getString("link"));
        n.setRead(rs.getBoolean("is_read"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) n.setCreatedAt(ts.toLocalDateTime());
        n.setRelatedUserId(rs.getObject("related_user_id", Integer.class));
        return n;
    }
}
