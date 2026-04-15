package entities;

import java.time.LocalDate;

public class Culture {
    private int id;
    private String typeCulture;
    private String variete;
    private LocalDate datePlantation;
    private LocalDate dateRecoltePrevue;
    private String statut;
    private int parcelleId;

    public Culture() {
    }

    public Culture(int id, String typeCulture, String variete, LocalDate datePlantation, LocalDate dateRecoltePrevue, String statut, int parcelleId) {
        this.id = id;
        this.typeCulture = typeCulture;
        this.variete = variete;
        this.datePlantation = datePlantation;
        this.dateRecoltePrevue = dateRecoltePrevue;
        this.statut = statut;
        this.parcelleId = parcelleId;
    }

    public Culture(String typeCulture, String variete, LocalDate datePlantation, LocalDate dateRecoltePrevue, String statut, int parcelleId) {
        this.typeCulture = typeCulture;
        this.variete = variete;
        this.datePlantation = datePlantation;
        this.dateRecoltePrevue = dateRecoltePrevue;
        this.statut = statut;
        this.parcelleId = parcelleId;
    }

    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTypeCulture() { return typeCulture; }
    public void setTypeCulture(String typeCulture) { this.typeCulture = typeCulture; }

    public String getVariete() { return variete; }
    public void setVariete(String variete) { this.variete = variete; }

    public LocalDate getDatePlantation() { return datePlantation; }
    public void setDatePlantation(LocalDate datePlantation) { this.datePlantation = datePlantation; }

    public LocalDate getDateRecoltePrevue() { return dateRecoltePrevue; }
    public void setDateRecoltePrevue(LocalDate dateRecoltePrevue) { this.dateRecoltePrevue = dateRecoltePrevue; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public int getParcelleId() { return parcelleId; }
    public void setParcelleId(int parcelleId) { this.parcelleId = parcelleId; }
}
