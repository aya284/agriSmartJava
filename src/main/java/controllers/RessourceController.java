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

public class RessourceController {

    @FXML private TableView<Ressource> tvRessources;
    @FXML private TableColumn<Ressource, String> colNom, colType, colUnite;
    @FXML private TableColumn<Ressource, Double> colStock;
    @FXML private TableColumn<Ressource, Void> colActions;
    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cbSort;

    private RessourceService rs = new RessourceService();
    private ObservableList<Ressource> ressourceList = FXCollections.observableArrayList();

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

        SortedList<Ressource> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tvRessources.comparatorProperty());
        tvRessources.setItems(sortedData);
    }

    private void applySort() {
        String selection = cbSort.getValue();
        if (selection == null) return;

        tvRessources.getSortOrder().clear();
        switch (selection) {
            case "Nom (A-Z)":
                colNom.setSortType(TableColumn.SortType.ASCENDING);
                tvRessources.getSortOrder().add(colNom);
                break;
            case "Type":
                colType.setSortType(TableColumn.SortType.ASCENDING);
                tvRessources.getSortOrder().add(colType);
                break;
            case "Stock (Décroissant)":
                colStock.setSortType(TableColumn.SortType.DESCENDING);
                tvRessources.getSortOrder().add(colStock);
                break;
        }
        tvRessources.sort();
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
            stage.setScene(new Scene(root));
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
                refreshList();
            } catch (SQLException e) {
                Alert error = new Alert(Alert.AlertType.ERROR);
                error.setContentText("Erreur lors de la suppression : " + e.getMessage());
                error.show();
            }
        }
    }
}
