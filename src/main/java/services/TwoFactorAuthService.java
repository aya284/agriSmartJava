package services;

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
import org.json.JSONObject;

public class TwoFactorAuthService {

    private final EmailService emailService;
    private final HttpClient httpClient;

    // In-memory storage for OTPs (userId -> code)
    // In production, this should probably be in DB with expiration
    private static final Map<Integer, String> otpStorage = new HashMap<>();

    public TwoFactorAuthService() {
        this.emailService = new EmailService();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .version(HttpClient.Version.HTTP_1_1)
                .build();
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
        JSONObject body = new JSONObject();
        body.put("stored_path", path1);
        body.put("captured_path", path2);

        String jsonBody = body.toString();
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

        JSONObject root = new JSONObject(response.body());
        boolean match = root.optBoolean("match", false);
        double score = root.optDouble("score", 0.0);
        String message = root.optString("message", "");

        System.out.println("Face ID Result: Match=" + match + ", Score=" + score + ", Msg=" + message);

        return match;
    }

    // ── 2FA Decision Logic ──────────────────────────────────

    /**
     * Check if 2FA should be triggered for this login attempt.
     */
    public boolean is2faRequired(User user, int failedAttempts, boolean isFirstLogin) {

        // Admin bypass 2FA completely
        if ("admin".equalsIgnoreCase(user.getRole())) {
            return false;
        }
        // Other users: apply security rules
        if (failedAttempts > 3) {
            return true;
        }
      
        // default: no 2FA
        return false;
    }
}
