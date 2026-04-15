package controllers.admin;

import entities.Commande;
import entities.Produit;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.geometry.Side;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.geometry.Pos;
import services.CommandeService;
import services.ProduitService;

import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.time.temporal.WeekFields;

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
    @FXML private ComboBox<String> periodCombo;
    @FXML private LineChart<String, Number> kpiTrendChart;
    @FXML private LineChart<String, Number> revenueTrendChart;

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
    private final CommandeService commandeService = new CommandeService();
    private List<Produit> allProducts = new ArrayList<>();
    private static final String PERIOD_DAILY = "Daily";
    private static final String PERIOD_WEEKLY = "Weekly";
    private static final String PERIOD_MONTHLY = "Monthly";
    private static final DateTimeFormatter DAILY_LABEL_FORMAT = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter MONTHLY_LABEL_FORMAT = DateTimeFormatter.ofPattern("MM/yy");

    @FXML
    public void initialize() {
        setupTable();
        setupPeriodControl();
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
    public void handlePeriodChange() {
        try {
            refreshKpiTrends();
        } catch (SQLException e) {
            showError("Erreur tendances KPI", e.getMessage());
        }
    }

    @FXML
    public void refreshAll() {
        try {
            allProducts = produitService.afficherTous();
            applyFilter();
            refreshStats();
            refreshCharts();
            refreshKpiTrends();
        } catch (SQLException e) {
            showError("Erreur chargement produits", e.getMessage());
        }
    }

    private void setupPeriodControl() {
        if (periodCombo == null) {
            return;
        }
        periodCombo.getItems().setAll(PERIOD_DAILY, PERIOD_WEEKLY, PERIOD_MONTHLY);
        periodCombo.setValue(PERIOD_DAILY);
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

    private void refreshKpiTrends() throws SQLException {
        if (kpiTrendChart == null || revenueTrendChart == null) {
            return;
        }

        String period = periodCombo == null || periodCombo.getValue() == null
                ? PERIOD_DAILY
                : periodCombo.getValue();
        int days = daysForPeriod(period);

        LinkedHashMap<String, KpiAccumulator> buckets = initBuckets(period, days);
        List<Commande> commandes = commandeService.getRecentSinceDays(days);

        for (Commande commande : commandes) {
            LocalDateTime createdAt = commande.getCreatedAt();
            if (createdAt == null) {
                continue;
            }
            String bucket = resolveBucketLabel(createdAt.toLocalDate(), period);
            KpiAccumulator acc = buckets.get(bucket);
            if (acc == null) {
                continue;
            }

            acc.orders += 1;
            acc.revenue += Math.max(0.0, commande.getMontantTotal());

            String status = safe(commande.getStatut()).toLowerCase(Locale.ROOT);
            if (status.contains("annul") || status.contains("cancel")) {
                acc.cancellations += 1;
            }
            if (status.contains("echec") || status.contains("failed")) {
                acc.failedPayments += 1;
            }
        }

        XYChart.Series<String, Number> ordersSeries = new XYChart.Series<>();
        ordersSeries.setName("Orders");
        XYChart.Series<String, Number> cancellationsSeries = new XYChart.Series<>();
        cancellationsSeries.setName("Cancellations");
        XYChart.Series<String, Number> failedSeries = new XYChart.Series<>();
        failedSeries.setName("Failed Payments");
        XYChart.Series<String, Number> conversionSeries = new XYChart.Series<>();
        conversionSeries.setName("Conversion %");

        XYChart.Series<String, Number> revenueSeries = new XYChart.Series<>();
        revenueSeries.setName("Revenue TND");

        for (Map.Entry<String, KpiAccumulator> entry : buckets.entrySet()) {
            String label = entry.getKey();
            KpiAccumulator acc = entry.getValue();

            ordersSeries.getData().add(new XYChart.Data<>(label, acc.orders));
            cancellationsSeries.getData().add(new XYChart.Data<>(label, acc.cancellations));
            failedSeries.getData().add(new XYChart.Data<>(label, acc.failedPayments));

            double successful = Math.max(0, acc.orders - acc.cancellations - acc.failedPayments);
            double conversion = acc.orders == 0 ? 0.0 : (successful * 100.0) / acc.orders;
            conversionSeries.getData().add(new XYChart.Data<>(label, Math.min(100.0, Math.max(0.0, conversion))));

            revenueSeries.getData().add(new XYChart.Data<>(label, acc.revenue));
        }

        kpiTrendChart.getData().setAll(ordersSeries, cancellationsSeries, failedSeries, conversionSeries);
        revenueTrendChart.getData().setAll(revenueSeries);
        kpiTrendChart.setLegendVisible(true);
        revenueTrendChart.setLegendVisible(true);
    }

    private int daysForPeriod(String period) {
        if (PERIOD_MONTHLY.equals(period)) {
            return 365;
        }
        if (PERIOD_WEEKLY.equals(period)) {
            return 84;
        }
        return 30;
    }

    private LinkedHashMap<String, KpiAccumulator> initBuckets(String period, int days) {
        LinkedHashMap<String, KpiAccumulator> buckets = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();

        if (PERIOD_MONTHLY.equals(period)) {
            YearMonth current = YearMonth.from(today);
            for (int i = 11; i >= 0; i--) {
                YearMonth ym = current.minusMonths(i);
                buckets.put(ym.format(MONTHLY_LABEL_FORMAT), new KpiAccumulator());
            }
            return buckets;
        }

        if (PERIOD_WEEKLY.equals(period)) {
            LocalDate weekStart = today.with(DayOfWeek.MONDAY);
            for (int i = 11; i >= 0; i--) {
                LocalDate start = weekStart.minusWeeks(i);
                int week = start.get(WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear());
                buckets.put("W" + week, new KpiAccumulator());
            }
            return buckets;
        }

        LocalDate start = today.minusDays(days - 1L);
        for (int i = 0; i < days; i++) {
            LocalDate day = start.plusDays(i);
            buckets.put(day.format(DAILY_LABEL_FORMAT), new KpiAccumulator());
        }
        return buckets;
    }

    private String resolveBucketLabel(LocalDate date, String period) {
        if (date == null) {
            return "-";
        }
        if (PERIOD_MONTHLY.equals(period)) {
            return YearMonth.from(date).format(MONTHLY_LABEL_FORMAT);
        }
        if (PERIOD_WEEKLY.equals(period)) {
            int week = date.with(DayOfWeek.MONDAY).get(WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear());
            return "W" + week;
        }
        return date.format(DAILY_LABEL_FORMAT);
    }

    private static final class KpiAccumulator {
        int orders;
        int cancellations;
        int failedPayments;
        double revenue;
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
