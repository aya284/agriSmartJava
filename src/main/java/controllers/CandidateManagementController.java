package controllers;

import entities.Demande;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import services.DemandeService;
import java.awt.Desktop;
import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

public class CandidateManagementController implements Initializable {
    @FXML private Label candidateName, phoneL, idL;
    @FXML private ComboBox<String> statusCombo;
    private static final String UPLOAD_DIR = "C:\\Users\\USER\\Documents\\Esprit\\PI JAVA\\agriSmartJava\\src\\main\\resources\\uploads\\";
    public static Demande selectedDemande; // Passed from the previous screen
    private final DemandeService service = new DemandeService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (selectedDemande != null) {
            candidateName.setText(selectedDemande.getNom() + " " + selectedDemande.getPrenom());
            phoneL.setText(selectedDemande.getPhone_number());
            idL.setText("ID Candidature: #" + selectedDemande.getId());

            statusCombo.getItems().addAll("En cours", "Acceptée", "Refusée");
            statusCombo.setValue(selectedDemande.getStatut());
        }
    }

    @FXML
    private void handleStatusUpdate() {
        try {
            selectedDemande.setStatut(statusCombo.getValue());
            service.modifier(selectedDemande);
            new Alert(Alert.AlertType.INFORMATION, "Statut mis à jour avec succès !").show();
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    private void openCV() {
        // Concatenate the base path + the subfolder + the filename from the database
        openFile(UPLOAD_DIR + "cv\\" + selectedDemande.getCv());
    }

    @FXML
    private void openLettre() {
        // Concatenate the base path + the subfolder + the filename from the database
        openFile(UPLOAD_DIR + "lettres\\" + selectedDemande.getLettre_motivation());
    }

    private void openFile(String fullPath) {
        try {
            File file = new File(fullPath);
            if (file.exists()) {
                // This triggers the default system PDF viewer (Chrome, Adobe, etc.)
                Desktop.getDesktop().open(file);
            } else {
                showAlert("Erreur", "Le fichier est introuvable au chemin : " + fullPath);
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
