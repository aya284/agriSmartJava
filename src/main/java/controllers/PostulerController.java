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
import java.time.LocalDateTime;
import java.util.ResourceBundle;

public class PostulerController implements Initializable {

    // Champs de saisie
    @FXML private TextField nomF, prenomF, phoneF;
    @FXML private Label cvLabel, lettreLabel;
    @FXML private Button btnSubmit;

    // Labels d'erreur (Sous les champs et sous les boutons)
    @FXML private Label nomError, prenomError, phoneError;
    @FXML private Label cvError, lettreError;

    private File selectedCV;
    private File selectedLettre;
    private final DemandeService service = new DemandeService();

    public static long currentOffreId;
    public static Demande selectedDemandeForEdit;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Active la validation en temps réel pour le texte
        setupRealTimeValidation();

        // Mode Edition : Remplir les champs si on modifie
        if (selectedDemandeForEdit != null) {
            nomF.setText(selectedDemandeForEdit.getNom());
            prenomF.setText(selectedDemandeForEdit.getPrenom());
            phoneF.setText(selectedDemandeForEdit.getPhone_number());
            cvLabel.setText(selectedDemandeForEdit.getCv());
            lettreLabel.setText(selectedDemandeForEdit.getLettre_motivation());
            btnSubmit.setText("💾 Modifier ma candidature");
        }
    }

    private void setupRealTimeValidation() {
        // Validation Nom (Style Web/PHP)
        nomF.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.trim().length() < 3) {
                nomError.setText("Le nom doit avoir au moins 3 caractères");
                nomF.setStyle("-fx-border-color: #e74c3c;");
            } else if (!newVal.matches("^[a-zA-Z\\sçéàâêîôûäëïöü]+$")) {
                nomError.setText("Lettres uniquement");
                nomF.setStyle("-fx-border-color: #e74c3c;");
            } else {
                nomError.setText("");
                nomF.setStyle("-fx-border-color: #2ecc71;");
            }
        });

        // Validation Prénom
        prenomF.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.trim().length() < 3) {
                prenomError.setText("Le prénom doit avoir au moins 3 caractères");
                prenomF.setStyle("-fx-border-color: #e74c3c;");
            } else if (!newVal.matches("^[a-zA-Z\\sçéàâêîôûäëïöü]+$")) {
                prenomError.setText("Lettres uniquement");
                prenomF.setStyle("-fx-border-color: #e74c3c;");
            } else {
                prenomError.setText("");
                prenomF.setStyle("-fx-border-color: #2ecc71;");
            }
        });

        // Validation Téléphone (8 chiffres uniquement)
        phoneF.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                phoneF.setText(oldVal);
            } else if (newVal.length() != 8) {
                phoneError.setText("Doit contenir exactement 8 chiffres");
                phoneF.setStyle("-fx-border-color: #e74c3c;");
            } else {
                phoneError.setText("");
                phoneF.setStyle("-fx-border-color: #2ecc71;");
            }
        });
    }

    @FXML
    public void handleSubmit() {
        if (!validate()) return;

        try {
            if (selectedDemandeForEdit == null) {
                boolean alreadyApplied = service.afficher().stream()
                        .anyMatch(d -> d.getUsers_id() == 2 && d.getOffre_id() == (int) currentOffreId);

                if (alreadyApplied) {
                    showAlert("Attention", "Vous avez déjà postulé pour cette offre !");
                    cancel();
                    return;
                }
            }

            String cvFileName = (selectedCV != null) ?
                    saveFile(selectedCV, "src/main/resources/uploads/cv") :
                    (selectedDemandeForEdit != null ? selectedDemandeForEdit.getCv() : "");

            String lettreFileName = (selectedLettre != null) ?
                    saveFile(selectedLettre, "src/main/resources/uploads/lettres") :
                    (selectedDemandeForEdit != null ? selectedDemandeForEdit.getLettre_motivation() : "");

            if (selectedDemandeForEdit != null) {
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

    private boolean validate() {
        boolean isValid = true;

        if (nomF.getText().trim().isEmpty() || !nomError.getText().isEmpty()) {
            if(nomF.getText().trim().isEmpty()) nomError.setText("Champ obligatoire");
            isValid = false;
        }
        if (prenomF.getText().trim().isEmpty() || !prenomError.getText().isEmpty()) {
            if(prenomF.getText().trim().isEmpty()) prenomError.setText("Champ obligatoire");
            isValid = false;
        }
        if (phoneF.getText().trim().isEmpty() || !phoneError.getText().isEmpty()) {
            if(phoneF.getText().trim().isEmpty()) phoneError.setText("Champ obligatoire");
            isValid = false;
        }

        // Validation des fichiers sous les boutons
        if (selectedDemandeForEdit == null) {
            if (selectedCV == null) {
                cvError.setText("Le CV est obligatoire");
                isValid = false;
            }
            if (selectedLettre == null) {
                lettreError.setText("La lettre est obligatoire");
                isValid = false;
            }
        }

        return isValid;
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML public void uploadCV() {
        selectedCV = selectPDF();
        if(selectedCV != null) {
            cvLabel.setText(selectedCV.getName());
            cvError.setText("");
        }
    }

    @FXML public void uploadLettre() {
        selectedLettre = selectPDF();
        if(selectedLettre != null) {
            lettreLabel.setText(selectedLettre.getName());
            lettreError.setText("");
        }
    }

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
        selectedDemandeForEdit = null;
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/Views/Offres/MesCandidaturesList.fxml"));
            StackPane contentArea = (StackPane) nomF.getScene().lookup("#contentArea");
            contentArea.getChildren().setAll(root);
        } catch (IOException e) { e.printStackTrace(); }
    }
}