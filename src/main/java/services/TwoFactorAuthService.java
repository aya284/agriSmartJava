package services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import entities.User;
import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class TwoFactorAuthService {

    private final EmailService emailService;
    private final HttpClient httpClient;
    private final ObjectMapper mapper;

    // In-memory storage for OTPs (userId -> code)
    // In production, this should probably be in DB with expiration
    private static final Map<Integer, String> otpStorage = new HashMap<>();

    public TwoFactorAuthService() {
        this.emailService = new EmailService();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        this.mapper = new ObjectMapper();
    }

    // ── OTP Logic ───────────────────────────────────────────

    public void sendOtp(User user) throws Exception {
        String otp = String.format("%06d", new Random().nextInt(999999));
        otpStorage.put(user.getId(), otp);

        System.out.println("[DEBUG] 2FA: OTP generated for user " + user.getId() + " is: " + otp);
        System.out.println("[DEBUG] 2FA: Current otpStorage keys: " + otpStorage.keySet());

        emailService.sendTwoFactorOtpEmail(user.getEmail(), otp);
    }

    public boolean verifyOtp(int userId, String inputCode) {
        String stored = otpStorage.get(userId);
        System.out.println("[DEBUG] 2FA Verify: userId=" + userId + ", input=" + inputCode + ", stored=" + stored);

        if (stored != null && stored.equals(inputCode)) {
            System.out.println("[DEBUG] 2FA Match! Removing code from storage.");
            otpStorage.remove(userId);
            return true;
        }
        System.out.println("[DEBUG] 2FA Mismatch or code expired.");
        return false;
    }

    // ── Face ID Logic ───────────────────────────────────────

    /**
     * Verify face against local FastAPI service.
     * 
     * @param storedImagePath   path to the reference image in DB
     * @param capturedImagePath path to the image just taken from camera
     */
    public boolean verifyFace(String storedImagePath, String capturedImagePath) throws Exception {
        if (storedImagePath == null || capturedImagePath == null) {
            throw new Exception("Missing image paths for Face ID verification");
        }

        // 1. Resolve stored image to absolute path if possible
        String absoluteStored = storedImagePath;
        File storedFile = new File(storedImagePath);
        if (!storedFile.isAbsolute()) {
            // Try to resolve relative to project root or uploads folder
            File uploadsDir = new File(System.getProperty("user.dir"), "uploads");
            File resolved = new File(uploadsDir, storedImagePath);
            if (resolved.exists()) {
                absoluteStored = resolved.getAbsolutePath();
            } else {
                // Fallback to project root
                absoluteStored = new File(System.getProperty("user.dir"), storedImagePath).getAbsolutePath();
            }
        }

        // 2. Normalize paths for Python (forward slashes)
        String path1 = absoluteStored.replace("\\", "/");
        String path2 = capturedImagePath.replace("\\", "/");

        System.out.println("[DEBUG] 2FA: Sending paths to Face ID:");
        System.out.println("  Stored: " + path1);
        System.out.println("  Captured: " + path2);

        // Build JSON body — FastAPI model: { "stored_path": ..., "captured_path": ... }
        Map<String, String> body = new HashMap<>();
        body.put("stored_path", path1);
        body.put("captured_path", path2);

        String jsonBody = mapper.writeValueAsString(body);
        System.out.println("[DEBUG] JSON SENT TO FASTAPI: " + jsonBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:8000/verify-face"))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, java.nio.charset.StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            System.err.println("[DEBUG] Face ID Error Body: " + response.body());
            throw new Exception("Face ID Service Error: " + response.statusCode());
        }

        JsonNode root = mapper.readTree(response.body());
        boolean match = root.path("match").asBoolean();
        double score = root.path("score").asDouble();
        String message = root.path("message").asText();

        System.out.println("Face ID Result: Match=" + match + ", Score=" + score + ", Msg=" + message);

        return match;
    }

    // ── 2FA Decision Logic ──────────────────────────────────

    /**
     * Check if 2FA should be triggered for this login attempt.
     */
    public boolean is2faRequired(User user, int failedAttempts, boolean isFirstLogin) {
        // Admins bypass 2FA — direct login to dashboard
        if ("admin".equalsIgnoreCase(user.getRole()))
            return false;

        // All other roles always require 2FA
        return true;
    }
}
