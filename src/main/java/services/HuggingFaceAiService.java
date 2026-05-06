package services;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.Properties;

public class HuggingFaceAiService {

    private static final String DEFAULT_MODEL = "Qwen/Qwen2.5-7B-Instruct";
    private static final String DEFAULT_TASKS_MODEL = "meta-llama/Llama-3.2-3B-Instruct";
    private static final String LOCAL_SECRETS_FILE = "local.secrets.properties";
    private static final String CHAT_COMPLETIONS_ENDPOINT = "https://router.huggingface.co/v1/chat/completions";

    private final HttpClient client;
    private final Properties localSecrets;
    private final String apiKey;
    private final String model;
    private final String tasksModel;

    public HuggingFaceAiService() {
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.localSecrets = loadLocalSecrets();
        this.apiKey = readConfig("HUGGINGFACE_API_KEY", "");
        this.model = readConfig("HUGGINGFACE_MODEL", DEFAULT_MODEL);
        this.tasksModel = readConfig("HUGGINGFACE_TASKS_MODEL", DEFAULT_TASKS_MODEL);
    }

    public String suggestProductDescription(String productName, String category, String offerType) throws IOException, InterruptedException {
        String prompt = "You are an assistant for an agricultural marketplace. "
                + "Write one concise French product description (max 90 words), professional and persuasive, "
                + "without markdown and without bullet points. Include quality, usage, and trust tone."
                + " Product name: " + safe(productName)
                + ". Category: " + safe(category)
                + ". Offer type: " + safe(offerType)
                + ". Return only the final description in French.";
        return queryModelWithFallback(tasksModel, model, prompt, 220);
    }

    public String chat(String userMessage, String contextHint) throws IOException, InterruptedException {
        String prompt = "You are AgriSmart assistant. Reply in French with short practical guidance. "
                + "If asked about products, payments, shipping, or marketplace actions, provide clear steps. "
                + "Context: " + safe(contextHint)
                + "\nUser: " + safe(userMessage)
                + "\nAssistant:";
        return queryModelWithFallback(model, tasksModel, prompt, 260);
    }

    private String queryModelWithFallback(String primaryModel, String secondaryModel, String prompt, int maxTokens)
            throws IOException, InterruptedException {
        IOException primaryError;
        try {
            return queryModel(primaryModel, prompt, maxTokens);
        } catch (IOException ex) {
            primaryError = ex;
        }

        String secondary = safe(secondaryModel);
        if (secondary.isBlank() || secondary.equalsIgnoreCase(safe(primaryModel))) {
            throw primaryError;
        }

        try {
            return queryModel(secondary, prompt, maxTokens);
        } catch (IOException secondaryError) {
            throw new IOException(primaryError.getMessage() + " | Fallback: " + secondaryError.getMessage(), secondaryError);
        }
    }

    private String queryModel(String modelName, String prompt, int maxTokens) throws IOException, InterruptedException {
        if (apiKey.isBlank()) {
            throw new IOException("HUGGINGFACE_API_KEY manquante.");
        }

        String selectedModel = safe(modelName);
        if (selectedModel.isBlank()) {
            throw new IOException("Modele Hugging Face manquant.");
        }

        JSONObject payload = new JSONObject();
        payload.put("model", selectedModel);
        payload.put("max_tokens", maxTokens);
        payload.put("temperature", 0.6);

        JSONArray messages = new JSONArray();
        messages.put(new JSONObject().put("role", "user").put("content", prompt));
        payload.put("messages", messages);

        HttpResponse<String> response = sendInferenceRequest(payload.toString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException(buildHttpErrorMessage(response));
        }

        String body = safe(response.body());
        String apiError = extractApiError(body);
        if (!apiError.isBlank()) {
            throw new IOException("Erreur Hugging Face: " + apiError);
        }

        String text = parseModelText(body);
        if (text.isBlank()) {
            throw new IOException("Aucune reponse IA exploitable.");
        }
        return clean(text);
    }

    private HttpResponse<String> sendInferenceRequest(String payload) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(CHAT_COMPLETIONS_ENDPOINT))
                .timeout(Duration.ofSeconds(60))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private String buildHttpErrorMessage(HttpResponse<String> response) {
        String headerMessage = response.headers().firstValue("x-error-message").orElse("").trim();
        if (!headerMessage.isBlank()) {
            return "Erreur Hugging Face HTTP " + response.statusCode() + ": " + headerMessage;
        }

        String body = safe(response.body()).trim();
        String apiError = extractApiError(body);
        if (!apiError.isBlank()) {
            return "Erreur Hugging Face HTTP " + response.statusCode() + ": " + apiError;
        }

        if (!body.isBlank()) {
            return "Erreur Hugging Face HTTP " + response.statusCode() + ": " + cleanHtml(body);
        }

        return "Erreur Hugging Face HTTP " + response.statusCode() + ".";
    }

    private String extractApiError(String body) {
        if (body == null || body.isBlank()) {
            return "";
        }

        String trimmed = body.trim();
        if (!trimmed.startsWith("{")) {
            return "";
        }
        try {
            JSONObject json = new JSONObject(trimmed);
            return json.optString("error", "").trim();
        } catch (Exception ex) {
            return "";
        }
    }

    private String cleanHtml(String raw) {
        String text = safe(raw).replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();
        if (text.length() > 220) {
            return text.substring(0, 220) + "...";
        }
        return text;
    }

    private String parseModelText(String body) {
        if (body == null || body.isBlank()) {
            return "";
        }

        String trimmed = body.trim();
        if (trimmed.startsWith("{")) {
            JSONObject json = new JSONObject(trimmed);
            JSONArray choices = json.optJSONArray("choices");
            if (choices != null && !choices.isEmpty()) {
                JSONObject firstChoice = choices.optJSONObject(0);
                if (firstChoice != null) {
                    JSONObject message = firstChoice.optJSONObject("message");
                    if (message != null) {
                        return message.optString("content", "");
                    }
                    String text = firstChoice.optString("text", "");
                    if (!text.isBlank()) {
                        return text;
                    }
                }
            }

            if (json.has("generated_text")) {
                return json.optString("generated_text", "");
            }
            if (json.has("error")) {
                return "";
            }
            return "";
        }

        JSONArray array = new JSONArray(trimmed);
        if (array.isEmpty()) {
            return "";
        }
        Object first = array.get(0);
        if (first instanceof JSONObject) {
            JSONObject obj = (JSONObject) first;
            return obj.optString("generated_text", "");
        }
        return String.valueOf(first);
    }

    private String clean(String text) {
        return safe(text)
                .replace("\r", " ")
                .replace("\n", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String readConfig(String key, String defaultValue) {
        String sys = System.getProperty(key);
        if (sys != null && !sys.isBlank()) {
            return sys;
        }
        String env = System.getenv(key);
        if (env != null && !env.isBlank()) {
            return env;
        }
        String local = localSecrets.getProperty(key);
        if (local != null && !local.isBlank()) {
            return local;
        }
        return defaultValue;
    }

    private Properties loadLocalSecrets() {
        Properties properties = new Properties();
        List<Path> candidates = List.of(
                Paths.get(LOCAL_SECRETS_FILE),
                Paths.get("config", LOCAL_SECRETS_FILE),
                Paths.get("src", "main", "resources", LOCAL_SECRETS_FILE)
        );

        for (Path candidate : candidates) {
            if (!Files.exists(candidate) || !Files.isRegularFile(candidate)) {
                continue;
            }
            try (InputStream stream = Files.newInputStream(candidate)) {
                properties.load(stream);
                break;
            } catch (IOException ignored) {
                // Ignore and continue searching other locations.
            }
        }
        return properties;
    }

    private String safe(String input) {
        return input == null ? "" : input.trim();
    }
}