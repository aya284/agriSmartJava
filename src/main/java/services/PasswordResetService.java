package services;

import utils.ConfigLoader;

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

    private static final String SECRET_KEY      = ConfigLoader.get("APP_SECRET", "agrismart-fallback-secret");
    private static final int    EXPIRY_MINUTES  = 60;
    private final Connection    conn            = MyConnection.getInstance().getConn();

    // ── Create OTP and persist it ────────────────────────────
    public String createResetOTP(int userId) throws Exception {
        // Clean up old tokens/OTPs for this user first
        deleteOldTokens(userId);

        String otp = generateOTP(6);
        String hashed = hmac(otp);

        LocalDateTime now     = LocalDateTime.now();
        LocalDateTime expires = now.plusMinutes(EXPIRY_MINUTES);

        String sql = """
            INSERT INTO reset_password_request
                (selector, hashed_token, requested_at, expires_at, user_id)
            VALUES (?, ?, ?, ?, ?)
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "OTP_REQUEST"); // We use a generic selector for OTPs
            ps.setString(2, hashed);
            ps.setTimestamp(3, Timestamp.valueOf(now));
            ps.setTimestamp(4, Timestamp.valueOf(expires));
            ps.setInt(5, userId);
            ps.executeUpdate();
        }

        return otp;
    }

    // ── Validate OTP → returns userId if valid ───────────────
    public Optional<Integer> validateOTP(String otp) throws Exception {
        String hashed = hmac(otp);

        String sql = """
            SELECT user_id, expires_at
            FROM reset_password_request
            WHERE hashed_token = ?
            ORDER BY id DESC LIMIT 1
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, hashed);
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) return Optional.empty();

            int       userId     = rs.getInt("user_id");
            Timestamp expiresAt  = rs.getTimestamp("expires_at");

            boolean valid = expiresAt.toLocalDateTime().isAfter(LocalDateTime.now());

            return valid ? Optional.of(userId) : Optional.empty();
        }
    }

    // ── Delete all OTPs for a user after use ─────────────────
    public void invalidateOTP(int userId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM reset_password_request WHERE user_id = ?")) {
            ps.setInt(1, userId);
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
    private String generateOTP(int length) {
        String digits = "0123456789";
        SecureRandom rng = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++)
            sb.append(digits.charAt(rng.nextInt(digits.length())));
        return sb.toString();
    }

    private String hmac(String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET_KEY.getBytes(), "HmacSHA256"));
        return Base64.getEncoder().encodeToString(mac.doFinal(data.getBytes()));
    }
}