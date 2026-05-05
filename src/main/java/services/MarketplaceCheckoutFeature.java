package services;

import entities.CartItem;
import entities.Produit;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import utils.MarketplaceValidator;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public class MarketplaceCheckoutFeature {

    public interface Host {
        int getCurrentUserId();
        Produit findLatestProduitById(int produitId);
        String normalizeText(String input);
        void applyDialogStylesheet(DialogPane pane);
        void showToast(String title, String message, boolean success);
        void showSqlAlert(SQLException exception);
        void onCheckoutSuccess(int commandeId, int itemCount, double totalAmount);
    }

    private final CommandeService commandeService;
    private final CartSessionService cartSessionService;
    private final MarketplaceInvoicePdfService invoicePdfService = new MarketplaceInvoicePdfService();
    private final PaymentGatewayService paymentGatewayService = new PaymentGatewayService();

    public MarketplaceCheckoutFeature(CommandeService commandeService, CartSessionService cartSessionService) {
        this.commandeService = commandeService;
        this.cartSessionService = cartSessionService;
    }

    public void openCheckoutDialog(List<CartItem> cartItems, Host host) {
        if (cartItems == null || cartItems.isEmpty()) {
            host.showToast("Panier", "Votre panier est vide.", false);
            return;
        }

        for (CartItem item : cartItems) {
            Produit cartProduit = item == null ? null : item.getProduit();
            if (cartProduit == null) {
                host.showToast("Panier", "Un article du panier est invalide.", false);
                return;
            }

            if (cartProduit.getVendeurId() > 0 && cartProduit.getVendeurId() == host.getCurrentUserId()) {
                host.showToast("Panier", "Vous ne pouvez pas commander vos propres produits.", false);
                return;
            }

            Produit latest = host.findLatestProduitById(cartProduit.getId());
            if (latest == null) {
                host.showToast("Stock", "Le produit '" + host.normalizeText(safe(cartProduit.getNom())) + "' n'existe plus.", false);
                return;
            }

            if (item.getQuantite() > latest.getQuantiteStock()) {
                host.showToast(
                        "Stock",
                        "Stock insuffisant pour '" + host.normalizeText(safe(latest.getNom()))
                                + "' (stock: " + latest.getQuantiteStock() + ", demande: " + item.getQuantite() + ").",
                        false
                );
                return;
            }
        }

        Dialog<ButtonType> checkoutDialog = new Dialog<>();
        checkoutDialog.setTitle("Passer commande");
        checkoutDialog.getDialogPane().getStyleClass().add("checkout-dialog-pane");
        host.applyDialogStylesheet(checkoutDialog.getDialogPane());
        checkoutDialog.getDialogPane().setPrefWidth(560);
        checkoutDialog.getDialogPane().setPrefHeight(520);
        checkoutDialog.getDialogPane().getButtonTypes().setAll(ButtonType.CANCEL);

        int previewItemCount = 0;
        double previewTotal = 0.0;
        for (CartItem cartItem : cartItems) {
            if (cartItem == null) {
                continue;
            }
            previewItemCount += Math.max(0, cartItem.getQuantite());
            previewTotal += cartItem.getLineTotal();
        }

        ComboBox<String> modePaiementBox = new ComboBox<>();
        modePaiementBox.getItems().setAll("domicile", "carte");
        modePaiementBox.setValue("domicile");

        ComboBox<String> modeLivraisonBox = new ComboBox<>();
        modeLivraisonBox.getItems().setAll("Retrait sur place", "Livraison a domicile");
        modeLivraisonBox.setValue("Retrait sur place");

        TextField adresseField = new TextField();
        adresseField.setPromptText("Adresse de livraison");
        adresseField.setDisable(true);

        TextField cardNumberField = new TextField();
        cardNumberField.setPromptText("Numero de carte");
        cardNumberField.setDisable(true);

        TextField expMonthField = new TextField();
        expMonthField.setPromptText("MM");
        expMonthField.setDisable(true);

        TextField expYearField = new TextField();
        expYearField.setPromptText("YY ou YYYY");
        expYearField.setDisable(true);

        TextField cvcField = new TextField();
        cvcField.setPromptText("CVC");
        cvcField.setDisable(true);

        TextField cardHolderField = new TextField();
        cardHolderField.setPromptText("Titulaire de la carte");
        cardHolderField.setDisable(true);

        modeLivraisonBox.getStyleClass().add("checkout-field");
        modePaiementBox.getStyleClass().add("checkout-field");
        adresseField.getStyleClass().add("checkout-field");
        cardNumberField.getStyleClass().add("checkout-field");
        expMonthField.getStyleClass().add("checkout-field");
        expYearField.getStyleClass().add("checkout-field");
        cvcField.getStyleClass().add("checkout-field");
        cardHolderField.getStyleClass().add("checkout-field");

        Label policyLabel = new Label("Modes de paiement disponibles: domicile ou carte.");
        policyLabel.getStyleClass().add("checkout-policy-label");

        Label cardHintLabel = new Label("Paiement carte: testez avec 4242 4242 4242 4242.");
        cardHintLabel.getStyleClass().add("checkout-helper-label");
        cardHintLabel.setVisible(false);
        cardHintLabel.setManaged(false);

        Label sectionTitle = new Label("Finalisez votre commande");
        sectionTitle.getStyleClass().add("checkout-title");

        Label sectionSubtitle = new Label("Choisissez le mode de paiement, puis confirmez.");
        sectionSubtitle.getStyleClass().add("checkout-subtitle");

        Label summaryLabel = new Label("Articles: " + previewItemCount + "  |  Total: " + String.format("%.2f TND", previewTotal));
        summaryLabel.getStyleClass().add("checkout-summary-label");

        Label deliveryHintLabel = new Label("Retrait: adresse non requise.");
        deliveryHintLabel.getStyleClass().add("checkout-helper-label");

        GridPane grid = new GridPane();
        grid.getStyleClass().add("checkout-grid");
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(18));
        grid.add(new Label("Mode livraison"), 0, 0);
        grid.add(modeLivraisonBox, 1, 0);
        grid.add(new Label("Mode paiement"), 0, 1);
        grid.add(modePaiementBox, 1, 1);
        grid.add(new Label("Adresse"), 0, 2);
        grid.add(adresseField, 1, 2);
        grid.add(new Label("Numero carte"), 0, 3);
        grid.add(cardNumberField, 1, 3);

        HBox cardMetaBox = new HBox(8, expMonthField, expYearField, cvcField);
        grid.add(new Label("Expiration / CVC"), 0, 4);
        grid.add(cardMetaBox, 1, 4);

        grid.add(new Label("Titulaire"), 0, 5);
        grid.add(cardHolderField, 1, 5);

        grid.add(cardHintLabel, 1, 6);
        grid.add(deliveryHintLabel, 1, 7);
        grid.add(policyLabel, 0, 8, 2, 1);

        // Error display area (initially hidden)
        Label errorDisplay = new Label();
        errorDisplay.setStyle("-fx-text-fill: #cc0000; -fx-font-size: 11; -fx-wrap-text: true;");
        errorDisplay.setVisible(false);
        errorDisplay.setManaged(false);
        errorDisplay.setMaxWidth(Double.MAX_VALUE);

        Button validateInlineButton = new Button("Valider");
        validateInlineButton.getStyleClass().add("checkout-save-btn");

        Button closeInlineButton = new Button("Fermer");
        closeInlineButton.getStyleClass().add("checkout-cancel-btn");
        closeInlineButton.setOnAction(e -> {
            checkoutDialog.setResult(ButtonType.CANCEL);
            checkoutDialog.close();
        });

        HBox inlineActions = new HBox(10, validateInlineButton, closeInlineButton);
        inlineActions.setAlignment(Pos.CENTER_RIGHT);
        inlineActions.getStyleClass().add("checkout-inline-actions");

        VBox content = new VBox(10, sectionTitle, sectionSubtitle, summaryLabel, grid, errorDisplay, inlineActions);
        content.getStyleClass().add("checkout-content-shell");
        checkoutDialog.getDialogPane().setContent(content);

        // Helper function to display errors in the modal
        java.util.function.Consumer<String> showErrorInModal = (errorMsg) -> {
            errorDisplay.setText("⚠ " + errorMsg);
            errorDisplay.setVisible(true);
            errorDisplay.setManaged(true);
        };

        // Helper function to clear errors
        Runnable clearErrorInModal = () -> {
            errorDisplay.setText("");
            errorDisplay.setVisible(false);
            errorDisplay.setManaged(false);
        };

        Node hiddenCancelNode = checkoutDialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        if (hiddenCancelNode != null) {
            hiddenCancelNode.setVisible(false);
            hiddenCancelNode.setManaged(false);
        }
        checkoutDialog.setOnShown(e -> {
            Node cancelNodeOnShow = checkoutDialog.getDialogPane().lookupButton(ButtonType.CANCEL);
            if (cancelNodeOnShow != null) {
                cancelNodeOnShow.setVisible(false);
                cancelNodeOnShow.setManaged(false);
            }
        });

        Runnable updateCheckoutValidation = () -> {
            boolean homeDelivery = "Livraison a domicile".equals(modeLivraisonBox.getValue());
            adresseField.setDisable(!homeDelivery);
            if (!homeDelivery) {
                adresseField.clear();
            }

            boolean cardPayment = "carte".equalsIgnoreCase(safe(modePaiementBox.getValue()).trim());
            cardNumberField.setDisable(!cardPayment);
            expMonthField.setDisable(!cardPayment);
            expYearField.setDisable(!cardPayment);
            cvcField.setDisable(!cardPayment);
            cardHolderField.setDisable(!cardPayment);
            cardHintLabel.setVisible(cardPayment);
            cardHintLabel.setManaged(cardPayment);

            if (!cardPayment) {
                cardNumberField.clear();
                expMonthField.clear();
                expYearField.clear();
                cvcField.clear();
                cardHolderField.clear();
            }

            String modePaiement = safe(modePaiementBox.getValue()).trim();
            String adresse = safe(adresseField.getText()).trim();
            boolean hasValidAddress = !homeDelivery || !adresse.isEmpty();

            boolean cardInputsOk = true;
            if (cardPayment) {
                cardInputsOk = !safe(cardNumberField.getText()).isBlank()
                        && !safe(expMonthField.getText()).isBlank()
                        && !safe(expYearField.getText()).isBlank()
                        && !safe(cvcField.getText()).isBlank();
            }

            validateInlineButton.setDisable(modePaiement.isEmpty() || !hasValidAddress || !cardInputsOk);

            if (homeDelivery) {
                deliveryHintLabel.setText("Livraison: indiquez une adresse complete.");
            } else {
                deliveryHintLabel.setText("Retrait: adresse non requise.");
            }
        };

        modeLivraisonBox.valueProperty().addListener((obs, oldValue, newValue) -> updateCheckoutValidation.run());
        modePaiementBox.valueProperty().addListener((obs, oldValue, newValue) -> updateCheckoutValidation.run());
        adresseField.textProperty().addListener((obs, oldValue, newValue) -> updateCheckoutValidation.run());
        cardNumberField.textProperty().addListener((obs, oldValue, newValue) -> updateCheckoutValidation.run());
        expMonthField.textProperty().addListener((obs, oldValue, newValue) -> updateCheckoutValidation.run());
        expYearField.textProperty().addListener((obs, oldValue, newValue) -> updateCheckoutValidation.run());
        cvcField.textProperty().addListener((obs, oldValue, newValue) -> updateCheckoutValidation.run());
        updateCheckoutValidation.run();

        validateInlineButton.setOnAction(event -> {
            boolean homeDelivery = "Livraison a domicile".equals(modeLivraisonBox.getValue());
            String modePaiement = safe(modePaiementBox.getValue()).trim();
            String adresse = homeDelivery ? safe(adresseField.getText()).trim() : "Retrait sur place";

            String checkoutError = MarketplaceValidator.validateCheckout(
                    modeLivraisonBox.getValue(),
                    modePaiement,
                    adresse
            );
            if (checkoutError != null) {
                showErrorInModal.accept(checkoutError);
                return;
            }

            // Pre-validate payment configuration if using card payment
            if ("carte".equalsIgnoreCase(modePaiement)) {
                String configError = paymentGatewayService.validateConfiguration();
                if (!configError.isBlank()) {
                    showErrorInModal.accept("Configuration Stripe: " + configError);
                    return;
                }
            }

            String cardValidationError = null;
            PaymentGatewayService.CardInput cardInput = null;
            if ("carte".equalsIgnoreCase(modePaiement)) {
                String cardNumber = safe(cardNumberField.getText()).trim();
                String expMonth = safe(expMonthField.getText()).trim();
                String expYear = safe(expYearField.getText()).trim();
                String cvc = safe(cvcField.getText()).trim();
                String holder = safe(cardHolderField.getText()).trim();

                cardValidationError = MarketplaceValidator.validateCardFields(cardNumber, expMonth, expYear, cvc);
                if (cardValidationError != null) {
                    showErrorInModal.accept(cardValidationError);
                    return;
                }
                cardInput = new PaymentGatewayService.CardInput(cardNumber, expMonth, expYear, cvc, holder);
            }

            try {
                clearErrorInModal.run();
                
                int itemCount = 0;
                double totalAmount = 0.0;
                for (CartItem cartItem : cartItems) {
                    if (cartItem == null) {
                        continue;
                    }
                    itemCount += Math.max(0, cartItem.getQuantite());
                    totalAmount += cartItem.getLineTotal();
                }

                String paymentRef = null;
                LocalDateTime paidAt = null;
                String initialStatus = "en_attente";

                if ("carte".equalsIgnoreCase(modePaiement)) {
                    PaymentGatewayService.PaymentResult paymentResult = paymentGatewayService.chargeCard(
                            totalAmount,
                            host.getCurrentUserId(),
                            "Commande marketplace (" + itemCount + " articles)",
                            cardInput
                    );
                    if (!paymentResult.success()) {
                        // Display payment error in red in the modal
                        String errorMsg = paymentResult.errorMessage();
                        if (errorMsg.contains("STRIPE_SECRET_KEY")) {
                            showErrorInModal.accept("Configuration Stripe manquante. Contactez l'administrateur.");
                        } else if (errorMsg.contains("401") || errorMsg.contains("403")) {
                            showErrorInModal.accept("Les identifiants Stripe sont invalides. Verifiez la configuration.");
                        } else {
                            showErrorInModal.accept(errorMsg);
                        }
                        return;
                    }
                    paymentRef = paymentResult.paymentReference();
                    paidAt = LocalDateTime.now();
                    initialStatus = "en_attente";
                }

                int commandeId = commandeService.createCommandeFromCart(
                        host.getCurrentUserId(),
                        modePaiement,
                        adresse,
                        cartItems,
                        paymentRef,
                        paidAt,
                        initialStatus
                );

                cartSessionService.clear(host.getCurrentUserId());
                host.onCheckoutSuccess(commandeId, itemCount, totalAmount);
                if (paymentRef != null && !paymentRef.isBlank()) {
                    host.showToast("Paiement", "Paiement confirme. Reference: " + paymentRef, true);
                }
                host.showToast("Commande", "Commande validee avec succes.", true);

                checkoutDialog.setTitle("Commande validee");
                checkoutDialog.getDialogPane().setContent(
                        buildCheckoutSuccessPane(checkoutDialog, host, commandeId, modePaiement, adresse, itemCount, totalAmount)
                );
            } catch (SQLException e) {
                String sqlError = safe(e.getMessage());
                if (sqlError.isBlank()) {
                    sqlError = "Erreur base de donnees.";
                }
                showErrorInModal.accept("Erreur base de donnees: " + sqlError);
                host.showSqlAlert(e);
            } catch (Exception e) {
                String details = safe(e.getMessage());
                if (details.isBlank()) {
                    details = e.getClass().getSimpleName();
                }
                showErrorInModal.accept("Erreur inattendue: " + details);
            } catch (Throwable t) {
                showErrorInModal.accept("Erreur technique: " + t.getClass().getSimpleName());
            }
        });

        checkoutDialog.showAndWait();
    }

    private VBox buildCheckoutSuccessPane(Dialog<ButtonType> checkoutDialog, Host host, int commandeId, String modePaiement,
                                          String adresse, int itemCount, double totalAmount) {
        Label icon = new Label("OK");
        icon.getStyleClass().add("checkout-success-icon");

        Label title = new Label("Felicitations, commande validee");
        title.getStyleClass().add("checkout-success-title");

        Label subtitle = new Label("Votre commande a ete enregistree avec succes.");
        subtitle.getStyleClass().add("checkout-success-subtitle");

        GridPane summary = new GridPane();
        summary.getStyleClass().add("checkout-success-grid");
        summary.setHgap(10);
        summary.setVgap(8);
        summary.add(new Label("Commande"), 0, 0);
        summary.add(new Label("#" + commandeId), 1, 0);
        summary.add(new Label("Paiement"), 0, 1);
        summary.add(new Label(resolvePaymentLabel(modePaiement)), 1, 1);
        summary.add(new Label("Adresse"), 0, 2);
        summary.add(new Label(adresse), 1, 2);
        summary.add(new Label("Articles"), 0, 3);
        summary.add(new Label(String.valueOf(itemCount)), 1, 3);
        summary.add(new Label("Montant total"), 0, 4);
        summary.add(new Label(String.format("%.2f TND", totalAmount)), 1, 4);

        Button downloadInvoiceButton = new Button("Telecharger facture PDF");
        downloadInvoiceButton.getStyleClass().add("checkout-cancel-btn");
        downloadInvoiceButton.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Enregistrer la facture");
            chooser.setInitialFileName("facture_commande_" + commandeId + ".pdf");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));

            var ownerWindow = checkoutDialog.getDialogPane() == null || checkoutDialog.getDialogPane().getScene() == null
                    ? null
                    : checkoutDialog.getDialogPane().getScene().getWindow();
            var selected = chooser.showSaveDialog(ownerWindow);
            if (selected == null) {
                return;
            }

            MarketplaceInvoicePdfService.InvoiceData invoice = new MarketplaceInvoicePdfService.InvoiceData();
            invoice.commandeId = commandeId;
            entities.User currentUser = utils.SessionManager.getInstance().getCurrentUser();
            invoice.clientName = currentUser != null ? currentUser.getFirstName() + " " + currentUser.getLastName() : "Client " + host.getCurrentUserId();
            invoice.paymentMode = resolvePaymentLabel(modePaiement);
            invoice.deliveryAddress = adresse;
            invoice.itemCount = itemCount;
            invoice.totalAmount = totalAmount;
            invoice.issuedAt = LocalDateTime.now();

            try {
                invoicePdfService.generateInvoice(selected.toPath(), invoice);
                host.showToast("Facture", "Facture PDF enregistree.", true);
            } catch (Exception ex) {
                host.showToast("Facture", "Echec generation PDF: " + ex.getMessage(), false);
            }
        });

        Button closeSuccessButton = new Button("Fermer");
        closeSuccessButton.getStyleClass().add("checkout-save-btn");
        closeSuccessButton.setOnAction(e -> {
            checkoutDialog.setResult(ButtonType.CANCEL);
            checkoutDialog.close();
        });

        HBox actions = new HBox(10, downloadInvoiceButton, closeSuccessButton);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.getStyleClass().add("checkout-inline-actions");

        VBox shell = new VBox(10, icon, title, subtitle, summary, actions);
        shell.getStyleClass().add("checkout-success-shell");
        return shell;
    }

    private String resolvePaymentLabel(String rawPayment) {
        String value = safe(rawPayment).trim().toLowerCase();
        if ("carte".equals(value)) {
            return "Carte";
        }
        if ("domicile".equals(value)) {
            return "Domicile";
        }
        return "Domicile";
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
