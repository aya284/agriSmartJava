package services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import entities.User;
import utils.ConfigLoader;

import java.net.URI;
import java.net.http.*;

public class AiVerificationService {

    private static final String API_KEY = ConfigLoader.get("ANTHROPIC_API_KEY");
    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private final ObjectMapper mapper = new ObjectMapper();

    public VerificationResult analyze(User user, String extractedText) throws Exception {

        String prompt = """
            You are a document verification assistant for an agricultural platform.
            
            User profile:
            - Full name : %s %s
            - Email     : %s
            - Role      : %s
            
            Extracted document text:
            ---
            %s
            ---
            
            Analyze the document and respond ONLY in this exact JSON format, no extra text:
            {
              "decision"   : "APPROVE" or "REJECT" or "REVIEW",
              "confidence" : number between 0 and 100,
              "reason"     : "short explanation in French",
              "flags"      : ["list of issues found, empty if none"]
            }
            
            Rules:
            - APPROVE  if document clearly matches the user profile and looks legitimate
            - REJECT   if document is clearly fake, expired, or does not match user at all
            - REVIEW   if you are unsure or need a human to verify
        """.formatted(
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole(),
                extractedText
        );

        // Build request body
        String requestBody = """
            {
              "model"      : "claude-opus-4-5",
              "max_tokens" : 512,
              "messages"   : [
                { "role": "user", "content": "%s" }
              ]
            }
        """.formatted(prompt.replace("\"", "\\\"").replace("\n", "\\n"));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type",      "application/json")
                .header("x-api-key",         API_KEY)
                .header("anthropic-version", "2023-06-01")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        // Parse Claude's response
        JsonNode root    = mapper.readTree(response.body());
        String content   = root.path("content").get(0).path("text").asText();
        JsonNode decision = mapper.readTree(content);

        return new VerificationResult(
                decision.path("decision").asText(),
                decision.path("confidence").asInt(),
                decision.path("reason").asText(),
                decision.path("flags").toString()
        );
    }
}