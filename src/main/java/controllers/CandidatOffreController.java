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

    // ── Détail ──────────────────────────────────────────────
    @FXML private Label  detailTitle, detailDesc, detailLieu,
            detailSalaire, detailDebut, detailFin, dateLimitLabel;
    @FXML private Button btnPostuler;

    // ── Chatbot ─────────────────────────────────────────────
    @FXML private VBox       chatContainer;
    @FXML private VBox       chatMessagesArea;
    @FXML private ScrollPane chatScrollPane;
    @FXML private TextField  chatInputField;

    // ── Simulateur Modal ─────────────────────────────────────
    @FXML private Region     overlay;
    @FXML private VBox       simulatorModal;

    // Pane INTRO
    @FXML private VBox   simulatorIntroPane;
    @FXML private Label  simulatorMainText;
    @FXML private Label  simulatorSubText;
    @FXML private Button btnLancerEntrainement;
    @FXML private HBox   listeningStatus;
    @FXML private HBox   micIndicator;
    @FXML private Label  micDot;

    // Pane BILAN
    @FXML private VBox       simulatorBilanPane;
    @FXML private VBox       bilanContent;       // ← on construit le design ici
    @FXML private ScrollPane bilanScrollPane;

    // ══════════════════════════════════════════════════════════
    //  ÉTAT SIMULATEUR
    // ══════════════════════════════════════════════════════════
    private final List<String> reponsesUtilisateur = new ArrayList<>();
    private final List<String> questionsIA         = new ArrayList<>();
    private int     questionStep       = 0;
    private static final int MAX_QUESTIONS = 7;
    private boolean isSimulationActive = false;
    private Thread  ecouteThread       = null;
    private Timeline micBlinkTimeline  = null;

    // ── Chatbot ──────────────────────────────────────────────
    private boolean isFirstOpen               = true;
    private Demande offreSelectionneePourChat = null;
    private String  currentCVName             = "";

    // ── Services ─────────────────────────────────────────────
    private final OffreService         service              = new OffreService();
    private final DemandeService       demandeService       = new DemandeService();
    private final ChatbotUpdateService updateService        = new ChatbotUpdateService();
    private final RecrutementIAService recrutementIAService = new RecrutementIAService();

    public static Offre selectedOffreForView = null;

    // ════════════════════════════════════════════════════════
    //  INIT
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
    //  OFFRES
    // ════════════════════════════════════════════════════════
    private void loadCandidateData() {
        try {
            List<Offre> list = service.afficher();
            cardsContainer.getChildren().clear();
            for (Offre o : list) cardsContainer.getChildren().add(createCandidateCard(o));
            countLabel.setText(list.size() + " offre(s) disponible(s) actuellement");
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private VBox createCandidateCard(Offre o) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color:white;-fx-padding:20;-fx-background-radius:10;" +
                "-fx-pref-width:300;-fx-effect:dropshadow(three-pass-box,rgba(0,0,0,0.1),10,0,0,0);");
        card.setAlignment(Pos.TOP_LEFT);
        boolean isClosed = "Clôturée".equalsIgnoreCase(o.getStatut()) ||
                (o.getDate_fin() != null && LocalDateTime.now().isAfter(o.getDate_fin()));
        Label badge = new Label(isClosed ? "Clôturée" : "Ouvert");
        badge.setStyle((isClosed ? "-fx-background-color:#95a5a6;" : "-fx-background-color:#27ae60;") +
                "-fx-text-fill:white;-fx-padding:2 10;-fx-background-radius:10;");
        Label title = new Label(o.getTitle() != null ? o.getTitle() : "Titre non disponible");
        title.setStyle("-fx-font-weight:bold;-fx-font-size:16;-fx-text-fill:#1a3323;");
        title.setWrapText(true);
        Label loc = new Label("📍 " + (o.getLieu() != null ? o.getLieu() : "N/A"));
        Label sal = new Label(o.getSalaire() + " TND mensuel");
        sal.setStyle("-fx-text-fill:#27ae60;-fx-font-weight:bold;");
        Button btnVoir = new Button("👁 Voir");
        btnVoir.setStyle("-fx-background-color:#1a3323;-fx-text-fill:white;-fx-background-radius:5;-fx-cursor:hand;");
        btnVoir.setMaxWidth(Double.MAX_VALUE);
        btnVoir.setOnAction(e -> { selectedOffreForView = o; navigateToDetail(); });
        card.getChildren().addAll(badge, title, loc, sal, btnVoir);
        return card;
    }

    // ════════════════════════════════════════════════════════
    //  SIMULATEUR — ÉTAPE 1 : ouvrir modal
    // ════════════════════════════════════════════════════════
    @FXML
    public void handleLancerTest() {
        if (overlay == null || simulatorModal == null) return;
        resetModalToIntro();
        overlay.setVisible(true);
        simulatorModal.setVisible(true);
        simulatorModal.setScaleX(0.85); simulatorModal.setScaleY(0.85); simulatorModal.setOpacity(0);
        ScaleTransition st = new ScaleTransition(Duration.millis(200), simulatorModal);
        st.setToX(1); st.setToY(1);
        FadeTransition ft = new FadeTransition(Duration.millis(200), simulatorModal);
        ft.setToValue(1);
        new ParallelTransition(st, ft).play();
    }

    private void resetModalToIntro() {
        stopTout();
        // Montre le pane intro, cache le bilan
        simulatorIntroPane.setVisible(true);  simulatorIntroPane.setManaged(true);
        simulatorBilanPane.setVisible(false); simulatorBilanPane.setManaged(false);

        simulatorMainText.setText("Simulateur d'Entretien");
        simulatorMainText.setStyle("-fx-font-size:20;-fx-font-weight:bold;-fx-text-fill:#1a3323;-fx-padding:0 40 0 40;");
        simulatorSubText.setText("Entraînement général pour tous les métiers agricoles.");
        simulatorSubText.setVisible(true); simulatorSubText.setManaged(true);

        btnLancerEntrainement.setText("Lancer l'entraînement");
        btnLancerEntrainement.setOnAction(e -> startSimulation());
        btnLancerEntrainement.setStyle("-fx-background-color:#1a7a43;-fx-text-fill:white;" +
                "-fx-font-weight:bold;-fx-font-size:14;-fx-background-radius:30;-fx-padding:12 40;-fx-cursor:hand;");
        btnLancerEntrainement.setVisible(true); btnLancerEntrainement.setManaged(true);

        listeningStatus.setVisible(false); listeningStatus.setManaged(false);
        micIndicator.setVisible(false);    micIndicator.setManaged(false);
        stopMicAnimation();
    }

    // ════════════════════════════════════════════════════════
    //  SIMULATEUR — ÉTAPE 2 : démarrer
    // ════════════════════════════════════════════════════════
    @FXML
    public void startSimulation() {
        reponsesUtilisateur.clear();
        questionsIA.clear();
        questionStep       = 0;
        isSimulationActive = true;

        // Passe en mode entretien
        simulatorSubText.setVisible(true);  simulatorSubText.setManaged(true);
        btnLancerEntrainement.setVisible(false); btnLancerEntrainement.setManaged(false);
        listeningStatus.setVisible(true);   listeningStatus.setManaged(true);
        micIndicator.setVisible(true);      micIndicator.setManaged(true);
        simulatorMainText.setStyle("-fx-font-size:16;-fx-font-weight:bold;-fx-text-fill:#1a3323;-fx-padding:0 30 0 30;");

        String accueil = "Bonjour ! Je suis votre recruteur AgriSmart. " +
                "Nous allons réaliser un entretien de " + MAX_QUESTIONS + " questions. " +
                "L'entretien commence dans quelques instants.";
        simulatorMainText.setText("🌱 " + accueil);
        simulatorSubText.setText("🔊 Message d'accueil...");

        ecouteThread = new Thread(() -> {
            parlerBloquant(accueil);
            if (!isSimulationActive) return;
            dormirMs(800);
            poserQuestion(0);
        });
        ecouteThread.setDaemon(true);
        ecouteThread.start();
    }

    // ════════════════════════════════════════════════════════
    //  SIMULATEUR — POSER UNE QUESTION (séquentiel)
    // ════════════════════════════════════════════════════════
    private void poserQuestion(int step) {
        if (!isSimulationActive) return;

        final String question = getQuestion(step);
        questionsIA.add(question);

        Platform.runLater(() -> {
            simulatorMainText.setText("📢 Question " + (step + 1) + " / " + MAX_QUESTIONS + "\n\n" + question);
            simulatorSubText.setText("🔊 L'IA lit la question...");
            stopMicAnimation();
        });

        parlerBloquant(question);
        if (!isSimulationActive) return;

        Platform.runLater(() -> simulatorSubText.setText("💭 Réfléchissez..."));
        dormirMs(2000);
        if (!isSimulationActive) return;

        Platform.runLater(() -> {
            simulatorSubText.setText("🎤 Parlez maintenant... (15 s)");
            startMicAnimation();
        });

        String reponse = capturerVoix(15);
        if (!isSimulationActive) return;
        Platform.runLater(this::stopMicAnimation);

        if (reponse != null && reponse.trim().length() > 3) {
            reponsesUtilisateur.add(reponse.trim());
            final String rep = reponse.trim();
            Platform.runLater(() -> {
                simulatorMainText.setText(
                        "📢 Question " + (step + 1) + " / " + MAX_QUESTIONS + "\n\n" + question +
                                "\n\n🗣️ Votre réponse :\n\"" + rep + "\"");
                simulatorSubText.setText("✅ Réponse enregistrée !");
            });
        } else {
            reponsesUtilisateur.add("");
            Platform.runLater(() -> {
                simulatorMainText.setText(
                        "📢 Question " + (step + 1) + " / " + MAX_QUESTIONS + "\n\n" + question +
                                "\n\n⚠️ Aucune réponse détectée.");
                simulatorSubText.setText("⏭️ Passage à la suite...");
            });
        }

        dormirMs(1500);
        if (!isSimulationActive) return;

        int next = step + 1;
        if (next >= MAX_QUESTIONS) {
            evaluerAvecGemini();
        } else {
            poserQuestion(next);
        }
    }

    // ════════════════════════════════════════════════════════
    //  ÉVALUATION PAR GEMINI
    // ════════════════════════════════════════════════════════
    private void evaluerAvecGemini() {
        if (!isSimulationActive) return;

        Platform.runLater(() -> {
            stopMicAnimation();
            listeningStatus.setVisible(false);
            micIndicator.setVisible(false);
            simulatorMainText.setText("📊 Analyse de votre entretien...");
            simulatorSubText.setText("🤖 L'IA évalue vos réponses, veuillez patienter...");
        });

        StringBuilder dialogue = new StringBuilder();
        for (int i = 0; i < questionsIA.size(); i++) {
            dialogue.append("QUESTION ").append(i + 1).append(": ").append(questionsIA.get(i)).append("\n");
            String rep = i < reponsesUtilisateur.size() ? reponsesUtilisateur.get(i) : "";
            dialogue.append("RÉPONSE: ").append(rep.isEmpty() ? "(aucune réponse)" : rep).append("\n\n");
        }

        String promptEval =
                "Tu es un expert RH spécialisé en recrutement agricole en Tunisie pour AgriSmart.\n" +
                        "Voici la transcription d'un entretien :\n\n" + dialogue.toString() +
                        "\nRègle de notation :\n" +
                        "- Aucune réponse → score bas (0-30)\n" +
                        "- Réponses vagues ou très courtes → score moyen (30-55)\n" +
                        "- Réponses précises et structurées → score élevé (60-100)\n" +
                        "Réponds UNIQUEMENT dans ce format exact :\n" +
                        "SCORE: [0-100]\n" +
                        "NIVEAU: [Débutant / Intermédiaire / Confirmé / Expert]\n" +
                        "POINTS_FORTS: [courte phrase]\n" +
                        "POINTS_FAIBLES: [courte phrase]\n" +
                        "CONSEIL: [une phrase actionnable]\n" +
                        "CONCLUSION: [une phrase motivante]";

        try {
            String evaluation = recrutementIAService.obtenirConseilRecrutement(
                    promptEval, currentCVName, new ArrayList<>());

            System.out.println("📥 Éval Gemini:\n" + evaluation);

            boolean apiOk = evaluation != null && evaluation.contains("SCORE:")
                    && !evaluation.contains("temporairement indisponible")
                    && !evaluation.contains("difficulté technique");

            if (apiOk) {
                int    score    = parseChamp(evaluation, "SCORE:");
                String niveau   = parseTexte(evaluation, "NIVEAU:");
                String forts    = parseTexte(evaluation, "POINTS_FORTS:");
                String faibles  = parseTexte(evaluation, "POINTS_FAIBLES:");
                String conseil  = parseTexte(evaluation, "CONSEIL:");
                String concl    = parseTexte(evaluation, "CONCLUSION:");
                final int s = score;
                Platform.runLater(() -> afficherBilanGraphique(s, niveau, forts, faibles, conseil, concl, true));
                parlerBloquant("Votre score est de " + score + " sur cent. " +
                        (score >= 70 ? "Excellent résultat !" : score >= 40 ? "Continuez à pratiquer !" : "Ne vous découragez pas."));
            } else {
                int score = calculerScoreLocal();
                Platform.runLater(() -> afficherBilanGraphique(score, null, null, null, null, null, false));
                parlerBloquant("Votre score estimé est de " + score + " sur cent.");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            int score = calculerScoreLocal();
            Platform.runLater(() -> afficherBilanGraphique(score, null, null, null, null, null, false));
        }
    }

    // ════════════════════════════════════════════════════════
    //  BILAN GRAPHIQUE — construit dynamiquement avec JavaFX
    // ════════════════════════════════════════════════════════
    private void afficherBilanGraphique(int score, String niveau,
                                        String forts, String faibles,
                                        String conseil, String conclusion,
                                        boolean fromGemini) {
        isSimulationActive = false;

        // Cache le pane intro, montre le bilan
        simulatorIntroPane.setVisible(false); simulatorIntroPane.setManaged(false);
        simulatorBilanPane.setVisible(true);  simulatorBilanPane.setManaged(true);

        bilanContent.getChildren().clear();

        // ── Couleurs selon le score ──────────────────────────
        String couleurPrincipale;
        String emoji;
        String medailleTexte;
        if      (score >= 85) { couleurPrincipale = "#1a7a43"; emoji = "🏆"; medailleTexte = "EXCEPTIONNEL"; }
        else if (score >= 70) { couleurPrincipale = "#27ae60"; emoji = "🌟"; medailleTexte = "TRÈS BON"; }
        else if (score >= 55) { couleurPrincipale = "#2980b9"; emoji = "👍"; medailleTexte = "BON"; }
        else if (score >= 40) { couleurPrincipale = "#e67e22"; emoji = "📈"; medailleTexte = "MOYEN"; }
        else                  { couleurPrincipale = "#e74c3c"; emoji = "💪"; medailleTexte = "À AMÉLIORER"; }

        // ════ 1. EN-TÊTE SCORE ══════════════════════════════
        VBox headerCard = new VBox(8);
        headerCard.setAlignment(Pos.CENTER);
        headerCard.setStyle("-fx-background-color: " + couleurPrincipale + ";" +
                "-fx-background-radius: 16; -fx-padding: 20 30 20 30;");

        Label emojiLbl = new Label(emoji);
        emojiLbl.setStyle("-fx-font-size: 38;");

        Label scoreLbl = new Label(score + " / 100");
        scoreLbl.setStyle("-fx-font-size: 32; -fx-font-weight: bold; -fx-text-fill: white;");

        Label medailleLbl = new Label(medailleTexte);
        medailleLbl.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: rgba(255,255,255,0.85);" +
                "-fx-background-color: rgba(0,0,0,0.15); -fx-background-radius: 20; -fx-padding: 3 14;");

        if (niveau != null && !niveau.isBlank()) {
            Label niveauLbl = new Label("Niveau : " + niveau);
            niveauLbl.setStyle("-fx-font-size: 12; -fx-text-fill: rgba(255,255,255,0.8);");
            headerCard.getChildren().addAll(emojiLbl, scoreLbl, medailleLbl, niveauLbl);
        } else {
            headerCard.getChildren().addAll(emojiLbl, scoreLbl, medailleLbl);
        }
        bilanContent.getChildren().add(headerCard);

        // ════ 2. BARRE DE PROGRESSION ═══════════════════════
        VBox barreSection = new VBox(6);
        barreSection.setAlignment(Pos.CENTER);

        Label barreTitre = new Label("Progression");
        barreTitre.setStyle("-fx-font-size: 11; -fx-text-fill: #95a5a6; -fx-font-weight: bold;");

        // Fond gris
        StackPane barreWrapper = new StackPane();
        barreWrapper.setMaxWidth(400); barreWrapper.setPrefWidth(400);

        Rectangle barFond = new Rectangle(400, 14);
        barFond.setArcWidth(14); barFond.setArcHeight(14);
        barFond.setFill(Color.web("#ecf0f1"));

        // Barre colorée selon score
        double largeurRemplie = Math.max(14, (score / 100.0) * 400);
        Rectangle barRemplie = new Rectangle(largeurRemplie, 14);
        barRemplie.setArcWidth(14); barRemplie.setArcHeight(14);
        barRemplie.setFill(Color.web(couleurPrincipale));

        StackPane.setAlignment(barRemplie, Pos.CENTER_LEFT);
        barreWrapper.getChildren().addAll(barFond, barRemplie);

        barreSection.getChildren().addAll(barreTitre, barreWrapper);
        bilanContent.getChildren().add(barreSection);

        // ════ 3. CARTES POINTS FORTS / FAIBLES ═════════════
        if (fromGemini && forts != null && !forts.isBlank()) {
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER);

            // Points forts
            VBox carteForte = creerCarteInfo("✅ Points forts", forts, "#e8f8f0", "#1a7a43");
            carteForte.setPrefWidth(240);
            HBox.setHgrow(carteForte, Priority.ALWAYS);

            // Points faibles
            VBox carteFaible = creerCarteInfo("⚠️ À améliorer", faibles != null ? faibles : "—", "#fff8e1", "#e67e22");
            carteFaible.setPrefWidth(240);
            HBox.setHgrow(carteFaible, Priority.ALWAYS);

            row.getChildren().addAll(carteForte, carteFaible);
            bilanContent.getChildren().add(row);
        }

        // ════ 4. CONSEIL ════════════════════════════════════
        if (fromGemini && conseil != null && !conseil.isBlank()) {
            VBox conseilCard = creerCarteInfo("💡 Conseil personnalisé", conseil, "#f0f4ff", "#2980b9");
            conseilCard.setMaxWidth(Double.MAX_VALUE);
            bilanContent.getChildren().add(conseilCard);
        }

        // ════ 5. CONCLUSION ═════════════════════════════════
        if (fromGemini && conclusion != null && !conclusion.isBlank()) {
            Label concLbl = new Label("💬  " + conclusion);
            concLbl.setWrapText(true);
            concLbl.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
            concLbl.setStyle("-fx-font-size: 13; -fx-text-fill: #555; -fx-font-style: italic;" +
                    "-fx-padding: 5 20 5 20;");
            bilanContent.getChildren().add(concLbl);
        } else if (!fromGemini) {
            // Message si API indisponible
            VBox avisCarte = creerCarteInfo("ℹ️ Évaluation locale",
                    "L'API Gemini n'est pas disponible. Score calculé selon la longueur de vos réponses.\n" +
                            "Configurez votre clé API pour une analyse précise.", "#fff3f3", "#e74c3c");
            bilanContent.getChildren().add(avisCarte);
        }

        // ════ 6. SYNTHÈSE DES RÉPONSES ══════════════════════
        Label syntheLabel = new Label("📋  Synthèse de vos réponses");
        syntheLabel.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #1a3323;");
        bilanContent.getChildren().add(syntheLabel);

        for (int i = 0; i < questionsIA.size(); i++) {
            String rep = i < reponsesUtilisateur.size() ? reponsesUtilisateur.get(i) : "";
            boolean aRepondu = rep != null && !rep.isEmpty();

            VBox questionCard = new VBox(6);
            questionCard.setStyle("-fx-background-color: " + (aRepondu ? "#f9f9f9" : "#fff5f5") + ";" +
                    "-fx-background-radius: 10; -fx-padding: 10 14 10 14;" +
                    "-fx-border-color: " + (aRepondu ? "#e8e8e8" : "#ffcccc") + ";" +
                    "-fx-border-radius: 10; -fx-border-width: 1;");

            HBox qHeader = new HBox(6);
            qHeader.setAlignment(Pos.CENTER_LEFT);
            Label numLbl = new Label("Q" + (i + 1));
            numLbl.setStyle("-fx-background-color: " + couleurPrincipale + ";-fx-text-fill: white;" +
                    "-fx-font-weight: bold;-fx-font-size: 11;-fx-padding: 2 8;-fx-background-radius: 8;");
            Label qText = new Label(questionsIA.get(i));
            qText.setWrapText(true);
            qText.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: #333;");
            qText.setMaxWidth(380);
            HBox.setHgrow(qText, Priority.ALWAYS);
            qHeader.getChildren().addAll(numLbl, qText);

            Label repLbl;
            if (aRepondu) {
                String affiche = rep.length() > 100 ? rep.substring(0, 100) + "..." : rep;
                repLbl = new Label("→  " + affiche);
                repLbl.setStyle("-fx-font-size: 11; -fx-text-fill: #555; -fx-font-style: italic;");
            } else {
                repLbl = new Label("→  ⚠️ Aucune réponse détectée");
                repLbl.setStyle("-fx-font-size: 11; -fx-text-fill: #e74c3c;");
            }
            repLbl.setWrapText(true);

            questionCard.getChildren().addAll(qHeader, repLbl);
            bilanContent.getChildren().add(questionCard);
        }

        // ════ 7. MESSAGE FINAL ═══════════════════════════════
        Label finalMsg = new Label("🎉  Merci pour votre participation !\nRelancez l'entraînement pour progresser.");
        finalMsg.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        finalMsg.setWrapText(true);
        finalMsg.setStyle("-fx-font-size: 12; -fx-text-fill: #888; -fx-padding: 5 20 0 20;");
        bilanContent.getChildren().add(finalMsg);

        // Scroll en haut
        Platform.runLater(() -> bilanScrollPane.setVvalue(0));
    }

    /** Crée une carte info réutilisable avec titre + contenu */
    private VBox creerCarteInfo(String titre, String contenu, String bgColor, String titreColor) {
        VBox carte = new VBox(5);
        carte.setStyle("-fx-background-color:" + bgColor + ";-fx-background-radius:10;" +
                "-fx-padding:10 14 10 14;-fx-border-color:" + titreColor +
                ";-fx-border-radius:10;-fx-border-width:1.5;");
        Label t = new Label(titre);
        t.setStyle("-fx-font-size:11;-fx-font-weight:bold;-fx-text-fill:" + titreColor + ";");
        Label c = new Label(contenu);
        c.setWrapText(true);
        c.setStyle("-fx-font-size:12;-fx-text-fill:#333;");
        carte.getChildren().addAll(t, c);
        return carte;
    }

    // ════════════════════════════════════════════════════════
    //  PARSING GEMINI
    // ════════════════════════════════════════════════════════
    private int parseChamp(String text, String champ) {
        for (String line : text.split("\n")) {
            if (line.trim().startsWith(champ)) {
                String val = line.replace(champ, "").trim().replaceAll("[^0-9]", "");
                if (!val.isEmpty()) return Math.max(0, Math.min(100, Integer.parseInt(val)));
            }
        }
        return calculerScoreLocal();
    }

    private String parseTexte(String text, String champ) {
        for (String line : text.split("\n")) {
            if (line.trim().startsWith(champ))
                return line.replace(champ, "").trim();
        }
        return "";
    }

    // ════════════════════════════════════════════════════════
    //  SCORE LOCAL HONNÊTE
    // ════════════════════════════════════════════════════════
    private int calculerScoreLocal() {
        if (reponsesUtilisateur.isEmpty()) return 0;
        int totalMax = MAX_QUESTIONS * 14;
        int total    = 0;
        String[] mots = {"agriculture","culture","sol","irrigation","récolte",
                "fertilisant","expérience","compétence","équipe","motivé",
                "saison","production","formation","parcelle","semence"};
        for (String rep : reponsesUtilisateur) {
            if (rep == null || rep.trim().isEmpty()) continue;
            int len = rep.trim().length();
            int pts = len > 150 ? 14 : len > 50 ? 10 : len > 10 ? 6 : 2;
            for (String mot : mots) {
                if (rep.toLowerCase().contains(mot)) { pts = Math.min(pts + 4, 14); break; }
            }
            total += pts;
        }
        return (int) Math.round((total * 100.0) / totalMax);
    }

    // ════════════════════════════════════════════════════════
    //  7 QUESTIONS
    // ════════════════════════════════════════════════════════
    private String getQuestion(int step) {
        String[] questions = {
                "Bonjour ! Commençons par votre présentation. Parlez-moi de vous et de votre parcours en agriculture.",
                "Quelles sont vos compétences principales dans le domaine agricole ? Citez des exemples concrets.",
                "Pourquoi souhaitez-vous rejoindre AgriSmart ? Qu'est-ce qui vous motive dans cette entreprise ?",
                "Décrivez une situation difficile vécue dans une exploitation agricole. Comment l'avez-vous résolue ?",
                "Comment gérez-vous les périodes de forte activité, comme les récoltes ou les semailles ?",
                "Quelle est votre connaissance des nouvelles technologies agricoles, comme l'irrigation intelligente ?",
                "Avez-vous des questions pour nous ? Et quelle est votre disponibilité pour commencer ?"
        };
        return questions[Math.min(step, questions.length - 1)];
    }

    // ════════════════════════════════════════════════════════
    //  RECONNAISSANCE VOCALE
    // ════════════════════════════════════════════════════════
    private String capturerVoix(int secondes) {
        try {
            String script =
                    "Add-Type -AssemblyName System.Speech;" +
                            "$rec = New-Object System.Speech.Recognition.SpeechRecognitionEngine;" +
                            "$rec.SetInputToDefaultAudioDevice();" +
                            "$g = New-Object System.Speech.Recognition.DictationGrammar;" +
                            "$rec.LoadGrammar($g);" +
                            "$r = $rec.Recognize([System.TimeSpan]::FromSeconds(" + secondes + "));" +
                            "if ($r) { Write-Output $r.Text } else { Write-Output '' }";
            ProcessBuilder pb = new ProcessBuilder("powershell.exe", "-NonInteractive", "-Command", script);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder out = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) out.append(line).append(" ");
            process.waitFor();
            String result = out.toString().trim();
            System.out.println("🎤 Capturé: [" + result + "]");
            return result.length() > 2 ? result : null;
        } catch (Exception e) {
            System.err.println("Erreur micro: " + e.getMessage());
            return null;
        }
    }

    // ════════════════════════════════════════════════════════
    //  TTS BLOQUANT
    // ════════════════════════════════════════════════════════
    private void parlerBloquant(String texte) {
        try {
            String clean = texte.replace("'", " ").replace("\"", " ").replace("\n", " ")
                    .replace("%", " pourcent")
                    .replaceAll("[📊✅📈💬⚠️▶⏳🌱📢🗣️💭🎤🏆🌟👍💪💡🎉🏅ℹ️]", "");
            String cmd = "Add-Type -AssemblyName System.Speech; " +
                    "(New-Object System.Speech.Synthesis.SpeechSynthesizer).Speak('" + clean + "')";
            new ProcessBuilder("powershell.exe", "-NonInteractive", "-Command", cmd).start().waitFor();
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ════════════════════════════════════════════════════════
    //  FERMETURE PROPRE
    // ════════════════════════════════════════════════════════
    @FXML
    public void closeSimulator() {
        stopTout();
        FadeTransition ft = new FadeTransition(Duration.millis(150), simulatorModal);
        ft.setToValue(0);
        ft.setOnFinished(e -> {
            overlay.setVisible(false);
            simulatorModal.setVisible(false);
            simulatorModal.setOpacity(1.0);
            resetModalToIntro();
        });
        ft.play();
    }

    private void stopTout() {
        isSimulationActive = false;
        if (ecouteThread != null) { ecouteThread.interrupt(); ecouteThread = null; }
        stopMicAnimation();
    }

    private void startMicAnimation() {
        if (micDot == null) return;
        stopMicAnimation();
        micBlinkTimeline = new Timeline(
                new KeyFrame(Duration.ZERO,        e -> micDot.setStyle("-fx-font-size:14;-fx-text-fill:#e74c3c;")),
                new KeyFrame(Duration.millis(500), e -> micDot.setStyle("-fx-font-size:14;-fx-text-fill:transparent;"))
        );
        micBlinkTimeline.setCycleCount(Timeline.INDEFINITE);
        micBlinkTimeline.play();
    }

    private void stopMicAnimation() {
        if (micBlinkTimeline != null) { micBlinkTimeline.stop(); micBlinkTimeline = null; }
        if (micDot != null) micDot.setStyle("-fx-font-size:14;-fx-text-fill:#e74c3c;");
    }

    private void dormirMs(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    // ════════════════════════════════════════════════════════
    //  NAVIGATION & DÉTAIL
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
            detailFin.setText(df); dateLimitLabel.setText(df);
        }
        boolean expired = selectedOffreForView.getDate_fin() != null
                && LocalDateTime.now().isAfter(selectedOffreForView.getDate_fin());
        boolean closed = "Clôturée".equalsIgnoreCase(selectedOffreForView.getStatut());
        if (expired || closed) {
            btnPostuler.setDisable(true);
            btnPostuler.setText(closed ? "Offre Clôturée" : "Offre expirée");
            btnPostuler.setStyle("-fx-background-color:#95a5a6;-fx-text-fill:white;-fx-background-radius:10;");
        }
    }

    @FXML public void handlePostuler() {
        if (selectedOffreForView == null) return;
        try {
            boolean exists = demandeService.afficher().stream()
                    .anyMatch(d -> d.getUsers_id() == 2 && d.getOffre_id() == selectedOffreForView.getId().intValue());
            if (exists) new Alert(Alert.AlertType.INFORMATION, "Vous avez déjà postulé !").showAndWait();
            else { PostulerController.currentOffreId = selectedOffreForView.getId();
                navigateToView("/Views/Offres/PostulerForm.fxml"); }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @FXML public void showListPage() {
        selectedOffreForView = null; switchView("/Views/Offres/CandidatOffreList.fxml");
    }
    private void navigateToDetail() { switchView("/Views/Offres/OffreDetailView.fxml"); }
    private void navigateToView(String path) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(path));
            StackPane ca = (StackPane) btnPostuler.getScene().lookup("#contentArea");
            if (ca != null) ca.getChildren().setAll(root);
        } catch (IOException e) { e.printStackTrace(); }
    }
    private void switchView(String path) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(path));
            Scene scene = (cardsContainer != null && cardsContainer.getScene() != null)
                    ? cardsContainer.getScene() : (detailTitle != null ? detailTitle.getScene() : null);
            if (scene != null) {
                StackPane ca = (StackPane) scene.lookup("#contentArea");
                if (ca != null) ca.getChildren().setAll(root); else scene.setRoot(root);
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML public void handleSearch() {
        String q = searchField.getText() == null ? "" : searchField.getText().toLowerCase().trim();
        String s = statutFilter.getValue() == null ? "Toutes les offres" : statutFilter.getValue();
        try {
            List<Offre> all = service.afficher();
            cardsContainer.getChildren().clear();
            all.stream().filter(o ->
                            (o.getTitle().toLowerCase().contains(q) || o.getLieu().toLowerCase().contains(q))
                                    && (s.equals("Toutes les offres") || o.getStatut().equalsIgnoreCase(s)))
                    .forEach(o -> cardsContainer.getChildren().add(createCandidateCard(o)));
            countLabel.setText(cardsContainer.getChildren().size() + " offre(s) trouvée(s)");
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // ════════════════════════════════════════════════════════
    //  CHATBOT FLOTTANT
    // ════════════════════════════════════════════════════════
    @FXML public void handleUploadCV() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choisir votre CV");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Documents","*.pdf","*.png","*.jpg"));
        File file = fc.showOpenDialog(chatContainer.getScene().getWindow());
        if (file != null) {
            this.currentCVName = file.getName();
            addMessage("📁 CV sélectionné : " + file.getName(), true);
            handleSendIA("Analyse mon CV nommé '" + file.getName() + "' et propose-moi les offres AgriSmart adaptées.");
        }
    }

    @FXML public void toggleChat() {
        boolean visible = chatContainer.isVisible();
        chatContainer.setVisible(!visible); chatContainer.setManaged(!visible);
        if (!visible && isFirstOpen) {
            isFirstOpen = false;
            typeMessage("Bonjour ! 😊 Je suis AgriSmart AI. Je peux vous aider à trouver " +
                    "un travail ou simuler un entretien. Que voulez-vous faire ?", false);
        }
    }

    @FXML public void handleSendChat() {
        String userText = chatInputField.getText().trim();
        if (userText.isEmpty()) return;
        addMessage(userText, true); chatInputField.clear();
        try {
            List<Demande> userDemandes = demandeService.afficher().stream()
                    .filter(d -> d.getUsers_id() == 2).toList();
            if (offreSelectionneePourChat == null) {
                for (Demande d : userDemandes) {
                    Offre o = service.afficher().stream()
                            .filter(off -> off.getId().equals((long) d.getOffre_id())).findFirst().orElse(null);
                    if (o != null && userText.toLowerCase().contains(o.getTitle().toLowerCase())) {
                        offreSelectionneePourChat = d;
                        typeMessage("Bonjour ! 😊 Que souhaitez-vous modifier pour '" + o.getTitle() + "' ?", false);
                        return;
                    }
                }
            }
            if (offreSelectionneePourChat != null) {
                String lr = updateService.processMessage(userText, offreSelectionneePourChat);
                if (lr != null) {
                    typeMessage(lr, false);
                    if (lr.contains("✅")) { demandeService.modifier(offreSelectionneePourChat); offreSelectionneePourChat = null; }
                    return;
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        handleSendIA(userText);
    }

    private void handleSendIA(String userPrompt) {
        addMessage("...", false);
        int loadingIdx = chatMessagesArea.getChildren().size() - 1;
        new Thread(() -> {
            try {
                List<Offre> offres = service.afficher();
                String response = recrutementIAService.obtenirConseilRecrutement(userPrompt, currentCVName, offres);
                Platform.runLater(() -> {
                    if (loadingIdx < chatMessagesArea.getChildren().size())
                        chatMessagesArea.getChildren().remove(loadingIdx);
                    typeMessage(response, false);
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    if (loadingIdx < chatMessagesArea.getChildren().size())
                        chatMessagesArea.getChildren().remove(loadingIdx);
                    addMessage("Désolé, l'IA est indisponible.", false);
                });
            }
        }).start();
    }

    private void typeMessage(String fullText, boolean isUser) {
        Label msgLabel = new Label("");
        msgLabel.setWrapText(true); msgLabel.setMaxWidth(220.0);
        msgLabel.setPadding(new Insets(7, 11, 7, 11));
        msgLabel.setStyle(isUser
                ? "-fx-background-color:#1a3323;-fx-text-fill:white;-fx-background-radius:12;"
                : "-fx-background-color:#e8f5e9;-fx-text-fill:#1a3323;-fx-background-radius:12;");
        HBox box = new HBox(msgLabel);
        box.setAlignment(isUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        chatMessagesArea.getChildren().add(box);
        String[] words = fullText.split(" ");
        Timeline tl = new Timeline();
        for (int i = 0; i < words.length; i++) {
            final int idx = i; final String w = words[i];
            tl.getKeyFrames().add(new KeyFrame(Duration.millis(i * 70L), e -> {
                msgLabel.setText(msgLabel.getText() + (idx == 0 ? "" : " ") + w);
                chatScrollPane.setVvalue(1.0);
            }));
        }
        tl.play();
    }

    private void addMessage(String text, boolean isUser) {
        Label msg = new Label(text);
        msg.setWrapText(true); msg.setMaxWidth(220.0);
        msg.setPadding(new Insets(7, 11, 7, 11));
        msg.setStyle(isUser
                ? "-fx-background-color:#1a3323;-fx-text-fill:white;-fx-background-radius:12;"
                : "-fx-background-color:#e8f5e9;-fx-text-fill:#1a3323;-fx-background-radius:12;");
        HBox box = new HBox(msg);
        box.setAlignment(isUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        chatMessagesArea.getChildren().add(box);
        chatScrollPane.setVvalue(1.0);
    }
}
