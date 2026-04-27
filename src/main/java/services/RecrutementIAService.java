package services;

import entities.Offre;
import java.util.List;
import java.util.Random;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.json.JSONObject;
import org.json.JSONArray;
import java.io.InputStream;
import java.util.Properties;

public class RecrutementIAService {

    private String API_KEY;
    private String API_URL;

    public RecrutementIAService() {
        loadApiKey();
    }

    private void loadApiKey() {
        try {
            Properties props = new Properties();
            InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties");
            if (input != null) {
                props.load(input);
                this.API_KEY = props.getProperty("GEMINI_API_KEY", "");
                input.close();
            }
            if (this.API_KEY == null || this.API_KEY.isEmpty()) {
                System.err.println("❌ ERREUR: Clé API Gemini non trouvée dans config.properties");
                this.API_KEY = ""; // Clé vide pour éviter les erreurs
            } else {
                System.out.println("✅ Clé API Gemini chargée avec succès depuis config.properties");
            }
            // ✅ Utiliser gemini-2.5-flash
            this.API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + API_KEY;
        } catch (Exception e) {
            System.err.println("❌ Erreur lors du chargement de config.properties: " + e.getMessage());
            e.printStackTrace();
            this.API_KEY = "";
        }
    }

    public String obtenirConseilRecrutement(String userMessage, String cvContent, List<Offre> offresDisponibles) {

        StringBuilder listeOffresStr = new StringBuilder();
        if (offresDisponibles == null || offresDisponibles.isEmpty()) {
            listeOffresStr.append("Aucune offre disponible pour le moment.");
        } else {
            for (Offre o : offresDisponibles) {
                listeOffresStr.append("- OFFRE #").append(o.getId())
                        .append(" : ").append(o.getTitle())
                        .append(" à ").append(o.getLieu()).append("\n");
            }
        }

        String systemPrompt = "Tu es AgriSmart AI, expert en recrutement agricole en Tunisie.\n" +
                "Règles :\n" +
                "- Réponds UNIQUEMENT en français\n" +
                "- Sois concis (max 3-4 phrases)\n" +
                "- Utilise des emojis pour rendre la lecture agréable\n" +
                "- Si hors-sujet, dis : \"🌱 Je suis spécialisé uniquement dans le recrutement agricole.\"\n\n" +
                "📋 Offres disponibles :\n" + listeOffresStr.toString() + "\n\n" +
                "📄 CV du candidat : " + (cvContent != null && !cvContent.isEmpty() ? cvContent : "Aucun CV téléchargé");

        return appelerGemini(systemPrompt, userMessage);
    }

    private String appelerGemini(String systemPrompt, String userMessage) {
        try {
            HttpClient client = HttpClient.newHttpClient();

            JSONObject requestBody = new JSONObject();
            JSONArray contents = new JSONArray();

            JSONObject content = new JSONObject();
            JSONArray parts = new JSONArray();

            String fullPrompt = systemPrompt + "\n\n👤 Utilisateur : " + userMessage;
            parts.put(new JSONObject().put("text", fullPrompt));
            content.put("parts", parts);
            contents.put(content);

            requestBody.put("contents", contents);

            JSONObject generationConfig = new JSONObject();
            generationConfig.put("temperature", 0.7);
            generationConfig.put("maxOutputTokens", 500);
            generationConfig.put("topP", 0.95);
            requestBody.put("generationConfig", generationConfig);

            System.out.println("📤 Envoi requête à Gemini avec modèle: gemini-2.5-flash");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("📥 Code réponse: " + response.statusCode());

            if (response.statusCode() == 200) {
                JSONObject jsonResponse = new JSONObject(response.body());

                if (jsonResponse.has("candidates") && !jsonResponse.getJSONArray("candidates").isEmpty()) {
                    JSONObject candidate = jsonResponse.getJSONArray("candidates").getJSONObject(0);
                    if (candidate.has("content")) {
                        JSONObject contentObj = candidate.getJSONObject("content");
                        if (contentObj.has("parts")) {
                            JSONArray partsArray = contentObj.getJSONArray("parts");
                            if (partsArray.length() > 0) {
                                String text = partsArray.getJSONObject(0).getString("text");
                                System.out.println("✅ Réponse générée avec succès !");
                                return text;
                            }
                        }
                    }
                }
                return "🤖 Je n'ai pas pu générer de réponse. Veuillez réessayer.";
            } else {
                System.err.println("❌ Erreur API: " + response.body());
                return getQuestionSecours();
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur: " + e.getMessage());
            e.printStackTrace();
            return getQuestionSecours();
        }
    }

    private String getQuestionSecours() {
        String[] questions = {
                "Bonjour, pouvez-vous vous présenter et parler de votre expérience en agriculture ?",
                "Quelles sont vos compétences principales dans le domaine agricole ?",
                "Pourquoi souhaitez-vous rejoindre AgriSmart ?",
                "Comment gérez-vous les périodes de forte activité agricole ?",
                "Quelle est votre disponibilité pour commencer ?",
                "Décrivez une situation où vous avez résolu un problème dans une exploitation agricole.",
                "Avez-vous des questions sur notre entreprise ?"
        };
        Random rand = new Random();
        return questions[rand.nextInt(questions.length)];
    }
}