package entities;

import java.time.LocalDateTime;

public class Commande {
    private int id;
    private String statut;
    private String modePaiement;
    private String adresseLivraison;
    private double montantTotal;
    private String paymentRef;
    private LocalDateTime paidAt;
    private LocalDateTime emailSentAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int clientId;

    public Commande() {}

    public Commande(String statut, String modePaiement, String adresseLivraison,
                    double montantTotal, int clientId) {
        this.statut = statut;
        this.modePaiement = modePaiement;
        this.adresseLivraison = adresseLivraison;
        this.montantTotal = montantTotal;
        this.clientId = clientId;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
    public String getModePaiement() { return modePaiement; }
    public void setModePaiement(String modePaiement) { this.modePaiement = modePaiement; }
    public String getAdresseLivraison() { return adresseLivraison; }
    public void setAdresseLivraison(String adresseLivraison) { this.adresseLivraison = adresseLivraison; }
    public double getMontantTotal() { return montantTotal; }
    public void setMontantTotal(double montantTotal) { this.montantTotal = montantTotal; }
    public String getPaymentRef() { return paymentRef; }
    public void setPaymentRef(String paymentRef) { this.paymentRef = paymentRef; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
    public LocalDateTime getEmailSentAt() { return emailSentAt; }
    public void setEmailSentAt(LocalDateTime emailSentAt) { this.emailSentAt = emailSentAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public int getClientId() { return clientId; }
    public void setClientId(int clientId) { this.clientId = clientId; }

    @Override
    public String toString() {
        return "Commande #" + id + " - " + statut + " - " + montantTotal + " TND";
    }
}
