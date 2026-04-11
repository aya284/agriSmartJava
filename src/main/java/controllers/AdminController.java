package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import java.io.IOException;
import java.net.URL;

public class AdminController {

    @FXML private StackPane contentArea;
    @FXML private Label footerStatusLabel;
    @FXML private Label adminNameLabel;

    @FXML
    public void initialize() {
        // Default page to load upon admin login
        openManageOffres();
    }

    @FXML
    public void openManageOffres() {
        // Note: This loads the admin version (OffreList.fxml) which has Edit/Delete
        loadView("/Views/Offres/AdminOffreList.fxml", "Gestion des offres d'emploi");
    }

    @FXML
    public void openDashboardStats() {
        loadView("/Views/Admin/DashboardStats.fxml", "Tableau de bord - Statistiques");
    }

    private void loadView(String fxmlPath, String statusMessage) {
        try {
            URL resource = getClass().getResource(fxmlPath);
            if (resource == null) {
                System.err.println("FXML not found: " + fxmlPath);
                return;
            }
            Parent view = FXMLLoader.load(resource);
            contentArea.getChildren().setAll(view);
            footerStatusLabel.setText(statusMessage);
        } catch (IOException e) {
            e.printStackTrace();
            footerStatusLabel.setText("Erreur: Impossible de charger le module.");
        }
    }
}