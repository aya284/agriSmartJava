package controllers.user;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamResolution;
import entities.User;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.event.ActionEvent;
import services.UserService;
import utils.SessionManager;

import javax.imageio.ImageIO;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class VerifyFaceController {

    @FXML private ImageView cameraFeed;
    @FXML private Label statusLabel;
    @FXML private Label errorLabel;
    @FXML private Button captureBtn;

    private User pendingUser;
    private final UserService userService = new UserService();
    
    private Webcam webcam;
    private final AtomicBoolean stopCamera = new AtomicBoolean(false);

    static {
        // Helps release camera handle when the JVM is terminated (e.g. IDE stop button)
        Webcam.setHandleTermSignal(true);
    }

    public void setPendingUser(User user) {
        this.pendingUser = user;
    }

    @FXML
    public void initialize() {
        startWebcam();
        
        // Ensure camera stops if window is closed
        Platform.runLater(() -> {
            if (cameraFeed.getScene() != null && cameraFeed.getScene().getWindow() != null) {
                cameraFeed.getScene().getWindow().addEventHandler(javafx.stage.WindowEvent.WINDOW_CLOSE_REQUEST, e -> {
                    stopCamera.set(true);
                });
            }
        });
    }

    private void startWebcam() {
        new Thread(() -> {
            try {
                webcam = Webcam.getDefault();
                if (webcam != null) {
                    // 1. Disable the lock file mechanism entirely for this instance 
                    // to prevent stale locks from blocking the camera
                    webcam.getLock().disable();

                    // 2. If it's already open in this JVM session, close it first
                    if (webcam.isOpen()) {
                        System.out.println("[DEBUG] Webcam already open. Closing to reset.");
                        webcam.close();
                    }

                    Dimension size = WebcamResolution.QVGA.getSize();
                    webcam.setViewSize(size);
                    
                    // 2. Try to open with retries (in case of temporary lock)
                    int attempts = 0;
                    boolean opened = false;
                    while (attempts < 3 && !opened) {
                        try {
                            webcam.open();
                            opened = true;
                        } catch (Exception e) {
                            System.err.println("[DEBUG] Open attempt " + (attempts+1) + " failed: " + e.getMessage());
                            attempts++;
                            if (attempts < 3) Thread.sleep(500);
                            else throw e;
                        }
                    }
                    
                    Platform.runLater(() -> {
                        statusLabel.setText("Regardez la caméra...");
                        errorLabel.setVisible(false);
                    });

                    while (!stopCamera.get()) {
                        if (webcam != null && webcam.isOpen()) {
                            try {
                                BufferedImage image = webcam.getImage();
                                if (image != null) {
                                    WritableImage fxImage = SwingFXUtils.toFXImage(image, null);
                                    Platform.runLater(() -> cameraFeed.setImage(fxImage));
                                }
                            } catch (Exception e) {
                                System.err.println("[DEBUG] Error fetching image: " + e.getMessage());
                            }
                        }
                        Thread.sleep(50); // ~20 FPS
                    }
                } else {
                    Platform.runLater(() -> {
                        statusLabel.setText("Aucune caméra détectée.");
                        showError("Veuillez brancher une caméra.");
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> showError("Erreur caméra: " + e.getMessage()));
            } finally {
                if (webcam != null && webcam.isOpen()) {
                    webcam.close();
                    System.out.println("[DEBUG] Webcam released.");
                }
            }
        }).start();
    }

    @FXML
    private void handleCapture(ActionEvent event) {
        if (webcam == null || !webcam.isOpen()) return;

        statusLabel.setText("Vérification en cours...");
        captureBtn.setDisable(true);

        new Thread(() -> {
            try {
                // 1. Capture and Save to Temp File
                BufferedImage image = webcam.getImage();
                File tempFile = File.createTempFile("captured_face", ".jpg");
                ImageIO.write(image, "JPG", tempFile);
                
                String capturedPath = tempFile.getAbsolutePath();
                System.out.println("[DEBUG] Face captured at: " + capturedPath);

                // 2. Call the service (connects to FastAPI)
                User authenticatedUser = userService.verifyFaceLogin(pendingUser.getId(), capturedPath);
                
                // 3. Success -> UI Update
                Platform.runLater(() -> {
                    stopCamera.set(true);
                    SessionManager.getInstance().setCurrentUser(authenticatedUser);
                    try {
                        navigateToMain(event, authenticatedUser);
                    } catch (Exception e) {
                        showError("Erreur navigation: " + e.getMessage());
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    showError("Reconnaissance échouée: " + e.getMessage());
                    captureBtn.setDisable(false);
                    statusLabel.setText("Réessayez.");
                });
            }
        }).start();
    }

    @FXML
    private void handleBack(ActionEvent event) {
        stopCamera.set(true);
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
        String view = user.getRole().equalsIgnoreCase("admin") ? "/Views/Admin/AdminLayout.fxml" : "/Views/MainView.fxml";
        Parent root = FXMLLoader.load(getClass().getResource(view));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
    }
}
