package services;

import org.json.JSONArray;
import org.json.JSONObject;
import utils.ConfigLoader;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * GrokService - Integration with Grok API (X/Twitter)
 * 
 * Features:
 * - Document analysis and verification
 * - OCR for text extraction
 * - JSON response parsing
 * - Image/document vision capabilities
 * - Automatic retry on rate limits
 */
public class GrokService {

    // ── Grok / Groq API Configuration ────────────────────
    private static final String API_KEY = ConfigLoader.get("GROK_API_KEY");
    
    // Auto-detect if it's a Groq key (starts with gsk_) or Grok key (x.ai)
    private static final boolean IS_GROQ = API_KEY != null && API_KEY.startsWith("gsk_");
    
    private static final String API_URL = IS_GROQ 
            ? "https://api.groq.com/openai/v1/chat/completions"
            : "https://api.x.ai/v1/chat/completions";
    
    private static final String MODEL = IS_GROQ
            ? "llama-3.3-70b-versatile" 
            : "grok-vision-beta";
    
    private final HttpClient httpClient;
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY = 2000; // 2 seconds

    public GrokService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
    }

    // ── Simple text prompts ────────────────────────────────
    /**
     * Ask Grok a simple text question
     */
    public String ask(String prompt) throws Exception {
        return ask(prompt, 1024);
    }

    /**
     * Ask Grok with custom max tokens
     */
    public String ask(String prompt, int maxTokens) throws Exception {
        System.out.println("Grok API: Sending prompt (" + 
                prompt.length() + " chars)...");

        JSONObject body = new JSONObject();
        body.put("model", MODEL);
        body.put("temperature", 0.2);
        body.put("max_tokens", maxTokens);

        // Messages array with user prompt
        JSONArray messages = new JSONArray();
        JSONObject message = new JSONObject();
        message.put("role", "user");
        message.put("content", prompt);
        messages.put(message);
        body.put("messages", messages);

        return sendRequest(body, "text");
    }

    // ── JSON responses ────────────────────────────────────
    /**
     * Ask Grok and expect JSON response
     */
    public JSONObject askForJson(String prompt) throws Exception {
        String fullPrompt = prompt +
                "\n\n IMPORTANT: Respond ONLY with valid JSON. " +
                "No markdown code fences (```), no explanation, just pure JSON.";

        String raw = ask(fullPrompt, 2048);

        // Clean markdown if present
        String cleaned = raw
                .replaceAll("(?s)```json\\s*", "")
                .replaceAll("(?s)```\\s*", "")
                .trim();

        System.out.println(" Raw response length: " + raw.length());
        System.out.println("Cleaned response: " + cleaned.substring(
                0, Math.min(200, cleaned.length())) + "...");

        try {
            return new JSONObject(cleaned);
        } catch (Exception e) {
            System.err.println("❌ JSON parse error: " + e.getMessage());
            System.err.println("Response was: " + cleaned);
            throw new Exception("Grok returned invalid JSON: " + cleaned, e);
        }
    }

    // ── Internal HTTP handling ────────────────────────────
    /**
     * Send request to Grok API with retry logic
     */
    private String sendRequest(JSONObject body, String operationType) 
            throws Exception {
        
        if (API_KEY == null || API_KEY.isEmpty()) {
            throw new Exception(
                    "GROK_API_KEY environment variable not set. " +
                    "Get your free key at: https://console.x.ai");
        }

        int attempt = 0;
        while (attempt < MAX_RETRIES) {
            try {
                System.out.println("   [Attempt " + (attempt + 1) + "/" + 
                        MAX_RETRIES + "]");

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(API_URL))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + API_KEY)
                        .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                        .build();

                HttpResponse<String> response = httpClient.send(
                        request, HttpResponse.BodyHandlers.ofString());

                int statusCode = response.statusCode();
                System.out.println("   HTTP Status: " + statusCode);

                // Handle rate limiting (429)
                if (statusCode == 429) {
                    System.out.println("Rate limited. Retrying in " + 
                            (RETRY_DELAY / 1000) + " seconds...");
                    attempt++;
                    Thread.sleep(RETRY_DELAY);
                    continue;
                }

                // Handle errors
                if (statusCode >= 400) {
                    String errorMsg = "Unknown error";
                    try {
                        JSONObject errorNode = new JSONObject(response.body());
                        errorMsg = errorNode
                                .optJSONObject("error")
                                != null ? errorNode.optJSONObject("error").optString("message", "Unknown error") : errorMsg;
                    } catch (Exception ignored) {
                    }
                    
                    throw new Exception(
                            "Grok API Error (" + statusCode + "): " + errorMsg +
                            "\n" + response.body());
                }

                // Success - parse response
                JSONObject root = new JSONObject(response.body());
                JSONArray choices = root.optJSONArray("choices");
                String content = "";
                if (choices != null && !choices.isEmpty()) {
                    JSONObject firstChoice = choices.optJSONObject(0);
                    if (firstChoice != null) {
                        JSONObject message = firstChoice.optJSONObject("message");
                        if (message != null) {
                            content = message.optString("content", "");
                        }
                    }
                }

                if (content.isEmpty()) {
                    throw new Exception("Empty response from Grok API");
                }

                System.out.println("Grok response received (" + 
                        content.length() + " chars)");
                return content;

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw e;
            } catch (Exception e) {
                if (attempt == MAX_RETRIES - 1) {
                    throw e; // Last attempt, throw error
                }
                System.err.println("Attempt failed: " + e.getMessage());
                attempt++;
                Thread.sleep(RETRY_DELAY);
            }
        }

        throw new Exception(
                operationType + " failed after " + MAX_RETRIES + " attempts");
    }

    // ── Utility: Check API key validity ────────────────────
    /**
     * Test if Grok API key is valid
     */
    public boolean testConnection() {
        try {
            String result = ask("Respond with just: OK", 10);
            return result.contains("OK");
        } catch (Exception e) {
            System.err.println("Grok connection test failed: " + 
                    e.getMessage());
            return false;
        }
    }
}