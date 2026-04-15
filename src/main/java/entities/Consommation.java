package entities;

import java.time.LocalDate;

public class Consommation {
    private int id;
    private double quantite;
    private LocalDate dateConsommation;
    private int ressourceId;
    private int cultureId;
    private String ressourceNom; // Champ additionnel pour l'affichage
    private String unite; // Champ additionnel pour l'affichage

    public Consommation() {
    }

    public Consommation(int id, double quantite, LocalDate dateConsommation, int ressourceId, int cultureId) {
        this.id = id;
        this.quantite = quantite;
        this.dateConsommation = dateConsommation;
        this.ressourceId = ressourceId;
        this.cultureId = cultureId;
    }

    public Consommation(double quantite, LocalDate dateConsommation, int ressourceId, int cultureId) {
        this.quantite = quantite;
        this.dateConsommation = dateConsommation;
        this.ressourceId = ressourceId;
        this.cultureId = cultureId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public double getQuantite() {
        return quantite;
    }

    public void setQuantite(double quantite) {
        this.quantite = quantite;
    }

    public LocalDate getDateConsommation() {
        return dateConsommation;
    }

    public void setDateConsommation(LocalDate dateConsommation) {
        this.dateConsommation = dateConsommation;
    }

    public int getRessourceId() {
        return ressourceId;
    }

    public void setRessourceId(int ressourceId) {
        this.ressourceId = ressourceId;
    }

    public int getCultureId() {
        return cultureId;
    }

    public void setCultureId(int cultureId) {
        this.cultureId = cultureId;
    }

    public String getRessourceNom() {
        return ressourceNom;
    }

    public void setRessourceNom(String ressourceNom) {
        this.ressourceNom = ressourceNom;
    }

    public String getUnite() {
        return unite;
    }

    public void setUnite(String unite) {
        this.unite = unite;
    }
}
