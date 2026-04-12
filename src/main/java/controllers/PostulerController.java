package controllers;

import entities.Demande;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import services.DemandeService;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.*;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ResourceBundle;

public class PostulerController implements Initializable {
    @FXML private TextField nomF, prenomF, phoneF;
    @FXML private Label cvLabel, lettreLabel;
    @FXML private Button btnSubmit;

    private File selectedCV;
    private File selectedLettre;
    private final DemandeService service = new DemandeService();

    public static long currentOffreId;
    // New static field for editing
    public static Demande selectedDemandeForEdit;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // If we are in edit mode, fill the fields
        if (selectedDemandeForEdit != null) {
            nomF.setText(selectedDemandeForEdit.getNom());
            prenomF.setText(selectedDemandeForEdit.getPrenom());
            phoneF.setText(selectedDemandeForEdit.getPhone_number());
            cvLabel.setText(selectedDemandeForEdit.getCv());
            lettreLabel.setText(selectedDemandeForEdit.getLettre_motivation());
            btnSubmit.setText("💾 Modifier ma candidature");
        }
    }

    @FXML
    public void handleSubmit() {
        if (!validate()) return;

        try {
            // --- NEW CHECK: Prevent duplicates for Add Mode ---
            if (selectedDemandeForEdit == null) {
                boolean alreadyApplied = service.afficher().stream()
                        .anyMatch(d -> d.getUsers_id() == 2 && d.getOffre_id() == (int) currentOffreId);

                if (alreadyApplied) {
                    showAlert("Attention", "Vous avez déjà postulé pour cette offre !");
                    cancel(); // Redirect them back to their list
                    return;
                }
            }

            // Only upload new files if the user selected new ones
            String cvFileName = (selectedCV != null) ?
                    saveFile(selectedCV, "src/main/resources/uploads/cv") :
                    (selectedDemandeForEdit != null ? selectedDemandeForEdit.getCv() : "");

            String lettreFileName = (selectedLettre != null) ?
                    saveFile(selectedLettre, "src/main/resources/uploads/lettres") :
                    (selectedDemandeForEdit != null ? selectedDemandeForEdit.getLettre_motivation() : "");

            if (selectedDemandeForEdit != null) {
                // UPDATE MODE
                selectedDemandeForEdit.setNom(nomF.getText().trim());
                selectedDemandeForEdit.setPrenom(prenomF.getText().trim());
                selectedDemandeForEdit.setPhone_number(phoneF.getText().trim());
                selectedDemandeForEdit.setCv(cvFileName);
                selectedDemandeForEdit.setLettre_motivation(lettreFileName);
                selectedDemandeForEdit.setDate_modification(LocalDateTime.now());

                service.modifier(selectedDemandeForEdit);
                showAlert("Succès", "Candidature modifiée !");
                selectedDemandeForEdit = null;
            } else {
                // ADD MODE
                Demande d = new Demande();
                d.setNom(nomF.getText().trim());
                d.setPrenom(prenomF.getText().trim());
                d.setPhone_number(phoneF.getText().trim());
                d.setDate_postulation(LocalDateTime.now());
                d.setDate_modification(LocalDateTime.now());
                d.setCv(cvFileName);
                d.setLettre_motivation(lettreFileName);
                d.setStatut("En cours");
                d.setUsers_id(2);
                d.setOffre_id((int) currentOffreId);

                service.ajouter(d);
                showAlert("Succès", "Candidature envoyée !");
            }
            cancel();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String succès, String s) {
    }

    // Helper to allow validation to skip file check if we are editing (since files already exist)
    private boolean validate() {
        StringBuilder errors = new StringBuilder();
        String nom = nomF.getText().trim();
        String prenom = prenomF.getText().trim();
        String phone = phoneF.getText().trim();

        // 1. Validation du Nom (Min 3 caractères, pas de chiffres)
        if (nom.length() < 3) {
            errors.append("- Le Nom doit contenir au moins 3 caractères.\n");
        } else if (!nom.matches("^[a-zA-Z\\sçéàâêîôûäëïöü]+$")) {
            errors.append("- Le Nom ne doit contenir que des lettres.\n");
        }

        // 2. Validation du Prénom (Min 3 caractères, pas de chiffres)
        if (prenom.length() < 3) {
            errors.append("- Le Prénom doit contenir au moins 3 caractères.\n");
        } else if (!prenom.matches("^[a-zA-Z\\sçéàâêîôûäëïöü]+$")) {
            errors.append("- Le Prénom ne doit contenir que des lettres.\n");
        }

        // 3. Validation du Téléphone (Exactement 8 chiffres)
        if (!phone.matches("\\d{8}")) {
            errors.append("- Le numéro de téléphone doit contenir exactement 8 chiffres (uniquement des nombres).\n");
        }

        // 4. Validation des fichiers (Uniquement en mode Ajout)
        if (selectedDemandeForEdit == null) {
            if (selectedCV == null) {
                errors.append("- Veuillez sélectionner votre CV (PDF).\n");
            }
            if (selectedLettre == null) {
                errors.append("- Veuillez sélectionner votre Lettre de Motivation (PDF).\n");
            }
        }

        // Affichage des erreurs s'il y en a
        if (errors.length() > 0) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Erreur de saisie");
            alert.setHeaderText("Veuillez corriger les points suivants :");
            alert.setContentText(errors.toString());
            alert.showAndWait();
            return false;
        }

        return true;
    }

    @FXML public void uploadCV() { selectedCV = selectPDF(); if(selectedCV != null) cvLabel.setText(selectedCV.getName()); }
    @FXML public void uploadLettre() { selectedLettre = selectPDF(); if(selectedLettre != null) lettreLabel.setText(selectedLettre.getName()); }

    private File selectPDF() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        return fc.showOpenDialog(nomF.getScene().getWindow());
    }

    private String saveFile(File file, String destPath) throws IOException {
        Path uploadPath = Paths.get(destPath);
        if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);
        String fileName = System.currentTimeMillis() + "_" + file.getName();
        Files.copy(file.toPath(), uploadPath.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
        return fileName;
    }

    @FXML public void cancel() {
        selectedDemandeForEdit = null; // Clean up
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/Views/Offres/MesCandidaturesList.fxml"));
            StackPane contentArea = (StackPane) nomF.getScene().lookup("#contentArea");
            contentArea.getChildren().setAll(root);
        } catch (IOException e) { e.printStackTrace(); }
    }
}