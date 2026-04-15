package controllers.admin;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import utils.SessionManager;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class AdminLayoutController {

    @FXML private StackPane adminContentArea;
    @FXML private Label     adminNameLabel;
    @FXML private Label     pageTitle;
    @FXML private Label     pageBreadcrumb;
    @FXML private Label     dateLabel;

    @FXML
    public void initialize() {
        // Nom de l'admin connecté
        if (SessionManager.getInstance().getCurrentUser() != null)
            adminNameLabel.setText(
                    SessionManager.getInstance().getCurrentUser().getFullName());

        // Date
        dateLabel.setText(LocalDate.now().format(
                DateTimeFormatter.ofPattern("dd MMMM yyyy")));

        // Page par défaut
        openDashboard();
    }

    @FXML public void openDashboard() {
        loadContent("/Views/Admin/AdminDashboard.fxml",
                "Tableau de bord", "Admin / Dashboard");
    }

    @FXML public void openUsers() {
        loadContent("/Views/Admin/AdminUsersView.fxml",
                "Gestion des Utilisateurs", "Admin / Utilisateurs");
    }

    @FXML public void openCultures() {
        loadContent("/Views/AdminCulturesView.fxml",
                "Gestion des Cultures", "Admin / Cultures");
    }

    @FXML public void openMarketplace() {
        loadContent("/Views/Admin/AdminMarketplaceView.fxml",
            "Marketplace Dashboard", "Admin / Marketplace");
    }

    @FXML public void openTaches() {
        loadContent("/Views/TachesView.fxml",
                "Tâches", "Admin / Tâches");
    }

    @FXML public void openEmployes() {
        loadContent("/Views/Offres/AdminOffreList.fxml",
                "Gestion des Offres", "Admin / Offres");
    }

    @FXML public void openRessources() {
        loadContent("/Views/admin/AdminRessourcesView.fxml",
                "Gestion des Ressources", "Admin / Ressources");
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
                    adminContentArea.getScene().setRoot(root);
                } catch (Exception e) {
                    System.err.println("Erreur logout : " + e.getMessage());
                }
            }
        });
    }

    // ── Helper ────────────────────────────────────────────────
    private void loadContent(String fxmlPath, String title, String breadcrumb) {
        try {
            System.out.println("[ADMIN] Requested navigation: " + fxmlPath);
            java.net.URL url = getClass().getResource(fxmlPath);
            if (url == null) {
                System.err.println("!!! FXML FILE NOT FOUND: " + fxmlPath);
                showError("Ressource manquante", "Le fichier FXML est introuvable : " + fxmlPath);
                return;
            }

            Node view = FXMLLoader.load(url);
            adminContentArea.getChildren().setAll(view);
            pageTitle.setText(title);
            pageBreadcrumb.setText(breadcrumb);
            System.out.println("[ADMIN] Load successful for: " + title);
        } catch (Exception e) {
            String errorMsg = e.getMessage() != null ? e.getMessage() : e.toString();
            System.err.println("!!! Exception during FXML load: " + errorMsg);
            e.printStackTrace();
            showError("Erreur de chargement", "Impossible d'ouvrir la page [" + title + "] :\n" + errorMsg);
        }
    }
    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
