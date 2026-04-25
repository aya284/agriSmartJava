package utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Utility class to load application configurations from config.properties.
 * This ensures sensitive data like API keys are not hardcoded in the source.
 */
public class ConfigLoader {
    private static final String CONFIG_FILE = "config.properties";
    private static final Properties properties = new Properties();

    static {
        try (InputStream input = ConfigLoader.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (input == null) {
                System.err.println("CRITICAL: Unable to find " + CONFIG_FILE + " in the classpath.");
            } else {
                properties.load(input);
                System.out.println("ConfigLoader: Successfully loaded " + properties.size() + " properties.");
            }
        } catch (IOException ex) {
            System.err.println("ConfigLoader Error: Failed to load " + CONFIG_FILE + ": " + ex.getMessage());
        }
    }

    /**
     * Gets a property value by key.
     * @param key The property key.
     * @return The property value or null if not found.
     */
    public static String get(String key) {
        return properties.getProperty(key);
    }

    /**
     * Gets a property value by key, with a default value.
     * @param key The property key.
     * @param defaultValue The default value if key is not found.
     * @return The property value or default.
     */
    public static String get(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
}
