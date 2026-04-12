package entities;

public class Ressource {
    private int id;
    private String nom;
    private String type;
    private double stockRestant;
    private String unite;
    private int userId;

    public Ressource() {}

    public Ressource(int id, String nom, String type, double stockRestant, String unite, int userId) {
        this.id = id;
        this.nom = nom;
        this.type = type;
        this.stockRestant = stockRestant;
        this.unite = unite;
        this.userId = userId;
    }

    public Ressource(String nom, String type, double stockRestant, String unite, int userId) {
        this.nom = nom;
        this.type = type;
        this.stockRestant = stockRestant;
        this.unite = unite;
        this.userId = userId;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public double getStockRestant() { return stockRestant; }
    public void setStockRestant(double stockRestant) { this.stockRestant = stockRestant; }
    public String getUnite() { return unite; }
    public void setUnite(String unite) { this.unite = unite; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
}
