package controllers;

import entities.Commande;
import entities.MarketplaceMessage;
import entities.Produit;
import entities.WishlistItem;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import services.CommandeService;
import services.MarketplaceMessageService;
import services.ProduitService;
import services.WishlistService;

import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class MarketplaceController implements Initializable {

    @FXML private TableView<Produit> produitTable;
    @FXML private TableColumn<Produit, Integer> colId;
    @FXML private TableColumn<Produit, String> colNom;
    @FXML private TableColumn<Produit, String> colType;
    @FXML private TableColumn<Produit, String> colCategorie;
    @FXML private TableColumn<Produit, Double> colPrix;
    @FXML private TableColumn<Produit, Integer> colStock;
    @FXML private TableColumn<Produit, Boolean> colPromo;
    @FXML private TableColumn<Produit, Void> colActions;

    @FXML private TableView<Commande> commandeTable;
    @FXML private TableColumn<Commande, Integer> colCmdId;
    @FXML private TableColumn<Commande, String> colCmdStatut;
    @FXML private TableColumn<Commande, Double> colCmdMontant;
    @FXML private TableColumn<Commande, String> colCmdPaiement;
    @FXML private TableColumn<Commande, Void> colCmdActions;

    @FXML private TableView<WishlistItem> wishlistTable;
    @FXML private TableColumn<WishlistItem, Integer> colWishId;
    @FXML private TableColumn<WishlistItem, Integer> colWishUser;
    @FXML private TableColumn<WishlistItem, Integer> colWishProduit;
    @FXML private TableColumn<WishlistItem, Void> colWishActions;

    @FXML private TableView<MarketplaceMessage> messageTable;
    @FXML private TableColumn<MarketplaceMessage, Integer> colMsgId;
    @FXML private TableColumn<MarketplaceMessage, Integer> colMsgConversation;
    @FXML private TableColumn<MarketplaceMessage, Integer> colMsgSender;
    @FXML private TableColumn<MarketplaceMessage, String> colMsgContent;
    @FXML private TableColumn<MarketplaceMessage, Boolean> colMsgRead;
    @FXML private TableColumn<MarketplaceMessage, Void> colMsgActions;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> typeFilterCombo;
    @FXML private ComboBox<String> promoFilterCombo;
    @FXML private Label statusLabel;

    @FXML private Label statProduits;
    @FXML private Label statCommandes;
    @FXML private Label statWishlist;
    @FXML private Label statMessages;

    private final ProduitService produitService = new ProduitService();
    private final CommandeService commandeService = new CommandeService();
    private final WishlistService wishlistService = new WishlistService();
    private final MarketplaceMessageService messageService = new MarketplaceMessageService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupProduitTable();
        setupCommandeTable();
        setupWishlistTable();
        setupMessageTable();
        setupFilters();

        loadProduits();
        loadCommandes();
        loadWishlist();
        loadMessages();
        refreshStats();
    }

    private void setupFilters() {
        typeFilterCombo.getItems().setAll("Tous", "vente", "location");
        typeFilterCombo.setValue("Tous");

        promoFilterCombo.getItems().setAll("Tous", "En promotion", "Sans promotion");
        promoFilterCombo.setValue("Tous");
    }

    private void setupProduitTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colCategorie.setCellValueFactory(new PropertyValueFactory<>("categorie"));
        colPrix.setCellValueFactory(new PropertyValueFactory<>("prix"));
        colStock.setCellValueFactory(new PropertyValueFactory<>("quantiteStock"));
        colPromo.setCellValueFactory(new PropertyValueFactory<>("promotion"));

        produitTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit = new Button("Edit");
            private final Button btnDelete = new Button("Delete");
            private final HBox box = new HBox(8, btnEdit, btnDelete);

            {
                btnEdit.getStyleClass().add("btn-gold");
                btnDelete.getStyleClass().add("btn-danger");
                btnEdit.setOnAction(e -> openModifierProduit(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(e -> handleDeleteProduit(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    private void setupCommandeTable() {
        colCmdId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colCmdStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        colCmdMontant.setCellValueFactory(new PropertyValueFactory<>("montantTotal"));
        colCmdPaiement.setCellValueFactory(new PropertyValueFactory<>("modePaiement"));

        commandeTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        colCmdActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit = new Button("Edit");
            private final Button btnDelete = new Button("Delete");
            private final HBox box = new HBox(8, btnEdit, btnDelete);

            {
                btnEdit.getStyleClass().add("btn-gold");
                btnDelete.getStyleClass().add("btn-danger");
                btnEdit.setOnAction(e -> openModifierCommande(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(e -> handleDeleteCommande(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    private void setupWishlistTable() {
        colWishId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colWishUser.setCellValueFactory(new PropertyValueFactory<>("userId"));
        colWishProduit.setCellValueFactory(new PropertyValueFactory<>("produitId"));

        wishlistTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        colWishActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit = new Button("Edit");
            private final Button btnDelete = new Button("Delete");
            private final HBox box = new HBox(8, btnEdit, btnDelete);

            {
                btnEdit.getStyleClass().add("btn-gold");
                btnDelete.getStyleClass().add("btn-danger");
                btnEdit.setOnAction(e -> openModifierWishlist(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(e -> handleDeleteWishlist(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    private void setupMessageTable() {
        colMsgId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colMsgConversation.setCellValueFactory(new PropertyValueFactory<>("conversationId"));
        colMsgSender.setCellValueFactory(new PropertyValueFactory<>("senderId"));
        colMsgContent.setCellValueFactory(new PropertyValueFactory<>("content"));
        colMsgRead.setCellValueFactory(new PropertyValueFactory<>("read"));

        messageTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        colMsgActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit = new Button("Edit");
            private final Button btnDelete = new Button("Delete");
            private final HBox box = new HBox(8, btnEdit, btnDelete);

            {
                btnEdit.getStyleClass().add("btn-gold");
                btnDelete.getStyleClass().add("btn-danger");
                btnEdit.setOnAction(e -> openModifierMessage(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(e -> handleDeleteMessage(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    private void loadProduits() {
        try {
            List<Produit> produits = produitService.afficher();
            produitTable.setItems(FXCollections.observableArrayList(produits));
            statusLabel.setText(produits.size() + " produits charges");
        } catch (SQLException e) {
            showSqlAlert(e);
        }
    }

    private void loadCommandes() {
        try {
            List<Commande> commandes = commandeService.afficher();
            commandeTable.setItems(FXCollections.observableArrayList(commandes));
        } catch (SQLException e) {
            showSqlAlert(e);
        }
    }

    private void loadWishlist() {
        try {
            List<WishlistItem> items = wishlistService.afficher();
            wishlistTable.setItems(FXCollections.observableArrayList(items));
        } catch (SQLException e) {
            showSqlAlert(e);
        }
    }

    private void loadMessages() {
        try {
            List<MarketplaceMessage> messages = messageService.afficher();
            messageTable.setItems(FXCollections.observableArrayList(messages));
        } catch (SQLException e) {
            showSqlAlert(e);
        }
    }

    private void refreshStats() {
        try {
            statProduits.setText(String.valueOf(produitService.countAll()));
            statCommandes.setText(String.valueOf(commandeService.countAll()));
            statWishlist.setText(String.valueOf(wishlistService.countAll()));
            statMessages.setText(String.valueOf(messageService.countAll()));
        } catch (SQLException e) {
            showSqlAlert(e);
        }
    }

    @FXML
    public void handleSearch() {
        applyProduitFilters();
    }

    @FXML
    public void handleFilterChange() {
        applyProduitFilters();
    }

    private void applyProduitFilters() {
        try {
            List<Produit> base = produitService.afficher();
            String keyword = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
            String typeFilter = typeFilterCombo.getValue();
            String promoFilter = promoFilterCombo.getValue();

            List<Produit> filtered = new ArrayList<>();
            for (Produit p : base) {
                boolean matchesKeyword = keyword.isEmpty()
                        || p.getNom().toLowerCase().contains(keyword)
                        || p.getCategorie().toLowerCase().contains(keyword);
                boolean matchesType = "Tous".equals(typeFilter) || p.getType().equalsIgnoreCase(typeFilter);
                boolean matchesPromo = "Tous".equals(promoFilter)
                        || ("En promotion".equals(promoFilter) && p.isPromotion())
                        || ("Sans promotion".equals(promoFilter) && !p.isPromotion());

                if (matchesKeyword && matchesType && matchesPromo) {
                    filtered.add(p);
                }
            }

            produitTable.setItems(FXCollections.observableArrayList(filtered));
            statusLabel.setText(filtered.size() + " resultat(s)");
        } catch (SQLException e) {
            showSqlAlert(e);
        }
    }

    @FXML
    public void openAjouterProduit() {
        Dialog<Produit> dialog = buildProduitDialog(null);
        Optional<Produit> result = dialog.showAndWait();
        result.ifPresent(p -> {
            try {
                produitService.ajouter(p);
                loadProduits();
                refreshStats();
            } catch (SQLException e) {
                showSqlAlert(e);
            }
        });
    }

    private void openModifierProduit(Produit produit) {
        Dialog<Produit> dialog = buildProduitDialog(produit);
        Optional<Produit> result = dialog.showAndWait();
        result.ifPresent(updated -> {
            updated.setId(produit.getId());
            try {
                produitService.modifier(updated);
                loadProduits();
            } catch (SQLException e) {
                showSqlAlert(e);
            }
        });
    }

    private Dialog<Produit> buildProduitDialog(Produit existing) {
        Dialog<Produit> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Ajouter Produit" : "Modifier Produit");

        ButtonType saveBtn = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        TextField nomField = new TextField(existing == null ? "" : existing.getNom());
        TextArea descriptionField = new TextArea(existing == null ? "" : existing.getDescription());
        descriptionField.setPrefRowCount(3);
        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll("vente", "location");
        typeCombo.setValue(existing == null ? "vente" : existing.getType());
        TextField prixField = new TextField(existing == null ? "" : String.valueOf(existing.getPrix()));
        TextField categorieField = new TextField(existing == null ? "" : existing.getCategorie());
        TextField stockField = new TextField(existing == null ? "" : String.valueOf(existing.getQuantiteStock()));
        CheckBox promoCheck = new CheckBox("Promotion");
        promoCheck.setSelected(existing != null && existing.isPromotion());
        TextField promoPriceField = new TextField(existing == null ? "0" : String.valueOf(existing.getPromotionPrice()));
        TextField locationAddressField = new TextField(existing == null ? "" : safe(existing.getLocationAddress()));
        TextField vendeurIdField = new TextField(existing == null ? "1" : String.valueOf(existing.getVendeurId()));

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(18));

        grid.add(new Label("Nom"), 0, 0); grid.add(nomField, 1, 0);
        grid.add(new Label("Description"), 0, 1); grid.add(descriptionField, 1, 1);
        grid.add(new Label("Type"), 0, 2); grid.add(typeCombo, 1, 2);
        grid.add(new Label("Prix"), 0, 3); grid.add(prixField, 1, 3);
        grid.add(new Label("Categorie"), 0, 4); grid.add(categorieField, 1, 4);
        grid.add(new Label("Stock"), 0, 5); grid.add(stockField, 1, 5);
        grid.add(new Label("Prix promo"), 0, 6); grid.add(promoPriceField, 1, 6);
        grid.add(promoCheck, 1, 7);
        grid.add(new Label("Adresse"), 0, 8); grid.add(locationAddressField, 1, 8);
        grid.add(new Label("Vendeur ID"), 0, 9); grid.add(vendeurIdField, 1, 9);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn != saveBtn) {
                return null;
            }

            Integer stock = parseInteger(stockField.getText());
            Double prix = parseDouble(prixField.getText());
            Double prixPromo = parseDouble(promoPriceField.getText());
            Integer vendeurId = parseInteger(vendeurIdField.getText());
            if (stock == null || prix == null || prixPromo == null || vendeurId == null || nomField.getText().isBlank()) {
                showAlert("Validation", "Champs invalides pour le produit.");
                return null;
            }

            Produit p = new Produit();
            p.setNom(nomField.getText().trim());
            p.setDescription(descriptionField.getText().trim());
            p.setType(typeCombo.getValue());
            p.setPrix(prix);
            p.setCategorie(categorieField.getText().trim());
            p.setQuantiteStock(stock);
            p.setImage("");
            p.setPromotion(promoCheck.isSelected());
            p.setPromotionPrice(prixPromo);
            p.setLocationAddress(locationAddressField.getText().trim());
            p.setBanned(false);
            p.setVendeurId(vendeurId);
            return p;
        });

        return dialog;
    }

    private void handleDeleteProduit(Produit produit) {
        if (!confirm("Supprimer produit", "Supprimer " + produit.getNom() + " ?")) {
            return;
        }
        try {
            produitService.supprimer(produit.getId());
            loadProduits();
            refreshStats();
        } catch (SQLException e) {
            showSqlAlert(e);
        }
    }

    @FXML
    public void openAjouterCommande() {
        Dialog<Commande> dialog = buildCommandeDialog(null);
        dialog.showAndWait().ifPresent(c -> {
            try {
                commandeService.ajouter(c);
                loadCommandes();
                refreshStats();
            } catch (SQLException e) {
                showSqlAlert(e);
            }
        });
    }

    private void openModifierCommande(Commande commande) {
        Dialog<Commande> dialog = buildCommandeDialog(commande);
        dialog.showAndWait().ifPresent(c -> {
            c.setId(commande.getId());
            try {
                commandeService.modifier(c);
                loadCommandes();
            } catch (SQLException e) {
                showSqlAlert(e);
            }
        });
    }

    private Dialog<Commande> buildCommandeDialog(Commande existing) {
        Dialog<Commande> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Ajouter Commande" : "Modifier Commande");
        ButtonType saveBtn = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        TextField statutField = new TextField(existing == null ? "en_attente" : existing.getStatut());
        TextField paiementField = new TextField(existing == null ? "carte" : existing.getModePaiement());
        TextField adresseField = new TextField(existing == null ? "" : safe(existing.getAdresseLivraison()));
        TextField montantField = new TextField(existing == null ? "0" : String.valueOf(existing.getMontantTotal()));
        TextField paymentRefField = new TextField(existing == null ? "" : safe(existing.getPaymentRef()));
        TextField clientIdField = new TextField(existing == null ? "1" : String.valueOf(existing.getClientId()));

        GridPane grid = new GridPane();
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
            if (btn != saveBtn) {
                return null;
            }
            Double montant = parseDouble(montantField.getText());
            Integer clientId = parseInteger(clientIdField.getText());
            if (montant == null || clientId == null || statutField.getText().isBlank()) {
                showAlert("Validation", "Champs invalides pour la commande.");
                return null;
            }
            Commande c = new Commande();
            c.setStatut(statutField.getText().trim());
            c.setModePaiement(paiementField.getText().trim());
            c.setAdresseLivraison(adresseField.getText().trim());
            c.setMontantTotal(montant);
            c.setPaymentRef(paymentRefField.getText().trim());
            c.setClientId(clientId);
            return c;
        });

        return dialog;
    }

    private void handleDeleteCommande(Commande commande) {
        if (!confirm("Supprimer commande", "Supprimer la commande #" + commande.getId() + " ?")) {
            return;
        }
        try {
            commandeService.supprimer(commande.getId());
            loadCommandes();
            refreshStats();
        } catch (SQLException e) {
            showSqlAlert(e);
        }
    }

    @FXML
    public void openAjouterWishlist() {
        Dialog<WishlistItem> dialog = buildWishlistDialog(null);
        dialog.showAndWait().ifPresent(item -> {
            try {
                wishlistService.ajouter(item);
                loadWishlist();
                refreshStats();
            } catch (SQLException e) {
                showSqlAlert(e);
            }
        });
    }

    private void openModifierWishlist(WishlistItem item) {
        Dialog<WishlistItem> dialog = buildWishlistDialog(item);
        dialog.showAndWait().ifPresent(updated -> {
            updated.setId(item.getId());
            try {
                wishlistService.modifier(updated);
                loadWishlist();
            } catch (SQLException e) {
                showSqlAlert(e);
            }
        });
    }

    private Dialog<WishlistItem> buildWishlistDialog(WishlistItem existing) {
        Dialog<WishlistItem> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Ajouter Wishlist" : "Modifier Wishlist");
        ButtonType saveBtn = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        TextField userIdField = new TextField(existing == null ? "1" : String.valueOf(existing.getUserId()));
        TextField produitIdField = new TextField(existing == null ? "1" : String.valueOf(existing.getProduitId()));

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(18));
        grid.add(new Label("User ID"), 0, 0); grid.add(userIdField, 1, 0);
        grid.add(new Label("Produit ID"), 0, 1); grid.add(produitIdField, 1, 1);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn != saveBtn) {
                return null;
            }
            Integer userId = parseInteger(userIdField.getText());
            Integer produitId = parseInteger(produitIdField.getText());
            if (userId == null || produitId == null) {
                showAlert("Validation", "IDs invalides pour la wishlist.");
                return null;
            }
            return new WishlistItem(userId, produitId);
        });

        return dialog;
    }

    private void handleDeleteWishlist(WishlistItem item) {
        if (!confirm("Supprimer wishlist", "Supprimer l'entree #" + item.getId() + " ?")) {
            return;
        }
        try {
            wishlistService.supprimer(item.getId());
            loadWishlist();
            refreshStats();
        } catch (SQLException e) {
            showSqlAlert(e);
        }
    }

    @FXML
    public void openAjouterMessage() {
        Dialog<MarketplaceMessage> dialog = buildMessageDialog(null);
        dialog.showAndWait().ifPresent(message -> {
            try {
                messageService.ajouter(message);
                loadMessages();
                refreshStats();
            } catch (SQLException e) {
                showSqlAlert(e);
            }
        });
    }

    private void openModifierMessage(MarketplaceMessage message) {
        Dialog<MarketplaceMessage> dialog = buildMessageDialog(message);
        dialog.showAndWait().ifPresent(updated -> {
            updated.setId(message.getId());
            try {
                messageService.modifier(updated);
                loadMessages();
            } catch (SQLException e) {
                showSqlAlert(e);
            }
        });
    }

    private Dialog<MarketplaceMessage> buildMessageDialog(MarketplaceMessage existing) {
        Dialog<MarketplaceMessage> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Ajouter Message" : "Modifier Message");
        ButtonType saveBtn = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        TextField conversationIdField = new TextField(existing == null ? "1" : String.valueOf(existing.getConversationId()));
        TextField senderIdField = new TextField(existing == null ? "1" : String.valueOf(existing.getSenderId()));
        TextArea contentField = new TextArea(existing == null ? "" : safe(existing.getContent()));
        contentField.setPrefRowCount(3);
        CheckBox readCheck = new CheckBox("Lu");
        readCheck.setSelected(existing != null && existing.isRead());
        TextField audioPathField = new TextField(existing == null ? "" : safe(existing.getAudioPath()));

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(18));

        grid.add(new Label("Conversation ID"), 0, 0); grid.add(conversationIdField, 1, 0);
        grid.add(new Label("Sender ID"), 0, 1); grid.add(senderIdField, 1, 1);
        grid.add(new Label("Content"), 0, 2); grid.add(contentField, 1, 2);
        grid.add(readCheck, 1, 3);
        grid.add(new Label("Audio path"), 0, 4); grid.add(audioPathField, 1, 4);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn != saveBtn) {
                return null;
            }
            Integer conversationId = parseInteger(conversationIdField.getText());
            Integer senderId = parseInteger(senderIdField.getText());
            if (conversationId == null || senderId == null || contentField.getText().isBlank()) {
                showAlert("Validation", "Champs invalides pour le message.");
                return null;
            }

            MarketplaceMessage message = new MarketplaceMessage();
            message.setConversationId(conversationId);
            message.setSenderId(senderId);
            message.setContent(contentField.getText().trim());
            message.setRead(readCheck.isSelected());
            message.setAudioPath(audioPathField.getText().trim());
            return message;
        });

        return dialog;
    }

    private void handleDeleteMessage(MarketplaceMessage message) {
        if (!confirm("Supprimer message", "Supprimer le message #" + message.getId() + " ?")) {
            return;
        }
        try {
            messageService.supprimer(message.getId());
            loadMessages();
            refreshStats();
        } catch (SQLException e) {
            showSqlAlert(e);
        }
    }

    private Integer parseInteger(String raw) {
        if (raw == null || raw.isBlank() || !raw.trim().matches("-?\\d+")) {
            return null;
        }
        return Integer.parseInt(raw.trim());
    }

    private Double parseDouble(String raw) {
        if (raw == null || raw.isBlank() || !raw.trim().matches("-?\\d+(\\.\\d+)?")) {
            return null;
        }
        return Double.parseDouble(raw.trim());
    }

    private boolean confirm(String title, String message) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle(title);
        confirm.setHeaderText(null);
        confirm.setContentText(message);
        Optional<ButtonType> result = confirm.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showSqlAlert(SQLException exception) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur SQL");
        alert.setHeaderText("Operation base de donnees echouee");
        alert.setContentText(exception.getMessage());
        alert.showAndWait();
        statusLabel.setText("Erreur SQL detectee");
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
