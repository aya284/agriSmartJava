package controllers.admin;

import entities.User;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import services.UserService;
import utils.FileStorageUtils;

import java.io.FileInputStream;
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

    private User            currentUser;
    private Runnable        onBack; // callback pour rafraîchir la liste au retour
    private final UserService userService = new UserService();

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ── Appelé par AdminUsersController ──────────────────────
    public void setUser(User user, Runnable onBackCallback) {
        this.currentUser = user;
        this.onBack      = onBackCallback;
        populate();
    }

    private void populate() {
        fullNameLabel.setText(currentUser.getFullName());
        roleLabel.setText(currentUser.getRole().toUpperCase());
        firstNameLabel.setText(currentUser.getFirstName());
        lastNameLabel.setText(currentUser.getLastName());
        emailLabel.setText(currentUser.getEmail());
        phoneLabel.setText(or(currentUser.getPhone(),    "—"));
        addressLabel.setText(or(currentUser.getAddress(), "—"));
        idLabel.setText(String.valueOf(currentUser.getId()));
        googleLabel.setText(currentUser.getGoogleId() != null ? "✅ Lié" : "—");
        documentLabel.setText(or(currentUser.getDocumentFile(), "Aucun document"));

        if (currentUser.getCreatedAt() != null)
            createdAtLabel.setText(currentUser.getCreatedAt().format(FMT));
        if (currentUser.getUpdatedAt() != null)
            updatedAtLabel.setText(currentUser.getUpdatedAt().format(FMT));

        refreshStatusBadge();

        // Photo de profil
        if (currentUser.getImage() != null) {
            try {
                String path = FileStorageUtils.getAbsolutePath(currentUser.getImage());
                profileImage.setImage(new Image(new FileInputStream(path)));
            } catch (Exception ignored) {}
        }
    }

    // ── Changer statut ────────────────────────────────────────
    @FXML public void setActive()   { changeStatus("active");   }
    @FXML public void setInactive() { changeStatus("inactive"); }
    @FXML public void setPending()  { changeStatus("pending");  }

    private void changeStatus(String newStatus) {
        if (currentUser.getStatus().equals(newStatus)) {
            showStatusMsg("⚠ Statut déjà : " + newStatus, false); return;
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
        statusBadge.setText(currentUser.getStatus().toUpperCase());
        String color = switch (currentUser.getStatus().toLowerCase()) {
            case "active"   -> "#27ae60";
            case "inactive" -> "#e74c3c";
            case "pending"  -> "#f39c12";
            default         -> "#95a5a6";
        };
        statusBadge.setStyle(
                "-fx-background-color:" + color + "; -fx-text-fill:white;" +
                        "-fx-font-size:12; -fx-padding:3 14; -fx-background-radius:10;");
    }

    // ── Retour à la liste ─────────────────────────────────────
    @FXML
    public void goBack() {
        try {
            Node view = FXMLLoader.load(
                    getClass().getResource("/Views/admin/AdminUsersView.fxml"));
            StackPane contentArea = (StackPane) fullNameLabel
                    .getScene().lookup("#adminContentArea");
            contentArea.getChildren().setAll(view);
            if (onBack != null) onBack.run();
        } catch (Exception e) {
            System.err.println("Erreur retour : " + e.getMessage());
        }
    }

    // ── Helpers ───────────────────────────────────────────────
    private void showStatusMsg(String msg, boolean success) {
        statusMessage.setStyle(success
                ? "-fx-text-fill:#27ae60; -fx-font-size:12;"
                : "-fx-text-fill:#e74c3c; -fx-font-size:12;");
        statusMessage.setText(msg);
        statusMessage.setVisible(true);
    }

    private String or(String value, String fallback) {
        return (value != null && !value.isBlank()) ? value : fallback;
    }
}