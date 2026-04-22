package main;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import utils.MyConnection;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        MyConnection.getInstance();
        // On démarre par le layout de Login
        Parent root = FXMLLoader.load(getClass().getResource("/Views/Mainview.fxml"));
        Scene scene = new Scene(root, 1200, 750);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        primaryStage.setTitle("AgriSmart Desktop");
        if (getClass().getResource("/images/logo.png") != null) {
            primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/images/logo.png")));
        }
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(650);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}


