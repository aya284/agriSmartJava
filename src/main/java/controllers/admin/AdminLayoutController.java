package controllers.admin;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.control.ScrollPane;
import javafx.application.Platform;
import javafx.scene.input.MouseEvent;
import utils.SessionManager;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class AdminLayoutController {

    @FXML private StackPane adminContentArea;
    @FXML private Label     adminNameLabel;
    @FXML private Label     pageTitle;
    @FXML private Label     pageBreadcrumb;
    @FXML private Label     dateLabel;
    @FXML private Label     notificationBadge;

    private final services.AdminNotificationService notificationService = new services.AdminNotificationService();

    @FXML
    public void initialize() {
        // Nom de l'admin connecté
        if (SessionManager.getInstance().getCurrentUser() != null)
            adminNameLabel.setText(
                    SessionManager.getInstance().getCurrentUser().getFullName());

        // Date
        dateLabel.setText(LocalDate.now().format(
                DateTimeFormatter.ofPattern("dd MMMM yyyy")));

        // Update notification badge
        updateNotificationCount();

        // Page par défaut
        openDashboard();
    }

    private void updateNotificationCount() {
        new Thread(() -> {
            try {
                int count = notificationService.countUnread();
                Platform.runLater(() -> {
                    if (count > 0) {
                        notificationBadge.setText(String.valueOf(count));
                        notificationBadge.setVisible(true);
                    } else {
                        notificationBadge.setVisible(false);
                    }
                });
            } catch (Exception e) {
                System.err.println("Error updating notifications: " + e.getMessage());
            }
        }).start();
    }

    @FXML
    public void showNotifications(MouseEvent event) {
        try {
            java.util.List<entities.AdminNotification> unread = notificationService.getUnreadNotifications();
            if (unread.isEmpty()) {
                Alert info = new Alert(Alert.AlertType.INFORMATION);
                info.setTitle("Notifications");
                info.setHeaderText(null);
                info.setContentText("Aucune nouvelle notification.");
                info.showAndWait();
                return;
            }

            VBox container = new VBox(10);
            container.setPadding(new javafx.geometry.Insets(10));
            container.setPrefWidth(300);

            for (entities.AdminNotification n : unread) {
                VBox card = new VBox(2);
                card.getStyleClass().add("notification-item");
                
                Label title = new Label(n.getTitle());
                title.getStyleClass().add("notification-item-type");
                
                Label msg = new Label(n.getMessage());
                msg.getStyleClass().add("notification-item-msg");
                msg.setWrapText(true);
                
                Label date = new Label(n.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM HH:mm")));
                date.getStyleClass().add("notification-item-date");
                
                card.getChildren().addAll(title, msg, date);
                container.getChildren().add(card);
            }

            ScrollPane scroll = new ScrollPane(container);
            scroll.setFitToWidth(true);
            scroll.setPrefHeight(400);
            scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

            Alert dialog = new Alert(Alert.AlertType.NONE);
            dialog.setTitle("Notifications");
            dialog.getDialogPane().setContent(scroll);
            dialog.getDialogPane().getButtonTypes().add(javafx.scene.control.ButtonType.CLOSE);
            
            // Mark as read when closed
            dialog.showAndWait();
            
            new Thread(() -> {
                try {
                    notificationService.markAllAsRead();
                    updateNotificationCount();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();

        } catch (Exception e) {
            e.printStackTrace();
        }
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
        loadContent("/Views/EmployesView.fxml",
                "Employés", "Admin / Employés");
    }

    @FXML public void openRessources() {
        loadContent("/Views/Admin/AdminRessourcesView.fxml",
                "Gestion des Ressources", "Admin / Ressources");
    }

    @FXML public void openAiChat() {
        loadContent("/Views/Admin/AdminAiChatView.fxml",
                "Assistant AI AgriSmart", "Admin / Assistant AI");
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