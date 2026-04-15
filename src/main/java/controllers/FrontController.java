package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import utils.SessionManager;
import java.io.IOException;
import java.net.URL;

public class FrontController {

    @FXML private StackPane contentArea;
    @FXML private Label footerStatusLabel;

    @FXML
    public void initialize() {
        // Automatically load the job listings for the candidate when they open the front office
        openCandidatures();
    }

    @FXML
    public void openCandidatures() {
        loadView("/Views/Offres/CandidatOffreList.fxml", "Liste des offres disponibles");
    }

    @FXML
    public void openMarketplace() {
        loadView("/Views/Marketplace/MarketplaceView.fxml", "Marketplace");
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
                    Parent root = FXMLLoader.load(getClass().getResource("/Views/LoginView.fxml"));
                    contentArea.getScene().setRoot(root);
                } catch (Exception e) {
                    System.err.println("Erreur logout : " + e.getMessage());
                }
            }
        });
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
            footerStatusLabel.setText("Erreur de chargement du module.");
        }
    }
    @FXML
    public void openMesCandidatures() {
        loadView("/Views/Offres/MesCandidaturesList.fxml", "Mon historique de candidatures");
    }
}