package controllers;

import entities.Demande;
import javafx.animation.*;
import javafx.application.Platform;
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
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import services.DemandeService;

import java.awt.Desktop;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.ResourceBundle;

public class PostulerController implements Initializable {

    // ══════════════════════════════════════════════════════════
    //  CHAMPS EXISTANTS — NE PAS MODIFIER
    //  Ces noms correspondent exactement à votre PostulerForm.fxml
    // ══════════════════════════════════════════════════════════
    @FXML private TextField nomF;           // fx:id="nomF"
    @FXML private TextField prenomF;        // fx:id="prenomF"
    @FXML private TextField phoneF;         // fx:id="phoneF"
    @FXML private Label     cvLabel;        // fx:id="cvLabel"
    @FXML private Label     lettreLabel;    // fx:id="lettreLabel"
    @FXML private Button    btnSubmit;      // fx:id="btnSubmit"

    // Labels d'erreur existants
    @FXML private Label nomError;
    @FXML private Label prenomError;
    @FXML private Label phoneError;
    @FXML private Label cvError;
    @FXML private Label lettreError;

    // ══════════════════════════════════════════════════════════
    //  NOUVEAUX CHAMPS FXML — à ajouter dans PostulerForm.fxml
    // ══════════════════════════════════════════════════════════
    @FXML private Button voiceBtn;           // fx:id="voiceBtn"
    @FXML private Label  voiceStatusLabel;   // fx:id="voiceStatusLabel"
    @FXML private Label  voiceSubLabel;      // fx:id="voiceSubLabel"

    @FXML private VBox   iaZone;             // fx:id="iaZone"
    @FXML private VBox   iaPlaceholder;      // fx:id="iaPlaceholder"
    @FXML private VBox   iaSuccessBox;       // fx:id="iaSuccessBox"
    @FXML private Label  iaFilenameLabel;    // fx:id="iaFilenameLabel"

    // ══════════════════════════════════════════════════════════
    //  ÉTAT INTERNE
    // ══════════════════════════════════════════════════════════
    private File     selectedCV;
    private File     selectedLettre;
    private String   iaCvFileName  = null;
    private boolean  isRecording   = false;
    private Timeline micBlink      = null;

    private final DemandeService service = new DemandeService();

    public static long    currentOffreId;
    public static Demande selectedDemandeForEdit;

    private static final String API_BASE = "http://127.0.0.1:8000";

    // ══════════════════════════════════════════════════════════
    //  INITIALIZE
    // ══════════════════════════════════════════════════════════
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupRealTimeValidation();

        if (selectedDemandeForEdit != null) {
            nomF.setText(selectedDemandeForEdit.getNom());
            prenomF.setText(selectedDemandeForEdit.getPrenom());
            phoneF.setText(selectedDemandeForEdit.getPhone_number());
            cvLabel.setText(selectedDemandeForEdit.getCv());
            lettreLabel.setText(selectedDemandeForEdit.getLettre_motivation());
            btnSubmit.setText("💾 Modifier ma candidature");
        }
    }

    // ══════════════════════════════════════════════════════════
    //  VALIDATION TEMPS RÉEL — code existant conservé
    // ══════════════════════════════════════════════════════════
    private void setupRealTimeValidation() {
        nomF.textProperty().addListener((obs, o, newVal) -> {
            if (newVal.trim().length() < 3) {
                nomError.setText("Le nom doit avoir au moins 3 caractères");
                nomF.setStyle("-fx-border-color: #e74c3c;");
            } else if (!newVal.matches("^[a-zA-Z\\sçéàâêîôûäëïöü]+$")) {
                nomError.setText("Lettres uniquement");
                nomF.setStyle("-fx-border-color: #e74c3c;");
            } else {
                nomError.setText("");
                nomF.setStyle("-fx-border-color: #2ecc71;");
            }
        });

        prenomF.textProperty().addListener((obs, o, newVal) -> {
            if (newVal.trim().length() < 3) {
                prenomError.setText("Le prénom doit avoir au moins 3 caractères");
                prenomF.setStyle("-fx-border-color: #e74c3c;");
            } else if (!newVal.matches("^[a-zA-Z\\sçéàâêîôûäëïöü]+$")) {
                prenomError.setText("Lettres uniquement");
                prenomF.setStyle("-fx-border-color: #e74c3c;");
            } else {
                prenomError.setText("");
                prenomF.setStyle("-fx-border-color: #2ecc71;");
            }
        });

        phoneF.textProperty().addListener((obs, o, newVal) -> {
            if (!newVal.matches("\\d*")) {
                phoneF.setText(o);
            } else if (newVal.length() != 8) {
                phoneError.setText("Doit contenir exactement 8 chiffres");
                phoneF.setStyle("-fx-border-color: #e74c3c;");
            } else {
                phoneError.setText("");
                phoneF.setStyle("-fx-border-color: #2ecc71;");
            }
        });
    }

    // ══════════════════════════════════════════════════════════
    //  SUBMIT — code existant + accepte CV IA
    // ══════════════════════════════════════════════════════════
    @FXML
    public void handleSubmit() {
        if (!validate()) return;

        try {
            if (selectedDemandeForEdit == null) {
                boolean alreadyApplied = service.afficher().stream()
                        .anyMatch(d -> d.getUsers_id() == 2
                                && d.getOffre_id() == (int) currentOffreId);
                if (alreadyApplied) {
                    showAlert("Attention", "Vous avez déjà postulé pour cette offre !");
                    cancel();
                    return;
                }
            }

            // CV : priorité → upload classique > CV IA > edit existant
            String cvFileName;
            if (selectedCV != null) {
                cvFileName = saveFile(selectedCV, "src/main/resources/uploads/cv");
            } else if (iaCvFileName != null && !iaCvFileName.isBlank()) {
                cvFileName = iaCvFileName;
            } else {
                cvFileName = selectedDemandeForEdit != null
                        ? selectedDemandeForEdit.getCv() : "";
            }

            String lettreFileName = selectedLettre != null
                    ? saveFile(selectedLettre, "src/main/resources/uploads/lettres")
                    : (selectedDemandeForEdit != null
                       ? selectedDemandeForEdit.getLettre_motivation() : "");

            if (selectedDemandeForEdit != null) {
                // ✅ setters corrects de votre entité Demande existante
                selectedDemandeForEdit.setNom(nomF.getText().trim());
                selectedDemandeForEdit.setPrenom(prenomF.getText().trim());
                selectedDemandeForEdit.setPhone_number(phoneF.getText().trim());
                selectedDemandeForEdit.setCv(cvFileName);
                selectedDemandeForEdit.setLettre_motivation(lettreFileName);
                selectedDemandeForEdit.setDate_modification(LocalDateTime.now());
                service.modifier(selectedDemandeForEdit);
                showAlert("Succès", "Candidature modifiée !");
                selectedDemandeForEdit = null;
            } else {
                Demande d = new Demande();
                // ✅ setters corrects de votre entité Demande existante
                d.setNom(nomF.getText().trim());
                d.setPrenom(prenomF.getText().trim());
                d.setPhone_number(phoneF.getText().trim());       // ✅ phone_number
                d.setDate_postulation(LocalDateTime.now());        // ✅ date_postulation
                d.setDate_modification(LocalDateTime.now());       // ✅ date_modification
                d.setCv(cvFileName);
                d.setLettre_motivation(lettreFileName);            // ✅ lettre_motivation
                d.setStatut("En cours");
                d.setUsers_id(2);
                d.setOffre_id((int) currentOffreId);
                service.ajouter(d);
                showAlert("Succès", "Candidature envoyée !");
            }
            cancel();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ══════════════════════════════════════════════════════════
    //  VALIDATE — accepte aussi le CV IA
    // ══════════════════════════════════════════════════════════
    private boolean validate() {
        boolean isValid = true;
        if (nomF.getText().trim().isEmpty() || !nomError.getText().isEmpty()) {
            if (nomF.getText().trim().isEmpty()) nomError.setText("Champ obligatoire");
            isValid = false;
        }
        if (prenomF.getText().trim().isEmpty() || !prenomError.getText().isEmpty()) {
            if (prenomF.getText().trim().isEmpty()) prenomError.setText("Champ obligatoire");
            isValid = false;
        }
        if (phoneF.getText().trim().isEmpty() || !phoneError.getText().isEmpty()) {
            if (phoneF.getText().trim().isEmpty()) phoneError.setText("Champ obligatoire");
            isValid = false;
        }
        if (selectedDemandeForEdit == null) {
            boolean hasCV = selectedCV != null
                    || (iaCvFileName != null && !iaCvFileName.isBlank());
            if (!hasCV) {
                cvError.setText("Le CV est obligatoire");
                isValid = false;
            }
            if (selectedLettre == null) {
                lettreError.setText("La lettre est obligatoire");
                isValid = false;
            }
        }
        return isValid;
    }

    // ══════════════════════════════════════════════════════════
    //  UPLOAD CLASSIQUE — code existant conservé
    // ══════════════════════════════════════════════════════════
    @FXML public void uploadCV() {
        selectedCV = selectPDF();
        if (selectedCV != null) { cvLabel.setText(selectedCV.getName()); cvError.setText(""); }
    }

    @FXML public void uploadLettre() {
        selectedLettre = selectPDF();
        if (selectedLettre != null) { lettreLabel.setText(selectedLettre.getName()); lettreError.setText(""); }
    }

    private File selectPDF() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        return fc.showOpenDialog(nomF.getScene().getWindow());
    }

    private String saveFile(File file, String destPath) throws IOException {
        Path uploadPath = Paths.get(destPath);
        if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);
        String fileName = System.currentTimeMillis() + "_" + file.getName();
        Files.copy(file.toPath(), uploadPath.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
        return fileName;
    }

    @FXML public void cancel() {
        selectedDemandeForEdit = null;
        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource("/Views/Offres/MesCandidaturesList.fxml"));
            StackPane contentArea = (StackPane) nomF.getScene().lookup("#contentArea");
            contentArea.getChildren().setAll(root);
        } catch (IOException e) { e.printStackTrace(); }
    }

    // ══════════════════════════════════════════════════════════
    //  ████  ASSISTANT VOCAL IA  ████
    //
    //  AJOUT dans PostulerForm.fxml — AVANT le HBox nom/prenom :
    //
    //  <HBox alignment="CENTER_LEFT" spacing="15"
    //        style="-fx-background-color:white;-fx-background-radius:50;
    //               -fx-padding:12 25;-fx-border-color:#e0e0e0;
    //               -fx-border-radius:50;-fx-margin-bottom:15;">
    //      <Button fx:id="voiceBtn" onAction="#handleVoiceAssistant"
    //              text="🎙"
    //              style="-fx-background-color:#1a4331;-fx-background-radius:50;
    //                     -fx-min-width:50;-fx-min-height:50;
    //                     -fx-max-width:50;-fx-max-height:50;
    //                     -fx-text-fill:white;-fx-font-size:18;-fx-cursor:hand;"/>
    //      <VBox spacing="2">
    //          <Label fx:id="voiceStatusLabel" text="Assistant Vocal IA"
    //                 style="-fx-font-weight:bold;-fx-font-size:13;-fx-text-fill:#1a4331;"/>
    //          <Label fx:id="voiceSubLabel" text="Cliquez pour parler librement"
    //                 style="-fx-font-size:11;-fx-text-fill:#95a5a6;"/>
    //      </VBox>
    //  </HBox>
    // ══════════════════════════════════════════════════════════

    @FXML
    public void handleVoiceAssistant() {
        if (isRecording) { stopVoice(); return; }
        speak("Bonjour ! Présentez-vous. Dites votre prénom, votre nom, "
                + "puis votre numéro de téléphone.", this::startListening);
    }

    private void startListening() {
        isRecording = true;
        Platform.runLater(() -> {
            voiceBtn.setText("⏹");
            voiceBtn.setStyle(styleBtn("#dc3545"));
            voiceStatusLabel.setText("🔴 Je vous écoute...");
            voiceSubLabel.setText("Parlez clairement : Prénom, Nom, Numéro");
            startMicBlink();
        });
        new Thread(() -> {
            // ── On tente la reconnaissance 2 fois pour maximiser la détection ──
            String transcript = captureVoice(12);
            if (transcript == null || transcript.isBlank()) {
                transcript = captureVoice(10); // 2ème tentative
            }
            isRecording = false;
            final String finalT = transcript;
            Platform.runLater(this::stopVoiceUI);
            if (finalT != null && !finalT.isBlank()) {
                analyzeVoice(finalT);
            } else {
                speak("Je n'ai pas compris. Veuillez répéter lentement.", null);
                Platform.runLater(() -> voiceStatusLabel.setText("❌ Rien détecté, réessayez"));
            }
        }).start();
    }

    private void stopVoice() { isRecording = false; stopVoiceUI(); }

    private void stopVoiceUI() {
        if (voiceBtn == null) return;
        voiceBtn.setText("🎙");
        voiceBtn.setStyle(styleBtn("#1a4331"));
        voiceStatusLabel.setText("Assistant Vocal IA");
        voiceSubLabel.setText("Cliquez pour parler librement");
        stopMicBlink();
    }

    /**
     * ════════════════════════════════════════════════════
     *  DÉTECTION AMÉLIORÉE — Nom / Prénom / Téléphone
     *
     *  Gère les formules :
     *   "je m'appelle Ahmed Ben Ali mon numéro est 55123456"
     *   "bonjour je suis Sana Trabelsi 55 12 34 56"
     *   "mon prénom est Mohamed mon nom est Chaabane 22334455"
     *   "Ahmed Chaabane 55123456"
     * ════════════════════════════════════════════════════
     */
    private void analyzeVoice(String text) {
        String nom = "", prenom = "", phone = "";
        String t = text.toLowerCase().trim();

        // ── 1. TÉLÉPHONE : 8 chiffres consécutifs (Tunisie) ──────────────────
        // Accepte avec ou sans espaces entre les chiffres
        String digitsOnly = t.replaceAll("[^0-9]", "");
        if (digitsOnly.length() >= 8) {
            phone = digitsOnly.substring(0, 8);
        }

        // ── 2. PRÉNOM explicite : "mon prénom est X" / "je m'appelle X" ──────
        String[] prenomTriggers = {
                "mon prénom est ", "prénom est ", "je m'appelle ", "je mappelle ",
                "je suis ", "m'appelle ", "appelle moi ", "prénom "
        };
        for (String trigger : prenomTriggers) {
            if (t.contains(trigger)) {
                String after = t.substring(t.indexOf(trigger) + trigger.length()).trim();
                String[] parts = after.replaceAll("[^a-zA-Zçéàâêîôûäëïöü ]", "").trim().split("\\s+");
                if (parts.length > 0 && parts[0].length() > 1) {
                    prenom = cap(parts[0]);
                    break;
                }
            }
        }

        // ── 3. NOM explicite : "mon nom est X" / "nom de famille X" ─────────
        String[] nomTriggers = {
                "mon nom est ", "nom est ", "nom de famille ", "famille ", "nom "
        };
        for (String trigger : nomTriggers) {
            if (t.contains(trigger)) {
                String after = t.substring(t.indexOf(trigger) + trigger.length()).trim();
                String[] parts = after.replaceAll("[^a-zA-Zçéàâêîôûäëïöü ]", "").trim().split("\\s+");
                if (parts.length > 0 && parts[0].length() > 1) {
                    nom = cap(parts[0]);
                    break;
                }
            }
        }

        // ── 4. FALLBACK : extraction positionnelle ────────────────────────────
        // Si pas de trigger trouvé, on nettoie et prend les 1er/2ème mot
        if (prenom.isBlank() || nom.isBlank()) {
            String[] stopWords = {
                    "bonjour", "bonsoir", "salut", "je", "suis", "suis", "mon",
                    "ma", "prénom", "prenom", "nom", "est", "numéro", "numero",
                    "téléphone", "telephone", "appelle", "mappelle", "monsieur",
                    "madame", "mademoiselle", "famille", "de"
            };
            String clean = t;
            for (String w : stopWords) {
                clean = clean.replaceAll("\\b" + w + "\\b", " ");
            }
            clean = clean.replaceAll("[0-9]+", "").replaceAll("[^a-zA-Zçéàâêîôûäëïöü ]", "").trim();
            String[] words = Arrays.stream(clean.split("\\s+"))
                    .filter(w -> w.length() > 1)
                    .toArray(String[]::new);

            if (prenom.isBlank() && words.length >= 1) prenom = cap(words[0]);
            if (nom.isBlank()    && words.length >= 2) nom    = cap(words[1]);
        }

        final String fP = prenom, fN = nom, fPh = phone;
        Platform.runLater(() -> {
            if (!fP.isBlank())  { prenomF.setText(fP);  prenomError.setText(""); prenomF.setStyle("-fx-border-color:#2ecc71;"); }
            if (!fN.isBlank())  { nomF.setText(fN);     nomError.setText("");    nomF.setStyle("-fx-border-color:#2ecc71;"); }
            if (!fPh.isBlank()) { phoneF.setText(fPh);  phoneError.setText("");  phoneF.setStyle("-fx-border-color:#2ecc71;"); }

            boolean extracted = !fP.isBlank() || !fN.isBlank() || !fPh.isBlank();
            if (extracted) {
                voiceStatusLabel.setText("✅ Données extraites !");
                speak("Parfait ! J'ai rempli vos informations.", null);
            } else {
                voiceStatusLabel.setText("❌ Rien extrait, réessayez");
                speak("Je n'ai pas pu extraire vos informations. Parlez plus lentement.", null);
            }
            new Timeline(new KeyFrame(Duration.seconds(5),
                    e -> voiceStatusLabel.setText("Assistant Vocal IA"))).play();
        });
    }

    // ══════════════════════════════════════════════════════════
    //  ████  GÉNÉRATEUR CV IA — MODAL 4 ÉTAPES  ████
    //
    //  AJOUT dans PostulerForm.fxml — APRÈS votre bouton uploadCV :
    //
    //  <Button onAction="#openIAModal" text="🤖  Générer avec IA"
    //          style="-fx-background-color:white;-fx-text-fill:#00b37e;
    //                 -fx-border-color:#00b37e;-fx-border-radius:8;
    //                 -fx-padding:8 20;-fx-cursor:hand;"/>
    //
    //  <VBox fx:id="iaZone" spacing="10">
    //    <VBox fx:id="iaPlaceholder" alignment="CENTER" spacing="10"
    //          style="-fx-background-color:#f0fdf8;-fx-background-radius:10;
    //                 -fx-border-color:#00b37e;-fx-border-radius:10;
    //                 -fx-border-style:dashed;-fx-padding:25;">
    //      <Label text="Besoin d'un CV professionnel complet ?"
    //             style="-fx-font-weight:bold;-fx-text-fill:#00b37e;"/>
    //      <Button onAction="#openIAModal" text="✨  Créer mon CV avec l'IA"
    //              style="-fx-background-color:#00b37e;-fx-text-fill:white;
    //                     -fx-background-radius:20;-fx-padding:8 24;-fx-cursor:hand;"/>
    //    </VBox>
    //    <VBox fx:id="iaSuccessBox" spacing="8" visible="false" managed="false"
    //          style="-fx-background-color:white;-fx-background-radius:10;
    //                 -fx-border-color:#00b37e;-fx-border-radius:10;
    //                 -fx-border-width:2;-fx-padding:14;">
    //      <HBox spacing="10" alignment="CENTER_LEFT">
    //        <Label text="✅" style="-fx-font-size:20;"/>
    //        <VBox spacing="2">
    //          <Label text="CV IA généré !"
    //                 style="-fx-font-weight:bold;-fx-text-fill:#00b37e;"/>
    //          <Label fx:id="iaFilenameLabel" text=""
    //                 style="-fx-text-fill:#888;-fx-font-size:11;"/>
    //        </VBox>
    //      </HBox>
    //      <HBox spacing="10">
    //        <Button onAction="#previewIACV" text="👁  Consulter"
    //                style="-fx-background-color:#00b37e;-fx-text-fill:white;
    //                       -fx-background-radius:15;-fx-padding:5 16;-fx-cursor:hand;"/>
    //        <Button onAction="#resetIA" text="🗑 Supprimer"
    //                style="-fx-background-color:transparent;-fx-text-fill:#e74c3c;
    //                       -fx-cursor:hand;"/>
    //      </HBox>
    //    </VBox>
    //  </VBox>
    // ══════════════════════════════════════════════════════════

    // Photo sélectionnée pour le CV IA
    private File   photoFile     = null;
    private String photoBase64   = null;

    @FXML
    public void openIAModal() {
        // Reset photo pour ce nouveau CV
        photoFile   = null;
        photoBase64 = null;

        Stage modal = new Stage();
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.initStyle(StageStyle.UNDECORATED);

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color:white;-fx-background-radius:15;");
        root.setPrefWidth(620);

        // En-tête vert
        HBox header = new HBox();
        header.setStyle("-fx-background-color:#1a4331;-fx-padding:14 20;"
                + "-fx-background-radius:15 15 0 0;");
        Label hTitle = new Label("🤖  Assistant CV Agricole IA");
        hTitle.setStyle("-fx-text-fill:white;-fx-font-weight:bold;-fx-font-size:14;");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Button closeBtn = new Button("✕");
        closeBtn.setStyle("-fx-background-color:transparent;-fx-text-fill:white;"
                + "-fx-font-size:16;-fx-cursor:hand;");
        closeBtn.setOnAction(e -> modal.close());
        header.getChildren().addAll(hTitle, sp, closeBtn);

        // Barre progression
        HBox progWrapper = new HBox();
        progWrapper.setStyle("-fx-background-color:#e0e0e0;");
        Region progBar = new Region();
        progBar.setPrefHeight(6);
        progBar.setStyle("-fx-background-color:#1a4331;");
        progWrapper.getChildren().add(progBar);

        // Contenu (étapes)
        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color:white;-fx-background:white;"
                + "-fx-border-color:transparent;");
        VBox content = new VBox(18);
        content.setPadding(new Insets(25, 35, 25, 35));
        scroll.setContent(content);
        scroll.setPrefHeight(520);

        root.getChildren().addAll(header, progWrapper, scroll);

        // Données partagées entre étapes — tableau[21]
        String[] data = new String[21];
        Arrays.fill(data, "");

        int[] step = {1};
        renderStep(step, data, content, progBar, modal);

        Scene scene = new Scene(root);
        scene.setFill(null);
        modal.setScene(scene);
        modal.showAndWait();
    }

    private void renderStep(int[] step, String[] data, VBox content, Region pb, Stage modal) {
        content.getChildren().clear();
        pb.setPrefWidth((step[0] / 4.0) * 620);
        switch (step[0]) {
            case 1 -> buildStep1(step, data, content, pb, modal);
            case 2 -> buildStep2(step, data, content, pb, modal);
            case 3 -> buildStep3(step, data, content, pb, modal);
            case 4 -> buildStep4(step, data, content, pb, modal);
        }
    }

    // ── Étape 1 : Identité & Contact + PHOTO ─────────────────
    private void buildStep1(int[] step, String[] data, VBox content, Region pb, Stage modal) {
        content.getChildren().add(stepBadge("1", "Identité & Contact"));

        TextField fNom    = tf("Votre nom",       data[0]);
        TextField fPrenom = tf("Votre prénom",    data[1]);
        TextField fVille  = tf("Ville / Adresse", data[2]);
        TextField fEmail  = tf("Email",           data[3]);
        TextField fPhone  = tf("Téléphone",       data[4]);

        ComboBox<String> cSexe   = combo("Homme", "Femme");
        cSexe.setValue(data[5].isBlank() ? "Homme" : data[5]);

        ComboBox<String> cPermis = combo("Non", "Oui (Catégorie B)", "Oui (Tracteur / Agricole)");
        cPermis.setValue(data[6].isBlank() ? "Non" : data[6]);

        DatePicker dp = new DatePicker();
        dp.setMaxWidth(Double.MAX_VALUE);
        if (!data[7].isBlank()) {
            try { dp.setValue(java.time.LocalDate.parse(data[7])); } catch (Exception ignored) {}
        }

        // ── PHOTO DE PROFIL ───────────────────────────────────
        // ════════════════════════════════════════════════════════
        //  LIGNE ~380 — Zone de sélection de photo
        //  C'est ici que l'upload photo est géré dans le modal IA
        // ════════════════════════════════════════════════════════
        ImageView photoPreview = new ImageView();
        photoPreview.setFitWidth(80);
        photoPreview.setFitHeight(80);
        photoPreview.setStyle("-fx-border-radius:50;");

        Label photoLabel = new Label("Aucune photo");
        photoLabel.setStyle("-fx-font-size:11;-fx-text-fill:#888;");

        Button btnPhoto = new Button("📷  Choisir une photo");
        btnPhoto.setStyle("-fx-background-color:#f0f0f0;-fx-background-radius:8;"
                + "-fx-padding:7 14;-fx-cursor:hand;-fx-font-size:12;");

        // Si une photo avait déjà été choisie avant, on ré-affiche l'aperçu
        if (photoFile != null) {
            photoLabel.setText(photoFile.getName());
            photoPreview.setImage(new Image(photoFile.toURI().toString()));
        }

        // ══ ACTION : clic sur "Choisir une photo" ══════════════
        // LIGNE ~400 — Handler upload photo
        btnPhoto.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Choisir votre photo de profil");
            fc.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
            File chosen = fc.showOpenDialog(modal);
            if (chosen != null) {
                photoFile = chosen;
                photoLabel.setText(chosen.getName());
                photoPreview.setImage(new Image(chosen.toURI().toString()));
                // Convertir en Base64 pour l'envoyer à l'API Symfony
                try {
                    byte[] bytes = Files.readAllBytes(chosen.toPath());
                    String ext   = chosen.getName().contains("png") ? "image/png" : "image/jpeg";
                    photoBase64  = "data:" + ext + ";base64," + Base64.getEncoder().encodeToString(bytes);
                } catch (IOException ex) { ex.printStackTrace(); }
            }
        });

        VBox photoBox = new VBox(8, photoPreview, btnPhoto, photoLabel);
        photoBox.setAlignment(Pos.CENTER_LEFT);
        photoBox.setStyle("-fx-background-color:#f9f9f9;-fx-background-radius:10;"
                + "-fx-padding:12;-fx-border-color:#e0e0e0;-fx-border-radius:10;");

        // Pré-remplissage depuis le formulaire principal
        if (fNom.getText().isBlank()    && nomF   != null && !nomF.getText().isBlank())    fNom.setText(nomF.getText());
        if (fPrenom.getText().isBlank() && prenomF != null && !prenomF.getText().isBlank()) fPrenom.setText(prenomF.getText());
        if (fPhone.getText().isBlank()  && phoneF  != null && !phoneF.getText().isBlank())  fPhone.setText(phoneF.getText());

        content.getChildren().addAll(
                row(lbl("Nom *",     fNom),    lbl("Prénom *",           fPrenom)),
                row(lbl("Sexe *",    cSexe),   lbl("Date de naissance",  dp)),
                row(lbl("Ville *",   fVille),  lbl("Permis de conduire", cPermis)),
                row(lbl("Email *",   fEmail),  lbl("Téléphone",          fPhone)),
                lbl("📷 Photo de profil (optionnel)", photoBox)  // ← PHOTO ICI
        );

        Button next = nextBtn("Suivant →");
        next.setOnAction(e -> {
            if (fNom.getText().isBlank() || fPrenom.getText().isBlank()
                    || fVille.getText().isBlank() || fEmail.getText().isBlank()) {
                showAlert("Champs manquants", "Nom, Prénom, Ville et Email sont obligatoires.");
                return;
            }
            data[0] = fNom.getText();    data[1] = fPrenom.getText();
            data[2] = fVille.getText();  data[3] = fEmail.getText();
            data[4] = fPhone.getText();  data[5] = cSexe.getValue();
            data[6] = cPermis.getValue();
            data[7] = dp.getValue() != null ? dp.getValue().toString() : "";
            step[0] = 2; renderStep(step, data, content, pb, modal);
        });
        content.getChildren().add(navRow(null, next));
    }

    // ── Étape 2 : Profil & Formation ─────────────────────────
    private void buildStep2(int[] step, String[] data, VBox content, Region pb, Stage modal) {
        content.getChildren().add(stepBadge("2", "Profil & Formation"));

        TextField fPoste = tf("Ex: Technicien Agricole",   data[8]);
        TextField fEtude = tf("Ex: Licence en Agronomie",  data[9]);
        TextField fAnnee = tf("Ex: 2022",                  data[10]);
        TextField fSpec  = tf("Établissement & Spécialité",data[11]);

        content.getChildren().addAll(
                lbl("Titre du poste souhaité *", fPoste),
                row(lbl("Dernier Diplôme *", fEtude), lbl("Année d'obtention", fAnnee)),
                lbl("Établissement & Spécialité", fSpec)
        );

        Button back = backBtn("← Retour");
        Button next = nextBtn("Suivant →");
        back.setOnAction(e -> { step[0] = 1; renderStep(step, data, content, pb, modal); });
        next.setOnAction(e -> {
            if (fPoste.getText().isBlank() || fEtude.getText().isBlank()) {
                showAlert("Champs manquants", "Poste et diplôme sont obligatoires.");
                return;
            }
            data[8]  = fPoste.getText(); data[9]  = fEtude.getText();
            data[10] = fAnnee.getText(); data[11] = fSpec.getText();
            step[0] = 3; renderStep(step, data, content, pb, modal);
        });
        content.getChildren().add(navRow(back, next));
    }

    // ── Étape 3 : Expériences & Outils ───────────────────────
    private void buildStep3(int[] step, String[] data, VBox content, Region pb, Stage modal) {
        content.getChildren().add(stepBadge("3", "Expériences & Outils"));

        TextField fEnt    = tf("Dernière entreprise / ferme",          data[12]);
        TextField fDebut  = tf("Ex: 2021",                             data[13]);
        TextField fFin    = tf("Ex: 2023 ou Présent",                  data[14]);
        TextArea  fDesc   = new TextArea(data[15]);
        fDesc.setPromptText("Décrivez vos missions...");
        fDesc.setPrefRowCount(3);
        fDesc.setStyle(tfStyle());
        TextField fInfo   = tf("Ex: Excel, GPS, Drones",               data[16]);
        TextField fSkills = tf("Ex: Irrigation, Récolte, Pesticides",  data[17]);

        content.getChildren().addAll(
                lbl("Dernière entreprise / ferme", fEnt),
                row(lbl("Début", fDebut), lbl("Fin", fFin)),
                lbl("Description des missions", fDesc),
                lbl("Outils informatiques", fInfo),
                lbl("Compétences techniques *", fSkills)
        );

        Button back = backBtn("← Retour");
        Button next = nextBtn("Suivant →");
        back.setOnAction(e -> { step[0] = 2; renderStep(step, data, content, pb, modal); });
        next.setOnAction(e -> {
            if (fSkills.getText().isBlank()) {
                showAlert("Champ manquant", "Les compétences techniques sont obligatoires.");
                return;
            }
            data[12] = fEnt.getText();   data[13] = fDebut.getText();
            data[14] = fFin.getText();   data[15] = fDesc.getText();
            data[16] = fInfo.getText();  data[17] = fSkills.getText();
            step[0] = 4; renderStep(step, data, content, pb, modal);
        });
        content.getChildren().add(navRow(back, next));
    }

    // ── Étape 4 : Langues & Loisirs ──────────────────────────
    private void buildStep4(int[] step, String[] data, VBox content, Region pb, Stage modal) {
        content.getChildren().add(stepBadge("4", "Langues & Loisirs"));

        TextField fFrench  = tf("Ex: Courant",           data[18]);
        TextField fEnglish = tf("Ex: Bien",              data[19]);
        TextField fLoisirs = tf("Ex: Jardinage, Lecture",data[20]);

        content.getChildren().addAll(
                row(lbl("Français *", fFrench), lbl("Anglais *", fEnglish)),
                lbl("Loisirs", fLoisirs)
        );

        Button back   = backBtn("← Retour");
        Button genBtn = nextBtn("✨  Générer mon CV");
        genBtn.setStyle("-fx-background-color:#00b37e;-fx-text-fill:white;"
                + "-fx-font-size:13;-fx-font-weight:bold;-fx-background-radius:25;"
                + "-fx-padding:10;-fx-cursor:hand;");

        back.setOnAction(e -> { step[0] = 3; renderStep(step, data, content, pb, modal); });
        genBtn.setOnAction(e -> {
            if (fFrench.getText().isBlank() || fEnglish.getText().isBlank()) {
                showAlert("Champs manquants", "Veuillez indiquer vos niveaux de langue.");
                return;
            }
            data[18] = fFrench.getText();
            data[19] = fEnglish.getText();
            data[20] = fLoisirs.getText();
            genBtn.setDisable(true);
            genBtn.setText("⏳  Génération en cours...");
            generateCVWithAPI(data, modal, genBtn);
        });
        content.getChildren().add(navRow(back, genBtn));
    }

    // ══════════════════════════════════════════════════════════
    //  APPEL API SYMFONY — même endpoint que la version web
    //  POST /candidature/generate-cv-ia
    //  Envoie aussi la photo en Base64 (champ "photo_base64")
    // ══════════════════════════════════════════════════════════
    private void generateCVWithAPI(String[] data, Stage modal, Button genBtn) {
        new Thread(() -> {
            try {
                String boundary = "----JavaFXBoundary" + System.currentTimeMillis();
                StringBuilder body = new StringBuilder();

                // Champs texte (identiques au FormData JS de la version web)
                String[][] fields = {
                        {"nom",        data[0]}, {"prenom",     data[1]},
                        {"ville",      data[2]}, {"email",      data[3]},
                        {"phone",      data[4]}, {"sexe",       data[5]},
                        {"permis",     data[6]}, {"birthdate",  data[7]},
                        {"jobTitle",   data[8]}, {"etude",      data[9]},
                        {"etudeAnnee", data[10]},{"specialite", data[11]},
                        {"entreprise", data[12]},{"expDebut",   data[13]},
                        {"expFin",     data[14]},{"expDesc",    data[15]},
                        {"informatique",data[16]},{"skills",    data[17]},
                        {"french",     data[18]},{"english",    data[19]},
                        {"loisirs",    data[20]}
                };

                for (String[] f : fields) {
                    body.append("--").append(boundary).append("\r\n")
                            .append("Content-Disposition: form-data; name=\"")
                            .append(f[0]).append("\"\r\n\r\n")
                            .append(f[1] != null ? f[1] : "").append("\r\n");
                }

                // ══ PHOTO EN BASE64 ══════════════════════════════════
                // LIGNE ~570 — Envoi de la photo à l'API Symfony
                // Le serveur PHP reçoit "photo_base64" et l'intègre dans le CV
                if (photoBase64 != null && !photoBase64.isBlank()) {
                    body.append("--").append(boundary).append("\r\n")
                            .append("Content-Disposition: form-data; name=\"photo_base64\"\r\n\r\n")
                            .append(photoBase64).append("\r\n");
                }

                body.append("--").append(boundary).append("--\r\n");

                URL url = new URL(API_BASE + "/candidature/generate-cv-ia");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setConnectTimeout(15_000);
                conn.setReadTimeout(60_000);
                conn.setRequestProperty("Content-Type",
                        "multipart/form-data; boundary=" + boundary);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body.toString().getBytes("UTF-8"));
                }

                int code = conn.getResponseCode();
                if (code == 200) {
                    BufferedReader br = new BufferedReader(
                            new InputStreamReader(conn.getInputStream()));
                    StringBuilder resp = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) resp.append(line);
                    String json = resp.toString();

                    if (json.contains("\"success\":true") && json.contains("\"fileName\"")) {
                        String fileName = json.split("\"fileName\"\\s*:\\s*\"")[1].split("\"")[0];
                        Platform.runLater(() -> onCVReady(fileName, modal));
                    } else {
                        Platform.runLater(() -> {
                            showAlert("Erreur", "La génération a échoué côté serveur.");
                            genBtn.setDisable(false);
                            genBtn.setText("✨  Générer mon CV");
                        });
                    }
                } else {
                    // Serveur non disponible → fallback HTML local
                    Platform.runLater(() -> generateCVLocally(data, modal));
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() -> generateCVLocally(data, modal));
            }
        }).start();
    }

    /** Fallback hors-ligne : génère un CV HTML en local */
    private void generateCVLocally(String[] d, Stage modal) {
        try {
            String fileName = "CV_IA_AgriSmart_" + System.currentTimeMillis() + ".html";
            Path dir = Paths.get(System.getProperty("user.home"), "AgriSmart_CVs");
            Files.createDirectories(dir);
            Files.writeString(dir.resolve(fileName), buildLocalCVHtml(d));
            iaCvFileName = "LOCAL:" + dir.resolve(fileName).toString();
            onCVReady("LOCAL:" + fileName, modal);
        } catch (IOException e) {
            showAlert("Erreur", "Impossible de générer le CV : " + e.getMessage());
        }
    }

    private void onCVReady(String fileName, Stage modal) {
        iaCvFileName = fileName;
        iaFilenameLabel.setText("Fichier : " + fileName.replace("LOCAL:", ""));
        iaPlaceholder.setVisible(false); iaPlaceholder.setManaged(false);
        iaSuccessBox.setVisible(true);   iaSuccessBox.setManaged(true);
        cvError.setText("");
        modal.close();
        showAlert("Succès !", "CV IA généré !\nVous pouvez soumettre votre candidature.");
    }

    @FXML public void previewIACV() {
        if (iaCvFileName == null) return;
        try {
            if (iaCvFileName.startsWith("LOCAL:")) {
                String path = iaCvFileName.replace("LOCAL:", "");
                Desktop.getDesktop().open(new File(path).getParentFile());
            } else {
                Desktop.getDesktop().browse(
                        new java.net.URI(API_BASE + "/uploads/cv/" + iaCvFileName));
            }
        } catch (Exception e) { showAlert("Erreur", e.getMessage()); }
    }

    @FXML public void resetIA() {
        iaCvFileName = null; photoFile = null; photoBase64 = null;
        iaFilenameLabel.setText("");
        iaSuccessBox.setVisible(false); iaSuccessBox.setManaged(false);
        iaPlaceholder.setVisible(true); iaPlaceholder.setManaged(true);
    }

    // ══════════════════════════════════════════════════════════
    //  CV HTML LOCAL — FALLBACK HORS LIGNE
    // ══════════════════════════════════════════════════════════
    private String buildLocalCVHtml(String[] d) {
        String name   = (d[1] + " " + d[0]).trim().toUpperCase();
        String photo  = (photoBase64 != null)
                ? "<img src='" + photoBase64 + "' style='width:100px;height:100px;"
                  + "border-radius:50%;object-fit:cover;border:3px solid rgba(255,255,255,0.3);'>"
                : "<div style='width:100px;height:100px;background:rgba(255,255,255,.1);"
                  + "border-radius:50%;margin:0 auto;line-height:100px;text-align:center;"
                  + "font-size:11px;border:2px dashed rgba(255,255,255,.3)'>PHOTO</div>";
        String skills = Arrays.stream(d[17].split(","))
                .map(s -> "<span style='display:inline-block;background:#f0f7f4;color:#1a4331;"
                        + "padding:4px 10px;border-radius:15px;font-size:11px;margin:3px;"
                        + "border:1px solid #d1e7dd'>" + s.trim() + "</span>")
                .reduce("", String::concat);
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'>"
                + "<style>body{font-family:Arial,sans-serif;margin:0;color:#2c3e50;}"
                + ".sidebar{width:30%;background:#1a4331;color:white;position:absolute;"
                + "left:0;top:0;bottom:0;padding:40px 20px;box-sizing:border-box;}"
                + ".main{margin-left:35%;padding:40px 30px;}"
                + ".name{font-size:26px;font-weight:bold;text-transform:uppercase;color:#1a4331;}"
                + ".job{font-size:15px;color:#4a7c59;text-transform:uppercase;margin-top:5px;}"
                + ".sec{margin-top:20px;font-size:12px;font-weight:bold;color:#1a4331;"
                + "text-transform:uppercase;border-bottom:2px solid #e8f5e9;padding-bottom:4px;}"
                + ".lbl{font-size:10px;color:#a8c69f;text-transform:uppercase;"
                + "display:block;margin-top:10px;}</style></head><body>"
                + "<div class='sidebar'>"
                + "<div style='text-align:center;margin-bottom:20px'>" + photo + "</div>"
                + "<span class='lbl'>Téléphone</span>" + d[4]
                + "<span class='lbl'>Email</span>" + d[3]
                + "<span class='lbl'>Localisation</span>" + d[2]
                + "<span class='lbl'>Sexe</span>" + d[5]
                + "<div style='margin-top:18px;font-weight:bold;font-size:12px;"
                + "border-bottom:1px solid rgba(255,255,255,.2);padding-bottom:5px'>LANGUES</div>"
                + "<div style='font-size:11px;margin-top:6px'><b>Français :</b> " + d[18]
                + "<br><b>Anglais :</b> " + d[19] + "</div>"
                + "<div style='margin-top:18px;font-weight:bold;font-size:12px;"
                + "border-bottom:1px solid rgba(255,255,255,.2);padding-bottom:5px'>LOISIRS</div>"
                + "<div style='font-size:11px;margin-top:6px'>" + d[20] + "</div>"
                + "</div>"
                + "<div class='main'>"
                + "<div class='name'>" + name + "</div>"
                + "<div class='job'>" + d[8] + "</div>"
                + "<div style='font-size:12px;color:#666;margin-top:3px'>Spécialité : " + d[11] + "</div>"
                + "<div class='sec'>Formation & Études</div>"
                + "<div style='font-weight:bold;margin-top:8px'>" + d[9] + "</div>"
                + "<div style='font-size:11px;color:#666;font-style:italic'>Promotion " + d[10] + "</div>"
                + "<div class='sec'>Expérience Professionnelle</div>"
                + "<div style='font-weight:bold;margin-top:8px'>" + d[12] + "</div>"
                + "<div style='font-size:11px;color:#666;font-style:italic'>"
                + d[13] + " — " + (d[14].isBlank() ? "Présent" : d[14]) + "</div>"
                + "<div style='font-size:12px;margin-top:6px;color:#444'>" + d[15] + "</div>"
                + "<div class='sec'>Outils Informatiques</div>"
                + "<div style='font-size:12px;background:#fcfcfc;padding:8px;"
                + "border-left:3px solid #1a4331;margin-top:8px'>" + d[16] + "</div>"
                + "<div class='sec'>Compétences Techniques</div>"
                + "<div style='margin-top:8px'>" + skills + "</div>"
                + "</div></body></html>";
    }

    // ══════════════════════════════════════════════════════════
    //  TTS + STT POWERSHELL (Windows)
    // ══════════════════════════════════════════════════════════
    private void speak(String text, Runnable onDone) {
        new Thread(() -> {
            try {
                String clean = text.replace("'", " ").replace("\"", " ");
                String cmd = "Add-Type -AssemblyName System.Speech; "
                        + "(New-Object System.Speech.Synthesis.SpeechSynthesizer)"
                        + ".Speak('" + clean + "')";
                new ProcessBuilder("powershell.exe", "-NonInteractive", "-Command", cmd)
                        .redirectErrorStream(true).start().waitFor();
            } catch (Exception ignored) {}
            if (onDone != null) onDone.run();
        }).start();
    }

    private String captureVoice(int seconds) {
        try {
            String script =
                    "Add-Type -AssemblyName System.Speech;"
                            + "$r=New-Object System.Speech.Recognition.SpeechRecognitionEngine;"
                            + "$r.SetInputToDefaultAudioDevice();"
                            + "$g=New-Object System.Speech.Recognition.DictationGrammar;"
                            + "$r.LoadGrammar($g);"
                            + "$res=$r.Recognize([System.TimeSpan]::FromSeconds(" + seconds + "));"
                            + "if($res){Write-Output $res.Text}else{Write-Output ''}";
            ProcessBuilder pb = new ProcessBuilder(
                    "powershell.exe", "-NonInteractive", "-Command", script);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(p.getInputStream()));
            StringBuilder out = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) out.append(line).append(" ");
            p.waitFor();
            return out.toString().trim();
        } catch (Exception e) { return null; }
    }

    // ══════════════════════════════════════════════════════════
    //  HELPERS UI
    // ══════════════════════════════════════════════════════════
    private HBox stepBadge(String num, String title) {
        Label badge = new Label(" " + num + " ");
        badge.setStyle("-fx-background-color:#1a4331;-fx-text-fill:white;"
                + "-fx-padding:3 9;-fx-background-radius:50;-fx-font-weight:bold;");
        Label lbl = new Label(title);
        lbl.setStyle("-fx-font-size:15;-fx-font-weight:bold;-fx-text-fill:#1a4331;");
        HBox h = new HBox(10, badge, lbl);
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    private TextField tf(String prompt, String value) {
        TextField t = new TextField(value);
        t.setPromptText(prompt);
        t.setStyle(tfStyle());
        return t;
    }

    private String tfStyle() {
        return "-fx-background-radius:8;-fx-border-color:#ddd;-fx-border-radius:8;"
                + "-fx-padding:8;-fx-font-size:12;";
    }

    @SafeVarargs
    private <T> ComboBox<T> combo(T... items) {
        ComboBox<T> cb = new ComboBox<>();
        cb.getItems().addAll(items);
        cb.setMaxWidth(Double.MAX_VALUE);
        cb.setStyle("-fx-background-radius:8;-fx-border-color:#ddd;-fx-border-radius:8;");
        return cb;
    }

    private VBox lbl(String label, javafx.scene.Node field) {
        Label l = new Label(label);
        l.setStyle("-fx-font-size:11;-fx-font-weight:bold;-fx-text-fill:#555;");
        VBox v = new VBox(5, l, field);
        HBox.setHgrow(v, Priority.ALWAYS);
        return v;
    }

    private HBox row(javafx.scene.Node... nodes) {
        HBox h = new HBox(15, nodes);
        for (javafx.scene.Node n : nodes) HBox.setHgrow(n, Priority.ALWAYS);
        return h;
    }

    private HBox navRow(Button back, Button next) {
        HBox h = back == null ? new HBox(next) : new HBox(15, back, next);
        h.setAlignment(Pos.CENTER_RIGHT);
        if (back != null) HBox.setHgrow(back, Priority.ALWAYS);
        HBox.setHgrow(next, Priority.ALWAYS);
        return h;
    }

    private Button nextBtn(String text) {
        Button b = new Button(text);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setStyle("-fx-background-color:#1a4331;-fx-text-fill:white;"
                + "-fx-background-radius:20;-fx-padding:9 20;"
                + "-fx-font-weight:bold;-fx-cursor:hand;");
        return b;
    }

    private Button backBtn(String text) {
        Button b = new Button(text);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setStyle("-fx-background-color:#f0f0f0;-fx-text-fill:#333;"
                + "-fx-background-radius:20;-fx-padding:9 20;-fx-cursor:hand;");
        return b;
    }

    private String styleBtn(String color) {
        return "-fx-background-color:" + color + ";-fx-background-radius:50;"
                + "-fx-min-width:50;-fx-min-height:50;-fx-max-width:50;-fx-max-height:50;"
                + "-fx-text-fill:white;-fx-font-size:18;-fx-cursor:hand;";
    }

    private void startMicBlink() {
        stopMicBlink();
        micBlink = new Timeline(
                new KeyFrame(Duration.ZERO, e ->
                        voiceBtn.setStyle(styleBtn("#dc3545")
                                + "-fx-effect:dropshadow(three-pass-box,rgba(220,53,69,.7),15,0,0,0);")),
                new KeyFrame(Duration.millis(600), e ->
                        voiceBtn.setStyle(styleBtn("#dc3545")))
        );
        micBlink.setCycleCount(Timeline.INDEFINITE);
        micBlink.play();
    }

    private void stopMicBlink() {
        if (micBlink != null) { micBlink.stop(); micBlink = null; }
    }

    private String cap(String s) {
        if (s == null || s.isBlank()) return "";
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }

    private void showAlert(String title, String content) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(content);
        a.showAndWait();
    }
}