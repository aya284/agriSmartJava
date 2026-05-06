package controllers;

import entities.Demande;
import entities.Offre;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import services.DemandeService;
import services.EmailOffreService;
import java.awt.Desktop;
import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

public class CandidateManagementController implements Initializable {
    @FXML private Label candidateName, phoneL, idL;
    @FXML private ComboBox<String> statusCombo;

    private static final String UPLOAD_DIR = "C:\\Users\\USER\\Documents\\Esprit\\PI JAVA\\agriSmartJava\\src\\main\\resources\\uploads\\";

    public static Demande selectedDemande;
    private final DemandeService service = new DemandeService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (selectedDemande != null) {
            candidateName.setText(selectedDemande.getNom() + " " + selectedDemande.getPrenom());
            phoneL.setText(selectedDemande.getPhone_number());
            idL.setText("ID Candidature: #" + selectedDemande.getId());

            statusCombo.getItems().setAll("En cours", "Acceptée", "Refusée");
            statusCombo.setValue(selectedDemande.getStatut());
        }
    }

    @FXML
    private void handleStatusUpdate() {
        if (selectedDemande == null) return;

        try {
            String nouveauStatut = statusCombo.getValue();
            selectedDemande.setStatut(nouveauStatut);
            service.modifier(selectedDemande);

            Offre currentOffre = OffreController.getSelectedOffre();
            String titrePoste = (currentOffre != null) ? currentOffre.getTitle() : "Poste AgriSmart";
            String lieuPoste = (currentOffre != null) ? currentOffre.getLieu() : "Tunisie";

            // Envoi asynchrone
            new Thread(() -> {
                EmailOffreService.sendCandidatureStatusEmail(
                        "akrem.zaied@etudiant-fsegt.utm.tn",
                        selectedDemande.getNom() + " " + selectedDemande.getPrenom(),
                        titrePoste,
                        lieuPoste,
                        nouveauStatut
                );
            }).start();

            new Alert(Alert.AlertType.INFORMATION, "Statut mis à jour et email envoyé !").show();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de mettre à jour.");
        }
    }

    @FXML private void openCV() { openFile(UPLOAD_DIR + "cv\\" + selectedDemande.getCv()); }
    @FXML private void openLettre() { openFile(UPLOAD_DIR + "lettres\\" + selectedDemande.getLettre_motivation()); }

    private void openFile(String fullPath) {
        try {
            File file = new File(fullPath);
            if (file.exists()) Desktop.getDesktop().open(file);
            else showAlert("Erreur", "Fichier introuvable.");
        } catch (Exception e) { showAlert("Erreur", "Ouverture impossible."); }
    }

    private void showAlert(String title, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR, content);
            alert.setTitle(title);
            alert.showAndWait();
        });
    }
}