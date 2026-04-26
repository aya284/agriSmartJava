package entities;

public class Parcelle {
    private int id;
    private String nom;
    private double surface;
    private double latitude;
    private double longitude;
    private String typeSol;
    private int userId;
    private String coordonnees; // Stockage JSON des points du polygone

    // Constructeurs
    public Parcelle() {
    }

    public Parcelle(int id, String nom, double surface, double latitude, double longitude, String typeSol, int userId, String coordonnees) {
        this.id = id;
        this.nom = nom;
        this.surface = surface;
        this.latitude = latitude;
        this.longitude = longitude;
        this.typeSol = typeSol;
        this.userId = userId;
        this.coordonnees = coordonnees;
    }

    public Parcelle(String nom, double surface, double latitude, double longitude, String typeSol, int userId, String coordonnees) {
        this.nom = nom;
        this.surface = surface;
        this.latitude = latitude;
        this.longitude = longitude;
        this.typeSol = typeSol;
        this.userId = userId;
        this.coordonnees = coordonnees;
    }

    // Getters et Setters (indispensables pour la TableView)
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public double getSurface() {
        return surface;
    }

    public void setSurface(double surface) {
        this.surface = surface;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getTypeSol() {
        return typeSol;
    }

    public void setTypeSol(String typeSol) {
        this.typeSol = typeSol;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getCoordonnees() {
        return coordonnees;
    }

    public void setCoordonnees(String coordonnees) {
        this.coordonnees = coordonnees;
    }
}