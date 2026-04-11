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
import java.time.LocalDate;
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

    private final OffreService service = new OffreService();
    private static Offre selectedOffre = null;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Run if on List page
        if (cardsContainer != null) {
            loadData();
        }

        // Run if on Form page
        if (typeContratC != null) {
            typeContratC.setItems(FXCollections.observableArrayList("CDI", "CDD", "Stage"));
            statutC.setItems(FXCollections.observableArrayList("Ouvert", "Clôturée"));

            if (selectedOffre != null) {
                fillForm(selectedOffre);
            } else {
                statutC.setValue("Ouvert");
            }
        }
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

            // Converting LocalDate to LocalDateTime for the entity
            o.setDate_debut(dateDebutP.getValue().atStartOfDay());
            o.setDate_fin(dateFinP.getValue().atTime(23, 59, 59));

            o.setAgriculteur_id(2); // Simulated session ID
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
        StringBuilder sb = new StringBuilder();

        // Length and Null checks
        if (titleF.getText().trim().length() < 3) sb.append("- Titre: minimum 3 caractères\n");
        if (typePosteF.getText().trim().length() < 3) sb.append("- Type Poste: minimum 3 caractères\n");
        if (descF.getText().trim().isEmpty()) sb.append("- Description est requise\n");
        if (lieuF.getText().trim().isEmpty()) sb.append("- Lieu est requis\n");
        if (typeContratC.getValue() == null) sb.append("- Sélectionnez un type de contrat\n");

        // Numeric check for salary
        try {
            double s = Double.parseDouble(salaireF.getText());
            if (s <= 0) sb.append("- Salaire doit être supérieur à 0\n");
        } catch (NumberFormatException e) {
            sb.append("- Salaire doit être un nombre valide\n");
        }

        // Date Logic
        LocalDate today = LocalDate.now();
        LocalDate debut = dateDebutP.getValue();
        LocalDate fin = dateFinP.getValue();

        if (debut == null || fin == null) {
            sb.append("- Les dates de début et fin sont obligatoires\n");
        } else {
            // Debut >= Today (Only for new offers)
            if (selectedOffre == null && debut.isBefore(today)) {
                sb.append("- Date début ne peut pas être dans le passé\n");
            }
            // Fin > Debut
            if (!fin.isAfter(debut)) {
                sb.append("- Date fin doit être après la date début\n");
            }
        }

        if (sb.length() > 0) {
            showAlert("Erreurs de validation", sb.toString());
            return false;
        }
        return true;
    }

    private VBox createCard(Offre o) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: white; -fx-padding: 20; -fx-background-radius: 10; -fx-pref-width: 280; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 0);");

        // Existing Type Label
        Label type = new Label(o.getType_contrat());
        type.setStyle("-fx-background-color: #eee; -fx-padding: 2 8; -fx-background-radius: 5;");

        // --- NEW VALIDATION STATUS LABEL ---
        Label validationStatus = new Label(o.getStatut_validation().toUpperCase());
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

        // --- VOIR BUTTON ---
        Button voirBtn = new Button("Voir Détails");
        voirBtn.setStyle("-fx-background-color: #1a3323; -fx-text-fill: white; -fx-background-radius: 5; -fx-cursor: hand;");
        voirBtn.setMaxWidth(Double.MAX_VALUE);
        voirBtn.setOnAction(e -> {
            selectedOffre = o;
            switchView("/Views/Offres/OffreDetailAdmin.fxml");
        });

        // Added validationStatus to the top HBox
        card.getChildren().addAll(
                new HBox(type, new Region(){{HBox.setHgrow(this, Priority.ALWAYS);}}, options),
                title,
                loc,
                sal,
                validationStatus, // Positioned above the button
                voirBtn
        );
        return card;
    }

    /**
     * Helper method to apply dynamic colors to the status label
     */
    private void applyStatusStyle(Label label, String status) {
        String baseStyle = "-fx-padding: 4 10; -fx-background-radius: 15; -fx-font-size: 11; -fx-font-weight: bold; ";

        if (status == null) status = "en_attente";

        switch (status.toLowerCase()) {
            case "approuvée":
                label.setStyle(baseStyle + "-fx-background-color: #d4edda; -fx-text-fill: #155724;"); // Green
                label.setText("✔ Acceptée");
                break;
            case "refusée":
                label.setStyle(baseStyle + "-fx-background-color: #f8d7da; -fx-text-fill: #721c24;"); // Red
                label.setText("✘ Refusée");
                break;
            case "en_attente":
            default:
                label.setStyle(baseStyle + "-fx-background-color: #fff3cd; -fx-text-fill: #856404;"); // Orange/Yellow
                label.setText("⏳ En cours");
                break;
        }
    }
    @FXML
    public void handleSearch() {
        String query = searchField.getText().toLowerCase().trim();
        try {
            List<Offre> allOffres = service.afficher();
            cardsContainer.getChildren().clear();
            int approvedCount = 0;
            for (Offre o : allOffres) {
                if (o.getTitle().toLowerCase().contains(query) || o.getLieu().toLowerCase().contains(query)) {
                    cardsContainer.getChildren().add(createCard(o));
                    if ("approuvée".equals(o.getStatut_validation())) approvedCount++;
                }
            }
            statTotalOffres.setText(String.valueOf(cardsContainer.getChildren().size()));
            statApprouve.setText(String.valueOf(approvedCount));
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

            // Dynamic anchor selection
            javafx.scene.Node anchor = (cardsContainer != null) ? cardsContainer : submitBtn;
            StackPane contentArea = (StackPane) anchor.getScene().lookup("#contentArea");

            if (contentArea != null) {
                contentArea.getChildren().setAll(root);
            } else {
                anchor.getScene().setRoot(root);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static Offre getSelectedOffre() {
        return selectedOffre;
    }
}