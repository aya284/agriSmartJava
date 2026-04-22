package controllers;

import entities.Demande;
import entities.Offre;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import services.DemandeService;
import services.MatchingServiceIA; // Import du service IA
import services.EmailOffreService;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class OffreDetailAdminController implements Initializable {
    @FXML private Label titleL, lieuL, salaireL, descL, countL;
    @FXML private VBox demandesContainer;

    // Nouveaux éléments ajoutés pour l'IA
    @FXML private HBox topCandidatsIA;
    @FXML private Button btnTopIA;

    private final DemandeService demandeService = new DemandeService();
    private final MatchingServiceIA matchingService = new MatchingServiceIA();
    private Offre currentOffre = OffreController.getSelectedOffre();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (currentOffre != null) {
            titleL.setText(currentOffre.getTitle());
            lieuL.setText(currentOffre.getLieu());
            salaireL.setText(currentOffre.getSalaire() + " DT");
            descL.setText(currentOffre.getDescription());
            loadDemandes();

            // Action du bouton IA
            btnTopIA.setOnAction(e -> runIAMatching());
        }
    }

    private void runIAMatching() {
        btnTopIA.setDisable(true);
        btnTopIA.setText("Analyse...");
        topCandidatsIA.getChildren().clear();
        topCandidatsIA.getChildren().add(new Label("L'IA analyse les CV..."));

        new Thread(() -> {
            try {
                List<Demande> candidatures = demandeService.afficher().stream()
                        .filter(d -> d.getOffre_id() == currentOffre.getId().intValue())
                        .collect(Collectors.toList());

                // Calcul des scores
                for (Demande d : candidatures) {
                    int score = matchingService.getMatchingScore(d, currentOffre);
                    d.setScoreIA(score); // Assure-toi d'avoir ce champ (ou utilise une Map)
                }

                // Trier pour avoir les meilleurs
                candidatures.sort((a, b) -> Integer.compare(b.getScoreIA(), a.getScoreIA()));
                List<Demande> top5 = candidatures.stream().limit(5).collect(Collectors.toList());

                Platform.runLater(() -> {
                    topCandidatsIA.getChildren().clear();
                    for (int i = 0; i < top5.size(); i++) {
                        topCandidatsIA.getChildren().add(createIABadge(top5.get(i), i + 1));
                    }
                    btnTopIA.setDisable(false);
                    btnTopIA.setText("⭐ Voir Top 5 IA");
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> btnTopIA.setDisable(false));
            }
        }).start();
    }

    // Crée le petit carré blanc avec score vert comme sur le Web
    private VBox createIABadge(Demande d, int rank) {
        VBox badge = new VBox(2);
        badge.setAlignment(Pos.CENTER);
        badge.setStyle("-fx-background-color: white; -fx-padding: 10; -fx-background-radius: 5; " +
                "-fx-border-color: #d1e7dd; -fx-border-width: 1; -fx-min-width: 80;");

        Label scoreLabel = new Label(d.getScoreIA() + "%");
        scoreLabel.setStyle("-fx-text-fill: #198754; -fx-font-weight: bold; -fx-font-size: 14;");

        Label nameLabel = new Label(d.getNom());
        nameLabel.setStyle("-fx-font-size: 10; -fx-text-fill: #6c757d;");

        Label rankLabel = new Label("RANG #" + rank);
        rankLabel.setStyle("-fx-background-color: #f1f8f5; -fx-text-fill: #198754; -fx-font-size: 8; -fx-padding: 2 5;");

        badge.getChildren().addAll(scoreLabel, nameLabel, rankLabel);
        return badge;
    }

    private void loadDemandes() {
        try {
            List<Demande> filtered = demandeService.afficher().stream()
                    .filter(d -> d.getOffre_id() == currentOffre.getId().intValue())
                    .collect(Collectors.toList());

            countL.setText("Candidatures (" + filtered.size() + ")");
            demandesContainer.getChildren().clear();

            for (Demande d : filtered) {
                demandesContainer.getChildren().add(createDemandeRow(d));
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private HBox createDemandeRow(Demande d) {
        HBox row = new HBox(20);
        row.setStyle("-fx-padding: 15; -fx-border-color: #eee; -fx-border-width: 0 0 1 0; -fx-alignment: CENTER_LEFT;");

        Label name = new Label(d.getNom() + " " + d.getPrenom());
        name.setStyle("-fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button manageBtn = new Button("Voir");
        manageBtn.setOnAction(e -> {
            CandidateManagementController.selectedDemande = d;
            new Thread(() -> {
                try {
                    EmailOffreService.sendCandidatureStatusEmail(
                            "akrem.zaied@etudiant-fsegt.utm.tn",
                            d.getNom() + " " + d.getPrenom(),
                            currentOffre.getTitle(),
                            currentOffre.getLieu(),
                            "En cours d'examen"
                    );
                } catch (Exception ex) { ex.printStackTrace(); }
            }).start();
            switchView("/Views/Offres/CandidateManagement.fxml");
        });

        row.getChildren().addAll(name, spacer, manageBtn);
        return row;
    }

    private void switchView(String path) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(path));
            StackPane contentArea = (StackPane) demandesContainer.getScene().lookup("#contentArea");
            if (contentArea != null) {
                contentArea.getChildren().setAll(root);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }
}