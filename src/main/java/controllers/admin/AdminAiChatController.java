package controllers.admin;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;
import services.AdminAiService;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class AdminAiChatController {

    @FXML private ScrollPane chatScrollPane;
    @FXML private VBox chatContainer;
    @FXML private TextField queryField;
    @FXML private Button sendBtn;

    private final AdminAiService adminAiService = new AdminAiService();
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    // Holds the typing indicator so we can remove it after the response arrives
    private HBox typingIndicatorNode;

    @FXML
    public void initialize() {
        // Auto-scroll to bottom whenever the container grows
        chatContainer.heightProperty().addListener((obs, o, n) ->
                chatScrollPane.setVvalue(1.0));
    }

    // ── Send ────────────────────────────────────────────────────

    @FXML
    private void handleSend() {
        String query = queryField.getText().trim();
        if (query.isBlank()) return;
        dispatch(query);
    }

    /**
     * Suggestion chips inside the welcome bubble fill-and-fire.
     * The chip's userData holds the pre-written query string.
     */
    @FXML
    private void handleSuggestion(ActionEvent event) {
        if (event.getSource() instanceof Button btn) {
            String query = (String) btn.getUserData();
            if (query != null && !query.isBlank()) {
                dispatch(query);
            }
        }
    }

    // ── Core Dispatch ───────────────────────────────────────────

    private void dispatch(String query) {
        addUserMessage(query);
        queryField.clear();
        setLoading(true);
        showTypingIndicator();

        new Thread(() -> {
            try {
                String response = adminAiService.handleAdminQuery(query);
                Platform.runLater(() -> {
                    removeTypingIndicator();
                    addAiMessage(response);
                    setLoading(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    removeTypingIndicator();
                    addAiMessage("⚠️ Une erreur est survenue : " + e.getMessage());
                    setLoading(false);
                });
            }
        }).start();
    }

    // ── Message Builders ────────────────────────────────────────

    private void addUserMessage(String text) {
        // Spacer pushes the bubble to the right
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Sender info (right-aligned)
        HBox meta = new HBox(8);
        meta.setAlignment(Pos.CENTER_RIGHT);
        Label name = new Label("Vous");
        name.getStyleClass().add("sender-name");
        Label time = new Label(now());
        time.getStyleClass().add("msg-time");
        meta.getChildren().addAll(time, name);

        // Bubble
        VBox bubble = new VBox();
        bubble.getStyleClass().add("user-bubble");
        bubble.setMaxWidth(520);
        Label msg = new Label(text);
        msg.setWrapText(true);
        msg.getStyleClass().add("user-msg-text");
        bubble.getChildren().add(msg);

        VBox content = new VBox(6, meta, bubble);
        content.setAlignment(Pos.TOP_RIGHT);

        // User avatar
        VBox avatar = new VBox();
        avatar.getStyleClass().add("user-avatar");
        avatar.setAlignment(Pos.CENTER);
        avatar.setMinWidth(36); avatar.setMaxWidth(36);
        avatar.setMinHeight(36); avatar.setMaxHeight(36);
        Label avatarLabel = new Label("A");
        avatarLabel.getStyleClass().add("user-avatar-label");
        avatar.getChildren().add(avatarLabel);

        HBox row = new HBox(12, spacer, content, avatar);
        row.setAlignment(Pos.TOP_RIGHT);
        chatContainer.getChildren().add(row);
    }

    private void addAiMessage(String text) {
        // AI avatar
        VBox avatar = new VBox();
        avatar.getStyleClass().add("ai-avatar");
        avatar.setAlignment(Pos.CENTER);
        avatar.setMinWidth(36); avatar.setMaxWidth(36);
        avatar.setMinHeight(36); avatar.setMaxHeight(36);
        Label avatarLabel = new Label("AI");
        avatarLabel.getStyleClass().add("ai-avatar-label");
        avatar.getChildren().add(avatarLabel);

        // Meta
        HBox meta = new HBox(8);
        meta.setAlignment(Pos.CENTER_LEFT);
        Label name = new Label("Assistant AgriSmart");
        name.getStyleClass().add("sender-name");
        Label time = new Label(now());
        time.getStyleClass().add("msg-time");
        meta.getChildren().addAll(name, time);

        // Bubble
        VBox bubble = new VBox();
        bubble.getStyleClass().add("ai-bubble");
        bubble.setMaxWidth(560);
        Label msg = new Label(text);
        msg.setWrapText(true);
        msg.getStyleClass().add("msg-text");
        bubble.getChildren().add(msg);

        VBox content = new VBox(6, meta, bubble);
        content.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(content, Priority.ALWAYS);

        HBox row = new HBox(12, avatar, content);
        row.setAlignment(Pos.TOP_LEFT);
        chatContainer.getChildren().add(row);
    }

    // ── Typing Indicator ────────────────────────────────────────

    private void showTypingIndicator() {
        VBox avatar = new VBox();
        avatar.getStyleClass().add("ai-avatar");
        avatar.setAlignment(Pos.CENTER);
        avatar.setMinWidth(36); avatar.setMaxWidth(36);
        avatar.setMinHeight(36); avatar.setMaxHeight(36);
        Label avatarLabel = new Label("AI");
        avatarLabel.getStyleClass().add("ai-avatar-label");
        avatar.getChildren().add(avatarLabel);

        VBox bubble = new VBox();
        bubble.getStyleClass().add("typing-bubble");
        Label dots = new Label("Analyse en cours…");
        dots.getStyleClass().add("typing-text");
        bubble.getChildren().add(dots);

        typingIndicatorNode = new HBox(12, avatar, bubble);
        typingIndicatorNode.setAlignment(Pos.TOP_LEFT);
        chatContainer.getChildren().add(typingIndicatorNode);
    }

    private void removeTypingIndicator() {
        if (typingIndicatorNode != null) {
            chatContainer.getChildren().remove(typingIndicatorNode);
            typingIndicatorNode = null;
        }
    }

    // ── Loading State ────────────────────────────────────────────

    private void setLoading(boolean loading) {
        sendBtn.setDisable(loading);
        queryField.setDisable(loading);
        sendBtn.setText(loading ? "…" : "Envoyer →");
    }

    // ── Helpers ──────────────────────────────────────────────────

    private String now() {
        return LocalTime.now().format(TIME_FMT);
    }
}