package controllers.marketplace;

import entities.MarketplaceMessage;
import entities.Produit;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import services.MarketplaceImageService;
import services.MarketplaceConversationService;
import services.MarketplaceMessageService;
import services.ProduitService;
import services.UserService;
import services.VoiceRecordingService;
import services.WebSocketClient;
import services.WebSocketServer;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.LineEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import javafx.util.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

public class MarketplaceMessagingFeature {
    private static final DateTimeFormatter CHAT_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM HH:mm");
    private static final String VOICE_MESSAGE_TEXT = "Message vocal";
    private static final String LEGACY_CALL_PREFIX = "CALL_INVITE|";

    private final MarketplaceMessagingState state;
    private final MarketplaceConversationService conversationService;
    private final MarketplaceMessageService messageService;
    private final ProduitService produitService;
    private final UserService userService;
    private final Button btnMessagingCenter;
    private final StackPane messagingOverlay;
    private final VBox conversationListBox;
    private final VBox messageListBox;
    private final Label messagingTitleLabel;
    private final Label messagingMetaLabel;
    private final TextArea messageInputArea;
    private final ToggleButton btnVoiceMessage;
    private final Consumer<StackPane> animateOverlayIn;
    private final Consumer<StackPane> animateOverlayOut;
    private final IntSupplier currentUserIdSupplier;
    private final IntSupplier fallbackSellerIdSupplier;
    private final BiConsumer<String, String> showAlert;
    private final Consumer<SQLException> showSqlAlert;
    private final BiConsumer<String, Boolean> showToast;
    private final java.util.function.Function<String, String> normalizeText;
    private final WebSocketClient webSocketClient = new WebSocketClient();
    private final VoiceRecordingService voiceRecorder = new VoiceRecordingService();
    private final MarketplaceImageService uploads = new MarketplaceImageService();
    private int websocketConversationId = -1;
    /** UI while recording near composer */
    private VBox voiceRecordingBanner;
    private Label recordingTimerLabel;
    private Timeline recordingElapsedTimeline;
    private Timeline recordingWaveTimeline;
    private final ArrayList<Rectangle> waveBars = new ArrayList<>();
    private long recordingWallClockStartMs;

    private Clip playbackClip;
    private Path playbackClipSource;
    private Timeline playbackPositionTicker;
    private Button playbackActiveButton;
    private ProgressBar playbackActiveBar;
    private Label playbackActiveTimeLabel;

    public MarketplaceMessagingFeature(
            MarketplaceMessagingState state,
            MarketplaceConversationService conversationService,
            MarketplaceMessageService messageService,
            ProduitService produitService,
            UserService userService,
            Button btnMessagingCenter,
            StackPane messagingOverlay,
            VBox conversationListBox,
            VBox messageListBox,
            Label messagingTitleLabel,
            Label messagingMetaLabel,
            TextArea messageInputArea,
            ToggleButton btnVoiceMessage,
            Consumer<StackPane> animateOverlayIn,
            Consumer<StackPane> animateOverlayOut,
            IntSupplier currentUserIdSupplier,
            IntSupplier fallbackSellerIdSupplier,
            BiConsumer<String, String> showAlert,
            Consumer<SQLException> showSqlAlert,
            BiConsumer<String, Boolean> showToast,
            java.util.function.Function<String, String> normalizeText) {
        this.state = state;
        this.conversationService = conversationService;
        this.messageService = messageService;
        this.produitService = produitService;
        this.userService = userService;
        this.btnMessagingCenter = btnMessagingCenter;
        this.messagingOverlay = messagingOverlay;
        this.conversationListBox = conversationListBox;
        this.messageListBox = messageListBox;
        this.messagingTitleLabel = messagingTitleLabel;
        this.messagingMetaLabel = messagingMetaLabel;
        this.messageInputArea = messageInputArea;
        this.btnVoiceMessage = btnVoiceMessage;
        this.animateOverlayIn = animateOverlayIn;
        this.animateOverlayOut = animateOverlayOut;
        this.currentUserIdSupplier = currentUserIdSupplier;
        this.fallbackSellerIdSupplier = fallbackSellerIdSupplier;
        this.showAlert = showAlert;
        this.showSqlAlert = showSqlAlert;
        this.showToast = showToast;
        this.normalizeText = normalizeText;
        initializeRealtimeMessaging();
    }

    private void initializeRealtimeMessaging() {
        webSocketClient.addMessageListener(message -> {
            String type = message.optString("type", "");
            if ("message".equals(type)) {
                Platform.runLater(() -> handleRealtimeMessage(message.optInt("conversationId", -1)));
            } else if ("conversation_read".equals(type)) {
                Platform.runLater(() -> {
                    int selectedConversationId = getSelectedConversationId();
                    if (selectedConversationId > 0) {
                        loadConversationsAndRender(selectedConversationId);
                    } else {
                        refreshMessagingBadge();
                    }
                });
            }
        });

        webSocketClient.addErrorListener(error -> {
            if (error != null && !error.isBlank()) {
                Platform.runLater(() -> showToast.accept("Temps reel indisponible", false));
            }
        });
    }

    private void handleRealtimeMessage(int conversationId) {
        refreshMessagingBadge();
        if (conversationId > 0 && state.selectedConversation != null && state.selectedConversation.getId() == conversationId) {
            loadConversationsAndRender(conversationId);
        }
    }

    private void connectRealtimeIfNeeded(entities.MarketplaceConversation conversation) {
        if (conversation == null || conversation.getId() <= 0) {
            return;
        }

        if (webSocketClient.isConnected() && websocketConversationId == conversation.getId()) {
            return;
        }

        webSocketClient.disconnect();
        websocketConversationId = conversation.getId();

        webSocketClient.connect(
                WebSocketServer.getInstance().getConnectionUrl(),
                currentUserIdSupplier.getAsInt(),
                conversation.getId(),
                new CountDownLatch(1)
        );
    }

    public void refreshMessagingBadge() {
        if (btnMessagingCenter == null) {
            return;
        }
        try {
            int unread = messageService.countUnreadForUser(currentUserIdSupplier.getAsInt());
            btnMessagingCenter.setText(unread <= 0 ? "Messagerie" : "Messagerie " + unread);
        } catch (SQLException e) {
            btnMessagingCenter.setText("Messagerie");
        }
    }

    public void openMessagingCenter() {
        if (messagingOverlay == null) {
            return;
        }
        cancelVoiceRecordingQuietly();
        clearPendingMessageContext();
        loadConversationsAndRender(null);
        animateOverlayIn.accept(messagingOverlay);
    }

    public void closeMessagingCenter() {
        if (messagingOverlay == null) {
            return;
        }
        cancelVoiceRecordingQuietly();
        stopPlaybackFully();
        animateOverlayOut.accept(messagingOverlay);
    }

    public void sendCurrentMessage() {
        if (messageInputArea == null) {
            return;
        }

        String content = safe(messageInputArea.getText()).trim();
        if (content.isEmpty()) {
            return;
        }

        try {
            if (state.selectedConversation == null) {
                if (!state.hasPendingMessageContext()) {
                    showAlert.accept("Messagerie", "Choisissez une conversation ou contactez un vendeur depuis la fiche produit.");
                    return;
                }
                state.selectedConversation = conversationService.findOrCreateConversation(
                        state.pendingProductId,
                        currentUserIdSupplier.getAsInt(),
                        state.pendingSellerId
                );
            }

            connectRealtimeIfNeeded(state.selectedConversation);

            if (webSocketClient.isConnected()) {
                webSocketClient.sendMessage(content, "");
                messageInputArea.clear();
                refreshMessagingBadge();
                clearPendingMessageContext();
                showToast.accept("Message envoye.", true);
                return;
            }

            MarketplaceMessage message = new MarketplaceMessage();
            message.setConversationId(state.selectedConversation.getId());
            message.setSenderId(currentUserIdSupplier.getAsInt());
            message.setContent(content);
            message.setRead(false);
            messageService.ajouter(message);
            conversationService.touchLastMessage(state.selectedConversation.getId());

            messageInputArea.clear();
            renderMessages(state.selectedConversation);
            loadConversationsAndRender(state.selectedConversation.getId());
            refreshMessagingBadge();
            clearPendingMessageContext();
            showToast.accept("Message envoye.", true);
        } catch (SQLException e) {
            showSqlAlert.accept(e);
        }
    }

    /**
     * Bound to the "Message vocal" toggle in FXML: selected = recording; unselect = send WAV.
     */
    public void handleVoiceToggle() {
        if (btnVoiceMessage == null) {
            return;
        }
        if (btnVoiceMessage.isSelected()) {
            beginVoiceRecording();
        } else {
            finishVoiceRecordingAndSend();
        }
    }

    private void beginVoiceRecording() {
        stopPlaybackFully();
        try {
            voiceRecorder.start();
            if (btnVoiceMessage != null) {
                btnVoiceMessage.setText("Envoyer...");
                btnVoiceMessage.getStyleClass().remove("recording-active-toggle");
                btnVoiceMessage.getStyleClass().add("recording-active-toggle");
            }
            if (messageInputArea != null) {
                messageInputArea.setEditable(false);
            }
            openRecordingBanner();
            showToast.accept("Microphone actif.", true);
        } catch (LineUnavailableException ex) {
            hideRecordingBannerFully();
            if (btnVoiceMessage != null) {
                btnVoiceMessage.setSelected(false);
                btnVoiceMessage.setText("Message vocal");
                btnVoiceMessage.getStyleClass().remove("recording-active-toggle");
            }
            if (messageInputArea != null) {
                messageInputArea.setEditable(true);
            }
            showAlert.accept("Microphone", "Micro indisponible: " + ex.getMessage());
        }
    }

    private void cancelVoiceRecordingQuietly() {
        voiceRecorder.cancel();
        hideRecordingBannerFully();
        if (btnVoiceMessage != null) {
            btnVoiceMessage.setSelected(false);
            btnVoiceMessage.setText("Message vocal");
            btnVoiceMessage.getStyleClass().remove("recording-active-toggle");
        }
        if (messageInputArea != null) {
            messageInputArea.setEditable(true);
        }
    }

    /** User clicked Annuler — does not send. */
    private void cancelVoiceRecordingFromUser() {
        voiceRecorder.cancel();
        hideRecordingBannerFully();
        if (btnVoiceMessage != null) {
            btnVoiceMessage.setSelected(false);
            btnVoiceMessage.setText("Message vocal");
            btnVoiceMessage.getStyleClass().remove("recording-active-toggle");
        }
        if (messageInputArea != null) {
            messageInputArea.setEditable(true);
        }
        showToast.accept("Enregistrement annule.", false);
    }

    private void finishVoiceRecordingAndSend() {
        hideRecordingBannerFully();
        if (messageInputArea != null) {
            messageInputArea.setEditable(true);
        }
        if (btnVoiceMessage != null) {
            btnVoiceMessage.setText("Message vocal");
            btnVoiceMessage.getStyleClass().remove("recording-active-toggle");
        }
        Path outFile = uploads.getSharedUploadsDir()
                .resolve("messages")
                .resolve(UUID.randomUUID() + ".wav");
        try {
            voiceRecorder.stopAndSaveWav(outFile);
        } catch (IOException ex) {
            showToast.accept(ex.getMessage() == null ? "Enregistrement annule." : ex.getMessage(), false);
            cancelVoiceRecordingQuietly();
            return;
        }

        String relative = "uploads/messages/" + outFile.getFileName();

        try {
            if (state.selectedConversation == null) {
                if (!state.hasPendingMessageContext()) {
                    showAlert.accept("Messagerie", "Choisissez une conversation ou contactez un vendeur depuis la fiche produit.");
                    cleanupVoiceDraft(outFile);
                    cancelVoiceRecordingQuietly();
                    return;
                }
                state.selectedConversation = conversationService.findOrCreateConversation(
                        state.pendingProductId,
                        currentUserIdSupplier.getAsInt(),
                        state.pendingSellerId
                );
            }

            connectRealtimeIfNeeded(state.selectedConversation);

            if (webSocketClient.isConnected()) {
                webSocketClient.sendMessage(VOICE_MESSAGE_TEXT, relative);
                refreshMessagingBadge();
                clearPendingMessageContext();
                showToast.accept("Message vocal envoye.", true);
                cancelVoiceRecordingQuietly();
                return;
            }

            MarketplaceMessage message = new MarketplaceMessage();
            message.setConversationId(state.selectedConversation.getId());
            message.setSenderId(currentUserIdSupplier.getAsInt());
            message.setContent(VOICE_MESSAGE_TEXT);
            message.setRead(false);
            message.setAudioPath(relative);
            messageService.ajouter(message);
            conversationService.touchLastMessage(state.selectedConversation.getId());

            renderMessages(state.selectedConversation);
            loadConversationsAndRender(state.selectedConversation.getId());
            refreshMessagingBadge();
            clearPendingMessageContext();
            showToast.accept("Message vocal envoye.", true);
            cancelVoiceRecordingQuietly();
        } catch (SQLException e) {
            cleanupVoiceDraft(outFile);
            cancelVoiceRecordingQuietly();
            showSqlAlert.accept(e);
        }
    }

    private static void cleanupVoiceDraft(Path wav) {
        try {
            Files.deleteIfExists(wav);
        } catch (IOException ignored) {
        }
    }

    private void ensureRecordingBannerInserted() {
        if (voiceRecordingBanner != null || messageInputArea == null) {
            return;
        }
        Parent p = messageInputArea.getParent();
        if (!(p instanceof VBox wrap)) {
            return;
        }
        voiceRecordingBanner = new VBox(10);
        voiceRecordingBanner.getStyleClass().add("voice-recording-banner");
        voiceRecordingBanner.setVisible(false);
        voiceRecordingBanner.setManaged(false);

        HBox top = new HBox(12);
        top.setAlignment(Pos.CENTER_LEFT);
        Circle pulse = new Circle(5, Color.web("#dc2626"));
        Label subtitle = new Label("Enregistrement en cours");
        subtitle.getStyleClass().add("voice-recording-title");
        Region grow = new Region();
        HBox.setHgrow(grow, Priority.ALWAYS);
        recordingTimerLabel = new Label("0:00");
        recordingTimerLabel.getStyleClass().add("voice-recording-timer");
        top.getChildren().addAll(pulse, subtitle, grow, recordingTimerLabel);

        HBox waves = new HBox(3);
        waves.setAlignment(Pos.CENTER_LEFT);
        waves.setPadding(new Insets(4, 0, 8, 0));
        waveBars.clear();
        for (int i = 0; i < 14; i++) {
            Rectangle r = new Rectangle(4, 10);
            r.setArcWidth(3);
            r.setArcHeight(3);
            r.setFill(Color.web(i % 2 == 0 ? "#145f3d" : "#2e9d62"));
            waveBars.add(r);
            waves.getChildren().add(r);
        }

        HBox bottom = new HBox(12);
        bottom.setAlignment(Pos.CENTER_LEFT);
        Button cancel = new Button("Annuler");
        cancel.getStyleClass().add("voice-recording-cancel");
        cancel.setOnAction(e -> cancelVoiceRecordingFromUser());
        Label hint = new Label("Recliquez sur « Message vocal » pour envoyer l'audio.");
        hint.setWrapText(true);
        hint.getStyleClass().add("voice-msg-time-hint");

        bottom.getChildren().addAll(cancel, hint);
        voiceRecordingBanner.getChildren().addAll(top, waves, bottom);
        wrap.getChildren().add(0, voiceRecordingBanner);
    }

    private void openRecordingBanner() {
        ensureRecordingBannerInserted();
        if (voiceRecordingBanner == null) {
            return;
        }
        recordingWallClockStartMs = System.currentTimeMillis();
        if (recordingTimerLabel != null) {
            recordingTimerLabel.setText("0:00");
        }
        if (recordingElapsedTimeline != null) {
            recordingElapsedTimeline.stop();
        }
        recordingElapsedTimeline = new Timeline(new KeyFrame(Duration.millis(120), ev -> {
            if (recordingTimerLabel != null) {
                long sec = (System.currentTimeMillis() - recordingWallClockStartMs) / 1000L;
                recordingTimerLabel.setText(formatElapsedClock(sec));
            }
        }));
        recordingElapsedTimeline.setCycleCount(Animation.INDEFINITE);
        recordingElapsedTimeline.play();

        if (recordingWaveTimeline != null) {
            recordingWaveTimeline.stop();
        }
        final double[] phase = {0};
        recordingWaveTimeline = new Timeline(new KeyFrame(Duration.millis(65), ev -> {
            phase[0] += 0.45;
            for (int i = 0; i < waveBars.size(); i++) {
                double amp = 0.55 + 0.45 * Math.sin(phase[0] + i * 0.51);
                double h = Math.max(4, Math.min(28, 8 + 20 * amp));
                waveBars.get(i).setHeight(h);
            }
        }));
        recordingWaveTimeline.setCycleCount(Animation.INDEFINITE);
        recordingWaveTimeline.play();

        voiceRecordingBanner.setVisible(true);
        voiceRecordingBanner.setManaged(true);
    }

    private static String formatElapsedClock(long totalSec) {
        long m = totalSec / 60;
        long s = totalSec % 60;
        return String.format("%d:%02d", m, s);
    }

    private void hideRecordingBannerFully() {
        if (recordingElapsedTimeline != null) {
            recordingElapsedTimeline.stop();
            recordingElapsedTimeline = null;
        }
        if (recordingWaveTimeline != null) {
            recordingWaveTimeline.stop();
            recordingWaveTimeline = null;
        }
        if (voiceRecordingBanner != null) {
            voiceRecordingBanner.setVisible(false);
            voiceRecordingBanner.setManaged(false);
        }
    }

    public void openMessagingForProduct(Produit produit) {
        if (produit == null) {
            return;
        }
        try {
            int sellerId = resolveSellerIdForProduct(produit);
            entities.MarketplaceConversation existingConversation = conversationService.findConversation(
                    produit.getId(),
                    currentUserIdSupplier.getAsInt(),
                    sellerId
            );

            state.pendingProductId = produit.getId();
            state.pendingSellerId = sellerId;

            if (messagingOverlay != null) {
                loadConversationsAndRender(existingConversation == null ? null : existingConversation.getId());
                animateOverlayIn.accept(messagingOverlay);
            }

            if (existingConversation == null) {
                state.selectedConversation = null;
                if (messagingTitleLabel != null) {
                    refreshMessagingDisplayMaps();
                    messagingTitleLabel.setText("Nouveau message - " + buildConversationHeader(state.pendingProductId, state.pendingSellerId));
                }
                if (messagingMetaLabel != null) {
                    messagingMetaLabel.setText("Conversation creee a l'envoi du premier message");
                }
                if (messageListBox != null) {
                    messageListBox.getChildren().clear();
                    Label hint = new Label("Ecrivez votre premier message. La conversation sera creee apres envoi.");
                    hint.getStyleClass().add("chat-empty-label");
                    messageListBox.getChildren().add(hint);
                }
            }
        } catch (SQLException e) {
            showSqlAlert.accept(e);
        }
    }

    public void loadConversationsAndRender(Integer preferredConversationId) {
        if (conversationListBox == null || messageListBox == null) {
            return;
        }

        conversationListBox.getChildren().clear();
        messageListBox.getChildren().clear();

        try {
            refreshMessagingDisplayMaps();
            List<entities.MarketplaceConversation> conversations = conversationService.getByUser(currentUserIdSupplier.getAsInt());
            if (conversations.isEmpty()) {
                Label empty = new Label("Aucune conversation pour le moment. Ouvrez un produit puis cliquez sur Contacter le vendeur.");
                empty.getStyleClass().add("chat-empty-label");
                conversationListBox.getChildren().add(empty);
                state.selectedConversation = null;
                if (messagingTitleLabel != null) {
                    messagingTitleLabel.setText("Messagerie");
                }
                if (messagingMetaLabel != null) {
                    messagingMetaLabel.setText("0 conversation");
                }
                return;
            }

            entities.MarketplaceConversation toSelect = null;
            for (entities.MarketplaceConversation conversation : conversations) {
                VBox item = buildConversationTile(conversation);
                conversationListBox.getChildren().add(item);

                if (preferredConversationId != null && conversation.getId() == preferredConversationId) {
                    toSelect = conversation;
                }
            }

            if (toSelect == null && !state.hasPendingMessageContext()) {
                toSelect = conversations.get(0);
            }

            if (toSelect != null) {
                selectConversation(toSelect);
            }

            if (messagingMetaLabel != null) {
                messagingMetaLabel.setText(conversations.size() + " conversation(s)");
            }
        } catch (SQLException e) {
            showSqlAlert.accept(e);
        }
    }

    public void selectConversation(entities.MarketplaceConversation conversation) {
        state.selectedConversation = conversation;
        connectRealtimeIfNeeded(conversation);
        renderMessages(conversation);

        if (messagingTitleLabel != null) {
            messagingTitleLabel.setText(buildConversationHeader(conversation.getProduitId(), conversation.getSellerId()));
        }

        try {
            messageService.markConversationAsRead(conversation.getId(), currentUserIdSupplier.getAsInt());
            refreshMessagingBadge();
        } catch (SQLException e) {
            showSqlAlert.accept(e);
        }
    }

    public void clearPendingMessageContext() {
        state.clearPendingMessageContext();
    }

    public boolean hasPendingMessageContext() {
        return state.hasPendingMessageContext();
    }

    public int getSelectedConversationId() {
        return state.selectedConversation == null ? -1 : state.selectedConversation.getId();
    }

    private void renderMessages(entities.MarketplaceConversation conversation) {
        if (messageListBox == null) {
            return;
        }

        messageListBox.getChildren().clear();
        try {
            List<MarketplaceMessage> messages = messageService.getByConversation(conversation.getId());
            if (messages.isEmpty()) {
                Label empty = new Label("Aucun message. Dites bonjour pour demarrer la discussion.");
                empty.getStyleClass().add("chat-empty-label");
                messageListBox.getChildren().add(empty);
                return;
            }

            for (MarketplaceMessage message : messages) {
                boolean mine = message.getSenderId() == currentUserIdSupplier.getAsInt();
                String rawContent = safe(message.getContent());
                String audio = safe(message.getAudioPath());
                VBox bubble;
                if (!audio.isBlank()) {
                    bubble = buildVoiceMessageBubble(audio, mine);
                } else if (rawContent.startsWith(LEGACY_CALL_PREFIX)) {
                    bubble = buildLegacyCallInviteBubble(mine);
                } else {
                    Label body = new Label(normalizeText.apply(rawContent));
                    body.setWrapText(true);
                    body.getStyleClass().add(mine ? "chat-bubble-me" : "chat-bubble-them");
                    body.setMaxWidth(460);
                    bubble = new VBox(4, body);
                    bubble.setFillWidth(false);
                }

                String senderDisplay = mine ? "Vous" : resolveUserDisplayName(message.getSenderId());
                Label meta = new Label(senderDisplay + " - " + formatDateTime(message.getCreatedAt()));
                meta.getStyleClass().add("chat-bubble-meta");
                bubble.getChildren().add(meta);

                HBox row = new HBox(bubble);
                row.setAlignment(mine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
                row.getStyleClass().add("chat-row");

                messageListBox.getChildren().add(row);
            }
        } catch (SQLException e) {
            showSqlAlert.accept(e);
        }
    }

    private VBox buildVoiceMessageBubble(String audioRelativePath, boolean mine) {
        Path wav = resolveUploadedAudioPath(audioRelativePath);
        boolean playable = wav != null && Files.exists(wav);

        Label mic = new Label("\u266A");
        mic.setMinSize(36, 36);
        mic.setAlignment(Pos.CENTER);
        mic.getStyleClass().add("voice-msg-mic");
        if (!mine) {
            mic.getStyleClass().add("voice-msg-mic-them");
        }

        Label title = new Label(VOICE_MESSAGE_TEXT);
        title.getStyleClass().add("voice-msg-title");

        Label hint = new Label(playable ? "Lecture ou pause pendant l'écoute." : "Fichier audio introuvable");
        hint.getStyleClass().add("voice-msg-time-hint");

        ProgressBar progress = new ProgressBar(0);
        progress.setPrefWidth(220);
        progress.setMaxWidth(Double.MAX_VALUE);
        progress.getStyleClass().add("voice-msg-progress");

        Label timeLbl = new Label("0:00 / 0:00");
        timeLbl.getStyleClass().add("voice-msg-time-hint");

        Button playPause = new Button("Lecture");
        playPause.setDisable(!playable);
        playPause.setMinSize(40, 40);
        playPause.getStyleClass().add("voice-msg-play-btn");
        playPause.getStyleClass().add(mine ? "voice-msg-play-btn-mine" : "voice-msg-play-btn-them");
        playPause.setOnAction(e -> {
            if (playable && wav != null) {
                toggleVoicePlayback(wav, playPause, progress, timeLbl);
            }
        });

        VBox center = new VBox(4, title, hint, progress, timeLbl);
        center.setFillWidth(true);

        HBox row = new HBox(12, mic, center, playPause);
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(center, Priority.ALWAYS);

        VBox card = new VBox(6, row);
        card.setMaxWidth(480);
        card.getStyleClass().add("voice-msg-card");
        card.getStyleClass().add(mine ? "voice-msg-card-mine" : "voice-msg-card-them");

        if (playable && wav != null) {
            try {
                Clip probe = AudioSystem.getClip();
                try (AudioInputStream ais = AudioSystem.getAudioInputStream(wav.toFile())) {
                    probe.open(ais);
                }
                long len = Math.max(0L, probe.getMicrosecondLength());
                timeLbl.setText("0:00 / " + formatPlaybackUs(len));
                probe.close();
            } catch (Exception ignored) {
                timeLbl.setText("0:00 / —");
            }
        }

        return card;
    }

    private VBox buildLegacyCallInviteBubble(boolean mine) {
        Label t = new Label("Invitation d'appel (ancienne fonction)");
        t.getStyleClass().add("voice-msg-title");
        Label s = new Label("Les appels utilisent maintenant des messages vocaux.");
        s.setWrapText(true);
        s.getStyleClass().add("voice-msg-time-hint");

        VBox card = new VBox(6, t, s);
        card.setMaxWidth(420);
        card.getStyleClass().add("voice-msg-card");
        card.getStyleClass().add(mine ? "voice-msg-card-mine" : "voice-msg-card-them");
        return card;
    }

    private Path resolveUploadedAudioPath(String stored) {
        String s = safe(stored).trim().replace('\\', '/');
        if (s.isEmpty()) {
            return null;
        }
        Path root = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        if (s.startsWith("uploads/")) {
            return root.resolve(s).normalize();
        }
        return uploads.getSharedUploadsDir().resolve(s).normalize();
    }

    private void toggleVoicePlayback(Path wav, Button toggle, ProgressBar bar, Label timeLabel) {
        if (wav == null) {
            return;
        }
        try {
            Path abs = wav.toAbsolutePath().normalize();
            if (playbackClip != null && playbackClipSource != null && playbackClipSource.equals(abs)) {
                if (playbackClip.isRunning()) {
                    playbackClip.stop();
                    stopPlaybackTickerOnly();
                    toggle.setText("Lecture");
                    return;
                }
                long clipLen = Math.max(1, playbackClip.getMicrosecondLength());
                long pos = playbackClip.getMicrosecondPosition();
                if (pos >= clipLen - 180_000L) {
                    playbackClip.setMicrosecondPosition(0);
                    if (bar != null) {
                        bar.setProgress(0);
                    }
                }
                playbackClip.start();
                startPlaybackTicker(bar, timeLabel);
                toggle.setText("Pause");
                return;
            }
        } catch (Exception ignored) {
        }

        stopPlaybackFully();
        try {
            Clip clip = AudioSystem.getClip();
            try (AudioInputStream ais = AudioSystem.getAudioInputStream(wav.toFile())) {
                clip.open(ais);
            }
            final long length = Math.max(1, clip.getMicrosecondLength());
            playbackClip = clip;
            playbackClipSource = wav.toAbsolutePath().normalize();
            playbackActiveButton = toggle;
            playbackActiveBar = bar;
            playbackActiveTimeLabel = timeLabel;

            toggle.setText("Pause");

            clip.addLineListener(ev -> {
                if (ev.getType() != LineEvent.Type.STOP) {
                    return;
                }
                Clip c = playbackClip;
                Platform.runLater(() -> {
                    if (c == null || playbackClip != c) {
                        return;
                    }
                    long pos = Math.max(0, c.getMicrosecondPosition());
                    boolean naturallyEnded = pos >= length - 200_000L;
                    if (!naturallyEnded) {
                        return;
                    }
                    stopPlaybackTickerOnly();
                    toggle.setText("Lecture");
                    if (bar != null) {
                        bar.setProgress(0);
                    }
                    if (timeLabel != null) {
                        timeLabel.setText("0:00 / " + formatPlaybackUs(length));
                    }
                    try {
                        c.flush();
                        c.close();
                    } catch (Exception ignored) {
                    }
                    playbackClip = null;
                    playbackClipSource = null;
                    playbackActiveButton = null;
                    playbackActiveBar = null;
                    playbackActiveTimeLabel = null;
                });
            });

            clip.start();
            startPlaybackTicker(bar, timeLabel);
        } catch (IOException | UnsupportedAudioFileException | LineUnavailableException ex) {
            showToast.accept("Lecture impossible.", false);
        }
    }

    private void startPlaybackTicker(ProgressBar bar, Label timeLabel) {
        if (playbackClip == null) {
            return;
        }
        stopPlaybackTickerOnly();
        playbackPositionTicker = new Timeline(new KeyFrame(Duration.millis(45), ev -> {
            if (playbackClip == null || !playbackClip.isOpen()) {
                return;
            }
            long len = Math.max(1, playbackClip.getMicrosecondLength());
            long pos = Math.min(len, playbackClip.getMicrosecondPosition());
            double p = (double) pos / (double) len;
            if (bar != null) {
                bar.setProgress(p);
            }
            if (timeLabel != null) {
                timeLabel.setText(formatPlaybackUs(pos) + " / " + formatPlaybackUs(len));
            }
        }));
        playbackPositionTicker.setCycleCount(Animation.INDEFINITE);
        playbackPositionTicker.play();
    }

    private void stopPlaybackTickerOnly() {
        if (playbackPositionTicker != null) {
            playbackPositionTicker.stop();
            playbackPositionTicker = null;
        }
    }

    private void stopPlaybackFully() {
        stopPlaybackTickerOnly();
        try {
            if (playbackClip != null) {
                playbackClip.stop();
                playbackClip.flush();
                playbackClip.close();
            }
        } catch (Exception ignored) {
        }
        playbackClip = null;
        playbackClipSource = null;
        if (playbackActiveButton != null) {
            playbackActiveButton.setText("Lecture");
        }
        if (playbackActiveBar != null) {
            playbackActiveBar.setProgress(0);
        }
        playbackActiveButton = null;
        playbackActiveBar = null;
        playbackActiveTimeLabel = null;
    }

    private static String formatPlaybackUs(long micros) {
        if (micros < 0) {
            micros = 0;
        }
        long totalSec = micros / 1_000_000L;
        long m = totalSec / 60;
        long s = totalSec % 60;
        return String.format("%d:%02d", m, s);
    }

    private void refreshMessagingDisplayMaps() throws SQLException {
        state.productNameById.clear();
        state.userNameById.clear();

        for (Produit produit : produitService.afficher()) {
            if (produit == null) {
                continue;
            }
            state.productNameById.put(produit.getId(), normalizeText.apply(safe(produit.getNom())));
        }

        for (entities.User user : userService.getAllUsers()) {
            if (user == null) {
                continue;
            }
            state.userNameById.put(user.getId(), buildUserDisplayName(user));
        }
    }

    private VBox buildConversationTile(entities.MarketplaceConversation conversation) {
        Label title = new Label(resolveProductDisplayName(conversation.getProduitId()));
        title.getStyleClass().add("chat-tile-title");

        Label subtitle = new Label("Vendeur: " + resolveUserDisplayName(conversation.getSellerId()));
        subtitle.getStyleClass().add("chat-tile-subtitle");

        Label date = new Label(formatDateTime(conversation.getLastMessageAt()));
        date.getStyleClass().add("chat-tile-date");

        Circle dot = new Circle(4, Color.web("#10b981"));
        HBox bottom = new HBox(8, dot, subtitle);
        bottom.setAlignment(Pos.CENTER_LEFT);

        VBox item = new VBox(4, title, date, bottom);
        item.getStyleClass().add("chat-tile");
        item.setOnMouseClicked(e -> selectConversation(conversation));
        return item;
    }

    private int resolveSellerIdForProduct(Produit produit) {
        int sellerId = produit.getVendeurId() > 0 ? produit.getVendeurId() : fallbackSellerIdSupplier.getAsInt();
        if (sellerId == currentUserIdSupplier.getAsInt()) {
            return fallbackSellerIdSupplier.getAsInt();
        }
        return sellerId;
    }

    private String resolveProductDisplayName(int productId) {
        String name = state.productNameById.get(productId);
        return name == null || name.isBlank() ? "Produit non disponible" : name;
    }

    private String resolveUserDisplayName(int userId) {
        String name = state.userNameById.get(userId);
        return name == null || name.isBlank() ? "Vendeur inconnu" : name;
    }

    private String buildUserDisplayName(entities.User user) {
        String first = normalizeText.apply(safe(user.getFirstName()));
        String last = normalizeText.apply(safe(user.getLastName()));
        String fullName = (first + " " + last).trim();
        if (!fullName.isBlank()) {
            return fullName;
        }
        String email = normalizeText.apply(safe(user.getEmail()));
        if (!email.isBlank()) {
            return email;
        }
        return "Utilisateur";
    }

    private String buildConversationHeader(int productId, int sellerId) {
        return resolveProductDisplayName(productId) + " - vendeur: " + resolveUserDisplayName(sellerId);
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "-" : value.format(CHAT_TIME_FORMAT);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
