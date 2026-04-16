package controllers;

import entities.SuiviTache;
import entities.Task;
import entities.TaskAssignment;
import entities.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.util.StringConverter;
import services.SuiviTacheService;
import services.TaskAssignmentService;
import services.TaskService;
import services.UserService;
import utils.SessionManager;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class TaskAssignmentController {

    @FXML
    private StackPane rootPane;

    @FXML
    private ScrollPane farmerView;
    @FXML
    private ScrollPane employeeView;

    // ── Farmer Fields ──
    @FXML
    private ComboBox<Task> taskComboBox;
    @FXML
    private ComboBox<User> workerComboBox;
    @FXML
    private javafx.scene.control.DatePicker dateAssignmentPicker;
    @FXML
    private ComboBox<String> statutComboBox;

    @FXML
    private TableView<TaskAssignment> assignmentTable;
    @FXML
    private TableColumn<TaskAssignment, Integer> colId;
    @FXML
    private TableColumn<TaskAssignment, String> colTask;
    @FXML
    private TableColumn<TaskAssignment, String> colWorker;
    @FXML
    private TableColumn<TaskAssignment, LocalDateTime> colDate;
    @FXML
    private TableColumn<TaskAssignment, String> colStatut;

    // Farmer stat labels
    @FXML
    private Label statTotalLabel;
    @FXML
    private Label statAcceptedLabel;
    @FXML
    private Label statDoneLabel;
    @FXML
    private Label statPendingLabel;

    // ── Employee Fields ──
    @FXML
    private TableView<TaskAssignment> employeeTable;
    @FXML
    private TableColumn<TaskAssignment, Integer> colEmpId;
    @FXML
    private TableColumn<TaskAssignment, String> colEmpTask;
    @FXML
    private TableColumn<TaskAssignment, LocalDateTime> colEmpDate;
    @FXML
    private TableColumn<TaskAssignment, String> colEmpStatut;
    @FXML
    private Button markDoneBtn;

    // Employee stat labels
    @FXML
    private Label empStatTotalLabel;
    @FXML
    private Label empStatDoneLabel;
    @FXML
    private Label empStatPendingLabel;

    private final TaskService taskService = new TaskService();
    private final UserService userService = new UserService();
    private final TaskAssignmentService taskAssignmentService = new TaskAssignmentService();
    private final SuiviTacheService suiviTacheService = new SuiviTacheService();

    private final ObservableList<Task> taskList = FXCollections.observableArrayList();
    private final ObservableList<User> workerList = FXCollections.observableArrayList();
    private final ObservableList<TaskAssignment> assignmentList = FXCollections.observableArrayList();
    private final ObservableList<TaskAssignment> employeeAssignmentList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        User currentUser = SessionManager.getInstance().getCurrentUser();

        if (currentUser != null && "agriculteur".equals(currentUser.getRole())) {
            initializeFarmerView();
            farmerView.setVisible(true);
            employeeView.setVisible(false);
            loadFarmerData();
        } else if (currentUser != null) {
            initializeEmployeeView();
            employeeView.setVisible(true);
            farmerView.setVisible(false);
            loadEmployeeData();
        } else {
            showWarning("Session Error", "No active user session. Please log in.");
            farmerView.setVisible(false);
            employeeView.setVisible(false);
        }
    }

    private void initializeFarmerView() {
        statutComboBox.setItems(FXCollections.observableArrayList("assignee", "acceptee", "realisee"));

        configureCombos();
        initializeFarmerTable();
        setupFarmerListeners();
    }

    private void initializeEmployeeView() {
        initializeEmployeeTable();
        setupEmployeeListeners();
    }

    private void configureCombos() {
        // Task ComboBox
        taskComboBox.setItems(taskList);
        taskComboBox.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(Task item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getIdTask() + " - " + item.getTitre());
            }
        });
        taskComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Task task) {
                return task == null ? "" : task.getIdTask() + " - " + task.getTitre();
            }

            @Override
            public Task fromString(String string) {
                return null;
            }
        });

        // Worker ComboBox
        workerComboBox.setItems(workerList);
        workerComboBox.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(User item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getFullName());
            }
        });
        workerComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(User user) {
                return user == null ? "" : user.getFullName();
            }

            @Override
            public User fromString(String string) {
                return null;
            }
        });
    }

    private void initializeFarmerTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("idAssignment"));
        colTask.setCellValueFactory(new PropertyValueFactory<>("taskTitre"));
        colWorker.setCellValueFactory(new PropertyValueFactory<>("workerName"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("dateAssignment"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));

        // Colour-coded status badges for the farmer table
        colStatut.setCellFactory(col -> new javafx.scene.control.TableCell<TaskAssignment, String>() {
            private final Label badge = new Label();
            private static final String BASE = "-fx-background-radius: 12; -fx-padding: 3 10; -fx-font-weight: bold; -fx-font-size: 11px; ";

            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setGraphic(null);
                    setText(null);
                } else if ("realisee".equals(status)) {
                    badge.setText("\u23F3 A valider");
                    badge.setStyle(BASE + "-fx-background-color: #FFC107; -fx-text-fill: #5a3e00;");
                    setGraphic(badge);
                    setText(null);
                } else if ("realisea".equals(status)) {
                    badge.setText("\u2705 Realisee");
                    badge.setStyle(BASE + "-fx-background-color: #28a745; -fx-text-fill: #ffffff;");
                    setGraphic(badge);
                    setText(null);
                } else if ("assignee".equals(status)) {
                    badge.setText("\uD83D\uDD34 A faire");
                    badge.setStyle(BASE + "-fx-background-color: #dc3545; -fx-text-fill: #ffffff;");
                    setGraphic(badge);
                    setText(null);
                } else {
                    setGraphic(null);
                    setText(status);
                }
            }
        });

        assignmentTable.setItems(assignmentList);
        assignmentTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Single click → populate form
        assignmentTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                populateFarmerForm(newValue);
            }
        });

        // Double click → open suivi dialog
        assignmentTable.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                TaskAssignment selected = assignmentTable.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    openSuiviDialog(selected);
                }
            }
        });
    }

    private void initializeEmployeeTable() {
        colEmpId.setCellValueFactory(new PropertyValueFactory<>("idAssignment"));
        colEmpTask.setCellValueFactory(new PropertyValueFactory<>("taskTitre"));
        colEmpDate.setCellValueFactory(new PropertyValueFactory<>("dateAssignment"));
        colEmpStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));

        // Colour-coded status labels for the employee table
        colEmpStatut.setCellFactory(col -> new javafx.scene.control.TableCell<TaskAssignment, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else if ("realisee".equals(status)) {
                    setText("\u23F3 En attente de validation");
                    setStyle("-fx-text-fill: #8a6500; -fx-font-weight: bold;");
                } else if ("realisea".equals(status)) {
                    setText("\u2705 Realisee");
                    setStyle("-fx-text-fill: #1a7a34; -fx-font-weight: bold;");
                } else if ("assignee".equals(status)) {
                    setText("\uD83D\uDD34 A faire");
                    setStyle("-fx-text-fill: #b00020; -fx-font-weight: bold;");
                } else {
                    setText(status);
                    setStyle("");
                }
            }
        });

        employeeTable.setItems(employeeAssignmentList);
        employeeTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void setupFarmerListeners() {
        // Buttons are wired via onAction in FXML; double-click is set in
        // initializeFarmerTable
    }

    private void setupEmployeeListeners() {
        markDoneBtn.setOnAction(e -> handleMarkDone());
    }

    @FXML
    private void handleAssign() {
        if (!validateFarmerForm()) {
            return;
        }

        try {
            TaskAssignment assignment = buildTaskAssignmentFromForm();
            taskAssignmentService.add(assignment);
            showInfo("Success", "Task assigned successfully.");
            refreshFarmerData();
            handleClear();
        } catch (SQLException exception) {
            showSqlError(exception);
        }
    }

    @FXML
    private void handleUpdate() {
        TaskAssignment selectedAssignment = assignmentTable.getSelectionModel().getSelectedItem();
        if (selectedAssignment == null) {
            showWarning("Selection Required", "Select an assignment to update.");
            return;
        }

        if (!validateFarmerForm()) {
            return;
        }

        try {
            TaskAssignment assignment = buildTaskAssignmentFromForm();
            assignment.setIdAssignment(selectedAssignment.getIdAssignment());
            taskAssignmentService.update(assignment);
            showInfo("Success", "Assignment updated successfully.");
            refreshFarmerData();
            handleClear();
        } catch (SQLException exception) {
            showSqlError(exception);
        }
    }

    @FXML
    private void handleDelete() {
        TaskAssignment selectedAssignment = assignmentTable.getSelectionModel().getSelectedItem();
        if (selectedAssignment == null) {
            showWarning("Selection Required", "Select an assignment to delete.");
            return;
        }

        if (!confirmDeletion()) {
            return;
        }

        try {
            taskAssignmentService.delete(selectedAssignment.getIdAssignment());
            showInfo("Success", "Assignment deleted successfully.");
            refreshFarmerData();
            handleClear();
        } catch (SQLException exception) {
            showSqlError(exception);
        }
    }

    /** Called when double-clicking a row in the farmer's assignment table. */
    private void openSuiviDialog(TaskAssignment selectedAssignment) {
        try {
            List<SuiviTache> suivis = suiviTacheService.getByTask(selectedAssignment.getTaskId());
            if (suivis.isEmpty()) {
                showInfo("No Details", "No task completion details submitted yet.");
                return;
            }

            // Get the most recent SuiviTache
            SuiviTache suivi = suivis.get(suivis.size() - 1);

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Task Completion Details");
            dialog.setHeaderText("Review employee's task completion");

            GridPane gridPane = new GridPane();
            gridPane.setHgap(10);
            gridPane.setVgap(10);
            gridPane.setPadding(new Insets(20));

            // Title and date info
            Label titleLabel = new Label("Task: " + suivi.getTaskTitre());
            titleLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold;");

            Label dateLabel = new Label("Submitted: " + suivi.getDate());

            gridPane.add(titleLabel, 0, 0, 2, 1);
            gridPane.add(dateLabel, 0, 1, 2, 1);

            // Rendement
            Label rendementLbl = new Label("Rendement / Output:");
            rendementLbl.setStyle("-fx-font-weight: bold;");
            TextArea rendementArea = new TextArea(suivi.getRendement());
            rendementArea.setEditable(false);
            rendementArea.setWrapText(true);
            rendementArea.setPrefRowCount(3);

            // Problemes
            Label problemesLbl = new Label("Problèmes / Issues:");
            problemesLbl.setStyle("-fx-font-weight: bold;");
            TextArea problemesArea = new TextArea(suivi.getProblemes());
            problemesArea.setEditable(false);
            problemesArea.setWrapText(true);
            problemesArea.setPrefRowCount(3);

            // Solution
            Label solutionLbl = new Label("Solution / Actions Taken:");
            solutionLbl.setStyle("-fx-font-weight: bold;");
            TextArea solutionArea = new TextArea(suivi.getSolution());
            solutionArea.setEditable(false);
            solutionArea.setWrapText(true);
            solutionArea.setPrefRowCount(3);

            gridPane.add(rendementLbl, 0, 2);
            gridPane.add(rendementArea, 0, 3, 2, 1);
            gridPane.add(problemesLbl, 0, 4);
            gridPane.add(problemesArea, 0, 5, 2, 1);
            gridPane.add(solutionLbl, 0, 6);
            gridPane.add(solutionArea, 0, 7, 2, 1);

            dialog.getDialogPane().setContent(gridPane);

            // Add Validate, Refuse and Close buttons to the dialog.
            // Refuse uses ButtonData.OTHER so that closing via the X button
            // (which JavaFX maps to the CANCEL_CLOSE button) does NOT trigger refuse.
            ButtonType validateType = new ButtonType("Validate", ButtonBar.ButtonData.OK_DONE);
            ButtonType refuseType = new ButtonType("Refuse", ButtonBar.ButtonData.OTHER);
            ButtonType closeType = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
            dialog.getDialogPane().getButtonTypes().addAll(validateType, refuseType, closeType);

            Optional<ButtonType> result = dialog.showAndWait();
            if (result.isPresent()) {
                if (result.get() == validateType) {
                    handleValidateFromDialog(selectedAssignment);
                } else if (result.get() == refuseType) {
                    handleRefuseFromDialog(selectedAssignment);
                }
                // closeType or X-button: do nothing
            }

        } catch (SQLException exception) {
            showSqlError(exception);
        }
    }

    private void handleValidateFromDialog(TaskAssignment selectedAssignment) {
        try {
            taskAssignmentService.validateTask(selectedAssignment.getTaskId());
            selectedAssignment.setStatut("realisea");
            taskAssignmentService.update(selectedAssignment);
            showInfo("Success", "Task validated successfully.");
            refreshFarmerData();
            handleClear();
        } catch (SQLException exception) {
            showSqlError(exception);
        }
    }

    private void handleRefuseFromDialog(TaskAssignment selectedAssignment) {
        try {
            taskAssignmentService.refuseTask(selectedAssignment.getTaskId());
            selectedAssignment.setStatut("assignee");
            taskAssignmentService.update(selectedAssignment);
            showInfo("Success", "Task refused successfully.");
            refreshFarmerData();
            handleClear();
        } catch (SQLException exception) {
            showSqlError(exception);
        }
    }

    @FXML
    private void handleMarkDone() {
        TaskAssignment selectedAssignment = employeeTable.getSelectionModel().getSelectedItem();
        if (selectedAssignment == null) {
            showWarning("Selection Required", "Select a task to mark as done.");
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Mark Task as Done");
        dialog.setHeaderText("Provide task completion details");

        GridPane gridPane = new GridPane();
        gridPane.setHgap(10);
        gridPane.setVgap(10);
        gridPane.setPadding(new Insets(20));

        TextArea rendementArea = new TextArea();
        rendementArea.setPromptText("Rendement / Output");
        rendementArea.setWrapText(true);
        rendementArea.setPrefRowCount(3);

        TextArea problemesArea = new TextArea();
        problemesArea.setPromptText("Problèmes / Issues encountered");
        problemesArea.setWrapText(true);
        problemesArea.setPrefRowCount(3);

        TextArea solutionArea = new TextArea();
        solutionArea.setPromptText("Solution / Actions taken");
        solutionArea.setWrapText(true);
        solutionArea.setPrefRowCount(3);

        gridPane.add(new Label("Rendement:"), 0, 0);
        gridPane.add(rendementArea, 0, 1);
        gridPane.add(new Label("Problèmes:"), 0, 2);
        gridPane.add(problemesArea, 0, 3);
        gridPane.add(new Label("Solution:"), 0, 4);
        gridPane.add(solutionArea, 0, 5);

        dialog.getDialogPane().setContent(gridPane);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                SuiviTache suivi = new SuiviTache(
                        selectedAssignment.getTaskId(),
                        LocalDateTime.now(),
                        rendementArea.getText(),
                        problemesArea.getText(),
                        solutionArea.getText());
                suiviTacheService.add(suivi);

                selectedAssignment.setStatut("realisee");
                taskAssignmentService.update(selectedAssignment);

                showInfo("Success", "Task marked as done successfully.");
                refreshEmployeeData();
            } catch (SQLException exception) {
                showSqlError(exception);
            }
        }
    }

    @FXML
    private void handleClear() {
        taskComboBox.getSelectionModel().clearSelection();
        workerComboBox.getSelectionModel().clearSelection();
        dateAssignmentPicker.setValue(null);
        statutComboBox.getSelectionModel().clearSelection();
        assignmentTable.getSelectionModel().clearSelection();
    }

    private void populateFarmerForm(TaskAssignment assignment) {
        selectTaskInCombo(assignment.getTaskId());
        selectWorkerInCombo(assignment.getWorkerId());
        dateAssignmentPicker.setValue(assignment.getDateAssignment().toLocalDate());
        statutComboBox.setValue(assignment.getStatut());
    }

    private TaskAssignment buildTaskAssignmentFromForm() {
        Task selectedTask = taskComboBox.getValue();
        User selectedWorker = workerComboBox.getValue();
        LocalDate date = dateAssignmentPicker.getValue();

        // Defensive check - should not happen if validation is called first
        if (selectedTask == null || selectedWorker == null) {
            throw new IllegalArgumentException("Task and Worker must be selected before creating assignment");
        }

        return new TaskAssignment(
                selectedTask.getIdTask(),
                selectedWorker.getId(),
                date == null ? LocalDateTime.now() : date.atStartOfDay(),
                statutComboBox.getValue());
    }

    private boolean validateFarmerForm() {
        if (taskComboBox.getValue() == null) {
            showWarning("Validation Error", "Please select a task.");
            return false;
        }
        if (workerComboBox.getValue() == null) {
            showWarning("Validation Error", "Please select a worker.");
            return false;
        }
        if (dateAssignmentPicker.getValue() == null) {
            showWarning("Validation Error", "Please select an assignment date.");
            return false;
        }
        if (statutComboBox.getValue() == null) {
            showWarning("Validation Error", "Please select a status.");
            return false;
        }
        return true;
    }

    private void selectTaskInCombo(int taskId) {
        taskComboBox.getItems().stream()
                .filter(task -> task.getIdTask() == taskId)
                .findFirst()
                .ifPresent(taskComboBox.getSelectionModel()::select);
    }

    private void selectWorkerInCombo(int workerId) {
        workerComboBox.getItems().stream()
                .filter(user -> user.getId() == workerId)
                .findFirst()
                .ifPresent(workerComboBox.getSelectionModel()::select);
    }

    private void loadFarmerData() {
        try {
            taskList.setAll(taskService.getAll());
            workerList.setAll(userService.getByRole("employee"));
            assignmentList.setAll(taskAssignmentService.getAll());
            updateFarmerStats();
        } catch (SQLException exception) {
            showSqlError(exception);
        }
    }

    private void updateFarmerStats() {
        long total = assignmentList.size();
        long accepted = assignmentList.stream().filter(a -> "acceptee".equals(a.getStatut())).count();
        long done = assignmentList.stream()
                .filter(a -> "realisee".equals(a.getStatut()) || "realisea".equals(a.getStatut())).count();
        long pending = assignmentList.stream().filter(a -> "assignee".equals(a.getStatut())).count();
        if (statTotalLabel != null)
            statTotalLabel.setText(String.valueOf(total));
        if (statAcceptedLabel != null)
            statAcceptedLabel.setText(String.valueOf(accepted));
        if (statDoneLabel != null)
            statDoneLabel.setText(String.valueOf(done));
        if (statPendingLabel != null)
            statPendingLabel.setText(String.valueOf(pending));
    }

    private void loadEmployeeData() {
        try {
            User currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser != null) {
                employeeAssignmentList.setAll(taskAssignmentService.getByWorker(currentUser.getId()));
                updateEmployeeStats();
            }
        } catch (SQLException exception) {
            showSqlError(exception);
        }
    }

    private void updateEmployeeStats() {
        long total = employeeAssignmentList.size();
        long done = employeeAssignmentList.stream()
                .filter(a -> "realisee".equals(a.getStatut()) || "realisea".equals(a.getStatut())).count();
        long pending = total - done;
        if (empStatTotalLabel != null)
            empStatTotalLabel.setText(String.valueOf(total));
        if (empStatDoneLabel != null)
            empStatDoneLabel.setText(String.valueOf(done));
        if (empStatPendingLabel != null)
            empStatPendingLabel.setText(String.valueOf(pending));
    }

    private void refreshFarmerData() {
        loadFarmerData();
    }

    private void refreshEmployeeData() {
        loadEmployeeData();
    }

    private boolean confirmDeletion() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Assignment");
        alert.setHeaderText(null);
        alert.setContentText("Are you sure you want to delete this assignment?");
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showSqlError(SQLException exception) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Database Error");
        alert.setHeaderText("Operation failed");
        alert.setContentText(exception.getMessage());
        alert.showAndWait();
    }
}
