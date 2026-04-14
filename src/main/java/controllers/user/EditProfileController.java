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

    // ── Photo de profil ───────────────────────────────────────
    @FXML private ImageView     profilePreview;
    @FXML private Label         imageFileLabel;

    // ── Document justificatif ─────────────────────────────────
    @FXML private Label         documentFileLabel;

    // ── Changement mot de passe ───────────────────────────────
    @FXML private PasswordField currentPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmNewPasswordField;

    // ── Feedback ──────────────────────────────────────────────
    @FXML private Label         infoMessage;
    @FXML private Label         passwordMessage;
    @FXML private Button        saveBtn;
    @FXML private Button        changePasswordBtn;

    // ── État interne ──────────────────────────────────────────
    private File selectedImage;
    private File selectedDocument;
    private final UserService    userService    = new UserService();
    private final SessionManager sessionManager = SessionManager.getInstance();

    // ─────────────────────────────────────────────────────────
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

        // Afficher la photo de profil actuelle
        if (user.getImage() != null) {
            String absPath = FileStorageUtils.getAbsolutePath(user.getImage());
            try {
                profilePreview.setImage(new Image(new FileInputStream(absPath)));
                imageFileLabel.setText("Photo actuelle : " + user.getImage());
            } catch (Exception ignored) {}
        }

        // Afficher le document actuel
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
                showInfoError("L'image ne doit pas dépasser 5 Mo."); return;
            }
            selectedImage = file;
            imageFileLabel.setText(file.getName());
            profilePreview.setImage(new Image(file.toURI().toString()));
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
                showInfoError("Le document ne doit pas dépasser 5 Mo."); return;
            }
            selectedDocument = file;
            documentFileLabel.setText(file.getName());
        }
    }

    // ── Sauvegarder le profil ─────────────────────────────────
    @FXML
    public void handleSave() {
        clearMessages();
        User current = sessionManager.getCurrentUser();

        saveBtn.setDisable(true);
        saveBtn.setText("Sauvegarde...");

        new Thread(() -> {
            try {
                // Garder les anciens fichiers si pas de nouveau choix
                String imagePath    = selectedImage    != null
                        ? FileStorageUtils.save(selectedImage,    "profiles")
                        : current.getImage();
                String documentPath = selectedDocument != null
                        ? FileStorageUtils.save(selectedDocument, "documents")
                        : current.getDocumentFile();

                // Mettre à jour l'objet user
                current.setFirstName(firstNameField.getText().trim());
                current.setLastName(lastNameField.getText().trim());
                current.setEmail(emailField.getText().trim());
                current.setPhone(phoneField.getText().trim());
                current.setAddress(addressField.getText().trim());
                current.setImage(imagePath);
                current.setDocumentFile(documentPath);

                userService.updateProfile(current);

                // Mettre à jour la session
                sessionManager.setCurrentUser(current);

                Platform.runLater(() ->
                        showInfoSuccess("✔ Profil mis à jour avec succès !"));

            } catch (Exception e) {
                Platform.runLater(() -> showInfoError(e.getMessage()));
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

        // Validation locale
        if (current.isEmpty()) { showPasswordError("Entrez votre mot de passe actuel."); return; }
        String err = Validator.validatePassword(newPw);
        if (err != null) { showPasswordError(err); return; }
        String err2 = Validator.validateConfirmPassword(newPw, confirm);
        if (err2 != null) { showPasswordError(err2); return; }

        changePasswordBtn.setDisable(true);
        changePasswordBtn.setText("Modification...");

        new Thread(() -> {
            try {
                userService.changePassword(
                        sessionManager.getCurrentUser().getId(), current, newPw);

                Platform.runLater(() -> {
                    showPasswordSuccess("✔ Mot de passe modifié avec succès !");
                    currentPasswordField.clear();
                    newPasswordField.clear();
                    confirmNewPasswordField.clear();
                });
            } catch (Exception e) {
                Platform.runLater(() -> showPasswordError(e.getMessage()));
            } finally {
                Platform.runLater(() -> {
                    changePasswordBtn.setDisable(false);
                    changePasswordBtn.setText("Modifier le mot de passe");
                });
            }
        }).start();
    }

    // ── Helpers messages ──────────────────────────────────────
    private void showInfoError(String msg) {
        infoMessage.setStyle("-fx-text-fill: #e74c3c;");
        infoMessage.setText("⚠ " + msg);
        infoMessage.setVisible(true);
    }
    private void showInfoSuccess(String msg) {
        infoMessage.setStyle("-fx-text-fill: #27ae60;");
        infoMessage.setText(msg);
        infoMessage.setVisible(true);
    }
    private void showPasswordError(String msg) {
        passwordMessage.setStyle("-fx-text-fill: #e74c3c;");
        passwordMessage.setText("⚠ " + msg);
        passwordMessage.setVisible(true);
    }
    private void showPasswordSuccess(String msg) {
        passwordMessage.setStyle("-fx-text-fill: #27ae60;");
        passwordMessage.setText(msg);
        passwordMessage.setVisible(true);
    }
    private void clearMessages() {
        infoMessage.setVisible(false);
        passwordMessage.setVisible(false);
    }
}