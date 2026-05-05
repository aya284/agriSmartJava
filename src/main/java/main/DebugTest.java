package main;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import utils.MyConnection;
import services.OffreService;
import entities.Offre;
import controllers.CandidatOffreController;
import javafx.application.Platform;

public class DebugTest {
    public static void main(String[] args) {
        try {
            OffreService os = new OffreService();
            var list = os.afficher();
            System.out.println("Total offers: " + list.size());
            if (list.size() > 0) {
                Offre last = list.get(list.size() - 1);
                System.out.println("Last offer: " + last);
                
                // Let's check for any null fields that might cause issues
                System.out.println("ID: " + last.getId());
                System.out.println("Title: " + last.getTitle());
                System.out.println("Desc: " + last.getDescription());
                System.out.println("Salaire: " + last.getSalaire());
                System.out.println("Date debut: " + last.getDate_debut());
                System.out.println("Date fin: " + last.getDate_fin());
                System.out.println("Statut: " + last.getStatut());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.exit(0);
    }
}
