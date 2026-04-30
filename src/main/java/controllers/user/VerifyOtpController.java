package controllers.user;

import entities.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.event.ActionEvent;
import services.UserService;
import utils.SessionManager;

public class VerifyOtpController {

    @FXML private TextField otpField;
    @FXML private Label errorLabel;
    @FXML private Label emailLabel;

    private User pendingUser;
    private final UserService userService = new UserService();

    public void setPendingUser(User user) {
        this.pendingUser = user;
        if (user.getEmail() != null) {
            String masked = user.getEmail().replaceAll("(^.{3})(.*)(@.*$)", "$1***$3");
            emailLabel.setText("Un code a été envoyé à : " + masked);
        }
    }

    @FXML
    private void handleVerify(ActionEvent event) {
        String code = otpField.getText().trim();
        if (code.isEmpty()) {
            showError("Veuillez entrer le code.");
            return;
        }

        try {
            User authenticatedUser = userService.finalize2faLogin(pendingUser.getId(), code);
            
            // Login Success
            SessionManager.getInstance().setCurrentUser(authenticatedUser);
            navigateToMain(event, authenticatedUser);
            
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void handleResend() {
        try {
            userService.requestOtp(pendingUser.getId());
            showError("Nouveau code envoyé !");
            errorLabel.setStyle("-fx-text-fill: #16a34a;"); // Green for success
        } catch (Exception e) {
            showError("Erreur lors de l'envoi.");
        }
    }

    @FXML
    private void handleBack(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Views/TwoFactorSelectionView.fxml"));
            Parent root = loader.load();
            TwoFactorSelectionController controller = loader.getController();
            controller.setPendingUser(pendingUser);

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void navigateToMain(ActionEvent event, User user) throws Exception {
        String view = user.getRole().equals("admin") ? "/Views/Admin/AdminLayout.fxml" : "/Views/MainView.fxml";
        Parent root = FXMLLoader.load(getClass().getResource(view));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setStyle("-fx-text-fill: #e11d48;");
    }
}
