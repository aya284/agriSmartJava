package controllers;

import entities.Ressource;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import services.RessourceService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Optional;
import utils.NotificationUtil;

import java.io.File;
import java.io.PrintWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import javafx.stage.FileChooser;

public class RessourceController {

    @FXML private TableView<Ressource> tvRessources;
    @FXML private TableColumn<Ressource, String> colNom, colType, colUnite;
    @FXML private TableColumn<Ressource, Double> colStock;
    @FXML private TableColumn<Ressource, Void> colActions;
    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cbSort;
    @FXML private Pagination pagination;

    private final int ITEMS_PER_PAGE = 8;
    private RessourceService rs = new RessourceService();
    private ObservableList<Ressource> ressourceList = FXCollections.observableArrayList();
    private SortedList<Ressource> sortedData;

    @FXML
    public void initialize() {
        setupTable();
        refreshList();
        setupSearchAndSort();
    }

    private void setupTable() {
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colStock.setCellValueFactory(new PropertyValueFactory<>("stockRestant"));
        colUnite.setCellValueFactory(new PropertyValueFactory<>("unite"));

        colNom.setSortable(false);
        colType.setSortable(false);
        colStock.setSortable(false);
        colUnite.setSortable(false);
        colActions.setSortable(false);

        // Custom Actions Column
        colActions.setCellFactory(column -> new TableCell<>() {
            private final Button btnEdit = new Button("✎");
            private final Button btnDelete = new Button("🗑");
            private final HBox container = new HBox(10, btnEdit, btnDelete);

            {
                btnEdit.getStyleClass().add("btn-gold");
                btnEdit.setStyle("-fx-padding: 4 8; -fx-font-size: 12px;");
                btnDelete.getStyleClass().add("btn-danger");
                btnDelete.setStyle("-fx-padding: 4 8; -fx-font-size: 12px;");
                
                btnEdit.setOnAction(e -> {
                    Ressource r = getTableView().getItems().get(getIndex());
                    openEditModal(r);
                });
                
                btnDelete.setOnAction(e -> {
                    Ressource r = getTableView().getItems().get(getIndex());
                    handleDelete(r);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(container);
                }
            }
        });
    }

    private void refreshList() {
        try {
            ressourceList.setAll(rs.afficher());
        } catch (SQLException e) {
            System.err.println("Error loading resources: " + e.getMessage());
        }
    }

    private void setupSearchAndSort() {
        cbSort.setItems(FXCollections.observableArrayList("Nom (A-Z)", "Type", "Stock (Décroissant)"));
        cbSort.setOnAction(e -> applySort());

        FilteredList<Ressource> filteredData = new FilteredList<>(ressourceList, p -> true);

        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(r -> {
                if (newValue == null || newValue.isEmpty()) return true;
                String lowerCaseFilter = newValue.toLowerCase();
                if (r.getNom().toLowerCase().contains(lowerCaseFilter)) return true;
                if (r.getType().toLowerCase().contains(lowerCaseFilter)) return true;
                return false;
            });
        });

        sortedData = new SortedList<>(filteredData);
        
        pagination.setPageFactory(this::createPage);
        filteredData.addListener((javafx.collections.ListChangeListener.Change<? extends Ressource> c) -> {
            updatePagination();
        });
        
        updatePagination();
    }

    private void updatePagination() {
        int pageCount = (int) Math.ceil((double) sortedData.size() / ITEMS_PER_PAGE);
        pagination.setPageCount(pageCount == 0 ? 1 : pageCount);
        pagination.setCurrentPageIndex(0);
        createPage(0);
    }

    private javafx.scene.Node createPage(int pageIndex) {
        int fromIndex = pageIndex * ITEMS_PER_PAGE;
        int toIndex = Math.min(fromIndex + ITEMS_PER_PAGE, sortedData.size());
        
        if (fromIndex <= toIndex && fromIndex < sortedData.size()) {
            tvRessources.setItems(FXCollections.observableArrayList(sortedData.subList(fromIndex, toIndex)));
        } else {
            tvRessources.setItems(FXCollections.observableArrayList());
        }
        return new javafx.scene.layout.VBox(); // Layout dummy pour la factory
    }

    private void applySort() {
        String selection = cbSort.getValue();
        if (selection == null) return;

        java.util.Comparator<Ressource> comparator = null;
        switch (selection) {
            case "Nom (A-Z)":
                comparator = java.util.Comparator.comparing(Ressource::getNom);
                break;
            case "Type":
                comparator = java.util.Comparator.comparing(Ressource::getType);
                break;
            case "Stock (Décroissant)":
                comparator = java.util.Comparator.comparing(Ressource::getStockRestant).reversed();
                break;
        }
        sortedData.setComparator(comparator);
        createPage(pagination.getCurrentPageIndex());
    }

    @FXML
    public void exportExcel() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sauvegarder l'inventaire");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel/CSV Fichier", "*.csv"));
        File file = fileChooser.showSaveDialog(tvRessources.getScene().getWindow());

        if (file != null) {
            try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"))) {
                writer.write('\ufeff'); // BOM pour Excel
                writer.println("Nom;Type;Stock Restant;Unité"); // Séparateur point-virgule pour Excel FR

                for (Ressource r : sortedData) {
                    writer.printf("%s;%s;%.2f;%s\n",
                            r.getNom().replace(";", " "),
                            r.getType().replace(";", " "),
                            r.getStockRestant(),
                            r.getUnite().replace(";", " "));
                }
                NotificationUtil.showSuccess(tvRessources.getScene().getWindow(), "Export réussi !");
            } catch (Exception e) {
                NotificationUtil.showError(tvRessources.getScene().getWindow(), "Erreur : " + e.getMessage());
            }
        }
    }

    @FXML
    private void openAddModal() {
        showModal(null);
    }

    private void openEditModal(Ressource r) {
        showModal(r);
    }

    private void showModal(Ressource r) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Views/FormulaireRessource.fxml"));
            Parent root = loader.load();
            FormulaireRessourceController controller = loader.getController();
            
            if (r != null) {
                controller.setRessourceData(r);
            }

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(r == null ? "Ajouter une Ressource" : "Modifier la Ressource");
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            stage.setScene(scene);
            stage.showAndWait();
            refreshList();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleDelete(Ressource r) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Suppression");
        alert.setHeaderText("Supprimer " + r.getNom() + " ?");
        alert.setContentText("Cette action est irréversible.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                rs.supprimer(r.getId());
                NotificationUtil.showDelete(tvRessources.getScene().getWindow(), "Ressource supprimée.");
                refreshList();
            } catch (SQLException e) {
                Alert error = new Alert(Alert.AlertType.ERROR);
                error.setContentText("Erreur lors de la suppression : " + e.getMessage());
                error.show();
            }
        }
    }
}
