package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;

import java.io.IOException;

public class MainController {

    @FXML private StackPane contentArea;
    @FXML private TextField globalSearchField;
    @FXML private Label footerStatusLabel;
    @FXML private Label currentModuleLabel;

    @FXML
    public void initialize() {
        openMarketplace();
    }

    @FXML
    public void openMarketplace() {
        loadView("/Views/MarketplaceView.fxml", "Marketplace loaded", "Marketplace");
    }

    @FXML
    public void openCulture() {
        loadView("/Views/CultureView.fxml", "Culture module not available yet", "Gestion Culture");
    }

    @FXML
    public void openTaches() {
        loadView("/Views/TachesView.fxml", "Tasks module not available yet", "Gestion Taches");
    }

    @FXML
    public void openEmployes() {
        loadView("/Views/EmployesView.fxml", "Employees module not available yet", "Gestion Employes");
    }

    @FXML
    public void openUsers() {
        loadView("/Views/UsersView.fxml", "Users module not available yet", "Utilisateurs");
    }

    @FXML
    public void handleGlobalSearch() {
        String query = globalSearchField.getText() == null ? "" : globalSearchField.getText().trim();
        if (query.isEmpty()) {
            footerStatusLabel.setText("Ready");
            return;
        }
        footerStatusLabel.setText("Searching: " + query);
    }

    @FXML
    public void showNotifications() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Notifications");
        alert.setHeaderText("Marketplace notifications");
        alert.setContentText("No new notifications.");
        alert.showAndWait();
    }

    @FXML
    public void showProfile() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("User Profile");
        alert.setHeaderText("Current user");
        alert.setContentText("Logged in as: agrismart.user@example.com");
        alert.showAndWait();
    }

    private void loadView(String fxmlPath, String statusMessage, String moduleName) {
        try {
            Parent view = FXMLLoader.load(getClass().getResource(fxmlPath));
            contentArea.getChildren().setAll(view);
            footerStatusLabel.setText(statusMessage);
            currentModuleLabel.setText("Current: " + moduleName);
        } catch (IOException e) {
            footerStatusLabel.setText(statusMessage);
            currentModuleLabel.setText("Current: " + moduleName);
        }
    }
}
