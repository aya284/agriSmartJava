package controllers.user;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import entities.User;
import services.UserService;
import utils.FileStorageUtils;
import utils.SessionManager;
import utils.Validator;

import java.io.File;
import java.io.FileInputStream;

public class EditProfileController {

    // ── Infos personnelles ────────────────────────────────────
    @FXML private TextField     firstNameField;
    @FXML private TextField     lastNameField;
    @FXML private TextField     emailField;
    @FXML private TextField     phoneField;
    @FXML private TextField     addressField;
    @FXML private Label         roleLabel;

    // ── Erreurs par champ ─────────────────────────────────────
    @FXML private Label         firstNameError;
    @FXML private Label         lastNameError;
    @FXML private Label         emailError;
    @FXML private Label         phoneError;
    @FXML private Label         addressError;

    // ── Photo de profil ───────────────────────────────────────
    @FXML private ImageView     profilePreview;
    @FXML private Label         imageFileLabel;
    @FXML private Label         imageMessage;

    // ── Document justificatif ─────────────────────────────────
    @FXML private Label         documentFileLabel;
    @FXML private Label         documentMessage;

    // ── Changement mot de passe ───────────────────────────────
    @FXML private PasswordField currentPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmNewPasswordField;

    // ── Erreurs mot de passe ──────────────────────────────────
    @FXML private Label         currentPasswordError;
    @FXML private Label         newPasswordError;
    @FXML private Label         confirmPasswordError;

    // ── Feedback global ───────────────────────────────────────
    @FXML private Label         infoMessage;
    @FXML private Label         passwordMessage;
    @FXML private Button        saveBtn;
    @FXML private Button        changePasswordBtn;

    // ── État interne ──────────────────────────────────────────
    private File selectedImage;
    private File selectedDocument;
    private final UserService    userService    = new UserService();
    private final SessionManager sessionManager = SessionManager.getInstance();

    @FXML
    public void initialize() {
        loadCurrentUser();
    }

    private void loadCurrentUser() {
        User user = sessionManager.getCurrentUser();
        if (user == null) return;

        firstNameField.setText(user.getFirstName());
        lastNameField.setText(user.getLastName());
        emailField.setText(user.getEmail());
        phoneField.setText(user.getPhone() != null ? user.getPhone() : "");
        addressField.setText(user.getAddress() != null ? user.getAddress() : "");
        roleLabel.setText(user.getRole());

        if (user.getImage() != null) {
            String absPath = FileStorageUtils.getAbsolutePath(user.getImage());
            try {
                profilePreview.setImage(new Image(new FileInputStream(absPath)));
                imageFileLabel.setText("Photo actuelle : " + user.getImage());
            } catch (Exception ignored) {}
        }

        if (user.getDocumentFile() != null)
            documentFileLabel.setText("Document actuel : " + user.getDocumentFile());
    }

    // ── Choisir photo de profil ───────────────────────────────
    @FXML
    public void handleChooseImage() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir une photo de profil");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images (JPEG, PNG)", "*.jpg", "*.jpeg", "*.png")
        );
        File file = chooser.showOpenDialog(saveBtn.getScene().getWindow());
        if (file != null) {
            if (file.length() > 5 * 1024 * 1024) {
                showFieldError(imageMessage, "L'image ne doit pas dépasser 5 Mo.");
                return;
            }
            selectedImage = file;
            imageFileLabel.setText(file.getName());
            profilePreview.setImage(new Image(file.toURI().toString()));
            imageMessage.setVisible(false);
        }
    }

    // ── Choisir document ──────────────────────────────────────
    @FXML
    public void handleChooseDocument() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir un document justificatif");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichiers acceptés (PDF, JPEG, PNG)",
                        "*.pdf", "*.jpg", "*.jpeg", "*.png")
        );
        File file = chooser.showOpenDialog(saveBtn.getScene().getWindow());
        if (file != null) {
            if (file.length() > 5 * 1024 * 1024) {
                showFieldError(documentMessage, "Le document ne doit pas dépasser 5 Mo.");
                return;
            }
            selectedDocument = file;
            documentFileLabel.setText(file.getName());
            documentMessage.setVisible(false);
        }
    }

    // ── Sauvegarder le profil ─────────────────────────────────
    @FXML
    public void handleSave() {
        clearMessages();

        // ── Validation par champ ──────────────────────────────
        boolean hasError = false;

        if (firstNameField.getText().trim().isEmpty()) {
            showFieldError(firstNameError, "Le prénom est obligatoire.");
            hasError = true;
        }
        if (lastNameField.getText().trim().isEmpty()) {
            showFieldError(lastNameError, "Le nom est obligatoire.");
            hasError = true;
        }

        String email = emailField.getText().trim();
        if (email.isEmpty()) {
            showFieldError(emailError, "L'adresse e-mail est obligatoire.");
            hasError = true;
        } else if (!email.matches("^[\\w.+-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            showFieldError(emailError, "Format d'e-mail invalide.");
            hasError = true;
        }

        String phone = phoneField.getText().trim();
        if (!phone.isEmpty() && !phone.matches("^[+\\d\\s\\-]{7,15}$")) {
            showFieldError(phoneError, "Numéro de téléphone invalide.");
            hasError = true;
        }

        if (hasError) return;

        // ── Sauvegarde ────────────────────────────────────────
        User current = sessionManager.getCurrentUser();
        saveBtn.setDisable(true);
        saveBtn.setText("Sauvegarde...");

        new Thread(() -> {
            try {
                String imagePath    = selectedImage    != null
                        ? FileStorageUtils.save(selectedImage,    "profiles")
                        : current.getImage();
                String documentPath = selectedDocument != null
                        ? FileStorageUtils.save(selectedDocument, "documents")
                        : current.getDocumentFile();

                current.setFirstName(firstNameField.getText().trim());
                current.setLastName(lastNameField.getText().trim());
                current.setEmail(emailField.getText().trim());
                current.setPhone(phoneField.getText().trim());
                current.setAddress(addressField.getText().trim());
                current.setImage(imagePath);
                current.setDocumentFile(documentPath);

                userService.updateProfile(current);
                sessionManager.setCurrentUser(current);

                Platform.runLater(() -> showGlobalSuccess(infoMessage, "✔ Profil mis à jour avec succès !"));

            } catch (Exception e) {
                Platform.runLater(() -> showGlobalError(infoMessage, e.getMessage()));
            } finally {
                Platform.runLater(() -> {
                    saveBtn.setDisable(false);
                    saveBtn.setText("Sauvegarder");
                });
            }
        }).start();
    }

    // ── Changer le mot de passe ───────────────────────────────
    @FXML
    public void handleChangePassword() {
        clearMessages();

        String current = currentPasswordField.getText();
        String newPw   = newPasswordField.getText();
        String confirm = confirmNewPasswordField.getText();

        boolean hasError = false;

        if (current.isEmpty()) {
            showFieldError(currentPasswordError, "Entrez votre mot de passe actuel.");
            hasError = true;
        }

        String err = Validator.validatePassword(newPw);
        if (err != null) {
            showFieldError(newPasswordError, err);
            hasError = true;
        }

        String err2 = Validator.validateConfirmPassword(newPw, confirm);
        if (err2 != null) {
            showFieldError(confirmPasswordError, err2);

            hasError = true;
        }

        if (hasError) return;

        changePasswordBtn.setDisable(true);
        changePasswordBtn.setText("Modification...");

        new Thread(() -> {
            try {
                userService.changePassword(
                        sessionManager.getCurrentUser().getId(), current, newPw);

                Platform.runLater(() -> {
                    showGlobalSuccess(passwordMessage, "✔ Mot de passe modifié avec succès !");
                    currentPasswordField.clear();
                    newPasswordField.clear();
                    confirmNewPasswordField.clear();
                });
            } catch (Exception e) {
                Platform.runLater(() -> showGlobalError(passwordMessage, e.getMessage()));
            } finally {
                Platform.runLater(() -> {
                    changePasswordBtn.setDisable(false);
                    changePasswordBtn.setText("Modifier le mot de passe");
                });
            }
        }).start();
    }

    // ── Helpers messages ──────────────────────────────────────

    /** Red label directly under a single input field */
    private void showFieldError(Label label, String msg) {
        label.setText("⚠ " + msg);
        label.setVisible(true);
    }

    /** Green success banner (section-level) */
    private void showGlobalSuccess(Label label, String msg) {
        label.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 12;");
        label.setText(msg);
        label.setVisible(true);
    }

    /** Red error banner (section-level, e.g. server error) */
    private void showGlobalError(Label label, String msg) {
        label.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12;");
        label.setText("⚠ " + msg);
        label.setVisible(true);
    }

    private void clearMessages() {
        // Per-field errors
        for (Label l : new Label[]{
                firstNameError, lastNameError, emailError,
                phoneError, addressError,
                currentPasswordError, newPasswordError, confirmPasswordError,
                imageMessage, documentMessage
        }) l.setVisible(false);

        // Global banners
        infoMessage.setVisible(false);
        passwordMessage.setVisible(false);
    }
}