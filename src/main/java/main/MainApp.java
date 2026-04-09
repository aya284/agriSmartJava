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
        MyConnection.getInstance();
        Parent root = FXMLLoader.load(getClass().getResource("/Views/MainView.fxml"));
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
}
