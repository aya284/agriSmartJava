package models;

/**
 * VerificationResult - Holds the outcome of document verification
 * 
 * Stores:
 * - Verification decision (APPROVED, PENDING_REVIEW, REJECTED)
 * - Confidence score (0-100%)
 * - Extracted text from OCR
 * - User email for tracking
 * - Message explaining the result
 */
public class VerificationResult {
    
    private String userEmail;
    private String decision;           // APPROVE, REVIEW, REJECT
    private String reason;             // Why this decision was made
    private String extractedText;      // Raw OCR text
    private String extractedCin;       // Extracted 8-digit number
    private int qualityScore;          // OCR quality metric
    private long verificationTimestamp;

    // ── Constructor ────────────────────────────────────────
    public VerificationResult() {
        this.verificationTimestamp = System.currentTimeMillis();
    }

    public VerificationResult(String userEmail, String decision, String reason) {
        this();
        this.userEmail = userEmail;
        this.decision = decision;
        this.reason = reason;
    }

    // ── Getters ────────────────────────────────────────────
    public String getUserEmail() { return userEmail; }
    public String getDecision() { return decision; }
    public String getReason() { return reason; }
    public String getMessage() { return reason; }
    public String getExtractedText() { return extractedText; }
    public String getExtractedCin() { return extractedCin; }
    public int getQualityScore() { return qualityScore; }
    public long getVerificationTimestamp() { return verificationTimestamp; }

    // ── Setters ────────────────────────────────────────────
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    public void setDecision(String decision) { this.decision = decision; }
    public void setReason(String reason) { this.reason = reason; }
    public void setMessage(String message) { this.reason = message; }
    public void setExtractedText(String extractedText) { this.extractedText = extractedText; }
    public void setExtractedCin(String cin) { this.extractedCin = cin; }
    public void setQualityScore(int score) { this.qualityScore = score; }
    public void setVerificationTimestamp(long ts) { this.verificationTimestamp = ts; }

    // ── Status checking helpers ────────────────────────────
    public boolean isApproved() {
        return "APPROVE".equalsIgnoreCase(decision);
    }

    public boolean isRejected() {
        return "REJECT".equalsIgnoreCase(decision);
    }

    public boolean isPendingReview() {
        return "REVIEW".equalsIgnoreCase(decision);
    }

    @Override
    public String toString() {
        return "VerificationResult{" +
                "email='" + userEmail + '\'' +
                ", decision='" + decision + '\'' +
                ", cin='" + extractedCin + '\'' +
                ", quality=" + qualityScore + "%" +
                '}';
    }



}