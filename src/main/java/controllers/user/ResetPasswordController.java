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

    @FXML private TextField otp1, otp2, otp3, otp4, otp5, otp6;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label         statusLabel;
    @FXML private Button        resetBtn;

    private final PasswordResetService passwordResetService = new PasswordResetService();
    private final UserService          userService          = new UserService();

    @FXML
    public void initialize() {
        setupOtpNavigation();
    }

    private void setupOtpNavigation() {
        TextField[] boxes = {otp1, otp2, otp3, otp4, otp5, otp6};
        for (int i = 0; i < boxes.length; i++) {
            final int index = i;
            boxes[i].textProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal.length() > 1) {
                    boxes[index].setText(newVal.substring(0, 1));
                }
                if (newVal.length() == 1 && index < boxes.length - 1) {
                    boxes[index + 1].requestFocus();
                }
            });

            boxes[i].setOnKeyPressed(event -> {
                if (event.getCode().toString().equals("BACK_SPACE") && boxes[index].getText().isEmpty() && index > 0) {
                    boxes[index - 1].requestFocus();
                }
            });
        }
    }

    @FXML
    public void handleReset() {
        String otp = otp1.getText() + otp2.getText() + otp3.getText() + 
                     otp4.getText() + otp5.getText() + otp6.getText();
        String newPass = newPasswordField.getText();
        String confirm = confirmPasswordField.getText();

        if (otp.isEmpty() || newPass.isEmpty() || confirm.isEmpty()) {
            showStatus("Tous les champs sont requis.", false); return;
        }
        if (!newPass.equals(confirm)) {
            showStatus("Les mots de passe ne correspondent pas.", false); return;
        }

        resetBtn.setDisable(true);
        resetBtn.setText("Réinitialisation…");

        new Thread(() -> {
            try {
                Optional<Integer> userIdOpt = passwordResetService.validateOTP(otp);

                if (userIdOpt.isEmpty()) {
                    Platform.runLater(() ->
                            showStatus("Code OTP invalide ou expiré.", false));
                    return;
                }

                userService.resetPassword(userIdOpt.get(), newPass);
                passwordResetService.invalidateOTP(userIdOpt.get());

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
            otp1.getScene().setRoot(root);
        } catch (Exception e) {
            showStatus("Erreur navigation : " + e.getMessage(), false);
        }
    }
}