package controllers;

import entities.Offre;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import services.OffreService;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;

public class AdminOffreController implements Initializable {

    @FXML private TableView<Offre> tableOffres;
    @FXML private TableColumn<Offre, String> colTitre, colType, colValidation;
    @FXML private TableColumn<Offre, Double> colSalaire;
    @FXML private TableColumn<Offre, Void> colActions;

    @FXML private Label statAttente, statApprouvee, statRefusee;
    @FXML private javafx.scene.shape.Circle statusDot;
    // Admin Detail Labels
    @FXML private Label adminDetailTitle, adminDetailDesc, adminDetailLieu, adminDetailStatus;

    private final OffreService service = new OffreService();
    private ObservableList<Offre> masterData = FXCollections.observableArrayList();

    // Static to persist selection between scene switches
    private static Offre currentSelectedOffre;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Mode 1: Table List View
        if (tableOffres != null) {
            setupTable();
            loadData();
        }
        // Mode 2: Detail View
        else if (adminDetailTitle != null && currentSelectedOffre != null) {
            setupAdminDetailPage();
        }
    }

    private void setupTable() {
        colTitre.setCellValueFactory(new PropertyValueFactory<>("title"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type_contrat"));
        colSalaire.setCellValueFactory(new PropertyValueFactory<>("salaire"));

        // 1. Enhanced Validation Column Formatting
        colValidation.setCellValueFactory(new PropertyValueFactory<>("statut_validation"));
        colValidation.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    // Formatting logic
                    String formattedText;
                    String color;

                    switch (item.toLowerCase()) {
                        case "en_attente":
                        case "en attente":
                            formattedText = "En attente";
                            color = "#f39c12"; // Orange
                            break;
                        case "approuvée":
                            formattedText = "Approuvée";
                            color = "#27ae60"; // Green
                            break;
                        case "refusée":
                            formattedText = "Refusée";
                            color = "#e74c3c"; // Red
                            break;
                        default:
                            // Capitalize first letter for any other status
                            formattedText = item.substring(0, 1).toUpperCase() + item.substring(1);
                            color = "#2c3e50";
                    }

                    setText(formattedText);
                    setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");
                }
            }
        });

        // 2. Custom Cell Factory for Actions (Keep your existing logic here)
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnApprove = new Button("✔");
            private final Button btnRefuse = new Button("✖");
            private final Button btnDetails = new Button("👁 Détails");
            private final HBox container = new HBox(8, btnDetails, btnApprove, btnRefuse);

            {
                container.setAlignment(Pos.CENTER);
                btnApprove.setStyle("-fx-background-color: #1a3323; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 5;");
                btnRefuse.setStyle("-fx-background-color: #d63031; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 5;");
                btnDetails.setStyle("-fx-background-color: #f1f2f6; -fx-cursor: hand; -fx-background-radius: 5;");

                btnApprove.setOnAction(e -> updateStatus(getTableView().getItems().get(getIndex()), "approuvée"));
                btnRefuse.setOnAction(e -> updateStatus(getTableView().getItems().get(getIndex()), "refusée"));
                btnDetails.setOnAction(e -> {
                    currentSelectedOffre = getTableView().getItems().get(getIndex());
                    switchView("/Views/Offres/AdminOffreDetail.fxml");
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Offre o = getTableView().getItems().get(getIndex());
                    // Handle both "en_attente" and "en attente" from DB
                    String status = o.getStatut_validation().toLowerCase();
                    boolean isPending = status.equals("en_attente") || status.equals("en attente");

                    btnApprove.setVisible(isPending);
                    btnRefuse.setVisible(isPending);
                    setGraphic(container);
                }
            }
        });
    }

    private void loadData() {
        try {
            List<Offre> list = service.afficher();
            masterData.setAll(list);
            tableOffres.setItems(masterData);
            updateStats();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateStatus(Offre o, String status) {
        try {
            service.updateValidationStatus(o.getId(), status);
            loadData(); // Refresh table and cards
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateStats() {
        statAttente.setText(String.valueOf(masterData.stream().filter(o -> "en attente".equalsIgnoreCase(o.getStatut_validation())).count()));
        statApprouvee.setText(String.valueOf(masterData.stream().filter(o -> "approuvée".equalsIgnoreCase(o.getStatut_validation())).count()));
        statRefusee.setText(String.valueOf(masterData.stream().filter(o -> "refusée".equalsIgnoreCase(o.getStatut_validation())).count()));
    }

    private void setupAdminDetailPage() {
        adminDetailTitle.setText(currentSelectedOffre.getTitle());
        adminDetailDesc.setText(currentSelectedOffre.getDescription());
        adminDetailLieu.setText("📍 " + currentSelectedOffre.getLieu());

        String rawStatus = currentSelectedOffre.getStatut_validation();
        String formattedStatus;
        String color;

        // Consistency with your Table logic
        switch (rawStatus.toLowerCase()) {
            case "en_attente":
            case "en attente":
                formattedStatus = "En attente";
                color = "#f39c12"; // Orange
                break;
            case "approuvée":
                formattedStatus = "Approuvée";
                color = "#27ae60"; // Green
                break;
            case "refusée":
                formattedStatus = "Refusée";
                color = "#e74c3c"; // Red
                break;
            default:
                formattedStatus = rawStatus.substring(0, 1).toUpperCase() + rawStatus.substring(1);
                color = "#2c3e50";
        }

        adminDetailStatus.setText(formattedStatus);
        adminDetailStatus.setStyle("-fx-text-fill: " + color + ";");
        statusDot.setStyle("-fx-fill: " + color + ";");
    }

    @FXML
    public void goBackToTable() {
        switchView("/Views/Offres/AdminOffreList.fxml");
    }

    private void switchView(String path) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(path));
            // Ensure the scene lookup matches your shell ID
            StackPane contentArea = (StackPane) (tableOffres != null ? tableOffres : adminDetailTitle).getScene().lookup("#contentArea");
            contentArea.getChildren().setAll(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
