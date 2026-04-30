package controllers;

import entities.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import services.GoogleCalendarService;
import services.TaskService;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class CalendarController {

    @FXML private Label monthYearLabel;
    @FXML private GridPane calendarGrid;

    private YearMonth currentYearMonth;
    private final TaskService taskService = new TaskService();
    private final GoogleCalendarService googleCalendarService = new GoogleCalendarService();

    @FXML
    public void initialize() {
        currentYearMonth = YearMonth.now();
        drawCalendar();
    }

    private void drawCalendar() {
        calendarGrid.getChildren().clear();
        monthYearLabel.setText(currentYearMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.FRENCH) + " " + currentYearMonth.getYear());

        LocalDate firstOfMonth = currentYearMonth.atDay(1);
        int dayOfWeekValue = firstOfMonth.getDayOfWeek().getValue(); // 1 = Monday, 7 = Sunday
        int daysInMonth = currentYearMonth.lengthOfMonth();

        int row = 0;
        int col = dayOfWeekValue - 1;

        try {
            List<Task> allTasks = taskService.getAll();
            List<com.google.api.services.calendar.model.Event> googleEvents = null;
            try {
                long timeMin = currentYearMonth.atDay(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
                long timeMax = currentYearMonth.atEndOfMonth().atTime(23, 59, 59).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
                googleEvents = googleCalendarService.getEvents(timeMin, timeMax);
            } catch (Exception e) {
                System.out.println("Could not fetch Google Events: " + e.getMessage());
            }

            for (int day = 1; day <= daysInMonth; day++) {
                LocalDate date = currentYearMonth.atDay(day);
                VBox dayCell = createDayCell(day, date, allTasks, googleEvents);
                calendarGrid.add(dayCell, col, row);

                col++;
                if (col > 6) {
                    col = 0;
                    row++;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private VBox createDayCell(int day, LocalDate date, List<Task> tasks, List<com.google.api.services.calendar.model.Event> googleEvents) {
        VBox cell = new VBox(5);
        cell.getStyleClass().add("calendar-day-cell");
        if (date.equals(LocalDate.now())) {
            cell.getStyleClass().add("today");
        }

        Label dayLabel = new Label(String.valueOf(day));
        dayLabel.getStyleClass().add("calendar-day-number");
        cell.getChildren().add(dayLabel);

        // Local Tasks
        List<Task> tasksForDay = tasks.stream()
                .filter(t -> t.getDateDebut() != null && t.getDateDebut().toLocalDate().equals(date))
                .collect(Collectors.toList());

        for (Task task : tasksForDay) {
            Label taskLabel = new Label(task.getTitre());
            taskLabel.getStyleClass().add("calendar-task-label");
            taskLabel.setCursor(javafx.scene.Cursor.HAND);
            taskLabel.setOnMouseClicked(e -> syncSpecificTask(task));
            
            String status = task.getStatut();
            if ("termine".equalsIgnoreCase(status)) {
                taskLabel.setStyle("-fx-background-color: #28a745; -fx-text-fill: white;");
            } else if ("en_cours".equalsIgnoreCase(status)) {
                taskLabel.setStyle("-fx-background-color: #0d6efd; -fx-text-fill: white;");
            } else {
                taskLabel.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white;");
            }
            cell.getChildren().add(taskLabel);
        }

        // Google Events
        if (googleEvents != null) {
            for (com.google.api.services.calendar.model.Event gEvent : googleEvents) {
                LocalDate eventDate = null;
                if (gEvent.getStart() != null) {
                    if (gEvent.getStart().getDateTime() != null) {
                        eventDate = LocalDate.ofInstant(
                            java.time.Instant.ofEpochMilli(gEvent.getStart().getDateTime().getValue()), 
                            java.time.ZoneId.systemDefault());
                    } else if (gEvent.getStart().getDate() != null) {
                        eventDate = LocalDate.parse(gEvent.getStart().getDate().toString());
                    }
                }
                
                if (eventDate != null && eventDate.equals(date)) {
                    Label gLabel = new Label("G: " + gEvent.getSummary());
                    gLabel.getStyleClass().add("calendar-task-label");
                    gLabel.setStyle("-fx-background-color: #FFC107; -fx-text-fill: black;"); // Gold for Google
                    cell.getChildren().add(gLabel);
                }
            }
        }

        return cell;
    }

    @FXML
    void nextMonth(ActionEvent event) {
        currentYearMonth = currentYearMonth.plusMonths(1);
        drawCalendar();
    }

    @FXML
    void previousMonth(ActionEvent event) {
        currentYearMonth = currentYearMonth.minusMonths(1);
        drawCalendar();
    }

    @FXML
    void syncWithGoogle(ActionEvent event) {
        showAlert("Info", "Pour synchroniser une tâche spécifique, cliquez simplement sur son nom dans le calendrier !");
    }

    private void syncSpecificTask(Task task) {
        try {
            String url = googleCalendarService.generateGoogleCalendarUrl(task);
            if (url != null) {
                if (java.awt.Desktop.isDesktopSupported() && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.BROWSE)) {
                    java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
                } else {
                    showAlert("Erreur", "Impossible d'ouvrir le navigateur.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    void goBack(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/Views/TachesView.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
