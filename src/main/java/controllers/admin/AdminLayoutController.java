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
        loadContent("/Views/admin/AdminDashboard.fxml",
                "Tableau de bord", "Admin / Dashboard");
    }

    @FXML public void openUsers() {
        loadContent("/Views/admin/AdminUsersView.fxml",
                "Gestion des Utilisateurs", "Admin / Utilisateurs");
    }

    @FXML public void openCultures() {
        loadContent("/Views/CultureView.fxml",
                "Cultures", "Admin / Cultures");
    }

    @FXML public void openMarketplace() {
        loadContent("/Views/MarketplaceView.fxml",
                "Marketplace", "Admin / Marketplace");
    }

    @FXML public void openTaches() {
        loadContent("/Views/task.fxml",
                "Tâches", "Admin / Tâches");
    }

    @FXML public void openEmployes() {
        loadContent("/Views/EmployesView.fxml",
                "Employés", "Admin / Employés");
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
            Node view = FXMLLoader.load(getClass().getResource(fxmlPath));
            adminContentArea.getChildren().setAll(view);
            pageTitle.setText(title);
            pageBreadcrumb.setText(breadcrumb);
        } catch (Exception e) {
            System.err.println("Erreur chargement [" + fxmlPath + "] : " + e.getMessage());
        }
    }
}
