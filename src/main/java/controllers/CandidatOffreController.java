package controllers;

import entities.Offre;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import services.DemandeService;
import services.OffreService;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class CandidatOffreController implements Initializable {

    // ── Offres ──────────────────────────────────────────────
    @FXML private FlowPane cardsContainer;
    @FXML private TextField searchField;
    @FXML private Label countLabel;
    @FXML private ComboBox<String> statutFilter;

    // ── Détail ──────────────────────────────────────────────
    @FXML private Label detailTitle, detailDesc, detailLieu,
            detailSalaire, detailDebut, detailFin, dateLimitLabel;
    @FXML private Button btnPostuler;

    // ── Chatbot ─────────────────────────────────────────────
    @FXML private VBox chatContainer;
    @FXML private VBox chatMessagesArea;
    @FXML private ScrollPane chatScrollPane;
    @FXML private TextField chatInputField;

    // ── Historique conversation ──────────────────────────────
    private final List<String[]> conversationHistory = new ArrayList<>();
    private boolean isFirstOpen = true;

    // ── API Mistral ──────────────────────────────────────────
    private static final String MISTRAL_KEY = "nA6CO7ubQH56abPXG6GfeymxyNV8B2oT";
    private static final String MISTRAL_URL = "https://api.mistral.ai/v1/chat/completions";

    // ── Service ─────────────────────────────────────────────
    private final OffreService service = new OffreService();
    public static Offre selectedOffreForView = null;

    // ════════════════════════════════════════════════════════
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (statutFilter != null) {
            statutFilter.setItems(FXCollections.observableArrayList(
                    "Toutes les offres", "Ouvert", "Clôturée"));
            statutFilter.setValue("Toutes les offres");
            statutFilter.setOnAction(e -> handleSearch());
        }
        if (cardsContainer != null) loadCandidateData();
        if (detailTitle != null && selectedOffreForView != null) setupDetailPage();
    }

    // ════════════════════════════════════════════════════════
    //  LISTE DES OFFRES
    // ════════════════════════════════════════════════════════
    private void loadCandidateData() {
        try {
            List<Offre> list = service.afficher();
            cardsContainer.getChildren().clear();
            for (Offre o : list)
                cardsContainer.getChildren().add(createCandidateCard(o));
            countLabel.setText(list.size() + " offre(s) disponible(s) actuellement");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private VBox createCandidateCard(Offre o) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: white; -fx-padding: 20; " +
                "-fx-background-radius: 10; -fx-pref-width: 300; " +
                "-fx-effect: dropshadow(three-pass-box,rgba(0,0,0,0.1),10,0,0,0);");
        card.setAlignment(Pos.TOP_LEFT);

        boolean isClosed = "Clotуrée".equalsIgnoreCase(o.getStatut()) ||
                (o.getDate_fin() != null && LocalDateTime.now().isAfter(o.getDate_fin()));

        Label badge = new Label(isClosed ? "Clôturée" : "Ouvert");
        badge.setStyle((isClosed ? "-fx-background-color: #95a5a6;"
                : "-fx-background-color: #27ae60;") +
                "-fx-text-fill: white; -fx-padding: 2 10; -fx-background-radius: 10;");

        Label title = new Label(o.getTitle() != null ? o.getTitle() : "Titre non disponible");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 16; -fx-text-fill: #1a3323;");
        title.setWrapText(true);

        Label loc = new Label("📍 " + (o.getLieu() != null ? o.getLieu() : "N/A"));
        Label sal = new Label(o.getSalaire() + " TND mensuel");
        sal.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");

        Button btnVoir = new Button("👁 Voir");
        btnVoir.setStyle("-fx-background-color: #1a3323; -fx-text-fill: white; " +
                "-fx-background-radius: 5; -fx-cursor: hand;");
        btnVoir.setMaxWidth(Double.MAX_VALUE);
        btnVoir.setOnAction(e -> {
            selectedOffreForView = o;
            navigateToDetail();
        });

        card.getChildren().addAll(badge, title, loc, sal, btnVoir);
        return card;
    }

    // ════════════════════════════════════════════════════════
    //  DETAIL
    // ════════════════════════════════════════════════════════
    private void setupDetailPage() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        detailTitle.setText(selectedOffreForView.getTitle());
        detailDesc.setText(selectedOffreForView.getDescription());
        detailLieu.setText(selectedOffreForView.getLieu());
        detailSalaire.setText(selectedOffreForView.getSalaire() + " TND mensuel");

        if (selectedOffreForView.getDate_debut() != null)
            detailDebut.setText(selectedOffreForView.getDate_debut().format(fmt));

        if (selectedOffreForView.getDate_fin() != null) {
            String df = selectedOffreForView.getDate_fin().format(fmt);
            detailFin.setText(df);
            dateLimitLabel.setText(df);
        }

        boolean expired = selectedOffreForView.getDate_fin() != null
                && LocalDateTime.now().isAfter(selectedOffreForView.getDate_fin());
        boolean closed = "Clôturée".equalsIgnoreCase(selectedOffreForView.getStatut());

        if (expired || closed) {
            btnPostuler.setDisable(true);
            btnPostuler.setText(closed ? "Offre Clôturée" : "Offre expirée");
            btnPostuler.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; " +
                    "-fx-background-radius: 10;");
        }
    }

    @FXML
    public void handlePostuler() {
        if (selectedOffreForView == null) return;
        try {
            DemandeService ds = new DemandeService();
            boolean exists = ds.afficher().stream()
                    .anyMatch(d -> d.getUsers_id() == 2
                            && d.getOffre_id() == selectedOffreForView.getId().intValue());
            if (exists) {
                new Alert(Alert.AlertType.INFORMATION, "Vous avez déjà postulé !").showAndWait();
            } else {
                // FIX SQL : On passe bien l'ID pour éviter la SQLIntegrityConstraintViolationException
                PostulerController.currentOffreId = selectedOffreForView.getId();
                navigateToView("/Views/Offres/PostulerForm.fxml");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ════════════════════════════════════════════════════════
    //  NAVIGATION & SEARCH
    // ════════════════════════════════════════════════════════
    @FXML public void showListPage() {
        selectedOffreForView = null;
        switchView("/Views/Offres/CandidatOffreList.fxml");
    }

    private void navigateToDetail() { switchView("/Views/Offres/OffreDetailView.fxml"); }

    private void navigateToView(String path) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(path));
            StackPane ca = (StackPane) btnPostuler.getScene().lookup("#contentArea");
            if (ca != null) ca.getChildren().setAll(root);
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML
    public void handleSearch() {
        String query = searchField.getText() == null ? "" : searchField.getText().toLowerCase().trim();
        String statut = statutFilter.getValue() == null ? "Toutes les offres" : statutFilter.getValue();
        try {
            List<Offre> all = service.afficher();
            cardsContainer.getChildren().clear();
            all.stream()
                    .filter(o -> (o.getTitle().toLowerCase().contains(query) || o.getLieu().toLowerCase().contains(query))
                            && (statut.equals("Toutes les offres") || o.getStatut().equalsIgnoreCase(statut)))
                    .forEach(o -> cardsContainer.getChildren().add(createCandidateCard(o)));
            countLabel.setText(cardsContainer.getChildren().size() + " offre(s) trouvée(s)");
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void switchView(String path) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(path));
            Scene scene = (cardsContainer != null && cardsContainer.getScene() != null) ? cardsContainer.getScene() : (detailTitle != null ? detailTitle.getScene() : null);
            if (scene != null) {
                StackPane ca = (StackPane) scene.lookup("#contentArea");
                if (ca != null) ca.getChildren().setAll(root);
                else scene.setRoot(root);
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    // ════════════════════════════════════════════════════════
    //  CHATBOT AVANCÉ (Analyse CV + Typing Effect)
    // ════════════════════════════════════════════════════════

    @FXML
    public void handleUploadCV() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir votre CV");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Documents", "*.pdf", "*.png", "*.jpg"));
        File file = fileChooser.showOpenDialog(chatContainer.getScene().getWindow());

        if (file != null) {
            addMessage("📁 CV sélectionné : " + file.getName(), true);
            // On envoie un prompt spécial à l'IA incluant le nom du fichier pour simuler l'analyse
            handleSendIA("Analyse mon CV nommé '" + file.getName() + "' et propose-moi les offres AgriSmart adaptées à mon profil.");
        }
    }

    @FXML
    public void toggleChat() {
        boolean visible = chatContainer.isVisible();
        chatContainer.setVisible(!visible);
        chatContainer.setManaged(!visible);
        if (!visible && isFirstOpen) {
            isFirstOpen = false;
            typeMessage("Bonjour ! Je suis AgriSmart Assistant.\nCliquez sur 📁 pour me montrer votre CV ou parlez-moi de vos compétences !", false);
        }
    }

    @FXML
    public void handleSendChat() {
        String userText = chatInputField.getText().trim();
        if (userText.isEmpty()) return;
        addMessage(userText, true);
        chatInputField.clear();
        handleSendIA(userText);
    }

    private void handleSendIA(String userPrompt) {
        conversationHistory.add(new String[]{"user", userPrompt});

        StringBuilder offresCtx = new StringBuilder();
        try {
            List<Offre> offres = service.afficher();
            for (Offre o : offres) {
                offresCtx.append("- ").append(o.getTitle()).append(" à ").append(o.getLieu()).append(". ");
            }
        } catch (Exception ignored) {}

        String systemPrompt = "Tu es AgriSmart Bot. Tu aides les candidats. Voici les offres : " + offresCtx +
                ". Si l'utilisateur envoie un CV, analyse ses chances et propose les postes correspondants.";

        StringBuilder messagesJson = new StringBuilder();
        messagesJson.append("{\"role\":\"system\",\"content\":\"").append(cleanForJson(systemPrompt)).append("\"}");
        for (String[] msg : conversationHistory) {
            messagesJson.append(",{\"role\":\"").append(msg[0]).append("\",\"content\":\"").append(cleanForJson(msg[1])).append("\"}");
        }

        String jsonBody = "{\"model\":\"mistral-small-latest\",\"temperature\":0.7,\"messages\":[" + messagesJson + "]}";

        addMessage("...", false);
        int loadingIdx = chatMessagesArea.getChildren().size() - 1;

        Thread thread = new Thread(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder().uri(URI.create(MISTRAL_URL))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + MISTRAL_KEY)
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody)).build();

                HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
                String raw = response.body();

                String reply = "Désolé, je rencontre un problème de connexion.";
                if (response.statusCode() == 200 && raw.contains("\"content\":")) {
                    int s = raw.lastIndexOf("\"content\":");
                    s = raw.indexOf("\"", s + 10) + 1;
                    int e = s;
                    while (e < raw.length()) {
                        if (raw.charAt(e) == '"' && raw.charAt(e - 1) != '\\') break;
                        e++;
                    }
                    reply = raw.substring(s, e).replace("\\n", "\n").replace("\\\"", "\"").replace("**", "");
                    conversationHistory.add(new String[]{"assistant", reply});
                }

                final String finalReply = reply;
                Platform.runLater(() -> {
                    if (loadingIdx < chatMessagesArea.getChildren().size()) chatMessagesArea.getChildren().remove(loadingIdx);
                    typeMessage(finalReply, false);
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    if (loadingIdx < chatMessagesArea.getChildren().size()) chatMessagesArea.getChildren().remove(loadingIdx);
                    addMessage("Erreur : " + ex.getMessage(), false);
                });
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private void typeMessage(String fullText, boolean isUser) {
        Label msgLabel = new Label("");
        msgLabel.setWrapText(true); msgLabel.setMaxWidth(220.0);
        msgLabel.setPadding(new Insets(7, 11, 7, 11));
        msgLabel.setStyle(isUser ? "-fx-background-color: #1a3323; -fx-text-fill: white; -fx-background-radius: 12;"
                : "-fx-background-color: #e8f5e9; -fx-text-fill: #1a3323; -fx-background-radius: 12;");
        HBox box = new HBox(msgLabel);
        box.setAlignment(isUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        chatMessagesArea.getChildren().add(box);

        String[] words = fullText.split(" ");
        Timeline timeline = new Timeline();
        for (int i = 0; i < words.length; i++) {
            final int index = i;
            KeyFrame kf = new KeyFrame(Duration.millis(i * 70), e -> {
                msgLabel.setText(msgLabel.getText() + (index == 0 ? "" : " ") + words[index]);
                chatScrollPane.setVvalue(1.0);
            });
            timeline.getKeyFrames().add(kf);
        }
        timeline.play();
    }

    private void addMessage(String text, boolean isUser) {
        Label msg = new Label(text);
        msg.setWrapText(true); msg.setMaxWidth(220.0);
        msg.setPadding(new Insets(7, 11, 7, 11));
        msg.setStyle(isUser ? "-fx-background-color: #1a3323; -fx-text-fill: white; -fx-background-radius: 12;"
                : "-fx-background-color: #e8f5e9; -fx-text-fill: #1a3323; -fx-background-radius: 12;");
        HBox box = new HBox(msg);
        box.setAlignment(isUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        chatMessagesArea.getChildren().add(box);
        chatScrollPane.setVvalue(1.0);
    }

    private String cleanForJson(String text) {
        return text.replace("\\", "").replace("\"", "'").replace("\n", "\\n").replace("\r", "");
    }
}