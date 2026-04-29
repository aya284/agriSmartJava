package controllers;

import entities.Parcelle;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import services.GeminiService;

public class RecommendationModalController {

    @FXML private Label lblContext;
    @FXML private VBox loadingBox;
    @FXML private Label lblLoadingText;
    @FXML private VBox resultsBox;
    @FXML private VBox cardsContainer;

    private Parcelle parcelle;
    private GeminiService geminiService = new GeminiService();
    private Runnable onCultureAdded;

    public void setOnCultureAdded(Runnable callback) {
        this.onCultureAdded = callback;
    }

    public void setParcelle(Parcelle p) {
        this.parcelle = p;
        if (p != null) {
            lblContext.setText("Analyse pour la parcelle : " + p.getNom() + " (" + p.getSurface() + " Ha, " + p.getTypeSol() + ")");
            startAnalysis();
        }
    }

    private void startAnalysis() {
        loadingBox.setVisible(true);
        loadingBox.setManaged(true);
        resultsBox.setVisible(false);
        resultsBox.setManaged(false);

        Thread thread = new Thread(() -> {
            try {
                String prompt = String.format(
                    "Tu es un expert agronome travaillant en Tunisie. " +
                    "J'ai une parcelle agricole de %.2f hectares. Le sol est de type : %s. " +
                    "Les coordonnées GPS sont Latitude: %s, Longitude: %s. " +
                    "En te basant sur le climat méditerranéen de ces coordonnées et ce type de sol, " +
                    "quelles sont les 3 meilleures cultures à planter ? " +
                    "Retourne UNIQUEMENT un tableau JSON strict contenant 3 objets. " +
                    "Chaque objet doit avoir EXACTEMENT ces clés : 'nom' (le nom de la culture, ex: Tomate), 'variete' (une variété spécifique recommandée, ex: Marmande), 'reussite' (un pourcentage estimé de réussite, ex: '85%%'), " +
                    "'justification' (une brève explication de 1 à 2 phrases maximum). " +
                    "Ne rajoute aucun texte avant ou après le JSON. Ne mets pas de bloc markdown json.",
                    parcelle.getSurface(), parcelle.getTypeSol(), parcelle.getLatitude(), parcelle.getLongitude()
                );

                String resultat = geminiService.generateRecommendation(prompt);

                // Nettoyage au cas où l'IA renvoie quand même du markdown
                resultat = resultat.trim();
                if (resultat.startsWith("```json")) resultat = resultat.substring(7);
                else if (resultat.startsWith("```")) resultat = resultat.substring(3);
                if (resultat.endsWith("```")) resultat = resultat.substring(0, resultat.length() - 3);
                
                org.json.JSONArray jsonArray = new org.json.JSONArray(resultat.trim());

                Platform.runLater(() -> {
                    loadingBox.setVisible(false);
                    loadingBox.setManaged(false);
                    resultsBox.setVisible(true);
                    resultsBox.setManaged(true);
                    
                    cardsContainer.getChildren().clear();
                    
                    for (int i = 0; i < jsonArray.length(); i++) {
                        org.json.JSONObject obj = jsonArray.getJSONObject(i);
                        String nom = obj.getString("nom");
                        String variete = obj.has("variete") ? obj.getString("variete") : "";
                        String reussite = obj.getString("reussite");
                        String justification = obj.getString("justification");
                        
                        VBox card = new VBox(8);
                        card.setStyle("-fx-background-color: #f8f9fa; -fx-padding: 15; -fx-border-color: #d1d8e0; -fx-border-radius: 8; -fx-background-radius: 8;");
                        
                        Label lblTitle = new Label("🌿 " + nom + (variete.isEmpty() ? "" : " - " + variete) + " (" + reussite + " de réussite)");
                        lblTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #2c3e50;");
                        
                        Label lblDesc = new Label(justification);
                        lblDesc.setWrapText(true);
                        lblDesc.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px;");
                        
                        javafx.scene.control.Button btnPlanter = new javafx.scene.control.Button("🌱 Planter cette culture");
                        btnPlanter.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
                        btnPlanter.setOnAction(e -> openAddCultureForm(nom, variete));
                        
                        card.getChildren().addAll(lblTitle, lblDesc, btnPlanter);
                        cardsContainer.getChildren().add(card);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    loadingBox.setVisible(false);
                    loadingBox.setManaged(false);
                    resultsBox.setVisible(true);
                    resultsBox.setManaged(true);
                    cardsContainer.getChildren().clear();
                    
                    Label warnLabel = new Label("⚠️ Mode Hors-Ligne (Serveur IA surchargé) - Voici des recommandations standards :");
                    warnLabel.setStyle("-fx-text-fill: #e67e22; -fx-font-style: italic; -fx-font-size: 12px; -fx-padding: 0 0 10 0;");
                    cardsContainer.getChildren().add(warnLabel);

                    String fallbackJson = "[\n" +
                    "  {\"nom\": \"Blé dur\", \"variete\": \"Karim\", \"reussite\": \"90%\", \"justification\": \"Variété locale très résistante et adaptée à la majorité des sols tunisiens.\"},\n" +
                    "  {\"nom\": \"Olivier\", \"variete\": \"Chemlali\", \"reussite\": \"95%\", \"justification\": \"L'olivier s'adapte parfaitement à ce type de sol et demande peu d'eau.\"},\n" +
                    "  {\"nom\": \"Luzerne\", \"variete\": \"Gabès\", \"reussite\": \"80%\", \"justification\": \"Excellente culture fourragère pour restaurer la fertilité de votre parcelle.\"}\n" +
                    "]";

                    try {
                        org.json.JSONArray fallbackArray = new org.json.JSONArray(fallbackJson);
                        for (int i = 0; i < fallbackArray.length(); i++) {
                            org.json.JSONObject obj = fallbackArray.getJSONObject(i);
                            String nom = obj.getString("nom");
                            String variete = obj.has("variete") ? obj.getString("variete") : "";
                            String reussite = obj.getString("reussite");
                            String justification = obj.getString("justification");
                            
                            VBox card = new VBox(8);
                            card.setStyle("-fx-background-color: #f8f9fa; -fx-padding: 15; -fx-border-color: #d1d8e0; -fx-border-radius: 8; -fx-background-radius: 8;");
                            
                            Label lblTitle = new Label("🌿 " + nom + (variete.isEmpty() ? "" : " - " + variete) + " (" + reussite + " de réussite)");
                            lblTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #2c3e50;");
                            
                            Label lblDesc = new Label(justification);
                            lblDesc.setWrapText(true);
                            lblDesc.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px;");
                            
                            javafx.scene.control.Button btnPlanter = new javafx.scene.control.Button("🌱 Planter cette culture");
                            btnPlanter.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
                            btnPlanter.setOnAction(evt -> openAddCultureForm(nom, variete));
                            
                            card.getChildren().addAll(lblTitle, lblDesc, btnPlanter);
                            cardsContainer.getChildren().add(card);
                        }
                    } catch (Exception ex) {
                        Label errorLabel = new Label("❌ Erreur Critique : " + e.getMessage());
                        errorLabel.setStyle("-fx-text-fill: #e74c3c;");
                        cardsContainer.getChildren().add(errorLabel);
                    }
                });
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void closeModal() {
        Stage stage = (Stage) lblContext.getScene().getWindow();
        stage.close();
    }

    private void openAddCultureForm(String nomCulture, String variete) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/Views/FormulaireCulture.fxml"));
            javafx.scene.Parent root = loader.load();
            FormulaireCultureController controller = loader.getController();
            controller.setParcelleId(parcelle.getId());
            controller.prefillCultureName(nomCulture, variete);

            Stage stage = new Stage();
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.setTitle("Ajouter Culture");
            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            stage.setScene(scene);
            
            closeModal();
            
            Platform.runLater(() -> {
                stage.showAndWait();
                if (onCultureAdded != null) {
                    onCultureAdded.run();
                }
            });
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }
}
