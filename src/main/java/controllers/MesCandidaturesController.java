package controllers;

import entities.Demande;
import entities.Offre;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import services.DemandeService;
import services.OffreService;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class MesCandidaturesController implements Initializable {
    @FXML private VBox listContainer;
    @FXML private Label statAcceptee, statEnCours, statRefusee;

    private final DemandeService demandeService = new DemandeService();
    private final OffreService offreService = new OffreService();
    private final int STATIC_USER_ID = 2;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        refreshList();
    }

    private void refreshList() {
        try {
            listContainer.getChildren().clear();
            List<Demande> allDemandes = demandeService.afficher();

            List<Demande> userDemandes = allDemandes.stream()
                    .filter(d -> d.getUsers_id() == STATIC_USER_ID)
                    .collect(Collectors.toList());

            // Update Stats
            long countAcceptee = userDemandes.stream().filter(d -> "Acceptée".equalsIgnoreCase(d.getStatut())).count();
            long countEnCours = userDemandes.stream().filter(d -> "En cours".equalsIgnoreCase(d.getStatut())).count();
            long countRefusee = userDemandes.stream().filter(d -> "Refusée".equalsIgnoreCase(d.getStatut())).count();

            statAcceptee.setText(String.valueOf(countAcceptee));
            statEnCours.setText(String.valueOf(countEnCours));
            statRefusee.setText(String.valueOf(countRefusee));

            for (Demande d : userDemandes) {
                Offre o = offreService.afficher().stream()
                        .filter(off -> off.getId().equals((long)d.getOffre_id()))
                        .findFirst().orElse(null);

                listContainer.getChildren().add(createCandidatureRow(d, o));
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private HBox createCandidatureRow(Demande d, Offre o) {
        HBox row = new HBox(20);
        row.setStyle("-fx-padding: 15; -fx-border-color: #eee; -fx-border-width: 0 0 1 0; -fx-alignment: CENTER_LEFT; -fx-background-color: white;");

        VBox offerInfo = new VBox(5);
        offerInfo.setPrefWidth(200);
        Label title = new Label(o != null ? o.getTitle() : "Offre inconnue");
        title.setStyle("-fx-font-weight: bold;");
        Label detail = new Label((o != null ? o.getLieu() : "") + " • " + (o != null ? o.getSalaire() : "") + " DT");
        detail.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11;");
        offerInfo.getChildren().addAll(title, detail);

        VBox dateInfo = new VBox(5);
        dateInfo.setPrefWidth(120);
        Label dateL = new Label(d.getDate_postulation().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        Label timeL = new Label(d.getDate_postulation().format(DateTimeFormatter.ofPattern("HH:mm")));
        timeL.setStyle("-fx-text-fill: #7f8c8d;");
        dateInfo.getChildren().addAll(dateL, timeL);

        HBox docs = new HBox(10);
        docs.setPrefWidth(150);
        Button btnCV = new Button("📄 CV");
        Button btnML = new Button("📄 Lettre");

        // Paths
        String basePath = "C:\\Users\\USER\\Documents\\Esprit\\PI JAVA\\agriSmartJava\\src\\main\\resources\\uploads\\";
        btnCV.setOnAction(e -> openFile(basePath + "cv\\" + d.getCv()));
        btnML.setOnAction(e -> openFile(basePath + "lettres\\" + d.getLettre_motivation()));
        docs.getChildren().addAll(btnCV, btnML);

        Label statusLabel = new Label(d.getStatut());
        statusLabel.setPrefWidth(100);
        String color = d.getStatut().equals("En cours") ? "#f39c12" : d.getStatut().equals("Acceptée") ? "#27ae60" : "#e74c3c";
        statusLabel.setStyle("-fx-background-color: " + color + "22; -fx-text-fill: " + color + "; -fx-padding: 5 10; -fx-background-radius: 15; -fx-font-weight: bold; -fx-alignment: center;");

        HBox actions = new HBox(10);
        Button viewBtn = new Button("👁");
        Button editBtn = new Button("✏");
        Button deleteBtn = new Button("🗑");
        deleteBtn.setStyle("-fx-text-fill: #e74c3c;");

        // Action Logic
        viewBtn.setOnAction(e -> showOffreDetail(o));

        // EDIT LOGIC START
        editBtn.setOnAction(e -> {
            PostulerController.selectedDemandeForEdit = d; // Set the object to edit
            PostulerController.currentOffreId = d.getOffre_id(); // Keep track of the offer
            switchView("/Views/Offres/PostulerForm.fxml");
        });
        // EDIT LOGIC END

        deleteBtn.setOnAction(e -> handleDelete(d));

        actions.getChildren().addAll(viewBtn, editBtn, deleteBtn);
        row.getChildren().addAll(offerInfo, dateInfo, docs, statusLabel, actions);
        return row;
    }

    private void openFile(String path) {
        try {
            File file = new File(path);
            if (file.exists()) {
                Desktop.getDesktop().open(file);
            } else {
                new Alert(Alert.AlertType.ERROR, "Fichier introuvable : " + path).show();
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void handleDelete(Demande d) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer cette candidature ?");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                demandeService.supprimer(d.getId().intValue());
                refreshList();
            } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    private void showOffreDetail(Offre o) {
        CandidatOffreController.selectedOffreForView = o;
        switchView("/Views/Offres/OffreDetailView.fxml");
    }

    @FXML private void showOffres() { switchView("/Views/Offres/CandidatOffreList.fxml"); }

    private void switchView(String fxmlPath) {
        try {
            URL resource = getClass().getResource(fxmlPath);
            if (resource == null) return;
            Parent root = FXMLLoader.load(resource);
            StackPane contentArea = (StackPane) listContainer.getScene().lookup("#contentArea");
            if (contentArea != null) contentArea.getChildren().setAll(root);
        } catch (IOException e) { e.printStackTrace(); }
    }
}