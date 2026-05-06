package services;

import javax.websocket.*;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * WebSocket client endpoint for receiving messages
 * Callbacks are invoked when WebSocket events occur
 */
public class WebSocketClientEndpoint extends Endpoint {
    private static final Logger logger = Logger.getLogger(WebSocketClientEndpoint.class.getName());
    
    private final Consumer<String> onMessageReceived;
    private final Runnable onConnectionOpen;
    private final Runnable onConnectionClose;
    private final Consumer<String> onError;
    
    public WebSocketClientEndpoint(
            Consumer<String> onMessageReceived,
            Runnable onConnectionOpen,
            Runnable onConnectionClose,
            Consumer<String> onError
    ) {
        this.onMessageReceived = onMessageReceived;
        this.onConnectionOpen = onConnectionOpen;
        this.onConnectionClose = onConnectionClose;
        this.onError = onError;
    }
    
    @Override
    public void onOpen(Session session, EndpointConfig endpointConfig) {
        logger.info("WebSocket client connected: " + session.getId());
        
        // Add message handler
        session.addMessageHandler(new MessageHandler.Whole<String>() {
            @Override
            public void onMessage(String message) {
                try {
                    if (onMessageReceived != null) {
                        onMessageReceived.accept(message);
                    }
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Error in message handler", e);
                }
            }
        });
        
        // Notify connection open
        if (onConnectionOpen != null) {
            try {
                onConnectionOpen.run();
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error in connection open handler", e);
            }
        }
    }
    
    @Override
    public void onClose(Session session, CloseReason closeReason) {
        logger.info("WebSocket client disconnected: " + session.getId() + " - " + closeReason.getReasonPhrase());
        
        if (onConnectionClose != null) {
            try {
                onConnectionClose.run();
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error in connection close handler", e);
            }
        }
    }
    
    @Override
    public void onError(Session session, Throwable error) {
        logger.log(Level.SEVERE, "WebSocket client error: " + session.getId(), error);
        
        if (onError != null) {
            try {
                onError.accept(error.getMessage());
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error in error handler", e);
            }
        }
    }
}
