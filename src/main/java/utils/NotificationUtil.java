package utils;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.util.Duration;

public class NotificationUtil {

    public static void showSuccess(Window owner, String message) {
        showToast(owner, "✅ " + message, "#27ae60", "#ebf7f0");
    }

    public static void showError(Window owner, String message) {
        showToast(owner, "❌ " + message, "#dc3545", "#fdf0f1");
    }

    public static void showDelete(Window owner, String message) {
        showToast(owner, "🗑 " + message, "#dc3545", "#fdf0f1");
    }

    public static void showInfo(Window owner, String message) {
        showToast(owner, "ℹ " + message, "#007bff", "#e6f2ff");
    }

    private static void showToast(Window owner, String message, String colorCode, String bgColor) {
        // Fallback s'il n'y a pas de fenêtre propriétaire transmise
        if (owner == null) {
            owner = Window.getWindows().stream().filter(Window::isShowing).findFirst().orElse(null);
        }
        if (owner == null) return;

        final Window targetWindow = owner;
        
        Platform.runLater(() -> {
            Popup popup = new Popup();
            popup.setAutoFix(true);
            popup.setAutoHide(true);

            Label lblInfo = new Label(message);
            lblInfo.setStyle("-fx-text-fill: " + colorCode + "; -fx-font-size: 15px; -fx-font-weight: bold;");
            
            HBox box = new HBox(lblInfo);
            box.setAlignment(Pos.CENTER);
            box.setStyle("-fx-background-color: " + bgColor + "; -fx-border-color: " + colorCode + "; -fx-border-width: 1.5; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 12 24; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 10, 0, 0, 5);");

            popup.getContent().add(box);
            
            // Apparaître en haut, centré
            popup.setOnShown(e -> {
                popup.setX(targetWindow.getX() + targetWindow.getWidth() / 2 - popup.getWidth() / 2);
                popup.setY(targetWindow.getY() + 40); // 40px depuis le haut de la fenêtre
            });

            popup.show(targetWindow);

            FadeTransition ft = new FadeTransition(Duration.millis(250), box);
            ft.setFromValue(0.0);
            ft.setToValue(1.0);
            ft.play();

            // Cacher après 3 secondes
            new Thread(() -> {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException ex) {}
                Platform.runLater(() -> {
                    FadeTransition fadeOut = new FadeTransition(Duration.millis(400), box);
                    fadeOut.setFromValue(1.0);
                    fadeOut.setToValue(0.0);
                    fadeOut.setOnFinished(e -> popup.hide());
                    fadeOut.play();
                });
            }).start();
        });
    }
}
