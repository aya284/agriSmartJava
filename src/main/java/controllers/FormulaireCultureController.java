package controllers;

import entities.Culture;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import services.CultureService;

import java.sql.SQLException;
import java.time.LocalDate;
import utils.NotificationUtil;

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
        if (!validate()) {
            ((javafx.stage.Stage) txtType.getScene().getWindow()).sizeToScene();
            return;
        }

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
                NotificationUtil.showSuccess(null, "Culture ajoutée avec succès !");
            } else {
                c.setId(currentId);
                cs.modifier(c);
                NotificationUtil.showSuccess(null, "Culture mise à jour !");
            }
            MainController.refreshNotifications();
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
        } else if (txtType.getText().trim().length() < 3) {
            showError(txtType, lblErrorType, "Type trop court (3 min).");
            valid = false;
        }

        if (txtVariete.getText().trim().isEmpty()) {
            showError(txtVariete, lblErrorVariete, "La variété est obligatoire.");
            valid = false;
        } else if (txtVariete.getText().trim().length() < 3) {
            showError(txtVariete, lblErrorVariete, "Variété trop courte (3 min).");
            valid = false;
        }

        LocalDate start = dpPlantation.getValue();
        LocalDate end = dpRecolte.getValue();

        if (start == null) {
            showError(dpPlantation, lblErrorPlantation, "Date de plantation requise.");
            valid = false;
        }

        if (end == null) {
            showError(dpRecolte, lblErrorRecolte, "Date de récolte requise.");
            valid = false;
        }

        if (start != null && end != null) {
            if (!end.isAfter(start)) {
                showError(dpRecolte, lblErrorRecolte, "La récolte doit être après la plantation.");
                showError(dpPlantation, lblErrorPlantation, "Date incohérente.");
                valid = false;
            }
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
