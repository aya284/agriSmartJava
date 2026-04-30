package services;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Properties;
import java.io.InputStream;

public class HuggingFaceService {

    private static final String API_KEY = loadApiKey();
    private static final String SUMMARIZATION_MODEL = "facebook/bart-large-cnn";
    private static final String TTS_MODEL = "facebook/mms-tts-fra";

    private static String loadApiKey() {
        // 1. Try environment variable first
        String envKey = System.getenv("HUGGINGFACE_API_KEY");
        if (envKey != null && !envKey.trim().isEmpty()) {
            return envKey.trim();
        }

        // 2. Fallback to config.properties
        try (InputStream input = HuggingFaceService.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                System.err.println("WARNING: config.properties not found.");
            } else {
                Properties prop = new Properties();
                prop.load(input);
                String propKey = prop.getProperty("HUGGINGFACE_API_KEY");
                if (propKey != null && !propKey.trim().isEmpty() && !propKey.contains("your_")) {
                    return propKey.trim();
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading API key from config.properties: " + e.getMessage());
        }

        System.err.println("WARNING: HUGGINGFACE_API_KEY not found in environment or config.properties.");
        return "";
    }
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public String summarize(String text) throws Exception {
        try {
            return summarize(text, 0);
        } catch (Exception e) {
            System.err.println("Unexpected error in summarize: " + e.getMessage());
            e.printStackTrace();
            // Last resort fallback
            return localSummarize(text);
        }
    }

    private String summarize(String text, int retryCount) throws Exception {
        if (text == null || text.trim().length() < 20)
            return "";

        JSONObject payload = new JSONObject();
        payload.put("inputs", text);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api-inference.huggingface.co/models/" + SUMMARIZATION_MODEL))
                .header("Authorization", "Bearer " + API_KEY)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        System.out.println("Attempting API call with token: " + API_KEY.substring(0, 10) + "...");

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("API Response Status: " + response.statusCode());
        System.out.println("API Response Body: " + response.body());

        if (response.statusCode() == 200) {
            JSONArray result = new JSONArray(response.body());
            return result.getJSONObject(0).getString("summary_text");
        } else if (response.statusCode() == 503 && retryCount < 1) {
            // Model is loading, wait a bit and retry once
            System.out.println("Model loading, retrying in 5 seconds...");
            Thread.sleep(5000);
            return summarize(text, retryCount + 1);
        } else {
            System.err.println("Summarization API failed (" + response.statusCode() + "): " + response.body());
            System.out.println("Falling back to local summarization...");
            // Fallback to local summarization
            return localSummarize(text);
        }
    }

    /**
     * Local summarization fallback that extracts the most important sentence
     */
    private String localSummarize(String text) {
        if (text == null || text.trim().isEmpty())
            return "";

        System.out.println("Using local summarization for text of length: " + text.length());

        try {
            // Split into sentences
            String[] sentences = text.split("(?<=[.!?])\\s+");
            System.out.println("Found " + sentences.length + " sentences");

            if (sentences.length == 0) {
                return text.length() > 30 ? text.substring(0, 30) + "..." : text;
            }

            // For very short text, just take first sentence
            if (sentences.length == 1) {
                String sentence = sentences[0].trim();
                return sentence.length() > 30 ? sentence.substring(0, 30) + "..." : sentence;
            }

            // Score sentences by length and position (first sentences are often summaries)
            String bestSentence = "";
            for (int i = 0; i < Math.min(2, sentences.length); i++) {
                String sentence = sentences[i].trim();
                // Prefer first sentence, but skip if it's too short
                if (sentence.split("\\s+").length >= 8) {
                    bestSentence = sentence;
                    break;
                }
            }

            // If no good sentence found in first two, take the longest one
            if (bestSentence.isEmpty()) {
                for (String sentence : sentences) {
                    if (sentence.split("\\s+").length > bestSentence.split("\\s+").length) {
                        bestSentence = sentence.trim();
                    }
                }
            }

            // Limit to ~30 words (approximately 150 characters)
            String[] words = bestSentence.split("\\s+");
            StringBuilder summary = new StringBuilder();
            int wordCount = 0;

            for (String word : words) {
                if (wordCount >= 25) { // Stop at ~25 words
                    summary.append("...");
                    break;
                }
                summary.append(word).append(" ");
                wordCount++;
            }

            String result = summary.toString().trim();
            // Ensure proper punctuation
            if (!result.isEmpty() && !result.endsWith(".") && !result.endsWith("!") && !result.endsWith("?") && !result.endsWith("...")) {
                result += ".";
            }

            System.out.println("Final summary (" + wordCount + " words): " + result);
            return result;
        } catch (Exception e) {
            System.err.println("Error in local summarization: " + e.getMessage());
            e.printStackTrace();
            // Last resort: return first 30 characters
            return text.length() > 30 ? text.substring(0, 30) + "..." : text;
        }
    }

    public byte[] textToSpeech(String text) throws Exception {
        if (text == null || text.trim().isEmpty())
            return null;

        JSONObject payload = new JSONObject();
        payload.put("inputs", text);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api-inference.huggingface.co/models/" + TTS_MODEL))
                .header("Authorization", "Bearer " + API_KEY)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

        if (response.statusCode() == 200) {
            return response.body();
        } else {
            System.err.println("TTS failed: " + new String(response.body()));
            return null;
        }
    }
}
