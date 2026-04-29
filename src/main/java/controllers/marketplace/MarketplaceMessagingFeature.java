package controllers.marketplace;

import entities.MarketplaceMessage;
import entities.Produit;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import services.MarketplaceConversationService;
import services.MarketplaceMessageService;
import services.ProduitService;
import services.UserService;
import services.WebSocketClient;
import services.WebSocketServer;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.net.URI;
import java.awt.Desktop;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

public class MarketplaceMessagingFeature {
    private static final DateTimeFormatter CHAT_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM HH:mm");
    private static final String CALL_INVITE_PREFIX = "CALL_INVITE|";
    private static final String DEFAULT_JITSI_BASE_URL = "https://meet.jit.si/";

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
    private final Consumer<StackPane> animateOverlayIn;
    private final Consumer<StackPane> animateOverlayOut;
    private final IntSupplier currentUserIdSupplier;
    private final IntSupplier fallbackSellerIdSupplier;
    private final BiConsumer<String, String> showAlert;
    private final Consumer<SQLException> showSqlAlert;
    private final BiConsumer<String, Boolean> showToast;
    private final java.util.function.Function<String, String> normalizeText;
    private final WebSocketClient webSocketClient = new WebSocketClient();
    private int websocketConversationId = -1;

    /** Full-screen overlay on top of the messaging dialog; hosts embedded Jitsi (WebView). */
    private StackPane jitsiEmbeddedOverlayRoot;
    private WebEngine jitsiEmbeddedEngine;
    private String lastEmbeddedJitsiUrl;

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
        closeEmbeddedJitsiCall();
        clearPendingMessageContext();
        loadConversationsAndRender(null);
        animateOverlayIn.accept(messagingOverlay);
    }

    public void closeMessagingCenter() {
        if (messagingOverlay == null) {
            return;
        }
        closeEmbeddedJitsiCall();
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

    public void sendCallInvite() {
        try {
            if (state.selectedConversation == null) {
                if (!state.hasPendingMessageContext()) {
                    showAlert.accept("Appel vocal", "Choisissez une conversation ou contactez un vendeur depuis la fiche produit.");
                    return;
                }
                state.selectedConversation = conversationService.findOrCreateConversation(
                        state.pendingProductId,
                        currentUserIdSupplier.getAsInt(),
                        state.pendingSellerId
                );
            }

            connectRealtimeIfNeeded(state.selectedConversation);

            String roomName = buildJitsiRoomName(state.selectedConversation.getId());
            String roomUrl = DEFAULT_JITSI_BASE_URL + roomName;
            String payload = CALL_INVITE_PREFIX + roomUrl + "|" + roomName;

            if (webSocketClient.isConnected()) {
                webSocketClient.sendMessage(payload, "");
                refreshMessagingBadge();
                clearPendingMessageContext();
                showToast.accept("Invitation d'appel envoyee.", true);
                openEmbeddedJitsiCall(roomUrl);
                return;
            }

            MarketplaceMessage message = new MarketplaceMessage();
            message.setConversationId(state.selectedConversation.getId());
            message.setSenderId(currentUserIdSupplier.getAsInt());
            message.setContent(payload);
            message.setRead(false);
            messageService.ajouter(message);
            conversationService.touchLastMessage(state.selectedConversation.getId());

            renderMessages(state.selectedConversation);
            loadConversationsAndRender(state.selectedConversation.getId());
            refreshMessagingBadge();
            clearPendingMessageContext();
            showToast.accept("Invitation d'appel envoyee.", true);
            openEmbeddedJitsiCall(roomUrl);
        } catch (SQLException e) {
            showSqlAlert.accept(e);
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
                VBox bubble;
                if (isCallInviteMessage(rawContent)) {
                    bubble = buildCallInviteBubble(rawContent, mine);
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

    private boolean isCallInviteMessage(String content) {
        return content != null && content.startsWith(CALL_INVITE_PREFIX);
    }

    private VBox buildCallInviteBubble(String rawContent, boolean mine) {
        CallInvite invite = parseCallInvite(rawContent);

        Label title = new Label("Appel vocal");
        title.setStyle("-fx-font-weight: 800;");

        Label subtitle = new Label(invite.roomName == null || invite.roomName.isBlank() ? "Invitation d'appel" : invite.roomName);
        subtitle.setWrapText(true);
        subtitle.setStyle("-fx-opacity: 0.9;");

        Button join = new Button("Rejoindre dans l'app");
        join.getStyleClass().add(mine ? "chat-send-btn" : "action-chip");
        join.setOnAction(e -> openEmbeddedJitsiCall(invite.roomUrl));

        Button browser = new Button("Navigateur");
        browser.getStyleClass().add("action-chip");
        browser.setOnAction(e -> openBrowserForCall(invite.roomUrl));

        Button copy = new Button("Copier lien");
        copy.getStyleClass().add("action-chip");
        copy.setOnAction(e -> copyToClipboard(invite.roomUrl));

        HBox actions = new HBox(8, join, browser, copy);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(8, title, subtitle, actions);
        card.setMaxWidth(460);
        card.setStyle(
                "-fx-padding: 12; " +
                "-fx-background-radius: 12; " +
                "-fx-border-radius: 12; " +
                "-fx-border-color: rgba(148,163,184,0.55); " +
                "-fx-background-color: " + (mine ? "rgba(59,130,246,0.12)" : "rgba(15,23,42,0.04)") + ";"
        );
        return card;
    }

    private CallInvite parseCallInvite(String rawContent) {
        // Format: CALL_INVITE|<url>|<roomName>
        String payload = rawContent == null ? "" : rawContent;
        String rest = payload.startsWith(CALL_INVITE_PREFIX) ? payload.substring(CALL_INVITE_PREFIX.length()) : payload;
        String[] parts = rest.split("\\|", 2);
        String url = parts.length >= 1 ? parts[0] : "";
        String room = parts.length == 2 ? parts[1] : "";
        CallInvite invite = new CallInvite();
        invite.roomUrl = url;
        invite.roomName = room;
        return invite;
    }

    private void openEmbeddedJitsiCall(String url) {
        String safeUrl = safe(url).trim();
        if (safeUrl.isEmpty()) {
            showToast.accept("Lien d'appel invalide.", false);
            return;
        }

        ensureJitsiEmbeddedOverlayUi();
        if (jitsiEmbeddedEngine == null || jitsiEmbeddedOverlayRoot == null) {
            copyToClipboard(safeUrl);
            showToast.accept("Embedded call UI indisponible. Lien copie.", false);
            return;
        }

        lastEmbeddedJitsiUrl = safeUrl;
        jitsiEmbeddedEngine.load(safeUrl);
        jitsiEmbeddedOverlayRoot.setManaged(true);
        jitsiEmbeddedOverlayRoot.setVisible(true);
        jitsiEmbeddedOverlayRoot.toFront();
    }

    private void closeEmbeddedJitsiCall() {
        lastEmbeddedJitsiUrl = null;
        if (jitsiEmbeddedEngine != null) {
            try {
                jitsiEmbeddedEngine.load("about:blank");
            } catch (Exception ignored) {
            }
        }
        if (jitsiEmbeddedOverlayRoot != null) {
            jitsiEmbeddedOverlayRoot.setManaged(false);
            jitsiEmbeddedOverlayRoot.setVisible(false);
        }
    }

    private void ensureJitsiEmbeddedOverlayUi() {
        if (messagingOverlay == null || jitsiEmbeddedOverlayRoot != null) {
            return;
        }

        StackPane backdrop = new StackPane();
        backdrop.setPickOnBounds(true);
        StackPane.setAlignment(backdrop, Pos.CENTER);

        BorderPane shell = new BorderPane();
        shell.setStyle("-fx-background-color: white; -fx-background-radius: 12;");
        shell.setPrefSize(860, 580);
        shell.setMaxWidth(960);
        shell.setMaxHeight(720);

        Label titleLbl = new Label("Appel vocal");
        titleLbl.setStyle("-fx-font-size: 16px; -fx-font-weight: 700;");

        Button closeBtn = new Button("Fermer");
        closeBtn.getStyleClass().add("publish-header-btn");
        closeBtn.setOnAction(e -> closeEmbeddedJitsiCall());

        Button browserBtn = new Button("Navigateur");
        browserBtn.getStyleClass().add("action-chip");
        browserBtn.setOnAction(e -> {
            String u = lastEmbeddedJitsiUrl;
            if (u == null || u.isBlank()) {
                showToast.accept("Aucun lien disponible.", false);
                return;
            }
            openBrowserForCall(u);
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(10, titleLbl, spacer, browserBtn, closeBtn);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(12, 14, 8, 14));

        WebView webView = new WebView();
        WebEngine engine = webView.getEngine();
        webView.setPrefSize(860, 520);

        BorderPane.setMargin(webView, new Insets(0, 14, 14, 14));
        shell.setTop(header);
        shell.setCenter(webView);

        backdrop.getChildren().add(shell);
        StackPane.setAlignment(shell, Pos.CENTER);

        jitsiEmbeddedOverlayRoot = backdrop;
        jitsiEmbeddedEngine = engine;
        jitsiEmbeddedOverlayRoot.setVisible(false);
        jitsiEmbeddedOverlayRoot.setManaged(false);

        messagingOverlay.getChildren().add(jitsiEmbeddedOverlayRoot);
    }

    private void openBrowserForCall(String url) {
        String safeUrl = safe(url).trim();
        if (safeUrl.isEmpty()) {
            showToast.accept("Lien d'appel invalide.", false);
            return;
        }

        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(safeUrl));
                return;
            }
        } catch (Exception ignored) {
            // fallthrough to clipboard copy
        }

        copyToClipboard(safeUrl);
        showToast.accept("Lien copie. Ouvrez-le dans votre navigateur.", true);
    }

    private void copyToClipboard(String text) {
        String value = safe(text);
        ClipboardContent content = new ClipboardContent();
        content.putString(value);
        Clipboard.getSystemClipboard().setContent(content);
        showToast.accept("Lien copie.", true);
    }

    private String buildJitsiRoomName(int conversationId) {
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        return "agriSmart-" + conversationId + "-" + random;
    }

    private static class CallInvite {
        String roomUrl;
        String roomName;
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
