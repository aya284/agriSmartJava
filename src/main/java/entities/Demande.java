package entities;

import java.time.LocalDateTime;

public class Demande {
    private Long id;
    private String nom;
    private String prenom;
    private String phone_number;
    private LocalDateTime date_postulation;
    private LocalDateTime date_modification;
    private String cv;
    private String lettre_motivation;
    private String statut;
    private int score; // Integer allows null as per your SQL (Oui NULL)
    private int users_id; // Integer allows null
    private int offre_id;

    // --- Constructors ---

    public Demande() {
    }

    // Constructor with all fields (including ID for fetching from DB)
    public Demande(Long id, String nom, String prenom, String phone_number, LocalDateTime date_postulation,
                   LocalDateTime date_modification, String cv, String lettre_motivation, String statut,
                   int score, int users_id, int offre_id) {
        this.id = id;
        this.nom = nom;
        this.prenom = prenom;
        this.phone_number = phone_number;
        this.date_postulation = date_postulation;
        this.date_modification = date_modification;
        this.cv = cv;
        this.lettre_motivation = lettre_motivation;
        this.statut = statut;
        this.score = score;
        this.users_id = users_id;
        this.offre_id = offre_id;
    }

    // Constructor without ID (for creating new applications)
    public Demande(String nom, String prenom, String phone_number, LocalDateTime date_postulation,
                   LocalDateTime date_modification, String cv, String lettre_motivation, String statut,
                   int score, int users_id, int offre_id) {
        this.nom = nom;
        this.prenom = prenom;
        this.phone_number = phone_number;
        this.date_postulation = date_postulation;
        this.date_modification = date_modification;
        this.cv = cv;
        this.lettre_motivation = lettre_motivation;
        this.statut = statut;
        this.score = score;
        this.users_id = users_id;
        this.offre_id = offre_id;
    }

    // --- Getters & Setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public String getPhone_number() {
        return phone_number;
    }

    public void setPhone_number(String phone_number) {
        this.phone_number = phone_number;
    }

    public LocalDateTime getDate_postulation() {
        return date_postulation;
    }

    public void setDate_postulation(LocalDateTime date_postulation) {
        this.date_postulation = date_postulation;
    }

    public LocalDateTime getDate_modification() {
        return date_modification;
    }

    public void setDate_modification(LocalDateTime date_modification) {
        this.date_modification = date_modification;
    }

    public String getCv() {
        return cv;
    }

    public void setCv(String cv) {
        this.cv = cv;
    }

    public String getLettre_motivation() {
        return lettre_motivation;
    }

    public void setLettre_motivation(String lettre_motivation) {
        this.lettre_motivation = lettre_motivation;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getUsers_id() {
        return users_id;
    }

    public void setUsers_id(int users_id) {
        this.users_id = users_id;
    }

    public int getOffre_id() {
        return offre_id;
    }

    public void setOffre_id(int offre_id) {
        this.offre_id = offre_id;
    }

    // --- ToString ---

    @Override
    public String toString() {
        return "Demande{" +
                "id=" + id +
                ", nom='" + nom + '\'' +
                ", prenom='" + prenom + '\'' +
                ", phone_number='" + phone_number + '\'' +
                ", date_postulation=" + date_postulation +
                ", date_modification=" + date_modification +
                ", cv='" + cv + '\'' +
                ", lettre_motivation='" + lettre_motivation + '\'' +
                ", statut='" + statut + '\'' +
                ", score=" + score +
                ", users_id=" + users_id +
                ", offre_id=" + offre_id +
                '}';
    }
}