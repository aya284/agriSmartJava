package controllers;

import entities.Culture;
import entities.Parcelle;
import entities.Task;
import entities.User;
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
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.StringConverter;
import services.CultureService;
import services.ParcelleService;
import services.TaskService;
import services.UserService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class TaskController {

    @FXML
    private TextField titreField;
    @FXML
    private TextArea descriptionArea;
    @FXML
    private TextArea resumeArea;
    @FXML
    private ComboBox<String> prioriteComboBox;
    @FXML
    private ComboBox<String> statutComboBox;
    @FXML
    private ComboBox<String> typeComboBox;
    @FXML
    private DatePicker dateDebutPicker;
    @FXML
    private DatePicker dateFinPicker;
    @FXML
    private TextField localisationField;
    @FXML
    private ComboBox<SelectionOption> parcelleIdComboBox;
    @FXML
    private ComboBox<SelectionOption> cultureIdComboBox;
    @FXML
    private ComboBox<SelectionOption> createdByComboBox;

    // Stat Labels
    @FXML private Label todoStatLabel;
    @FXML private Label doingStatLabel;
    @FXML private Label doneStatLabel;
    @FXML private Label urgentStatLabel;

    // Error Labels
    @FXML private Label titreErrorLabel;
    @FXML private Label prioriteErrorLabel;
    @FXML private Label dateDebutErrorLabel;
    @FXML private Label dateFinErrorLabel;
    @FXML private Label statutErrorLabel;
    @FXML private Label typeErrorLabel;

    @FXML
    private TableView<Task> taskTable;
    @FXML
    private TableColumn<Task, Integer> idTaskColumn;
    @FXML
    private TableColumn<Task, String> titreColumn;
    @FXML
    private TableColumn<Task, String> descriptionColumn;
    @FXML
    private TableColumn<Task, String> prioriteColumn;
    @FXML
    private TableColumn<Task, String> statutColumn;
    @FXML
    private TableColumn<Task, String> typeColumn;
    @FXML
    private TableColumn<Task, LocalDate> dateDebutColumn;
    @FXML
    private TableColumn<Task, LocalDate> dateFinColumn;

    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> sortComboBox;

    private final TaskService taskService = new TaskService();
    private final ParcelleService parcelleService = new ParcelleService();
    private final CultureService cultureService = new CultureService();
    private final UserService userService = new UserService();
    private final services.HuggingFaceService hfService = new services.HuggingFaceService();
    private final javafx.animation.PauseTransition debounce = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(2));
    
    private final ObservableList<Task> taskList = FXCollections.observableArrayList();
    private final ObservableList<SelectionOption> parcelleOptions = FXCollections.observableArrayList();
    private final ObservableList<SelectionOption> cultureOptions = FXCollections.observableArrayList();
    private final ObservableList<SelectionOption> userOptions = FXCollections.observableArrayList();

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
    public void showCalendar(javafx.event.ActionEvent event) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/Views/CalendarView.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.scene.Scene scene = ((javafx.scene.Node) event.getSource()).getScene();
            scene.setRoot(root);
        } catch (java.io.IOException e) {
            e.printStackTrace();
            showWarning("Navigation Error", "Could not load calendar view.");
        }
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
        typeComboBox.setItems(
                FXCollections.observableArrayList("arrosage", "recolte", "fertilisation", "inspection", "autre"));

        configureSelectionComboBox(parcelleIdComboBox, parcelleOptions, "Choisir une parcelle");
        configureSelectionComboBox(cultureIdComboBox, cultureOptions, "Choisir une culture");
        configureSelectionComboBox(createdByComboBox, userOptions, "Choisir un utilisateur");
        loadReferenceData();

        parcelleIdComboBox.valueProperty().addListener(
                (observable, oldValue, newValue) -> loadCulturesForParcelle(newValue == null ? null : newValue.id()));
    }

    private void initializeInputValidation() {
        titreField.setTextFormatter(
                new TextFormatter<>(change -> change.getControlNewText().length() > 255 ? null : change));

        descriptionArea.setTextFormatter(
                new TextFormatter<>(change -> change.getControlNewText().length() > 1000 ? null : change));

        // Real-time error clearing
        titreField.textProperty().addListener((obs, old, val) -> resetField(titreField, titreErrorLabel));
        prioriteComboBox.valueProperty().addListener((obs, old, val) -> resetField(prioriteComboBox, prioriteErrorLabel));
        statutComboBox.valueProperty().addListener((obs, old, val) -> resetField(statutComboBox, statutErrorLabel));
        typeComboBox.valueProperty().addListener((obs, old, val) -> resetField(typeComboBox, typeErrorLabel));
        dateDebutPicker.valueProperty().addListener((obs, old, val) -> resetField(dateDebutPicker, dateDebutErrorLabel));
        dateFinPicker.valueProperty().addListener((obs, old, val) -> resetField(dateFinPicker, dateFinErrorLabel));

        // AI Summarization
        descriptionArea.textProperty().addListener((obs, old, val) -> {
            if (val != null && val.trim().length() > 50) {
                // Show immediate feedback that AI is thinking
                resumeArea.setPromptText("L'IA prépare un résumé..."); 
                
                debounce.setOnFinished(e -> {
                    System.out.println("Starting AI Summarization for: " + val.substring(0, 20) + "...");
                    javafx.application.Platform.runLater(() -> resumeArea.setText("Génération du résumé par IA..."));
                    
                    new Thread(() -> {
                        try {
                            String summary = hfService.summarize(val);
                            System.out.println("Summary generated: " + (summary != null ? summary.substring(0, Math.min(50, summary.length())) : "null"));
                            javafx.application.Platform.runLater(() -> {
                                if (summary != null && !summary.isEmpty()) {
                                    resumeArea.setText(summary);
                                    resumeArea.setPromptText(""); // Clear prompt
                                } else {
                                    resumeArea.setText("Résumé indisponible");
                                    resumeArea.setPromptText("");
                                }
                            });
                        } catch (Exception ex) {
                            System.err.println("Exception in summarization: " + ex.getMessage());
                            ex.printStackTrace();
                            javafx.application.Platform.runLater(() -> {
                                resumeArea.setText("Erreur: " + ex.getMessage());
                                resumeArea.setPromptText("");
                            });
                        }
                    }).start();
                });
                debounce.playFromStart();
            }
        });

        // Auto-Priority based on keywords
        titreField.textProperty().addListener((obs, old, val) -> {
            if (val != null) {
                String lower = val.toLowerCase();
                if (lower.contains("fuite") || lower.contains("urgent") || lower.contains("danger") || lower.contains("mort") || lower.contains("panne")) {
                    prioriteComboBox.setValue("Haute");
                } else if (lower.contains("check") || lower.contains("visite") || lower.contains("routine")) {
                    prioriteComboBox.setValue("Basse");
                }
            }
        });
    }

    private void initializeTable() {
        idTaskColumn.setCellValueFactory(new PropertyValueFactory<>("idTask"));
        titreColumn.setCellValueFactory(new PropertyValueFactory<>("titre"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        prioriteColumn.setCellValueFactory(new PropertyValueFactory<>("priorite"));
        statutColumn.setCellValueFactory(new PropertyValueFactory<>("statut"));

        // Colour-coded status badges
        statutColumn.setCellFactory(col -> new javafx.scene.control.TableCell<Task, String>() {
            private final javafx.scene.control.Label badge = new javafx.scene.control.Label();
            private static final String BASE = "-fx-background-radius: 12; -fx-padding: 3 10; -fx-font-weight: bold; -fx-font-size: 11px; ";

            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    switch (status) {
                        case "todo" -> {
                            badge.setText("\uD83D\uDD34 To Do");
                            badge.setStyle(BASE + "-fx-background-color: #dc3545; -fx-text-fill: #ffffff;");
                        }
                        case "en_cours" -> {
                            badge.setText("\uD83D\uDD35 En cours");
                            badge.setStyle(BASE + "-fx-background-color: #0d6efd; -fx-text-fill: #ffffff;");
                        }
                        case "a_valider" -> {
                            badge.setText("\u23F3 A valider");
                            badge.setStyle(BASE + "-fx-background-color: #FFC107; -fx-text-fill: #5a3e00;");
                        }
                        case "termine" -> {
                            badge.setText("\u2705 Termine");
                            badge.setStyle(BASE + "-fx-background-color: #28a745; -fx-text-fill: #ffffff;");
                        }
                        default -> {
                            badge.setText(status);
                            badge.setStyle(BASE + "-fx-background-color: #e0e0e0; -fx-text-fill: #333;");
                        }
                    }
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        dateDebutColumn.setCellValueFactory(
                cellData -> new SimpleObjectProperty<>(toLocalDate(cellData.getValue().getDateDebut())));
        dateFinColumn.setCellValueFactory(
                cellData -> new SimpleObjectProperty<>(toLocalDate(cellData.getValue().getDateFin())));
        taskTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void initializeSearchAndSort() {
        sortComboBox.setItems(FXCollections.observableArrayList(
                "ID", "Titre", "Description", "Priorite", "Statut", "Type", "Date Debut", "Date Fin"));
        sortComboBox.setValue("ID");

        FilteredList<Task> filteredList = new FilteredList<>(taskList, p -> true);
        SortedList<Task> sortedList = new SortedList<>(filteredList);
        sortedList.comparatorProperty().bind(taskTable.comparatorProperty());
        taskTable.setItems(sortedList);

        searchField.textProperty()
                .addListener((observable, oldValue, newValue) -> updateFilter(filteredList, newValue));
    }

    private void updateFilter(FilteredList<Task> filteredList, String searchText) {
        if (searchText == null || searchText.isEmpty()) {
            filteredList.setPredicate(task -> true);
        } else {
            String lowerCaseFilter = searchText.toLowerCase();
            filteredList.setPredicate(
                    task -> (task.getTitre() != null && task.getTitre().toLowerCase().contains(lowerCaseFilter)) ||
                            (task.getDescription() != null
                                    && task.getDescription().toLowerCase().contains(lowerCaseFilter))
                            ||
                            (task.getType() != null && task.getType().toLowerCase().contains(lowerCaseFilter)) ||
                            (task.getStatut() != null && task.getStatut().toLowerCase().contains(lowerCaseFilter)) ||
                            (task.getPriorite() != null && task.getPriorite().toLowerCase().contains(lowerCaseFilter))
                            ||
                            (task.getLocalisation() != null
                                    && task.getLocalisation().toLowerCase().contains(lowerCaseFilter))
                            ||
                            String.valueOf(task.getIdTask()).contains(lowerCaseFilter));
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
            default:
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
            updateStats();
        } catch (SQLException exception) {
            showSqlError(exception);
        }
    }

    private void updateStats() {
        long todo = taskList.stream().filter(t -> "todo".equalsIgnoreCase(t.getStatut())).count();
        long doing = taskList.stream().filter(t -> "en_cours".equalsIgnoreCase(t.getStatut())).count();
        long done = taskList.stream().filter(t -> "termine".equalsIgnoreCase(t.getStatut())).count();
        long urgent = taskList.stream().filter(t -> "high".equalsIgnoreCase(t.getPriorite()) || "haute".equalsIgnoreCase(t.getPriorite())).count();

        todoStatLabel.setText(String.valueOf(todo));
        doingStatLabel.setText(String.valueOf(doing));
        doneStatLabel.setText(String.valueOf(done));
        urgentStatLabel.setText(String.valueOf(urgent));
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
                getSelectedOptionId(parcelleIdComboBox),
                getSelectedOptionId(cultureIdComboBox),
                getSelectedOptionId(createdByComboBox));
    }

    private boolean validateForm() {
        resetErrorStates();
        boolean isValid = true;

        String titre = titreField.getText().trim();
        if (isBlank(titre)) {
            showError(titreField, titreErrorLabel, "Le titre de la tâche est requis.");
            isValid = false;
        } else if (titre.length() < 3) {
            showError(titreField, titreErrorLabel, "Le titre doit contenir au moins 3 caractères.");
            isValid = false;
        }

        if (dateDebutPicker.getValue() == null) {
            showError(dateDebutPicker, dateDebutErrorLabel, "La date de début est requise.");
            isValid = false;
        }

        if (isBlank(prioriteComboBox.getValue())) {
            showError(prioriteComboBox, prioriteErrorLabel, "Veuillez choisir une priorité.");
            isValid = false;
        }

        if (isBlank(statutComboBox.getValue())) {
            showError(statutComboBox, statutErrorLabel, "Veuillez choisir un statut.");
            isValid = false;
        }

        if (isBlank(typeComboBox.getValue())) {
            showError(typeComboBox, typeErrorLabel, "Veuillez choisir un type.");
            isValid = false;
        }

        if (isValid && dateFinPicker.getValue() != null && dateFinPicker.getValue().isBefore(dateDebutPicker.getValue())) {
            showError(dateFinPicker, dateFinErrorLabel, "La date de fin ne peut pas être avant le début.");
            isValid = false;
        }

        SelectionOption selectedCulture = cultureIdComboBox.getValue();
        SelectionOption selectedParcelle = parcelleIdComboBox.getValue();
        if (selectedCulture != null && selectedParcelle != null && selectedCulture.parentId() != null
                && !selectedCulture.parentId().equals(selectedParcelle.id())) {
            showWarning("Validation Error - Culture", "The selected culture does not belong to the selected parcelle.");
            isValid = false;
        }

        return isValid;
    }

    private void resetErrorStates() {
        resetField(titreField, titreErrorLabel);
        resetField(prioriteComboBox, prioriteErrorLabel);
        resetField(dateDebutPicker, dateDebutErrorLabel);
        resetField(dateFinPicker, dateFinErrorLabel);
        resetField(statutComboBox, statutErrorLabel);
        resetField(typeComboBox, typeErrorLabel);
    }

    private void resetField(javafx.scene.Node node, Label errorLabel) {
        node.getStyleClass().remove("field-error");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    private void showError(javafx.scene.Node node, Label errorLabel, String message) {
        if (!node.getStyleClass().contains("field-error")) {
            node.getStyleClass().add("field-error");
        }
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void populateForm(Task task) {
        resetErrorStates();
        titreField.setText(task.getTitre());
        descriptionArea.setText(task.getDescription() == null ? "" : task.getDescription());
        resumeArea.setText(task.getResume() == null ? "" : task.getResume());
        prioriteComboBox.setValue(task.getPriorite());
        statutComboBox.setValue(task.getStatut());
        typeComboBox.setValue(task.getType());
        dateDebutPicker.setValue(toLocalDate(task.getDateDebut()));
        dateFinPicker.setValue(toLocalDate(task.getDateFin()));
        localisationField.setText(task.getLocalisation() == null ? "" : task.getLocalisation());
        selectOptionById(parcelleIdComboBox, task.getParcelleId());
        loadCulturesForParcelle(task.getParcelleId());
        selectOptionById(cultureIdComboBox, task.getCultureId());
        selectOptionById(createdByComboBox, task.getCreatedBy());
    }

    private void clearForm() {
        resetErrorStates();
        titreField.clear();
        descriptionArea.clear();
        resumeArea.clear();
        prioriteComboBox.getSelectionModel().clearSelection();
        statutComboBox.getSelectionModel().clearSelection();
        typeComboBox.getSelectionModel().clearSelection();
        dateDebutPicker.setValue(null);
        dateFinPicker.setValue(null);
        localisationField.clear();
        parcelleIdComboBox.getSelectionModel().clearSelection();
        loadCulturesForParcelle(null);
        cultureIdComboBox.getSelectionModel().clearSelection();
        createdByComboBox.getSelectionModel().clearSelection();
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

    private void loadReferenceData() {
        try {
            parcelleOptions.setAll(toParcelleOptions(parcelleService.afficher()));
            userOptions.setAll(toUserOptions(userService.getAllUsers()));
            loadCulturesForParcelle(null);
        } catch (SQLException exception) {
            showSqlError(exception);
        }
    }

    private void loadCulturesForParcelle(Integer parcelleId) {
        try {
            List<Culture> cultures = parcelleId == null ? cultureService.afficher()
                    : cultureService.getByParcelle(parcelleId);
            Integer currentCultureId = getSelectedOptionId(cultureIdComboBox);
            cultureOptions.setAll(toCultureOptions(cultures));
            selectOptionById(cultureIdComboBox, currentCultureId);
        } catch (SQLException exception) {
            showSqlError(exception);
        }
    }

    private void configureSelectionComboBox(ComboBox<SelectionOption> comboBox,
            ObservableList<SelectionOption> items,
            String promptText) {
        comboBox.setItems(items);
        comboBox.setPromptText(promptText);
        comboBox.setMaxWidth(Double.MAX_VALUE);
        comboBox.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(SelectionOption item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.label());
            }
        });
        comboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(SelectionOption option) {
                return option == null ? "" : option.label();
            }

            @Override
            public SelectionOption fromString(String string) {
                return null;
            }
        });
    }

    private List<SelectionOption> toParcelleOptions(List<Parcelle> parcelles) {
        return parcelles.stream()
                .map(parcelle -> new SelectionOption(
                        parcelle.getId(),
                        parcelle.getId() + " - " + parcelle.getNom(),
                        null))
                .toList();
    }

    private List<SelectionOption> toCultureOptions(List<Culture> cultures) {
        return cultures.stream()
                .map(culture -> new SelectionOption(
                        culture.getId(),
                        culture.getId() + " - " + culture.getTypeCulture() + " (" + culture.getVariete() + ")",
                        culture.getParcelleId()))
                .toList();
    }

    private List<SelectionOption> toUserOptions(List<User> users) {
        return users.stream()
                .map(user -> new SelectionOption(
                        user.getId(),
                        user.getId() + " - " + user.getFullName(),
                        null))
                .toList();
    }

    private void selectOptionById(ComboBox<SelectionOption> comboBox, Integer id) {
        if (id == null) {
            comboBox.getSelectionModel().clearSelection();
            return;
        }
        comboBox.getItems().stream()
                .filter(option -> option.id().equals(id))
                .findFirst()
                .ifPresentOrElse(
                        comboBox.getSelectionModel()::select,
                        () -> comboBox.getSelectionModel().clearSelection());
    }

    private Integer getSelectedOptionId(ComboBox<SelectionOption> comboBox) {
        SelectionOption selected = comboBox.getValue();
        return selected == null ? null : selected.id();
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

    private record SelectionOption(Integer id, String label, Integer parentId) {
    }
}
