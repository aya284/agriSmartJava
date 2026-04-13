package controllers.admin;

import entities.User;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import services.UserService;

import java.util.List;

public class AdminUsersController {

    // ── Filtres ───────────────────────────────────────────────
    @FXML private TextField          searchField;
    @FXML private ComboBox<String>   roleFilter;
    @FXML private ComboBox<String>   statusFilter;
    @FXML private Label              totalLabel;

    // ── Tableau ───────────────────────────────────────────────
    @FXML private TableView<User>              usersTable;
    @FXML private TableColumn<User, String>    colId;
    @FXML private TableColumn<User, String>    colName;
    @FXML private TableColumn<User, String>    colEmail;
    @FXML private TableColumn<User, String>    colRole;
    @FXML private TableColumn<User, String>    colStatus;
    @FXML private TableColumn<User, String>    colDate;
    @FXML private TableColumn<User, Void>      colActions;

    private final UserService userService = new UserService();

    // ─────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        setupFilters();
        setupColumns();
        loadUsers();
    }

    // ── Initialiser les filtres ───────────────────────────────
    private void setupFilters() {
        roleFilter.getItems().addAll("Tous", "admin", "agriculteur", "fournisseur", "employee");
        roleFilter.setValue("Tous");

        statusFilter.getItems().addAll("Tous", "active", "inactive", "pending");
        statusFilter.setValue("Tous");

        // Recherche en temps réel
        searchField.textProperty().addListener((obs, o, n) -> applyFilters());
        roleFilter.valueProperty().addListener((obs, o, n) -> applyFilters());
        statusFilter.valueProperty().addListener((obs, o, n) -> applyFilters());
    }

    // ── Configurer les colonnes ───────────────────────────────
    private void setupColumns() {
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
                                ? c.getValue().getCreatedAt().toLocalDate().toString()
                                : "-"));

        // Colonne statut avec badge coloré
        colStatus.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getStatus()));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) { setGraphic(null); return; }
                Label badge = new Label(status.toUpperCase());
                badge.setStyle(getBadgeStyle(status));
                setGraphic(badge);
                setText(null);
            }
        });

        // Colonne actions
        colActions.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }

                User user = getTableView().getItems().get(getIndex());
                HBox box  = new HBox(6);

                Button btnActive   = makeBtn("Activer",    "#27ae60", "active",   user);
                Button btnInactive = makeBtn("Désactiver", "#e67e22", "inactive", user);
                Button btnPending  = makeBtn("En attente", "#95a5a6", "pending",  user);

                // Griser le bouton du statut actuel
                highlight(btnActive,   user.getStatus().equals("active"));
                highlight(btnInactive, user.getStatus().equals("inactive"));
                highlight(btnPending,  user.getStatus().equals("pending"));

                box.getChildren().addAll(btnActive, btnInactive, btnPending);
                setGraphic(box);
            }

            private Button makeBtn(String label, String color,
                                   String targetStatus, User user) {
                Button btn = new Button(label);
                btn.setStyle("-fx-background-color:" + color + ";" +
                        "-fx-text-fill:white; -fx-font-size:11;" +
                        "-fx-padding: 3 8; -fx-background-radius:4;");
                btn.setOnAction(e -> handleStatusChange(user, targetStatus));
                return btn;
            }

            private void highlight(Button btn, boolean isCurrent) {
                if (isCurrent) btn.setOpacity(0.4);
            }
        });
    }

    // ── Charger les utilisateurs ──────────────────────────────
    private void loadUsers() {
        new Thread(() -> {
            try {
                List<User> users = userService.getAllUsers();
                Platform.runLater(() -> {
                    usersTable.setItems(FXCollections.observableArrayList(users));
                    totalLabel.setText("Total : " + users.size() + " utilisateurs");
                });
            } catch (Exception e) {
                Platform.runLater(() ->
                        totalLabel.setText("Erreur de chargement : " + e.getMessage()));
            }
        }).start();
    }

    // ── Appliquer les filtres ─────────────────────────────────
    @FXML
    public void applyFilters() {
        String keyword = searchField.getText();
        String role    = roleFilter.getValue();
        String status  = statusFilter.getValue();

        new Thread(() -> {
            try {
                List<User> users = userService.searchUsers(keyword, role, status);
                Platform.runLater(() -> {
                    usersTable.setItems(FXCollections.observableArrayList(users));
                    totalLabel.setText("Total : " + users.size() + " utilisateurs");
                });
            } catch (Exception e) {
                Platform.runLater(() ->
                        totalLabel.setText("Erreur : " + e.getMessage()));
            }
        }).start();
    }

    // ── Changer le statut ─────────────────────────────────────
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
                        user.setStatus(newStatus);
                        Platform.runLater(() -> usersTable.refresh());
                    } catch (Exception e) {
                        Platform.runLater(() -> {
                            Alert err = new Alert(Alert.AlertType.ERROR,
                                    "Erreur : " + e.getMessage());
                            err.show();
                        });
                    }
                }).start();
            }
        });
    }

    // ── Style badge statut ────────────────────────────────────
    private String getBadgeStyle(String status) {
        String color = switch (status.toLowerCase()) {
            case "active"   -> "#27ae60";
            case "inactive" -> "#e74c3c";
            case "pending"  -> "#f39c12";
            default         -> "#95a5a6";
        };
        return "-fx-background-color:" + color + ";" +
                "-fx-text-fill:white; -fx-font-size:10;" +
                "-fx-padding:2 8; -fx-background-radius:10;";
    }

    // ── Réinitialiser les filtres ─────────────────────────────
    @FXML
    public void resetFilters() {
        searchField.clear();
        roleFilter.setValue("Tous");
        statusFilter.setValue("Tous");
    }
}