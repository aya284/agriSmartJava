package controllers.marketplace;

import entities.Commande;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import services.CommandeService;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

/**
 * Extracted feature for order/commande management (add, edit, delete, status updates)
 */
public class MarketplaceOrderManagementFeature {
    private final CommandeService commandeService;
    private final TableView<Commande> commandeTable;
    private final Label statusLabel;
    private final Consumer<SQLException> showSqlAlert;
    private final BiConsumer<String, Boolean> showToast;
    private final BiConsumer<String, String> showAlert;
    private final IntSupplier currentUserIdSupplier;
    private final java.util.function.Function<String, String> normalizeText;
    private final Runnable onOrderChanged;
    private boolean showingBoughtOrders = false;

    public MarketplaceOrderManagementFeature(
            CommandeService commandeService,
            TableView<Commande> commandeTable,
            Label statusLabel,
            Consumer<SQLException> showSqlAlert,
            BiConsumer<String, Boolean> showToast,
            BiConsumer<String, String> showAlert,
            IntSupplier currentUserIdSupplier,
            java.util.function.Function<String, String> normalizeText,
            Runnable onOrderChanged) {
        this.commandeService = commandeService;
        this.commandeTable = commandeTable;
        this.statusLabel = statusLabel;
        this.showSqlAlert = showSqlAlert;
        this.showToast = showToast;
        this.showAlert = showAlert;
        this.currentUserIdSupplier = currentUserIdSupplier;
        this.normalizeText = normalizeText;
        this.onOrderChanged = onOrderChanged;
    }

    public void setShowingBoughtOrders(boolean showing) {
        this.showingBoughtOrders = showing;
    }

    public void loadCommandes() {
        if (commandeTable == null) {
            return;
        }
        int userId = currentUserIdSupplier.getAsInt();
        try {
            List<Commande> commandes;
            if (showingBoughtOrders) {
                commandes = commandeService.getByClient(userId);
            } else {
                commandes = commandeService.getBySeller(userId);
            }
            commandeTable.setItems(FXCollections.observableArrayList(commandes));
        } catch (SQLException e) {
            showSqlAlert.accept(e);
        }
    }

    public void openAjouterCommande() {
        Dialog<Commande> dialog = buildCommandeDialog(null);
        dialog.showAndWait().ifPresent(c -> {
            try {
                commandeService.ajouter(c);
                loadCommandes();
                showToast.accept("Commande ajoutee.", true);
                onOrderChanged.run();
            } catch (SQLException e) {
                showSqlAlert.accept(e);
            }
        });
    }

    public void openModifierCommande(Commande commande) {
        Dialog<Commande> dialog = buildCommandeDialog(commande);
        dialog.showAndWait().ifPresent(c -> {
            c.setId(commande.getId());
            try {
                String currentStatus = safe(commande.getStatut()).trim().toLowerCase();
                String nextStatus = safe(c.getStatut()).trim().toLowerCase();
                CommandeService.OrderActor actor = showingBoughtOrders
                        ? CommandeService.OrderActor.BUYER
                        : CommandeService.OrderActor.SELLER;

                if (!currentStatus.equals(nextStatus)) {
                    commandeService.updateStatut(
                            commande.getId(),
                            nextStatus,
                            currentUserIdSupplier.getAsInt(),
                            actor,
                            "Mise a jour depuis Marketplace"
                    );
                }
                
                loadCommandes();
                showToast.accept("Statut mis a jour.", true);
                onOrderChanged.run();
            } catch (SQLException e) {
                showSqlAlert.accept(e);
            }
        });
    }

    public void handleDeleteCommande(Commande commande) {
        if (commande == null) {
            return;
        }
        if (!confirm("Supprimer commande", "Supprimer la commande #" + commande.getId() + " ?")) {
            return;
        }
        try {
            commandeService.supprimer(commande.getId());
            loadCommandes();
            showToast.accept("Commande supprimee.", true);
            onOrderChanged.run();
        } catch (SQLException e) {
            showSqlAlert.accept(e);
        }
    }

    public void downloadFacture(Commande commande) {
        if (commande == null) {
            return;
        }
        showToast.accept("Facture #" + commande.getId() + " telecharge.", true);
    }

    private Dialog<Commande> buildCommandeDialog(Commande existing) {
        Dialog<Commande> dialog = new Dialog<>();
        boolean isEditing = existing != null;
        dialog.setTitle(isEditing ? "Mettre a jour statut" : "Ajouter Commande");
        dialog.getDialogPane().getStyleClass().add("commande-dialog-pane");
        ButtonType saveBtn = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        ComboBox<String> statutField = new ComboBox<>();
        if (!isEditing) {
            statutField.getItems().setAll("en_attente", "confirmee", "livree", "annulee");
            statutField.setValue("en_attente");
        } else {
            String currentStatus = safe(existing.getStatut()).trim().toLowerCase();
            CommandeService.OrderActor actor = showingBoughtOrders
                    ? CommandeService.OrderActor.BUYER
                    : CommandeService.OrderActor.SELLER;

            List<String> statusOptions = new ArrayList<>();
            statusOptions.add(currentStatus);
            for (String candidate : commandeService.getAllowedTransitions(currentStatus, actor)) {
                if (!candidate.equals(currentStatus)) {
                    statusOptions.add(candidate);
                }
            }

            statutField.getItems().setAll(statusOptions);
            statutField.setValue(currentStatus);
        }

        ComboBox<String> paiementField = new ComboBox<>();
        paiementField.getItems().setAll("domicile", "carte");
        paiementField.setValue(existing == null || safe(existing.getModePaiement()).isBlank() ? "domicile" : existing.getModePaiement());

        TextField adresseField = new TextField(existing == null ? "" : safe(existing.getAdresseLivraison()));
        TextField montantField = new TextField(existing == null ? "0" : String.valueOf(existing.getMontantTotal()));
        TextField paymentRefField = new TextField(existing == null ? "" : safe(existing.getPaymentRef()));
        TextField clientIdField = new TextField(existing == null ? String.valueOf(currentUserIdSupplier.getAsInt()) : String.valueOf(existing.getClientId()));

        if (isEditing) {
            paiementField.setDisable(true);
            adresseField.setDisable(true);
            montantField.setDisable(true);
            paymentRefField.setDisable(true);
            clientIdField.setDisable(true);
        }

        statutField.getStyleClass().add("commande-form-field");
        paiementField.getStyleClass().add("commande-form-field");
        adresseField.getStyleClass().add("commande-form-field");
        montantField.getStyleClass().add("commande-form-field");
        paymentRefField.getStyleClass().add("commande-form-field");
        clientIdField.getStyleClass().add("commande-form-field");

        GridPane grid = new GridPane();
        grid.getStyleClass().add("commande-dialog-grid");
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(18));

        grid.add(new Label("Statut"), 0, 0); grid.add(statutField, 1, 0);
        grid.add(new Label("Mode paiement"), 0, 1); grid.add(paiementField, 1, 1);
        grid.add(new Label("Adresse"), 0, 2); grid.add(adresseField, 1, 2);
        grid.add(new Label("Montant"), 0, 3); grid.add(montantField, 1, 3);
        grid.add(new Label("Payment ref"), 0, 4); grid.add(paymentRefField, 1, 4);
        grid.add(new Label("Client ID"), 0, 5); grid.add(clientIdField, 1, 5);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                Commande result = isEditing ? new Commande() : new Commande();
                result.setStatut(statutField.getValue());
                result.setModePaiement(paiementField.getValue());
                result.setAdresseLivraison(adresseField.getText());
                result.setMontantTotal(parseDouble(montantField.getText()));
                result.setPaymentRef(paymentRefField.getText());
                result.setClientId(parseInteger(clientIdField.getText()));
                return result;
            }
            return null;
        });

        return dialog;
    }

    private String resolveStatusLabel(String rawStatus) {
        String value = safe(rawStatus).trim().toLowerCase();
        if ("confirmee".equals(value)) return "Confirmee";
        if ("livree".equals(value)) return "Livree";
        if ("annulee".equals(value)) return "Annulee";
        return "En attente";
    }

    private String resolveStatusStyle(String rawStatus) {
        String value = safe(rawStatus).trim().toLowerCase();
        if ("confirmee".equals(value)) return "confirmed";
        if ("livree".equals(value)) return "delivered";
        if ("annulee".equals(value)) return "cancelled";
        return "pending";
    }

    private String resolvePaymentLabel(String rawPayment) {
        String value = safe(rawPayment).trim().toLowerCase();
        if ("carte".equals(value)) return "Carte bancaire";
        if ("domicile".equals(value)) return "A domicile";
        return "Domicile";
    }

    private String resolvePaymentStyle(String rawPayment) {
        String value = safe(rawPayment).trim().toLowerCase();
        return "carte".equals(value) ? "card" : "home";
    }

    private boolean confirm(String title, String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    private Integer parseInteger(String raw) {
        try {
            return Integer.parseInt(raw == null ? "" : raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Double parseDouble(String raw) {
        try {
            return Double.parseDouble(raw == null ? "" : raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
