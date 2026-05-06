package services;

import entities.MarketplaceMessage;
import org.json.JSONObject;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * WebSocket endpoint for real-time marketplace messaging
 * Handles connections, disconnections, and message broadcasts
 */
@ServerEndpoint("/api/messages/live")
public class WebSocketMarketplaceEndpoint {
    private static final Logger logger = Logger.getLogger(WebSocketMarketplaceEndpoint.class.getName());
    
    // Map of session ID -> user context (userId, conversationId)
    private static final Map<String, UserContext> userContexts = new ConcurrentHashMap<>();
    
    // Map of session ID -> session object
    private static final Map<String, Session> sessions = new ConcurrentHashMap<>();
    
    private static final WebSocketSessionManager sessionManager = WebSocketSessionManager.getInstance();
    private static final MarketplaceMessageService messageService = new MarketplaceMessageService();
    
    /**
     * Handles new WebSocket connections
     */
    @OnOpen
    public void onOpen(Session session, EndpointConfig config) {
        sessions.put(session.getId(), session);
        logger.info("WebSocket connection opened: " + session.getId());
    }
    
    /**
     * Handles incoming messages
     * Expected message format:
     * {
     *   "type": "auth" | "message" | "typing" | "read",
     *   "userId": <int>,
     *   "conversationId": <int>,
     *   "content": "<string>",
     *   "audioPath": "<string>" (optional)
     * }
     */
    @OnMessage
    public void onMessage(String messageStr, Session session) {
        try {
            JSONObject json = new JSONObject(messageStr);
            String type = json.optString("type", "");
            
            switch (type) {
                case "auth":
                    handleAuth(session, json);
                    break;
                case "message":
                    handleNewMessage(session, json);
                    break;
                case "typing":
                    handleTypingIndicator(session, json);
                    break;
                case "read":
                    handleMessageRead(session, json);
                    break;
                default:
                    logger.warning("Unknown message type: " + type);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error processing WebSocket message", e);
            try {
                sendErrorToClient(session, "Invalid message format");
            } catch (IOException ex) {
                logger.log(Level.SEVERE, "Failed to send error", ex);
            }
        }
    }
    
    /**
     * Authenticate user for WebSocket session
     */
    private void handleAuth(Session session, JSONObject json) throws IOException {
        int userId = json.optInt("userId", -1);
        int conversationId = json.optInt("conversationId", -1);
        
        if (userId <= 0) {
            sendErrorToClient(session, "Invalid user ID");
            return;
        }
        
        // Store user context
        UserContext context = new UserContext(userId, conversationId);
        userContexts.put(session.getId(), context);
        
        // Register session
        sessionManager.registerSession(userId, session.getId());
        if (conversationId > 0) {
            sessionManager.registerForConversation(conversationId, userId);
        }
        
        // Send confirmation
        JSONObject response = new JSONObject();
        response.put("type", "auth_success");
        response.put("userId", userId);
        response.put("conversationId", conversationId);
        
        session.getBasicRemote().sendText(response.toString());
        logger.info("User " + userId + " authenticated on WebSocket");
    }
    
    /**
     * Handle new message
     */
    private void handleNewMessage(Session session, JSONObject json) throws IOException {
        UserContext context = userContexts.get(session.getId());
        if (context == null) {
            sendErrorToClient(session, "Not authenticated");
            return;
        }
        
        int conversationId = json.optInt("conversationId", context.conversationId);
        int senderId = context.userId;
        String content = json.optString("content", "").trim();
        String audioPath = json.optString("audioPath", "");
        boolean hasAudio = audioPath != null && !audioPath.isBlank();
        if (!hasAudio && content.isEmpty()) {
            sendErrorToClient(session, "Message content cannot be empty");
            return;
        }
        if (hasAudio && content.isEmpty()) {
            content = "Message vocal";
        }
        
        try {
            // Save message to database
            MarketplaceMessage message = new MarketplaceMessage();
            message.setConversationId(conversationId);
            message.setSenderId(senderId);
            message.setContent(content);
            message.setAudioPath(audioPath);
            message.setRead(false);
            
            messageService.ajouter(message);
            new MarketplaceConversationService().touchLastMessage(conversationId);
            
            // Broadcast to all users in the conversation
            broadcastToConversation(conversationId, message);
            
            logger.info("Message from user " + senderId + " saved to conversation " + conversationId);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error saving message", e);
            sendErrorToClient(session, "Failed to save message");
        }
    }
    
    /**
     * Handle typing indicator
     */
    private void handleTypingIndicator(Session session, JSONObject json) throws IOException {
        UserContext context = userContexts.get(session.getId());
        if (context == null) return;
        
        int conversationId = json.optInt("conversationId", context.conversationId);
        int userId = context.userId;
        boolean isTyping = json.optBoolean("isTyping", false);
        
        JSONObject notification = new JSONObject();
        notification.put("type", "typing");
        notification.put("conversationId", conversationId);
        notification.put("userId", userId);
        notification.put("isTyping", isTyping);
        
        broadcastToConversationExcluding(conversationId, session.getId(), notification.toString());
    }
    
    /**
     * Handle message read receipt
     */
    private void handleMessageRead(Session session, JSONObject json) {
        UserContext context = userContexts.get(session.getId());
        if (context == null) return;
        
        int conversationId = json.optInt("conversationId", context.conversationId);
        int readerId = context.userId;
        
        try {
            messageService.markConversationAsRead(conversationId, readerId);
            
            JSONObject notification = new JSONObject();
            notification.put("type", "conversation_read");
            notification.put("conversationId", conversationId);
            notification.put("readBy", readerId);
            
            broadcastToConversation(conversationId, notification.toString());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error marking message as read", e);
        }
    }
    
    /**
     * Broadcast message to all users in a conversation
     */
    private void broadcastToConversation(int conversationId, MarketplaceMessage message) throws IOException {
        JSONObject json = messageToJson(message);
        json.put("type", "message");
        broadcastToConversation(conversationId, json.toString());
    }
    
    /**
     * Broadcast to all users in a conversation
     */
    private void broadcastToConversation(int conversationId, String messageStr) {
        Set<Integer> users = sessionManager.getConversationUsers(conversationId);
        for (int userId : users) {
            broadcastToUser(userId, messageStr);
        }
    }
    
    /**
     * Broadcast to all users in a conversation excluding a session
     */
    private void broadcastToConversationExcluding(int conversationId, String excludeSessionId, String messageStr) {
        Set<Integer> users = sessionManager.getConversationUsers(conversationId);
        for (int userId : users) {
            Set<String> userSessions = sessionManager.getUserSessions(userId);
            for (String sessionId : userSessions) {
                if (!sessionId.equals(excludeSessionId)) {
                    Session session = sessions.get(sessionId);
                    if (session != null && session.isOpen()) {
                        try {
                            session.getBasicRemote().sendText(messageStr);
                        } catch (IOException e) {
                            logger.log(Level.WARNING, "Failed to send message to session " + sessionId, e);
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Broadcast to a specific user
     */
    private void broadcastToUser(int userId, String messageStr) {
        Set<String> userSessions = sessionManager.getUserSessions(userId);
        for (String sessionId : userSessions) {
            Session session = sessions.get(sessionId);
            if (session != null && session.isOpen()) {
                try {
                    session.getBasicRemote().sendText(messageStr);
                } catch (IOException e) {
                    logger.log(Level.WARNING, "Failed to send message to user " + userId, e);
                }
            }
        }
    }
    
    /**
     * Send error to client
     */
    private void sendErrorToClient(Session session, String error) throws IOException {
        JSONObject response = new JSONObject();
        response.put("type", "error");
        response.put("message", error);
        session.getBasicRemote().sendText(response.toString());
    }
    
    /**
     * Convert MarketplaceMessage to JSON
     */
    private JSONObject messageToJson(MarketplaceMessage message) {
        JSONObject json = new JSONObject();
        json.put("id", message.getId());
        json.put("conversationId", message.getConversationId());
        json.put("senderId", message.getSenderId());
        json.put("content", message.getContent());
        json.put("createdAt", message.getCreatedAt() != null ? message.getCreatedAt().toString() : "");
        json.put("isRead", message.isRead());
        json.put("audioPath", message.getAudioPath() != null ? message.getAudioPath() : "");
        return json;
    }
    
    /**
     * Handles WebSocket disconnection
     */
    @OnClose
    public void onClose(Session session) {
        UserContext context = userContexts.remove(session.getId());
        sessions.remove(session.getId());
        
        if (context != null) {
            sessionManager.unregisterSession(context.userId, session.getId());
            if (context.conversationId > 0) {
                sessionManager.unregisterFromConversation(context.conversationId, context.userId);
            }
            logger.info("WebSocket connection closed for user " + context.userId);
        }
    }
    
    /**
     * Handles WebSocket errors
     */
    @OnError
    public void onError(Session session, Throwable error) {
        logger.log(Level.SEVERE, "WebSocket error for session " + (session != null ? session.getId() : "unknown"), error);
        if (session != null && session.isOpen()) {
            try {
                session.close();
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to close session", e);
            }
        }
    }
    
    /**
     * Inner class to store user context
     */
    private static class UserContext {
        int userId;
        int conversationId;
        
        UserContext(int userId, int conversationId) {
            this.userId = userId;
            this.conversationId = conversationId;
        }
    }
}
