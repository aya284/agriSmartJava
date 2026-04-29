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
import models.VerificationResult;
import services.UserService;
import services.DocumentVerificationService;
import services.GoogleAuthService;
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
    @FXML private TextField       cinField;
    @FXML private ComboBox<String> roleComboBox;

    // ── Fichiers ──────────────────────────────────────────────
    @FXML private Label     documentFileLabel;
    @FXML private Label     imageFileLabel;
    @FXML private ImageView profilePreview;
    @FXML private ImageView logoImage;

    // ── Labels d'erreur inline ────────────────────────────────
    @FXML private Label errFirstName;
    @FXML private Label errLastName;
    @FXML private Label errEmail;
    @FXML private Label errRole;
    @FXML private Label errPassword;
    @FXML private Label errConfirm;
    @FXML private Label errPhone;
    @FXML private Label errAddress;
    @FXML private Label errCin;
    @FXML private Label errImage;
    @FXML private Label errDocument;

    // ── Feedback global ───────────────────────────────────────
    @FXML private Label  successLabel;
    @FXML private Button registerBtn;

    // ── État interne ──────────────────────────────────────────
    private File selectedDocument;
    private File selectedImage;

    private final UserService       userService       = new UserService();
    private final GoogleAuthService googleAuthService = new GoogleAuthService();

    // ─────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        roleComboBox.getItems().addAll("agriculteur", "fournisseur", "employee");
        roleComboBox.setValue("agriculteur");
        setupRealtimeValidation();
    }

    // ── Validation en temps réel ──────────────────────────────
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
                new FileChooser.ExtensionFilter("JPEG, PNG",
                        "*.jpg", "*.jpeg", "*.png"));
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

        // Document CIN obligatoire
        if (selectedDocument == null) {
            showInline(errDocument, "Veuillez joindre une photo de votre CIN.");
            hasError = true;
        }

        if (hasError) return;

        registerBtn.setDisable(true);
        registerBtn.setText("Création du compte...");

        // ✅ Extract all UI data on the FX thread before starting background work
        final String firstName = firstNameField.getText().trim();
        final String lastName = lastNameField.getText().trim();
        final String email = emailField.getText().trim();
        final String role = roleComboBox.getValue();
        final String password = passwordField.getText();
        final String phone = phoneField.getText().trim();
        final String address = addressField.getText().trim();
        final String cin = cinField.getText().trim();
        final File docFile = selectedDocument;
        final File imgFile = selectedImage;

        new Thread(() -> {
            try {
                // ── Step 1: Save files ────────────────────────────────
                System.out.println("\n" + "=".repeat(70));
                System.out.println("🔵 STEP 1: Saving files...");
                System.out.println("=".repeat(70));

                String documentPath = FileStorageUtils.save(docFile, "documents");
                String imagePath    = FileStorageUtils.save(imgFile, "profiles");

                System.out.println("🟢 STEP 1 COMPLETE ✓");

                // ── Step 2: Create User object ───────────────────────
                System.out.println("\n🔵 STEP 2: Creating User object...");

                User user = new User();
                user.setFirstName(firstName);
                user.setLastName(lastName);
                user.setEmail(email);
                user.setPassword(password);
                user.setRole(role);
                user.setPhone(phone);
                user.setAddress(address);
                user.setCinNumber(cin);
                user.setDocumentFile(documentPath);
                user.setImage(imagePath);
                user.setStatus("pending");

                System.out.println("   - UserService class: " + userService.getClass().getName());

                userService.register(user);

                System.out.println("🟢 STEP 4 COMPLETE ✓");
                System.out.println("   - User ID (after DB save): " + user.getId());
                System.out.println("   - User email: " + user.getEmail());
                System.out.println("   - User status: " + user.getStatus());
                System.out.println("   - User role: " + user.getRole());

                System.out.println("\n✅ USER REGISTERED SUCCESSFULLY");
                System.out.println("=".repeat(70) + "\n");

                // ── Notify user immediately ───────────────────
                Platform.runLater(() -> {
                    successLabel.setText(
                            "✔ Compte créé ! Vérification de votre CIN en cours..." +
                                    " Vous recevrez un email sous peu."
                    );
                    successLabel.setVisible(true);
                });

                // ── Fire OCR + AI in separate thread ──────────
                final User   savedUser = user;
                final String docPath   = documentPath;

                new Thread(() -> {
                    try {
                        System.out.println("\n" + "=".repeat(70));
                        System.out.println("🔵 VERIFICATION PROCESS STARTED (background thread)");
                        System.out.println("=".repeat(70));

                        // ✅ Convert relative path → absolute for Tesseract
                        String absoluteDocPath =
                                FileStorageUtils.getAbsolutePath(docPath);
                        System.out.println("   - Relative path: " + docPath);
                        System.out.println("   - Absolute path: " + absoluteDocPath);

                        DocumentVerificationService verificationService =
                                new DocumentVerificationService();

                        System.out.println("   - Starting document verification...");

                        VerificationResult result =
                                verificationService.processDocument(
                                        savedUser, absoluteDocPath);

                        System.out.println("🟢 VERIFICATION COMPLETE");
                        System.out.println("   - Decision: " + result.getDecision());
                        System.out.println("   - Reason: " + result.getReason());
                        System.out.println("   - Extracted CIN: " + result.getExtractedCin());
                        System.out.println("   - Quality Score: " + result.getQualityScore() + "%");
                        System.out.println("=".repeat(70) + "\n");

                    } catch (Exception ex) {
                        System.err.println("\n" + "=".repeat(70));
                        System.err.println(" VERIFICATION ERROR");
                        System.err.println("=".repeat(70));
                        System.err.println("   - User: " + savedUser.getEmail());
                        System.err.println("   - Exception type: " + ex.getClass().getName());
                        System.err.println("   - Message: " + ex.getMessage());
                        System.err.println("   - Status: User remains 'pending' for manual review");
                        System.err.println("=".repeat(70) + "\n");
                        ex.printStackTrace();
                        // User stays as pending — admin will review manually
                    }
                }).start();

                // ── Navigate to login after short delay ───────
                Thread.sleep(2000);
                Platform.runLater(this::goToLogin);

            } catch (Exception e) {
                System.err.println("\n" + "=".repeat(70));
                System.err.println(" REGISTRATION ERROR");
                System.err.println("=".repeat(70));
                System.err.println("   - Exception type: " + e.getClass().getName());
                System.err.println("   - Message: " + e.getMessage());
                System.err.println("=".repeat(70) + "\n");
                e.printStackTrace();

                Platform.runLater(() -> {
                    String msg = e.getMessage();
                    if (msg != null && msg.contains("e-mail"))
                        showInline(errEmail, msg);
                    else
                        showInline(errEmail, msg != null
                                ? msg : "Erreur inconnue.");
                });
            } finally {
                Platform.runLater(() -> {
                    registerBtn.setDisable(false);
                    registerBtn.setText("Créer le compte");
                });
            }
        }).start();
    }

    // ── Google Sign-up / Login ────────────────────────────────
    @FXML
    public void handleGoogleLogin() {
        registerBtn.setDisable(true);
        new Thread(() -> {
            try {
                var userInfo = googleAuthService.authenticate();
                userService.loginWithGoogle(
                                userInfo.getId(),
                                userInfo.getEmail(),
                                userInfo.getGivenName(),
                                userInfo.getFamilyName())
                        .ifPresent(user -> Platform.runLater(() -> {
                            utils.SessionManager.getInstance().setCurrentUser(user);
                            goToLogin();
                        }));
            } catch (Exception e) {
                Platform.runLater(() ->
                        showInline(errEmail, "Google : " + e.getMessage()));
            } finally {
                Platform.runLater(() -> registerBtn.setDisable(false));
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
        clearInline(errFirstName);
        clearInline(errLastName);
        clearInline(errEmail);
        clearInline(errRole);
        clearInline(errPassword);
        clearInline(errConfirm);
        clearInline(errPhone);
        clearInline(errAddress);
        clearInline(errImage);
        clearInline(errDocument);
        successLabel.setVisible(false);
    }

    private void highlightField(Label errLabel, boolean hasError) {
        String border = hasError
                ? "-fx-border-color:#e74c3c;-fx-border-radius:4;-fx-border-width:1.5;"
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