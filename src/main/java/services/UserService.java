package services;

import entities.User;
import utils.MyConnection;
import utils.PasswordUtils;
import utils.Validator;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
              (first_name, last_name, email, role, password,
               phone, address, document_file, image,
               status, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1,  user.getFirstName());
            ps.setString(2,  user.getLastName());
            ps.setString(3,  user.getEmail());
            ps.setString(4,  user.getRole() != null ? user.getRole() : "agriculteur");
            ps.setString(5,  PasswordUtils.hash(user.getPassword()));
            ps.setString(6,  user.getPhone().isEmpty() ? null : user.getPhone());
            ps.setString(7,  user.getAddress().isEmpty() ? null : user.getAddress());
            ps.setString(8,  user.getDocumentFile());  // ← document_file
            ps.setString(9,  user.getImage());          // ← image
            ps.setString(10, "pending");
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
            ps.setString(6, "pending");
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
            user.setStatus("pending");
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
        // ── Dates ── ← c'était manquant
        Timestamp createdAt = rs.getTimestamp("created_at");
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (createdAt != null) u.setCreatedAt(createdAt.toLocalDateTime());
        if (updatedAt != null) u.setUpdatedAt(updatedAt.toLocalDateTime());

        return u;
    }

    public User findById(int id) throws SQLException {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapUser(rs);
        }
        return null;
    }
    // ── UPDATE PROFILE ────────────────────────────────────────
    public void updateProfile(User user) throws Exception {

        // Validation des champs modifiables
        String error = Validator.validateFirstName(user.getFirstName());
        if (error != null) throw new Exception(error);
        error = Validator.validateLastName(user.getLastName());
        if (error != null) throw new Exception(error);
        error = Validator.validateEmail(user.getEmail());
        if (error != null) throw new Exception(error);
        error = Validator.validatePhone(user.getPhone());
        if (error != null) throw new Exception(error);
        error = Validator.validateAddress(user.getAddress());
        if (error != null) throw new Exception(error);

        // Vérifier que le nouvel email n'appartient pas à un autre utilisateur
        if (emailExistsForOther(user.getEmail(), user.getId()))
            throw new Exception("Cet e-mail est déjà utilisé par un autre compte.");

        String sql = """
        UPDATE users SET
            first_name   = ?,
            last_name    = ?,
            email        = ?,
            phone        = ?,
            address      = ?,
            image        = ?,
            document_file= ?,
            updated_at   = ?
        WHERE id = ?
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getFirstName());
            ps.setString(2, user.getLastName());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getPhone());
            ps.setString(5, user.getAddress());
            ps.setString(6, user.getImage());
            ps.setString(7, user.getDocumentFile());
            ps.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(9, user.getId());
            ps.executeUpdate();
        }
    }

    // ── CHANGER MOT DE PASSE ──────────────────────────────────
    public void changePassword(int userId, String currentPassword,
                               String newPassword) throws Exception {
        // Récupérer le mot de passe actuel
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT password FROM users WHERE id = ?")) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) throw new Exception("Utilisateur introuvable.");

            if (!PasswordUtils.verify(currentPassword, rs.getString("password")))
                throw new Exception("Mot de passe actuel incorrect.");
        }

        String error = Validator.validatePassword(newPassword);
        if (error != null) throw new Exception(error);

        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE users SET password = ?, updated_at = ? WHERE id = ?")) {
            ps.setString(1, PasswordUtils.hash(newPassword));
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(3, userId);
            ps.executeUpdate();
        }
    }
    // ── LISTE TOUS LES UTILISATEURS ───────────────────────────
    public List<User> getAllUsers() throws SQLException {
        List<User> users = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM users ORDER BY created_at DESC")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) users.add(mapUser(rs));
        }
        return users;
    }

    public Optional<User> getById(int userId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM users WHERE id = ?")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapUser(rs));
                }
            }
        }
        return Optional.empty();
    }

    // ── CHANGER LE STATUT ─────────────────────────────────────
    public void updateStatus(int userId, String status) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE users SET status = ?, updated_at = ? WHERE id = ?")) {
            ps.setString(1, status);
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(3, userId);
            ps.executeUpdate();
        }
    }

    // ── RECHERCHE ─────────────────────────────────────────────
    public List<User> searchUsers(String keyword, String role, String status) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT * FROM users WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (first_name LIKE ? OR last_name LIKE ? OR email LIKE ?)");
            String k = "%" + keyword.trim() + "%";
            params.add(k); params.add(k); params.add(k);
        }
        if (role != null && !role.equals("Tous")) {
            sql.append(" AND role = ?");
            params.add(role);
        }
        if (status != null && !status.equals("Tous")) {
            sql.append(" AND status = ?");
            params.add(status);
        }
        sql.append(" ORDER BY created_at DESC");

        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++)
                ps.setObject(i + 1, params.get(i));
            ResultSet rs = ps.executeQuery();
            List<User> users = new ArrayList<>();
            while (rs.next()) users.add(mapUser(rs));
            return users;
        }
    }
    // ── HELPER ────────────────────────────────────────────────
    private boolean emailExistsForOther(String email, int userId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id FROM users WHERE email = ? AND id != ?")) {
            ps.setString(1, email);
            ps.setInt(2, userId);
            return ps.executeQuery().next();
        }
    }
    // ── COMPTER LES RÉSULTATS ─────────────────────────────────
    public int countUsers(String keyword, String role, String status) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM users WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (first_name LIKE ? OR last_name LIKE ? OR email LIKE ?)");
            String k = "%" + keyword.trim() + "%";
            params.add(k); params.add(k); params.add(k);
        }
        if (role != null && !role.equals("Tous")) {
            sql.append(" AND role = ?");
            params.add(role);
        }
        if (status != null && !status.equals("Tous")) {
            sql.append(" AND status = ?");
            params.add(status);
        }

        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++)
                ps.setObject(i + 1, params.get(i));
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    // ── RECHERCHE AVEC PAGINATION ─────────────────────────────
    public List<User> searchUsersPaged(String keyword, String role,
                                       String status, int page,
                                       int pageSize) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT * FROM users WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (first_name LIKE ? OR last_name LIKE ? OR email LIKE ?)");
            String k = "%" + keyword.trim() + "%";
            params.add(k); params.add(k); params.add(k);
        }
        if (role != null && !role.equals("Tous")) {
            sql.append(" AND role = ?");
            params.add(role);
        }
        if (status != null && !status.equals("Tous")) {
            sql.append(" AND status = ?");
            params.add(status);
        }

        sql.append(" ORDER BY created_at DESC LIMIT ? OFFSET ?");
        params.add(pageSize);
        params.add((page - 1) * pageSize);

        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++)
                ps.setObject(i + 1, params.get(i));
            ResultSet rs = ps.executeQuery();
            List<User> users = new ArrayList<>();
            while (rs.next()) users.add(mapUser(rs));
            return users;
        }
    }
    // ── FIND BY EMAIL ─────────────────────────────────────────
    public Optional<User> findByEmail(String email) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM users WHERE email = ? LIMIT 1")) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? Optional.of(mapUser(rs)) : Optional.empty();
        }
    }

    // ── RESET PASSWORD (no current password needed) ───────────
    public void resetPassword(int userId, String newPassword) throws Exception {
        String error = Validator.validatePassword(newPassword);
        if (error != null) throw new Exception(error);

        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE users SET password = ?, updated_at = ? WHERE id = ?")) {
            ps.setString(1, PasswordUtils.hash(newPassword));
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(3, userId);
            ps.executeUpdate();
        }
    }
}
