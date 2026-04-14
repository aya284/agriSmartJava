package controllers;

import entities.Parcelle;
import entities.User;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Control;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import netscape.javascript.JSObject;
import services.ParcelleService;
import java.sql.SQLException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.Scanner;
import org.json.JSONObject;
import utils.NotificationUtil;
import utils.SessionManager;

public class FormulaireParcelleController {
    @FXML
    private TextField txtNom, txtSurface, txtLat, txtLon, txtTypeAutre;
    @FXML
    private javafx.scene.control.ComboBox<String> cbType;
    @FXML
    private Label lblErrorNom, lblErrorSurface, lblErrorType, lblErrorLocation, lblErrorTypeAutre;
    @FXML
    private javafx.scene.layout.VBox boxTypeAutre;
    @FXML
    private WebView webViewFormMap;
    @FXML
    private Label lblTitle;

    private ParcelleService ps = new ParcelleService();
    private int currentId = -1;
    private WebEngine engine;

    // Garder une référence forte pour éviter le Garbage Collection du pont JS
    private final MapBridge mapBridge = new MapBridge();
    private boolean isMapLoaded = false;
    private Parcelle pendingParcelle;

    @FXML
    public void initialize() {
        engine = webViewFormMap.getEngine();
        loadInteractiveMap();

        cbType.setItems(javafx.collections.FXCollections.observableArrayList(
                "Argileux", "Sableux", "Limoneux", "Calcaire", "Tourbeux", "Humifère", "Autre"));

        cbType.valueProperty().addListener((obs, oldVal, newVal) -> {
            boolean isAutre = "Autre".equals(newVal);
            boxTypeAutre.setVisible(isAutre);
            boxTypeAutre.setManaged(isAutre);
            if (!isAutre) {
                txtTypeAutre.clear();
                if (lblErrorTypeAutre != null) {
                    lblErrorTypeAutre.setVisible(false);
                    lblErrorTypeAutre.setManaged(false);
                }
                txtTypeAutre.getStyleClass().remove("danger-field");
            }
            if (txtNom.getScene() != null && txtNom.getScene().getWindow() != null) {
                ((javafx.stage.Stage) txtNom.getScene().getWindow()).sizeToScene();
            }
        });
    }

    private void loadInteractiveMap() {
        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) engine.executeScript("window");
                window.setMember("app", mapBridge);
                isMapLoaded = true;
                // Si des données attendaient le chargement de la carte
                if (pendingParcelle != null) {
                    updateMapLocation(pendingParcelle.getLatitude(), pendingParcelle.getLongitude());
                    pendingParcelle = null;
                }
            }
        });

        javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.millis(300));
        pause.setOnFinished(ev -> {
            java.net.URL cssResource = getClass().getResource("/leaflet/leaflet.css");
            java.net.URL jsResource = getClass().getResource("/leaflet/leaflet.js");
            String cssUrl = cssResource != null ? cssResource.toExternalForm()
                    : "https://unpkg.com/leaflet@1.9.4/dist/leaflet.css";
            String jsUrl = jsResource != null ? jsResource.toExternalForm()
                    : "https://unpkg.com/leaflet@1.9.4/dist/leaflet.js";

            String content = "<html><head><link rel='stylesheet' href='" + cssUrl + "'/><script src='" + jsUrl
                    + "'></script></head>"
                    + "<body style='margin:0;'><div id='map' style='height:100%; cursor:crosshair;'></div>"
                    + "<script>"
                    + "var customIcon = L.divIcon({ className: 'custom-icon', html: '<svg viewBox=\"0 0 24 24\" width=\"36\" height=\"36\" fill=\"#2D6A4F\"><path d=\"M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z\"/></svg>', iconSize: [36, 36], iconAnchor: [18, 36] });"
                    + "var map = L.map('map').setView([36.8065, 10.1815], 10);"
                    + "L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png').addTo(map);"
                    + "var marker;"
                    + "map.on('click', function(e) {"
                    + "  if(marker) map.removeLayer(marker);"
                    + "  marker = L.marker(e.latlng, {icon: customIcon}).addTo(map);"
                    + "  app.onMapClick(e.latlng.lat, e.latlng.lng);"
                    + "});"
                    + "function setLocation(lat, lon) {"
                    + "  if(marker) map.removeLayer(marker);"
                    + "  marker = L.marker([lat, lon], {icon: customIcon}).addTo(map);"
                    + "  map.setView([lat, lon], 13);"
                    + "}"
                    + "</script></body></html>";

            engine.loadContent(content);
        });
        pause.play();
    }

    /**
     * Pont entre JavaScript et Java
     */
    public class MapBridge {
        public void onMapClick(double lat, double lon) {
            Platform.runLater(() -> {
                txtLat.setText(String.format(Locale.US, "%.6f", lat));
                txtLon.setText(String.format(Locale.US, "%.6f", lon));
                reverseGeocode(lat, lon);
            });
        }
    }

    private void reverseGeocode(double lat, double lon) {
        new Thread(() -> {
            try {
                // Forcer la langue française avec accept-language=fr
                URL url = new URL("https://nominatim.openstreetmap.org/reverse?format=json&lat=" + lat
                        + "&lon=" + lon + "&accept-language=fr");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "AgriSmartApp/1.0");

                if (conn.getResponseCode() == 200) {
                    Scanner scanner = new Scanner(conn.getInputStream(), "UTF-8");
                    StringBuilder response = new StringBuilder();
                    while (scanner.hasNext())
                        response.append(scanner.nextLine());
                    scanner.close();

                    JSONObject json = new JSONObject(response.toString());
                    String displayName = json.optString("display_name", "");

                    if (!displayName.isEmpty()) {
                        String[] parts = displayName.split(",");
                        String suggestedName = (parts.length > 0) ? parts[0].trim() : displayName;

                        Platform.runLater(() -> {
                            txtNom.setText("Parcelle à " + suggestedName);
                        });
                    }
                }
            } catch (Exception e) {
                System.err.println("Geocoding error: " + e.getMessage());
            }
        }).start();
    }

    public void setParcelleData(Parcelle p) {
        currentId = p.getId();
        lblTitle.setText("Modification de la Parcelle");
        txtNom.setText(p.getNom());
        txtSurface.setText(String.valueOf(p.getSurface()));

        String t = p.getTypeSol();
        if (!cbType.getItems().contains(t)) {
            cbType.setValue("Autre");
            txtTypeAutre.setText(t);
        } else {
            cbType.setValue(t);
        }

        txtLat.setText(String.valueOf(p.getLatitude()));
        txtLon.setText(String.valueOf(p.getLongitude()));

        if (isMapLoaded) {
            updateMapLocation(p.getLatitude(), p.getLongitude());
        } else {
            pendingParcelle = p;
        }
    }

    private void updateMapLocation(double lat, double lon) {
        Platform.runLater(() -> {
            try {
                engine.executeScript("setLocation(" + lat + ", " + lon + ")");
            } catch (Exception e) {
                System.err.println("Map script failed: " + e.getMessage());
            }
        });
    }

    @FXML
    private void save() {
        if (!validateInputs()) {
            ((javafx.stage.Stage) txtNom.getScene().getWindow()).sizeToScene();
            return;
        }

        try {
            String finalType = cbType.getValue();
            if ("Autre".equals(finalType)) {
                finalType = txtTypeAutre.getText().trim();
            }

            Parcelle p = new Parcelle(
                    txtNom.getText().trim(),
                    Double.parseDouble(txtSurface.getText().replace(",", ".")),
                    Double.parseDouble(txtLat.getText().replace(",", ".")),
                    Double.parseDouble(txtLon.getText().replace(",", ".")),
                    finalType,
                    SessionManager.getInstance().getCurrentUser().getId());

            if (currentId == -1) {
                ps.ajouter(p);
                NotificationUtil.showSuccess(null, "Parcelle enregistrée avec succès !");
            } else {
                p.setId(currentId);
                ps.modifier(p);
                NotificationUtil.showSuccess(null, "Parcelle mise à jour !");
            }
            close();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Une erreur technique est survenue : " + e.getMessage());
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur de format", "Veuillez saisir des nombres valides.");
        } catch (RuntimeException e) {
            showAlert(Alert.AlertType.WARNING, "Erreur de Validation", e.getMessage());
        }
    }

    @FXML
    private void cancel() {
        close();
    }

    private void close() {
        ((Stage) txtNom.getScene().getWindow()).close();
    }

    private boolean validateInputs() {
        boolean valid = true;
        clearErrors();

        if (txtNom.getText().trim().isEmpty()) {
            showError(txtNom, lblErrorNom, "Le nom est obligatoire.");
            valid = false;
        } else if (txtNom.getText().trim().length() < 3) {
            showError(txtNom, lblErrorNom, "Le nom doit comporter au moins 3 caractères.");
            valid = false;
        }

        if (txtSurface.getText().isEmpty()) {
            showError(txtSurface, lblErrorSurface, "La surface est obligatoire.");
            valid = false;
        } else {
            try {
                double surface = Double.parseDouble(txtSurface.getText().replace(",", "."));
                if (surface <= 0) {
                    showError(txtSurface, lblErrorSurface, "La surface doit être positive.");
                    valid = false;
                } else if (surface > 50000) {
                    showError(txtSurface, lblErrorSurface, "La surface maximale autorisée est de 50,000 hectares.");
                    valid = false;
                }
            } catch (NumberFormatException e) {
                showError(txtSurface, lblErrorSurface, "Veuillez saisir un nombre valide.");
                valid = false;
            }
        }

        if (cbType.getValue() == null) {
            showError(cbType, lblErrorType, "Le type de sol est obligatoire.");
            valid = false;
        } else if ("Autre".equals(cbType.getValue()) && txtTypeAutre.getText().trim().isEmpty()) {
            showError(txtTypeAutre, lblErrorTypeAutre, "Veuillez préciser le type de sol.");
            valid = false;
        }

        if (txtLat.getText().isEmpty() || txtLon.getText().isEmpty()) {
            lblErrorLocation.setText("Veuillez sélectionner un point sur la carte.");
            lblErrorLocation.setVisible(true);
            lblErrorLocation.setManaged(true);
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
        txtSurface.getStyleClass().remove("danger-field");
        cbType.getStyleClass().remove("danger-field");
        txtTypeAutre.getStyleClass().remove("danger-field");

        lblErrorNom.setVisible(false);
        lblErrorSurface.setVisible(false);
        lblErrorType.setVisible(false);
        lblErrorTypeAutre.setVisible(false);
        lblErrorLocation.setVisible(false);
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.show();
    }
}