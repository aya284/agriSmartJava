package utils;

import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * Utility for fuzzy name matching using Levenshtein distance.
 */
public class NameMatcher {

    private static final double DEFAULT_THRESHOLD = 0.75;

    /**
     * Compares two names and returns true if they match above the threshold.
     * 
     * @param name1 Registered name
     * @param name2 Extracted name
     * @return true if match is sufficient
     */
    public static boolean matches(String name1, String name2) {
        return matches(name1, name2, DEFAULT_THRESHOLD);
    }

    public static boolean matches(String name1, String name2, double threshold) {
        if (name1 == null || name2 == null) return false;

        String n1 = normalize(name1);
        String n2 = normalize(name2);

        if (n1.isEmpty() || n2.isEmpty()) return false;

        int distance = calculateLevenshteinDistance(n1, n2);
        int maxLength = Math.max(n1.length(), n2.length());
        
        double similarity = 1.0 - ((double) distance / maxLength);
        
        System.out.println("🔍 Fuzzy Match: [" + n1 + "] vs [" + n2 + "]");
        System.out.println("   Distance: " + distance + ", Similarity: " + (similarity * 100) + "%");
        
        return similarity >= threshold;
    }

    /**
     * Normalizes a name: lowercase, removes accents, removes special characters.
     */
    public static String normalize(String name) {
        if (name == null) return "";
        
        // Convert to lowercase
        String normalized = name.toLowerCase().trim();
        
        // Remove accents (e.g., é -> e)
        normalized = Normalizer.normalize(normalized, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        normalized = pattern.matcher(normalized).replaceAll("");
        
        // Remove special characters, keeping only letters and spaces
        normalized = normalized.replaceAll("[^a-z\\s]", " ");
        
        // Consolidate spaces
        normalized = normalized.replaceAll("\\s+", " ").trim();
        
        return normalized;
    }

    /**
     * Standard Levenshtein Distance algorithm.
     */
    private static int calculateLevenshteinDistance(String s1, String s2) {
        int[] costs = new int[s2.length() + 1];
        for (int i = 0; i <= s1.length(); i++) {
            int lastValue = i;
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0) {
                    costs[j] = j;
                } else {
                    if (j > 0) {
                        int newValue = costs[j - 1];
                        if (s1.charAt(i - 1) != s2.charAt(j - 1)) {
                            newValue = Math.min(Math.min(newValue, lastValue), costs[j]) + 1;
                        }
                        costs[j - 1] = lastValue;
                        lastValue = newValue;
                    }
                }
            }
            if (i > 0) costs[s2.length()] = lastValue;
        }
        return costs[s2.length()];
    }
}
