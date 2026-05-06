package services;

import entities.Culture;
import entities.Ressource;
import entities.User;
import utils.SessionManager;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class NotificationService {

    private final RessourceService ressourceService = new RessourceService();
    private final CultureService cultureService = new CultureService();
    private final ParcelleService parcelleService = new ParcelleService();

    public List<String> getNotificationsForCurrentUser() {
        List<String> notifications = new ArrayList<>();
        User currentUser = SessionManager.getInstance().getCurrentUser();
        
        if (currentUser == null) return notifications;

        try {
            // 1. Check Resources Low Stock
            List<Ressource> ressources = ressourceService.afficherByUser(currentUser.getId());
            for (Ressource r : ressources) {
                if (r.getStockRestant() == 0) {
                    notifications.add("⚠️ Stock Épuisé : La ressource '" + r.getNom() + "' est à 0 " + r.getUnite() + ".");
                } else if (r.getStockRestant() <= 10) {
                    notifications.add("🔔 Stock Faible : La ressource '" + r.getNom() + "' est presque épuisée (" + r.getStockRestant() + " " + r.getUnite() + ").");
                }
            }

            // 2. Check Upcoming Harvests
            // We need to get all cultures for all parcelles of the user
            var parcelles = parcelleService.afficherByUser(currentUser.getId());
            for (var p : parcelles) {
                List<Culture> cultures = cultureService.getByParcelle(p.getId());
                for (Culture c : cultures) {
                    if (c.getDateRecoltePrevue() != null && !"Récolté".equalsIgnoreCase(c.getStatut())) {
                        long daysUntilHarvest = ChronoUnit.DAYS.between(LocalDate.now(), c.getDateRecoltePrevue());
                        if (daysUntilHarvest == 0) {
                            notifications.add("🚜 Récolte Aujourd'hui : C'est le jour de la récolte pour " + c.getTypeCulture() + " (" + c.getVariete() + ") !");
                        } else if (daysUntilHarvest == 1) {
                            notifications.add("📅 Récolte Demain : Préparez-vous, la récolte de " + c.getTypeCulture() + " (" + c.getVariete() + ") est prévue pour demain.");
                        } else if (daysUntilHarvest > 0 && daysUntilHarvest <= 3) {
                            notifications.add("ℹ️ Récolte Proche : La récolte de " + c.getTypeCulture() + " est prévue dans " + daysUntilHarvest + " jours.");
                        }
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return notifications;
    }
}
