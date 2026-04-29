package controllers;

import entities.Demande;
import entities.Offre;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;
import services.ChatbotUpdateService;
import services.DemandeService;
import services.OffreService;
import utils.SessionManager;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class MesCandidaturesController implements Initializable {
    @FXML private VBox listContainer;
    @FXML private Label statAcceptee, statEnCours, statRefusee;

    // Éléments du Chatbot
    @FXML private VBox chatContainer;
    @FXML private VBox chatMessagesArea;
    @FXML private ScrollPane chatScrollPane;
    @FXML private TextField chatInputField;

    private final DemandeService demandeService = new DemandeService();
    private final OffreService offreService = new OffreService();
    private final ChatbotUpdateService updateService = new ChatbotUpdateService();

    // ✅ SUPPRIMÉ : private final int STATIC_USER_ID = 2;
    // ✅ On utilise SessionManager pour récupérer l'ID du vrai utilisateur connecté

    private Demande offreSelectionneePourChat = null;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Vérification de sécurité : si personne n'est connecté, on ne charge rien
        if (SessionManager.getInstance().getCurrentUser() == null) {
            System.err.println("⚠️ Aucun utilisateur connecté dans MesCandidaturesController !");
            return;
        }
        System.out.println("✅ Candidatures chargées pour : "
                + SessionManager.getInstance().getCurrentUser().getEmail()
                + " (ID: " + getCurrentUserId() + ")");
        refreshList();
    }

    // ✅ Méthode utilitaire — récupère l'ID de l'utilisateur connecté
    private int getCurrentUserId() {
        return SessionManager.getInstance().getCurrentUser().getId();
    }

    // --- LOGIQUE D'AFFICHAGE DE LA LISTE ---

    private void refreshList() {
        try {
            listContainer.getChildren().clear();
            List<Demande> allDemandes = demandeService.afficher();
            List<Offre> allOffres = offreService.afficher();

            // ✅ Filtre par l'ID de l'utilisateur connecté (pas 2 en dur)
            int userId = getCurrentUserId();
            List<Demande> userDemandes = allDemandes.stream()
                    .filter(d -> d.getUsers_id() == userId)
                    .collect(Collectors.toList());

            updateStats(userDemandes);

            if (userDemandes.isEmpty()) {
                Label vide = new Label("Vous n'avez pas encore postulé à une offre.");
                vide.setStyle("-fx-text-fill: #95a5a6; -fx-font-size: 14; -fx-padding: 30;");
                listContainer.getChildren().add(vide);
                return;
            }

            for (Demande d : userDemandes) {
                Offre o = allOffres.stream()
                        .filter(off -> String.valueOf(off.getId()).equals(String.valueOf(d.getOffre_id())))
                        .findFirst()
                        .orElse(null);
                listContainer.getChildren().add(createCandidatureRow(d, o));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private HBox createCandidatureRow(Demande d, Offre o) {
        HBox row = new HBox(20);
        row.setStyle("-fx-padding: 15; -fx-border-color: #eee; -fx-border-width: 0 0 1 0; " +
                "-fx-alignment: CENTER_LEFT; -fx-background-color: white;");

        VBox offerInfo = new VBox(5);
        offerInfo.setPrefWidth(250);
        String titreAffiche = (o != null) ? o.getTitle() : "Offre #" + d.getOffre_id();
        Label title = new Label(titreAffiche);
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: #1a3323;");
        Label detail = new Label((o != null ? o.getLieu() : "Lieu inconnu") + " • "
                + (o != null ? o.getSalaire() : "0") + " DT");
        detail.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11;");
        offerInfo.getChildren().addAll(title, detail);

        VBox dateInfo = new VBox(5);
        dateInfo.setPrefWidth(100);
        Label dateL = new Label(d.getDate_postulation().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        Label timeL = new Label(d.getDate_postulation().format(DateTimeFormatter.ofPattern("HH:mm")));
        timeL.setStyle("-fx-text-fill: #7f8c8d;");
        dateInfo.getChildren().addAll(dateL, timeL);

        HBox docs = new HBox(10);
        docs.setPrefWidth(140);
        Button btnCV = new Button("📄 CV");
        Button btnML = new Button("📄 Lettre");
        String basePath = "C:\\Users\\USER\\Documents\\Esprit\\PI JAVA\\agriSmartJava\\src\\main\\resources\\uploads\\";
        btnCV.setOnAction(e -> openFile(basePath + "cv\\" + d.getCv()));
        btnML.setOnAction(e -> openFile(basePath + "lettres\\" + d.getLettre_motivation()));
        docs.getChildren().addAll(btnCV, btnML);

        Label statusLabel = new Label(d.getStatut());
        statusLabel.setPrefWidth(90);
        String color = d.getStatut().equalsIgnoreCase("Acceptée") ? "#27ae60" :
                d.getStatut().equalsIgnoreCase("En cours") ? "#f39c12" : "#e74c3c";
        statusLabel.setStyle("-fx-background-color: " + color + "22; -fx-text-fill: " + color
                + "; -fx-padding: 5 10; -fx-background-radius: 15; -fx-font-weight: bold; -fx-alignment: center;");

        HBox actions = new HBox(8);
        Button viewBtn   = new Button("👁");
        Button editBtn   = new Button("✏");
        Button deleteBtn = new Button("🗑");
        deleteBtn.setStyle("-fx-text-fill: #e74c3c;");
        viewBtn.setOnAction(e   -> showOffreDetail(o));
        editBtn.setOnAction(e   -> handleEdit(d));
        deleteBtn.setOnAction(e -> handleDelete(d));
        actions.getChildren().addAll(viewBtn, editBtn, deleteBtn);

        row.getChildren().addAll(offerInfo, dateInfo, docs, statusLabel, actions);
        return row;
    }

    // --- LOGIQUE DU CHATBOT ---

    @FXML
    public void handleSendChat() {
        String userText = chatInputField.getText().trim();
        if (userText.isEmpty()) return;

        addSimpleMessage(userText, true);
        chatInputField.clear();
        String userTextLower = userText.toLowerCase();

        if (userTextLower.contains("bonjour") || userTextLower.contains("salut")
                || userTextLower.contains("aider")) {
            typeMessage("Bonjour ! 😊 Je peux vous aider à modifier vos candidatures. "
                    + "Tapez simplement le nom de l'offre concernée.", false);
            return;
        }

        try {
            int userId = getCurrentUserId(); // ✅ ID dynamique

            if (offreSelectionneePourChat == null) {
                List<Offre> offresPostulees = offreService.afficher();
                List<Demande> mesDemandes = demandeService.afficher().stream()
                        .filter(dem -> dem.getUsers_id() == userId) // ✅ ID dynamique
                        .collect(Collectors.toList());

                for (Demande d : mesDemandes) {
                    Offre offre = offresPostulees.stream()
                            .filter(off -> String.valueOf(off.getId()).equals(String.valueOf(d.getOffre_id())))
                            .findFirst().orElse(null);

                    if (offre != null) {
                        String titreOffre = offre.getTitle().toLowerCase();
                        boolean matchTrouve = false;
                        String[] motsUtilisateur = userTextLower.split(" ");
                        for (String mot : motsUtilisateur) {
                            if (mot.length() > 3 && titreOffre.contains(mot)) {
                                matchTrouve = true;
                                break;
                            }
                        }
                        if (matchTrouve || titreOffre.contains(userTextLower)) {
                            offreSelectionneePourChat = d;
                            typeMessage("✅ J'ai trouvé l'offre : '" + offre.getTitle()
                                    + "'.\nQue voulez-vous modifier ? (nom, prenom ou num)", false);
                            return;
                        }
                    }
                }
                typeMessage("❌ Désolé, je ne trouve pas cette offre. Pouvez-vous préciser le titre ?", false);

            } else {
                String response = updateService.processMessage(userText, offreSelectionneePourChat);
                if (response != null) {
                    typeMessage(response, false);
                    if (response.contains("✅")) {
                        demandeService.modifier(offreSelectionneePourChat);
                        refreshList();
                        offreSelectionneePourChat = null;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // --- AUTRES MÉTHODES ---

    private void updateStats(List<Demande> demandes) {
        statAcceptee.setText(String.valueOf(demandes.stream()
                .filter(d -> "Acceptée".equalsIgnoreCase(d.getStatut())).count()));
        statEnCours.setText(String.valueOf(demandes.stream()
                .filter(d -> "En cours".equalsIgnoreCase(d.getStatut())).count()));
        statRefusee.setText(String.valueOf(demandes.stream()
                .filter(d -> "Refusée".equalsIgnoreCase(d.getStatut())).count()));
    }

    private void handleEdit(Demande d) {
        PostulerController.selectedDemandeForEdit = d;
        PostulerController.currentOffreId = d.getOffre_id();
        switchView("/Views/Offres/PostulerForm.fxml");
    }

    private void handleDelete(Demande d) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer cette candidature ?");
        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                demandeService.supprimer(d.getId().intValue());
                refreshList();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void openFile(String path) {
        try {
            File f = new File(path);
            if (f.exists()) Desktop.getDesktop().open(f);
            else System.err.println("⚠️ Fichier introuvable : " + path);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showOffreDetail(Offre o) {
        if (o != null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/Views/Offres/OffreDetailView.fxml"));
                Parent root = loader.load();
                
                CandidatOffreController controller = loader.getController();
                controller.setData(o);
                
                StackPane area = (StackPane) listContainer.getScene().lookup("#contentArea");
                if (area != null) area.getChildren().setAll(root);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void toggleChat() {
        chatContainer.setVisible(!chatContainer.isVisible());
        chatContainer.setManaged(chatContainer.isVisible());
    }

    @FXML
    private void showOffres() {
        switchView("/Views/Offres/CandidatOffreList.fxml");
    }

    private void switchView(String path) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(path));
            StackPane area = (StackPane) listContainer.getScene().lookup("#contentArea");
            if (area != null) area.getChildren().setAll(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void typeMessage(String text, boolean isUser) {
        addSimpleMessage(text, isUser);
    }

    private void addSimpleMessage(String text, boolean isUser) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setMaxWidth(220);
        label.setStyle(isUser
                ? "-fx-background-color: #1a3323; -fx-text-fill: white; -fx-padding: 8; -fx-background-radius: 12;"
                : "-fx-background-color: #e8f5e9; -fx-text-fill: #1a3323; -fx-padding: 8; -fx-background-radius: 12;");
        HBox box = new HBox(label);
        box.setAlignment(isUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        chatMessagesArea.getChildren().add(box);
        chatScrollPane.setVvalue(1.0);
    }
}