package entities;

public class CartItem {
    private final Produit produit;
    private int quantite;

    public CartItem(Produit produit, int quantite) {
        this.produit = produit;
        this.quantite = Math.max(1, quantite);
    }

    public Produit getProduit() {
        return produit;
    }

    public int getQuantite() {
        return quantite;
    }

    public void setQuantite(int quantite) {
        this.quantite = Math.max(1, quantite);
    }

    public void increment(int delta) {
        this.quantite = Math.max(1, this.quantite + delta);
    }

    public double getUnitPrice() {
        if (produit == null) {
            return 0.0;
        }
        if (produit.isPromotion() && produit.getPromotionPrice() > 0) {
            return produit.getPromotionPrice();
        }
        return produit.getPrix();
    }

    public double getLineTotal() {
        return getUnitPrice() * quantite;
    }
}
