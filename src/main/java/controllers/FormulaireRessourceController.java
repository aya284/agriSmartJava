package controllers;

import entities.Ressource;
import entities.User;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import services.RessourceService;

import java.sql.SQLException;
import utils.NotificationUtil;
import utils.SessionManager;

public class FormulaireRessourceController {

    @FXML private Label lblTitle;
    @FXML private TextField txtNom, txtStock, txtTypeAutre;
    @FXML private ComboBox<String> cbType, cbUnite;
    @FXML private Label lblErrorNom, lblErrorType, lblErrorStock, lblErrorUnite, lblErrorTypeAutre;
    @FXML private javafx.scene.layout.VBox boxTypeAutre;

    private RessourceService rs = new RessourceService();
    private int currentId = -1;

    @FXML
    public void initialize() {
        cbType.setItems(FXCollections.observableArrayList("Engrais", "Semences", "Équipement", "Pesticides", "Autre"));
        cbUnite.setItems(FXCollections.observableArrayList("Kg", "Litres", "Pièces", "Sacs", "Heures"));

        cbType.valueProperty().addListener((obs, oldVal, newVal) -> {
            boolean isAutre = "Autre".equals(newVal);
            boxTypeAutre.setVisible(isAutre);
            boxTypeAutre.setManaged(isAutre);
            if (!isAutre) {
                txtTypeAutre.clear();
                lblErrorTypeAutre.setVisible(false);
                lblErrorTypeAutre.setManaged(false);
                txtTypeAutre.getStyleClass().remove("danger-field");
            }
            if (txtNom.getScene() != null && txtNom.getScene().getWindow() != null) {
                ((javafx.stage.Stage) txtNom.getScene().getWindow()).sizeToScene();
            }
        });
    }

    public void setRessourceData(Ressource r) {
        this.currentId = r.getId();
        lblTitle.setText("Modifier la Ressource");
        txtNom.setText(r.getNom());
        
        String t = r.getType();
        if(!cbType.getItems().contains(t)) {
            cbType.setValue("Autre");
            txtTypeAutre.setText(t);
        } else {
            cbType.setValue(t);
        }

        txtStock.setText(String.valueOf(r.getStockRestant()));
        cbUnite.setValue(r.getUnite());
    }

    @FXML
    private void save() {
        clearErrors();
        if (!validate()) {
            ((javafx.stage.Stage) txtNom.getScene().getWindow()).sizeToScene();
            return;
        }

        try {
            String finalType = cbType.getValue();
            if ("Autre".equals(finalType)) {
                finalType = txtTypeAutre.getText().trim();
            }

            Ressource r = new Ressource(
                txtNom.getText(),
                finalType,
                Double.parseDouble(txtStock.getText().replace(",", ".")),
                cbUnite.getValue(),
                SessionManager.getInstance().getCurrentUser().getId()
            );

            if (currentId == -1) {
                rs.ajouter(r);
                NotificationUtil.showSuccess(null, "Ressource ajoutée avec succès !");
            } else {
                r.setId(currentId);
                rs.modifier(r);
                NotificationUtil.showSuccess(null, "Ressource mise à jour !");
            }
            close();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur SQL", "Une erreur technique est survenue : " + e.getMessage());
        } catch (NumberFormatException e) {
            showError(txtStock, lblErrorStock, "Veuillez saisir un nombre valide.");
        }
    }

    private boolean validate() {
        boolean valid = true;
        if (txtNom.getText().trim().isEmpty()) {
            showError(txtNom, lblErrorNom, "Le nom est obligatoire.");
            valid = false;
        }
        if (cbType.getValue() == null) {
            showError(cbType, lblErrorType, "Veuillez sélectionner un type.");
            valid = false;
        } else if ("Autre".equals(cbType.getValue()) && txtTypeAutre.getText().trim().isEmpty()) {
            showError(txtTypeAutre, lblErrorTypeAutre, "Veuillez préciser le type de ressource.");
            valid = false;
        }
        if (txtStock.getText().trim().isEmpty()) {
            showError(txtStock, lblErrorStock, "Le stock est obligatoire.");
            valid = false;
        }
        if (cbUnite.getValue() == null) {
            showError(cbUnite, lblErrorUnite, "Veuillez sélectionner une unité.");
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
        txtNom.getStyleClass().remove("danger-field");
        cbType.getStyleClass().remove("danger-field");
        txtStock.getStyleClass().remove("danger-field");
        cbUnite.getStyleClass().remove("danger-field");

        lblErrorNom.setVisible(false);
        lblErrorType.setVisible(false);
        lblErrorStock.setVisible(false);
        lblErrorUnite.setVisible(false);

        txtTypeAutre.getStyleClass().remove("danger-field");
        lblErrorTypeAutre.setVisible(false);
    }

    @FXML
    private void cancel() {
        close();
    }

    private void close() {
        ((Stage) txtNom.getScene().getWindow()).close();
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.show();
    }
}
