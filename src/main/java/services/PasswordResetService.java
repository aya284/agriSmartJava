package services;

import entities.PasswordResetRequest;
import utils.MyConnection;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

public class PasswordResetService {

    private static final String SECRET_KEY      = "agrismart-secret-2026"; // change in prod
    private static final int    EXPIRY_MINUTES  = 60;
    private final Connection    conn            = MyConnection.getInstance().getConn();

    // ── Create token and persist it ───────────────────────────
    public String createResetToken(int userId) throws Exception {
        // Clean up old tokens for this user first
        deleteOldTokens(userId);

        String selector  = randomString(20);
        String rawToken  = randomString(32);
        String hashed    = hmac(rawToken);

        LocalDateTime now     = LocalDateTime.now();
        LocalDateTime expires = now.plusMinutes(EXPIRY_MINUTES);

        String sql = """
            INSERT INTO reset_password_request
                (selector, hashed_token, requested_at, expires_at, user_id)
            VALUES (?, ?, ?, ?, ?)
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, selector);
            ps.setString(2, hashed);
            ps.setTimestamp(3, Timestamp.valueOf(now));
            ps.setTimestamp(4, Timestamp.valueOf(expires));
            ps.setInt(5, userId);
            ps.executeUpdate();
        }

        // What is sent in the email: selector:rawToken
        return selector + ":" + rawToken;
    }

    // ── Validate token → returns userId if valid ──────────────
    public Optional<Integer> validateToken(String fullToken) throws Exception {
        String[] parts = fullToken.split(":");
        if (parts.length != 2) return Optional.empty();

        String selector = parts[0];
        String rawToken = parts[1];
        String hashed   = hmac(rawToken);

        String sql = """
            SELECT user_id, hashed_token, expires_at
            FROM reset_password_request
            WHERE selector = ?
            ORDER BY id DESC LIMIT 1
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, selector);
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) return Optional.empty();

            String    storedHash = rs.getString("hashed_token");
            Timestamp expiresAt  = rs.getTimestamp("expires_at");
            int       userId     = rs.getInt("user_id");

            boolean valid = storedHash.equals(hashed)
                    && expiresAt.toLocalDateTime().isAfter(LocalDateTime.now());

            return valid ? Optional.of(userId) : Optional.empty();
        }
    }

    // ── Delete all tokens for a selector after use ────────────
    public void invalidateToken(String selector) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM reset_password_request WHERE selector = ?")) {
            ps.setString(1, selector);
            ps.executeUpdate();
        }
    }

    // ── Clean stale tokens for user ───────────────────────────
    private void deleteOldTokens(int userId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM reset_password_request WHERE user_id = ?")) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    // ── Helpers ───────────────────────────────────────────────
    private String randomString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        SecureRandom rng = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++)
            sb.append(chars.charAt(rng.nextInt(chars.length())));
        return sb.toString();
    }

    private String hmac(String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET_KEY.getBytes(), "HmacSHA256"));
        return Base64.getEncoder().encodeToString(mac.doFinal(data.getBytes()));
    }
}