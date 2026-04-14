package main;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import utils.MyConnection;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        applyPrimerLightTheme();
        MyConnection.getInstance();
        Parent root = FXMLLoader.load(getClass().getResource("/Views/LoginView.fxml"));
        Scene scene = new Scene(root, 1200, 750);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        primaryStage.setTitle("AgriSmart Desktop");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(650);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

    private void applyPrimerLightTheme() {
        try {
            Class<?> primerClass = Class.forName("atlantafx.base.theme.PrimerLight");
            Object primer = primerClass.getDeclaredConstructor().newInstance();
            String stylesheet = (String) primerClass.getMethod("getUserAgentStylesheet").invoke(primer);
            Application.setUserAgentStylesheet(stylesheet);
        } catch (ReflectiveOperationException ignored) {
            // Falls back to local stylesheet when AtlantaFX is not on classpath.
        }
    }
}
