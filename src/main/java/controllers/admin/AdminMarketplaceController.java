package controllers.admin;

import entities.Produit;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.geometry.Side;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.geometry.Pos;
import services.ProduitService;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AdminMarketplaceController {

    @FXML private Label totalProductsLabel;
    @FXML private Label activeProductsLabel;
    @FXML private Label bannedProductsLabel;
    @FXML private Label ventesLabel;
    @FXML private Label locationsLabel;
    @FXML private Label promotionsLabel;
    @FXML private PieChart typeChart;
    @FXML private PieChart statusChart;
    @FXML private BarChart<String, Number> stockChart;

    @FXML private TextField searchField;
    @FXML private TableView<Produit> productsTable;
    @FXML private TableColumn<Produit, Integer> colId;
    @FXML private TableColumn<Produit, String> colNom;
    @FXML private TableColumn<Produit, String> colCategorie;
    @FXML private TableColumn<Produit, String> colType;
    @FXML private TableColumn<Produit, Integer> colStock;
    @FXML private TableColumn<Produit, Boolean> colBanned;
    @FXML private TableColumn<Produit, Void> colActions;

    private final ProduitService produitService = new ProduitService();
    private List<Produit> allProducts = new ArrayList<>();

    @FXML
    public void initialize() {
        setupTable();
        refreshAll();
    }

    @FXML
    public void handleSearch() {
        applyFilter();
    }

    @FXML
    public void clearSearch() {
        if (searchField != null) {
            searchField.clear();
        }
        applyFilter();
    }

    @FXML
    public void refreshAll() {
        try {
            allProducts = produitService.afficherTous();
            applyFilter();
            refreshStats();
            refreshCharts();
        } catch (SQLException e) {
            showError("Erreur chargement produits", e.getMessage());
        }
    }

    private void setupTable() {
        if (productsTable == null) {
            return;
        }

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colCategorie.setCellValueFactory(new PropertyValueFactory<>("categorie"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colStock.setCellValueFactory(new PropertyValueFactory<>("quantiteStock"));
        colBanned.setCellValueFactory(new PropertyValueFactory<>("banned"));

        colBanned.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean banned, boolean empty) {
                super.updateItem(banned, empty);
                if (empty || banned == null) {
                    setText(null);
                    return;
                }
                setText(banned ? "Banni" : "Actif");
            }
        });

        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button banBtn = new Button("Bannir");
            private final Button unbanBtn = new Button("Debannir");
            private final HBox box = new HBox(8, banBtn, unbanBtn);

            {
                box.setAlignment(Pos.CENTER_LEFT);
                banBtn.getStyleClass().add("admin-btn-danger");
                unbanBtn.getStyleClass().add("admin-btn-positive");

                banBtn.setOnAction(e -> {
                    Produit produit = getTableView().getItems().get(getIndex());
                    updateBanStatus(produit, true);
                });
                unbanBtn.setOnAction(e -> {
                    Produit produit = getTableView().getItems().get(getIndex());
                    updateBanStatus(produit, false);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                    return;
                }
                Produit produit = getTableView().getItems().get(getIndex());
                banBtn.setDisable(produit.isBanned());
                unbanBtn.setDisable(!produit.isBanned());
                setGraphic(box);
            }
        });
    }

    private void updateBanStatus(Produit produit, boolean banned) {
        if (produit == null) {
            return;
        }

        String action = banned ? "bannir" : "debannir";
        if (!confirm("Produit", "Voulez-vous " + action + " le produit: " + produit.getNom() + " ?")) {
            return;
        }

        try {
            produitService.setBanned(produit.getId(), banned);
            refreshAll();
        } catch (SQLException e) {
            showError("Erreur mise a jour", e.getMessage());
        }
    }

    private void applyFilter() {
        if (productsTable == null) {
            return;
        }

        String keyword = searchField == null ? "" : safe(searchField.getText()).toLowerCase();
        List<Produit> filtered = new ArrayList<>();
        for (Produit produit : allProducts) {
            String nom = safe(produit.getNom()).toLowerCase();
            String categorie = safe(produit.getCategorie()).toLowerCase();
            if (keyword.isBlank() || nom.contains(keyword) || categorie.contains(keyword)) {
                filtered.add(produit);
            }
        }
        productsTable.setItems(FXCollections.observableArrayList(filtered));
    }

    private void refreshStats() {
        long total = allProducts.size();
        long active = allProducts.stream().filter(p -> !p.isBanned()).count();
        long banned = allProducts.stream().filter(Produit::isBanned).count();
        long ventes = allProducts.stream().filter(p -> "vente".equalsIgnoreCase(safe(p.getType()))).count();
        long locations = allProducts.stream().filter(p -> "location".equalsIgnoreCase(safe(p.getType()))).count();
        long promotions = allProducts.stream().filter(Produit::isPromotion).count();

        totalProductsLabel.setText(String.valueOf(total));
        activeProductsLabel.setText(String.valueOf(active));
        bannedProductsLabel.setText(String.valueOf(banned));
        ventesLabel.setText(String.valueOf(ventes));
        locationsLabel.setText(String.valueOf(locations));
        promotionsLabel.setText(String.valueOf(promotions));
    }

    private void refreshCharts() {
        if (typeChart != null) {
            long ventes = allProducts.stream().filter(p -> "vente".equalsIgnoreCase(safe(p.getType()))).count();
            long locations = allProducts.stream().filter(p -> "location".equalsIgnoreCase(safe(p.getType()))).count();
            typeChart.setData(FXCollections.observableArrayList(
                    new PieChart.Data("Vente", ventes),
                    new PieChart.Data("Location", locations)
            ));
            typeChart.setLegendSide(Side.BOTTOM);
            typeChart.setClockwise(true);
            typeChart.setLabelLineLength(16);
        }

        if (statusChart != null) {
            long actifs = allProducts.stream().filter(p -> !p.isBanned()).count();
            long bannis = allProducts.stream().filter(Produit::isBanned).count();
            statusChart.setData(FXCollections.observableArrayList(
                    new PieChart.Data("Actifs", actifs),
                    new PieChart.Data("Bannis", bannis)
            ));
            statusChart.setLegendSide(Side.BOTTOM);
            statusChart.setClockwise(true);
            statusChart.setLabelLineLength(16);
        }

        if (stockChart != null) {
            long faible = allProducts.stream().filter(p -> p.getQuantiteStock() <= 5).count();
            long moyen = allProducts.stream().filter(p -> p.getQuantiteStock() > 5 && p.getQuantiteStock() <= 20).count();
            long eleve = allProducts.stream().filter(p -> p.getQuantiteStock() > 20).count();

            XYChart.Series<String, Number> serie = new XYChart.Series<>();
            serie.setName("Produits");
            serie.getData().add(new XYChart.Data<>("Stock faible", faible));
            serie.getData().add(new XYChart.Data<>("Stock moyen", moyen));
            serie.getData().add(new XYChart.Data<>("Stock eleve", eleve));

            stockChart.getData().setAll(serie);
            stockChart.setLegendVisible(false);
            stockChart.setAnimated(false);
        }
    }

    private boolean confirm(String title, String message) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle(title);
        confirm.setHeaderText(null);
        confirm.setContentText(message);
        Optional<ButtonType> result = confirm.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
