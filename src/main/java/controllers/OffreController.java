package controllers;

import entities.Offre;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import services.OffreService;

import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;

public class OffreController implements Initializable {
    @FXML private FlowPane cardsContainer;
    @FXML private Label statTotalOffres, statApprouve, formTitle;
    @FXML private TextField titleF, typePosteF, lieuF, salaireF, searchField;
    @FXML private TextArea descF;
    @FXML private ComboBox<String> typeContratC, statutC;
    @FXML private DatePicker dateDebutP, dateFinP;
    @FXML private Button submitBtn;

    // Labels pour l'affichage des erreurs sous les champs
    @FXML private Label titleError, typePosteError, typeContratError, descError, lieuError, salaireError, dateDebutError, dateFinError;

    private final OffreService service = new OffreService();
    private static Offre selectedOffre = null;

    public static Offre getSelectedOffre() {
        return selectedOffre;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (cardsContainer != null) {
            loadData();
        }

        if (typeContratC != null) {
            typeContratC.setItems(FXCollections.observableArrayList("CDI", "CDD", "Stage"));
            statutC.setItems(FXCollections.observableArrayList("Ouvert", "Clôturée"));

            setupValidationListeners();

            if (selectedOffre != null) {
                fillForm(selectedOffre);
            } else {
                statutC.setValue("Ouvert");
            }
        }
    }

    // Gestion de la validation interactive (Style vidéo avec retouches Date et Contrat)
    private void setupValidationListeners() {
        // Validation Titre (Min 3)
        if (titleF != null) {
            titleF.textProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal.trim().length() < 3) {
                    titleError.setText("Min. 3 caractères");
                    titleF.setStyle("-fx-border-color: #e74c3c;");
                } else {
                    titleError.setText("");
                    titleF.setStyle("-fx-border-color: #2ecc71;");
                }
            });
        }

        // Validation Type Poste (Min 3)
        if (typePosteF != null) {
            typePosteF.textProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal.trim().length() < 3) {
                    typePosteError.setText("Min. 3 caractères");
                    typePosteF.setStyle("-fx-border-color: #e74c3c;");
                } else {
                    typePosteError.setText("");
                    typePosteF.setStyle("-fx-border-color: #2ecc71;");
                }
            });
        }

        // Validation Lieu (Min 5)
        if (lieuF != null) {
            lieuF.textProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal.trim().length() < 5) {
                    lieuError.setText("Minimum 5 caractères");
                    lieuF.setStyle("-fx-border-color: #e74c3c;");
                } else {
                    lieuError.setText("");
                    lieuF.setStyle("-fx-border-color: #2ecc71;");
                }
            });
        }

        // Validation Description (Min 12)
        if (descF != null) {
            descF.textProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal.trim().length() < 12) {
                    descError.setText("Minimum 12 caractères");
                    descF.setStyle("-fx-border-color: #e74c3c;");
                } else {
                    descError.setText("");
                    descF.setStyle("-fx-border-color: #2ecc71;");
                }
            });
        }

        // RETOUCHE : Validation Type Contrat
        if (typeContratC != null) {
            typeContratC.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal == null || newVal.isEmpty()) {
                    typeContratError.setText("Sélectionnez un type");
                    typeContratC.setStyle("-fx-border-color: #e74c3c;");
                } else {
                    typeContratError.setText("");
                    typeContratC.setStyle("-fx-border-color: #2ecc71;");
                }
            });
        }

        // Validation Salaire
        if (salaireF != null) {
            salaireF.textProperty().addListener((obs, oldVal, newVal) -> {
                if (!newVal.matches("\\d*(\\.\\d*)?")) {
                    salaireF.setText(oldVal);
                } else if (newVal.trim().isEmpty()) {
                    salaireError.setText("Requis");
                    salaireF.setStyle("-fx-border-color: #e74c3c;");
                } else {
                    salaireError.setText("");
                    salaireF.setStyle("-fx-border-color: #2ecc71;");
                }
            });
        }

        // RETOUCHE : Validation Date Début
        if (dateDebutP != null) {
            dateDebutP.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal == null) {
                    dateDebutError.setText("Requis");
                    dateDebutP.setStyle("-fx-border-color: #e74c3c;");
                } else {
                    dateDebutError.setText("");
                    dateDebutP.setStyle("-fx-border-color: #2ecc71;");
                }
            });
        }

        // Validation Date Fin
        if (dateFinP != null) {
            dateFinP.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (dateDebutP.getValue() != null && newVal != null && newVal.isBefore(dateDebutP.getValue())) {
                    dateFinError.setText("La date de fin doit être après le début");
                    dateFinP.setStyle("-fx-border-color: #e74c3c;");
                } else if (newVal != null) {
                    dateFinError.setText("");
                    dateFinP.setStyle("-fx-border-color: #2ecc71;");
                }
            });
        }
    }

    @FXML
    public void handleSave() {
        if (!validateInputs()) return;

        try {
            Offre o = (selectedOffre == null) ? new Offre() : selectedOffre;

            o.setTitle(titleF.getText().trim());
            o.setType_poste(typePosteF.getText().trim());
            o.setType_contrat(typeContratC.getValue());
            o.setDescription(descF.getText().trim());
            o.setLieu(lieuF.getText().trim());
            o.setSalaire(Double.parseDouble(salaireF.getText().trim()));
            o.setStatut(statutC.getValue());

            o.setDate_debut(dateDebutP.getValue().atStartOfDay());
            o.setDate_fin(dateFinP.getValue().atTime(23, 59, 59));

            o.setAgriculteur_id(2);
            o.setIs_active(true);

            if (selectedOffre == null) {
                o.setStatut_validation("en_attente");
                service.ajouter(o);
            } else {
                service.modifier(o);
            }

            selectedOffre = null;
            showListPage();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Une erreur technique est survenue.");
        }
    }

    private boolean validateInputs() {
        boolean isValid = true;

        if (titleF.getText().trim().length() < 3) { titleError.setText("Titre trop court"); isValid = false; }
        if (typePosteF.getText().trim().length() < 3) { typePosteError.setText("Type poste trop court"); isValid = false; }
        if (descF.getText().trim().length() < 12) { descError.setText("Description trop courte (min 12)"); isValid = false; }
        if (lieuF.getText().trim().length() < 5) { lieuError.setText("Lieu trop court (min 5)"); isValid = false; }

        if (typeContratC.getValue() == null) { typeContratError.setText("Sélectionnez"); isValid = false; }
        if (salaireF.getText().isEmpty()) { salaireError.setText("Requis"); isValid = false; }
        if (dateDebutP.getValue() == null) { dateDebutError.setText("Requis"); isValid = false; }
        if (dateFinP.getValue() == null) { dateFinError.setText("Requis"); isValid = false; }

        return isValid;
    }

    private void loadData() {
        try {
            List<Offre> list = service.afficher();
            cardsContainer.getChildren().clear();
            int approved = 0;
            for (Offre o : list) {
                cardsContainer.getChildren().add(createCard(o));
                if ("approuvée".equals(o.getStatut_validation())) approved++;
            }
            statTotalOffres.setText(String.valueOf(list.size()));
            statApprouve.setText(String.valueOf(approved));
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void fillForm(Offre o) {
        formTitle.setText("Modifier l'offre : " + o.getTitle());
        submitBtn.setText("Enregistrer les modifications");
        titleF.setText(o.getTitle());
        typePosteF.setText(o.getType_poste());
        typeContratC.setValue(o.getType_contrat());
        descF.setText(o.getDescription());
        lieuF.setText(o.getLieu());
        salaireF.setText(String.valueOf(o.getSalaire()));
        statutC.setValue(o.getStatut());
        if (o.getDate_debut() != null) dateDebutP.setValue(o.getDate_debut().toLocalDate());
        if (o.getDate_fin() != null) dateFinP.setValue(o.getDate_fin().toLocalDate());
    }

    private VBox createCard(Offre o) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: white; -fx-padding: 20; -fx-background-radius: 10; -fx-pref-width: 280; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 0);");

        Label type = new Label(o.getType_contrat());
        type.setStyle("-fx-background-color: #eee; -fx-padding: 2 8; -fx-background-radius: 5;");

        Label validationStatus = new Label();
        applyStatusStyle(validationStatus, o.getStatut_validation());

        Label title = new Label(o.getTitle());
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 16;");

        Label loc = new Label("📍 " + o.getLieu());
        Label sal = new Label(o.getSalaire() + " DT");
        sal.setStyle("-fx-font-weight: bold; -fx-text-fill: #27ae60;");

        MenuButton options = new MenuButton("...");
        MenuItem edit = new MenuItem("Modifier");
        MenuItem delete = new MenuItem("Supprimer");
        edit.setOnAction(e -> { selectedOffre = o; showAddPage(); });
        delete.setOnAction(e -> {
            try {
                service.supprimer(o.getId().intValue());
                loadData();
            } catch (SQLException ex) { ex.printStackTrace(); }
        });
        options.getItems().addAll(edit, delete);

        Button voirBtn = new Button("Voir Détails");
        voirBtn.setStyle("-fx-background-color: #1a3323; -fx-text-fill: white; -fx-background-radius: 5;");
        voirBtn.setMaxWidth(Double.MAX_VALUE);
        voirBtn.setOnAction(e -> {
            selectedOffre = o;
            switchView("/Views/Offres/OffreDetailAdmin.fxml");
        });

        card.getChildren().addAll(
                new HBox(type, new Region(){{HBox.setHgrow(this, Priority.ALWAYS);}}, options),
                title, loc, sal, validationStatus, voirBtn
        );
        return card;
    }

    private void applyStatusStyle(Label label, String status) {
        String baseStyle = "-fx-padding: 4 10; -fx-background-radius: 15; -fx-font-size: 11; -fx-font-weight: bold; ";
        if (status == null) status = "en_attente";
        switch (status.toLowerCase()) {
            case "approuvée":
                label.setStyle(baseStyle + "-fx-background-color: #d4edda; -fx-text-fill: #155724;");
                label.setText("✔ Acceptée");
                break;
            case "refusée":
                label.setStyle(baseStyle + "-fx-background-color: #f8d7da; -fx-text-fill: #721c24;");
                label.setText("✘ Refusée");
                break;
            default:
                label.setStyle(baseStyle + "-fx-background-color: #fff3cd; -fx-text-fill: #856404;");
                label.setText("⏳ En cours");
                break;
        }
    }

    @FXML public void handleSearch() {
        String query = searchField.getText().toLowerCase().trim();
        try {
            List<Offre> allOffres = service.afficher();
            cardsContainer.getChildren().clear();
            for (Offre o : allOffres) {
                if (o.getTitle().toLowerCase().contains(query) || o.getLieu().toLowerCase().contains(query)) {
                    cardsContainer.getChildren().add(createCard(o));
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML public void showAddPage() { switchView("/Views/Offres/OffreForm.fxml"); }
    @FXML public void showListPage() { selectedOffre = null; switchView("/Views/Offres/OffreList.fxml"); }

    private void switchView(String path) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
            Parent root = loader.load();
            javafx.scene.Node anchor = (cardsContainer != null) ? cardsContainer : submitBtn;
            StackPane contentArea = (StackPane) anchor.getScene().lookup("#contentArea");
            if (contentArea != null) {
                contentArea.getChildren().setAll(root);
            } else {
                anchor.getScene().setRoot(root);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }
}
