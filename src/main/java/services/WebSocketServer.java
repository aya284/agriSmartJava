package services;

import org.glassfish.tyrus.server.Server;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Embedded WebSocket server for marketplace real-time messaging
 * Starts automatically when the application starts
 */
public class WebSocketServer {
    private static final Logger logger = Logger.getLogger(WebSocketServer.class.getName());
    private static final String WEBSOCKET_HOST = "localhost";
    private static final int WEBSOCKET_PORT = 8080;
    
    private static WebSocketServer instance;
    private Server server;
    private boolean isRunning = false;
    
    private WebSocketServer() {}
    
    /**
     * Get singleton instance
     */
    public static synchronized WebSocketServer getInstance() {
        if (instance == null) {
            instance = new WebSocketServer();
        }
        return instance;
    }
    
    /**
     * Start the WebSocket server
     */
    public synchronized void start() {
        if (isRunning) {
            logger.info("WebSocket server is already running");
            return;
        }
        
        try {
            // Create Tyrus server
            server = new Server(
                    WEBSOCKET_HOST,
                    WEBSOCKET_PORT,
                    "/ws",
                    null,
                    WebSocketMarketplaceEndpoint.class
            );
            
            // Start server in a separate thread
            new Thread(() -> {
                try {
                    logger.info("Starting WebSocket server on ws://" + WEBSOCKET_HOST + ":" + WEBSOCKET_PORT + "/ws");
                    server.start();
                    isRunning = true;
                    logger.info("WebSocket server started successfully");
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Failed to start WebSocket server", e);
                    isRunning = false;
                }
            }, "WebSocketServerThread").start();
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error creating WebSocket server", e);
            isRunning = false;
        }
    }
    
    /**
     * Stop the WebSocket server
     */
    public synchronized void stop() {
        if (!isRunning || server == null) {
            return;
        }
        
        try {
            server.stop();
            isRunning = false;
            logger.info("WebSocket server stopped");
            
            // Clear session manager
            WebSocketSessionManager.getInstance().clearAll();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error stopping WebSocket server", e);
        }
    }
    
    /**
     * Check if server is running
     */
    public boolean isRunning() {
        return isRunning;
    }
    
    /**
     * Get WebSocket connection URL
     */
    public String getConnectionUrl() {
        return "ws://" + WEBSOCKET_HOST + ":" + WEBSOCKET_PORT + "/ws/api/messages/live";
    }
}
