package controllers.admin;

import entities.User;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import services.UserService;
import utils.FileStorageUtils;

import java.io.FileInputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.format.DateTimeFormatter;

public class AdminUserDetailController {

    @FXML private ImageView profileImage;
    @FXML private Label     fullNameLabel;
    @FXML private Label     roleLabel;
    @FXML private Label     statusBadge;
    @FXML private Label     firstNameLabel;
    @FXML private Label     lastNameLabel;
    @FXML private Label     emailLabel;
    @FXML private Label     phoneLabel;
    @FXML private Label     addressLabel;
    @FXML private Label     idLabel;
    @FXML private Label     createdAtLabel;
    @FXML private Label     updatedAtLabel;
    @FXML private Label     googleLabel;
    @FXML private Label     documentLabel;
    @FXML private Label     statusMessage;

    private User        currentUser;
    private Runnable    onBack;
    private final UserService userService = new UserService();

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    public void initialize() {
        documentLabel.setOnMouseClicked(e -> downloadDocument());
    }

    // ── Called by AdminUsersController after loader.load() ────
    public void setUser(User user, Runnable onBackCallback) {
        if (user == null) {
            System.err.println("[AdminUserDetailController] setUser() called with null user!");
            return;
        }
        this.currentUser = user;
        this.onBack      = onBackCallback;
        populate();
    }

    // ── Populate every label — all fields guarded against null ─
    private void populate() {
        if (currentUser == null) {
            System.err.println("[AdminUserDetailController] populate() — currentUser is null");
            return;
        }

        fullNameLabel.setText(or(currentUser.getFullName(), "—"));

        String role = currentUser.getRole();
        roleLabel.setText(role != null ? role.toUpperCase() : "—");

        firstNameLabel.setText(or(currentUser.getFirstName(), "—"));
        lastNameLabel .setText(or(currentUser.getLastName(),  "—"));
        emailLabel    .setText(or(currentUser.getEmail(),     "—"));
        phoneLabel    .setText(or(currentUser.getPhone(),     "—"));
        addressLabel  .setText(or(currentUser.getAddress(),   "—"));
        idLabel       .setText(String.valueOf(currentUser.getId()));
        googleLabel   .setText(currentUser.getGoogleId() != null ? "✅ Lié" : "—");
        documentLabel .setText(or(currentUser.getDocumentFile(), "Aucun document"));

        createdAtLabel.setText(currentUser.getCreatedAt() != null
                ? currentUser.getCreatedAt().format(FMT) : "—");
        updatedAtLabel.setText(currentUser.getUpdatedAt() != null
                ? currentUser.getUpdatedAt().format(FMT) : "—");

        refreshStatusBadge();
        loadProfileImage();
    }

    // ── Profile image with fallback ───────────────────────────
    private void loadProfileImage() {
        if (currentUser.getImage() != null && !currentUser.getImage().isBlank()) {
            try {
                String path = FileStorageUtils.getAbsolutePath(currentUser.getImage());
                Image img = new Image(new FileInputStream(path));
                if (!img.isError()) {
                    profileImage.setImage(img);
                    return;
                }
            } catch (Exception e) {
                System.err.println("[AdminUserDetailController] Image load failed: " + e.getMessage());
            }
        }
        // Fallback — silently ignore if resource is absent
        try {
            Image fallback = new Image(
                    getClass().getResourceAsStream("/images/default_avatar.png"));
            if (fallback != null && !fallback.isError())
                profileImage.setImage(fallback);
        } catch (Exception ignored) {}
    }

    // ── Status buttons ────────────────────────────────────────
    @FXML public void setActive()   { changeStatus("active");   }
    @FXML public void setInactive() { changeStatus("inactive"); }
    @FXML public void setPending()  { changeStatus("pending");  }

    private void changeStatus(String newStatus) {
        if (newStatus.equals(currentUser.getStatus())) {
            showStatusMsg("⚠ Statut déjà : " + newStatus, false);
            return;
        }
        new Thread(() -> {
            try {
                userService.updateStatus(currentUser.getId(), newStatus);
                currentUser.setStatus(newStatus);
                Platform.runLater(() -> {
                    refreshStatusBadge();
                    showStatusMsg("✔ Statut mis à jour : " + newStatus.toUpperCase(), true);
                });
            } catch (Exception e) {
                Platform.runLater(() ->
                        showStatusMsg("❌ Erreur : " + e.getMessage(), false));
            }
        }).start();
    }

    private void refreshStatusBadge() {
        String status = currentUser.getStatus() != null
                ? currentUser.getStatus().toLowerCase() : "unknown";
        statusBadge.setText(status.toUpperCase());

        String bg = switch (status) {
            case "active"   -> "#dcfce7";
            case "inactive" -> "#fee2e2";
            case "pending"  -> "#fef9c3";
            default         -> "#f1f5f9";
        };
        String fg = switch (status) {
            case "active"   -> "#15803d";
            case "inactive" -> "#b91c1c";
            case "pending"  -> "#a16207";
            default         -> "#64748b";
        };
        statusBadge.setStyle(
                "-fx-background-color:" + bg + ";" +
                        "-fx-text-fill:" + fg + ";" +
                        "-fx-font-size:11; -fx-font-weight:bold;" +
                        "-fx-padding:3 14; -fx-background-radius:20;");
    }

    @FXML
    private void downloadDocument() {
        if (currentUser == null || currentUser.getDocumentFile() == null || currentUser.getDocumentFile().isBlank()) {
            showError("Aucun document disponible pour cet utilisateur.");
            return;
        }

        try {
            Path source = Path.of(FileStorageUtils.getAbsolutePath(currentUser.getDocumentFile()));
            if (!Files.exists(source)) {
                showError("Le fichier n'existe pas sur le serveur : " + currentUser.getDocumentFile());
                return;
            }

            FileChooser chooser = new FileChooser();
            chooser.setTitle("Télécharger le document");
            chooser.setInitialFileName(source.getFileName().toString());
            File target = chooser.showSaveDialog(documentLabel.getScene().getWindow());
            if (target == null) return;

            Files.copy(source, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            showStatusMsg("✔ Document téléchargé : " + target.getName(), true);
        } catch (Exception ex) {
            showError("Erreur lors du téléchargement : " + ex.getMessage());
        }
    }

    // ── Go back ───────────────────────────────────────────────
    @FXML
    public void goBack() {
        try {
            // ✅ Instance loader — new controller's initialize() will fire
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/Views/admin/AdminUsersView.fxml"));
            Node view = loader.load();

            StackPane contentArea = (StackPane) statusBadge
                    .getScene().lookup("#adminContentArea");

            if (contentArea == null) {
                System.err.println("[AdminUserDetailController] #adminContentArea not found!");
                return;
            }

            contentArea.getChildren().setAll(view);

            // Refresh the newly loaded list controller
            AdminUsersController newCtrl = loader.getController();
            if (newCtrl != null) newCtrl.refreshList();

        } catch (Exception e) {
            System.err.println("[AdminUserDetailController] goBack() failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ── Helpers ───────────────────────────────────────────────
    private void showStatusMsg(String msg, boolean success) {
        statusMessage.setStyle(success
                ? "-fx-text-fill:#15803d; -fx-font-size:12;"
                : "-fx-text-fill:#b91c1c; -fx-font-size:12;");
        statusMessage.setText(msg);
        statusMessage.setVisible(true);
    }

    private String or(String value, String fallback) {
        return (value != null && !value.isBlank()) ? value : fallback;
    }

    private void showError(String message) {
        new Alert(Alert.AlertType.ERROR, message).show();
    }
}
