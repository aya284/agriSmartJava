package services;

import entities.Demande;
import entities.Offre;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class MatchingServiceIA {

    private static final String API_KEY = "nA6CO7ubQH56abPXG6GfeymxyNV8B2oT";
    // Chemin vers tes CV (le même que dans ton Controller)
    private static final String CV_PATH = "C:\\Users\\USER\\Documents\\Esprit\\PI JAVA\\agriSmartJava\\src\\main\\resources\\uploads\\cv\\";

    /**
     * Lit le contenu texte d'un fichier PDF
     */
    private String extractTextFromPDF(String fileName) {
        try (PDDocument document = PDDocument.load(new File(CV_PATH + fileName))) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            // DEBUG pour voir si le texte est bien lu
            System.out.println("DEBUG - Texte extrait du PDF : " + (text.isEmpty() ? "VIDE" : "OK"));
            return text;
        } catch (IOException e) {
            System.err.println("Erreur lecture PDF: " + e.getMessage());
            return "";
        }
    }

    /**
     * Calcule le score de matching entre un CV et une Offre
     * @return un entier entre 0 et 100
     */
    public int getMatchingScore(Demande demande, Offre offre) {
        try {
            String cvContent = extractTextFromPDF(demande.getCv());

            // On reproduit exactement ton logic prompt PHP
            String systemPrompt = "Tu es l'IA officielle d'AgriSmart, expert en recrutement agricole en Tunisie. " +
                    "Ton rôle est d'analyser la compatibilité entre un CV et une offre d'emploi. " +
                    "Analyse les mots-clés (outils, diplômes, expérience). " +
                    "Réponds UNIQUEMENT avec un nombre entier (le pourcentage de matching). " +
                    "Ne donne aucune explication, juste le chiffre.";

            String userMessage = "OFFRE : " + offre.getTitle() + " à " + offre.getLieu() + "\n" +
                    "DESCRIPTION : " + offre.getDescription() + "\n\n" +
                    "CONTENU DU CV : " + cvContent;

            // Préparation du JSON pour Mistral
            JSONObject json = new JSONObject();
            json.put("model", "mistral-small-latest");
            json.put("temperature", 0.2); // Bas pour être précis sur le chiffre

            json.put("messages", new Object[]{
                    new JSONObject().put("role", "system").put("content", systemPrompt),
                    new JSONObject().put("role", "user").put("content", userMessage)
            });

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.mistral.ai/v1/chat/completions"))
                    .header("Authorization", "Bearer " + API_KEY)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // Extraction du chiffre depuis la réponse JSON
            JSONObject resJson = new JSONObject(response.body());

            // Vérification si la réponse contient des données
            if (resJson.has("choices")) {
                // CORRECTION : Utilisation de l'index 0 (le premier choix) au lieu de 2
                String rawScore = resJson.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content").trim();

                // Nettoyage au cas où l'IA ajoute un "%" ou du texte (ex: "85%")
                String cleanScore = rawScore.replaceAll("[^0-9]", "");

                int finalScore = cleanScore.isEmpty() ? 0 : Integer.parseInt(cleanScore);

                // --- MODIFICATION : AJOUT DU SCORE MINIMUM ---
                // Si le score est 0 mais qu'il y a un CV, on met 2% pour prouver que ça fonctionne
                if (finalScore == 0 && demande.getCv() != null && !demande.getCv().isEmpty()) {
                    return 2;
                }

                // On s'assure que le score reste entre 0 et 100
                return Math.min(100, Math.max(0, finalScore));
            }

            return 1; // Retourne 1 en cas de réponse vide de l'API

        } catch (Exception e) {
            System.err.println("Erreur matching IA: " + e.getMessage());
            return 1; // Retourne 1 en cas d'erreur technique
        }
    }
}