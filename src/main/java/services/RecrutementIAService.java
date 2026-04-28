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

    // ✅ gemini-2.5-flash = modèle gratuit actuel (remplace gemini-pro qui est MORT)
    private static final String MODEL = "gemini-2.5-flash";

    public RecrutementIAService() {
        loadApiKey();
    }

    private void loadApiKey() {
        try {
            Properties props = new Properties();
            InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties");
            if (input != null) {
                props.load(input);
                this.API_KEY = props.getProperty("GEMINI_API_KEY", "").trim();
                input.close();
            }
            if (this.API_KEY == null || this.API_KEY.isEmpty()) {
                System.err.println("❌ ERREUR: Clé API Gemini non trouvée dans config.properties");
                this.API_KEY = "";
            } else {
                System.out.println("✅ Clé API Gemini chargée avec succès");
                System.out.println("📌 Modèle utilisé: " + MODEL);
                // ✅ URL correcte pour gemini-2.5-flash avec v1beta
                this.API_URL = "https://generativelanguage.googleapis.com/v1beta/models/"
                        + MODEL + ":generateContent?key=" + API_KEY;
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur chargement config: " + e.getMessage());
            this.API_KEY = "";
        }
    }

    public String obtenirConseilRecrutement(String userMessage, String cvContent, List<Offre> offresDisponibles) {

        if (API_KEY == null || API_KEY.isEmpty()) {
            System.err.println("⚠️ Pas de clé API — réponse de secours");
            return getReponseSecours();
        }

        // Construire la liste des offres
        StringBuilder listeOffresStr = new StringBuilder();
        if (offresDisponibles == null || offresDisponibles.isEmpty()) {
            listeOffresStr.append("Aucune offre disponible pour le moment.");
        } else {
            int count = 0;
            for (Offre o : offresDisponibles) {
                if (count < 10) {
                    listeOffresStr.append("- ").append(o.getTitle())
                            .append(" à ").append(o.getLieu())
                            .append(" | ").append(o.getSalaire()).append(" TND\n");
                    count++;
                }
            }
        }

        // Prompt système orienté UNIQUEMENT candidature — pas d'entretien
        String systemPrompt =
                "Tu es AgriSmart AI, assistant candidature agricole en Tunisie.\n" +
                        "RÈGLES ABSOLUES :\n" +
                        "- Réponds UNIQUEMENT en français\n" +
                        "- Sois concis : 3 à 5 lignes maximum par réponse\n" +
                        "- Tu aides UNIQUEMENT pour : trouver une offre, postuler, analyser un CV, rédiger une lettre de motivation\n" +
                        "- Tu ne poses JAMAIS de questions d'entretien dans ce chat\n" +
                        "- Si on te demande un entretien ou simulation : dis d'utiliser le bouton 'Lancer le test' en haut\n" +
                        "- Si hors-sujet : réponds 'Je suis spécialisé uniquement dans l aide à la candidature AgriSmart.'\n" +
                        "- Tutoie le candidat, sois chaleureux et direct\n\n" +
                        "📋 Offres disponibles :\n" + listeOffresStr + "\n" +
                        "📄 CV du candidat : " + (cvContent != null && !cvContent.isEmpty()
                        ? (cvContent.length() > 300 ? cvContent.substring(0, 300) + "..." : cvContent)
                        : "Aucun CV téléchargé");

        return appelerGemini(systemPrompt, userMessage);
    }

    private String appelerGemini(String systemPrompt, String userMessage) {
        // Jusqu'à 3 tentatives
        for (int tentative = 1; tentative <= 3; tentative++) {
            try {
                HttpClient client = HttpClient.newHttpClient();

                // ✅ Structure correcte pour gemini-2.5-flash : system_instruction séparé
                JSONObject requestBody = new JSONObject();

                // System instruction séparé (supporte gemini-2.5-flash)
                JSONObject systemInstruction = new JSONObject();
                JSONArray sysParts = new JSONArray();
                sysParts.put(new JSONObject().put("text", systemPrompt));
                systemInstruction.put("parts", sysParts);
                requestBody.put("system_instruction", systemInstruction);

                // Message utilisateur
                JSONArray contents = new JSONArray();
                JSONObject content = new JSONObject();
                JSONArray parts = new JSONArray();
                parts.put(new JSONObject().put("text", userMessage));
                content.put("role", "user");
                content.put("parts", parts);
                contents.put(content);
                requestBody.put("contents", contents);

                // Config de génération
                JSONObject generationConfig = new JSONObject();
                generationConfig.put("temperature", 0.7);
                generationConfig.put("maxOutputTokens", 500);
                generationConfig.put("topP", 0.95);
                requestBody.put("generationConfig", generationConfig);

                System.out.println("📤 Envoi requête à " + MODEL + " (tentative " + tentative + "/3)");
                System.out.println("🔗 URL: " + API_URL);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(API_URL))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                System.out.println("📥 Code réponse HTTP: " + response.statusCode());

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
                                    System.out.println("✅ Réponse IA générée avec succès !");
                                    return text;
                                }
                            }
                        }
                    }
                    System.err.println("⚠️ Réponse 200 mais contenu vide: " + response.body());
                    return getReponseSecours();

                } else if (response.statusCode() == 503 || response.statusCode() == 429) {
                    System.err.println("⚠️ Service surchargé (tentative " + tentative + "/3) — attente...");
                    if (tentative < 3) {
                        Thread.sleep(3000);
                        continue;
                    }
                    return "Le service IA est momentanément surchargé. Réessayez dans quelques instants.";

                } else if (response.statusCode() == 404) {
                    System.err.println("❌ Modèle introuvable (404). Body: " + response.body());
                    return getReponseSecours();

                } else if (response.statusCode() == 400) {
                    System.err.println("❌ Requête invalide (400). Body: " + response.body());
                    return getReponseSecours();

                } else if (response.statusCode() == 403) {
                    System.err.println("❌ Clé API invalide ou quota dépassé (403). Body: " + response.body());
                    return "Clé API invalide ou quota dépassé. Vérifiez votre clé dans config.properties.";

                } else {
                    System.err.println("❌ Erreur API inattendue " + response.statusCode() + ": " + response.body());
                    if (tentative < 3) {
                        Thread.sleep(2000);
                        continue;
                    }
                    return getReponseSecours();
                }

            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return getReponseSecours();
            } catch (Exception e) {
                System.err.println("❌ Exception tentative " + tentative + ": " + e.getMessage());
                if (tentative < 3) {
                    try { Thread.sleep(2000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                }
            }
        }
        return getReponseSecours();
    }

    // Réponses de secours orientées CANDIDATURE (pas d'entretien)
    private String getReponseSecours() {
        String[] reponses = {
                "Je suis votre assistant candidature AgriSmart. Importez votre CV ou consultez les offres disponibles pour commencer !",
                "Consultez les offres disponibles ci-dessus. Cliquez sur 'Voir l offre' pour plus de détails.",
                "Conseil : Importez votre CV pour que je vous conseille sur l offre la plus adaptée à votre profil.",
                "Pour postuler, cliquez sur 'Voir l offre' puis sur 'Postuler' en bas de la page.",
                "Voulez-vous de l aide pour rédiger votre lettre de motivation ou choisir une offre ?",
                "Je peux vous aider à trouver l offre qui correspond à votre profil. Importez votre CV pour commencer !"
        };
        Random rand = new Random();
        return reponses[rand.nextInt(reponses.length)];
    }
}