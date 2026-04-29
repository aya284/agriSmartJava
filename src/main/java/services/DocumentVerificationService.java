package services;

import entities.User;
import models.VerificationResult;
import utils.DocumentUtils;

public class DocumentVerificationService {

    private final OcrService            ocrService;
    private final UserService           userService;
    private final EmailService          emailService;

    public DocumentVerificationService() {
        this.ocrService   = new OcrService();
        this.userService  = new UserService();
        this.emailService = new EmailService();
    }

    /**
     * Simplified Pipeline:
     * OCR → Java CIN Extraction → Java Decision Logic → DB update → Email
     *
     * @param user         the newly registered user
     * @param documentPath absolute path to the uploaded CIN image
     * @return VerificationResult with final decision
     */
    public VerificationResult processDocument(User user,
                                               String documentPath) throws Exception {

        System.out.println("\n Starting CIN-based verification for: " + user.getEmail());

        // ── Step 1: OCR ───────────────────────────────────────
        System.out.println("Step 1: OCR extraction...");
        String extractedText;
        int ocrQuality = 0;
        try {
            extractedText = ocrService.extractText(documentPath);
            ocrQuality = (int) (ocrService.getLastQualityScore() * 100);
        } catch (Exception e) {
            System.err.println(" OCR failed: " + e.getMessage());
            return fallbackToReview(user, "OCR échoué : " + e.getMessage());
        }

        if (extractedText == null || extractedText.isBlank()) {
            return fallbackToReview(user, "Document illisible — aucun texte extrait.");
        }

        // ── Step 2: CIN Extraction (Java only) ────────────────
        System.out.println("Step 2: Extracting CIN number using Hybrid Strategy (Sliding Window + Regex Fallback)...");
        String extractedCin = DocumentUtils.extractCinNumber(extractedText);
        
        VerificationResult result = new VerificationResult();
        result.setUserEmail(user.getEmail());
        result.setExtractedText(extractedText);
        result.setExtractedCin(extractedCin);
        result.setQualityScore(ocrQuality);

        // ── Step 3: Java Decision Logic ───────────────────────
        System.out.println(" Step 3: Applying Java business rules...");
        applyJavaDecisions(user, result);

        // ── Step 4: Finalize ──────────────────────────────────
        finalizeVerification(user, result);

        return result;
    }

    /**
     * Compares extracted CIN with the one provided by the user during registration.
     */
    private void applyJavaDecisions(User user, VerificationResult result) {
        String extracted = result.getExtractedCin();
        String expected  = user.getCinNumber();

        System.out.println("   [DEBUG] Extracted CIN: " + extracted);
        System.out.println("   [DEBUG] Expected CIN:  " + expected);
        System.out.println("   [DEBUG] OCR Quality:   " + result.getQualityScore() + "%");

        // Rule 1: Check if any number was found
        if (extracted == null) {
            result.setDecision("REVIEW");
            result.setReason("Aucun numéro de CIN (8 chiffres) détecté sur l'image.");
            return;
        }

        // Rule 2: Strict Matching
        if (extracted.equals(expected)) {
            result.setDecision("APPROVE");
            result.setReason("Numéro CIN validé avec succès.");
        } else {
            result.setDecision("REJECT");
            result.setReason("Le numéro CIN extrait (" + extracted + ") ne correspond pas à celui fourni.");
        }
    }

    private void finalizeVerification(User user, VerificationResult result) {
        try {
            String status = switch (result.getDecision()) {
                case "APPROVE" -> "active";
                case "REJECT"  -> "inactive";
                default        -> "pending";
            };

            userService.updateStatus(user.getId(), status);
            emailService.sendVerificationResultEmail(
                user, status, result.getReason(),
                null, // No transliterated name needed
                result.getQualityScore()
            );

            System.out.println("✅ Decision applied: " + result.getDecision() + " -> " + status);
        } catch (Exception e) {
            System.err.println("❌ Error finalizing verification: " + e.getMessage());
        }
    }

    private VerificationResult fallbackToReview(User user, String reason) {
        VerificationResult result = new VerificationResult(user.getEmail(), "REVIEW", reason);
        try {
            userService.updateStatus(user.getId(), "pending");
            emailService.sendVerificationResultEmail(user, "pending", reason, null, 0);
        } catch (Exception ignored) {}
        return result;
    }
}