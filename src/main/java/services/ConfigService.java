package services;

import java.util.Properties;

public class ConfigService {
    private static Properties properties = new Properties();

    static {
        try {
            properties.load(ConfigService.class.getClassLoader().getResourceAsStream("config.properties"));
        } catch (Exception e) {
            System.err.println("ERREUR : Impossible de charger le fichier config.properties : " + e.getMessage());
        }
    }

    public static String getHuggingFaceKey() {
        return properties.getProperty("huggingface.key", "");
    }

    public static String getGeminiKey() {
        return properties.getProperty("gemini.key", "");
    }

    public static String getPlantIdKey() {
        return properties.getProperty("plantid.key", "");
    }

    public static String getGoogleCalendarKey() {
        return properties.getProperty("GOOGLE_CALENDAR_API_KEY", "");
    }
}
