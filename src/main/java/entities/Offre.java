package entities;

import java.time.LocalDateTime;

public class Offre {
    private Long id;
    private String title;
    private String type_poste;
    private String type_contrat;
    private String description;
    private String lieu;
    private String statut;
    private LocalDateTime date_debut;
    private LocalDateTime date_fin;
    private Double salaire;
    private Boolean is_active;
    private String statut_validation = "en_attente";
    private int agriculteur_id;

    public Offre() {
    }

    public Offre(Long id, String title, String type_poste, String type_contrat, String description, String lieu, String statut, LocalDateTime date_debut, LocalDateTime date_fin, Double salaire, Boolean is_active, String statut_validation, int agriculteur_id) {
        this.id = id;
        this.title = title;
        this.type_poste = type_poste;
        this.type_contrat = type_contrat;
        this.description = description;
        this.lieu = lieu;
        this.statut = statut;
        this.date_debut = date_debut;
        this.date_fin = date_fin;
        this.salaire = salaire;
        this.is_active = is_active;
        this.statut_validation = statut_validation;
        this.agriculteur_id = agriculteur_id;
    }

    public Offre(String title, String type_poste, String type_contrat, String description, String lieu, String statut, LocalDateTime date_debut, LocalDateTime date_fin, Double salaire, Boolean is_active, String statut_validation, int agriculteur_id) {
        this.title = title;
        this.type_poste = type_poste;
        this.type_contrat = type_contrat;
        this.description = description;
        this.lieu = lieu;
        this.statut = statut;
        this.date_debut = date_debut;
        this.date_fin = date_fin;
        this.salaire = salaire;
        this.is_active = is_active;
        this.statut_validation = statut_validation;
        this.agriculteur_id = agriculteur_id;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getType_poste() {
        return type_poste;
    }

    public void setType_poste(String type_poste) {
        this.type_poste = type_poste;
    }

    public String getType_contrat() {
        return type_contrat;
    }

    public void setType_contrat(String type_contrat) {
        this.type_contrat = type_contrat;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLieu() {
        return lieu;
    }

    public void setLieu(String lieu) {
        this.lieu = lieu;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public LocalDateTime getDate_debut() {
        return date_debut;
    }

    public void setDate_debut(LocalDateTime date_debut) {
        this.date_debut = date_debut;
    }

    public LocalDateTime getDate_fin() {
        return date_fin;
    }

    public void setDate_fin(LocalDateTime date_fin) {
        this.date_fin = date_fin;
    }

    public Double getSalaire() {
        return salaire;
    }

    public void setSalaire(Double salaire) {
        this.salaire = salaire;
    }

    public Boolean getIs_active() {
        return is_active;
    }

    public void setIs_active(Boolean is_active) {
        this.is_active = is_active;
    }

    public String getStatut_validation() {
        return statut_validation;
    }

    public void setStatut_validation(String statut_validation) {
        this.statut_validation = statut_validation;
    }

    public int getAgriculteur_id() {
        return agriculteur_id;
    }

    public void setAgriculteur_id(int agriculteur_id) {
        this.agriculteur_id = agriculteur_id;
    }

    @Override
    public String toString() {
        return "Offre{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", type_poste='" + type_poste + '\'' +
                ", type_contrat='" + type_contrat + '\'' +
                ", description='" + description + '\'' +
                ", lieu='" + lieu + '\'' +
                ", statut='" + statut + '\'' +
                ", date_debut=" + date_debut +
                ", date_fin=" + date_fin +
                ", salaire=" + salaire +
                ", is_active=" + is_active +
                ", statut_validation='" + statut_validation + '\'' +
                ", agriculteur_id=" + agriculteur_id +
                '}';
    }
}
