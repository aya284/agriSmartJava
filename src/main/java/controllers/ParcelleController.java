package controllers;

import entities.Parcelle;
import entities.Culture;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import services.ParcelleService;
import services.CultureService;
import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class ParcelleController {

    @FXML private ListView<Parcelle> lvParcelles;
    @FXML private Label lblDetailNom, lblDetailSurface, lblDetailType, lblDetailCoords;
    @FXML private WebView webViewMap;
    @FXML private VBox detailsContainer;
    @FXML private VBox cultureListContainer;
    @FXML private TextField txtSearch;

    private ParcelleService ps = new ParcelleService();
    private CultureService cs = new CultureService();
    private ObservableList<Parcelle> parcelleList = FXCollections.observableArrayList();
    private FilteredList<Parcelle> filteredList;

    @FXML
    public void initialize() {
        refreshList();
        setupSearch();
        setupSelectionListener();
        setupCellFactory();
        loadMap();
    }

    private void setupSearch() {
        filteredList = new FilteredList<>(parcelleList, p -> true);
        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredList.setPredicate(p -> {
                if (newVal == null || newVal.isEmpty()) return true;
                String lowerCaseFilter = newVal.toLowerCase();
                return p.getNom().toLowerCase().contains(lowerCaseFilter) ||
                       p.getTypeSol().toLowerCase().contains(lowerCaseFilter);
            });
        });
        lvParcelles.setItems(filteredList);
    }

    private void setupSelectionListener() {
        lvParcelles.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                detailsContainer.setVisible(true);
                afficherDetails(newVal);
            } else {
                detailsContainer.setVisible(false);
            }
        });
    }

    private void setupCellFactory() {
        lvParcelles.setCellFactory(lv -> new ListCell<Parcelle>() {
            @Override
            protected void updateItem(Parcelle p, boolean empty) {
                super.updateItem(p, empty);
                if (empty || p == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox card = new HBox(15);
                    card.getStyleClass().add("parcel-list-card");
                    card.setAlignment(Pos.CENTER_LEFT);

                    Label icon = new Label("🌱");
                    icon.getStyleClass().add("parcel-icon");

                    VBox info = new VBox(4);
                    Label name = new Label(p.getNom());
                    name.getStyleClass().add("parcel-name");
                    Label details = new Label(p.getSurface() + " Ha • " + p.getTypeSol());
                    details.getStyleClass().add("parcel-subtext");

                    info.getChildren().addAll(name, details);
                    card.getChildren().addAll(icon, info);
                    setGraphic(card);
                }
            }
        });
    }

    private void refreshList() {
        try {
            parcelleList.setAll(ps.afficher());
        } catch (SQLException e) {
            showError("Erreur SQL", "Impossible de charger les parcelles : " + e.getMessage());
        }
    }

    private void loadMap() {
        String content = "<html><head><link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'/><script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script></head>"
                + "<body style='margin:0;'><div id='map' style='height:100%;'></div>"
                + "<script>var map = L.map('map').setView([36.8065, 10.1815], 6); L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png').addTo(map); var marker;</script></body></html>";
        webViewMap.getEngine().loadContent(content);
    }

    private void afficherDetails(Parcelle p) {
        lblDetailNom.setText(p.getNom());
        lblDetailSurface.setText(p.getSurface() + " Hectares");
        lblDetailType.setText(p.getTypeSol());
        lblDetailCoords.setText("Lat: " + p.getLatitude() + " | Lon: " + p.getLongitude());

        String script = "if(marker) map.removeLayer(marker); " +
                "marker = L.marker([" + p.getLatitude() + "," + p.getLongitude() + "]).addTo(map);" +
                "map.setView([" + p.getLatitude() + "," + p.getLongitude() + "], 13);";
        webViewMap.getEngine().executeScript(script);

        loadCultures(p.getId());
    }

    private void loadCultures(int parcelleId) {
        cultureListContainer.getChildren().clear();
        try {
            List<Culture> cultures = cs.getByParcelle(parcelleId);
            if (cultures.isEmpty()) {
                Label placeholder = new Label("Aucune culture enregistrée.");
                placeholder.setStyle("-fx-text-fill: #999; -fx-italic: true;");
                cultureListContainer.getChildren().add(placeholder);
            } else {
                for (Culture c : cultures) {
                    cultureListContainer.getChildren().add(createCultureCard(c));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading cultures: " + e.getMessage());
        }
    }

    private VBox createCultureCard(Culture c) {
        VBox card = new VBox(10);
        card.getStyleClass().add("culture-card");

        // Header
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label icon = new Label("🌿");
        icon.getStyleClass().add("culture-icon-box");
        
        VBox titleArea = new VBox(2);
        Label typeLabel = new Label(c.getTypeCulture());
        typeLabel.getStyleClass().add("culture-card-title");
        Label varLabel = new Label("Variété : " + c.getVariete());
        varLabel.getStyleClass().add("culture-card-subtitle");
        titleArea.getChildren().addAll(typeLabel, varLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox actions = new HBox(5);
        Button btnEdit = new Button("✎");
        btnEdit.getStyleClass().add("culture-action-btn");
        btnEdit.setOnAction(e -> openEditCultureModal(c));
        
        Button btnDel = new Button("🗑");
        btnDel.getStyleClass().add("culture-action-btn-danger");
        btnDel.setOnAction(e -> handleCultureDelete(c));
        
        actions.getChildren().addAll(btnEdit, btnDel);

        header.getChildren().addAll(icon, titleArea, spacer, actions);

        // Dates
        HBox dateGrid = new HBox(0);
        dateGrid.getStyleClass().add("culture-date-grid");
        
        VBox plantCol = createDateCol("Plantation", c.getDatePlantation().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        Separator sep = new Separator();
        sep.setOrientation(javafx.geometry.Orientation.VERTICAL);
        VBox harvestCol = createDateCol("Récolte", c.getDateRecoltePrevue().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        
        HBox.setHgrow(plantCol, Priority.ALWAYS);
        HBox.setHgrow(harvestCol, Priority.ALWAYS);
        dateGrid.getChildren().addAll(plantCol, sep, harvestCol);

        // Status
        Label statusBadge = new Label(c.getStatut());
        statusBadge.getStyleClass().add("culture-status-badge");
        if (c.getStatut().equals("Récolté")) statusBadge.getStyleClass().add("status-harvested");
        else if (c.getStatut().equals("Planté")) statusBadge.getStyleClass().add("status-planted");

        card.getChildren().addAll(header, dateGrid, statusBadge);
        return card;
    }

    private VBox createDateCol(String label, String date) {
        VBox col = new VBox(4);
        col.setAlignment(Pos.CENTER);
        col.setPadding(new Insets(5));
        Label l = new Label(label);
        l.getStyleClass().add("culture-date-label");
        Label d = new Label(date);
        d.getStyleClass().add("culture-date-value");
        col.getChildren().addAll(l, d);
        return col;
    }

    @FXML
    private void openAddCultureModal() throws IOException {
        Parcelle p = lvParcelles.getSelectionModel().getSelectedItem();
        if (p == null) return;

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/Views/FormulaireCulture.fxml"));
        Parent root = loader.load();
        FormulaireCultureController controller = loader.getController();
        controller.setParcelleId(p.getId());

        showCultureStage(root, "Ajouter une Culture", p.getId());
    }

    private void openEditCultureModal(Culture c) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Views/FormulaireCulture.fxml"));
            Parent root = loader.load();
            FormulaireCultureController controller = loader.getController();
            controller.setCultureData(c);

            showCultureStage(root, "Modifier la Culture", c.getParcelleId());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showCultureStage(Parent root, String title, int parcelleId) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle(title);
        stage.setScene(new Scene(root));
        stage.showAndWait();
        loadCultures(parcelleId);
    }

    private void handleCultureDelete(Culture c) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Suppression");
        alert.setHeaderText("Supprimer la culture ?");
        alert.setContentText("Voulez-vous vraiment supprimer cette culture de la parcelle ?");
        if (alert.showAndWait().get() == ButtonType.OK) {
            try {
                cs.supprimer(c.getId());
                loadCultures(c.getParcelleId());
            } catch (SQLException e) {
                showError("Erreur", "Suppression impossible : " + e.getMessage());
            }
        }
    }

    @FXML
    private void openAddModal() throws IOException {
        showModal(null);
    }

    @FXML
    private void openEditModal() throws IOException {
        Parcelle selected = lvParcelles.getSelectionModel().getSelectedItem();
        if (selected != null) showModal(selected);
    }

    private void showModal(Parcelle p) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/Views/FormulaireParcelle.fxml"));
        Parent root = loader.load();
        FormulaireParcelleController controller = loader.getController();
        if (p != null) controller.setParcelleData(p);

        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle(p == null ? "Ajouter une Parcelle" : "Modifier la Parcelle");
        stage.setScene(new Scene(root));
        stage.showAndWait();
        refreshList();
    }

    @FXML
    private void handleDelete() {
        Parcelle selected = lvParcelles.getSelectionModel().getSelectedItem();
        if (selected != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirmation");
            alert.setHeaderText("Supprimer " + selected.getNom() + " ?");
            if (alert.showAndWait().get() == ButtonType.OK) {
                try {
                    ps.supprimer(selected.getId());
                    refreshList();
                    detailsContainer.setVisible(false);
                } catch (SQLException e) {
                    showError("Erreur", e.getMessage());
                }
            }
        }
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}