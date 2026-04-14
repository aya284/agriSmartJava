package controllers.admin;

import entities.Culture;
import entities.Parcelle;
import entities.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import services.CultureService;
import services.ParcelleService;
import services.UserService;
import utils.NotificationUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class AdminCulturesController {

    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> sortCombo;
    @FXML
    private TilePane cardsContainer;
    @FXML
    private Pagination pagination;

    private final ParcelleService parcelleService = new ParcelleService();
    private final CultureService cultureService = new CultureService();
    private final UserService userService = new UserService();

    private ObservableList<ParcelWithCultures> masterData = FXCollections.observableArrayList();
    private FilteredList<ParcelWithCultures> filteredData;
    private SortedList<ParcelWithCultures> sortedData;

    private static final int ITEMS_PER_PAGE = 9;

    public void initialize() {
        try {
            setupSortCombo();
            loadData();

            filteredData = new FilteredList<>(masterData, p -> true);
            sortedData = new SortedList<>(filteredData);

            setupPagination();
        } catch (Exception e) {
            System.err.println("!!! Critical Error in AdminCulturesController.initialize: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setupSortCombo() {
        sortCombo.getItems().addAll("Par Agriculteur", "Par Parcelle", "Par Culture");
        sortCombo.setValue("Par Agriculteur");
    }

    private void loadData() {
        try {
            System.out.println("[ADMIN] Fetching database records...");
            List<Parcelle> parcels = parcelleService.afficher();
            if (parcels == null)
                parcels = new ArrayList<>();

            List<ParcelWithCultures> list = new ArrayList<>();
            for (Parcelle p : parcels) {
                // Safeguard against missing owners or cultures
                String ownerName = "Inconnu (ID:" + p.getUserId() + ")";
                List<Culture> cultures = new ArrayList<>();

                try {
                    User owner = userService.findById(p.getUserId());
                    if (owner != null)
                        ownerName = owner.getFullName();
                } catch (Exception e) {
                    System.err.println("Warning: Could not fetch owner for parcel " + p.getId());
                }

                try {
                    List<Culture> cList = cultureService.getByParcelle(p.getId());
                    if (cList != null)
                        cultures = cList;
                } catch (Exception e) {
                    System.err.println("Warning: Could not fetch cultures for parcel " + p.getId());
                }

                list.add(new ParcelWithCultures(p, cultures, ownerName));
            }
            masterData.setAll(list);
            System.out.println("[ADMIN] Successfully processed " + list.size() + " cards.");
        } catch (Exception e) {
            System.err.println("!!! CRITICAL: loadData failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setupPagination() {
        int count = (int) Math.ceil((double) filteredData.size() / ITEMS_PER_PAGE);
        pagination.setPageCount(Math.max(1, count));
        pagination.currentPageIndexProperty().addListener((obs, old, newVal) -> updatePage(newVal.intValue()));

        updatePage(0);
    }

    @FXML
    public void handleExport() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exporter la liste des cultures");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichier CSV", "*.csv"));
        fileChooser.setInitialFileName("agri_cultures_global_" + java.time.LocalDate.now() + ".csv");

        File file = fileChooser.showSaveDialog(cardsContainer.getScene().getWindow());

        if (file != null) {
            try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"))) {
                // BOM for Excel UTF-8 compatibility
                writer.write('\ufeff');

                // Header
                writer.println("Agriculteur;Parcelle;Type de Sol;Surface (m2);Cultures");

                for (ParcelWithCultures item : sortedData) {
                    String culturesString = item.cultures.stream()
                            .map(c -> c.getTypeCulture() + " (" + c.getVariete() + ")")
                            .collect(Collectors.joining(", "));

                    writer.printf("%s;%s;%s;%.2f;%s\n",
                            item.ownerName.replace(";", " "),
                            item.parcel.getNom().replace(";", " "),
                            item.parcel.getTypeSol().replace(";", " "),
                            item.parcel.getSurface(),
                            culturesString.replace(";", " "));
                }

                NotificationUtil.showSuccess(cardsContainer.getScene().getWindow(), "Liste exportée avec succès !");
            } catch (Exception e) {
                System.err.println("Export error: " + e.getMessage());
                NotificationUtil.showError(cardsContainer.getScene().getWindow(),
                        "Échec de l'export : " + e.getMessage());
            }
        }
    }

    private void updatePage(int pageIndex) {
        cardsContainer.getChildren().clear();

        int fromIndex = pageIndex * ITEMS_PER_PAGE;
        int toIndex = Math.min(fromIndex + ITEMS_PER_PAGE, sortedData.size());

        for (int i = fromIndex; i < toIndex; i++) {
            cardsContainer.getChildren().add(createParcelCard(sortedData.get(i)));
        }

        pagination.setPageCount(Math.max(1, (int) Math.ceil((double) filteredData.size() / ITEMS_PER_PAGE)));
    }

    @FXML
    public void handleSearch() {
        String query = searchField.getText().toLowerCase().trim();
        filteredData.setPredicate(item -> {
            if (query.isEmpty())
                return true;

            boolean matchFarmer = item.ownerName.toLowerCase().contains(query);
            boolean matchParcel = item.parcel.getNom().toLowerCase().contains(query);

            boolean matchCulture = item.cultures.stream().anyMatch(c -> c.getTypeCulture().toLowerCase().contains(query)
                    || c.getVariete().toLowerCase().contains(query));

            return matchFarmer || matchParcel || matchCulture;
        });
        updatePage(0);
        pagination.setCurrentPageIndex(0);
    }

    @FXML
    public void handleSort() {
        String sortType = sortCombo.getValue();
        if (sortType == null)
            return;

        switch (sortType) {
            case "Par Agriculteur":
                sortedData.setComparator(Comparator.comparing(p -> p.ownerName));
                break;
            case "Par Parcelle":
                sortedData.setComparator(Comparator.comparing(p -> p.parcel.getNom()));
                break;
            case "Par Culture":
                sortedData.setComparator(
                        Comparator.comparing(p -> p.cultures.isEmpty() ? "" : p.cultures.get(0).getTypeCulture()));
                break;
        }
        updatePage(0);
    }

    private VBox createParcelCard(ParcelWithCultures item) {
        VBox card = new VBox(10);
        card.getStyleClass().add("parcel-card-admin");
        card.setMinWidth(240);
        card.setMaxWidth(240);
        card.setPadding(new Insets(12));

        // Farmer Info
        Label ownerTag = new Label("AGRICULTEUR");
        ownerTag.setStyle("-fx-font-size: 10; -fx-text-fill: #7f8c8d; -fx-font-weight: bold;");
        Label ownerName = new Label(item.ownerName);
        ownerName.setStyle("-fx-font-size: 15; -fx-font-weight: bold; -fx-text-fill: #2ecc71;");

        VBox ownerBox = new VBox(2, ownerTag, ownerName);
        ownerBox.setStyle("-fx-padding: 0 0 10 0; -fx-border-color: #ecf0f1; -fx-border-width: 0 0 1 0;");

        // Parcel Info
        Label parcelTag = new Label("PARCELLE");
        parcelTag.setStyle("-fx-font-size: 10; -fx-text-fill: #7f8c8d; -fx-font-weight: bold;");
        Label parcelName = new Label(item.parcel.getNom());
        parcelName.getStyleClass().add("card-title-admin");

        Label parcelDetails = new Label(item.parcel.getTypeSol() + " | " + item.parcel.getSurface() + " m²");
        parcelDetails.getStyleClass().add("card-subtitle-admin");

        // Cultures Info
        Label culturesTag = new Label("CULTURES");
        culturesTag.getStyleClass().add("section-label-admin");

        FlowPane cultureTags = new FlowPane(6, 6);
        if (item.cultures.isEmpty()) {
            Label placeholder = new Label("Vide");
            placeholder.setStyle("-fx-text-fill: #bdc3c7; -fx-font-style: italic;");
            cultureTags.getChildren().add(placeholder);
        } else {
            for (Culture c : item.cultures) {
                Label tag = new Label(c.getTypeCulture());
                tag.getStyleClass().add("culture-tag-admin");
                cultureTags.getChildren().add(tag);
            }
        }

        card.getChildren().addAll(ownerBox, parcelTag, parcelName, parcelDetails, culturesTag, cultureTags);

        return card;
    }

    // Static helper class
    private static class ParcelWithCultures {
        final Parcelle parcel;
        final List<Culture> cultures;
        final String ownerName;

        ParcelWithCultures(Parcelle parcel, List<Culture> cultures, String ownerName) {
            this.parcel = parcel;
            this.cultures = cultures;
            this.ownerName = ownerName;
        }
    }
}
