package controllers.admin;

import entities.User;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import services.UserService;

import java.util.List;

public class AdminDashboardController {

    @FXML private Label    totalUsersLabel;
    @FXML private Label    activeUsersLabel;
    @FXML private Label    inactiveUsersLabel;
    @FXML private Label    pendingUsersLabel;
    @FXML private Label    countAgriculteur;
    @FXML private Label    countFournisseur;
    @FXML private Label    countEmployee;
    @FXML private Label    countAdmin;
    @FXML private PieChart statusPieChart;
    @FXML private BarChart<String, Number> weeklyBarChart;
    @FXML private ListView<String> recentUsersList;

    private final UserService userService = new UserService();

    @FXML
    public void initialize() {
        loadStats();
    }

    private void loadStats() {
        new Thread(() -> {
            try {
                List<User> all = userService.getAllUsers();

                long active   = all.stream()
                        .filter(u -> "active".equalsIgnoreCase(u.getStatus())).count();
                long inactive = all.stream()
                        .filter(u -> "inactive".equalsIgnoreCase(u.getStatus())).count();
                long pending  = all.stream()
                        .filter(u -> "pending".equalsIgnoreCase(u.getStatus())).count();

                long agriculteurs = all.stream()
                        .filter(u -> "agriculteur".equalsIgnoreCase(u.getRole())).count();
                long fournisseurs = all.stream()
                        .filter(u -> "fournisseur".equalsIgnoreCase(u.getRole())).count();
                long employees    = all.stream()
                        .filter(u -> "employee".equalsIgnoreCase(u.getRole())).count();
                long admins       = all.stream()
                        .filter(u -> "admin".equalsIgnoreCase(u.getRole())).count();

                // 5 derniers inscrits
                List<String> recent = all.stream()
                        .limit(5)
                        .map(u -> u.getFullName() + "  —  " + u.getRole()
                                + "  [" + u.getStatus() + "]")
                        .toList();

                Platform.runLater(() -> {
                    totalUsersLabel.setText(String.valueOf(all.size()));
                    activeUsersLabel.setText(String.valueOf(active));
                    inactiveUsersLabel.setText(String.valueOf(inactive));
                    pendingUsersLabel.setText(String.valueOf(pending));
                    countAgriculteur.setText(String.valueOf(agriculteurs));
                    countFournisseur.setText(String.valueOf(fournisseurs));
                    countEmployee.setText(String.valueOf(employees));
                    countAdmin.setText(String.valueOf(admins));
                    loadCharts(active, inactive, pending);
                    recentUsersList.getItems().setAll(recent);
                });

            } catch (Exception e) {
                Platform.runLater(() ->
                        totalUsersLabel.setText("Erreur : " + e.getMessage()));
            }
        }).start();
    }

    private void loadCharts(long active, long inactive, long pending) {
        if (statusPieChart != null) {
            statusPieChart.getData().setAll(
                    new PieChart.Data("Actifs", active),
                    new PieChart.Data("Inactifs", inactive),
                    new PieChart.Data("En attente", pending)
            );
        }

        if (weeklyBarChart != null) {
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Inscriptions");
            series.getData().add(new XYChart.Data<>("Lun", 8));
            series.getData().add(new XYChart.Data<>("Mar", 11));
            series.getData().add(new XYChart.Data<>("Mer", 7));
            series.getData().add(new XYChart.Data<>("Jeu", 14));
            series.getData().add(new XYChart.Data<>("Ven", 10));
            series.getData().add(new XYChart.Data<>("Sam", 5));
            series.getData().add(new XYChart.Data<>("Dim", 4));
            weeklyBarChart.getData().setAll(series);
        }
    }
}