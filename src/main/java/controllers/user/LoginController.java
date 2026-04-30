package controllers.user;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.Scene;
import javafx.stage.Stage;
import entities.User;
import services.GoogleAuthService;
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

    private final UserService userService = new UserService();
    private final GoogleAuthService googleAuthService = new GoogleAuthService();

    @FXML
    public void initialize() {
    }

    // ── Login ─────────────────────────────────────────────────
    @FXML
    public void handleLogin() {
        clearError();

        String email    = emailField.getText().trim();
        String password = passwordField.getText();

        String error = Validator.validateLoginForm(email, password);
        if (error != null) { showError(error); return; }

        loginBtn.setDisable(true);
        loginBtn.setText("Connexion en cours...");

        new Thread(() -> {
            try {
                Optional<User> result = userService.login(email, password);

                if (result.isEmpty()) {
                    Platform.runLater(() -> showError("Email ou mot de passe incorrect."));
                    return;
                }

                result.ifPresent(user -> Platform.runLater(() -> {
                    SessionManager.getInstance().setCurrentUser(user);
                    navigateByRole(user.getRole());
                }));

            } catch (UserService.TwoFactorRequiredException e) {
                Platform.runLater(() -> navigateTo2faSelection(e.getUser()));
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

    private void navigateTo2faSelection(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Views/TwoFactorSelectionView.fxml"));
            Parent root = loader.load();
            
            TwoFactorSelectionController controller = loader.getController();
            controller.setPendingUser(user);
            
            emailField.getScene().setRoot(root);
        } catch (Exception e) {
            showError("Erreur 2FA : " + e.getMessage());
        }
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
    }
}