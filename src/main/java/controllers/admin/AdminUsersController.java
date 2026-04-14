package controllers.admin;

import entities.User;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import services.UserService;

import java.util.List;

public class AdminUsersController {

    // ── Filtres ───────────────────────────────────────────────
    @FXML private TextField        searchField;
    @FXML private ComboBox<String> roleFilter;
    @FXML private ComboBox<String> statusFilter;
    @FXML private Label            totalLabel;

    // ── Tableau ───────────────────────────────────────────────
    @FXML private TableView<User>           usersTable;
    @FXML private TableColumn<User, String> colId;
    @FXML private TableColumn<User, String> colName;
    @FXML private TableColumn<User, String> colEmail;
    @FXML private TableColumn<User, String> colRole;
    @FXML private TableColumn<User, String> colStatus;
    @FXML private TableColumn<User, String> colDate;
    @FXML private TableColumn<User, Void>   colActions;

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
    private int pageSize    = 10;
    private int totalPages  = 1;
    private int totalCount  = 0;

    private final UserService userService = new UserService();

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
        roleFilter.getItems().addAll("Tous", "admin", "agriculteur",
                "fournisseur", "employee");
        roleFilter.setValue("Tous");

        statusFilter.getItems().addAll("Tous", "active", "inactive", "pending");
        statusFilter.setValue("Tous");

        searchField.textProperty().addListener((o, old, n) -> resetAndLoad());
        roleFilter.valueProperty().addListener((o, old, n)  -> resetAndLoad());
        statusFilter.valueProperty().addListener((o, old, n) -> resetAndLoad());
    }

    // ── Taille de page ────────────────────────────────────────
    private void setupPageSizeCombo() {
        pageSizeCombo.getItems().addAll(5, 10, 20, 50);
        pageSizeCombo.setValue(10);
        pageSizeCombo.valueProperty().addListener((o, old, n) -> {
            pageSize = n;
            resetAndLoad();
        });
    }

    // ── Colonnes ──────────────────────────────────────────────
    private void setupColumns() {
        // Responsive — distribue l'espace restant sur Nom et Email
        usersTable.widthProperty().addListener((obs, oldW, newW) -> {
            double total     = newW.doubleValue() - 18; // 18px scrollbar
            double fixed     = 50 + 130 + 120 + 120 + 90; // id+role+statut+date+actions
            double remaining = total - fixed;
            colName.setPrefWidth(remaining * 0.38);
            colEmail.setPrefWidth(remaining * 0.62);
        });

        colId.setCellValueFactory(c ->
                new SimpleStringProperty(String.valueOf(c.getValue().getId())));
        colName.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getFullName()));
        colEmail.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getEmail()));
        colRole.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getRole()));
        colDate.setCellValueFactory(c ->
                new SimpleStringProperty(
                        c.getValue().getCreatedAt() != null
                                ? c.getValue().getCreatedAt().toLocalDate().toString() : "-"));

        setupStatusColumn();
        setupActionsColumn();
    }

    // ── Colonne Statut — badge cliquable avec dropdown ────────
    private void setupStatusColumn() {
        colStatus.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getStatus()));

        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) { setGraphic(null); return; }

                User user = getTableView().getItems().get(getIndex());

                Label badge = new Label(status.toUpperCase());
                badge.setStyle(getBadgeStyle(status) + " -fx-cursor: hand;");

                ContextMenu menu = new ContextMenu();
                MenuItem itemActive   = makeMenuItem("✅  Activer",    "active",   user);
                MenuItem itemInactive = makeMenuItem("🚫  Désactiver", "inactive", user);
                MenuItem itemPending  = makeMenuItem("⏳  En attente", "pending",  user);

                itemActive.setDisable(status.equals("active"));
                itemInactive.setDisable(status.equals("inactive"));
                itemPending.setDisable(status.equals("pending"));

                menu.getItems().addAll(itemActive, itemInactive, itemPending);
                badge.setOnMouseClicked(e ->
                        menu.show(badge, e.getScreenX(), e.getScreenY()));

                setGraphic(badge);
                setText(null);
            }

            private MenuItem makeMenuItem(String label, String targetStatus, User user) {
                MenuItem item = new MenuItem(label);
                item.setOnAction(e -> handleStatusChange(user, targetStatus));
                return item;
            }
        });
    }

    // ── Colonne Actions — bouton View uniquement ──────────────
    private void setupActionsColumn() {
        colActions.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }

                User user = getTableView().getItems().get(getIndex());

                Button btnView = new Button("👁  Voir");
                btnView.setStyle(
                        "-fx-background-color: #3498db;" +
                                "-fx-text-fill: white;" +
                                "-fx-font-size: 11;" +
                                "-fx-padding: 4 10;" +
                                "-fx-background-radius: 4;" +
                                "-fx-cursor: hand;");
                btnView.setOnAction(e -> openUserDetail(user));
                setGraphic(btnView);
            }
        });
    }

    // ── Ouvrir la page détail ─────────────────────────────────
    private void openUserDetail(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/Views/admin/AdminUserDetailView.fxml"));
            Node view = loader.load();

            AdminUserDetailController ctrl = loader.getController();
            ctrl.setUser(user, this::loadPage);

            StackPane contentArea = (StackPane) usersTable
                    .getScene().lookup("#adminContentArea");
            contentArea.getChildren().setAll(view);

        } catch (Exception e) {
            System.err.println("Erreur ouverture détail : " + e.getMessage());
        }
    }

    // ── Charger la page ───────────────────────────────────────
    private void loadPage() {
        String keyword = searchField.getText();
        String role    = roleFilter.getValue();
        String status  = statusFilter.getValue();

        new Thread(() -> {
            try {
                totalCount = userService.countUsers(keyword, role, status);
                totalPages = (int) Math.ceil((double) totalCount / pageSize);
                if (totalPages == 0) totalPages = 1;
                if (currentPage > totalPages) currentPage = totalPages;

                List<User> users = userService.searchUsersPaged(
                        keyword, role, status, currentPage, pageSize);

                Platform.runLater(() -> {
                    usersTable.setItems(FXCollections.observableArrayList(users));
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

        totalLabel.setText("Total : " + totalCount + " utilisateurs");
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
        roleFilter.setValue("Tous");
        statusFilter.setValue("Tous");
    }

    private void resetAndLoad() { currentPage = 1; loadPage(); }

    // ── Changement statut ─────────────────────────────────────
    private void handleStatusChange(User user, String newStatus) {
        if (user.getStatus().equals(newStatus)) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer");
        confirm.setHeaderText("Changer le statut de " + user.getFullName());
        confirm.setContentText("Passer à : " + newStatus.toUpperCase() + " ?");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                new Thread(() -> {
                    try {
                        userService.updateStatus(user.getId(), newStatus);
                        Platform.runLater(this::loadPage);
                    } catch (Exception e) {
                        Platform.runLater(() ->
                                new Alert(Alert.AlertType.ERROR,
                                        "Erreur : " + e.getMessage()).show());
                    }
                }).start();
            }
        });
    }

    // ── Badge style ───────────────────────────────────────────
    private String getBadgeStyle(String status) {
        String color = switch (status.toLowerCase()) {
            case "active"   -> "#27ae60";
            case "inactive" -> "#e74c3c";
            case "pending"  -> "#f39c12";
            default         -> "#95a5a6";
        };
        return "-fx-background-color:" + color + "; -fx-text-fill:white;" +
                "-fx-font-size:10; -fx-padding:3 10; -fx-background-radius:10;";
    }
}