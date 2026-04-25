package controllers.user;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import entities.User;
import netscape.javascript.JSObject;
import services.GoogleAuthService;
import services.RecaptchaService;
import services.UserService;
import utils.SessionManager;
import utils.Validator;

import java.util.Optional;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button loginBtn;
    @FXML private Button googleBtn;
    @FXML private CheckBox captchaCheckBox;
    @FXML private Label errCaptcha;

    private String recaptchaToken = null;

    private final UserService userService         = new UserService();
    private final GoogleAuthService googleAuthService = new GoogleAuthService();

    // ✅ Store as field so it's not garbage collected
    private RecaptchaBridge recaptchaBridge;
    private RecaptchaService recaptchaService;

    @FXML
    public void initialize() {
        // Initialize service but don't start server yet
        try {
            recaptchaService = new RecaptchaService();
        } catch (Exception e) {
            System.err.println("Failed to start reCAPTCHA service: " + e.getMessage());
        }
    }

    private Stage captchaStage;

    @FXML
    private void handleCaptchaClick() {
        if (captchaCheckBox.isSelected()) {
            if (recaptchaToken != null) return; // Already solved
            showCaptchaModal();
        } else {
            recaptchaToken = null; // Reset if unchecked
        }
    }

    private void showCaptchaModal() {
        if (captchaStage != null && captchaStage.isShowing()) return;

        WebView webView = new WebView();
        WebEngine engine = webView.getEngine();
        engine.setJavaScriptEnabled(true);

        recaptchaBridge = new RecaptchaBridge();

        engine.getLoadWorker().stateProperty().addListener((obs, old, state) -> {
            if (state == Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) engine.executeScript("window");
                window.setMember("javaConnector", recaptchaBridge);
            }
        });

        // Start server if not running
        recaptchaService.startServer();
        engine.load("http://localhost:" + RecaptchaService.PORT);

        captchaStage = new Stage();
        captchaStage.initModality(Modality.APPLICATION_MODAL);
        captchaStage.initOwner(captchaCheckBox.getScene().getWindow());
        captchaStage.setTitle("Vérification de sécurité");

        Scene scene = new Scene(webView, 360, 520);
        captchaStage.setScene(scene);

        captchaStage.setOnCloseRequest(e -> {
            if (recaptchaToken == null) {
                captchaCheckBox.setSelected(false);
            }
        });

        captchaStage.show();
    }

    public void cleanup() {
        if (recaptchaService != null) {
            recaptchaService.stopServer();
            System.out.println("reCAPTCHA server stopped.");
        }
    }

    // ── JS Bridge ─────────────────────────────────────────────
    public class RecaptchaBridge {
        public void onCaptchaCompleted(String token) {
            Platform.runLater(() -> {
                recaptchaToken = token;
                captchaCheckBox.setSelected(true);
                if (errCaptcha != null) errCaptcha.setVisible(false);
                if (captchaStage != null) captchaStage.close();
            });
        }

        public void onCaptchaExpired() {
            Platform.runLater(() -> {
                recaptchaToken = null;
                if (errCaptcha != null) {
                    errCaptcha.setText("⚠ Le captcha a expiré, veuillez réessayer.");
                    errCaptcha.setVisible(true);
                }
            });
        }
    }

    // ── Login ─────────────────────────────────────────────────
    @FXML
    public void handleLogin() {
        clearError();

        String email    = emailField.getText().trim();
        String password = passwordField.getText();

        String error = Validator.validateLoginForm(email, password);
        if (error != null) { showError(error); return; }

        // ✅ reCAPTCHA check
        if (recaptchaToken == null || recaptchaToken.isBlank()) {
            errCaptcha.setText("⚠ Veuillez compléter le captcha.");
            errCaptcha.setVisible(true);
            return;
        }

        loginBtn.setDisable(true);
        loginBtn.setText("Connexion en cours...");

        // ✅ Capture token before thread (avoid race condition)
        String tokenSnapshot = recaptchaToken;

        new Thread(() -> {
            try {
                if (!recaptchaService.verify(tokenSnapshot)) {
                    Platform.runLater(() -> {
                        showError("Échec de la vérification captcha. Réessayez.");
                        recaptchaToken = null;
                        captchaCheckBox.setSelected(false);
                    });
                    return;
                }

                Optional<User> result = userService.login(email, password);

                if (result.isEmpty()) {
                    Platform.runLater(() -> showError("Email ou mot de passe incorrect."));
                    return;
                }

                result.ifPresent(user -> Platform.runLater(() -> {
                    SessionManager.getInstance().setCurrentUser(user);
                    navigateByRole(user.getRole());
                }));

            } catch (Exception e) {
                Platform.runLater(() -> showError(e.getMessage()));
            } finally {
                Platform.runLater(() -> {
                    loginBtn.setDisable(false);
                    loginBtn.setText("Se connecter");
                });
            }
        }).start();
    }

    // ── Google Login ──────────────────────────────────────────
    @FXML
    public void handleGoogleLogin() {
        googleBtn.setDisable(true);
        googleBtn.setText("Connexion en cours...");

        new Thread(() -> {
            try {
                var userInfo = googleAuthService.authenticate();
                Optional<User> result = userService.loginWithGoogle(
                        userInfo.getId(),
                        userInfo.getEmail(),
                        userInfo.getGivenName(),
                        userInfo.getFamilyName());

                if (result.isEmpty()) {
                    Platform.runLater(() -> showError("Compte Google introuvable."));
                    return;
                }

                result.ifPresent(user -> Platform.runLater(() -> {
                    SessionManager.getInstance().setCurrentUser(user);
                    navigateByRole(user.getRole());
                }));

            } catch (Exception e) {
                Platform.runLater(() -> showError("Connexion Google échouée : " + e.getMessage()));
            } finally {
                Platform.runLater(() -> {
                    googleBtn.setDisable(false);
                    googleBtn.setText("Continuer avec Google");
                });
            }
        }).start();
    }

    // ── Navigation ────────────────────────────────────────────
    @FXML public void goToRegister()      { loadView("/Views/RegisterView.fxml"); }
    @FXML public void goToForgotPassword(){ loadView("/Views/ForgotPasswordView.fxml"); }

    private void navigateByRole(String role) {
        String view = switch (role.toLowerCase()) {
            case "admin"       -> "/Views/Admin/AdminLayout.fxml";
            case "employee"    -> "/Views/MainView.fxml";
            case "fournisseur" -> "/Views/MainView.fxml";
            default            -> "/Views/MainView.fxml";
        };
        loadView(view);
    }

    private void loadView(String fxmlPath) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            emailField.getScene().setRoot(root);
        } catch (Exception e) {
            showError("Erreur de navigation : " + e.getMessage());
        }
    }

    // ── Helpers ───────────────────────────────────────────────
    private void showError(String message) {
        errorLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12;");
        errorLabel.setText("⚠ " + message);
        errorLabel.setVisible(true);
    }

    private void clearError() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
        if (errCaptcha != null) errCaptcha.setVisible(false);
    }
}