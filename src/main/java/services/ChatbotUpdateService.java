package services;

import entities.Demande;

public class ChatbotUpdateService {

    private String attenteModification = null;

    public String processMessage(String message, Demande demande) {
        String msg = message.trim(); // On garde les espaces au cas où, mais on nettoie les bords
        String msgLower = msg.toLowerCase();

        // 1. Analyse de l'intention
        if (attenteModification == null) {
            if (msgLower.contains("nom")) {
                attenteModification = "nom";
                return "D'accord, quel est le nouveau **nom** ? (3 caractères min)";
            } else if (msgLower.contains("prenom") || msgLower.contains("prénom")) {
                attenteModification = "prenom";
                return "Très bien, quel est le nouveau **prénom** ? (3 caractères min)";
            } else if (msgLower.contains("num") || msgLower.contains("tel") || msgLower.contains("téléphone") || msgLower.contains("phone")) {
                attenteModification = "telephone";
                return "Entendu, quel est le nouveau **numéro de téléphone** ? (8 chiffres)";
            }
            return "Je n'ai pas compris. Voulez-vous modifier le nom, le prénom ou le num ?";
        }

        // 2. Traitement avec Contrôle de Saisie (Validation)
        switch (attenteModification) {
            case "nom":
                if (msg.length() < 3) {
                    return "⚠️ Erreur : Le nom est trop court. Il doit avoir **au moins 3 caractères**. Réessayez ?";
                }
                demande.setNom(msg);
                attenteModification = null;
                return "✅ Nom mis à jour : " + msg + ". Autre chose ?";

            case "prenom":
                if (msg.length() < 3) {
                    return "⚠️ Erreur : Le prénom est trop court. Il doit avoir **au moins 3 caractères**. Réessayez ?";
                }
                demande.setPrenom(msg);
                attenteModification = null;
                return "✅ Prénom mis à jour : " + msg + ". Autre chose ?";

            case "telephone":
                // On vérifie que c'est bien 8 chiffres exactement (ou plus selon ton besoin)
                String cleanNum = msg.replaceAll("\\s+", "");
                if (cleanNum.matches("^[0-9]{8}$")) {
                    demande.setPhone_number(cleanNum);
                    attenteModification = null;
                    return "✅ Numéro mis à jour : " + cleanNum + ". Autre chose ?";
                } else {
                    return "⚠️ Erreur : Le numéro doit contenir **exactement 8 chiffres**. Réessayez ?";
                }

            default:
                attenteModification = null;
                return "Une erreur est survenue. Recommençons.";
        }
    }
}