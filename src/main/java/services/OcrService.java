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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

public class OcrService {

    private static final String API_KEY = ConfigLoader.get("GOOGLE_VISION_API_KEY");
    private static final String API_URL = "https://vision.googleapis.com/v1/images:annotate";

    private final HttpClient httpClient;
    private final ObjectMapper mapper;

    public OcrService() {
        this.httpClient = HttpClient.newHttpClient();
        this.mapper = new ObjectMapper();
    }

    public String extractText(String filePath) throws Exception {
        byte[] fileBytes = Files.readAllBytes(Paths.get(filePath));
        String base64Content = Base64.getEncoder().encodeToString(fileBytes);

        String featureType = filePath.toLowerCase().endsWith(".pdf") ? "DOCUMENT_TEXT_DETECTION" : "TEXT_DETECTION";

        // Build request body
        ObjectNode body = mapper.createObjectNode();
        ArrayNode requests = mapper.createArrayNode();
        ObjectNode request = mapper.createObjectNode();
        
        ObjectNode image = mapper.createObjectNode();
        image.put("content", base64Content);
        request.set("image", image);

        ArrayNode features = mapper.createArrayNode();
        ObjectNode feature = mapper.createObjectNode();
        feature.put("type", featureType);
        features.add(feature);
        request.set("features", features);

        requests.add(request);
        body.set("requests", requests);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + "?key=" + API_KEY))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new Exception("Google Vision API Error (" + response.statusCode() + "): " + response.body());
        }

        JsonNode root = mapper.readTree(response.body());
        JsonNode fullTextAnnotation = root.path("responses").get(0).path("fullTextAnnotation");
        
        if (fullTextAnnotation.isMissingNode()) {
            return "";
        }

        return fullTextAnnotation.path("text").asText();
    }
}
