package services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import entities.User;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AdminAiService {

    private final GrokService grokService;
    private final UserService userService;
    private final ObjectMapper mapper;


    private static final Map<String, List<String>> INTENTS = new LinkedHashMap<>();
    static {
        INTENTS.put("suspicious", List.of(
            "suspicious", "suspect", "suspects", "suspicieux", "fraude", "fraud",
            "doublon", "duplicate", "duplique", "anomalie", "anomaly", "strange"
        ));
        INTENTS.put("pending", List.of(
            "pending", "attente", "en attente", "non verifie", "not verified",
            "unverified", "en cours", "a valider", "a verifier", "waiting"
        ));
        INTENTS.put("flagged", List.of(
            "flagged", "flag", "signale", "marked", "marque", "a revoir",
            "review", "manuel", "manual", "a examiner"
        ));
        INTENTS.put("rejected", List.of(
            "rejected", "rejete", "refuse", "denied", "blocked", "bloque",
            "banni", "banned", "inactive", "inactif", "desactive"
        ));
        INTENTS.put("active", List.of(
            "active", "actif", "actifs", "approved", "approuve", "verified",
            "verifie", "valide", "ok", "bons comptes"
        ));
        INTENTS.put("today", List.of(
            "today", "aujourd'hui", "aujourd", "ce jour", "du jour",
            "journee", "new today", "inscrits aujourd"
        ));
        INTENTS.put("recent", List.of(
            "recent", "recents", "last", "derniers", "dernieres", "latest",
            "nouveaux", "nouvelles inscriptions", "new users", "vient d'arriver"
        ));
        INTENTS.put("stats", List.of(
            "stats", "statistics", "statistiques", "statistique", "summary",
            "resume", "overview", "apercu", "count", "nombre", "combien",
            "how many", "total", "rapport", "report", "dashboard", "bilan",
            "chiffres", "numbers", "platform", "plateforme"
        ));
    }

    public AdminAiService() {
        this.grokService = new GrokService();
        this.userService = new UserService();
        this.mapper = new ObjectMapper();
    }

    // ── Entry Point ───────────────────────────────────────────

    public String handleAdminQuery(String input) {
        if (input == null || input.isBlank()) return "Veuillez poser une question.";

        String normalized = normalize(input);
        System.out.println("[AdminAI] Query: " + normalized);

        try {
            String intent = detectIntent(normalized);
            System.out.println("[AdminAI] Intent: " + intent);

            List<User> users = null;
            Map<String, Object> stats = null;
            String contextInfo;

            switch (intent) {
                case "suspicious" -> {
                    users = userService.getSuspiciousUsers();
                    contextInfo = "Utilisateurs suspects : doublons CIN ou comportement anormal.";
                }
                case "pending" -> {
                    users = userService.getPendingUsers();
                    contextInfo = "Utilisateurs en attente de vérification de document.";
                }
                case "flagged" -> {
                    users = userService.getFlaggedUsers();
                    contextInfo = "Utilisateurs signalés pour révision manuelle par l'IA.";
                }
                case "rejected" -> {
                    users = userService.getRejectedUsers();
                    contextInfo = "Utilisateurs dont le dossier CIN a été rejeté ou bloqués.";
                }
                case "active" -> {
                    users = userService.getActiveUsers();
                    contextInfo = "Utilisateurs actifs et approuvés sur la plateforme.";
                }
                case "today" -> {
                    users = userService.getTodayUsers();
                    contextInfo = "Inscriptions du jour.";
                }
                case "recent" -> {
                    users = userService.getRecentUsers(10);
                    contextInfo = "Les 10 inscriptions les plus récentes.";
                }
                case "stats" -> {
                    stats = userService.getAdminStats();
                    contextInfo = "Statistiques générales de la plateforme AgriSmart.";
                }
                case "email" -> {
                    String email = extractEmail(normalized);
                    if (email == null) return "❌ Impossible d'extraire l'email de votre requête.";
                    User u = userService.getUserByEmail(email);
                    if (u == null) return "❌ Aucun utilisateur trouvé avec l'email : " + email;
                    users = List.of(u);
                    contextInfo = "Fiche détaillée pour : " + email;
                }
                case "name" -> {
                    String name = extractName(normalized);
                    users = userService.searchUsers(name, "Tous", "Tous");
                    if (users == null || users.isEmpty())
                        return "❌ Aucun utilisateur trouvé avec le nom : \"" + name + "\"";
                    contextInfo = "Résultats de recherche pour le nom : \"" + name + "\"";
                }
                default -> {
                    // Smart fallback: give the AI platform stats + recent users so it can
                    // still answer intelligently instead of returning a useless help message.
                    stats = userService.getAdminStats();
                    users = userService.getRecentUsers(5);
                    contextInfo = "Question générale. Contexte de la plateforme fourni pour aider.";
                }
            }

            String jsonPayload = buildJsonPayload(users, stats);
            String prompt = buildPrompt(input, jsonPayload, contextInfo);
            return grokService.ask(prompt);

        } catch (Exception e) {
            System.err.println("[AdminAI] Error: " + e.getMessage());
            return "⚠️ Erreur lors de l'analyse : " + e.getMessage();
        }
    }

    // ── Intent Detection ──────────────────────────────────────

    /**
     * Scores each intent by summing the lengths of all matched keywords.
     * Longer matches win (e.g. "en attente" = 9 pts beats "attente" = 7 pts).
     * Email and name are checked separately before/after scoring.
     */
    private String detectIntent(String normalized) {
        if (containsEmail(normalized)) return "email";

        Map<String, Integer> scores = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : INTENTS.entrySet()) {
            int score = 0;
            for (String kw : entry.getValue()) {
                if (normalized.contains(kw)) score += kw.length();
            }
            if (score > 0) scores.put(entry.getKey(), score);
        }

        if (scores.isEmpty()) {
            // Last resort: if it looks like a person's name, try name search
            if (looksLikeName(normalized)) return "name";
            return "general";
        }

        return scores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .get().getKey();
    }

    /**
     * Lowercases + strips accents + collapses whitespace.
     * This makes "réjeté", "rejete", "REJETE" all equal "rejete".
     */
    private String normalize(String input) {
        return input.toLowerCase()
                .replaceAll("[éèêë]", "e")
                .replaceAll("[àâä]", "a")
                .replaceAll("[ùûü]", "u")
                .replaceAll("[îï]", "i")
                .replaceAll("[ôö]", "o")
                .replaceAll("[ç]", "c")
                .replaceAll("[^a-z0-9@._%+\\-'\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /** True if 1–3 plain words with no digits or special chars. */
    private boolean looksLikeName(String text) {
        String[] words = text.trim().split("\\s+");
        return words.length >= 1 && words.length <= 3 && text.matches("[a-z ]+");
    }

    /**
     * Strip common question words to isolate the name the admin is searching for.
     */
    private String extractName(String text) {
        return text
                .replaceAll("(trouve|recherche|cherche|show|find|display|qui est|user|utilisateur|profil|fiche|compte|account|info(s)?)", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    // ── JSON Payload Builder ──────────────────────────────────

    private String buildJsonPayload(List<User> users, Map<String, Object> stats) {
        ObjectNode root = mapper.createObjectNode();

        if (stats != null && !stats.isEmpty()) {
            ObjectNode statsNode = mapper.createObjectNode();
            stats.forEach((k, v) -> statsNode.put(k, String.valueOf(v)));
            root.set("platform_statistics", statsNode);
        }

        if (users != null && !users.isEmpty()) {
            ArrayNode arr = mapper.createArrayNode();
            for (User u : users) {
                ObjectNode node = mapper.createObjectNode();
                node.put("id",         u.getId());
                node.put("name",       u.getFirstName() + " " + u.getLastName());
                node.put("email",      u.getEmail() != null      ? u.getEmail()      : "N/A");
                node.put("role",       u.getRole() != null        ? u.getRole()       : "N/A");
                node.put("status",     u.getStatus() != null      ? u.getStatus()     : "N/A");
                node.put("cin",        u.getCinNumber() != null   ? u.getCinNumber()  : "N/A");
                node.put("created_at", u.getCreatedAt() != null   ? u.getCreatedAt().toString() : "N/A");
                arr.add(node);
            }
            root.set("users", arr);
        }

        return root.toPrettyString();
    }

    // ── Prompt Builder ────────────────────────────────────────

    private String buildPrompt(String question, String jsonData, String context) {
        return """
            Tu es un assistant administrateur expert pour AgriSmart, une plateforme de gestion agricole.
            Tu analyses les données utilisateurs et aides l'admin à prendre des décisions.
            
            === CONTEXTE ===
            """ + context + """
            
            === QUESTION DE L'ADMIN ===
            """ + question + """
            
            === DONNÉES (JSON) ===
            """ + jsonData + """
            
            === INSTRUCTIONS DE RÉPONSE ===
            - Réponds toujours en français, clairement et de façon structurée
            - Utilise des emojis pour la lisibilité : actif  rejeté  suspect  en attente  signalé  stats  utilisateur
            - Utilise des bullet points (•) pour les listes d'utilisateurs
            - Pour chaque utilisateur listé, affiche : Nom, Email, Statut, CIN si disponible
            - Si des anomalies sont détectables (doublon CIN, email suspect, inscriptions massives), SIGNALE-LES proactivement
            - Si les données sont vides, dis-le clairement et suggère une action à l'admin
            - Ne jamais inventer des données absentes du JSON
            - Sois concis mais complet — l'admin doit pouvoir agir directement sur ta réponse
            """;
    }

    // ── Regex Helpers ─────────────────────────────────────────

    private boolean containsEmail(String text) {
        return text.matches(".*[a-z0-9._%+\\-]+@[a-z0-9.\\-]+\\.[a-z]{2,}.*");
    }

    private String extractEmail(String text) {
        Matcher m = Pattern.compile("[a-z0-9._%+\\-]+@[a-z0-9.\\-]+\\.[a-z]{2,}").matcher(text);
        return m.find() ? m.group() : null;
    }
}