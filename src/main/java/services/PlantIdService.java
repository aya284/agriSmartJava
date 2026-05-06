package services;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class PlantIdService {

    private static final String API_KEY = ConfigService.getPlantIdKey();
    private static final String API_URL = "https://api.plant.id/v2/health_assessment";

    public JSONObject analyzePlantImage(File imageFile) throws Exception {
        // 1. Convertir l'image en Base64
        String base64Image = encodeFileToBase64(imageFile);

        // 2. Préparer le JSON de la requête
        JSONObject requestJson = new JSONObject();
        JSONArray imagesArray = new JSONArray();
        imagesArray.put(base64Image);
        requestJson.put("images", imagesArray);

        // Demander les détails de la maladie et les traitements si disponibles
        JSONArray detailsArray = new JSONArray();
        detailsArray.put("description");
        detailsArray.put("treatment");
        requestJson.put("disease_details", detailsArray);
        
        // Forcer la langue en Français
        requestJson.put("language", "fr");

        // 3. Envoyer la requête HTTP POST
        URL url = new URL(API_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Api-Key", API_KEY);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(10000); // 10 secondes (upload image)
        conn.setReadTimeout(15000);    // 15 secondes (analyse IA)

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = requestJson.toString().getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        // 4. Lire la réponse
        int responseCode = conn.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line);
            }
            in.close();
            return new JSONObject(response.toString());
        } else {
            // Lecture de l'erreur
            BufferedReader err = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
            StringBuilder errorResponse = new StringBuilder();
            String line;
            while ((line = err.readLine()) != null) {
                errorResponse.append(line);
            }
            err.close();
            throw new Exception("Erreur API Plant.id (" + responseCode + "): " + errorResponse.toString());
        }
    }

    private String encodeFileToBase64(File file) throws Exception {
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            byte[] bytes = new byte[(int) file.length()];
            fileInputStream.read(bytes);
            return Base64.getEncoder().encodeToString(bytes);
        }
    }
}
