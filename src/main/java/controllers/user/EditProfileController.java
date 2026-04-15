package controllers.user;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
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

    // ── Fields ─────────────────────────────
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private TextField addressField;

    @FXML private Label roleLabel;
    @FXML private Label fullNameLabel;

    @FXML private ImageView profilePreview;
    @FXML private Label documentFileLabel;

    @FXML private PasswordField currentPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmNewPasswordField;

    @FXML private PasswordField deletePasswordField;

    @FXML private Label infoMessage;
    @FXML private Label passwordMessage;
    @FXML private Label deleteMessage;

    @FXML private Button saveBtn;
    @FXML private Button changePasswordBtn;
    @FXML private Button deleteAccountBtn;

    // ── Internal ───────────────────────────
    private File selectedImage;
    private File selectedDocument;

    private final UserService userService = new UserService();
    private final SessionManager sessionManager = SessionManager.getInstance();

    @FXML
    public void initialize() {
        loadCurrentUser();
    }

    private void loadCurrentUser() {
        User user = sessionManager.getCurrentUser();
        if (user == null) return;

        fullNameLabel.setText(user.getFirstName() + " " + user.getLastName());

        firstNameField.setText(user.getFirstName());
        lastNameField.setText(user.getLastName());
        emailField.setText(user.getEmail());
        phoneField.setText(user.getPhone() != null ? user.getPhone() : "");
        addressField.setText(user.getAddress() != null ? user.getAddress() : "");
        roleLabel.setText(user.getRole());

        try {
            if (user.getImage() != null) {
                String path = FileStorageUtils.getAbsolutePath(user.getImage());
                profilePreview.setImage(new Image(new FileInputStream(path)));
            }
        } catch (Exception ignored) {}

        if (user.getDocumentFile() != null) {
            documentFileLabel.setText(user.getDocumentFile());
        }
    }

    // ── IMAGE ─────────────────────────────
    @FXML
    public void handleChooseImage() {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.jpg", "*.png", "*.jpeg")
        );

        File file = chooser.showOpenDialog(saveBtn.getScene().getWindow());
        if (file != null) {
            selectedImage = file;
            profilePreview.setImage(new Image(file.toURI().toString()));
        }
    }

    // ── DOCUMENT ──────────────────────────
    @FXML
    public void handleChooseDocument() {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Files", "*.pdf", "*.jpg", "*.png")
        );

        File file = chooser.showOpenDialog(saveBtn.getScene().getWindow());
        if (file != null) {
            selectedDocument = file;
            documentFileLabel.setText(file.getName());
        }
    }

    // ── SAVE PROFILE ──────────────────────
    @FXML
    public void handleSave() {
        clearStyles();

        if (firstNameField.getText().isEmpty()) {
            showError("Le prénom est obligatoire", firstNameField);
            return;
        }

        if (lastNameField.getText().isEmpty()) {
            showError("Le nom est obligatoire", lastNameField);
            return;
        }

        String email = emailField.getText();
        if (!email.matches("^[\\w.+-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            showError("Email invalide", emailField);
            return;
        }

        User current = sessionManager.getCurrentUser();

        saveBtn.setDisable(true);

        new Thread(() -> {
            try {
                if (selectedImage != null) {
                    current.setImage(FileStorageUtils.save(selectedImage, "profiles"));
                }

                if (selectedDocument != null) {
                    current.setDocumentFile(FileStorageUtils.save(selectedDocument, "documents"));
                }

                current.setFirstName(firstNameField.getText());
                current.setLastName(lastNameField.getText());
                current.setEmail(email);
                current.setPhone(phoneField.getText());
                current.setAddress(addressField.getText());

                userService.updateProfile(current);

                Platform.runLater(() ->
                        showSuccess(infoMessage, "Profil mis à jour ✔"));

            } catch (Exception e) {
                Platform.runLater(() ->
                        showError(e.getMessage(), null));
            } finally {
                Platform.runLater(() -> saveBtn.setDisable(false));
            }
        }).start();
    }

    // ── PASSWORD ──────────────────────────
    @FXML
    public void handleChangePassword() {
        clearStyles();

        String err = Validator.validatePassword(newPasswordField.getText());
        if (err != null) {
            showError(err, newPasswordField);
            return;
        }

        if (!newPasswordField.getText().equals(confirmNewPasswordField.getText())) {
            showError("Confirmation incorrecte", confirmNewPasswordField);
            return;
        }

        changePasswordBtn.setDisable(true);

        new Thread(() -> {
            try {
                userService.changePassword(
                        sessionManager.getCurrentUser().getId(),
                        currentPasswordField.getText(),
                        newPasswordField.getText()
                );

                Platform.runLater(() ->
                        showSuccess(passwordMessage, "Mot de passe changé ✔"));

            } catch (Exception e) {
                Platform.runLater(() ->
                        showError(e.getMessage(), null));
            } finally {
                Platform.runLater(() -> changePasswordBtn.setDisable(false));
            }
        }).start();
    }

    // ── DELETE ────────────────────────────
    @FXML
    public void handleDeleteAccount() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setContentText("Supprimer votre compte ?");

        alert.showAndWait().ifPresent(r -> {
            if (r != ButtonType.OK) return;

            new Thread(() -> {
                try {
                    userService.deleteAccount(
                            sessionManager.getCurrentUser().getId(),
                            deletePasswordField.getText()
                    );

                    Platform.runLater(() -> {
                        try {
                            Parent root = FXMLLoader.load(
                                    getClass().getResource("/Views/LoginView.fxml"));
                            deleteAccountBtn.getScene().setRoot(root);
                        } catch (Exception ignored) {}
                    });

                } catch (Exception e) {
                    Platform.runLater(() ->
                            showError(e.getMessage(), null));
                }
            }).start();
        });
    }

    // ── UI HELPERS ────────────────────────
    private void showError(String msg, Control field) {
        infoMessage.setText("⚠ " + msg);
        infoMessage.setStyle("-fx-text-fill: red;");
        infoMessage.setVisible(true);

        if (field != null)
            field.setStyle("-fx-border-color: red;");
    }

    private void showSuccess(Label label, String msg) {
        label.setText(msg);
        label.setStyle("-fx-text-fill: green;");
        label.setVisible(true);
    }

    private void clearStyles() {
        infoMessage.setVisible(false);
        passwordMessage.setVisible(false);

        for (Control c : new Control[]{
                firstNameField, lastNameField, emailField,
                phoneField, addressField,
                currentPasswordField, newPasswordField, confirmNewPasswordField
        }) {
            c.setStyle("");
        }
    }
}