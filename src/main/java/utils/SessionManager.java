package utils;

import entities.User;

public class SessionManager {
    private static SessionManager instance;
    private User currentUser;

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) instance = new SessionManager();
        return instance;
    }

    public void setCurrentUser(User user) { this.currentUser = user; }
    public User getCurrentUser() { return currentUser; }
    public boolean isLoggedIn() { return currentUser != null; }
    public void logout() { currentUser = null; }

    public boolean isAdmin()        { return hasRole("admin"); }
    public boolean isEmployee()     { return hasRole("employee"); }
    public boolean isAgriculteur()  { return hasRole("agriculteur"); }
    public boolean isFournisseur()  { return hasRole("fournisseur"); }

    private boolean hasRole(String role) {
        return currentUser != null && role.equalsIgnoreCase(currentUser.getRole());
    }
}