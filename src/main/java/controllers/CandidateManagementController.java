package controllers;

import entities.Demande;
import entities.Offre;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import services.DemandeService;
import services.EmailOffreService; // Vérifie bien cet import
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
        try {
            // 1. Récupérer la valeur RÉELLE de la ComboBox au moment du clic
            String nouveauStatut = statusCombo.getValue();

            // 2. Mettre à jour l'objet et la base de données
            selectedDemande.setStatut(nouveauStatut);
            service.modifier(selectedDemande);

            // 3. Récupérer l'offre pour le titre et le lieu
            Offre currentOffre = OffreController.getSelectedOffre();

            // 4. Envoi de l'email avec le statut choisi (Acceptée ou Refusée)
            System.out.println("Tentative d'envoi d'email avec le statut : " + nouveauStatut);

            new Thread(() -> {
                try {
                    EmailOffreService.sendCandidatureStatusEmail(
                            "akrem.zaied@etudiant-fsegt.utm.tn", // Ton mail de test
                            selectedDemande.getNom() + " " + selectedDemande.getPrenom(),
                            currentOffre.getTitle(),
                            currentOffre.getLieu(),
                            nouveauStatut // Ici on envoie "Acceptée" ou "Refusée"
                    );
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }).start();

            new Alert(Alert.AlertType.INFORMATION, "Statut mis à jour : " + nouveauStatut).show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void openCV() {
        openFile(UPLOAD_DIR + "cv\\" + selectedDemande.getCv());
    }

    @FXML
    private void openLettre() {
        openFile(UPLOAD_DIR + "lettres\\" + selectedDemande.getLettre_motivation());
    }

    private void openFile(String fullPath) {
        try {
            File file = new File(fullPath);
            if (file.exists()) {
                Desktop.getDesktop().open(file);
            } else {
                showAlert("Erreur", "Le fichier est introuvable.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir le fichier.");
        }
    }

    private void showAlert(String title, String content) {
        new Alert(Alert.AlertType.ERROR, content).showAndWait();
    }
}