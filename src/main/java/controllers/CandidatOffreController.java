package controllers;

import entities.Demande;
import entities.Offre;
import javafx.animation.*;
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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import services.DemandeService;
import services.OffreService;
import services.ChatbotUpdateService;
import services.RecrutementIAService;

import java.io.*;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class CandidatOffreController implements Initializable {

    // ── Offres ──────────────────────────────────────────────
    @FXML private FlowPane         cardsContainer;
    @FXML private TextField        searchField;
    @FXML private Label            countLabel;
    @FXML private ComboBox<String> statutFilter;

    // ── Detail ──────────────────────────────────────────────
    @FXML private Label  detailTitle, detailDesc, detailLieu,
            detailSalaire, detailDebut, detailFin, dateLimitLabel;
    @FXML private Button btnPostuler;

    // ── Chatbot ──────────────────────────────────────────────
    @FXML private VBox       chatContainer, chatMessagesArea;
    @FXML private ScrollPane chatScrollPane;
    @FXML private TextField  chatInputField;
    @FXML private Label      chatStatusLabel, chatStatusDot;
    @FXML private HBox       filePreviewBar, chipsBox;
    @FXML private Label      filePreviewName, filePreviewIcon;

    // ── Simulateur ───────────────────────────────────────────
    @FXML private Region     overlay;
    @FXML private VBox       simulatorModal, simulatorIntroPane, simulatorBilanPane, bilanContent;
    @FXML private Label      simulatorMainText, simulatorSubText, micDot;
    @FXML private Button     btnLancerEntrainement;
    @FXML private HBox       listeningStatus, micIndicator;
    @FXML private ScrollPane bilanScrollPane;

    // ══ ETAT CHATBOT ════════════════════════════════════════
    private boolean isFirstOpen   = true;
    private boolean isLoading     = false;
    private String  contenuCV     = "";
    private String  nomFichier    = "";
    private final List<String[]>  historique = new ArrayList<>();
    private Demande offreSelPourChat          = null;

    // ══ ETAT SIMULATEUR ══════════════════════════════════════
    private final List<String> reponsesUser = new ArrayList<>();
    private final List<String> questionsIA  = new ArrayList<>();
    private static final int   MAX_Q        = 7;
    private boolean  simActive   = false;
    private Thread   simThread   = null;
    private Timeline micTimeline = null;

    // ── Services ─────────────────────────────────────────────
    private final OffreService         offreService  = new OffreService();
    private final DemandeService       demandeService= new DemandeService();
    private final ChatbotUpdateService updateService = new ChatbotUpdateService();
    private final RecrutementIAService iaService     = new RecrutementIAService();

    public static Offre selectedOffreForView = null;

    // ════════════════════════════════════════════════════════
    //  INIT
    // ════════════════════════════════════════════════════════
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (statutFilter != null) {
            statutFilter.setItems(FXCollections.observableArrayList(
                    "Toutes les offres", "Ouvert", "Cloturee"));
            statutFilter.setValue("Toutes les offres");
            statutFilter.setOnAction(e -> handleSearch());
        }
        if (cardsContainer != null) loadData();
        if (detailTitle    != null && selectedOffreForView != null) setupDetail();
    }

    // ════════════════════════════════════════════════════════
    //  OFFRES
    // ════════════════════════════════════════════════════════
    private void loadData() {
        try {
            List<Offre> list = offreService.afficher();
            cardsContainer.getChildren().clear();
            list.forEach(o -> cardsContainer.getChildren().add(buildCard(o)));
            countLabel.setText(list.size() + " offre(s) disponible(s)");
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private VBox buildCard(Offre o) {
        VBox c = new VBox(10); c.setAlignment(Pos.TOP_LEFT);
        c.setStyle("-fx-background-color:white;-fx-padding:20;-fx-background-radius:12;" +
                "-fx-pref-width:290;-fx-effect:dropshadow(three-pass-box,rgba(0,0,0,0.08),10,0,0,2);");
        boolean closed = "Cloturee".equalsIgnoreCase(o.getStatut()) ||
                (o.getDate_fin() != null && LocalDateTime.now().isAfter(o.getDate_fin()));
        Label badge = new Label(closed ? "Cloturee" : "Ouvert");
        badge.setStyle((closed?"-fx-background-color:#95a5a6;":"-fx-background-color:#27ae60;") +
                "-fx-text-fill:white;-fx-padding:3 10;-fx-background-radius:10;-fx-font-size:11;");
        Label title = new Label(o.getTitle() != null ? o.getTitle() : "Sans titre");
        title.setStyle("-fx-font-weight:bold;-fx-font-size:15;-fx-text-fill:#1a3323;");
        title.setWrapText(true);
        Label loc = new Label("   " + (o.getLieu() != null ? o.getLieu() : "N/A"));
        loc.setStyle("-fx-font-size:12;-fx-text-fill:#666;");
        Label sal = new Label(o.getSalaire() + " TND / mois");
        sal.setStyle("-fx-text-fill:#27ae60;-fx-font-weight:bold;-fx-font-size:13;");
        Button btn = new Button("Voir l offre"); btn.setMaxWidth(Double.MAX_VALUE);
        btn.setStyle("-fx-background-color:#1a3323;-fx-text-fill:white;-fx-background-radius:8;-fx-cursor:hand;-fx-font-size:12;");
        btn.setOnAction(e -> { selectedOffreForView = o; navigateToDetail(); });
        c.getChildren().addAll(badge, title, loc, sal, btn);
        return c;
    }

    @FXML public void handleSearch() {
        String q = searchField != null && searchField.getText() != null ? searchField.getText().toLowerCase().trim() : "";
        String s = statutFilter != null && statutFilter.getValue() != null ? statutFilter.getValue() : "Toutes les offres";
        if (cardsContainer == null) return;
        try {
            List<Offre> all = offreService.afficher();
            cardsContainer.getChildren().clear();
            all.stream().filter(o -> {
                boolean mt = q.isEmpty() || (o.getTitle()!=null&&o.getTitle().toLowerCase().contains(q))
                        || (o.getLieu()!=null&&o.getLieu().toLowerCase().contains(q));
                boolean ms = s.equals("Toutes les offres") || o.getStatut().equalsIgnoreCase(s);
                return mt && ms;
            }).forEach(o -> cardsContainer.getChildren().add(buildCard(o)));
            if (countLabel != null) countLabel.setText(cardsContainer.getChildren().size() + " offre(s)");
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // ════════════════════════════════════════════════════════
    //  CHATBOT — TOGGLE
    // ════════════════════════════════════════════════════════
    @FXML
    public void toggleChat() {
        boolean v = chatContainer.isVisible();
        chatContainer.setVisible(!v);
        chatContainer.setManaged(!v);
        if (!v && isFirstOpen) {
            isFirstOpen = false;
            // ✅ Message d'accueil orienté CANDIDATURE uniquement
            dormirPuisBot(0, "👋 Bonjour ! Je suis votre assistant candidature AgriSmart.\n\n" +
                    "Je peux vous aider à :\n" +
                    "• Trouver l offre qui correspond à votre profil\n" +
                    "• Comprendre comment postuler étape par étape\n" +
                    "• Analyser votre CV et vous donner des conseils\n" +
                    "• Préparer votre lettre de motivation\n\n" +
                    "💡 Pour un entretien simulé, utilisez le bouton \"Lancer le test\" en haut de la page !");
        }
    }

    // ════════════════════════════════════════════════════════
    //  CHIPS ACTIONS RAPIDES
    // ════════════════════════════════════════════════════════
    @FXML public void handleChipOffres()   { voirOffres(); }
    @FXML public void handleChipPostuler() { expliquerPostuler(); }

    private void voirOffres() {
        if (isLoading) return;
        userMsg("Je veux voir les offres disponibles.");
        setLoading(true);
        new Thread(() -> {
            try {
                List<Offre> offres = offreService.afficher();
                long ouvertes = offres.stream().filter(o -> estOuverte(o)).count();
                StringBuilder sb = new StringBuilder();
                sb.append("Il y a actuellement ").append(ouvertes).append(" offre(s) ouvertes :\n\n");
                for (Offre o : offres) {
                    if (estOuverte(o)) {
                        sb.append("- ").append(o.getTitle())
                                .append(" a ").append(o.getLieu())
                                .append(" | ").append(o.getSalaire()).append(" TND\n");
                    }
                }
                if (!contenuCV.isEmpty()) {
                    sb.append("\nD apres votre CV, je peux vous dire laquelle vous convient le mieux. Voulez-vous ?");
                } else {
                    sb.append("\nImportez votre CV pour que je vous conseille sur laquelle postuler.");
                }
                String msg = sb.toString();
                Platform.runLater(() -> { setLoading(false); botMsg(msg); });
            } catch (Exception e) {
                Platform.runLater(() -> { setLoading(false); botMsg("Erreur acces aux offres."); });
            }
        }).start();
    }

    private void expliquerPostuler() {
        userMsg("Comment postuler a une offre ?");
        botMsg("Pour postuler chez AgriSmart :\n\n" +
                "1. Cliquez sur 'Voir l offre' sur la carte qui vous interesse\n" +
                "2. Lisez la description et verifiez que vous correspondez\n" +
                "3. Cliquez sur 'Postuler' en bas de la page\n" +
                "4. Remplissez le formulaire avec vos informations\n\n" +
                "Conseil : Si vous avez un doute sur quelle offre choisir, " +
                "importez votre CV et je vous dirai laquelle correspond le mieux a votre profil.");
    }

    // ════════════════════════════════════════════════════════
    //  CHATBOT — UPLOAD CV
    // ════════════════════════════════════════════════════════
    @FXML
    public void handleUploadCV() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choisir votre CV");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("CV (PDF)", "*.pdf"),
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg")
        );
        File file = fc.showOpenDialog(chatContainer.getScene().getWindow());
        if (file == null) return;

        nomFichier   = file.getName();
        contenuCV    = extrairePDF(file);

        filePreviewIcon.setText("CV");
        filePreviewName.setText(nomFichier);
        filePreviewBar.setVisible(true); filePreviewBar.setManaged(true);

        userMsg("Voici mon CV : " + nomFichier);

        String ext = nomFichier.toLowerCase();
        if (ext.endsWith(".png") || ext.endsWith(".jpg") || ext.endsWith(".jpeg")) {
            afficherImage(file);
        }

        analyserEtConseiller();
    }

    @FXML
    public void supprimerFichier() {
        nomFichier = ""; contenuCV = "";
        filePreviewBar.setVisible(false); filePreviewBar.setManaged(false);
        botMsg("CV supprime. Vous pouvez en importer un nouveau.");
    }

    // ════════════════════════════════════════════════════════
    //  ANALYSE CV + CONSEIL PERSONNALISE
    // ════════════════════════════════════════════════════════
    private void analyserEtConseiller() {
        setLoading(true);
        new Thread(() -> {
            try {
                List<Offre> offres = offreService.afficher();

                StringBuilder listeOffres = new StringBuilder();
                for (Offre o : offres) {
                    if (estOuverte(o)) {
                        listeOffres.append("- ").append(o.getTitle())
                                .append(" | ").append(o.getLieu())
                                .append(" | ").append(o.getSalaire()).append(" TND\n");
                    }
                }

                String prompt =
                        "Tu es un conseiller RH senior pour AgriSmart Tunisie.\n" +
                                "Analyse ce CV et reponds en 6 lignes maximum :\n" +
                                "1. Quel poste correspond le mieux a ce candidat (cite le nom exact de l offre)\n" +
                                "2. Pourquoi cette offre lui convient (1 phrase)\n" +
                                "3. Ce qui manque dans son profil pour cette offre (1 phrase)\n" +
                                "4. Un conseil concret pour ameliorer sa candidature (1 phrase)\n" +
                                "Tutoie le candidat. Sois direct, honnete, pas de bavardage.\n" +
                                "NE pose PAS de questions d entretien. Tu aides uniquement a postuler.\n\n" +
                                "OFFRES DISPONIBLES :\n" + listeOffres + "\n" +
                                "NOM DU CV : " + nomFichier + "\n" +
                                (contenuCV.isEmpty()
                                        ? "CONTENU CV : Non lisible. Analyse sur nom de fichier uniquement."
                                        : "CONTENU CV :\n" + contenuCV.substring(0, Math.min(contenuCV.length(), 2500)));

                String rep = iaService.obtenirConseilRecrutement(prompt, nomFichier, offres);
                if (rep == null || rep.isBlank() || rep.contains("specialise uniquement"))
                    rep = conseilSansIA(offres);

                historique.add(new String[]{"assistant", rep});

                final String finalRep = rep;
                Platform.runLater(() -> { setLoading(false); botMsg(finalRep); });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> { setLoading(false); botMsg("Impossible d analyser le CV. Reessayez."); });
            }
        }).start();
    }

    private String conseilSansIA(List<Offre> offres) {
        StringBuilder sb = new StringBuilder("Votre CV a ete recu.\n\nOffres ouvertes actuellement :\n");
        for (Offre o : offres) if (estOuverte(o)) sb.append("- ").append(o.getTitle()).append(" a ").append(o.getLieu()).append("\n");
        sb.append("\nCliquez sur 'Voir l offre' pour plus de details et postulez a celle qui vous correspond.");
        return sb.toString();
    }

    @FXML
    private void handleShowNews() {
        try {
            URL url = getClass().getResource("/Views/Offres/NewsView.fxml");

            if (url == null) {
                System.err.println("ERREUR : Le fichier NewsView.fxml est introuvable !");
                return;
            }

            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();

            javafx.scene.layout.StackPane area = (javafx.scene.layout.StackPane) searchField.getScene().lookup("#contentArea");

            if (area != null) {
                area.getChildren().setAll(root);
            } else {
                searchField.getScene().setRoot(root);
            }

        } catch (IOException e) {
            System.err.println("ERREUR IO : " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ════════════════════════════════════════════════════════
    //  CHATBOT — ENVOYER MESSAGE TEXTE
    // ════════════════════════════════════════════════════════
    @FXML
    public void handleSendChat() {
        if (chatInputField == null) return;
        String txt = chatInputField.getText().trim();
        if (txt.isEmpty() || isLoading) return;

        userMsg(txt);
        chatInputField.clear();
        historique.add(new String[]{"user", txt});

        String tl = txt.toLowerCase();

        // ✅ Filtre hors-sujet
        if (estHorsSujet(txt)) {
            botMsg("Je suis specialise uniquement dans l aide a la candidature AgriSmart. " +
                    "Posez-moi une question sur les offres, votre CV ou comment postuler.");
            return;
        }

        // ✅ Rediriger vers simulateur si entretien demande dans le chat
        if (tl.contains("entretien") || tl.contains("simulat") || tl.contains("test vocal")
                || tl.contains("question d entretien") || tl.contains("preparer entretien")) {
            botMsg("Pour pratiquer un entretien complet avec notre IA, utilisez le bouton \"Lancer le test\" en haut de la page.\n\n" +
                    "Ici je suis specialise pour vous aider a postuler : trouver une offre, analyser votre CV, " +
                    "rediger une lettre de motivation. Puis-je vous aider avec cela ?");
            return;
        }

        // Gestion des demandes de modification de candidature existante
        try {
            if (offreSelPourChat == null) {
                List<Demande> mes = demandeService.afficher().stream()
                        .filter(d -> d.getUsers_id() == 2).toList();
                for (Demande d : mes) {
                    for (Offre o : offreService.afficher()) {
                        if (o.getId().equals((long) d.getOffre_id())
                                && txt.toLowerCase().contains(o.getTitle().toLowerCase())) {
                            offreSelPourChat = d; break;
                        }
                    }
                }
            }
            if (offreSelPourChat != null) {
                String rep = updateService.processMessage(txt, offreSelPourChat);
                if (rep != null) {
                    botMsg(rep);
                    if (rep.contains("OK") || rep.contains("modifi")) {
                        demandeService.modifier(offreSelPourChat);
                        offreSelPourChat = null;
                    }
                    return;
                }
            }
        } catch (SQLException ex) { ex.printStackTrace(); }

        // Cas rapides sans IA
        if (tl.contains("offre") && (tl.contains("voir") || tl.contains("liste") || tl.contains("disponible"))) {
            voirOffres(); return;
        }
        if (tl.contains("postuler") || (tl.contains("comment") && tl.contains("candidat"))) {
            expliquerPostuler(); return;
        }
        if (tl.contains("lettre") || tl.contains("motivation")) {
            conseilLettreMotivation(); return;
        }
        if (tl.contains("cv") && (tl.contains("conseil") || tl.contains("ameliorer") || tl.contains("améliorer"))) {
            if (!contenuCV.isEmpty()) { analyserEtConseiller(); } else { botMsg("Importez votre CV en cliquant sur 'Analyser mon CV' pour que je puisse vous donner des conseils personnalises."); }
            return;
        }

        // Envoie a l IA avec contexte complet
        envoyerIA(txt);
    }

    // ════════════════════════════════════════════════════════
    //  CONSEIL LETTRE DE MOTIVATION
    // ════════════════════════════════════════════════════════
    private void conseilLettreMotivation() {
        userMsg("Comment rediger une lettre de motivation ?");
        if (isLoading) return;
        setLoading(true);
        new Thread(() -> {
            try {
                List<Offre> offres = offreService.afficher();
                String offreCtx = "";
                if (!contenuCV.isEmpty() && !offres.isEmpty()) {
                    offreCtx = " pour le poste de " + offres.stream().filter(this::estOuverte).findFirst().map(Offre::getTitle).orElse("ce poste");
                }
                String prompt =
                        "Tu es un conseiller RH AgriSmart Tunisie.\n" +
                                "Donne 4 conseils courts et concrets pour rediger une lettre de motivation" + offreCtx + " dans le secteur agricole.\n" +
                                "Format : conseils numerotes, 1 phrase chacun. Tutoie le candidat.\n" +
                                "NE redige PAS la lettre complete. NE pose PAS de questions d entretien.\n" +
                                (contenuCV.isEmpty() ? "" : "CV du candidat :\n" + contenuCV.substring(0, Math.min(contenuCV.length(), 800)));
                String rep = iaService.obtenirConseilRecrutement(prompt, nomFichier, offres);
                if (rep == null || rep.isBlank()) {
                    rep = "Pour une bonne lettre de motivation agricole :\n\n" +
                            "1. Commence par mentionner l offre exacte et comment tu l as trouvee\n" +
                            "2. Parle de ton experience concrete en agriculture (machines, cultures, elevage)\n" +
                            "3. Explique pourquoi tu veux rejoindre AgriSmart specifiquement\n" +
                            "4. Termine par une disponibilite claire pour un entretien\n\n" +
                            "Voulez-vous que je vous aide a choisir l offre la plus adaptee a votre profil ?";
                }
                final String finalRep = rep;
                Platform.runLater(() -> { setLoading(false); botMsg(finalRep); });
            } catch (Exception e) {
                Platform.runLater(() -> { setLoading(false); botMsg("Erreur. Reessayez."); });
            }
        }).start();
    }

    // ════════════════════════════════════════════════════════
    //  ENVOI IA — avec historique et contexte offres
    // ════════════════════════════════════════════════════════
    private void envoyerIA(String question) {
        setLoading(true);
        new Thread(() -> {
            try {
                List<Offre> offres = offreService.afficher();

                StringBuilder ctx = new StringBuilder("OFFRES AGRISMART EN BASE :\n");
                for (Offre o : offres) {
                    ctx.append(estOuverte(o) ? "[OUVERTE] " : "[CLOTUREE] ")
                            .append(o.getTitle()).append(" | ").append(o.getLieu())
                            .append(" | ").append(o.getSalaire()).append(" TND\n");
                }

                StringBuilder hist = new StringBuilder();
                int debut = Math.max(0, historique.size() - 6);
                for (int i = debut; i < historique.size(); i++) {
                    String[] h = historique.get(i);
                    hist.append(h[0].equals("user") ? "Candidat: " : "Conseiller: ").append(h[1]).append("\n");
                }

                // ✅ Prompt système orienté CANDIDATURE uniquement — pas d'entretien
                String systeme =
                        "Tu es un assistant candidature pour AgriSmart Tunisie.\n" +
                                "REGLES ABSOLUES :\n" +
                                "1. Tu aides UNIQUEMENT avec : trouver une offre adaptee, comprendre comment postuler, " +
                                "ameliorer un CV, rediger une lettre de motivation, comprendre les conditions d un poste\n" +
                                "2. Tu ne poses JAMAIS de questions d entretien dans ce chat\n" +
                                "3. Si le candidat demande un entretien ou une simulation : redis-lui d utiliser le bouton " +
                                "'Lancer le test' en haut de la page\n" +
                                "4. Si hors-sujet (sport, politique, etc.) : reponds 'Je suis specialise dans l aide a la candidature AgriSmart.'\n" +
                                "5. COURT : 3-5 lignes max par reponse\n" +
                                "6. HONNETE : si le profil ne correspond pas a une offre, dis-le clairement\n" +
                                "7. Tutoie le candidat, sois chaleureux et direct\n\n" +
                                ctx + "\n" +
                                (contenuCV.isEmpty() ? "" : "CV DU CANDIDAT :\n" +
                                        contenuCV.substring(0, Math.min(contenuCV.length(), 1800)) + "\n\n") +
                                (hist.length() > 0 ? "HISTORIQUE CONVERSATION :\n" + hist + "\n" : "") +
                                "QUESTION ACTUELLE : " + question;

                String rep = iaService.obtenirConseilRecrutement(systeme, nomFichier, offres);
                if (rep == null || rep.isBlank()) rep = "Je n ai pas pu generer une reponse. Pouvez-vous reformuler ?";

                historique.add(new String[]{"assistant", rep});
                final String finalRep = rep;
                Platform.runLater(() -> { setLoading(false); botMsg(finalRep); });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> { setLoading(false); botMsg("Erreur de connexion. Reessayez."); });
            }
        }).start();
    }

    // ════════════════════════════════════════════════════════
    //  AFFICHAGE MESSAGES
    // ════════════════════════════════════════════════════════
    private void userMsg(String texte) {
        Label lbl = new Label(texte);
        lbl.setWrapText(true); lbl.setMaxWidth(235);
        lbl.setPadding(new Insets(9, 13, 9, 13));
        lbl.setStyle("-fx-background-color:#1a4331;-fx-text-fill:white;" +
                "-fx-background-radius:18 18 4 18;-fx-font-size:12;");
        HBox box = new HBox(lbl); box.setAlignment(Pos.CENTER_RIGHT);
        box.setPadding(new Insets(1, 0, 1, 55));
        chatMessagesArea.getChildren().add(box);
        chatScrollPane.setVvalue(1.0);
    }

    private void botMsg(String texte) {
        HBox row = new HBox(8); row.setAlignment(Pos.TOP_LEFT);
        row.setPadding(new Insets(1, 55, 1, 0));

        StackPane av = new StackPane();
        av.setStyle("-fx-background-color:#1a4331;-fx-background-radius:50;-fx-min-width:28;-fx-min-height:28;-fx-max-width:28;-fx-max-height:28;");
        Label avLbl = new Label("AI"); avLbl.setStyle("-fx-text-fill:white;-fx-font-size:9;-fx-font-weight:bold;");
        av.getChildren().add(avLbl);

        VBox bubble = new VBox(3); bubble.setMaxWidth(242);
        bubble.setPadding(new Insets(9, 13, 9, 13));
        bubble.setStyle("-fx-background-color:white;-fx-background-radius:4 18 18 18;" +
                "-fx-effect:dropshadow(three-pass-box,rgba(0,0,0,0.06),4,0,0,2);");

        for (String ligne : texte.split("\n")) {
            Label l = new Label(ligne); l.setWrapText(true); l.setMaxWidth(222);
            boolean gras = ligne.startsWith("-") || ligne.matches("^[0-9]+\\..*");
            l.setStyle("-fx-font-size:12;-fx-text-fill:#1a3323;" + (gras ? "" : ""));
            bubble.getChildren().add(l);
        }
        row.getChildren().addAll(av, bubble);
        chatMessagesArea.getChildren().add(row);
        Platform.runLater(() -> chatScrollPane.setVvalue(1.0));
    }

    private void afficherImage(File file) {
        try {
            Image img = new Image(file.toURI().toString(), 180, 130, true, true);
            ImageView iv = new ImageView(img);
            iv.setFitWidth(180); iv.setFitHeight(130); iv.setPreserveRatio(true);
            Rectangle clip = new Rectangle(180, 130); clip.setArcWidth(12); clip.setArcHeight(12);
            iv.setClip(clip);
            HBox box = new HBox(iv); box.setAlignment(Pos.CENTER_RIGHT);
            box.setPadding(new Insets(2, 0, 2, 50));
            chatMessagesArea.getChildren().add(box);
            chatScrollPane.setVvalue(1.0);
        } catch (Exception e) { userMsg("Image : " + file.getName()); }
    }

    private void setLoading(boolean loading) {
        isLoading = loading;
        Platform.runLater(() -> {
            chatMessagesArea.getChildren().removeIf(n -> "loadingBox".equals(n.getId()));
            if (loading) {
                HBox row = new HBox(8); row.setId("loadingBox"); row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(1, 55, 1, 0));
                StackPane av = new StackPane();
                av.setStyle("-fx-background-color:#1a4331;-fx-background-radius:50;-fx-min-width:28;-fx-min-height:28;-fx-max-width:28;-fx-max-height:28;");
                Label avL = new Label("AI"); avL.setStyle("-fx-text-fill:white;-fx-font-size:9;-fx-font-weight:bold;");
                av.getChildren().add(avL);
                Label dots = new Label("...");
                dots.setStyle("-fx-background-color:white;-fx-text-fill:#1a4331;-fx-font-size:18;" +
                        "-fx-padding:8 14;-fx-background-radius:4 18 18 18;");
                Timeline tl = new Timeline(
                        new KeyFrame(Duration.ZERO,        e -> dots.setText(".")),
                        new KeyFrame(Duration.millis(350),  e -> dots.setText("..")),
                        new KeyFrame(Duration.millis(700),  e -> dots.setText("..."))
                );
                tl.setCycleCount(Timeline.INDEFINITE); tl.play();
                dots.setUserData(tl);
                row.getChildren().addAll(av, dots);
                chatMessagesArea.getChildren().add(row);
                chatScrollPane.setVvalue(1.0);
            }
        });
    }

    private void dormirPuisBot(long ms, String msg) {
        if (ms == 0) { botMsg(msg); return; }
        new Thread(() -> {
            try { Thread.sleep(ms); } catch (Exception ignored) {}
            Platform.runLater(() -> botMsg(msg));
        }).start();
    }

    // ════════════════════════════════════════════════════════
    //  UTILITAIRES
    // ════════════════════════════════════════════════════════
    private boolean estOuverte(Offre o) {
        return "Ouvert".equalsIgnoreCase(o.getStatut())
                && (o.getDate_fin() == null || LocalDateTime.now().isBefore(o.getDate_fin()));
    }

    private boolean estHorsSujet(String msg) {
        String m = msg.toLowerCase();
        // ✅ Mots-clés hors-sujet — entretien/simulation exclus car gérés séparément avec redirection
        String[] hors = {"football","sport","musique","film","cinema","recette","meteo","blague",
                "politique","guerre","jeux video","bitcoin","crypto","bourse","tu vas bien",
                "comment tu","qui est ton","quel age","chatgpt","programme",
                "entretien blanc","jeu de role","roleplay"};
        for (String h : hors) if (m.contains(h)) return true;
        return false;
    }

    private String extrairePDF(File pdf) {
        try {
            Class<?> doc  = Class.forName("org.apache.pdfbox.pdmodel.PDDocument");
            Class<?> strip= Class.forName("org.apache.pdfbox.text.PDFTextStripper");
            Object d = doc.getMethod("load", File.class).invoke(null, pdf);
            Object s = strip.getDeclaredConstructor().newInstance();
            String t = (String) strip.getMethod("getText", doc).invoke(s, d);
            doc.getMethod("close").invoke(d);
            return t != null ? t.trim() : "";
        } catch (Exception e) { return ""; }
    }

    // ════════════════════════════════════════════════════════
    //  SIMULATEUR VOCAL
    // ════════════════════════════════════════════════════════
    @FXML public void handleLancerTest() {
        if (overlay == null || simulatorModal == null) return;
        resetIntro();
        overlay.setVisible(true); simulatorModal.setVisible(true);
        simulatorModal.setScaleX(0.85); simulatorModal.setScaleY(0.85); simulatorModal.setOpacity(0);
        ScaleTransition st = new ScaleTransition(Duration.millis(200), simulatorModal); st.setToX(1); st.setToY(1);
        FadeTransition  ft = new FadeTransition(Duration.millis(200), simulatorModal);  ft.setToValue(1);
        new ParallelTransition(st, ft).play();
    }

    private void resetIntro() {
        stopSim();
        simulatorIntroPane.setVisible(true);  simulatorIntroPane.setManaged(true);
        simulatorBilanPane.setVisible(false); simulatorBilanPane.setManaged(false);
        simulatorMainText.setText("Simulateur d Entretien");
        simulatorMainText.setStyle("-fx-font-size:20;-fx-font-weight:bold;-fx-text-fill:#1a3323;-fx-padding:0 40 0 40;");
        simulatorSubText.setText("Entrainement general pour tous les metiers agricoles.");
        simulatorSubText.setVisible(true); simulatorSubText.setManaged(true);
        btnLancerEntrainement.setText("Lancer l entrainement");
        btnLancerEntrainement.setOnAction(e -> startSimulation());
        btnLancerEntrainement.setStyle("-fx-background-color:#1a7a43;-fx-text-fill:white;-fx-font-weight:bold;" +
                "-fx-font-size:14;-fx-background-radius:30;-fx-padding:12 40;-fx-cursor:hand;");
        btnLancerEntrainement.setVisible(true); btnLancerEntrainement.setManaged(true);
        listeningStatus.setVisible(false); listeningStatus.setManaged(false);
        micIndicator.setVisible(false);    micIndicator.setManaged(false);
        stopMic();
    }

    @FXML public void startSimulation() {
        reponsesUser.clear(); questionsIA.clear();
        simActive = true;
        simulatorSubText.setVisible(true); simulatorSubText.setManaged(true);
        btnLancerEntrainement.setVisible(false); btnLancerEntrainement.setManaged(false);
        listeningStatus.setVisible(true); listeningStatus.setManaged(true);
        micIndicator.setVisible(true); micIndicator.setManaged(true);
        simulatorMainText.setStyle("-fx-font-size:16;-fx-font-weight:bold;-fx-text-fill:#1a3323;-fx-padding:0 30 0 30;");
        String acc = "Bonjour ! Je suis votre recruteur AgriSmart. " +
                "Nous allons realiser un entretien de " + MAX_Q + " questions. Preparez-vous.";
        simulatorMainText.setText(acc);
        simulatorSubText.setText("Message d accueil...");
        simThread = new Thread(() -> { parler(acc); if (simActive) { sleep(800); poserQ(0); } });
        simThread.setDaemon(true); simThread.start();
    }

    private void poserQ(int step) {
        if (!simActive) return;
        String q = getQ(step); questionsIA.add(q);
        Platform.runLater(() -> {
            simulatorMainText.setText("Question " + (step+1) + " / " + MAX_Q + "\n\n" + q);
            simulatorSubText.setText("L IA lit la question..."); stopMic();
        });
        parler(q); if (!simActive) return;
        Platform.runLater(() -> simulatorSubText.setText("Reflechissez...")); sleep(2000); if (!simActive) return;
        Platform.runLater(() -> { simulatorSubText.setText("Parlez maintenant... (15s)"); startMic(); });
        String rep = capturer(15); if (!simActive) return;
        Platform.runLater(this::stopMic);
        if (rep != null && rep.trim().length() > 3) {
            reponsesUser.add(rep.trim()); final String r = rep.trim();
            Platform.runLater(() -> { simulatorMainText.setText("Q " + (step+1) + "\n\n" + q + "\n\nVotre reponse : " + r); simulatorSubText.setText("Reponse enregistree !"); });
        } else {
            reponsesUser.add("");
            Platform.runLater(() -> { simulatorMainText.setText("Q " + (step+1) + "\n\n" + q + "\n\nAucune reponse detectee."); simulatorSubText.setText("Passage a la suite..."); });
        }
        sleep(1500); if (!simActive) return;
        if (step + 1 >= MAX_Q) evaluer(); else poserQ(step + 1);
    }

    private void evaluer() {
        if (!simActive) return;
        Platform.runLater(() -> {
            stopMic(); listeningStatus.setVisible(false); micIndicator.setVisible(false);
            simulatorMainText.setText("Analyse en cours..."); simulatorSubText.setText("L IA evalue vos reponses...");
        });
        StringBuilder dial = new StringBuilder();
        for (int i = 0; i < questionsIA.size(); i++) {
            dial.append("Q").append(i+1).append(": ").append(questionsIA.get(i)).append("\n");
            String r = i < reponsesUser.size() ? reponsesUser.get(i) : "";
            dial.append("R: ").append(r.isEmpty() ? "(aucune)" : r).append("\n\n");
        }
        String prompt = "Expert RH AgriSmart Tunisie. Evalue cet entretien.\n" + dial +
                "Regles: aucune reponse=0-30, vagues=30-55, precises=60-100.\n" +
                "Format exact:\nSCORE: [0-100]\nNIVEAU: [Debutant/Intermediaire/Confirme/Expert]\n" +
                "POINTS_FORTS: [phrase]\nPOINTS_FAIBLES: [phrase]\nCONSEIL: [phrase]\nCONCLUSION: [phrase]";
        try {
            String ev = iaService.obtenirConseilRecrutement(prompt, nomFichier, new ArrayList<>());
            boolean ok = ev != null && ev.contains("SCORE:") && !ev.contains("indisponible");
            if (ok) {
                int s = pInt(ev, "SCORE:");
                Platform.runLater(() -> bilan(s, pStr(ev,"NIVEAU:"), pStr(ev,"POINTS_FORTS:"),
                        pStr(ev,"POINTS_FAIBLES:"), pStr(ev,"CONSEIL:"), pStr(ev,"CONCLUSION:"), true));
                parler("Votre score est de " + s + " sur cent. " + (s>=70?"Excellent !":s>=40?"Continuez.":"Pratiquez encore."));
            } else {
                int s = scoreLocal();
                Platform.runLater(() -> bilan(s, null, null, null, null, null, false));
                parler("Score estime " + s + " sur cent.");
            }
        } catch (Exception e) { int s=scoreLocal(); Platform.runLater(()->bilan(s,null,null,null,null,null,false)); }
    }

    private void bilan(int score, String niv, String forts, String faib, String conseil, String concl, boolean fromIA) {
        simActive = false;
        simulatorIntroPane.setVisible(false); simulatorIntroPane.setManaged(false);
        simulatorBilanPane.setVisible(true);  simulatorBilanPane.setManaged(true);
        bilanContent.getChildren().clear();

        String col = score>=85?"#1a7a43":score>=70?"#27ae60":score>=55?"#2980b9":score>=40?"#e67e22":"#e74c3c";
        String lab = score>=85?"Exceptionnel":score>=70?"Tres bon":score>=55?"Bon":score>=40?"Moyen":"A ameliorer";

        VBox hdr = new VBox(8); hdr.setAlignment(Pos.CENTER);
        hdr.setStyle("-fx-background-color:"+col+";-fx-background-radius:16;-fx-padding:20 30;");
        Label sl = new Label(score+" / 100"); sl.setStyle("-fx-font-size:34;-fx-font-weight:bold;-fx-text-fill:white;");
        Label ll = new Label(lab); ll.setStyle("-fx-font-size:14;-fx-font-weight:bold;-fx-text-fill:rgba(255,255,255,0.85);" +
                "-fx-background-color:rgba(0,0,0,0.15);-fx-background-radius:20;-fx-padding:3 14;");
        hdr.getChildren().addAll(sl, ll);
        if (niv!=null&&!niv.isBlank()){Label nl=new Label("Niveau : "+niv);nl.setStyle("-fx-font-size:12;-fx-text-fill:rgba(255,255,255,0.8);");hdr.getChildren().add(nl);}
        bilanContent.getChildren().add(hdr);

        StackPane bw = new StackPane(); bw.setMaxWidth(400); bw.setPrefWidth(400);
        Rectangle bg = new Rectangle(400,14); bg.setArcWidth(14); bg.setArcHeight(14); bg.setFill(Color.web("#ecf0f1"));
        Rectangle br = new Rectangle(Math.max(14,(score/100.0)*400),14); br.setArcWidth(14); br.setArcHeight(14); br.setFill(Color.web(col));
        StackPane.setAlignment(br,Pos.CENTER_LEFT); bw.getChildren().addAll(bg,br);
        bilanContent.getChildren().add(bw);

        if (fromIA) {
            if (forts!=null&&!forts.isBlank()) {
                HBox row=new HBox(10);row.setAlignment(Pos.CENTER);
                VBox cf=carte("Points forts",forts,"#e8f8f0","#1a7a43");cf.setPrefWidth(240);HBox.setHgrow(cf,Priority.ALWAYS);
                VBox cv=carte("A ameliorer",faib!=null?faib:"-","#fff8e1","#e67e22");cv.setPrefWidth(240);HBox.setHgrow(cv,Priority.ALWAYS);
                row.getChildren().addAll(cf,cv);bilanContent.getChildren().add(row);
            }
            if (conseil!=null&&!conseil.isBlank()){VBox cc=carte("Conseil",conseil,"#f0f4ff","#2980b9");cc.setMaxWidth(Double.MAX_VALUE);bilanContent.getChildren().add(cc);}
            if (concl!=null&&!concl.isBlank()){Label cl=new Label(concl);cl.setWrapText(true);cl.setStyle("-fx-font-size:13;-fx-text-fill:#555;-fx-font-style:italic;-fx-padding:5 20;");bilanContent.getChildren().add(cl);}
        } else {
            bilanContent.getChildren().add(carte("Info","Score calcule localement. Configurez l API Gemini pour une analyse precise.","#fff3f3","#e74c3c"));
        }

        Label tt = new Label("Synthese des reponses"); tt.setStyle("-fx-font-size:13;-fx-font-weight:bold;-fx-text-fill:#1a3323;");
        bilanContent.getChildren().add(tt);

        for (int i=0;i<questionsIA.size();i++) {
            String r = i<reponsesUser.size()?reponsesUser.get(i):"";
            boolean a = r!=null&&!r.isEmpty();
            VBox qc=new VBox(5);
            qc.setStyle("-fx-background-color:"+(a?"#f9f9f9":"#fff5f5")+";-fx-background-radius:10;-fx-padding:10 14;" +
                    "-fx-border-color:"+(a?"#e0e0e0":"#ffcccc")+";-fx-border-radius:10;-fx-border-width:1;");
            HBox qh=new HBox(8);qh.setAlignment(Pos.CENTER_LEFT);
            Label qn=new Label("Q"+(i+1));qn.setStyle("-fx-background-color:"+col+";-fx-text-fill:white;-fx-font-weight:bold;-fx-font-size:11;-fx-padding:2 8;-fx-background-radius:8;");
            Label qt=new Label(questionsIA.get(i));qt.setWrapText(true);qt.setMaxWidth(370);qt.setStyle("-fx-font-size:12;-fx-font-weight:bold;-fx-text-fill:#333;");
            HBox.setHgrow(qt,Priority.ALWAYS);qh.getChildren().addAll(qn,qt);
            Label rl=a?new Label("-> "+(r.length()>100?r.substring(0,100)+"...":r)):new Label("-> Aucune reponse");
            rl.setStyle(a?"-fx-font-size:11;-fx-text-fill:#555;-fx-font-style:italic;":"-fx-font-size:11;-fx-text-fill:#e74c3c;");
            rl.setWrapText(true);
            qc.getChildren().addAll(qh,rl);bilanContent.getChildren().add(qc);
        }
        Label fin=new Label("Merci pour votre participation ! Relancez pour progresser.");
        fin.setStyle("-fx-font-size:12;-fx-text-fill:#888;-fx-padding:5 20 0 20;");
        bilanContent.getChildren().add(fin);
        Platform.runLater(()->{if(bilanScrollPane!=null)bilanScrollPane.setVvalue(0);});
    }

    private VBox carte(String titre, String contenu, String bg, String color) {
        VBox v=new VBox(5);
        v.setStyle("-fx-background-color:"+bg+";-fx-background-radius:10;-fx-padding:10 14;" +
                "-fx-border-color:"+color+";-fx-border-radius:10;-fx-border-width:1.5;");
        Label t=new Label(titre);t.setStyle("-fx-font-size:11;-fx-font-weight:bold;-fx-text-fill:"+color+";");
        Label c=new Label(contenu);c.setWrapText(true);c.setStyle("-fx-font-size:12;-fx-text-fill:#333;");
        v.getChildren().addAll(t,c);return v;
    }

    private String getQ(int i) {
        String[] q={"Bonjour ! Presentez-vous et parlez de votre parcours en agriculture.",
                "Quelles sont vos competences agricoles principales ? Donnez des exemples.",
                "Pourquoi souhaitez-vous rejoindre AgriSmart ?",
                "Decrivez une situation difficile en exploitation agricole et comment vous l avez resolue.",
                "Comment gerez-vous les periodes de forte activite comme les recoltes ?",
                "Quelle est votre connaissance des technologies agricoles modernes ?",
                "Avez-vous des questions ? Quelle est votre disponibilite ?"};
        return q[Math.min(i,q.length-1)];
    }

    private void parler(String t) {
        try {
            String c=t.replace("'"," ").replace("\n"," ").replace("%"," pourcent")
                    .replaceAll("[^a-zA-Z0-9 !?,.;:àâäéèêëîïôùûüçÀÂÄÉÈÊËÎÏÔÙÛÜÇ-]","");
            new ProcessBuilder("powershell.exe","-NonInteractive","-Command",
                    "Add-Type -AssemblyName System.Speech;" +
                            "(New-Object System.Speech.Synthesis.SpeechSynthesizer).Speak('"+c+"')")
                    .redirectErrorStream(true).start().waitFor();
        } catch(Exception e){e.printStackTrace();}
    }

    private String capturer(int sec) {
        try {
            String script="Add-Type -AssemblyName System.Speech;" +
                    "$r=New-Object System.Speech.Recognition.SpeechRecognitionEngine;" +
                    "$r.SetInputToDefaultAudioDevice();" +
                    "$g=New-Object System.Speech.Recognition.DictationGrammar;$r.LoadGrammar($g);" +
                    "$res=$r.Recognize([System.TimeSpan]::FromSeconds("+sec+"));" +
                    "if($res){Write-Output $res.Text}else{Write-Output ''}";
            ProcessBuilder pb=new ProcessBuilder("powershell.exe","-NonInteractive","-Command",script);
            pb.redirectErrorStream(true);Process p=pb.start();
            BufferedReader br=new BufferedReader(new InputStreamReader(p.getInputStream()));
            StringBuilder out=new StringBuilder();String line;
            while((line=br.readLine())!=null)out.append(line).append(" ");
            p.waitFor();String res=out.toString().trim();
            return res.length()>2?res:null;
        } catch(Exception e){return null;}
    }

    private int scoreLocal() {
        if(reponsesUser.isEmpty())return 0;
        int tot=0,max=MAX_Q*14;
        String[] mots={"agriculture","sol","irrigation","recolte","experience","competence","ferme"};
        for(String r:reponsesUser){if(r==null||r.trim().isEmpty())continue;int len=r.trim().length();
            int pts=len>150?14:len>50?10:len>10?6:2;
            for(String m:mots)if(r.toLowerCase().contains(m)){pts=Math.min(pts+4,14);break;}tot+=pts;}
        return(int)Math.round((tot*100.0)/max);
    }

    @FXML public void closeSimulator() {
        stopSim();
        FadeTransition ft=new FadeTransition(Duration.millis(150),simulatorModal);ft.setToValue(0);
        ft.setOnFinished(e->{overlay.setVisible(false);simulatorModal.setVisible(false);simulatorModal.setOpacity(1);resetIntro();});
        ft.play();
    }

    private void stopSim(){simActive=false;if(simThread!=null){simThread.interrupt();simThread=null;}stopMic();}
    private void startMic(){if(micDot==null)return;stopMic();micTimeline=new Timeline(new KeyFrame(Duration.ZERO,e->micDot.setStyle("-fx-font-size:14;-fx-text-fill:#e74c3c;")),new KeyFrame(Duration.millis(500),e->micDot.setStyle("-fx-font-size:14;-fx-text-fill:transparent;")));micTimeline.setCycleCount(Timeline.INDEFINITE);micTimeline.play();}
    private void stopMic(){if(micTimeline!=null){micTimeline.stop();micTimeline=null;}if(micDot!=null)micDot.setStyle("-fx-font-size:14;-fx-text-fill:#e74c3c;");}
    private void sleep(long ms){try{Thread.sleep(ms);}catch(InterruptedException e){Thread.currentThread().interrupt();}}
    private int    pInt(String t,String c){for(String l:t.split("\n"))if(l.trim().startsWith(c)){String v=l.replace(c,"").trim().replaceAll("[^0-9]","");if(!v.isEmpty())return Math.max(0,Math.min(100,Integer.parseInt(v)));}return scoreLocal();}
    private String pStr(String t,String c){for(String l:t.split("\n"))if(l.trim().startsWith(c))return l.replace(c,"").trim();return "";}

    // ════════════════════════════════════════════════════════
    //  NAVIGATION
    // ════════════════════════════════════════════════════════
    private void setupDetail() {
        DateTimeFormatter fmt=DateTimeFormatter.ofPattern("dd/MM/yyyy");
        detailTitle.setText(selectedOffreForView.getTitle());
        detailDesc.setText(selectedOffreForView.getDescription());
        detailLieu.setText(selectedOffreForView.getLieu());
        detailSalaire.setText(selectedOffreForView.getSalaire()+" TND mensuel");
        if(selectedOffreForView.getDate_debut()!=null)detailDebut.setText(selectedOffreForView.getDate_debut().format(fmt));
        if(selectedOffreForView.getDate_fin()!=null){String df=selectedOffreForView.getDate_fin().format(fmt);detailFin.setText(df);if(dateLimitLabel!=null)dateLimitLabel.setText(df);}
        boolean exp=selectedOffreForView.getDate_fin()!=null&&LocalDateTime.now().isAfter(selectedOffreForView.getDate_fin());
        boolean cl="Cloturee".equalsIgnoreCase(selectedOffreForView.getStatut());
        if(exp||cl){btnPostuler.setDisable(true);btnPostuler.setText(cl?"Offre Cloturee":"Offre expiree");btnPostuler.setStyle("-fx-background-color:#95a5a6;-fx-text-fill:white;-fx-background-radius:10;");}
    }

    @FXML public void handlePostuler() {
        if(selectedOffreForView==null)return;
        try{boolean ex=demandeService.afficher().stream().anyMatch(d->d.getUsers_id()==2&&d.getOffre_id()==selectedOffreForView.getId().intValue());
            if(ex)new Alert(Alert.AlertType.INFORMATION,"Vous avez deja postule !").showAndWait();
            else{PostulerController.currentOffreId=selectedOffreForView.getId();navTo("/Views/Offres/PostulerForm.fxml");}
        }catch(SQLException e){e.printStackTrace();}
    }

    @FXML public void showListPage(){selectedOffreForView=null;switchView("/Views/Offres/CandidatOffreList.fxml");}
    private void navigateToDetail(){switchView("/Views/Offres/OffreDetailView.fxml");}
    private void navTo(String path){try{Parent r=FXMLLoader.load(getClass().getResource(path));if(btnPostuler!=null&&btnPostuler.getScene()!=null){StackPane ca=(StackPane)btnPostuler.getScene().lookup("#contentArea");if(ca!=null)ca.getChildren().setAll(r);}}catch(IOException e){e.printStackTrace();}}
    private void switchView(String path){try{Parent r=FXMLLoader.load(getClass().getResource(path));Scene sc=cardsContainer!=null&&cardsContainer.getScene()!=null?cardsContainer.getScene():detailTitle!=null?detailTitle.getScene():null;if(sc!=null){StackPane ca=(StackPane)sc.lookup("#contentArea");if(ca!=null)ca.getChildren().setAll(r);else sc.setRoot(r);}}catch(IOException e){e.printStackTrace();}}
}