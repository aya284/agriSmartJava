package services.ocr;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import utils.ConfigLoader;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class ClaudeService {

    private static final String API_KEY = ConfigLoader.get("ANTHROPIC_API_KEY");
    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String MODEL   = "claude-opus-4-5";

    private final HttpClient   httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper mapper     = new ObjectMapper();

    // ─────────────────────────────────────────────────────────
    //  CORE METHOD — send a prompt, get a response
    // ─────────────────────────────────────────────────────────

    public String ask(String prompt) throws Exception {
        return ask(prompt, null, 1024);
    }

    public String ask(String prompt, String systemPrompt) throws Exception {
        return ask(prompt, systemPrompt, 1024);
    }

    public String ask(String prompt, String systemPrompt, int maxTokens) throws Exception {

        // Build request body using Jackson (safe — no manual string escaping)
        ObjectNode body = mapper.createObjectNode();
        body.put("model",      MODEL);
        body.put("max_tokens", maxTokens);

        // Optional system prompt
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            body.put("system", systemPrompt);
        }

        // Messages array
        ArrayNode messages = mapper.createArrayNode();
        ObjectNode userMsg = mapper.createObjectNode();
        userMsg.put("role",    "user");
        userMsg.put("content", prompt);
        messages.add(userMsg);
        body.set("messages", messages);

        // Build HTTP request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type",      "application/json")
                .header("x-api-key",         API_KEY)
                .header("anthropic-version", "2023-06-01")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                .build();

        // Send request
        HttpResponse<String> response = httpClient.send(
                request, HttpResponse.BodyHandlers.ofString());

        // Handle errors
        if (response.statusCode() != 200) {
            JsonNode error = mapper.readTree(response.body());
            throw new Exception("Claude API error " + response.statusCode()
                    + ": " + error.path("error").path("message").asText());
        }

        // Extract text from response
        JsonNode root = mapper.readTree(response.body());
        return root.path("content").get(0).path("text").asText();
    }

    // ─────────────────────────────────────────────────────────
    //  JSON RESPONSE — for structured outputs
    // ─────────────────────────────────────────────────────────

    public JsonNode askForJson(String prompt, String systemPrompt) throws Exception {
        String systemWithJson = (systemPrompt != null ? systemPrompt + "\n" : "")
                + "Respond ONLY with valid JSON. No explanation, no markdown, no extra text.";

        String rawResponse = ask(prompt, systemWithJson, 1024);

        // Strip markdown code fences if Claude adds them
        String cleaned = rawResponse
                .replaceAll("```json", "")
                .replaceAll("```", "")
                .trim();

        return mapper.readTree(cleaned);
    }
}