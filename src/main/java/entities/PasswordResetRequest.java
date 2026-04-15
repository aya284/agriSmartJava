package entities;

import java.time.LocalDateTime;

public class PasswordResetRequest {
    private int           id;
    private String        selector;
    private String        hashedToken;
    private LocalDateTime requestedAt;
    private LocalDateTime expiresAt;
    private int           userId;

    // ── Constructors ──────────────────────────────────────────
    public PasswordResetRequest() {}

    public PasswordResetRequest(String selector, String hashedToken,
                                LocalDateTime requestedAt, LocalDateTime expiresAt,
                                int userId) {
        this.selector    = selector;
        this.hashedToken = hashedToken;
        this.requestedAt = requestedAt;
        this.expiresAt   = expiresAt;
        this.userId      = userId;
    }

    // ── Getters / Setters ─────────────────────────────────────
    public int           getId()          { return id; }
    public void          setId(int id)    { this.id = id; }

    public String        getSelector()              { return selector; }
    public void          setSelector(String s)      { this.selector = s; }

    public String        getHashedToken()            { return hashedToken; }
    public void          setHashedToken(String h)    { this.hashedToken = h; }

    public LocalDateTime getRequestedAt()            { return requestedAt; }
    public void          setRequestedAt(LocalDateTime t) { this.requestedAt = t; }

    public LocalDateTime getExpiresAt()              { return expiresAt; }
    public void          setExpiresAt(LocalDateTime t)   { this.expiresAt = t; }

    public int           getUserId()                 { return userId; }
    public void          setUserId(int userId)        { this.userId = userId; }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }
}