package services;

import utils.MyConnection;
import java.sql.*;
import java.time.LocalDateTime;

public class LoginHistoryService {

    private final Connection conn;

    public LoginHistoryService() {
        this.conn = MyConnection.getInstance().getConn();
    }

    /**
     * Record a login attempt (SUCCESS or FAILED).
     */
    public void recordLogin(int userId, String status, String failureReason) {
        String sql = "INSERT INTO loginhistory (user_id, status, failure_reason, login_time) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, status);
            ps.setString(3, failureReason);
            ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
            System.out.println("LOG: Login recorded for user " + userId + " [" + status + "]");
        } catch (SQLException e) {
            System.err.println("ERR: Failed to record login history: " + e.getMessage());
        }
    }

    /**
     * Record a logout.
     */
    public void recordLogout(int userId) {
        String sql = "UPDATE loginhistory SET logout_time = ? WHERE user_id = ? AND logout_time IS NULL ORDER BY login_time DESC LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("ERR: Failed to record logout: " + e.getMessage());
        }
    }

    /**
     * Get number of failed attempts in the last X minutes.
     */
    public int getRecentFailedAttempts(int userId, int minutes) {
        String sql = "SELECT COUNT(*) FROM loginhistory WHERE user_id = ? AND status = 'FAILED' AND login_time > ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now().minusMinutes(minutes)));
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public boolean hasLoginHistory(int userId) {
        String sql = "SELECT 1 FROM loginhistory WHERE user_id = ? AND status = 'SUCCESS' LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}
