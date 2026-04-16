package controllers;

import entities.Demande;
import entities.Offre;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import services.DemandeService;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class OffreDetailAdminController implements Initializable {
    @FXML private Label titleL, lieuL, salaireL, descL, countL;
    @FXML private VBox demandesContainer;

    private final DemandeService demandeService = new DemandeService();
    // This now works because we added the getter in OffreController
    private Offre currentOffre = OffreController.getSelectedOffre();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (currentOffre != null) {
            titleL.setText(currentOffre.getTitle());
            lieuL.setText(currentOffre.getLieu());
            salaireL.setText(currentOffre.getSalaire() + " DT");
            descL.setText(currentOffre.getDescription());
            loadDemandes();
        }
    }

    private void loadDemandes() {
        try {
            // Filter applications to only show those for this specific offer
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
            switchView("/Views/Offres/CandidateManagement.fxml");
        });

        row.getChildren().addAll(name, spacer, manageBtn);
        return row;
    }

    private void switchView(String path) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(path));
            StackPane contentArea = (StackPane) demandesContainer.getScene().lookup("#contentArea");
            contentArea.getChildren().setAll(root);
        } catch (Exception e) { e.printStackTrace(); }
    }
}