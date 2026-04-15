package controllers.admin;

import entities.Ressource;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import services.RessourceService;

import java.util.List;

public class AdminRessourcesController {

    // ── Filtres ───────────────────────────────────────────────
    @FXML private TextField        searchField;
    @FXML private Label            totalLabel;

    // ── Tableau ───────────────────────────────────────────────
    @FXML private TableView<Ressource>           ressourcesTable;
    @FXML private TableColumn<Ressource, String> colId;
    @FXML private TableColumn<Ressource, String> colNom;
    @FXML private TableColumn<Ressource, String> colType;
    @FXML private TableColumn<Ressource, String> colStock;
    @FXML private TableColumn<Ressource, String> colAgriculteur;

    // ── Pagination ────────────────────────────────────────────
    @FXML private Button            btnFirst;
    @FXML private Button            btnPrev;
    @FXML private Button            btnNext;
    @FXML private Button            btnLast;
    @FXML private Label             pageInfoLabel;
    @FXML private ComboBox<Integer> pageSizeCombo;
    @FXML private HBox              paginationBox;

    // ── État pagination ───────────────────────────────────────
    private int currentPage = 1;
    private int pageSize    = 8; // Max 8 as requested
    private int totalPages  = 1;
    private int totalCount  = 0;

    private final RessourceService ressourceService = new RessourceService();

    // ─────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        setupFilters();
        setupColumns();
        setupPageSizeCombo();
        loadPage();
    }

    // ── Filtres ───────────────────────────────────────────────
    private void setupFilters() {
        searchField.textProperty().addListener((o, old, n) -> resetAndLoad());
    }

    // ── Taille de page ────────────────────────────────────────
    private void setupPageSizeCombo() {
        pageSizeCombo.getItems().addAll(5, 8, 10, 20);
        pageSizeCombo.setValue(8);
        pageSizeCombo.valueProperty().addListener((o, old, n) -> {
            pageSize = n;
            resetAndLoad();
        });
    }

    // ── Colonnes ──────────────────────────────────────────────
    private void setupColumns() {
        colId.setCellValueFactory(c ->
                new SimpleStringProperty(String.valueOf(c.getValue().getId())));
        colNom.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getNom()));
        colType.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getType()));
        colStock.setCellValueFactory(c ->
                new SimpleStringProperty(String.format("%.2f %s", 
                        c.getValue().getStockRestant(), 
                        c.getValue().getUnite())));
        colAgriculteur.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getUserName() != null ? c.getValue().getUserName() : "Inconnu"));

        // Responsive columns logic could be added here similar to AdminUsersController
        ressourcesTable.widthProperty().addListener((obs, oldW, newW) -> {
            double total     = newW.doubleValue() - 18;
            double fixed     = 50 + 150 + 150; // id + type + stock
            double remaining = total - fixed;
            colNom.setPrefWidth(remaining * 0.55);
            colAgriculteur.setPrefWidth(remaining * 0.45);
        });
    }

    // ── Charger la page ───────────────────────────────────────
    private void loadPage() {
        String keyword = searchField.getText();

        new Thread(() -> {
            try {
                totalCount = ressourceService.countRessources(keyword, "Tous");
                totalPages = (int) Math.ceil((double) totalCount / pageSize);
                if (totalPages == 0) totalPages = 1;
                if (currentPage > totalPages) currentPage = totalPages;

                List<Ressource> ressources = ressourceService.searchRessourcesPaged(
                        keyword, "Tous", currentPage, pageSize);

                Platform.runLater(() -> {
                    ressourcesTable.setItems(FXCollections.observableArrayList(ressources));
                    updatePaginationUI();
                });
            } catch (Exception e) {
                Platform.runLater(() ->
                        totalLabel.setText("Erreur : " + e.getMessage()));
            }
        }).start();
    }

    // ── Pagination UI ─────────────────────────────────────────
    private void updatePaginationUI() {
        int from = totalCount == 0 ? 0 : (currentPage - 1) * pageSize + 1;
        int to   = Math.min(currentPage * pageSize, totalCount);

        totalLabel.setText("Total : " + totalCount + " ressources");
        pageInfoLabel.setText(from + " – " + to + " sur " + totalCount
                + "   |   Page " + currentPage + " / " + totalPages);

        btnFirst.setDisable(currentPage == 1);
        btnPrev.setDisable(currentPage == 1);
        btnNext.setDisable(currentPage == totalPages);
        btnLast.setDisable(currentPage == totalPages);

        buildPageButtons();
    }

    private void buildPageButtons() {
        paginationBox.getChildren().removeIf(n ->
                n instanceof Button b && b.getStyleClass().contains("page-number-btn"));

        int start = Math.max(1, currentPage - 2);
        int end   = Math.min(totalPages, start + 4);
        start     = Math.max(1, end - 4);

        for (int i = start; i <= end; i++) {
            final int page = i;
            Button btn = new Button(String.valueOf(i));
            btn.getStyleClass().add("page-number-btn");
            btn.setStyle(i == currentPage
                    ? "-fx-background-color:#2ecc71; -fx-text-fill:white;" +
                      "-fx-font-weight:bold; -fx-padding:5 10; -fx-background-radius:4;"
                    : "-fx-background-color:white; -fx-text-fill:#333;" +
                      "-fx-padding:5 10; -fx-background-radius:4;" +
                      "-fx-border-color:#ddd; -fx-border-radius:4;");
            btn.setOnAction(e -> { currentPage = page; loadPage(); });
            paginationBox.getChildren().add(
                    paginationBox.getChildren().indexOf(btnNext), btn);
        }
    }

    @FXML public void goFirst() { currentPage = 1;                              loadPage(); }
    @FXML public void goPrev()  { if (currentPage > 1)          { currentPage--; loadPage(); } }
    @FXML public void goNext()  { if (currentPage < totalPages) { currentPage++; loadPage(); } }
    @FXML public void goLast()  { currentPage = totalPages;                     loadPage(); }

    @FXML
    public void resetFilters() {
        searchField.clear();
    }

    private void resetAndLoad() { currentPage = 1; loadPage(); }
}
