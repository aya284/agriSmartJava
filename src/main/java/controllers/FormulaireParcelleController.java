package controllers;

import entities.Parcelle;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
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

public class FormulaireParcelleController {
    @FXML
    private TextField txtNom, txtSurface, txtType, txtLat, txtLon;
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
    }

    private void loadInteractiveMap() {
        String content = "<html><head><link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'/><script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script></head>"
                + "<body style='margin:0;'><div id='map' style='height:100%; cursor:crosshair;'></div>"
                + "<script>"
                + "var map = L.map('map').setView([36.8065, 10.1815], 10);"
                + "L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png').addTo(map);"
                + "var marker;"
                + "map.on('click', function(e) {"
                + "  if(marker) map.removeLayer(marker);"
                + "  marker = L.marker(e.latlng).addTo(map);"
                + "  app.onMapClick(e.latlng.lat, e.latlng.lng);"
                + "});"
                + "function setLocation(lat, lon) {"
                + "  if(marker) map.removeLayer(marker);"
                + "  marker = L.marker([lat, lon]).addTo(map);"
                + "  map.setView([lat, lon], 13);"
                + "}"
                + "</script></body></html>";

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

        engine.loadContent(content);
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
        txtType.setText(p.getTypeSol());
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
        if (!validateInputs())
            return;

        try {
            Parcelle p = new Parcelle(
                    txtNom.getText(),
                    Double.parseDouble(txtSurface.getText().replace(",", ".")),
                    Double.parseDouble(txtLat.getText().replace(",", ".")),
                    Double.parseDouble(txtLon.getText().replace(",", ".")),
                    txtType.getText(),
                    1);

            if (currentId == -1) {
                ps.ajouter(p);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Parcelle enregistrée avec succès !");
            } else {
                p.setId(currentId);
                ps.modifier(p);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Parcelle mise à jour !");
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
        if (txtNom.getText().trim().isEmpty() || txtSurface.getText().isEmpty() ||
                txtType.getText().trim().isEmpty() || txtLat.getText().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Champs Requis",
                    "Veuillez remplir tout le formulaire et sélectionner un point sur la carte.");
            return false;
        }
        try {
            double surface = Double.parseDouble(txtSurface.getText().replace(",", "."));
            if (surface <= 0) {
                showAlert(Alert.AlertType.WARNING, "Valeur Invalide", "La surface doit être un nombre positif.");
                return false;
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Nombre Invalide", "Veuillez saisir une surface correcte (ex: 1.5).");
            return false;
        }
        return true;
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.show();
    }
}