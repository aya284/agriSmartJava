package utils;

public class Validator {

    // ── Champs de base ────────────────────────────────────────

    public static String validateFirstName(String value) {
        if (value == null || value.trim().length() < 2)
            return "Prénom invalide (minimum 2 caractères).";
        if (!value.matches("[a-zA-ZÀ-ÿ\\s'\\-]+"))
            return "Prénom invalide (lettres seulement).";
        return null;
    }

    public static String validateLastName(String value) {
        if (value == null || value.trim().length() < 2)
            return "Nom invalide (minimum 2 caractères).";
        if (!value.matches("[a-zA-ZÀ-ÿ\\s'\\-]+"))
            return "Nom invalide (lettres seulement).";
        return null;
    }

    public static String validateEmail(String email) {
        if (email == null || email.trim().isEmpty())
            return "L'adresse e-mail est obligatoire.";
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))
            return "Adresse e-mail invalide.";
        return null;
    }

    public static String validatePassword(String password) {
        if (password == null || password.isEmpty())
            return "Le mot de passe est obligatoire.";
        if (password.length() < 8)
            return "Mot de passe : minimum 8 caractères.";
        if (!password.matches(".*[A-Z].*"))
            return "Mot de passe : au moins une lettre majuscule.";
        if (!password.matches(".*[a-z].*"))
            return "Mot de passe : au moins une lettre minuscule.";
        if (!password.matches(".*[0-9].*"))
            return "Mot de passe : au moins un chiffre.";
        if (!password.matches(".*[\\W].*"))
            return "Mot de passe : au moins un caractère spécial.";
        return null;
    }

    public static String validateConfirmPassword(String password, String confirm) {
        if (confirm == null || confirm.isEmpty())
            return "Veuillez confirmer votre mot de passe.";
        if (!password.equals(confirm))
            return "Les mots de passe ne correspondent pas.";
        return null;
    }

    public static String validatePhone(String phone) {
        if (phone == null || phone.trim().isEmpty())
            return null; // optionnel
        if (!phone.matches("^(\\+216)?[2459]\\d{7}$"))
            return "Numéro tunisien invalide (ex: 22123456 ou +21622123456).";
        return null;
    }

    public static String validateAddress(String address) {
        if (address == null || address.trim().isEmpty())
            return null; // optionnel
        if (address.trim().length() < 5 || address.length() > 255)
            return "Adresse : entre 5 et 255 caractères.";
        return null;
    }

    public static String validateRole(String role) {
        if (role == null || role.trim().isEmpty())
            return "Veuillez choisir un type de compte.";
        if (!role.matches("agriculteur|fournisseur|employee|admin"))
            return "Rôle invalide.";
        return null;
    }

    // ── Validation complète du formulaire Register ────────────
    // Retourne null si tout est valide, sinon le premier message d'erreur

    public static String validateRegisterForm(
            String firstName, String lastName, String email,
            String password,  String confirm,  String phone,
            String address,   String role) {

        String err;
        if ((err = validateFirstName(firstName)) != null)  return err;
        if ((err = validateLastName(lastName))   != null)  return err;
        if ((err = validateEmail(email))         != null)  return err;
        if ((err = validateRole(role))           != null)  return err;
        if ((err = validatePassword(password))   != null)  return err;
        if ((err = validateConfirmPassword(password, confirm)) != null) return err;
        if ((err = validatePhone(phone))         != null)  return err;
        if ((err = validateAddress(address))     != null)  return err;
        return null;
    }

    // ── Validation complète du formulaire Login ───────────────

    public static String validateLoginForm(String email, String password) {
        String err;
        if ((err = validateEmail(email))    != null) return err;
        if (password == null || password.isEmpty())  return "Le mot de passe est obligatoire.";
        return null;
    }
}
