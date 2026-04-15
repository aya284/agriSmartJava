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
    @FXML private TextField       firstNameField;
    @FXML private TextField       lastNameField;
    @FXML private TextField       emailField;
    @FXML private PasswordField   passwordField;
    @FXML private PasswordField   confirmPasswordField;
    @FXML private TextField       phoneField;
    @FXML private TextField       addressField;
    @FXML private ComboBox<String> roleComboBox;

    // ── Fichiers ──────────────────────────────────────────────
    @FXML private Label     documentFileLabel;
    @FXML private Label     imageFileLabel;
    @FXML private ImageView profilePreview;

    // ── Labels d'erreur inline ────────────────────────────────
    @FXML private Label errFirstName;
    @FXML private Label errLastName;
    @FXML private Label errEmail;
    @FXML private Label errRole;
    @FXML private Label errPassword;
    @FXML private Label errConfirm;
    @FXML private Label errPhone;
    @FXML private Label errAddress;
    @FXML private Label errImage;
    @FXML private Label errDocument;

    // ── Feedback global ───────────────────────────────────────
    @FXML private Label  successLabel;
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
        setupRealtimeValidation();
    }

    // ── Validation en temps réel à la perte du focus ──────────
    private void setupRealtimeValidation() {
        firstNameField.focusedProperty().addListener((o, was, focused) -> {
            if (!focused) showInline(errFirstName,
                    Validator.validateFirstName(firstNameField.getText().trim()));
        });
        lastNameField.focusedProperty().addListener((o, was, focused) -> {
            if (!focused) showInline(errLastName,
                    Validator.validateLastName(lastNameField.getText().trim()));
        });
        emailField.focusedProperty().addListener((o, was, focused) -> {
            if (!focused) showInline(errEmail,
                    Validator.validateEmail(emailField.getText().trim()));
        });
        passwordField.focusedProperty().addListener((o, was, focused) -> {
            if (!focused) showInline(errPassword,
                    Validator.validatePassword(passwordField.getText()));
        });
        confirmPasswordField.focusedProperty().addListener((o, was, focused) -> {
            if (!focused) showInline(errConfirm,
                    Validator.validateConfirmPassword(
                            passwordField.getText(),
                            confirmPasswordField.getText()));
        });
        phoneField.focusedProperty().addListener((o, was, focused) -> {
            if (!focused) showInline(errPhone,
                    Validator.validatePhone(phoneField.getText().trim()));
        });
        addressField.focusedProperty().addListener((o, was, focused) -> {
            if (!focused) showInline(errAddress,
                    Validator.validateAddress(addressField.getText().trim()));
        });
    }

    // ── Sélection document ────────────────────────────────────
    @FXML
    public void handleChooseDocument() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir un document justificatif");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF, JPEG, PNG",
                        "*.pdf", "*.jpg", "*.jpeg", "*.png"));
        File file = chooser.showOpenDialog(registerBtn.getScene().getWindow());
        if (file != null) {
            if (file.length() > 5 * 1024 * 1024) {
                showInline(errDocument, "Le document ne doit pas dépasser 5 Mo.");
                return;
            }
            selectedDocument = file;
            documentFileLabel.setText(file.getName());
            clearInline(errDocument);
        }
    }

    // ── Sélection image ───────────────────────────────────────
    @FXML
    public void handleChooseImage() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir une photo de profil");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JPEG, PNG",
                        "*.jpg", "*.jpeg", "*.png"));
        File file = chooser.showOpenDialog(registerBtn.getScene().getWindow());
        if (file != null) {
            if (file.length() > 5 * 1024 * 1024) {
                showInline(errImage, "L'image ne doit pas dépasser 5 Mo.");
                return;
            }
            selectedImage = file;
            imageFileLabel.setText(file.getName());
            profilePreview.setImage(new Image(file.toURI().toString()));
            clearInline(errImage);
        }
    }

    // ── Inscription ───────────────────────────────────────────
    @FXML
    public void handleRegister() {
        clearAllErrors();

        // Valider tous les champs et afficher les erreurs inline
        boolean hasError = false;

        String e1 = Validator.validateFirstName(firstNameField.getText().trim());
        String e2 = Validator.validateLastName(lastNameField.getText().trim());
        String e3 = Validator.validateEmail(emailField.getText().trim());
        String e4 = Validator.validateRole(roleComboBox.getValue());
        String e5 = Validator.validatePassword(passwordField.getText());
        String e6 = Validator.validateConfirmPassword(
                passwordField.getText(), confirmPasswordField.getText());
        String e7 = Validator.validatePhone(phoneField.getText().trim());
        String e8 = Validator.validateAddress(addressField.getText().trim());

        if (e1 != null) { showInline(errFirstName, e1); hasError = true; }
        if (e2 != null) { showInline(errLastName,  e2); hasError = true; }
        if (e3 != null) { showInline(errEmail,     e3); hasError = true; }
        if (e4 != null) { showInline(errRole,      e4); hasError = true; }
        if (e5 != null) { showInline(errPassword,  e5); hasError = true; }
        if (e6 != null) { showInline(errConfirm,   e6); hasError = true; }
        if (e7 != null) { showInline(errPhone,     e7); hasError = true; }
        if (e8 != null) { showInline(errAddress,   e8); hasError = true; }

        if (hasError) return;

        registerBtn.setDisable(true);
        registerBtn.setText("Création du compte...");

        new Thread(() -> {
            try {
                String documentPath = FileStorageUtils.save(selectedDocument, "documents");
                String imagePath    = FileStorageUtils.save(selectedImage,    "profiles");

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
                    successLabel.setText("✔ Compte créé avec succès ! Redirection...");
                    successLabel.setVisible(true);
                    goToLogin();
                });

            } catch (Exception e) {
                // Erreur serveur (ex: email déjà utilisé) → sous le champ email
                Platform.runLater(() -> {
                    String msg = e.getMessage();
                    if (msg != null && msg.contains("e-mail"))
                        showInline(errEmail, msg);
                    else
                        showInline(errEmail, msg); // fallback visible
                });
            } finally {
                Platform.runLater(() -> {
                    registerBtn.setDisable(false);
                    registerBtn.setText("Créer le compte");
                });
            }
        }).start();
    }

    // ── Navigation ────────────────────────────────────────────
    @FXML
    public void goToLogin() {
        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource("/Views/LoginView.fxml"));
            firstNameField.getScene().setRoot(root);
        } catch (Exception e) {
            showInline(errEmail, "Erreur de navigation : " + e.getMessage());
        }
    }

    // ── Helpers erreurs inline ────────────────────────────────
    private void showInline(Label label, String message) {
        if (message != null) {
            label.setText("⚠ " + message);
            label.setVisible(true);
            // Bordure rouge sur le champ associé
            highlightField(label, true);
        } else {
            clearInline(label);
        }
    }

    private void clearInline(Label label) {
        label.setText("");
        label.setVisible(false);
        highlightField(label, false);
    }

    private void clearAllErrors() {
        clearInline(errFirstName); clearInline(errLastName);
        clearInline(errEmail);     clearInline(errRole);
        clearInline(errPassword);  clearInline(errConfirm);
        clearInline(errPhone);     clearInline(errAddress);
        clearInline(errImage);     clearInline(errDocument);
        successLabel.setVisible(false);
    }

    // Bordure rouge/normale sur le champ au-dessus du label d'erreur
    private void highlightField(Label errLabel, boolean hasError) {
        String border = hasError
                ? "-fx-border-color:#e74c3c; -fx-border-radius:4; -fx-border-width:1.5;"
                : "";
        if      (errLabel == errFirstName) firstNameField.setStyle(border);
        else if (errLabel == errLastName)  lastNameField.setStyle(border);
        else if (errLabel == errEmail)     emailField.setStyle(border);
        else if (errLabel == errRole)      roleComboBox.setStyle(border);
        else if (errLabel == errPassword)  passwordField.setStyle(border);
        else if (errLabel == errConfirm)   confirmPasswordField.setStyle(border);
        else if (errLabel == errPhone)     phoneField.setStyle(border);
        else if (errLabel == errAddress)   addressField.setStyle(border);
    }
}
