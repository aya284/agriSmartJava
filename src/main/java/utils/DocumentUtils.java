package utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility for extracting data from documents using regex.
 */
public class DocumentUtils {

    /**
     * Extracts an 8-digit Tunisian CIN number using strict regex matching.
     * Prevents false positives by ensuring digits are consecutive and isolated.
     * 
     * Rules:
     * - Must be exactly 8 digits
     * - Must not be part of a larger number
     * - Must not be constructed from separate fragments (to avoid birth date confusion)
     */
    public static String extractCinNumber(String rawText) {
        if (rawText == null || rawText.isBlank()) return null;

        System.out.println("   [DEBUG] Starting Strict CIN Extraction...");

        // 1. Minimal Normalization (Fix common OCR character errors only)
        // Note: We do NOT remove spaces/punctuation here to keep \b boundaries effective
        String normalized = rawText.toUpperCase()
                                   .replace("O", "0")
                                   .replace("I", "1")
                                   .replace("L", "1");

        // 2. Strict Regex Matching (\b\d{8}\b)
        // This ensures we find a standalone 8-digit block
        Pattern pattern = Pattern.compile("\\b\\d{8}\\b");
        Matcher matcher = pattern.matcher(normalized);

        while (matcher.find()) {
            String candidate = matcher.group();
            System.out.println("   ✅ Valid CIN candidate found: " + candidate);
            return candidate; // Return the first valid standalone 8-digit number
        }

        System.err.println("   ❌ No strict 8-digit CIN block found.");
        return null;
    }

    /**
     * Strict validation for Tunisian CIN (8 digits).
     */
    public static boolean isValidCin(String cin) {
        return cin != null && cin.matches("^\\d{8}$");
    }
}
