package controllers.user;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.stage.FileChooser;
import entities.User;
import services.UserService;
import utils.FileStorageUtils;
import utils.Validator;

import java.io.File;

public class RegisterController {

    // ── Champs texte ──────────────────────────────────────────
    @FXML private TextField      firstNameField;
    @FXML private TextField      lastNameField;
    @FXML private TextField      emailField;
    @FXML private PasswordField  passwordField;
    @FXML private PasswordField  confirmPasswordField;
    @FXML private TextField      phoneField;
    @FXML private TextField      addressField;
    @FXML private ComboBox<String> roleComboBox;

    // ── Fichiers ──────────────────────────────────────────────
    @FXML private Label    documentFileLabel;
    @FXML private Label    imageFileLabel;
    @FXML private ImageView profilePreview;

    // ── Feedback ──────────────────────────────────────────────
    @FXML private Label  errorLabel;
    @FXML private Button registerBtn;

    // ── État interne ──────────────────────────────────────────
    private File selectedDocument;
    private File selectedImage;

    private final UserService userService = new UserService();

    // ─────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        roleComboBox.getItems().addAll("agriculteur", "fournisseur", "employee");
        roleComboBox.setValue("agriculteur");
    }

    // ── Sélection du document justificatif ───────────────────
    @FXML
    public void handleChooseDocument() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir un document justificatif");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichiers acceptés (PDF, JPEG, PNG)",
                        "*.pdf", "*.jpg", "*.jpeg", "*.png")
        );
        File file = chooser.showOpenDialog(registerBtn.getScene().getWindow());
        if (file != null) {
            if (file.length() > 5 * 1024 * 1024) {
                showError("Le document ne doit pas dépasser 5 Mo.");
                return;
            }
            selectedDocument = file;
            documentFileLabel.setText(file.getName());
        }
    }

    // ── Sélection de la photo de profil ──────────────────────
    @FXML
    public void handleChooseImage() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir une photo de profil");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images (JPEG, PNG)", "*.jpg", "*.jpeg", "*.png")
        );
        File file = chooser.showOpenDialog(registerBtn.getScene().getWindow());
        if (file != null) {
            if (file.length() > 5 * 1024 * 1024) {
                showError("L'image ne doit pas dépasser 5 Mo.");
                return;
            }
            selectedImage = file;
            imageFileLabel.setText(file.getName());
            profilePreview.setImage(new Image(file.toURI().toString()));
        }
    }

    // ── Inscription ───────────────────────────────────────────
    @FXML
    public void handleRegister() {
        clearError();

        // Validation côté client
        String error = validate();
        if (error != null) { showError(error); return; }

        registerBtn.setDisable(true);
        registerBtn.setText("Création du compte...");

        new Thread(() -> {
            try {
                // Copier les fichiers dans le dossier uploads
                String documentPath = FileStorageUtils.save(selectedDocument, "documents");
                String imagePath    = FileStorageUtils.save(selectedImage,    "profiles");

                // Construire l'objet User
                User user = new User(
                        firstNameField.getText().trim(),
                        lastNameField.getText().trim(),
                        emailField.getText().trim(),
                        roleComboBox.getValue(),
                        passwordField.getText(),
                        phoneField.getText().trim(),
                        addressField.getText().trim()
                );
                user.setDocumentFile(documentPath);
                user.setImage(imagePath);

                userService.register(user);

                Platform.runLater(() -> {
                    showSuccess("Compte créé avec succès ! Redirection...");
                    goToLogin();
                });

            } catch (Exception e) {
                Platform.runLater(() -> showError(e.getMessage()));
            } finally {
                Platform.runLater(() -> {
                    registerBtn.setDisable(false);
                    registerBtn.setText("Créer le compte");
                });
            }
        }).start();
    }

    // ── Validation locale ─────────────────────────────────────
    private String validate() {
        return Validator.validateRegisterForm(
                firstNameField.getText().trim(),
                lastNameField.getText().trim(),
                emailField.getText().trim(),
                passwordField.getText(),
                confirmPasswordField.getText(),
                phoneField.getText().trim(),
                addressField.getText().trim(),
                roleComboBox.getValue()
        );
    }
    // ── Navigation ────────────────────────────────────────────
    @FXML
    public void goToLogin() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/Views/LoginView.fxml"));
            firstNameField.getScene().setRoot(root);
        } catch (Exception e) {
            showError("Erreur de navigation : " + e.getMessage());
        }
    }

    // ── Helpers ───────────────────────────────────────────────
    private void showError(String msg) {
        errorLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12;");
        errorLabel.setText("⚠ " + msg);
        errorLabel.setVisible(true);
    }

    private void showSuccess(String msg) {
        errorLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 12;");
        errorLabel.setText("✔ " + msg);
        errorLabel.setVisible(true);
    }

    private void clearError() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
    }
}