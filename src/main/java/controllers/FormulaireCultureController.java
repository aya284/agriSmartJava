package controllers;

import entities.Culture;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import services.CultureService;

import java.sql.SQLException;
import java.time.LocalDate;

public class FormulaireCultureController {
    @FXML private Label lblTitle;
    @FXML private TextField txtType, txtVariete;
    @FXML private DatePicker dpPlantation, dpRecolte;
    @FXML private ComboBox<String> cbStatut;
    @FXML private Label lblErrorType, lblErrorVariete, lblErrorPlantation, lblErrorRecolte, lblErrorStatut;

    private CultureService cs = new CultureService();
    private int currentId = -1;
    private int parcelleId = -1;

    @FXML
    public void initialize() {
        cbStatut.setItems(FXCollections.observableArrayList("Planté", "En croissance", "Récolté"));
    }

    public void setParcelleId(int parcelleId) {
        this.parcelleId = parcelleId;
    }

    public void setCultureData(Culture c) {
        this.currentId = c.getId();
        this.parcelleId = c.getParcelleId();
        lblTitle.setText("Modifier la Culture");
        
        txtType.setText(c.getTypeCulture());
        txtVariete.setText(c.getVariete());
        dpPlantation.setValue(c.getDatePlantation());
        dpRecolte.setValue(c.getDateRecoltePrevue());
        cbStatut.setValue(c.getStatut());
    }

    @FXML
    private void save() {
        clearErrors();
        if (!validate()) return;

        Culture c = new Culture(
            txtType.getText(),
            txtVariete.getText(),
            dpPlantation.getValue(),
            dpRecolte.getValue(),
            cbStatut.getValue(),
            parcelleId
        );

        try {
            if (currentId == -1) {
                cs.ajouter(c);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Culture ajoutée avec succès !");
            } else {
                c.setId(currentId);
                cs.modifier(c);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Culture mise à jour !");
            }
            close();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur SQL", "Une erreur technique est survenue : " + e.getMessage());
        }
    }

    @FXML
    private void cancel() {
        close();
    }

    private void close() {
        ((Stage) txtType.getScene().getWindow()).close();
    }

    private boolean validate() {
        boolean valid = true;
        if (txtType.getText().trim().isEmpty()) {
            showError(txtType, lblErrorType, "Le type est obligatoire.");
            valid = false;
        }
        if (txtVariete.getText().trim().isEmpty()) {
            showError(txtVariete, lblErrorVariete, "La variété est obligatoire.");
            valid = false;
        }
        if (dpPlantation.getValue() == null) {
            showError(dpPlantation, lblErrorPlantation, "Date requise.");
            valid = false;
        }
        if (dpRecolte.getValue() == null) {
            showError(dpRecolte, lblErrorRecolte, "Date requise.");
            valid = false;
        }
        if (cbStatut.getValue() == null) {
            showError(cbStatut, lblErrorStatut, "Sélectionnez un statut.");
            valid = false;
        }
        return valid;
    }

    private void showError(Control field, Label errorLabel, String message) {
        field.getStyleClass().add("danger-field");
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void clearErrors() {
        txtType.getStyleClass().remove("danger-field");
        txtVariete.getStyleClass().remove("danger-field");
        dpPlantation.getStyleClass().remove("danger-field");
        dpRecolte.getStyleClass().remove("danger-field");
        cbStatut.getStyleClass().remove("danger-field");

        lblErrorType.setVisible(false);
        lblErrorVariete.setVisible(false);
        lblErrorPlantation.setVisible(false);
        lblErrorRecolte.setVisible(false);
        lblErrorStatut.setVisible(false);
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.show();
    }
}
