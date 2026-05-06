package services;

import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Embedded WebSocket server for marketplace real-time messaging
 * Starts automatically when the application starts
 */
public class WebSocketServer {
    private static final Logger logger = Logger.getLogger(WebSocketServer.class.getName());
    private static final String WEBSOCKET_HOST = "localhost";
    private static final int WEBSOCKET_PORT = 8888;
    
    private static WebSocketServer instance;
    private Object server;
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
            Class<?> serverClass = Class.forName("org.glassfish.tyrus.server.Server");
            server = serverClass
                    .getConstructor(String.class, int.class, String.class, java.util.Map.class, Class[].class)
                    .newInstance(WEBSOCKET_HOST, WEBSOCKET_PORT, "/ws", null, new Class<?>[] { WebSocketMarketplaceEndpoint.class });
            
            // Start server in a separate thread
            new Thread(() -> {
                try {
                    logger.info("Starting WebSocket server on ws://" + WEBSOCKET_HOST + ":" + WEBSOCKET_PORT + "/ws");
                    Method startMethod = server.getClass().getMethod("start");
                    startMethod.invoke(server);
                    isRunning = true;
                    logger.info("WebSocket server started successfully");
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "WebSocket server failed to start", e);
                    isRunning = false;
                }
            }, "WebSocketServerThread").start();
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to initialize WebSocket server", e);
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
            Method stopMethod = server.getClass().getMethod("stop");
            stopMethod.invoke(server);
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
