package controllers.admin;

import entities.Task;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import services.TaskService;

import javafx.scene.chart.PieChart;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AdminTaskController {

    @FXML private Label totalTasksLabel;
    @FXML private Label completedTasksLabel;
    @FXML private Label inProgressTasksLabel;
    @FXML private Label highPriorityTasksLabel;
    @FXML private PieChart statusChart;

    private final TaskService taskService = new TaskService();

    @FXML
    public void initialize() {
        loadStats();
    }

    @FXML
    public void loadStats() {
        new Thread(() -> {
            try {
                List<Task> allTasks = taskService.getAll();

                long total = allTasks.size();
                long completed = allTasks.stream()
                        .filter(t -> "termine".equalsIgnoreCase(t.getStatut()) || "realisee".equalsIgnoreCase(t.getStatut())).count();
                long inProgress = allTasks.stream()
                        .filter(t -> "en_cours".equalsIgnoreCase(t.getStatut()) || "assignee".equalsIgnoreCase(t.getStatut())).count();
                long highPriority = allTasks.stream()
                        .filter(t -> "haute".equalsIgnoreCase(t.getPriorite()) || "high".equalsIgnoreCase(t.getPriorite())).count();

                // Group by status for the chart
                Map<String, Long> statusCounts = allTasks.stream()
                        .collect(Collectors.groupingBy(
                                t -> t.getStatut() == null ? "Inconnu" : t.getStatut(), 
                                Collectors.counting()
                        ));

                Platform.runLater(() -> {
                    totalTasksLabel.setText(String.valueOf(total));
                    completedTasksLabel.setText(String.valueOf(completed));
                    inProgressTasksLabel.setText(String.valueOf(inProgress));
                    highPriorityTasksLabel.setText(String.valueOf(highPriority));

                    statusChart.getData().clear();
                    statusCounts.forEach((status, count) -> {
                        statusChart.getData().add(new PieChart.Data(status + " (" + count + ")", count));
                    });
                });

            } catch (SQLException e) {
                e.printStackTrace();
                Platform.runLater(() -> totalTasksLabel.setText("Error"));
            }
        }).start();
    }

    @FXML
    public void openFullTaskList() {
        AdminLayoutController layout = AdminLayoutController.getInstance();
        if (layout != null) {
            layout.navigateTo("/Views/TachesView.fxml", "Gestion des Tâches", "Admin / Liste des Tâches");
        }
    }
}
