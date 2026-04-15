package utils;

public class MarketplaceValidator {

    public static String validateProductName(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "Le nom du produit est obligatoire.";
        }
        String trimmed = value.trim();
        if (trimmed.length() < 3) {
            return "Le nom doit contenir au moins 3 caracteres.";
        }
        if (trimmed.length() > 120) {
            return "Le nom ne doit pas depasser 120 caracteres.";
        }
        return null;
    }

    public static String validateProductDescription(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "La description est obligatoire.";
        }
        String trimmed = value.trim();
        if (trimmed.length() < 10) {
            return "La description doit contenir au moins 10 caracteres.";
        }
        if (trimmed.length() > 1000) {
            return "La description ne doit pas depasser 1000 caracteres.";
        }
        return null;
    }

    public static String validateProductCategory(String value, String placeholder) {
        if (value == null || value.trim().isEmpty() || value.trim().equals(placeholder)) {
            return "Veuillez choisir une categorie.";
        }
        return null;
    }

    public static String validateOfferType(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "Veuillez choisir un type d'offre.";
        }
        return null;
    }

    public static String validatePositivePrice(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "Le prix est obligatoire.";
        }
        if (!value.trim().matches("\\d+(\\.\\d{1,2})?")) {
            return "Prix invalide (ex: 12 ou 12.50).";
        }
        double price = Double.parseDouble(value.trim());
        if (price <= 0) {
            return "Le prix doit etre superieur a 0.";
        }
        return null;
    }

    public static String validateStock(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "Le stock est obligatoire.";
        }
        if (!value.trim().matches("\\d+")) {
            return "Stock invalide (entier positif).";
        }
        int stock = Integer.parseInt(value.trim());
        if (stock < 0) {
            return "Le stock ne peut pas etre negatif.";
        }
        return null;
    }

    public static String validatePromoPrice(boolean promoEnabled, String promoValue, String priceValue) {
        if (!promoEnabled) {
            return null;
        }
        if (promoValue == null || promoValue.trim().isEmpty()) {
            return "Le prix promotionnel est obligatoire.";
        }
        if (!promoValue.trim().matches("\\d+(\\.\\d{1,2})?")) {
            return "Prix promotionnel invalide (ex: 9.90).";
        }

        double promoPrice = Double.parseDouble(promoValue.trim());
        if (promoPrice <= 0) {
            return "Le prix promotionnel doit etre superieur a 0.";
        }

        if (priceValue != null && priceValue.trim().matches("\\d+(\\.\\d{1,2})?")) {
            double normalPrice = Double.parseDouble(priceValue.trim());
            if (promoPrice >= normalPrice) {
                return "Le prix promotionnel doit etre inferieur au prix normal.";
            }
        }
        return null;
    }

    public static String validateCheckout(String deliveryMode, String paymentMode, String address) {
        String mode = deliveryMode == null ? "" : deliveryMode.trim();
        boolean homeDelivery = "Livraison a domicile".equals(mode);
        boolean pickup = "Retrait sur place".equals(mode);

        if (!homeDelivery && !pickup) {
            return "Mode de livraison invalide.";
        }
        if (paymentMode == null || paymentMode.trim().isEmpty()) {
            return "Mode de paiement obligatoire.";
        }
        String payment = paymentMode.trim().toLowerCase();
        if (!"domicile".equals(payment) && !"carte".equals(payment)) {
            return "Mode de paiement autorise: domicile ou carte.";
        }
        if (homeDelivery) {
            if (address == null || address.trim().isEmpty()) {
                return "Adresse de livraison obligatoire.";
            }
            if (address.trim().length() < 5) {
                return "Adresse trop courte (minimum 5 caracteres).";
            }
        }
        return null;
    }

    public static String validateCommande(String statut, String modePaiement, String adresse, String montant, String clientId) {
        if (statut == null || statut.trim().isEmpty()) {
            return "Statut obligatoire pour la commande.";
        }
        if (modePaiement == null || modePaiement.trim().isEmpty()) {
            return "Mode de paiement obligatoire pour la commande.";
        }
        String payment = modePaiement.trim().toLowerCase();
        if (!"domicile".equals(payment) && !"carte".equals(payment)) {
            return "Mode de paiement autorise pour la commande: domicile ou carte.";
        }
        if (adresse == null || adresse.trim().isEmpty()) {
            return "Adresse de livraison obligatoire pour la commande.";
        }
        if (montant == null || !montant.trim().matches("\\d+(\\.\\d{1,2})?")) {
            return "Montant invalide (ex: 120.50).";
        }
        if (Double.parseDouble(montant.trim()) <= 0) {
            return "Le montant doit etre superieur a 0.";
        }
        if (clientId == null || !clientId.trim().matches("\\d+")) {
            return "Client ID invalide.";
        }
        return null;
    }

    public static String validateWishlist(String userId, String produitId) {
        if (userId == null || !userId.trim().matches("\\d+")) {
            return "User ID invalide.";
        }
        if (produitId == null || !produitId.trim().matches("\\d+")) {
            return "Produit ID invalide.";
        }
        return null;
    }

    public static String validateMessage(String conversationId, String senderId, String content) {
        if (conversationId == null || !conversationId.trim().matches("\\d+")) {
            return "Conversation ID invalide.";
        }
        if (senderId == null || !senderId.trim().matches("\\d+")) {
            return "Sender ID invalide.";
        }
        if (content == null || content.trim().isEmpty()) {
            return "Le contenu du message est obligatoire.";
        }
        if (content.trim().length() < 2) {
            return "Le message est trop court.";
        }
        return null;
    }
}
