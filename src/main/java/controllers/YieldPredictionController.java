package controllers;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import entities.Consommation;
import entities.Culture;
import entities.Parcelle;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.json.JSONArray;
import org.json.JSONObject;
import services.HuggingFaceService;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

public class YieldPredictionController {

    @FXML private Label lblContext;
    @FXML private VBox loadingBox;
    @FXML private ScrollPane scrollResults;
    @FXML private Label lblTotalYield;
    @FXML private Label lblYieldPerHa;
    @FXML private VBox boxPositiveFactors;
    @FXML private VBox boxRiskFactors;
    @FXML private Label lblRecommendation;
    @FXML private Button btnExport;

    private Parcelle parcelle;
    private Culture culture;
    private List<Consommation> consommations;
    private HuggingFaceService hfService = new HuggingFaceService();

    // Stockage du JSON pour l'export PDF
    private JSONObject lastResult = null;

    public void setContext(Parcelle p, Culture c, List<Consommation> cons) {
        this.parcelle = p;
        this.culture = c;
        this.consommations = cons;
        
        lblContext.setText("Parcelle: " + p.getId() + " | Culture: " + c.getTypeCulture() + " (" + c.getVariete() + ")");
    }

    @FXML
    public void initialize() {
        // La prédiction démarre automatiquement après l'initialisation de la fenêtre (Platform.runLater)
        Platform.runLater(this::startPrediction);
    }

    private void startPrediction() {
        loadingBox.setVisible(true);
        scrollResults.setVisible(false);
        scrollResults.setManaged(false);
        btnExport.setDisable(true);

        Thread thread = new Thread(() -> {
            try {
                JSONObject result = hfService.predictYield(parcelle, culture, consommations);
                
                Platform.runLater(() -> {
                    displayResult(result);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    loadingBox.setVisible(false);
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Erreur IA");
                    alert.setHeaderText("La prédiction a échoué");
                    alert.setContentText(e.getMessage());
                    alert.showAndWait();
                    closeModal();
                });
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private void displayResult(JSONObject result) {
        this.lastResult = result;
        loadingBox.setVisible(false);
        loadingBox.setManaged(false);
        scrollResults.setVisible(true);
        scrollResults.setManaged(true);
        btnExport.setDisable(false);

        // Parsing JSON
        String yieldTotal = result.optString("estimated_yield_total", "Non estimé");
        String yieldPerHa = result.optString("estimated_yield_per_ha", "Non estimé");
        String rec = result.optString("recommendation", "Pas de recommandation");

        lblTotalYield.setText(yieldTotal);
        lblYieldPerHa.setText(yieldPerHa);
        lblRecommendation.setText(rec);

        // Facteurs positifs
        boxPositiveFactors.getChildren().clear();
        JSONArray posArr = result.optJSONArray("positive_factors");
        if (posArr != null) {
            for (int i = 0; i < posArr.length(); i++) {
                Label l = new Label("- " + posArr.getString(i));
                l.setStyle("-fx-text-fill: #2c3e50; -fx-font-size: 14px;");
                boxPositiveFactors.getChildren().add(l);
            }
        } else {
            boxPositiveFactors.getChildren().add(new Label("Aucun facteur spécifié."));
        }

        // Risques
        boxRiskFactors.getChildren().clear();
        JSONArray riskArr = result.optJSONArray("risk_factors");
        if (riskArr != null) {
            for (int i = 0; i < riskArr.length(); i++) {
                Label l = new Label("- " + riskArr.getString(i));
                l.setStyle("-fx-text-fill: #2c3e50; -fx-font-size: 14px;");
                boxRiskFactors.getChildren().add(l);
            }
        } else {
            boxRiskFactors.getChildren().add(new Label("Aucun risque spécifié."));
        }
    }

    @FXML
    private void exportPDF() {
        if (lastResult == null) return;

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer le rapport PDF");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers PDF", "*.pdf"));
        fileChooser.setInitialFileName("Rapport_Rendement_" + culture.getTypeCulture() + ".pdf");
        
        File file = fileChooser.showSaveDialog(btnExport.getScene().getWindow());
        if (file != null) {
            try {
                Document document = new Document();
                PdfWriter.getInstance(document, new FileOutputStream(file));
                document.open();

                // Styles
                Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22);
                Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
                Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 12);
                Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);

                // Titre
                Paragraph title = new Paragraph("Rapport de Prédiction de Rendement AgriSmart", titleFont);
                title.setAlignment(Element.ALIGN_CENTER);
                title.setSpacingAfter(20);
                document.add(title);

                // Contexte
                document.add(new Paragraph("Détails de la culture :", subtitleFont));
                document.add(new Paragraph("Culture : " + culture.getTypeCulture() + " (" + culture.getVariete() + ")", normalFont));
                document.add(new Paragraph("Parcelle ID : " + parcelle.getId() + " | Surface : " + parcelle.getSurface() + " Hectares", normalFont));
                document.add(new Paragraph("Date de plantation : " + culture.getDatePlantation(), normalFont));
                
                Paragraph separator = new Paragraph("--------------------------------------------------", normalFont);
                separator.setSpacingAfter(10);
                separator.setSpacingBefore(10);
                document.add(separator);

                // Résultats
                document.add(new Paragraph("Estimations de Récolte :", subtitleFont));
                document.add(new Paragraph("Rendement Total : " + lastResult.optString("estimated_yield_total", "N/A"), boldFont));
                document.add(new Paragraph("Rendement par Hectare : " + lastResult.optString("estimated_yield_per_ha", "N/A"), boldFont));
                
                document.add(separator);

                // Facteurs Positifs
                document.add(new Paragraph("Facteurs Favorables :", subtitleFont));
                JSONArray posArr = lastResult.optJSONArray("positive_factors");
                if (posArr != null) {
                    for (int i = 0; i < posArr.length(); i++) {
                        document.add(new Paragraph("- " + posArr.getString(i), normalFont));
                    }
                }

                document.add(separator);

                // Risques
                document.add(new Paragraph("Facteurs de Risque :", subtitleFont));
                JSONArray riskArr = lastResult.optJSONArray("risk_factors");
                if (riskArr != null) {
                    for (int i = 0; i < riskArr.length(); i++) {
                        document.add(new Paragraph("- " + riskArr.getString(i), normalFont));
                    }
                }

                document.add(separator);

                // Recommandations
                document.add(new Paragraph("Recommandation de l'Agronome IA :", subtitleFont));
                document.add(new Paragraph(lastResult.optString("recommendation", "Aucune"), normalFont));

                document.close();

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Succès");
                alert.setHeaderText(null);
                alert.setContentText("Le rapport PDF a été généré avec succès !");
                alert.showAndWait();

            } catch (Exception e) {
                e.printStackTrace();
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Erreur d'exportation");
                alert.setHeaderText("Impossible de créer le PDF");
                alert.setContentText(e.getMessage());
                alert.showAndWait();
            }
        }
    }

    @FXML
    private void closeModal() {
        ((Stage) btnExport.getScene().getWindow()).close();
    }
}
