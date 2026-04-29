package controllers;

import entities.Culture;
import entities.Parcelle;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.json.JSONArray;
import org.json.JSONObject;
import services.PlantIdService;

import java.io.File;

public class DiagnosticController {

    @FXML private Label lblCultureContext;
    @FXML private StackPane imageContainer;
    @FXML private VBox uploadPromptBox;
    @FXML private ImageView imageView;
    @FXML private Rectangle scannerLine;
    @FXML private VBox loadingBox;
    @FXML private VBox resultsBox;
    
    @FXML private Label lblStatus;
    @FXML private Label lblDiseaseName;
    @FXML private Label lblTreatment;

    private Culture culture;
    private Parcelle parcelle;
    private File selectedImageFile;
    private PlantIdService plantIdService = new PlantIdService();
    private TranslateTransition scanAnimation;

    public void setContext(Culture c, Parcelle p) {
        this.culture = c;
        this.parcelle = p;
        if (c != null) {
            lblCultureContext.setText("Culture: " + c.getTypeCulture() + " (" + c.getVariete() + ")");
        }
    }

    @FXML
    public void initialize() {
        // Configuration de l'animation du scanner
        scanAnimation = new TranslateTransition(Duration.seconds(1.5), scannerLine);
        scanAnimation.setFromY(-140);
        scanAnimation.setToY(140);
        scanAnimation.setCycleCount(TranslateTransition.INDEFINITE);
        scanAnimation.setAutoReverse(true);
    }

    @FXML
    private void handleImageClick() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sélectionner une photo de la feuille");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.jpg", "*.png", "*.jpeg")
        );
        selectedImageFile = fileChooser.showOpenDialog(imageContainer.getScene().getWindow());

        if (selectedImageFile != null) {
            // Afficher l'image
            Image image = new Image(selectedImageFile.toURI().toString());
            imageView.setImage(image);
            imageView.setVisible(true);
            uploadPromptBox.setVisible(false);
            
            // Lancer le diagnostic automatique
            startDiagnostic();
        }
    }

    private void startDiagnostic() {
        resultsBox.setVisible(false);
        loadingBox.setVisible(true);
        
        // Démarrer l'animation de scan
        scannerLine.setVisible(true);
        scanAnimation.play();

        Thread diagnosticThread = new Thread(() -> {
            try {
                JSONObject result = plantIdService.analyzePlantImage(selectedImageFile);
                
                Platform.runLater(() -> {
                    processResult(result);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    stopAnimation();
                    lblStatus.setText("Erreur d'analyse");
                    lblStatus.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
                    lblDiseaseName.setText(e.getMessage());
                    resultsBox.setVisible(true);
                });
            }
        });
        diagnosticThread.setDaemon(true);
        diagnosticThread.start();
    }

    private void processResult(JSONObject response) {
        stopAnimation();
        resultsBox.setVisible(true);
        
        try {
            // 1. Vérifier si c'est bien une plante
            boolean isPlant = response.getBoolean("is_plant");
            if (!isPlant) {
                lblStatus.setText("Erreur d'image");
                lblStatus.setStyle("-fx-background-color: #e67e22; -fx-text-fill: white; -fx-padding: 3 8; -fx-border-radius: 12; -fx-background-radius: 12;");
                lblDiseaseName.setText("Ceci ne semble pas être une plante !");
                lblTreatment.setText("Veuillez prendre une photo claire d'une feuille ou d'une plante.");
                return;
            }

            JSONObject health = response.getJSONObject("health_assessment");
            boolean isHealthy = health.getBoolean("is_healthy");
            double isHealthyProb = health.getDouble("is_healthy_probability");
            
            if (isHealthy && isHealthyProb > 0.5) {
                lblStatus.setText("Plante Saine");
                lblStatus.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-padding: 3 8; -fx-border-radius: 12; -fx-background-radius: 12;");
                lblDiseaseName.setText("Aucune maladie détectée");
                lblTreatment.setText("Continuez l'entretien normal.");
            } else {
                lblStatus.setText("Maladie Détectée");
                lblStatus.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-padding: 3 8; -fx-border-radius: 12; -fx-background-radius: 12;");
                
                JSONArray diseases = health.getJSONArray("diseases");
                if (diseases.length() > 0) {
                    JSONObject topDisease = diseases.getJSONObject(0);
                    String name = topDisease.getString("name");
                    
                    lblDiseaseName.setText(name);
                    
                    if (topDisease.has("disease_details")) {
                        JSONObject details = topDisease.getJSONObject("disease_details");
                        if (details.has("treatment")) {
                            JSONObject treatment = details.getJSONObject("treatment");
                            StringBuilder tStr = new StringBuilder();
                            if (treatment.has("chemical")) {
                                JSONArray chem = treatment.getJSONArray("chemical");
                                if (chem.length() > 0) tStr.append("Chimique: ").append(chem.getString(0)).append("\n");
                            }
                            if (treatment.has("biological")) {
                                JSONArray bio = treatment.getJSONArray("biological");
                                if (bio.length() > 0) tStr.append("Biologique: ").append(bio.getString(0));
                            }
                            if (tStr.length() > 0) {
                                lblTreatment.setText(tStr.toString());
                            } else {
                                lblTreatment.setText("Aucun traitement spécifique retourné.");
                            }
                        } else {
                            lblTreatment.setText("Pas de données de traitement.");
                        }
                    } else {
                        lblTreatment.setText("Nécessite une inspection plus approfondie.");
                    }
                } else {
                    lblDiseaseName.setText("Maladie inconnue");
                    lblTreatment.setText("Aucune information disponible.");
                }
            }
        } catch (Exception e) {
            lblDiseaseName.setText("Erreur de format de réponse");
            e.printStackTrace();
        }
    }

    private void stopAnimation() {
        loadingBox.setVisible(false);
        scanAnimation.stop();
        scannerLine.setVisible(false);
    }

    @FXML
    private void closeModal() {
        ((Stage) lblStatus.getScene().getWindow()).close();
    }
}
