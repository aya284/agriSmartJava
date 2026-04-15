package controllers.user;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import services.PasswordResetService;
import services.UserService;
import java.util.Optional;

public class ResetPasswordController {

    @FXML private TextField     tokenField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label         statusLabel;
    @FXML private Button        resetBtn;

    private final PasswordResetService passwordResetService = new PasswordResetService();
    private final UserService          userService          = new UserService();

    @FXML
    public void handleReset() {
        String token   = tokenField.getText().trim();
        String newPass = newPasswordField.getText();
        String confirm = confirmPasswordField.getText();

        if (token.isEmpty() || newPass.isEmpty() || confirm.isEmpty()) {
            showStatus("Tous les champs sont requis.", false); return;
        }
        if (!newPass.equals(confirm)) {
            showStatus("Les mots de passe ne correspondent pas.", false); return;
        }

        resetBtn.setDisable(true);
        resetBtn.setText("Réinitialisation…");

        new Thread(() -> {
            try {
                Optional<Integer> userIdOpt = passwordResetService.validateToken(token);

                if (userIdOpt.isEmpty()) {
                    Platform.runLater(() ->
                            showStatus("Jeton invalide ou expiré.", false));
                    return;
                }

                userService.resetPassword(userIdOpt.get(), newPass);
                passwordResetService.invalidateToken(token.split(":")[0]);

                Platform.runLater(() -> showStatus("Mot de passe mis à jour ! Redirection…", true));
                Thread.sleep(1500);
                Platform.runLater(() -> loadView("/Views/LoginView.fxml"));

            } catch (Exception e) {
                Platform.runLater(() ->
                        showStatus("Erreur : " + e.getMessage(), false));
            } finally {
                Platform.runLater(() -> {
                    resetBtn.setDisable(false);
                    resetBtn.setText("Réinitialiser");
                });
            }
        }).start();
    }

    @FXML public void goToLogin() { loadView("/Views/LoginView.fxml"); }

    private void showStatus(String msg, boolean success) {
        statusLabel.setStyle("-fx-text-fill:" + (success ? "#2ecc71" : "#e74c3c") + ";");
        statusLabel.setText(msg);
        statusLabel.setVisible(true);
    }

    private void loadView(String path) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(path));
            tokenField.getScene().setRoot(root);
        } catch (Exception e) {
            showStatus("Erreur navigation : " + e.getMessage(), false);
        }
    }
}