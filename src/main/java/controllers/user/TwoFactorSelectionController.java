package controllers.user;

import entities.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.event.ActionEvent;
import services.UserService;

public class TwoFactorSelectionController {

    private User pendingUser;
    private final UserService userService = new UserService();

    public void setPendingUser(User user) {
        this.pendingUser = user;
    }

    @FXML
    private void handleOtpSelection(ActionEvent event) {
        // Navigate to OTP view immediately (don't block UI waiting for email)
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Views/VerifyOtpView.fxml"));
            Parent root = loader.load();

            VerifyOtpController controller = loader.getController();
            controller.setPendingUser(pendingUser);

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        // Send OTP email in background thread so UI is never blocked
        new Thread(() -> {
            try {
                userService.requestOtp(pendingUser.getId());
                System.out.println("[DEBUG] OTP email sent successfully for user: " + pendingUser.getId());
            } catch (Exception e) {
                System.err.println("[ERROR] Failed to send OTP email: " + e.getMessage());
                e.printStackTrace();
            }
        }, "otp-email-sender").start();
    }

    @FXML
    private void handleFaceIdSelection(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Views/VerifyFaceView.fxml"));
            Parent root = loader.load();
            
            VerifyFaceController controller = loader.getController();
            controller.setPendingUser(pendingUser);

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/Views/LoginView.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
