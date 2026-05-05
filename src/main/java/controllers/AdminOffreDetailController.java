package controllers;

import entities.Offre;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class AdminOffreDetailController implements Initializable {

    @FXML private Label adminDetailTitle;
    @FXML private Label adminDetailDesc;
    @FXML private Label adminDetailLieu;
    @FXML private Label adminDetailStatus;
    @FXML private Circle statusDot;

    // Static to persist selection between scene switches
    private static Offre currentSelectedOffre;

    public static void setCurrentOffre(Offre offre) {
        currentSelectedOffre = offre;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (currentSelectedOffre != null) {
            setupAdminDetailPage();
        }
    }

    private void setupAdminDetailPage() {
        adminDetailTitle.setText(currentSelectedOffre.getTitle());
        adminDetailDesc.setText(currentSelectedOffre.getDescription());
        adminDetailLieu.setText("📍 " + currentSelectedOffre.getLieu());

        String rawStatus = currentSelectedOffre.getStatut_validation();
        String formattedStatus;
        String color;

        // Consistency with your Table logic
        switch (rawStatus.toLowerCase()) {
            case "en_attente":
            case "en attente":
                formattedStatus = "En attente";
                color = "#f39c12"; // Orange
                break;
            case "approuvée":
                formattedStatus = "Approuvée";
                color = "#27ae60"; // Green
                break;
            case "refusée":
                formattedStatus = "Refusée";
                color = "#e74c3c"; // Red
                break;
            default:
                formattedStatus = rawStatus.substring(0, 1).toUpperCase() + rawStatus.substring(1);
                color = "#2c3e50";
        }

        adminDetailStatus.setText(formattedStatus);
        adminDetailStatus.setStyle("-fx-text-fill: " + color + ";");
        statusDot.setStyle("-fx-fill: " + color + ";");
    }

    @FXML
    public void goBackToTable() {
        switchView("/Views/Offres/AdminOffreList.fxml");
    }

    private void switchView(String path) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(path));
            // Get the root scene and find the contentArea StackPane
            StackPane contentArea = (StackPane) adminDetailTitle.getScene().getRoot().lookup("#contentArea");
            if (contentArea != null) {
                contentArea.getChildren().setAll(root);
            } else {
                // Fallback: if contentArea is not found in the scene root hierarchy,
                // try to get it from the parent stage/window
                StackPane parent = (StackPane) adminDetailTitle.getScene().lookup("#contentArea");
                if (parent != null) {
                    parent.getChildren().setAll(root);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

