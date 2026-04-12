package utils;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtils {
    public static String hash(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));
    }

    public static boolean verify(String plainPassword, String hashedPassword) {
        // Handles both BCrypt hashed and legacy plain passwords
        try {
            return BCrypt.checkpw(plainPassword, hashedPassword);
        } catch (Exception e) {
            return plainPassword.equals(hashedPassword); // fallback for legacy
        }
    }
}