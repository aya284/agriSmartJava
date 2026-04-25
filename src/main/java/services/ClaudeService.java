package services;

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
    private static final String MODEL   = "claude-3-5-sonnet-20240620"; // Note: User asked for claude-opus-4-5, but sonnet-3-5 is the current top model. I will use the requested string though if they insist.
    // Wait, the user specifically said "claude-opus-4-5". I will follow the instruction literally.

    private final HttpClient httpClient;
    private final ObjectMapper mapper;

    public ClaudeService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.mapper = new ObjectMapper();
    }

    public String ask(String prompt, String systemPrompt) throws Exception {
        ObjectNode body = mapper.createObjectNode();
        body.put("model", "claude-opus-4-5");
        body.put("max_tokens", 1024);

        if (systemPrompt != null && !systemPrompt.isBlank()) {
            body.put("system", systemPrompt);
        }

        ArrayNode messages = mapper.createArrayNode();
        ObjectNode userMsg = mapper.createObjectNode();
        userMsg.put("role", "user");
        userMsg.put("content", prompt);
        messages.add(userMsg);
        body.set("messages", messages);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("x-api-key", API_KEY)
                .header("anthropic-version", "2023-06-01")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new Exception("Claude API Error (" + response.statusCode() + "): " + response.body());
        }

        JsonNode root = mapper.readTree(response.body());
        return root.path("content").get(0).path("text").asText();
    }

    public JsonNode askForJson(String prompt, String systemPrompt) throws Exception {
        String systemWithJson = (systemPrompt != null ? systemPrompt + "\n" : "")
                + "Respond ONLY with valid JSON. No preamble, no markdown fences.";
        
        String response = ask(prompt, systemWithJson);
        
        // Strip markdown code fences if present
        String cleaned = response.replaceAll("```json", "")
                                 .replaceAll("```", "")
                                 .trim();
        
        return mapper.readTree(cleaned);
    }
}
