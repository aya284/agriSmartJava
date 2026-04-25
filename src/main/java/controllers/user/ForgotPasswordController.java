package controllers.user;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import services.EmailService;
import services.PasswordResetService;
import services.UserService;
import entities.User;
import java.util.Optional;

public class ForgotPasswordController {

    @FXML private TextField emailField;
    @FXML private Label     statusLabel;
    @FXML private Button    sendBtn;

    private final UserService          userService          = new UserService();
    private final PasswordResetService passwordResetService = new PasswordResetService();
    private final EmailService         emailService         = new EmailService();

    @FXML
    public void handleSendReset() {
        String email = emailField.getText().trim();
        if (email.isEmpty()) {
            showStatus("Veuillez saisir votre adresse e-mail.", false);
            return;
        }

        sendBtn.setDisable(true);
        sendBtn.setText("Envoi en cours…");

        new Thread(() -> {
            try {
                // Always show same message to prevent email enumeration
                Optional<User> userOpt = userService.findByEmail(email);
                if (userOpt.isPresent()) {
                    String otp = passwordResetService.createResetOTP(userOpt.get().getId());
                    emailService.sendPasswordResetEmail(email, otp);
                }
                Platform.runLater(() ->
                        showStatus("Si cet e-mail existe, un code OTP a été envoyé.", true));
            } catch (Exception e) {
                Platform.runLater(() ->
                        showStatus("Erreur : " + e.getMessage(), false));
            } finally {
                Platform.runLater(() -> {
                    sendBtn.setDisable(false);
                    sendBtn.setText("Envoyer");
                });
            }
        }).start();
    }

    @FXML public void goToLogin()     { loadView("/Views/LoginView.fxml"); }
    @FXML public void goToResetForm() { loadView("/Views/ResetPasswordView.fxml"); }

    private void showStatus(String msg, boolean success) {
        statusLabel.setStyle("-fx-text-fill:" + (success ? "#2ecc71" : "#e74c3c") + ";");
        statusLabel.setText(msg);
        statusLabel.setVisible(true);
    }

    private void loadView(String path) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(path));
            emailField.getScene().setRoot(root);
        } catch (Exception e) {
            showStatus("Erreur navigation : " + e.getMessage(), false);
        }
    }
}