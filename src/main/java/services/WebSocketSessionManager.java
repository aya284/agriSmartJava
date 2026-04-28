package services;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages WebSocket sessions for real-time messaging
 * Tracks active user connections by user ID
 */
public class WebSocketSessionManager {
    private static final WebSocketSessionManager instance = new WebSocketSessionManager();
    
    // Map of userId -> Set of session IDs
    private final Map<Integer, Set<String>> userSessions = new ConcurrentHashMap<>();
    
    // Map of conversationId -> Set of user IDs connected to that conversation
    private final Map<Integer, Set<Integer>> conversationUsers = new ConcurrentHashMap<>();
    
    private WebSocketSessionManager() {}
    
    public static WebSocketSessionManager getInstance() {
        return instance;
    }
    
    /**
     * Register a user session
     * @param userId User ID
     * @param sessionId WebSocket session ID
     */
    public void registerSession(int userId, String sessionId) {
        userSessions.computeIfAbsent(userId, k -> new HashSet<>()).add(sessionId);
    }
    
    /**
     * Unregister a user session
     * @param userId User ID
     * @param sessionId WebSocket session ID
     */
    public void unregisterSession(int userId, String sessionId) {
        Set<String> sessions = userSessions.get(userId);
        if (sessions != null) {
            sessions.remove(sessionId);
            if (sessions.isEmpty()) {
                userSessions.remove(userId);
            }
        }
    }
    
    /**
     * Check if user is online
     * @param userId User ID
     * @return true if user has active sessions
     */
    public boolean isUserOnline(int userId) {
        Set<String> sessions = userSessions.get(userId);
        return sessions != null && !sessions.isEmpty();
    }
    
    /**
     * Get all session IDs for a user
     * @param userId User ID
     * @return Set of session IDs (empty if not online)
     */
    public Set<String> getUserSessions(int userId) {
        return new HashSet<>(userSessions.getOrDefault(userId, Collections.emptySet()));
    }
    
    /**
     * Register user for a conversation
     * @param conversationId Conversation ID
     * @param userId User ID
     */
    public void registerForConversation(int conversationId, int userId) {
        conversationUsers.computeIfAbsent(conversationId, k -> new HashSet<>()).add(userId);
    }
    
    /**
     * Unregister user from a conversation
     * @param conversationId Conversation ID
     * @param userId User ID
     */
    public void unregisterFromConversation(int conversationId, int userId) {
        Set<Integer> users = conversationUsers.get(conversationId);
        if (users != null) {
            users.remove(userId);
            if (users.isEmpty()) {
                conversationUsers.remove(conversationId);
            }
        }
    }
    
    /**
     * Get all users in a conversation
     * @param conversationId Conversation ID
     * @return Set of user IDs
     */
    public Set<Integer> getConversationUsers(int conversationId) {
        return new HashSet<>(conversationUsers.getOrDefault(conversationId, Collections.emptySet()));
    }
    
    /**
     * Get total online users
     * @return Number of unique online users
     */
    public int getOnlineUserCount() {
        return userSessions.size();
    }
    
    /**
     * Clear all sessions (for shutdown)
     */
    public void clearAll() {
        userSessions.clear();
        conversationUsers.clear();
    }
}
