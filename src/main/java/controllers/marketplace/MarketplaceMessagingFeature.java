package controllers.marketplace;

import entities.MarketplaceMessage;
import entities.Produit;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import services.MarketplaceConversationService;
import services.MarketplaceMessageService;
import services.ProduitService;
import services.UserService;
import utils.SessionManager;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public class MarketplaceMessagingFeature {
    private static final DateTimeFormatter CHAT_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM HH:mm");

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
        clearPendingMessageContext();
        loadConversationsAndRender(null);
        animateOverlayIn.accept(messagingOverlay);
    }

    public void closeMessagingCenter() {
        if (messagingOverlay == null) {
            return;
        }
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
                Label body = new Label(normalizeText.apply(safe(message.getContent())));
                body.setWrapText(true);
                body.getStyleClass().add(mine ? "chat-bubble-me" : "chat-bubble-them");
                body.setMaxWidth(460);

                String senderDisplay = mine ? "Vous" : resolveUserDisplayName(message.getSenderId());
                Label meta = new Label(senderDisplay + " - " + formatDateTime(message.getCreatedAt()));
                meta.getStyleClass().add("chat-bubble-meta");

                VBox bubble = new VBox(4, body, meta);
                bubble.setFillWidth(false);

                HBox row = new HBox(bubble);
                row.setAlignment(mine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
                row.getStyleClass().add("chat-row");

                messageListBox.getChildren().add(row);
            }
        } catch (SQLException e) {
            showSqlAlert.accept(e);
        }
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
