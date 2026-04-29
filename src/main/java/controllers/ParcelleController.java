package controllers;

import entities.Parcelle;
import entities.Culture;
import entities.Consommation;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import services.ParcelleService;
import services.CultureService;
import services.ConsommationService;
import services.WeatherService;
import services.SoilService;
import org.json.JSONObject;
import org.json.JSONArray;
import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import utils.NotificationUtil;
import utils.SessionManager;
import entities.User;

public class ParcelleController {

    @FXML
    private ListView<Parcelle> lvParcelles;
    @FXML
    private Label lblDetailNom, lblDetailSurface, lblDetailType, lblDetailCoords;
    @FXML
    private WebView webViewMap;
    @FXML
    private VBox detailsContainer;
    @FXML
    private VBox cultureListContainer;
    @FXML
    private TextField txtSearch;
    @FXML
    private VBox weatherContainer;
    @FXML
    private VBox soilAnalysisContainer;
    @FXML
    private Label lblSoilType, lblSoilPh, lblSoilHumidity, lblSoilFertility, lblSoilResult, lblSoilRecommendation;

    private WeatherService weatherS = new WeatherService();
    private SoilService soilS = new SoilService();

    private ParcelleService ps = new ParcelleService();
    private CultureService cs = new CultureService();
    private ConsommationService consS = new ConsommationService();
    private ObservableList<Parcelle> parcelleList = FXCollections.observableArrayList();
    private FilteredList<Parcelle> filteredList;
    private boolean isMapLoaded = false;
    private Parcelle pendingParcelle;

    @FXML
    public void initialize() {
        // Fixe la taille de la cellule pour empêcher le virtual flow de JavaFX de mal
        // calculer la hauteur (ce qui cause le chevauchement)
        lvParcelles.setFixedCellSize(95);

        setupSearch();
        setupSelectionListener();
        setupCellFactory();
        refreshList();
        loadMap();
        System.out.println("ParcelleController initialized. weatherContainer: " + weatherContainer);
    }

    private void setupSearch() {
        filteredList = new FilteredList<>(parcelleList, p -> true);
        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredList.setPredicate(p -> {
                if (newVal == null || newVal.isEmpty())
                    return true;
                String lowerCaseFilter = newVal.toLowerCase();
                return p.getNom().toLowerCase().contains(lowerCaseFilter) ||
                        p.getTypeSol().toLowerCase().contains(lowerCaseFilter);
            });
        });
        lvParcelles.setItems(filteredList);
    }

    private void setupSelectionListener() {
        lvParcelles.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                detailsContainer.setVisible(true);
                afficherDetails(newVal);
            } else {
                detailsContainer.setVisible(false);
            }
        });
    }

    private void setupCellFactory() {
        lvParcelles.setCellFactory(lv -> new ListCell<Parcelle>() {
            @Override
            protected void updateItem(Parcelle p, boolean empty) {
                super.updateItem(p, empty);
                if (empty || p == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox card = new HBox(15);
                    card.getStyleClass().add("parcel-list-card");
                    card.setAlignment(Pos.CENTER_LEFT);

                    Label icon = new Label("🌱");
                    icon.getStyleClass().add("parcel-icon");

                    VBox info = new VBox(4);
                    Label name = new Label(p.getNom());
                    name.getStyleClass().add("parcel-name");
                    Label details = new Label(p.getSurface() + " Ha • " + p.getTypeSol());
                    details.getStyleClass().add("parcel-subtext");

                    info.getChildren().addAll(name, details);
                    card.getChildren().addAll(icon, info);
                    setGraphic(card);
                }
            }
        });
    }

    private void refreshList() {
        try {
            Parcelle selected = lvParcelles.getSelectionModel().getSelectedItem();
            int selectedId = selected != null ? selected.getId() : -1;

            User current = SessionManager.getInstance().getCurrentUser();
            if (current != null) {
                if ("admin".equalsIgnoreCase(current.getRole())) {
                    parcelleList.setAll(ps.afficher());
                } else {
                    parcelleList.setAll(ps.afficherByUser(current.getId()));
                }
            }

            // Si filteredList est déjà installé
            if (lvParcelles.getItems() != null && !lvParcelles.getItems().isEmpty()) {
                if (selectedId != -1) {
                    for (Parcelle p : lvParcelles.getItems()) {
                        if (p.getId() == selectedId) {
                            lvParcelles.getSelectionModel().select(p);
                            return;
                        }
                    }
                }
                // Par défaut, sélectionner le premier s'il y en a un
                lvParcelles.getSelectionModel().selectFirst();
            }
        } catch (SQLException e) {
            showError("Erreur SQL", "Impossible de charger les parcelles : " + e.getMessage());
        }
    }

    private void loadMap() {
        webViewMap.getEngine().getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                isMapLoaded = true;
                if (pendingParcelle != null) {
                    afficherDetails(pendingParcelle);
                    pendingParcelle = null;
                }
            }
        });

        javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.millis(300));
        pause.setOnFinished(e -> {
            java.net.URL cssResource = getClass().getResource("/leaflet/leaflet.css");
            java.net.URL jsResource = getClass().getResource("/leaflet/leaflet.js");
            String cssUrl = cssResource != null ? cssResource.toExternalForm() : "https://unpkg.com/leaflet@1.9.4/dist/leaflet.css";
            String jsUrl = jsResource != null ? jsResource.toExternalForm() : "https://unpkg.com/leaflet@1.9.4/dist/leaflet.js";

            String content = "<html><head><link rel='stylesheet' href='" + cssUrl + "'/><script src='" + jsUrl + "'></script></head>"
                    + "<body style='margin:0;'><div id='map' style='height:100%;'></div>"
                    + "<script>"
                    + "var map = L.map('map').setView([36.8065, 10.1815], 6); "
                    + "L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png').addTo(map); "
                    + "var customIcon = L.divIcon({ className: 'custom-icon', html: '<svg viewBox=\"0 0 24 24\" width=\"36\" height=\"36\" fill=\"#2D6A4F\"><path d=\"M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z\"/></svg>', iconSize: [36, 36], iconAnchor: [18, 36] });"
                    + "var markers = []; var polygon; "
                    + "function resetMap() {"
                    + "  markers.forEach(m => map.removeLayer(m)); markers = [];"
                    + "  if(polygon) map.removeLayer(polygon); polygon = null;"
                    + "}"
                    + "function setCoordinates(json) {"
                    + "  resetMap(); if(!json) return;"
                    + "  try {"
                    + "    var pts = JSON.parse(json);"
                    + "    var points = pts.map(p => [p.lat, p.lng]);"
                    + "    points.forEach(p => {"
                    + "      var m = L.marker(p, {icon: customIcon}).addTo(map);"
                    + "      markers.push(m);"
                    + "    });"
                    + "    if(points.length > 1) {"
                    + "      polygon = L.polygon(points, {color: '#2D6A4F', fillOpacity: 0.3}).addTo(map);"
                    + "      map.fitBounds(polygon.getBounds());"
                    + "    }"
                    + "  } catch(e) { console.error(e); }"
                    + "}"
                    + "function setLocation(lat, lon) {"
                    + "  resetMap();"
                    + "  var m = L.marker([lat, lon], {icon: customIcon}).addTo(map);"
                    + "  markers.push(m);"
                    + "  map.setView([lat, lon], 13);"
                    + "}"
                    + "</script></body></html>";
            webViewMap.getEngine().loadContent(content);
        });
        pause.play();
    }

    private void afficherDetails(Parcelle p) {
        if (soilAnalysisContainer != null) {
            soilAnalysisContainer.setVisible(false);
        }
        
        if (!isMapLoaded) {
            pendingParcelle = p;
            lblDetailNom.setText(p.getNom());
            lblDetailSurface.setText(p.getSurface() + " Hectares");
            lblDetailType.setText(p.getTypeSol());
            lblDetailCoords.setText("Lat: " + p.getLatitude() + " | Lon: " + p.getLongitude());
            return;
        }

        lblDetailNom.setText(p.getNom());
        lblDetailSurface.setText(p.getSurface() + " Hectares");
        lblDetailType.setText(p.getTypeSol());
        lblDetailCoords.setText("Lat: " + p.getLatitude() + " | Lon: " + p.getLongitude());

        try {
            String coords = p.getCoordonnees();
            if (coords != null && !coords.isEmpty() && !coords.equals("[]")) {
                webViewMap.getEngine().executeScript("setCoordinates('" + coords + "')");
            } else {
                webViewMap.getEngine().executeScript("setLocation(" + p.getLatitude() + "," + p.getLongitude() + ")");
            }
        } catch (Exception e) {
            System.err.println("Map script error: " + e.getMessage());
        }

        loadCultures(p.getId());
        loadWeather(p);
    }

    private void loadWeather(Parcelle p) {
        if (weatherContainer == null) {
            System.err.println("weatherContainer is NULL!");
            return;
        }
        weatherContainer.getChildren().clear();
        Label loading = new Label("Chargement de la météo pour " + p.getNom() + "...");
        loading.getStyleClass().add("panel-muted");
        weatherContainer.getChildren().add(loading);

        // Run in background
        Thread thread = new Thread(() -> {
            try {
                JSONObject weatherData = weatherS.getWeatherData(p.getLatitude(), p.getLongitude());
                javafx.application.Platform.runLater(() -> {
                    weatherContainer.getChildren().clear();
                    if (weatherData != null) {
                        displayWeather(weatherData);
                    } else {
                        weatherContainer.getChildren().add(new Label("Erreur: Impossible de joindre le service météo."));
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                javafx.application.Platform.runLater(() -> {
                    weatherContainer.getChildren().clear();
                    weatherContainer.getChildren().add(new Label("Erreur interne: " + e.getMessage()));
                });
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private void displayWeather(JSONObject data) {
        JSONObject current = data.getJSONObject("current");
        double temp = current.getDouble("temperature_2m");
        int humidity = current.getInt("relative_humidity_2m");
        double wind = current.getDouble("wind_speed_10m");
        double rain = current.getDouble("rain");
        int code = current.getInt("weather_code");

        // Current Weather Card
        VBox currentCard = new VBox(15);
        currentCard.getStyleClass().add("weather-card-current");

        HBox header = new HBox(20);
        header.setAlignment(Pos.CENTER_LEFT);

        Label iconLarge = new Label(weatherS.getWeatherIcon(code));
        iconLarge.getStyleClass().addAll("weather-icon-large", "weather-icon-emoji");
        iconLarge.setMinWidth(80);
        iconLarge.setAlignment(Pos.CENTER);

        VBox tempBox = new VBox(2);
        Label lblTemp = new Label(temp + "°C");
        lblTemp.getStyleClass().add("weather-temp-main");
        Label lblDesc = new Label(weatherS.getWeatherDescription(code));
        lblDesc.getStyleClass().add("weather-desc-main");
        tempBox.getChildren().addAll(lblTemp, lblDesc);

        header.getChildren().addAll(iconLarge, tempBox);

        HBox infoRow = new HBox(10);
        infoRow.setAlignment(Pos.CENTER);
        infoRow.getChildren().addAll(
            createWeatherInfoBox("Humidité", humidity + "%", "💧"),
            createWeatherInfoBox("Vent", wind + " km/h", "💨"),
            createWeatherInfoBox("Pluie", rain + " mm", "☔")
        );

        currentCard.getChildren().addAll(header, infoRow);
        weatherContainer.getChildren().add(currentCard);

        // Alerts
        displayAlerts(current);

        // Forecast
        Label lblForecast = new Label("Prévisions sur 7 jours");
        lblForecast.getStyleClass().add("panel-title");
        lblForecast.setStyle("-fx-font-size: 14px; -fx-padding: 10 0 0 0;");
        weatherContainer.getChildren().add(lblForecast);

        ScrollPane forecastScroll = new ScrollPane();
        forecastScroll.setFitToHeight(true);
        forecastScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        
        HBox forecastBox = new HBox(10);
        forecastBox.setPadding(new Insets(5, 0, 10, 0));
        
        JSONObject daily = data.getJSONObject("daily");
        JSONArray times = daily.getJSONArray("time");
        JSONArray tempsMax = daily.getJSONArray("temperature_2m_max");
        JSONArray tempsMin = daily.getJSONArray("temperature_2m_min");
        JSONArray codes = daily.getJSONArray("weather_code");

        for (int i = 0; i < times.length(); i++) {
            forecastBox.getChildren().add(createForecastCard(
                times.getString(i),
                tempsMax.getDouble(i),
                tempsMin.getDouble(i),
                codes.getInt(i)
            ));
        }
        
        forecastScroll.setContent(forecastBox);
        weatherContainer.getChildren().add(forecastScroll);
    }

    private VBox createWeatherInfoBox(String label, String value, String icon) {
        VBox box = new VBox(2);
        box.getStyleClass().add("weather-info-box");
        box.setAlignment(Pos.CENTER);
        HBox.setHgrow(box, Priority.ALWAYS);

        Label l = new Label(icon + " " + label);
        l.getStyleClass().addAll("weather-info-label", "weather-icon-emoji");
        Label v = new Label(value);
        v.getStyleClass().add("weather-info-value");

        box.getChildren().addAll(l, v);
        return box;
    }

    private void displayAlerts(JSONObject current) {
        double temp = current.getDouble("temperature_2m");
        double wind = current.getDouble("wind_speed_10m");
        double rain = current.getDouble("rain");
        int code = current.getInt("weather_code");

        // --- SMART ALERTS (CRITICAL) ---
        if (temp > 35) {
            addAlert("Forte Chaleur", "Risque de stress hydrique. Augmentez l'irrigation et évitez le travail intensif.", "danger");
        } else if (temp < 2) {
            addAlert("Risque de Gel", "Protégez les cultures sensibles avec des voiles thermiques.", "danger");
        }

        if (wind > 40) {
            addAlert("Vent Fort", "Évitez tout traitement chimique aujourd'hui pour limiter la dérive.", "warning");
        }

        if (rain > 15) {
            addAlert("Pluie Intense", "Risque de lessivage. Ne fertilisez pas et ne pulvérisez pas de pesticides.", "warning");
        }

        // --- SMART ADVICE (OPTIMIZATION) ---
        if (rain == 0 && temp > 15 && temp < 28 && wind < 15) {
            addAlert("Conditions Idéales", "Moment parfait pour la fertilisation ou les traitements phytosanitaires.", "success");
        } else if (code >= 0 && code <= 3 && rain == 0 && wind < 25) {
            addAlert("Conseil Récolte", "Le temps est sec et stable : idéal pour la récolte ou le labour.", "info");
        }
    }

    private void addAlert(String title, String msg, String type) {
        VBox alert = new VBox(5);
        alert.getStyleClass().addAll("weather-alert-box", "weather-alert-" + type);
        
        String icon = "🔔 ";
        String color = "#d35400";
        
        if (type.equals("danger")) { icon = "⚠️ "; color = "#c0392b"; }
        else if (type.equals("success")) { icon = "🌱 "; color = "#27ae60"; }
        else if (type.equals("info")) { icon = "💡 "; color = "#2980b9"; }
        
        Label t = new Label(icon + title);
        t.getStyleClass().addAll("weather-alert-title", "weather-icon-emoji");
        t.setStyle("-fx-text-fill: " + color + ";");
        
        Label m = new Label(msg);
        m.getStyleClass().add("weather-alert-msg");
        m.setWrapText(true);
        
        alert.getChildren().addAll(t, m);
        weatherContainer.getChildren().add(alert);
    }

    private VBox createForecastCard(String date, double max, double min, int code) {
        VBox card = new VBox(8);
        card.getStyleClass().add("weather-card-forecast");
        
        String day = date.substring(5); // MM-DD
        Label lblDay = new Label(day);
        lblDay.getStyleClass().add("weather-forecast-day");
        
        Label icon = new Label(weatherS.getWeatherIcon(code));
        icon.getStyleClass().addAll("weather-forecast-icon", "weather-icon-emoji");
        icon.setMinWidth(40);
        icon.setAlignment(Pos.CENTER);
        
        Label lblTemp = new Label(Math.round(max) + "° / " + Math.round(min) + "°");
        lblTemp.getStyleClass().add("weather-forecast-temp");
        
        card.getChildren().addAll(lblDay, icon, lblTemp);
        return card;
    }

    private void loadCultures(int parcelleId) {
        cultureListContainer.getChildren().clear();
        try {
            List<Culture> cultures = cs.getByParcelle(parcelleId);
            if (cultures.isEmpty()) {
                Label placeholder = new Label("Aucune culture enregistrée.");
                placeholder.setStyle("-fx-text-fill: #999; -fx-italic: true;");
                cultureListContainer.getChildren().add(placeholder);
            } else {
                for (Culture c : cultures) {
                    cultureListContainer.getChildren().add(createCultureCard(c));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading cultures: " + e.getMessage());
        }
    }

    private VBox createCultureCard(Culture c) {
        VBox card = new VBox(10);
        card.getStyleClass().add("culture-card");

        // Header
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label icon = new Label("🌿");
        icon.getStyleClass().add("culture-icon-box");

        VBox titleArea = new VBox(2);
        Label typeLabel = new Label(c.getTypeCulture());
        typeLabel.getStyleClass().add("culture-card-title");
        Label varLabel = new Label("Variété : " + c.getVariete());
        varLabel.getStyleClass().add("culture-card-subtitle");
        titleArea.getChildren().addAll(typeLabel, varLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnUtiliser = new Button("Utiliser");
        btnUtiliser.getStyleClass().add("btn-primary");
        btnUtiliser.setStyle("-fx-font-size: 10px; -fx-padding: 3 8; -fx-background-color: #27ae60;");
        btnUtiliser.setOnAction(e -> openConsommationModal(c));

        Button btnDiagnostic = new Button("🔍 Diagnostic");
        btnDiagnostic.getStyleClass().add("btn-primary");
        btnDiagnostic.setStyle("-fx-font-size: 10px; -fx-padding: 3 8; -fx-background-color: #8e44ad;");
        btnDiagnostic.setOnAction(e -> openDiagnosticModal(c));

        Button btnYield = new Button("📈 Rendement");
        btnYield.getStyleClass().add("btn-primary");
        btnYield.setStyle("-fx-font-size: 10px; -fx-padding: 3 8; -fx-background-color: #e67e22;");
        btnYield.setOnAction(e -> openYieldPredictionModal(c));

        Button btnEdit = new Button("✎");
        btnEdit.getStyleClass().add("culture-action-btn");
        btnEdit.setOnAction(e -> openEditCultureModal(c));

        Button btnDel = new Button("🗑");
        btnDel.getStyleClass().add("culture-action-btn-danger");
        btnDel.setOnAction(e -> handleCultureDelete(c));

        VBox actionsBox = new VBox(5);
        actionsBox.setAlignment(Pos.CENTER_RIGHT);

        HBox topActions = new HBox(5);
        topActions.setAlignment(Pos.CENTER_RIGHT);
        topActions.getChildren().addAll(btnUtiliser, btnEdit, btnDel);

        HBox bottomActions = new HBox(5);
        bottomActions.setAlignment(Pos.CENTER_RIGHT);
        bottomActions.getChildren().addAll(btnDiagnostic, btnYield);

        actionsBox.getChildren().addAll(topActions, bottomActions);

        header.getChildren().addAll(icon, titleArea, spacer, actionsBox);

        // Dates
        HBox dateGrid = new HBox(0);
        dateGrid.getStyleClass().add("culture-date-grid");

        VBox plantCol = createDateCol("Plantation",
                c.getDatePlantation().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        Separator sep = new Separator();
        sep.setOrientation(javafx.geometry.Orientation.VERTICAL);
        VBox harvestCol = createDateCol("Récolte",
                c.getDateRecoltePrevue().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

        HBox.setHgrow(plantCol, Priority.ALWAYS);
        HBox.setHgrow(harvestCol, Priority.ALWAYS);
        dateGrid.getChildren().addAll(plantCol, sep, harvestCol);

        // Status
        Label statusBadge = new Label(c.getStatut());
        statusBadge.getStyleClass().add("culture-status-badge");
        if (c.getStatut().equals("Récolté"))
            statusBadge.getStyleClass().add("status-harvested");
        else if (c.getStatut().equals("Planté"))
            statusBadge.getStyleClass().add("status-planted");

        // Consommation (Affichage au dessus / dans la carte)
        VBox consumptionBox = new VBox(5);
        consumptionBox.setStyle("-fx-padding: 10 0 0 0;");
        try {
            List<Consommation> consumptions = consS.getByCulture(c.getId());
            if (!consumptions.isEmpty()) {
                Label consTitle = new Label("Consommation :");
                consTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #27ae60;");
                consumptionBox.getChildren().add(consTitle);

                for (Consommation cons : consumptions) {
                    HBox row = new HBox(5);
                    row.setAlignment(Pos.CENTER_LEFT);

                    Label l = new Label("• " + cons.getQuantite() + " " + cons.getUnite() + " " + cons.getRessourceNom()
                            + " (" + cons.getDateConsommation().format(DateTimeFormatter.ofPattern("dd/MM")) + ")");
                    l.setStyle("-fx-font-size: 10.5px; -fx-text-fill: #555;");

                    Region rSpacer = new Region();
                    HBox.setHgrow(rSpacer, Priority.ALWAYS);

                    Button btnMiniEdit = new Button("✎");
                    btnMiniEdit.getStyleClass().add("culture-action-btn");
                    btnMiniEdit.setStyle("-fx-font-size: 8px; -fx-padding: 2 5;");
                    btnMiniEdit.setOnAction(e -> openEditConsommationModal(cons, c));

                    Button btnMiniDel = new Button("🗑");
                    btnMiniDel.getStyleClass().add("culture-action-btn-danger");
                    btnMiniDel.setStyle("-fx-font-size: 8px; -fx-padding: 2 5;");
                    btnMiniDel.setOnAction(e -> handleConsommationDelete(cons, c));

                    row.getChildren().addAll(l, rSpacer, btnMiniEdit, btnMiniDel);
                    consumptionBox.getChildren().add(row);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        card.getChildren().addAll(header, consumptionBox, dateGrid, statusBadge);
        return card;
    }

    private VBox createDateCol(String label, String date) {
        VBox col = new VBox(4);
        col.setAlignment(Pos.CENTER);
        col.setPadding(new Insets(5));
        Label l = new Label(label);
        l.getStyleClass().add("culture-date-label");
        Label d = new Label(date);
        d.getStyleClass().add("culture-date-value");
        col.getChildren().addAll(l, d);
        return col;
    }

    @FXML
    private void openAddCultureModal() throws IOException {
        Parcelle p = lvParcelles.getSelectionModel().getSelectedItem();
        if (p == null)
            return;

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/Views/FormulaireCulture.fxml"));
        Parent root = loader.load();
        FormulaireCultureController controller = loader.getController();
        controller.setParcelleId(p.getId());

        showCultureStage(root, "Ajouter une Culture", p.getId());
    }

    private void openEditCultureModal(Culture c) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Views/FormulaireCulture.fxml"));
            Parent root = loader.load();
            FormulaireCultureController controller = loader.getController();
            controller.setCultureData(c);

            showCultureStage(root, "Modifier la Culture", c.getParcelleId());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openConsommationModal(Culture c) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Views/ConsommationForm.fxml"));
            Parent root = loader.load();
            ConsommationFormController controller = loader.getController();
            controller.setCultureData(c);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Enregistrer une Consommation");
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            stage.setScene(scene);
            stage.showAndWait();

            // Rafraîchir l'affichage pour voir la consommation
            Parcelle selected = lvParcelles.getSelectionModel().getSelectedItem();
            if (selected != null)
                loadCultures(selected.getId());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openDiagnosticModal(Culture c) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Views/DiagnosticModal.fxml"));
            Parent root = loader.load();
            DiagnosticController controller = loader.getController();
            Parcelle selected = lvParcelles.getSelectionModel().getSelectedItem();
            controller.setContext(c, selected);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Diagnostic Botanique IA");
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            stage.setScene(scene);
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openYieldPredictionModal(Culture c) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Views/YieldPredictionModal.fxml"));
            Parent root = loader.load();
            YieldPredictionController controller = loader.getController();
            Parcelle selected = lvParcelles.getSelectionModel().getSelectedItem();
            
            // Récupérer les consommations pour cette culture
            List<Consommation> consumptions = consS.getByCulture(c.getId());
            
            controller.setContext(selected, c, consumptions);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Prédiction de Rendement IA");
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            stage.setScene(scene);
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur", "Impossible d'ouvrir la fenêtre de prédiction : " + e.getMessage());
        }
    }

    private void openEditConsommationModal(Consommation cons, Culture c) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Views/ConsommationForm.fxml"));
            Parent root = loader.load();
            ConsommationFormController controller = loader.getController();
            controller.setConsommationData(cons, c);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Modifier la Consommation");
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            stage.setScene(scene);
            stage.showAndWait();

            loadCultures(c.getParcelleId());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleConsommationDelete(Consommation cons, Culture c) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Suppression");
        alert.setHeaderText("Supprimer cette consommation ?");
        alert.setContentText(
                "Le stock consommé (" + cons.getQuantite() + " " + cons.getUnite() + ") sera restitué à l'inventaire.");

        if (alert.showAndWait().get() == ButtonType.OK) {
            try {
                consS.supprimer(cons.getId());
                NotificationUtil.showDelete(lvParcelles.getScene().getWindow(), "Consommation supprimée.");
                loadCultures(c.getParcelleId());
            } catch (SQLException e) {
                showError("Erreur", e.getMessage());
            }
        }
    }

    private void showCultureStage(Parent root, String title, int parcelleId) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle(title);
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        stage.setScene(scene);
        stage.showAndWait();
        loadCultures(parcelleId);
    }

    private void handleCultureDelete(Culture c) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Suppression");
        alert.setHeaderText("Supprimer la culture ?");
        alert.setContentText("Voulez-vous vraiment supprimer cette culture de la parcelle ?");
        if (alert.showAndWait().get() == ButtonType.OK) {
            try {
                cs.supprimer(c.getId());
                NotificationUtil.showDelete(lvParcelles.getScene().getWindow(), "Culture supprimée.");
                loadCultures(c.getParcelleId());
            } catch (SQLException e) {
                showError("Erreur", "Suppression impossible : " + e.getMessage());
            }
        }
    }

    @FXML
    private void openAddModal() throws IOException {
        showModal(null);
    }

    @FXML
    private void openEditModal() throws IOException {
        Parcelle selected = lvParcelles.getSelectionModel().getSelectedItem();
        if (selected != null)
            showModal(selected);
    }

    private void showModal(Parcelle p) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/Views/FormulaireParcelle.fxml"));
        Parent root = loader.load();
        FormulaireParcelleController controller = loader.getController();
        if (p != null)
            controller.setParcelleData(p);

        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle(p == null ? "Ajouter une Parcelle" : "Modifier la Parcelle");
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        stage.setScene(scene);
        stage.showAndWait();
        refreshList();
    }

    @FXML
    private void handleDelete() {
        Parcelle selected = lvParcelles.getSelectionModel().getSelectedItem();
        if (selected != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirmation");
            alert.setHeaderText("Supprimer " + selected.getNom() + " ?");
            if (alert.showAndWait().get() == ButtonType.OK) {
                try {
                    ps.supprimer(selected.getId());
                    NotificationUtil.showDelete(lvParcelles.getScene().getWindow(), "Parcelle supprimée.");
                    refreshList();
                    detailsContainer.setVisible(false);
                } catch (SQLException e) {
                    showError("Erreur", e.getMessage());
                }
            }
        }
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    private void analyzeSoil() {
        Parcelle selected = lvParcelles.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Erreur", "Veuillez sélectionner une parcelle d'abord.");
            return;
        }

        // Afficher un message de chargement
        lblSoilType.setText("Chargement...");
        lblSoilPh.setText("Chargement...");
        lblSoilHumidity.setText("Chargement...");
        lblSoilFertility.setText("Chargement...");
        lblSoilResult.setText("Récupération des données API...");
        lblSoilRecommendation.setText("");
        soilAnalysisContainer.setVisible(true);

        Thread thread = new Thread(() -> {
            try {
                // Appel API réel (ISRIC SoilGrids - Spécifique au sol)
                JSONObject soilData = soilS.getRealSoilData(selected.getLatitude(), selected.getLongitude());

                javafx.application.Platform.runLater(() -> {
                    // Valeurs par défaut (Fallback si l'API est indisponible)
                    String typeSol = "Inconnu";
                    double ph = 6.5;
                    double realHumidityPercent = 40.0; 
                    int fertility = 50;
                    
                    boolean apiSuccess = false;

                    // Si l'API distincte de Sol a répondu
                    if (soilData != null && soilData.has("properties")) {
                        apiSuccess = true;
                        try {
                            org.json.JSONArray layers = soilData.getJSONObject("properties").getJSONArray("layers");
                            int sand = 0; int clay = 0;
                            
                            for (int i = 0; i < layers.length(); i++) {
                                JSONObject layer = layers.getJSONObject(i);
                                String name = layer.getString("name");
                                int meanValue = layer.getJSONArray("depths").getJSONObject(0).getJSONObject("values").getInt("mean");
                                
                                if (name.equals("phh2o")) ph = meanValue / 10.0;
                                else if (name.equals("sand")) sand = meanValue;
                                else if (name.equals("clay")) clay = meanValue;
                            }
                            
                            // Déduction du type de sol selon la texture
                            if (sand > 500) typeSol = "sableux";
                            else if (clay > 400) typeSol = "argileux";
                            else typeSol = "limoneux";
                            
                            // Fertilité estimée à partir de la qualité du sol (pH proche de 6.5 = très fertile)
                            fertility = 100 - (int)(Math.abs(ph - 6.5) * 20);
                            
                        } catch (Exception ex) {
                            apiSuccess = false;
                            System.err.println("Erreur parsing SoilGrids: " + ex.getMessage());
                        }
                    } 
                    
                    if (!apiSuccess) {
                        // Fallback logique en cas de non réponse de l'API (ISRIC est souvent très lente)
                        String[] types = {"sableux", "argileux", "limoneux"};
                        typeSol = types[Math.abs(selected.getNom().hashCode() + selected.getId()) % 3];
                        ph = 5.5 + (Math.abs(selected.getLatitude() * 100) % 3.0);
                        realHumidityPercent = 30 + (Math.abs(selected.getLongitude() * 100) % 50.0);
                        fertility = 40 + (Math.abs((int)selected.getSurface() * 10) % 60);
                    }

                    // 4. Affichage des résultats
                    lblSoilType.setText(typeSol + (apiSuccess ? " (API)" : " (Simulé)"));
                    lblSoilPh.setText(String.format(java.util.Locale.US, "%.1f", ph) + (apiSuccess ? " (API)" : " (Simulé)"));
                    lblSoilHumidity.setText(String.format(java.util.Locale.US, "%.1f", realHumidityPercent) + " %");
                    lblSoilFertility.setText(fertility + " %");

                    lblSoilResult.setText("📌 Résultat : \"Cette parcelle a un sol " + typeSol + " avec pH " + String.format(java.util.Locale.US, "%.1f", ph) + "\"");

                    // 5. Recommandations
                    String reco = "🌾 Action recommandée :\n";
                    if (typeSol.equals("sableux")) {
                        reco += "💧 Le sol est drainant. Arroser souvent.";
                    } else if (typeSol.equals("argileux")) {
                        reco += "⚠️ Retient beaucoup d'eau. Arroser moins.";
                    } else {
                        reco += "💧 Irrigation modérée (conditions optimales).";
                    }
                    lblSoilRecommendation.setText(reco);

                });
            } catch (Exception e) {
                e.printStackTrace();
                javafx.application.Platform.runLater(() -> {
                    lblSoilResult.setText("❌ Erreur lors de l'analyse : " + e.getMessage());
                });
            }
        });
        thread.setDaemon(true);
        thread.start();
    }
    @FXML
    private void recommanderCultureIA() {
        Parcelle selected = lvParcelles.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Erreur", "Veuillez sélectionner une parcelle d'abord.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Views/RecommendationModal.fxml"));
            Parent root = loader.load();
            RecommendationModalController controller = loader.getController();
            controller.setParcelle(selected);
            controller.setOnCultureAdded(() -> loadCultures(selected.getId()));

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Agronome Virtuel (IA)");
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            stage.setScene(scene);
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
            showError("Erreur", "Impossible d'ouvrir l'assistant IA.");
        }
    }
}