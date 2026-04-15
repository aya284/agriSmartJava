package controllers;

import entities.Task;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import services.TaskService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.Optional;

public class TaskController {

    @FXML private TextField titreField;
    @FXML private TextArea descriptionArea;
    @FXML private TextArea resumeArea;
    @FXML private ComboBox<String> prioriteComboBox;
    @FXML private ComboBox<String> statutComboBox;
    @FXML private ComboBox<String> typeComboBox;
    @FXML private DatePicker dateDebutPicker;
    @FXML private DatePicker dateFinPicker;
    @FXML private TextField localisationField;
    @FXML private TextField parcelleIdField;
    @FXML private TextField cultureIdField;
    @FXML private TextField createdByField;

    @FXML private TableView<Task> taskTable;
    @FXML private TableColumn<Task, Integer> idTaskColumn;
    @FXML private TableColumn<Task, String> titreColumn;
    @FXML private TableColumn<Task, String> descriptionColumn;
    @FXML private TableColumn<Task, String> prioriteColumn;
    @FXML private TableColumn<Task, String> statutColumn;
    @FXML private TableColumn<Task, String> typeColumn;
    @FXML private TableColumn<Task, LocalDate> dateDebutColumn;
    @FXML private TableColumn<Task, LocalDate> dateFinColumn;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortComboBox;

    private final TaskService taskService = new TaskService();
    private final ObservableList<Task> taskList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        initializeCombos();
        initializeTable();
        initializeSelectionHandling();
        initializeInputValidation();
        initializeSearchAndSort();
        loadTasks();
    }

    @FXML
    public void handleAdd() {
        if (!validateForm()) {
            return;
        }
        try {
            taskService.add(buildTaskFromForm());
            refreshTableAndClearForm();
            showInfo("Success", "Task added successfully.");
        } catch (SQLException exception) {
            showSqlError(exception);
        }
    }

    @FXML
    public void handleUpdate() {
        Task selectedTask = taskTable.getSelectionModel().getSelectedItem();
        if (selectedTask == null) {
            showWarning("Selection Required", "Select a task to update.");
            return;
        }
        if (!validateForm()) {
            return;
        }

        Task task = buildTaskFromForm();
        task.setIdTask(selectedTask.getIdTask());

        try {
            taskService.update(task);
            refreshTableAndClearForm();
            showInfo("Success", "Task updated successfully.");
        } catch (SQLException exception) {
            showSqlError(exception);
        }
    }

    @FXML
    public void handleDelete() {
        Task selectedTask = taskTable.getSelectionModel().getSelectedItem();
        if (selectedTask == null) {
            showWarning("Selection Required", "Select a task to delete.");
            return;
        }
        if (!confirmDeletion(selectedTask)) {
            return;
        }

        try {
            taskService.delete(selectedTask.getIdTask());
            refreshTableAndClearForm();
            showInfo("Success", "Task deleted successfully.");
        } catch (SQLException exception) {
            showSqlError(exception);
        }
    }

    @FXML
    public void handleClear() {
        clearForm();
    }

    @FXML
    public void handleResetSearch() {
        searchField.clear();
    }

    @FXML
    public void handleSortAscending() {
        applySorting(true);
    }

    @FXML
    public void handleSortDescending() {
        applySorting(false);
    }

    private void initializeCombos() {
        prioriteComboBox.setItems(FXCollections.observableArrayList("low", "medium", "high"));
        statutComboBox.setItems(FXCollections.observableArrayList("todo", "en_cours", "a_valider", "termine"));
        typeComboBox.setItems(FXCollections.observableArrayList("arrosage", "recolte", "fertilisation", "inspection", "autre"));
    }

    private void initializeInputValidation() {
        // Titre: max 255 caractères avec coloration en temps réel
        titreField.setTextFormatter(new TextFormatter<>(change -> {
            if (change.getControlNewText().length() > 255) {
                return null;
            }
            return change;
        }));

        // Description: max 1000 caractères (ne peut pas taper plus)
        descriptionArea.setTextFormatter(new TextFormatter<>(change -> {
            if (change.getControlNewText().length() > 1000) {
                return null;
            }
            return change;
        }));
    }

    private void initializeTable() {
        idTaskColumn.setCellValueFactory(new PropertyValueFactory<>("idTask"));
        titreColumn.setCellValueFactory(new PropertyValueFactory<>("titre"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        prioriteColumn.setCellValueFactory(new PropertyValueFactory<>("priorite"));
        statutColumn.setCellValueFactory(new PropertyValueFactory<>("statut"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        dateDebutColumn.setCellValueFactory(cellData ->
                new SimpleObjectProperty<>(toLocalDate(cellData.getValue().getDateDebut())));
        dateFinColumn.setCellValueFactory(cellData ->
                new SimpleObjectProperty<>(toLocalDate(cellData.getValue().getDateFin())));
        taskTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void initializeSearchAndSort() {
        // Populate sort options
        sortComboBox.setItems(FXCollections.observableArrayList(
                "ID", "Titre", "Description", "Priorite", "Statut", "Type", "Date Debut", "Date Fin"
        ));
        sortComboBox.setValue("ID");

        // Create FilteredList wrapping the taskList
        FilteredList<Task> filteredList = new FilteredList<>(taskList, p -> true);

        // Create SortedList wrapping FilteredList
        SortedList<Task> sortedList = new SortedList<>(filteredList);
        sortedList.comparatorProperty().bind(taskTable.comparatorProperty());

        // Bind the SortedList to TableView
        taskTable.setItems(sortedList);

        // Add listener to search field for real-time filtering
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            updateFilter(filteredList, newValue);
        });
    }

    private void updateFilter(FilteredList<Task> filteredList, String searchText) {
        if (searchText == null || searchText.isEmpty()) {
            filteredList.setPredicate(task -> true);
        } else {
            String lowerCaseFilter = searchText.toLowerCase();
            filteredList.setPredicate(task ->
                    (task.getTitre() != null && task.getTitre().toLowerCase().contains(lowerCaseFilter)) ||
                    (task.getDescription() != null && task.getDescription().toLowerCase().contains(lowerCaseFilter)) ||
                    (task.getType() != null && task.getType().toLowerCase().contains(lowerCaseFilter)) ||
                    (task.getStatut() != null && task.getStatut().toLowerCase().contains(lowerCaseFilter)) ||
                    (task.getPriorite() != null && task.getPriorite().toLowerCase().contains(lowerCaseFilter)) ||
                    (task.getLocalisation() != null && task.getLocalisation().toLowerCase().contains(lowerCaseFilter)) ||
                    String.valueOf(task.getIdTask()).contains(lowerCaseFilter)
            );
        }
    }

    private void applySorting(boolean ascending) {
        String selectedSort = sortComboBox.getValue();
        if (selectedSort == null) {
            showWarning("Selection Required", "Please select a sorting option.");
            return;
        }

        Comparator<Task> comparator = null;

        switch (selectedSort) {
            case "ID":
                comparator = Comparator.comparingInt(Task::getIdTask);
                break;
            case "Titre":
                comparator = Comparator.comparing(Task::getTitre, Comparator.nullsLast(String::compareTo));
                break;
            case "Description":
                comparator = Comparator.comparing(Task::getDescription, Comparator.nullsLast(String::compareTo));
                break;
            case "Priorite":
                comparator = Comparator.comparing(Task::getPriorite, Comparator.nullsLast(String::compareTo));
                break;
            case "Statut":
                comparator = Comparator.comparing(Task::getStatut, Comparator.nullsLast(String::compareTo));
                break;
            case "Type":
                comparator = Comparator.comparing(Task::getType, Comparator.nullsLast(String::compareTo));
                break;
            case "Date Debut":
                comparator = Comparator.comparing(Task::getDateDebut, Comparator.nullsLast(Comparator.naturalOrder()));
                break;
            case "Date Fin":
                comparator = Comparator.comparing(Task::getDateFin, Comparator.nullsLast(Comparator.naturalOrder()));
                break;
        }

        if (!ascending && comparator != null) {
            comparator = comparator.reversed();
        }

        if (comparator != null) {
            FXCollections.sort(taskList, comparator);
        }
    }

    private void initializeSelectionHandling() {
        taskTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                populateForm(newValue);
            }
        });
    }

    private void loadTasks() {
        try {
            taskList.setAll(taskService.getAll());
        } catch (SQLException exception) {
            showSqlError(exception);
        }
    }

    private Task buildTaskFromForm() {
        return new Task(
                titreField.getText().trim(),
                emptyToNull(descriptionArea.getText()),
                emptyToNull(resumeArea.getText()),
                dateDebutPicker.getValue() == null ? null : dateDebutPicker.getValue().atStartOfDay(),
                dateFinPicker.getValue() == null ? null : dateFinPicker.getValue().atStartOfDay(),
                prioriteComboBox.getValue(),
                statutComboBox.getValue(),
                typeComboBox.getValue(),
                emptyToNull(localisationField.getText()),
                parseInteger(parcelleIdField.getText()),
                parseInteger(cultureIdField.getText()),
                parseInteger(createdByField.getText())
        );
    }

    private boolean validateForm() {
        String titre = titreField.getText().trim();
        
        // Titre: requis et minimum 3 caractères
        if (isBlank(titre)) {
            showWarning("Validation Error - Titre", "Titre is required. Please enter a title.");
            return false;
        }
        if (titre.length() < 3) {
            showWarning("Validation Error - Titre", "Titre must be at least 3 characters long. Currently: " + titre.length() + " characters.");
            return false;
        }
        
        // Date debut: obligatoire
        if (dateDebutPicker.getValue() == null) {
            showWarning("Validation Error - Date Debut", "Date debut is required. Please select a start date.");
            return false;
        }
        
        // Priorite, statut and type: obligatoires
        if (isBlank(prioriteComboBox.getValue())) {
            showWarning("Validation Error - Priorite", "Priorite is required. Please select a priority level.");
            return false;
        }
        if (isBlank(statutComboBox.getValue())) {
            showWarning("Validation Error - Statut", "Statut is required. Please select a status.");
            return false;
        }
        if (isBlank(typeComboBox.getValue())) {
            showWarning("Validation Error - Type", "Type is required. Please select a type.");
            return false;
        }
        
        // Date fin doit être après date début
        if (dateFinPicker.getValue() != null && dateFinPicker.getValue().isBefore(dateDebutPicker.getValue())) {
            showWarning("Validation Error - Dates", 
                "Date fin must be after or equal to Date debut.\n\n" +
                "Date debut: " + dateDebutPicker.getValue() + "\n" +
                "Date fin: " + dateFinPicker.getValue());
            return false;
        }
        
        // Champs numériques doivent être valides
        if (!isIntegerOrBlank(parcelleIdField.getText())) {
            showWarning("Validation Error - Parcelle ID", "Parcelle ID must be a valid number or empty. Currently: " + parcelleIdField.getText());
            return false;
        }
        if (!isIntegerOrBlank(cultureIdField.getText())) {
            showWarning("Validation Error - Culture ID", "Culture ID must be a valid number or empty. Currently: " + cultureIdField.getText());
            return false;
        }
        if (!isIntegerOrBlank(createdByField.getText())) {
            showWarning("Validation Error - Created By", "Created By must be a valid number or empty. Currently: " + createdByField.getText());
            return false;
        }
        
        return true;
    }

    private void populateForm(Task task) {
        titreField.setText(task.getTitre());
        descriptionArea.setText(task.getDescription() == null ? "" : task.getDescription());
        resumeArea.setText(task.getResume() == null ? "" : task.getResume());
        prioriteComboBox.setValue(task.getPriorite());
        statutComboBox.setValue(task.getStatut());
        typeComboBox.setValue(task.getType());
        dateDebutPicker.setValue(toLocalDate(task.getDateDebut()));
        dateFinPicker.setValue(toLocalDate(task.getDateFin()));
        localisationField.setText(task.getLocalisation() == null ? "" : task.getLocalisation());
        parcelleIdField.setText(task.getParcelleId() == null ? "" : String.valueOf(task.getParcelleId()));
        cultureIdField.setText(task.getCultureId() == null ? "" : String.valueOf(task.getCultureId()));
        createdByField.setText(task.getCreatedBy() == null ? "" : String.valueOf(task.getCreatedBy()));
    }

    private void clearForm() {
        titreField.clear();
        descriptionArea.clear();
        resumeArea.clear();
        prioriteComboBox.getSelectionModel().clearSelection();
        statutComboBox.getSelectionModel().clearSelection();
        typeComboBox.getSelectionModel().clearSelection();
        dateDebutPicker.setValue(null);
        dateFinPicker.setValue(null);
        localisationField.clear();
        parcelleIdField.clear();
        cultureIdField.clear();
        createdByField.clear();
        taskTable.getSelectionModel().clearSelection();
    }

    private void refreshTableAndClearForm() {
        loadTasks();
        clearForm();
    }

    private boolean confirmDeletion(Task task) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Task");
        alert.setHeaderText(null);
        alert.setContentText("Delete task \"" + task.getTitre() + "\"?");
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
        alert.setHeaderText("Task operation failed");
        alert.setContentText(exception.getMessage());
        alert.showAndWait();
    }

    private Integer parseInteger(String value) {
        return isBlank(value) ? null : Integer.valueOf(value.trim());
    }

    private boolean isIntegerOrBlank(String value) {
        return isBlank(value) || value.trim().matches("-?\\d+");
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String emptyToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private LocalDate toLocalDate(java.time.LocalDateTime value) {
        return value == null ? null : value.toLocalDate();
    }
}
