package controllers.user;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import entities.User;
import netscape.javascript.JSObject;
import services.GoogleAuthService;
import services.RecaptchaService;
import services.UserService;
import utils.SessionManager;
import utils.Validator;

import java.util.Optional;

public class LoginController {

    @FXML
    private TextField emailField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label errorLabel;
    @FXML
    private Button loginBtn;
    @FXML
    private Button googleBtn;

    // ── reCAPTCHA ─────────────────────────────────────────────
    @FXML
    private WebView recaptchaWebView;
    @FXML
    private Label errCaptcha;

    private String recaptchaToken = null;

    private final UserService userService = new UserService();
    private final GoogleAuthService googleAuthService = new GoogleAuthService();
    private final RecaptchaService recaptchaService = new RecaptchaService();

    @FXML
    public void initialize() {
        setupRecaptcha();
    }

    // ── reCAPTCHA Setup ───────────────────────────────────────
    private void setupRecaptcha() {
        WebEngine engine = recaptchaWebView.getEngine();
        engine.setJavaScriptEnabled(true);

        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) engine.executeScript("window");
                window.setMember("javaConnector", new RecaptchaBridge());
            }
        });

        engine.loadContent(RecaptchaService.getRecaptchaHtml());
    }

    /**
     * JavaScript bridge to receive callbacks from the reCAPTCHA widget.
     */
    public class RecaptchaBridge {
        public void onCaptchaCompleted(String token) {
            Platform.runLater(() -> {
                recaptchaToken = token;
                if (errCaptcha != null) {
                    errCaptcha.setVisible(false);
                }
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

    // ── Login classique ───────────────────────────────────────
    @FXML
    public void handleLogin() {
        clearError();

        String email = emailField.getText().trim();
        String password = passwordField.getText();

        // Validation avant appel au service
        String error = Validator.validateLoginForm(email, password);
        if (error != null) {
            showError(error);
            return;
        }

        // Validate reCAPTCHA
        if (recaptchaToken == null || recaptchaToken.isBlank()) {
            if (errCaptcha != null) {
                errCaptcha.setText("⚠ Veuillez compléter le captcha.");
                errCaptcha.setVisible(true);
            }
            return;
        }

        loginBtn.setDisable(true);
        loginBtn.setText("Connexion en cours...");

        new Thread(() -> {
            try {
                // Server-side reCAPTCHA verification
                if (!recaptchaService.verify(recaptchaToken)) {
                    Platform.runLater(() -> {
                        showError("Échec de la vérification captcha. Réessayez.");
                        loginBtn.setDisable(false);
                        loginBtn.setText("Se connecter");
                        recaptchaToken = null;
                        recaptchaWebView.getEngine().loadContent(RecaptchaService.getRecaptchaHtml());
                    });
                    return;
                }

                Optional<User> result = userService.login(email, password);
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

    // ── Login Google ──────────────────────────────────────────
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

    @FXML
    public void goToForgotPassword() {
        loadView("/Views/ForgotPasswordView.fxml");
    }

    // ── Navigation ────────────────────────────────────────────
    @FXML
    public void goToRegister() {
        loadView("/Views/RegisterView.fxml");
    }

    private void navigateByRole(String role) {
        String view = switch (role.toLowerCase()) {
            case "admin" -> "/Views/Admin/AdminLayout.fxml";
            case "employee" -> "/Views/MainView.fxml";
            case "fournisseur" -> "/Views/MainView.fxml";
            default -> "/Views/MainView.fxml";
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
        if (errCaptcha != null) {
            errCaptcha.setVisible(false);
        }
    }
}