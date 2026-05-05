package controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import services.NewsService;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class NewsController implements Initializable {

    @FXML
    private ListView<NewsService.Article> newsListView;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurerCellules();
        chargerActualites();
    }

    private void configurerCellules() {
        newsListView.setCellFactory(param -> new ListCell<>() {
            private final ImageView imageView = new ImageView();
            private final Label titleLabel = new Label();
            private final HBox layout = new HBox(15, imageView, titleLabel);

            {
                imageView.setFitWidth(100);
                imageView.setFitHeight(70);
                imageView.setPreserveRatio(true);
                titleLabel.setWrapText(true);
                titleLabel.setMaxWidth(500);
                titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #2c3e50;");
                layout.setAlignment(Pos.CENTER_LEFT);
                layout.setPadding(new Insets(10));
            }

            @Override
            protected void updateItem(NewsService.Article item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    titleLabel.setText(item.title);
                    if (item.imageUrl != null && !item.imageUrl.isEmpty()) {
                        imageView.setImage(new Image(item.imageUrl, true));
                    } else {
                        imageView.setImage(null);
                    }
                    setGraphic(layout);
                }
            }
        });
    }

    private void chargerActualites() {
        new Thread(() -> {
            try {
                List<NewsService.Article> articles = NewsService.getLatestAgricultureNews();
                Platform.runLater(() -> {
                    newsListView.getItems().clear();
                    if (articles.isEmpty()) {
                        System.out.println("Aucune news reçue.");
                    } else {
                        newsListView.getItems().addAll(articles);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    private void handleRetour() {
        String[] cheminsPossibles = {
                "/Offres/CandidatOffreList.fxml",   // Chemin 1 : Dossier Offres avec Majuscule
                "/offres/CandidatOffreList.fxml",   // Chemin 2 : Dossier offres en minuscule
                "/Views/Offres/CandidatOffreList.fxml" // Chemin 3 : Si c'est dans un dossier Views
        };

        URL fxmlLocation = null;
        for (String chemin : cheminsPossibles) {
            fxmlLocation = getClass().getResource(chemin);
            if (fxmlLocation != null) {
                System.out.println("✅ Fichier trouvé ici : " + chemin);
                break;
            }
        }

        if (fxmlLocation == null) {
            System.err.println("❌ ERREUR : Impossible de trouver CandidatOffreList.fxml. Vérifie l'orthographe du dossier dans src/main/resources");
            return;
        }

        try {
            Parent root = FXMLLoader.load(fxmlLocation);
            StackPane area = (StackPane) newsListView.getScene().lookup("#contentArea");

            if (area != null) {
                area.getChildren().setAll(root);
            } else {
                newsListView.getScene().setRoot(root);
            }
        } catch (IOException e) {
            System.err.println("Erreur lors du chargement du FXML : " + e.getMessage());
            e.printStackTrace();
        }
    }
}