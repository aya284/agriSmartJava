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
import javafx.scene.shape.SVGPath;
import javafx.scene.paint.Color;

import java.util.Optional;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordVisibleField;
    @FXML private Button togglePasswordBtn;
    @FXML private Label errorLabel;
    @FXML private Button loginBtn;
    @FXML private Button googleBtn;

    private final UserService userService = new UserService();
    private final GoogleAuthService googleAuthService = new GoogleAuthService();

    @FXML
    public void initialize() {
        passwordVisibleField.textProperty().bindBidirectional(passwordField.textProperty());
        updatePasswordIcon(false);
    }

    @FXML
    public void togglePasswordVisibility() {
        boolean isVisible = passwordField.isVisible();
        passwordField.setVisible(!isVisible);
        passwordVisibleField.setVisible(isVisible);
        updatePasswordIcon(isVisible);
    }

    private void updatePasswordIcon(boolean isVisible) {
        SVGPath svg = new SVGPath();
        if (isVisible) {  
          svg.setContent("M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z");

        } else {
            
            svg.setContent("M12 7c2.76 0 5 2.24 5 5 0 .65-.13 1.26-.36 1.83l2.92 2.92c1.51-1.26 2.7-2.89 3.43-4.75-1.73-4.39-6-7.5-11-7.5-1.4 0-2.74.25-3.98.7l2.16 2.16C10.74 7.13 11.35 7 12 7zM2 4.27l2.28 2.28.46.46C3.08 8.3 1.78 10 1 12c1.73 4.39 6 7.5 11 7.5 1.55 0 3.03-.3 4.38-.84l.42.42L19.73 22 21 20.73 3.27 3 2 4.27zM7.53 9.8l1.55 1.55c-.05.21-.08.43-.08.65 0 1.66 1.34 3 3 3 .22 0 .44-.03.65-.08l1.55 1.55c-.67.33-1.41.53-2.2.53-2.76 0-5-2.24-5-5 0-.79.2-1.53.53-2.2zm4.31-.78l3.15 3.15.02-.16c0-1.66-1.34-3-3-3l-.17.01z");
}
        svg.setFill(Color.web("#6b7280"));
        togglePasswordBtn.setGraphic(svg);
        togglePasswordBtn.setText("");
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