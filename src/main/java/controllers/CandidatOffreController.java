package controllers;

import entities.Offre;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import services.DemandeService;
import services.OffreService;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class CandidatOffreController implements Initializable {
    @FXML private FlowPane cardsContainer;
    @FXML private TextField searchField;
    @FXML private Label countLabel;

    // Detail View Components
    @FXML private Label detailTitle, detailDesc, detailLieu, detailSalaire, detailDebut, detailFin, dateLimitLabel;
    @FXML private Button btnPostuler;
    @FXML private ComboBox<String> statutFilter;
    private final OffreService service = new OffreService();
    public static Offre selectedOffreForView = null;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // 1. Initialize the ComboBox options
        if (statutFilter != null) {
            statutFilter.setItems(FXCollections.observableArrayList("Toutes les offres", "Ouvert", "Clôturée"));
            statutFilter.setValue("Toutes les offres");

            // Trigger search whenever the filter changes
            statutFilter.setOnAction(e -> handleSearch());
        }

        if (cardsContainer != null) {
            loadCandidateData();
        }
        if (detailTitle != null && selectedOffreForView != null) {
            setupDetailPage();
        }
    }

    private void loadCandidateData() {
        try {
            List<Offre> list = service.afficher();
            cardsContainer.getChildren().clear();

            // Show all offers fetched from DB for the candidate view.
            for (Offre o : list) {
                cardsContainer.getChildren().add(createCandidateCard(o));
            }

            countLabel.setText(list.size() + " offre(s) disponible(s) actuellement");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private VBox createCandidateCard(Offre o) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: white; -fx-padding: 20; -fx-background-radius: 10; -fx-pref-width: 300; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 0);");

        // 1. Dynamic Logic for Badge
        boolean isClosed = "Clôturée".equalsIgnoreCase(o.getStatut()) ||
                (o.getDate_fin() != null && java.time.LocalDateTime.now().isAfter(o.getDate_fin()));

        Label badge = new Label(isClosed ? "Clôturée" : "Ouvert");

        // Green for Ouvert (#27ae60), Grey for Clôturée (#95a5a6)
        String badgeStyle = isClosed
                ? "-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-padding: 2 10; -fx-background-radius: 10;"
                : "-fx-background-color: #27ae60; -fx-text-fill: white; -fx-padding: 2 10; -fx-background-radius: 10;";
        badge.setStyle(badgeStyle);

        // 2. Content Setup
        Label title = new Label(o.getTitle());
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 16;");

        Label loc = new Label("📍 " + o.getLieu());

        Label sal = new Label(o.getSalaire() + " TND\nmensuel");
        sal.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");

        Button btnVoir = new Button("👁 Voir");
        btnVoir.setStyle("-fx-background-color: #1a3323; -fx-text-fill: white; -fx-background-radius: 5; -fx-cursor: hand;");
        btnVoir.setMaxWidth(Double.MAX_VALUE);

        // 3. Navigation Logic
        btnVoir.setOnAction(e -> {
            selectedOffreForView = o;
            navigateToDetail();
        });

        card.getChildren().addAll(badge, title, loc, sal, btnVoir);
        return card;
    }

    private void setupDetailPage() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        detailTitle.setText(selectedOffreForView.getTitle());
        detailDesc.setText(selectedOffreForView.getDescription());
        detailLieu.setText(selectedOffreForView.getLieu());
        detailSalaire.setText(selectedOffreForView.getSalaire() + " TND mensuel");
        detailDebut.setText(selectedOffreForView.getDate_debut().format(formatter));

        // 1. Handle Date formatting
        if (selectedOffreForView.getDate_fin() != null) {
            String dateFinStr = selectedOffreForView.getDate_fin().format(formatter);
            detailFin.setText(dateFinStr);
            dateLimitLabel.setText(dateFinStr);
        }

        // 2. Combined Logic: Check Date AND Status
        boolean isExpired = selectedOffreForView.getDate_fin() != null &&
                LocalDateTime.now().isAfter(selectedOffreForView.getDate_fin());

        boolean isClosed = "Clôturée".equalsIgnoreCase(selectedOffreForView.getStatut());

        if (isExpired || isClosed) {
            btnPostuler.setDisable(true);

            // Update text based on why it's disabled
            if (isClosed) {
                btnPostuler.setText("Offre Clôturée");
            } else {
                btnPostuler.setText("Offre expirée");
            }

            // Style it to look disabled/grey
            btnPostuler.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-background-radius: 10;");
        } else {
            // Reset to default style if active
            btnPostuler.setDisable(false);
            btnPostuler.setText(" Postuler");
            btnPostuler.setStyle("-fx-background-color: #1a3323; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16; -fx-background-radius: 10;");
        }
    }
    // Inside CandidatOffreController.java
    // Inside your CandidatOffreController logic for handling the "Postuler" click:
    @FXML
    private void handlePostuler() {
        // We use the static variable we saved when navigating to this page
        Offre o = selectedOffreForView;

        if (o == null) return;

        try {
            DemandeService ds = new DemandeService();
            // Check if already applied (User ID 2)
            boolean exists = ds.afficher().stream()
                    .anyMatch(d -> d.getUsers_id() == 2 && d.getOffre_id() == o.getId().intValue());

            if (exists) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Déjà postulé");
                alert.setHeaderText(null);
                alert.setContentText("Vous avez déjà postulé pour cette offre.");
                alert.showAndWait();

                // Navigate to history instead
                navigateToView("/Views/Offres/MesCandidaturesList.fxml");
            } else {
                // Proceed to the application form
                PostulerController.currentOffreId = o.getId();
                navigateToView("/Views/Offres/PostulerForm.fxml");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Helper method to handle the view switching within this controller
    private void navigateToView(String fxmlPath) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            // Find the contentArea in the current scene to swap the view
            StackPane contentArea = (StackPane) btnPostuler.getScene().lookup("#contentArea");
            contentArea.getChildren().setAll(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @FXML
    public void showListPage() {
        selectedOffreForView = null;
        switchView("/Views/Offres/CandidatOffreList.fxml");
    }

    private void navigateToDetail() {
        switchView("/Views/Offres/OffreDetailView.fxml");
    }
// Inside CandidatOffreController.java

    @FXML
    public void handleSearch() {
        String query = searchField.getText() == null ? "" : searchField.getText().toLowerCase().trim();
        String selectedStatut = (statutFilter != null) ? statutFilter.getValue() : "Toutes les offres";

        try {
            List<Offre> allOffres = service.afficher();
            cardsContainer.getChildren().clear();

            List<Offre> filteredList = allOffres.stream()
                    // Rule 1: Filter by Search Text (Title or Lieu)
                    .filter(o -> {
                    String title = o.getTitle() == null ? "" : o.getTitle().toLowerCase();
                    String lieu = o.getLieu() == null ? "" : o.getLieu().toLowerCase();
                    return title.contains(query) || lieu.contains(query);
                    })

                    // Rule 2: Filter by ComboBox Statut (Ouvert vs Clôturée)
                    .filter(o -> {
                        if ("Toutes les offres".equals(selectedStatut)) return true;

                        // Logic to determine if an offer is "Ouvert" or "Clôturée"
                        boolean isExpired = o.getDate_fin() != null && LocalDateTime.now().isAfter(o.getDate_fin());
                        boolean isManuallyClosed = "Clôturée".equalsIgnoreCase(o.getStatut());

                        if ("Ouvert".equals(selectedStatut)) {
                            return !isExpired && !isManuallyClosed;
                        } else { // Clôturée
                            return isExpired || isManuallyClosed;
                        }
                    })
                    .toList();

            for (Offre o : filteredList) {
                cardsContainer.getChildren().add(createCandidateCard(o));
            }

            countLabel.setText(filteredList.size() + " offre(s) trouvée(s)");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    private void switchView(String path) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(path));

            // Find ANY node that is currently on the screen to grab the Scene
            // If we are on List page, use cardsContainer. If on Detail page, use detailTitle.
            javafx.scene.Node anchorNode = (cardsContainer != null) ? cardsContainer : detailTitle;

            if (anchorNode != null && anchorNode.getScene() != null) {
                StackPane contentArea = (StackPane) anchorNode.getScene().lookup("#contentArea");

                if (contentArea != null) {
                    contentArea.getChildren().setAll(root);
                } else {
                    // If you don't have a contentArea StackPane, replace the whole window
                    anchorNode.getScene().setRoot(root);
                }
            } else {
                System.err.println("Could not find a valid scene anchor to switch views.");
            }
        } catch (Exception e) {
            System.err.println("Error switching view to: " + path);
            e.printStackTrace();
        }
    }
}