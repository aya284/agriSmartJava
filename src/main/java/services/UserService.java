package services;

import entities.User;
import utils.MyConnection;
import utils.PasswordUtils;
import utils.Validator;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Optional;

public class UserService {
    private final Connection conn = MyConnection.getInstance().getConn();

    // ── REGISTER ──────────────────────────────────────────────
    public void register(User user) throws Exception {
        String error = Validator.validateRegisterForm(
                user.getFirstName(), user.getLastName(), user.getEmail(),
                user.getPassword(), user.getPassword(),
                user.getPhone(), user.getAddress(), user.getRole()
        );
        if (error != null) throw new Exception(error);

        if (emailExists(user.getEmail()))
            throw new Exception("Cet e-mail est déjà utilisé.");

        String sql = """
            INSERT INTO users
              (first_name, last_name, email, role, password, phone, address,
               document_file, image, status, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getFirstName());
            ps.setString(2, user.getLastName());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getRole() != null ? user.getRole() : "agriculteur");
            ps.setString(5, PasswordUtils.hash(user.getPassword()));
            ps.setString(6, user.getPhone());
            ps.setString(7, user.getAddress());
            ps.setString(8, user.getDocumentFile());   // ← ajouté
            ps.setString(9, user.getImage());           // ← ajouté
            ps.setString(10, "active");
            ps.setTimestamp(11, Timestamp.valueOf(LocalDateTime.now()));
            ps.setTimestamp(12, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) user.setId(keys.getInt(1));
        }
    }

    // ── LOGIN ─────────────────────────────────────────────────
    public Optional<User> login(String email, String password) throws Exception {
        // Validation centralisée
        String error = Validator.validateLoginForm(email, password);
        if (error != null) throw new Exception(error);

        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM users WHERE email = ?")) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();

            if (!rs.next())
                throw new Exception("E-mail ou mot de passe incorrect.");

            if (!PasswordUtils.verify(password, rs.getString("password")))
                throw new Exception("E-mail ou mot de passe incorrect.");

            String status = rs.getString("status");
            if (!"active".equalsIgnoreCase(status))
                throw new Exception("Compte " + status + ". Veuillez contacter le support.");

            return Optional.of(mapUser(rs));
        }
    }

    // ── GOOGLE LOGIN / AUTO-REGISTER ─────────────────────────
    public Optional<User> loginWithGoogle(String googleId, String email,
                                          String firstName, String lastName) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM users WHERE google_id = ? OR email = ?")) {
            ps.setString(1, googleId);
            ps.setString(2, email);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                if (rs.getString("google_id") == null)
                    linkGoogleId(rs.getInt("id"), googleId);
                return Optional.of(mapUser(rs));
            }
        }

        String sql = """
            INSERT INTO users
              (first_name, last_name, email, role, password, status, google_id, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, firstName);
            ps.setString(2, lastName);
            ps.setString(3, email);
            ps.setString(4, "agriculteur");
            ps.setString(5, "");
            ps.setString(6, "active");
            ps.setString(7, googleId);
            ps.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now()));
            ps.setTimestamp(9, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();

            User user = new User();
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setEmail(email);
            user.setRole("agriculteur");
            user.setGoogleId(googleId);
            user.setStatus("active");
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) user.setId(keys.getInt(1));
            return Optional.of(user);
        }
    }

    // ── HELPERS ───────────────────────────────────────────────
    private boolean emailExists(String email) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id FROM users WHERE email = ?")) {
            ps.setString(1, email);
            return ps.executeQuery().next();
        }
    }

    private void linkGoogleId(int userId, String googleId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE users SET google_id = ? WHERE id = ?")) {
            ps.setString(1, googleId);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    private User mapUser(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setFirstName(rs.getString("first_name"));
        u.setLastName(rs.getString("last_name"));
        u.setEmail(rs.getString("email"));
        u.setRole(rs.getString("role"));
        u.setPhone(rs.getString("phone"));
        u.setAddress(rs.getString("address"));
        u.setImage(rs.getString("image"));
        u.setDocumentFile(rs.getString("document_file"));
        u.setStatus(rs.getString("status"));
        u.setGoogleId(rs.getString("google_id"));
        return u;
    }
}