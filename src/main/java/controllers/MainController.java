package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import utils.SessionManager;
import entities.User;

import java.io.IOException;

public class MainController {

    @FXML private StackPane contentArea;
    @FXML private TextField globalSearchField;
    @FXML private Label     footerStatusLabel;
    @FXML private Label     currentModuleLabel;
    @FXML private Label     userNameLabel;
    @FXML private Label     userRoleBadge;

    @FXML
    public void initialize() {
        SessionManager session = SessionManager.getInstance();
        
        // Update User Profile UI
        if (session.isLoggedIn()) {
            userNameLabel.setText(session.getCurrentUser().getFullName());
            userRoleBadge.setText(session.getCurrentUser().getRole());
        }

        if (session.isAdmin()) {
            openUsers();
        } else {
            openMarketplace();
        }
    }

    @FXML
    public void openMarketplace() {
        loadView("/Views/MarketplaceView.fxml", "Marketplace", "Marketplace");
    }

    @FXML
    public void openCulture() {
        loadView("/Views/ParcelleView.fxml", "Gestion des Parcelles chargée", "Gestion Culture");
    }

    @FXML
    public void openResources() {
        loadView("/Views/RessourceView.fxml", "Gestion des Ressources chargée", "Gestion Ressource");
    }

    @FXML
    public void openTaches() {
        loadView("/Views/TachesView.fxml", "Gestion Tâches", "Gestion Taches");
    }

    @FXML
    public void openParcelles() {
        // Redundant - Parcelle is now integrated into openCulture
    }

    @FXML
    public void openEmployes() {
        loadView("/Views/EmployesView.fxml", "Gestion Employés", "Gestion Employes");
    }

    @FXML
    public void openUsers() {
        if (!SessionManager.getInstance().isAdmin()) {
            showAccessDenied();
            return;
        }
        loadView("/Views/Admin/AdminUsersView.fxml", "Gestion Utilisateurs", "Utilisateurs");
    }

    @FXML
    public void showProfile() {
        loadView("/Views/EditProfileView.fxml", "Profil", "Profile");
    }

    @FXML
    public void showNotifications() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Notifications");
        alert.setHeaderText("Notifications");
        alert.setContentText("Aucune nouvelle notification.");
        alert.showAndWait();
    }

    @FXML
    public void handleGlobalSearch() {
        String query = globalSearchField.getText() == null
                ? "" : globalSearchField.getText().trim();
        footerStatusLabel.setText(query.isEmpty() ? "Ready" : "Recherche : " + query);
    }

    // ── Helpers ───────────────────────────────────────────────
    private void loadView(String fxmlPath, String status, String moduleName) {
        try {
            Parent view = FXMLLoader.load(getClass().getResource(fxmlPath));
            contentArea.getChildren().setAll(view);
            footerStatusLabel.setText(status);
            currentModuleLabel.setText("Current: " + moduleName);
        } catch (Exception e) {
            footerStatusLabel.setText("Erreur chargement: " + fxmlPath);
            System.err.println("Erreur loadView [" + fxmlPath + "] : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showAccessDenied() {
        Label msg = new Label("⛔ Accès refusé — réservé aux administrateurs.");
        msg.setStyle("-fx-font-size:16; -fx-text-fill:#e74c3c; -fx-padding:40;");
        contentArea.getChildren().setAll(msg);
        currentModuleLabel.setText("Current: Accès refusé");
    }
    @FXML
    public void handleLogout() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Déconnexion");
        confirm.setHeaderText("Voulez-vous vous déconnecter ?");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                SessionManager.getInstance().logout();
                try {
                    Parent root = FXMLLoader.load(
                            getClass().getResource("/Views/LoginView.fxml"));
                    contentArea.getScene().setRoot(root);
                } catch (Exception e) {
                    System.err.println("Erreur logout : " + e.getMessage());
                }
            }
        });
    }
}