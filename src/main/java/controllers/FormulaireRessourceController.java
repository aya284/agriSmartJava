package controllers;

import entities.Ressource;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import services.RessourceService;

import java.sql.SQLException;

public class FormulaireRessourceController {

    @FXML private Label lblTitle;
    @FXML private TextField txtNom, txtStock;
    @FXML private ComboBox<String> cbType, cbUnite;
    @FXML private Label lblErrorNom, lblErrorType, lblErrorStock, lblErrorUnite;

    private RessourceService rs = new RessourceService();
    private int currentId = -1;

    @FXML
    public void initialize() {
        cbType.setItems(FXCollections.observableArrayList("Engrais", "Semences", "Équipement", "Pesticides", "Autre"));
        cbUnite.setItems(FXCollections.observableArrayList("Kg", "Litres", "Pièces", "Sacs", "Heures"));
    }

    public void setRessourceData(Ressource r) {
        this.currentId = r.getId();
        lblTitle.setText("Modifier la Ressource");
        txtNom.setText(r.getNom());
        cbType.setValue(r.getType());
        txtStock.setText(String.valueOf(r.getStockRestant()));
        cbUnite.setValue(r.getUnite());
    }

    @FXML
    private void save() {
        clearErrors();
        if (!validate()) return;

        try {
            Ressource r = new Ressource(
                txtNom.getText(),
                cbType.getValue(),
                Double.parseDouble(txtStock.getText().replace(",", ".")),
                cbUnite.getValue(),
                1 // User ID statique pour le moment
            );

            if (currentId == -1) {
                rs.ajouter(r);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Ressource ajoutée avec succès !");
            } else {
                r.setId(currentId);
                rs.modifier(r);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Ressource mise à jour !");
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
