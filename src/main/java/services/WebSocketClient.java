package services;

import org.glassfish.tyrus.client.ClientManager;
import org.json.JSONObject;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.Session;
import java.net.URI;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * WebSocket client for JavaFX marketplace messaging
 * Handles connection to WebSocket server and message handling
 */
public class WebSocketClient {
    private static final Logger logger = Logger.getLogger(WebSocketClient.class.getName());
    
    private Session session;
    private int userId;
    private int conversationId;
    private boolean connected = false;
    
    // Listeners for incoming messages
    private final List<MessageListener> messageListeners = new ArrayList<>();
    private final List<TypingListener> typingListeners = new ArrayList<>();
    private final List<ErrorListener> errorListeners = new ArrayList<>();
    
    private final Object lock = new Object();
    
    /**
     * Connect to WebSocket server
     */
    public void connect(String wsUrl, int userId, int conversationId, CountDownLatch latch) {
        this.userId = userId;
        this.conversationId = conversationId;
        
        new Thread(() -> {
            try {
                ClientManager client = ClientManager.createClient();
                ClientEndpointConfig config = ClientEndpointConfig.Builder.create().build();
                
                // Create endpoint instance with callback handlers
                WebSocketClientEndpoint endpoint = new WebSocketClientEndpoint(
                        this::onMessageReceived,
                        this::onConnectionOpen,
                        this::onConnectionClose,
                        this::onError
                );
                
                session = client.connectToServer(endpoint, config, new URI(wsUrl));
                connected = true;
                
                // Authenticate with server
                authenticate();
                
                if (latch != null) {
                    latch.countDown();
                }
                
                logger.info("Connected to WebSocket server: " + wsUrl);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to connect to WebSocket server", e);
                connected = false;
                notifyError("Connection failed: " + e.getMessage());
                if (latch != null) {
                    latch.countDown();
                }
            }
        }, "WebSocketClientThread").start();
    }
    
    /**
     * Authenticate with WebSocket server
     */
    private void authenticate() {
        JSONObject auth = new JSONObject();
        auth.put("type", "auth");
        auth.put("userId", userId);
        auth.put("conversationId", conversationId);
        
        sendMessage(auth.toString());
    }
    
    /**
     * Send message to server
     */
    public void sendMessage(String content, String audioPath) {
        JSONObject msg = new JSONObject();
        msg.put("type", "message");
        msg.put("conversationId", conversationId);
        msg.put("content", content);
        if (audioPath != null && !audioPath.isEmpty()) {
            msg.put("audioPath", audioPath);
        }
        
        sendMessage(msg.toString());
    }
    
    /**
     * Send raw message
     */
    private void sendMessage(String messageStr) {
        if (!connected || session == null || !session.isOpen()) {
            logger.warning("Not connected to WebSocket server");
            notifyError("WebSocket not connected");
            return;
        }
        
        try {
            synchronized (lock) {
                session.getBasicRemote().sendText(messageStr);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to send message", e);
            notifyError("Failed to send message: " + e.getMessage());
        }
    }
    
    /**
     * Send typing indicator
     */
    public void sendTypingIndicator(boolean isTyping) {
        JSONObject msg = new JSONObject();
        msg.put("type", "typing");
        msg.put("conversationId", conversationId);
        msg.put("isTyping", isTyping);
        
        sendMessage(msg.toString());
    }
    
    /**
     * Mark message as read
     */
    public void markConversationAsRead() {
        JSONObject msg = new JSONObject();
        msg.put("type", "read");
        msg.put("conversationId", conversationId);
        
        sendMessage(msg.toString());
    }
    
    /**
     * Disconnect from server
     */
    public void disconnect() {
        if (session != null && session.isOpen()) {
            try {
                session.close();
                connected = false;
                logger.info("Disconnected from WebSocket server");
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error closing WebSocket connection", e);
            }
        }
    }
    
    /**
     * Check if connected
     */
    public boolean isConnected() {
        return connected && session != null && session.isOpen();
    }
    
    /**
     * Handle incoming message from server
     */
    private void onMessageReceived(String messageStr) {
        try {
            JSONObject json = new JSONObject(messageStr);
            String type = json.optString("type", "");
            
            switch (type) {
                case "message":
                    notifyMessageReceived(json);
                    break;
                case "typing":
                    notifyTyping(json);
                    break;
                case "conversation_read":
                    notifyConversationRead(json);
                    break;
                case "error":
                    notifyError(json.optString("message", "Unknown error"));
                    break;
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error processing incoming message", e);
        }
    }
    
    /**
     * Handle connection open
     */
    private void onConnectionOpen() {
        connected = true;
        logger.info("WebSocket connection opened");
    }
    
    /**
     * Handle connection close
     */
    private void onConnectionClose() {
        connected = false;
        logger.info("WebSocket connection closed");
    }
    
    /**
     * Handle error
     */
    private void onError(String error) {
        logger.warning("WebSocket error: " + error);
        notifyError(error);
    }
    
    // Listener Management
    
    public interface MessageListener {
        void onMessageReceived(JSONObject message);
    }
    
    public interface TypingListener {
        void onTyping(int userId, boolean isTyping);
    }
    
    public interface ErrorListener {
        void onError(String error);
    }
    
    public void addMessageListener(MessageListener listener) {
        messageListeners.add(listener);
    }
    
    public void removeMessageListener(MessageListener listener) {
        messageListeners.remove(listener);
    }
    
    public void addTypingListener(TypingListener listener) {
        typingListeners.add(listener);
    }
    
    public void removeTypingListener(TypingListener listener) {
        typingListeners.remove(listener);
    }
    
    public void addErrorListener(ErrorListener listener) {
        errorListeners.add(listener);
    }
    
    public void removeErrorListener(ErrorListener listener) {
        errorListeners.remove(listener);
    }
    
    private void notifyMessageReceived(JSONObject message) {
        for (MessageListener listener : messageListeners) {
            try {
                listener.onMessageReceived(message);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error in message listener", e);
            }
        }
    }
    
    private void notifyTyping(JSONObject json) {
        int userId = json.optInt("userId", -1);
        boolean isTyping = json.optBoolean("isTyping", false);
        
        for (TypingListener listener : typingListeners) {
            try {
                listener.onTyping(userId, isTyping);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error in typing listener", e);
            }
        }
    }
    
    private void notifyConversationRead(JSONObject json) {
        JSONObject msgObj = new JSONObject();
        msgObj.put("type", "conversation_read");
        msgObj.putOpt("readBy", json.optInt("readBy", -1));
        
        for (MessageListener listener : messageListeners) {
            try {
                listener.onMessageReceived(msgObj);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error in message listener", e);
            }
        }
    }
    
    private void notifyError(String error) {
        for (ErrorListener listener : errorListeners) {
            try {
                listener.onError(error);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error in error listener", e);
            }
        }
    }
}
