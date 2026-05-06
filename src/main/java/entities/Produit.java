package entities;

import java.time.LocalDateTime;

public class Produit {
    private int id;
    private String nom;
    private String description;
    private String type;
    private double prix;
    private String categorie;
    private int quantiteStock;
    private String image;
    private boolean isPromotion;
    private double promotionPrice;
    private String locationAddress;
    private LocalDateTime locationStart;
    private LocalDateTime locationEnd;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean banned;
    private int vendeurId;
    private Double latitude;
    private Double longitude;

    public Produit() {}

    public Produit(String nom, String description, String type, double prix,
                   String categorie, int quantiteStock, String image,
                   boolean isPromotion, double promotionPrice, int vendeurId) {
        this.nom = nom;
        this.description = description;
        this.type = type;
        this.prix = prix;
        this.categorie = categorie;
        this.quantiteStock = quantiteStock;
        this.image = image;
        this.isPromotion = isPromotion;
        this.promotionPrice = promotionPrice;
        this.vendeurId = vendeurId;
        this.banned = false;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public double getPrix() { return prix; }
    public void setPrix(double prix) { this.prix = prix; }
    public String getCategorie() { return categorie; }
    public void setCategorie(String categorie) { this.categorie = categorie; }
    public int getQuantiteStock() { return quantiteStock; }
    public void setQuantiteStock(int quantiteStock) { this.quantiteStock = quantiteStock; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public boolean isPromotion() { return isPromotion; }
    public void setPromotion(boolean promotion) { isPromotion = promotion; }
    public double getPromotionPrice() { return promotionPrice; }
    public void setPromotionPrice(double promotionPrice) { this.promotionPrice = promotionPrice; }
    public String getLocationAddress() { return locationAddress; }
    public void setLocationAddress(String locationAddress) { this.locationAddress = locationAddress; }
    public LocalDateTime getLocationStart() { return locationStart; }
    public void setLocationStart(LocalDateTime locationStart) { this.locationStart = locationStart; }
    public LocalDateTime getLocationEnd() { return locationEnd; }
    public void setLocationEnd(LocalDateTime locationEnd) { this.locationEnd = locationEnd; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public boolean isBanned() { return banned; }
    public void setBanned(boolean banned) { this.banned = banned; }
    public int getVendeurId() { return vendeurId; }
    public void setVendeurId(int vendeurId) { this.vendeurId = vendeurId; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    @Override
    public String toString() {
        return "[" + type.toUpperCase() + "] " + nom + " - " + prix + " TND";
    }
}
