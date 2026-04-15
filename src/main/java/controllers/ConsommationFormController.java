package controllers;

import entities.Consommation;
import entities.Culture;
import entities.Ressource;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import services.ConsommationService;
import services.RessourceService;

import utils.NotificationUtil;
import utils.SessionManager;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class ConsommationFormController {

    @FXML private Label lblCultureName, lblStockRestant;
    @FXML private ComboBox<Ressource> cbRessource;
    @FXML private TextField txtQuantite;
    @FXML private DatePicker dpDate;
    @FXML private Label lblErrorRessource, lblErrorQuantite, lblErrorDate;

    private ConsommationService cs = new ConsommationService();
    private RessourceService rs = new RessourceService();
    private int cultureId;
    private Consommation currentConsommation;

    @FXML
    public void initialize() {
        dpDate.setValue(LocalDate.now());
        setupRessourceComboBox();
        
        // Listener pour mettre à jour l'affichage du stock
        cbRessource.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                lblStockRestant.setText("Stock disponible : " + newVal.getStockRestant() + " " + newVal.getUnite());
            } else {
                lblStockRestant.setText("Stock disponible : -");
            }
        });
    }

    private void setupRessourceComboBox() {
        try {
            int userId = SessionManager.getInstance().getCurrentUser().getId();
            List<Ressource> ressources = rs.afficherByUser(userId);
            cbRessource.setItems(FXCollections.observableArrayList(ressources));
            
            cbRessource.setConverter(new StringConverter<Ressource>() {
                @Override
                public String toString(Ressource r) {
                    return r == null ? "" : r.getNom() + " (" + r.getType() + ")";
                }

                @Override
                public Ressource fromString(String string) {
                    return null; // Non utilisé
                }
            });
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setCultureData(Culture c) {
        this.cultureId = c.getId();
        lblCultureName.setText("Culture : " + c.getTypeCulture() + " (" + c.getVariete() + ")");
    }

    public void setConsommationData(Consommation cons, Culture c) {
        this.currentConsommation = cons;
        this.cultureId = c.getId();
        lblCultureName.setText("Modifier l'Usage : " + c.getTypeCulture());
        txtQuantite.setText(String.valueOf(cons.getQuantite()));
        dpDate.setValue(cons.getDateConsommation());
        
        // Sélectionner la ressource correspondante
        for (Ressource r : cbRessource.getItems()) {
            if (r.getId() == cons.getRessourceId()) {
                cbRessource.setValue(r);
                break;
            }
        }
    }

    @FXML
    private void save() {
        clearErrors();
        if (!validate()) {
            ((javafx.stage.Stage) txtQuantite.getScene().getWindow()).sizeToScene();
            return;
        }

        try {
            Ressource selectedRessource = cbRessource.getValue();
            double quantite = Double.parseDouble(txtQuantite.getText().replace(",", "."));
            
            if (currentConsommation == null) {
                Consommation c = new Consommation(
                    quantite,
                    dpDate.getValue(),
                    selectedRessource.getId(),
                    cultureId
                );
                cs.ajouter(c);
                NotificationUtil.showSuccess(null, "Consommation enregistrée !");
            } else {
                currentConsommation.setQuantite(quantite);
                currentConsommation.setDateConsommation(dpDate.getValue());
                currentConsommation.setRessourceId(selectedRessource.getId());
                
                cs.modifier(currentConsommation);
                NotificationUtil.showSuccess(null, "Consommation mise à jour !");
            }
            close();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        } catch (NumberFormatException e) {
            showError(txtQuantite, lblErrorQuantite, "Veuillez saisir un nombre valide.");
        }
    }

    private boolean validate() {
        boolean valid = true;
        if (cbRessource.getValue() == null) {
            showError(cbRessource, lblErrorRessource, "Veuillez sélectionner une ressource.");
            valid = false;
        }
        if (txtQuantite.getText().trim().isEmpty()) {
            showError(txtQuantite, lblErrorQuantite, "La quantité est obligatoire.");
            valid = false;
        } else {
            try {
                double q = Double.parseDouble(txtQuantite.getText().replace(",", "."));
                if (q <= 0) {
                    showError(txtQuantite, lblErrorQuantite, "La quantité doit être positive.");
                    valid = false;
                } else if (cbRessource.getValue() != null && q > cbRessource.getValue().getStockRestant()) {
                    showError(txtQuantite, lblErrorQuantite, "Quantité supérieure au stock disponible.");
                    valid = false;
                }
            } catch (NumberFormatException e) {
                showError(txtQuantite, lblErrorQuantite, "Nombre invalide.");
                valid = false;
            }
        }
        if (dpDate.getValue() == null) {
            showError(dpDate, lblErrorDate, "La date est obligatoire.");
            valid = false;
        }
        return valid;
    }

    private void showError(Control field, Label errorLabel, String message) {
        if (!field.getStyleClass().contains("danger-field")) {
            field.getStyleClass().add("danger-field");
        }
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void clearErrors() {
        cbRessource.getStyleClass().remove("danger-field");
        txtQuantite.getStyleClass().remove("danger-field");
        dpDate.getStyleClass().remove("danger-field");

        lblErrorRessource.setVisible(false);
        lblErrorQuantite.setVisible(false);
        lblErrorDate.setVisible(false);
    }

    @FXML
    private void cancel() {
        close();
    }

    private void close() {
        ((Stage) txtQuantite.getScene().getWindow()).close();
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.show();
    }
}
