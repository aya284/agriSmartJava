package controllers;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import utils.SessionManager;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MainController {

    private static MainController activeInstance;

    @FXML private StackPane contentArea;
    @FXML private TextField globalSearchField;
    @FXML private Label footerStatusLabel;
    @FXML private Label currentModuleLabel;
    @FXML private HBox headerAlertBox;
    @FXML private Label headerAlertLabel;
    @FXML private Button btnMarketplace;
    @FXML private Button btnCulture;
    @FXML private Button btnTaches;
    @FXML private Button btnEmployes;
    @FXML private Button btnUsers;

    private final Map<String, Parent> viewCache = new HashMap<>();
    private final PauseTransition headerAlertHide = new PauseTransition(Duration.seconds(2.6));

    @FXML
    public void initialize() {
        activeInstance = this;
        headerAlertHide.setOnFinished(e -> hideHeaderAlert());

        SessionManager session = SessionManager.getInstance();
        if (session.isAdmin()) {
            openUsers();
        } else {
            openMarketplace();
        }
    }

    public static void publishHeaderAlert(String message, boolean success) {
        MainController instance = activeInstance;
        if (instance == null || message == null || message.isBlank()) {
            return;
        }

        Platform.runLater(() -> instance.showHeaderAlert(message, success ? "header-alert-success" : "header-alert-error"));
    }

    public static void publishHeaderNotice(String message) {
        MainController instance = activeInstance;
        if (instance == null || message == null || message.isBlank()) {
            return;
        }

        Platform.runLater(() -> instance.showHeaderAlert(message, "header-alert-note"));
    }

    @FXML
    public void openMarketplace() {
        loadView("/Views/Marketplace/MarketplaceView.fxml", "Marketplace loaded", "Marketplace", btnMarketplace);
    }

    @FXML
    public void openCulture() {
        loadView("/Views/CultureView.fxml", "Culture module loaded", "Gestion Culture", btnCulture);
    }

    @FXML
    public void openTaches() {
        loadView("/Views/TachesView.fxml", "Tasks module loaded", "Gestion Taches", btnTaches);
    }

    @FXML
    public void openEmployes() {
        loadView("/Views/Offres/OffreList.fxml", " liste des offres", "Gestion des Employes/Offres", btnEmployes);
    }
    @FXML
    public void openUsers() {
        if (!SessionManager.getInstance().isAdmin()) {
            showAccessDenied();
            return;
        }
        loadView("/Views/Admin/AdminUsersView.fxml", "Users module loaded", "Utilisateurs", btnUsers);
    }

    @FXML
    public void showProfile() {
        loadView("/Views/EditProfileView.fxml", "Profil", "Profile", null);
    }

    @FXML
    public void showNotifications() {
        publishHeaderNotice("Notifications - Aucune nouvelle notification.");
    }

    @FXML
    public void handleGlobalSearch() {
        String query = globalSearchField.getText() == null
                ? "" : globalSearchField.getText().trim();
        footerStatusLabel.setText(query.isEmpty() ? "Ready" : "Recherche : " + query);
    }

    private void loadView(String fxmlPath, String statusMessage, String moduleName, Button activeButton) {
        try {
            Parent view = getOrLoadView(fxmlPath);
            Node current = contentArea.getChildren().isEmpty() ? null : contentArea.getChildren().get(0);

            if (current != null && fxmlPath.equals(current.getUserData())) {
                footerStatusLabel.setText(statusMessage);
                currentModuleLabel.setText("Current: " + moduleName);
                applyActiveModuleStyle(activeButton);
                return;
            }

            if (current == null) {
                view.setOpacity(0);
                view.setTranslateY(10);
                contentArea.getChildren().setAll(view);
                animateIn(view);
            } else {
                animateSwap(current, view);
            }

            footerStatusLabel.setText(statusMessage);
            currentModuleLabel.setText("Current: " + moduleName);
            applyActiveModuleStyle(activeButton);
        } catch (IOException e) {
            e.printStackTrace();
            footerStatusLabel.setText("View unavailable: " + moduleName);
            currentModuleLabel.setText("Current: " + moduleName);
            applyActiveModuleStyle(activeButton);
        }
    }

    private Parent getOrLoadView(String fxmlPath) throws IOException {
        Parent cached = viewCache.get(fxmlPath);
        if (cached != null) {
            return cached;
        }

        Parent loaded = FXMLLoader.load(getClass().getResource(fxmlPath));
        loaded.setUserData(fxmlPath);
        viewCache.put(fxmlPath, loaded);
        return loaded;
    }

    private void animateIn(Node node) {
        FadeTransition fadeIn = new FadeTransition(Duration.millis(180), node);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        TranslateTransition slideIn = new TranslateTransition(Duration.millis(180), node);
        slideIn.setFromY(10);
        slideIn.setToY(0);

        new ParallelTransition(fadeIn, slideIn).play();
    }

    private void animateSwap(Node current, Parent next) {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(120), current);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);

        TranslateTransition slideOut = new TranslateTransition(Duration.millis(120), current);
        slideOut.setFromY(0);
        slideOut.setToY(-6);

        ParallelTransition out = new ParallelTransition(fadeOut, slideOut);
        out.setOnFinished(e -> {
            next.setOpacity(0);
            next.setTranslateY(10);
            contentArea.getChildren().setAll(next);
            animateIn(next);

            current.setOpacity(1);
            current.setTranslateY(0);
        });
        out.play();
    }

    private void applyActiveModuleStyle(Button activeButton) {
        Button[] moduleButtons = {btnMarketplace, btnCulture, btnTaches, btnEmployes, btnUsers};
        for (Button button : moduleButtons) {
            if (button != null) {
                button.getStyleClass().remove("active");
            }
        }
        if (activeButton != null && !activeButton.getStyleClass().contains("active")) {
            activeButton.getStyleClass().add("active");
        }
    }

    private void showAccessDenied() {
        Label msg = new Label("⛔ Accès refusé — réservé aux administrateurs.");
        msg.setStyle("-fx-font-size:16; -fx-text-fill:#e74c3c; -fx-padding:40;");
        contentArea.getChildren().setAll(msg);
        currentModuleLabel.setText("Current: Accès refusé");
    }

    private void showHeaderAlert(String message, String styleClass) {
        if (headerAlertBox == null || headerAlertLabel == null || currentModuleLabel == null) {
            return;
        }

        headerAlertBox.getStyleClass().removeAll("header-alert-success", "header-alert-error", "header-alert-note");
        headerAlertBox.getStyleClass().add(styleClass);
        headerAlertLabel.setText(message.trim());

        currentModuleLabel.setManaged(false);
        currentModuleLabel.setVisible(false);

        headerAlertBox.setManaged(true);
        headerAlertBox.setVisible(true);
        headerAlertBox.setOpacity(0);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(130), headerAlertBox);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();

        headerAlertHide.stop();
        headerAlertHide.playFromStart();
    }

    private void hideHeaderAlert() {
        if (headerAlertBox == null || currentModuleLabel == null) {
            return;
        }

        FadeTransition fadeOut = new FadeTransition(Duration.millis(160), headerAlertBox);
        fadeOut.setFromValue(Math.max(0, headerAlertBox.getOpacity()));
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> {
            headerAlertBox.setVisible(false);
            headerAlertBox.setManaged(false);
            headerAlertBox.setOpacity(1);

            currentModuleLabel.setManaged(true);
            currentModuleLabel.setVisible(true);
        });
        fadeOut.play();
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
