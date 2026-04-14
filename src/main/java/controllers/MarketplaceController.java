package controllers;

import entities.CartItem;
import entities.Commande;
import entities.MarketplaceMessage;
import entities.Produit;
import entities.WishlistItem;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Spinner;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.image.ImageView;
import javafx.scene.image.Image;
import javafx.geometry.Pos;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.layout.Priority;
import javafx.scene.layout.HBox;
import services.CommandeService;
import services.CartSessionService;
import services.MarketplaceConversationService;
import services.MarketplaceMessageService;
import services.ProduitService;
import services.WishlistService;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;

public class MarketplaceController implements Initializable {

    private static final String CATEGORY_PLACEHOLDER = "Choisir...";
    private static final String CATEGORY_ALL = "Toutes";
    private static final String TYPE_ALL = "Tous";
    private static final String TYPE_VENTE = "Vente";
    private static final String TYPE_LOCATION = "Location";
    private static final int PAGE_SIZE = 8;
    private static final int CURRENT_SESSION_USER_ID = 1;
    private static final int FALLBACK_SELLER_ID = 2;
    private static final DateTimeFormatter CHAT_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM HH:mm");
    private static final List<String> CATEGORIES = Arrays.asList(
            "Legumes",
            "Fruits",
            "Cereales",
            "Engrais",
            "Semences",
            "Materiel",
            "Irrigation",
            "Services",
            "Autre"
    );

    @FXML private BorderPane mainContent;
    @FXML private Button btnMessagingCenter;
    @FXML private Button btnWishlistCenter;

    @FXML private TableView<Produit> produitTable;
    @FXML private TableColumn<Produit, Integer> colId;
    @FXML private TableColumn<Produit, String> colNom;
    @FXML private TableColumn<Produit, String> colType;
    @FXML private TableColumn<Produit, String> colCategorie;
    @FXML private TableColumn<Produit, Double> colPrix;
    @FXML private TableColumn<Produit, Integer> colStock;
    @FXML private TableColumn<Produit, Boolean> colPromo;
    @FXML private TableColumn<Produit, Void> colActions;

    @FXML private FlowPane productGrid;
    @FXML private ComboBox<String> categorieFilter;
    @FXML private ComboBox<String> typeFilter;
    @FXML private ComboBox<String> sortCombo;
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
    @FXML private Button prevPageButton;
    @FXML private Button nextPageButton;
    @FXML private Label pageInfoLabel;

    @FXML private StackPane messagingOverlay;
    @FXML private VBox conversationListBox;
    @FXML private VBox messageListBox;
    @FXML private Label messagingTitleLabel;
    @FXML private Label messagingMetaLabel;
    @FXML private TextArea messageInputArea;

    @FXML private StackPane wishlistOverlay;
    @FXML private Label wishlistMetaLabel;
    @FXML private FlowPane wishlistGrid;

    

    private final ProduitService produitService = new ProduitService();
    private final CommandeService commandeService = new CommandeService();
    private final CartSessionService cartSessionService = CartSessionService.getInstance();
    private final MarketplaceConversationService conversationService = new MarketplaceConversationService();
    private final WishlistService wishlistService = new WishlistService();
    private final MarketplaceMessageService messageService = new MarketplaceMessageService();
    private List<Produit> filteredProduits = new ArrayList<>();
    private final Set<Integer> wishlistProductIds = new HashSet<>();
    private int currentPage = 0;

    // Dynamic Modal Fields
    @FXML private StackPane modalOverlay;
    @FXML private StackPane sellerOverlay;
    @FXML private Label modalTitle;
    @FXML private TextField fldNom;
    @FXML private TextArea fldDescription;
    @FXML private TextField fldPrix;
    @FXML private TextField fldStock;
    @FXML private ComboBox<String> fldCategorie;
    @FXML private TextField fldImage;
    @FXML private ComboBox<String> fldType;
    @FXML private CheckBox chkPromo;
    @FXML private TextField fldPromoPrice;

    @FXML private Label publishPreviewCategory;
    @FXML private Label publishPreviewTypeBadge;
    @FXML private Label publishPreviewTitle;
    @FXML private Label publishPreviewPrice;
    @FXML private Label publishPreviewStock;
    @FXML private Label publishPreviewLocation;
    @FXML private Label publishPreviewDescription;
    @FXML private ImageView publishPreviewImage;

    // Dynamic Details Fields
    @FXML private StackPane detailsOverlay;
    @FXML private Label detailsTitle;
    @FXML private Label detailsCategory;
    @FXML private Label detailsPrice;
    @FXML private Label detailsOldPrice;
    @FXML private Label detailsConvertedPrice;
    @FXML private Label detailsDesc;
    @FXML private Label detailsStock;
    @FXML private Label detailsLocation;
    @FXML private Label detailsId;
    @FXML private ImageView detailsImageView;
    @FXML private Spinner<Integer> detailsQuantitySpinner;
    
    private Produit currentEditProduit = null;
    private entities.MarketplaceConversation selectedConversation = null;
    private int pendingProductId = -1;
    private int pendingSellerId = -1;

    @FXML
    public void closeModal() {
        if (modalOverlay != null) {
            modalOverlay.setVisible(false);
        }
        currentEditProduit = null;
    }

    @FXML
    public void openSellerSpace() {
        loadProduits();
        if (sellerOverlay != null) {
            sellerOverlay.setVisible(true);
            sellerOverlay.toFront();
        }
    }

    @FXML
    public void closeSellerSpace() {
        if (sellerOverlay != null) {
            sellerOverlay.setVisible(false);
        }
    }

    @FXML
    public void closeDetails() {
        if (detailsOverlay != null) {
            detailsOverlay.setVisible(false);
        }
    }

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
        updateCartStatus();
        refreshMessagingBadge();
        refreshWishlistState();
    }

    private void setupFilters() {
        if (typeFilterCombo != null) {
            typeFilterCombo.getItems().setAll(TYPE_ALL, TYPE_VENTE, TYPE_LOCATION);
            typeFilterCombo.setValue(TYPE_ALL);
        }

        if (promoFilterCombo != null) {
            promoFilterCombo.getItems().setAll("Tous", "En promotion", "Sans promotion");
            promoFilterCombo.setValue("Tous");
        }

        if (typeFilter != null) {
            typeFilter.getItems().setAll(TYPE_ALL, TYPE_VENTE, TYPE_LOCATION);
            typeFilter.setValue(TYPE_ALL);
        }

        if (categorieFilter != null) {
            categorieFilter.getItems().setAll(CATEGORY_ALL);
            categorieFilter.getItems().addAll(CATEGORIES);
            categorieFilter.setValue(CATEGORY_ALL);
        }

        if (sortCombo != null) {
            sortCombo.getItems().setAll("A-Z", "Prix croissant", "Prix decroissant");
            sortCombo.setValue("A-Z");
        }

        if (fldType != null) {
            fldType.getItems().setAll(TYPE_VENTE, TYPE_LOCATION);
            fldType.setValue(TYPE_VENTE);
        }

        if (fldCategorie != null) {
            fldCategorie.getItems().setAll(CATEGORY_PLACEHOLDER);
            fldCategorie.getItems().addAll(CATEGORIES);
            fldCategorie.setValue(CATEGORY_PLACEHOLDER);
        }

        updatePublishPreview();
    }

    private void setupProduitTable() {
        if (produitTable == null || colId == null || colNom == null || colType == null
                || colCategorie == null || colPrix == null || colStock == null
                || colPromo == null || colActions == null) {
            return;
        }

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colCategorie.setCellValueFactory(new PropertyValueFactory<>("categorie"));
        colPrix.setCellValueFactory(new PropertyValueFactory<>("prix"));
        colStock.setCellValueFactory(new PropertyValueFactory<>("quantiteStock"));
        colPromo.setCellValueFactory(new PropertyValueFactory<>("promotion"));

        colNom.setCellFactory(col -> new TableCell<>() {
            private final ImageView thumb = new ImageView();
            private final Label nameLabel = new Label();
            private final Label descLabel = new Label();
            private final VBox textBox = new VBox(2, nameLabel, descLabel);
            private final HBox row = new HBox(10, thumb, textBox);

            {
                row.setAlignment(Pos.CENTER_LEFT);
                thumb.setFitWidth(64);
                thumb.setFitHeight(44);
                thumb.setPreserveRatio(false);
                thumb.setSmooth(true);
                nameLabel.getStyleClass().add("seller-name");
                descLabel.getStyleClass().add("seller-desc");
                descLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
                descLabel.setMaxWidth(300);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                    return;
                }

                Produit p = getTableView().getItems().get(getIndex());
                Image img = resolveProductImage(p.getImage());
                thumb.setImage(img);
                nameLabel.setText(normalizeText(safe(p.getNom())));
                descLabel.setText(normalizeText(safe(p.getDescription())));
                setGraphic(row);
            }
        });

        colCategorie.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }
                Label badge = new Label(normalizeText(item));
                badge.getStyleClass().add("seller-pill");
                setGraphic(badge);
            }
        });

        colType.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }
                Label badge = new Label(normalizeTypeForDisplay(item));
                badge.getStyleClass().add("seller-type-pill");
                if (TYPE_LOCATION.equalsIgnoreCase(normalizeTypeForDisplay(item))) {
                    badge.getStyleClass().add("location");
                }
                setGraphic(badge);
            }
        });

        colPrix.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : String.format("%.2f TND", item));
            }
        });

        colStock.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }
                Label badge = new Label(item > 5 ? "OK (" + item + ")" : "Faible (" + item + ")");
                badge.getStyleClass().add(item > 5 ? "seller-stock-ok" : "seller-stock-low");
                setGraphic(badge);
            }
        });

        produitTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnView = new Button("\u25ce");
            private final Button btnEdit = new Button("\u270e");
            private final Button btnDelete = new Button("\ud83d\uddd1");
            private final HBox box = new HBox(8, btnView, btnEdit, btnDelete);

            {
                box.setAlignment(Pos.CENTER_LEFT);
                btnView.getStyleClass().addAll("seller-action-btn", "view");
                btnEdit.getStyleClass().addAll("seller-action-btn", "edit");
                btnDelete.getStyleClass().addAll("seller-action-btn", "delete");
                btnView.setOnAction(e -> showProductDetails(getTableView().getItems().get(getIndex())));
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
        if (commandeTable == null || colCmdId == null || colCmdStatut == null || colCmdMontant == null
                || colCmdPaiement == null || colCmdActions == null) {
            return;
        }

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
        if (wishlistTable == null || colWishId == null || colWishUser == null
                || colWishProduit == null || colWishActions == null) {
            return;
        }

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
        if (messageTable == null || colMsgId == null || colMsgConversation == null
                || colMsgSender == null || colMsgContent == null || colMsgRead == null
                || colMsgActions == null) {
            return;
        }

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
            filteredProduits = new ArrayList<>(produits);
            currentPage = 0;
            renderCurrentPage();
        } catch (SQLException e) {
            showSqlAlert(e);
        }
    }

    private void loadCommandes() {
        if (commandeTable == null) {
            return;
        }
        try {
            List<Commande> commandes = commandeService.afficher();
            commandeTable.setItems(FXCollections.observableArrayList(commandes));
        } catch (SQLException e) {
            showSqlAlert(e);
        }
    }

    private void loadWishlist() {
        if (wishlistTable == null) {
            refreshWishlistState();
            return;
        }
        try {
            List<WishlistItem> items = wishlistService.getByUser(CURRENT_SESSION_USER_ID);
            wishlistTable.setItems(FXCollections.observableArrayList(items));
        } catch (SQLException e) {
            showSqlAlert(e);
        }
    }

    private void loadMessages() {
        if (messageTable == null) {
            return;
        }
        try {
            List<MarketplaceMessage> messages = messageService.afficher();
            messageTable.setItems(FXCollections.observableArrayList(messages));
        } catch (SQLException e) {
            showSqlAlert(e);
        }
    }

    private void refreshStats() {
        updateCartStatus();
        refreshMessagingBadge();
        refreshWishlistBadge();
    }

    @FXML
    public void openMessagingCenter() {
        if (messagingOverlay == null) {
            return;
        }

        clearPendingMessageContext();
        loadConversationsAndRender(null);
        messagingOverlay.setVisible(true);
        messagingOverlay.toFront();
    }

    @FXML
    public void closeMessagingCenter() {
        if (messagingOverlay == null) {
            return;
        }
        messagingOverlay.setVisible(false);
    }

    @FXML
    public void openWishlistCenter() {
        refreshWishlistState();
        if (wishlistOverlay != null) {
            wishlistOverlay.setVisible(true);
            wishlistOverlay.toFront();
        }
    }

    @FXML
    public void closeWishlistCenter() {
        if (wishlistOverlay != null) {
            wishlistOverlay.setVisible(false);
        }
    }

    @FXML
    public void sendCurrentMessage() {
        if (messageInputArea == null) {
            return;
        }

        String content = safe(messageInputArea.getText()).trim();
        if (content.isEmpty()) {
            return;
        }

        try {
            if (selectedConversation == null) {
                if (!hasPendingMessageContext()) {
                    showAlert("Messagerie", "Choisissez une conversation ou contactez un vendeur depuis la fiche produit.");
                    return;
                }
                selectedConversation = conversationService.findOrCreateConversation(
                        pendingProductId,
                        CURRENT_SESSION_USER_ID,
                        pendingSellerId
                );
            }

            MarketplaceMessage message = new MarketplaceMessage();
            message.setConversationId(selectedConversation.getId());
            message.setSenderId(CURRENT_SESSION_USER_ID);
            message.setContent(content);
            message.setRead(false);
            messageService.ajouter(message);
            conversationService.touchLastMessage(selectedConversation.getId());

            messageInputArea.clear();
            renderMessages(selectedConversation);
            loadConversationsAndRender(selectedConversation.getId());
            refreshMessagingBadge();
            clearPendingMessageContext();
        } catch (SQLException e) {
            showSqlAlert(e);
        }
    }

    private void openMessagingForProduct(Produit produit) {
        if (produit == null) {
            return;
        }
        try {
            int sellerId = resolveSellerIdForProduct(produit);
            entities.MarketplaceConversation existingConversation = conversationService.findConversation(
                    produit.getId(),
                    CURRENT_SESSION_USER_ID,
                    sellerId
            );

            pendingProductId = produit.getId();
            pendingSellerId = sellerId;

            if (messagingOverlay != null) {
                loadConversationsAndRender(existingConversation == null ? null : existingConversation.getId());
                messagingOverlay.setVisible(true);
                messagingOverlay.toFront();
            }

            if (existingConversation == null) {
                selectedConversation = null;
                if (messagingTitleLabel != null) {
                    messagingTitleLabel.setText("Nouveau message - Produit #" + pendingProductId + " - vendeur #" + pendingSellerId);
                }
                if (messagingMetaLabel != null) {
                    messagingMetaLabel.setText("Conversation creee a l'envoi du premier message");
                }
                if (messageListBox != null) {
                    messageListBox.getChildren().clear();
                    Label hint = new Label("Ecrivez votre premier message. La conversation sera creee apres envoi.");
                    hint.getStyleClass().add("chat-empty-label");
                    messageListBox.getChildren().add(hint);
                }
            }
        } catch (SQLException e) {
            showSqlAlert(e);
        }
    }

    private void toggleWishlistForProduct(Produit produit) {
        if (produit == null) {
            return;
        }

        try {
            if (wishlistProductIds.contains(produit.getId())) {
                List<WishlistItem> userItems = wishlistService.getByUser(CURRENT_SESSION_USER_ID);
                for (WishlistItem item : userItems) {
                    if (item.getProduitId() == produit.getId()) {
                        wishlistService.supprimer(item.getId());
                        break;
                    }
                }
                if (statusLabel != null) {
                    statusLabel.setText(normalizeText(safe(produit.getNom())) + " retire de la wishlist");
                }
            } else {
                wishlistService.ajouter(new WishlistItem(CURRENT_SESSION_USER_ID, produit.getId()));
                if (statusLabel != null) {
                    statusLabel.setText(normalizeText(safe(produit.getNom())) + " ajoute a la wishlist");
                }
            }

            loadWishlist();
            refreshWishlistState();
            renderCurrentPage();
        } catch (SQLException e) {
            showSqlAlert(e);
        }
    }

    private void refreshWishlistState() {
        try {
            List<WishlistItem> userItems = wishlistService.getByUser(CURRENT_SESSION_USER_ID);
            wishlistProductIds.clear();
            for (WishlistItem item : userItems) {
                wishlistProductIds.add(item.getProduitId());
            }
            refreshWishlistBadge();
            renderWishlistGrid();
        } catch (SQLException e) {
            showSqlAlert(e);
        }
    }

    private void refreshWishlistBadge() {
        if (btnWishlistCenter == null) {
            return;
        }

        int count = wishlistProductIds.size();
        btnWishlistCenter.setText(count <= 0 ? "Wishlist" : "Wishlist " + count);
    }

    private void renderWishlistGrid() {
        if (wishlistGrid == null) {
            return;
        }

        wishlistGrid.getChildren().clear();
        try {
            List<Produit> allProduits = produitService.afficher();
            Map<Integer, Produit> productById = new HashMap<>();
            for (Produit produit : allProduits) {
                productById.put(produit.getId(), produit);
            }

            List<Produit> wishedProduits = new ArrayList<>();
            for (Integer productId : wishlistProductIds) {
                Produit produit = productById.get(productId);
                if (produit != null) {
                    wishedProduits.add(produit);
                }
            }

            if (wishlistMetaLabel != null) {
                wishlistMetaLabel.setText(wishedProduits.size() + " article(s) sauvegarde(s)");
            }

            if (wishedProduits.isEmpty()) {
                Label empty = new Label("Votre wishlist est vide. Cliquez sur le coeur d'un produit pour l'ajouter ici.");
                empty.getStyleClass().add("wishlist-empty-label");
                wishlistGrid.getChildren().add(empty);
                return;
            }

            for (Produit produit : wishedProduits) {
                VBox card = new VBox(8);
                card.getStyleClass().add("wishlist-card");
                card.setPrefWidth(280);
                card.setMinWidth(280);
                card.setMaxWidth(280);

                ImageView imageView = new ImageView(resolveProductImage(produit.getImage()));
                imageView.setFitWidth(260);
                imageView.setFitHeight(140);
                imageView.setPreserveRatio(false);
                imageView.setSmooth(true);
                StackPane imageWrap = new StackPane(imageView);
                imageWrap.getStyleClass().add("wishlist-image-wrap");

                Label title = new Label(normalizeText(safe(produit.getNom())));
                title.getStyleClass().add("wishlist-card-title");
                title.setWrapText(true);

                Label meta = new Label(normalizeText(safe(produit.getCategorie())) + " - " + normalizeTypeForDisplay(produit.getType()));
                meta.getStyleClass().add("wishlist-card-meta");

                double displayPrice = (produit.isPromotion() && produit.getPromotionPrice() > 0)
                        ? produit.getPromotionPrice()
                        : produit.getPrix();
                Label price = new Label(String.format("%.2f TND", displayPrice));
                price.getStyleClass().add("wishlist-card-price");

                Button viewBtn = new Button("Voir");
                viewBtn.getStyleClass().add("btn-primary-small");
                viewBtn.setOnAction(e -> showProductDetails(produit));

                Button removeBtn = new Button("Retirer");
                removeBtn.getStyleClass().add("wishlist-remove-btn");
                removeBtn.setOnAction(e -> toggleWishlistForProduct(produit));

                HBox actions = new HBox(8, viewBtn, removeBtn);
                actions.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(viewBtn, Priority.ALWAYS);
                HBox.setHgrow(removeBtn, Priority.ALWAYS);
                viewBtn.setMaxWidth(Double.MAX_VALUE);
                removeBtn.setMaxWidth(Double.MAX_VALUE);

                card.getChildren().addAll(imageWrap, title, meta, price, actions);
                wishlistGrid.getChildren().add(card);
            }
        } catch (SQLException e) {
            showSqlAlert(e);
        }
    }

    private void loadConversationsAndRender(Integer preferredConversationId) {
        if (conversationListBox == null || messageListBox == null) {
            return;
        }

        conversationListBox.getChildren().clear();
        messageListBox.getChildren().clear();

        try {
            List<entities.MarketplaceConversation> conversations = conversationService.getByUser(CURRENT_SESSION_USER_ID);
            if (conversations.isEmpty()) {
                Label empty = new Label("Aucune conversation pour le moment. Ouvrez un produit puis cliquez sur Contacter le vendeur.");
                empty.getStyleClass().add("chat-empty-label");
                conversationListBox.getChildren().add(empty);
                selectedConversation = null;
                if (messagingTitleLabel != null) {
                    messagingTitleLabel.setText("Messagerie");
                }
                if (messagingMetaLabel != null) {
                    messagingMetaLabel.setText("0 conversation");
                }
                return;
            }

            entities.MarketplaceConversation toSelect = null;
            for (entities.MarketplaceConversation conversation : conversations) {
                VBox item = buildConversationTile(conversation);
                conversationListBox.getChildren().add(item);

                if (preferredConversationId != null && conversation.getId() == preferredConversationId) {
                    toSelect = conversation;
                }
            }

            if (toSelect == null) {
                if (!hasPendingMessageContext()) {
                    toSelect = conversations.get(0);
                }
            }

            if (toSelect != null) {
                selectConversation(toSelect);
            }

            if (messagingMetaLabel != null) {
                messagingMetaLabel.setText(conversations.size() + " conversation(s)");
            }
        } catch (SQLException e) {
            showSqlAlert(e);
        }
    }

    private VBox buildConversationTile(entities.MarketplaceConversation conversation) {
        Label title = new Label("Produit #" + conversation.getProduitId());
        title.getStyleClass().add("chat-tile-title");

        int otherUser = conversation.getBuyerId() == CURRENT_SESSION_USER_ID
                ? conversation.getSellerId()
                : conversation.getBuyerId();
        Label subtitle = new Label("Avec utilisateur #" + otherUser);
        subtitle.getStyleClass().add("chat-tile-subtitle");

        Label date = new Label(formatDateTime(conversation.getLastMessageAt()));
        date.getStyleClass().add("chat-tile-date");

        Circle dot = new Circle(4, Color.web("#10b981"));
        HBox bottom = new HBox(8, dot, subtitle);
        bottom.setAlignment(Pos.CENTER_LEFT);

        VBox item = new VBox(4, title, date, bottom);
        item.getStyleClass().add("chat-tile");
        item.setOnMouseClicked(e -> selectConversation(conversation));
        return item;
    }

    private void selectConversation(entities.MarketplaceConversation conversation) {
        selectedConversation = conversation;
        renderMessages(conversation);

        if (messagingTitleLabel != null) {
            int otherUser = conversation.getBuyerId() == CURRENT_SESSION_USER_ID
                    ? conversation.getSellerId()
                    : conversation.getBuyerId();
            messagingTitleLabel.setText("Conversation avec utilisateur #" + otherUser);
        }

        try {
            messageService.markConversationAsRead(conversation.getId(), CURRENT_SESSION_USER_ID);
            refreshMessagingBadge();
        } catch (SQLException e) {
            showSqlAlert(e);
        }
    }

    private void renderMessages(entities.MarketplaceConversation conversation) {
        if (messageListBox == null) {
            return;
        }

        messageListBox.getChildren().clear();
        try {
            List<MarketplaceMessage> messages = messageService.getByConversation(conversation.getId());
            if (messages.isEmpty()) {
                Label empty = new Label("Aucun message. Dites bonjour pour demarrer la discussion.");
                empty.getStyleClass().add("chat-empty-label");
                messageListBox.getChildren().add(empty);
                return;
            }

            for (MarketplaceMessage message : messages) {
                boolean mine = message.getSenderId() == CURRENT_SESSION_USER_ID;
                Label body = new Label(normalizeText(safe(message.getContent())));
                body.setWrapText(true);
                body.getStyleClass().add(mine ? "chat-bubble-me" : "chat-bubble-them");
                body.setMaxWidth(460);

                Label meta = new Label("#" + message.getSenderId() + " - " + formatDateTime(message.getCreatedAt()));
                meta.getStyleClass().add("chat-bubble-meta");

                VBox bubble = new VBox(4, body, meta);
                bubble.setFillWidth(false);

                HBox row = new HBox(bubble);
                row.setAlignment(mine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
                row.getStyleClass().add("chat-row");

                messageListBox.getChildren().add(row);
            }
        } catch (SQLException e) {
            showSqlAlert(e);
        }
    }

    private void refreshMessagingBadge() {
        if (btnMessagingCenter == null) {
            return;
        }
        try {
            int unread = messageService.countUnreadForUser(CURRENT_SESSION_USER_ID);
            if (unread <= 0) {
                btnMessagingCenter.setText("Messagerie");
            } else {
                btnMessagingCenter.setText("Messagerie " + unread);
            }
        } catch (SQLException e) {
            btnMessagingCenter.setText("Messagerie");
        }
    }

    private int resolveSellerIdForProduct(Produit produit) {
        int sellerId = produit.getVendeurId() > 0 ? produit.getVendeurId() : FALLBACK_SELLER_ID;
        if (sellerId == CURRENT_SESSION_USER_ID) {
            return FALLBACK_SELLER_ID;
        }
        return sellerId;
    }

    private boolean hasPendingMessageContext() {
        return pendingProductId > 0 && pendingSellerId > 0;
    }

    private void clearPendingMessageContext() {
        pendingProductId = -1;
        pendingSellerId = -1;
    }

    private String formatDateTime(LocalDateTime value) {
        if (value == null) {
            return "-";
        }
        return value.format(CHAT_TIME_FORMAT);
    }

    @FXML
    public void openPanier() {
        List<CartItem> cartItems = cartSessionService.getItems(CURRENT_SESSION_USER_ID);
        if (cartItems.isEmpty()) {
            showAlert("Panier", "Votre panier est vide.");
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Mon panier");
        dialog.getDialogPane().getStyleClass().add("cart-dialog-pane");
        ButtonType checkoutBtn = new ButtonType("Passer commande", ButtonBar.ButtonData.OK_DONE);
        ButtonType clearBtn = new ButtonType("Vider", ButtonBar.ButtonData.OTHER);
        dialog.getDialogPane().getButtonTypes().addAll(checkoutBtn, clearBtn, ButtonType.CANCEL);
        dialog.getDialogPane().setPrefWidth(700);
        dialog.getDialogPane().setPrefHeight(560);

        VBox content = new VBox(12);
        content.getStyleClass().add("cart-dialog-content");
        content.setPadding(new Insets(14));

        Label heading = new Label("Liste du panier");
        heading.getStyleClass().add("cart-title");

        Label subHeading = new Label("Verifiez vos articles avant paiement.");
        subHeading.getStyleClass().add("cart-subtitle");

        VBox lineItemsBox = new VBox(8);
        lineItemsBox.getStyleClass().add("cart-items-box");
        for (CartItem item : cartItems) {
            lineItemsBox.getChildren().add(buildCartRow(item));
        }

        ScrollPane itemsScroll = new ScrollPane(lineItemsBox);
        itemsScroll.getStyleClass().addAll("invisible-scrollpane", "cart-items-scroll");
        itemsScroll.setFitToWidth(true);
        itemsScroll.setPrefHeight(340);

        HBox totalsBox = new HBox(16);
        totalsBox.getStyleClass().add("cart-total-box");
        totalsBox.setAlignment(Pos.CENTER_LEFT);
        totalsBox.setPadding(new Insets(10, 12, 10, 12));

        Label countLabel = new Label("Articles: " + cartSessionService.countItems(CURRENT_SESSION_USER_ID));
        countLabel.getStyleClass().add("cart-count-label");
        Label totalLabel = new Label("Total: " + String.format("%.2f", cartSessionService.getTotal(CURRENT_SESSION_USER_ID)) + " TND");
        totalLabel.getStyleClass().add("cart-grand-total");
        Region totalSpacer = new Region();
        HBox.setHgrow(totalSpacer, Priority.ALWAYS);
        totalsBox.getChildren().addAll(countLabel, totalSpacer, totalLabel);

        content.getChildren().addAll(heading, subHeading, itemsScroll, totalsBox);
        dialog.getDialogPane().setContent(content);

        Node checkoutButtonNode = dialog.getDialogPane().lookupButton(checkoutBtn);
        Node clearButtonNode = dialog.getDialogPane().lookupButton(clearBtn);
        Node cancelButtonNode = dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        checkoutButtonNode.getStyleClass().add("cart-checkout-btn");
        clearButtonNode.getStyleClass().add("cart-clear-btn");
        cancelButtonNode.getStyleClass().add("cart-cancel-btn");

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty()) {
            return;
        }

        if (result.get() == clearBtn) {
            cartSessionService.clear(CURRENT_SESSION_USER_ID);
            updateCartStatus();
            if (statusLabel != null) {
                statusLabel.setText("Panier vide");
            }
            return;
        }

        if (result.get() == checkoutBtn) {
            processCheckout(cartItems);
        }
    }

    private void processCheckout(List<CartItem> cartItems) {
        Dialog<ButtonType> checkoutDialog = new Dialog<>();
        checkoutDialog.setTitle("Passer commande");
        checkoutDialog.getDialogPane().getStyleClass().add("checkout-dialog-pane");
        ButtonType saveBtn = new ButtonType("Valider", ButtonBar.ButtonData.OK_DONE);
        checkoutDialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        ComboBox<String> modePaiementBox = new ComboBox<>();
        modePaiementBox.getItems().setAll("carte", "especes", "virement");
        modePaiementBox.setValue("carte");

        ComboBox<String> modeLivraisonBox = new ComboBox<>();
        modeLivraisonBox.getItems().setAll("Retrait sur place", "Livraison a domicile");
        modeLivraisonBox.setValue("Retrait sur place");

        TextField adresseField = new TextField();
        adresseField.setPromptText("Adresse de livraison");
        adresseField.setDisable(true);
        modeLivraisonBox.getStyleClass().add("checkout-field");
        modePaiementBox.getStyleClass().add("checkout-field");
        adresseField.getStyleClass().add("checkout-field");

        Label policyLabel = new Label("Paiement autorise uniquement avec Livraison a domicile.");
        policyLabel.getStyleClass().add("checkout-policy-label");

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
        grid.add(policyLabel, 0, 3, 2, 1);
        checkoutDialog.getDialogPane().setContent(grid);

        Node saveButtonNode = checkoutDialog.getDialogPane().lookupButton(saveBtn);
        Node cancelButtonNode = checkoutDialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        saveButtonNode.getStyleClass().add("checkout-save-btn");
        cancelButtonNode.getStyleClass().add("checkout-cancel-btn");
        Runnable updateCheckoutValidation = () -> {
            boolean homeDelivery = "Livraison a domicile".equals(modeLivraisonBox.getValue());
            adresseField.setDisable(!homeDelivery);
            if (!homeDelivery) {
                adresseField.clear();
            }

            String modePaiement = safe(modePaiementBox.getValue()).trim();
            String adresse = safe(adresseField.getText()).trim();
            saveButtonNode.setDisable(!homeDelivery || modePaiement.isEmpty() || adresse.isEmpty());
        };

        modeLivraisonBox.valueProperty().addListener((obs, oldValue, newValue) -> updateCheckoutValidation.run());
        modePaiementBox.valueProperty().addListener((obs, oldValue, newValue) -> updateCheckoutValidation.run());
        adresseField.textProperty().addListener((obs, oldValue, newValue) -> updateCheckoutValidation.run());
        updateCheckoutValidation.run();

        Optional<ButtonType> result = checkoutDialog.showAndWait();
        if (result.isEmpty() || result.get() != saveBtn) {
            return;
        }

        boolean homeDelivery = "Livraison a domicile".equals(modeLivraisonBox.getValue());
        String modePaiement = safe(modePaiementBox.getValue()).trim();
        String adresse = safe(adresseField.getText()).trim();
        if (!homeDelivery) {
            showAlert("Validation", "Selectionnez 'Livraison a domicile' pour passer au paiement.");
            return;
        }

        if (modePaiement.isEmpty() || adresse.isEmpty()) {
            showAlert("Validation", "Mode de paiement et adresse de livraison sont obligatoires.");
            return;
        }

        try {
            int commandeId = commandeService.createCommandeFromCart(
                    CURRENT_SESSION_USER_ID,
                    modePaiement,
                    adresse,
                    cartItems
            );
            cartSessionService.clear(CURRENT_SESSION_USER_ID);
            loadCommandes();
            loadProduits();
            refreshStats();
            if (statusLabel != null) {
                statusLabel.setText("Commande #" + commandeId + " creee avec succes");
            }
        } catch (SQLException e) {
            showSqlAlert(e);
        }
    }

    private HBox buildCartRow(CartItem item) {
        Produit produit = item.getProduit();
        StackPane thumbWrap = new StackPane();
        thumbWrap.getStyleClass().add("cart-thumb-wrap");
        ImageView thumb = new ImageView(resolveProductImage(produit == null ? "" : produit.getImage()));
        thumb.setFitWidth(74);
        thumb.setFitHeight(58);
        thumb.setPreserveRatio(false);
        thumb.setSmooth(true);
        thumbWrap.getChildren().add(thumb);

        Label nameLabel = new Label(normalizeText(safe(produit == null ? "" : produit.getNom())));
        nameLabel.getStyleClass().add("cart-item-name");

        Label detailLabel = new Label(
                "Qte: " + item.getQuantite()
                        + "   |   PU: " + String.format("%.2f", item.getUnitPrice()) + " TND"
                        + "   |   Ligne: " + String.format("%.2f", item.getLineTotal()) + " TND"
        );
        detailLabel.getStyleClass().add("cart-item-meta");

        Label typeLabel = new Label(normalizeTypeForDisplay(produit == null ? "" : produit.getType()));
        typeLabel.getStyleClass().add("cart-type-pill");

        VBox left = new VBox(4, nameLabel, detailLabel, typeLabel);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label totalBadge = new Label(String.format("%.2f TND", item.getLineTotal()));
        totalBadge.getStyleClass().add("cart-line-total");

        HBox row = new HBox(12, thumbWrap, left, spacer, totalBadge);
        row.getStyleClass().add("cart-line-row");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 12, 10, 12));
        return row;
    }

    private String buildCartSummary(List<CartItem> items) {
        StringBuilder builder = new StringBuilder();
        builder.append("Articles dans le panier\n\n");
        for (CartItem item : items) {
            builder.append("- ")
                    .append(normalizeText(safe(item.getProduit().getNom())))
                    .append(" | qte: ")
                    .append(item.getQuantite())
                    .append(" | PU: ")
                    .append(String.format("%.2f", item.getUnitPrice()))
                    .append(" TND")
                    .append(" | total ligne: ")
                    .append(String.format("%.2f", item.getLineTotal()))
                    .append(" TND\n");
        }

        builder.append("\nTotal panier: ")
                .append(String.format("%.2f", cartSessionService.getTotal(CURRENT_SESSION_USER_ID)))
                .append(" TND\n")
                .append("Nombre d'articles: ")
                .append(cartSessionService.countItems(CURRENT_SESSION_USER_ID));
        return builder.toString();
    }

    private void addToCart(Produit produit, int quantite) {
        if (produit == null) {
            return;
        }
        if (produit.getQuantiteStock() <= 0) {
            showAlert("Stock", "Ce produit est en rupture de stock.");
            return;
        }

        cartSessionService.addProduit(CURRENT_SESSION_USER_ID, produit, quantite);
        updateCartStatus();
        if (statusLabel != null) {
            statusLabel.setText(normalizeText(safe(produit.getNom())) + " ajoute au panier");
        }
    }

    private void updateCartStatus() {
        if (statusLabel == null) {
            return;
        }
        int count = cartSessionService.countItems(CURRENT_SESSION_USER_ID);
        if (count <= 0) {
            statusLabel.setText("Panier vide");
            return;
        }
        statusLabel.setText("Panier: " + count + " article(s)");
    }

    @FXML
    public void handleSearch() {
        applyProduitFilters();
    }

    @FXML
    public void handleFilterChange() {
        applyProduitFilters();
    }

    @FXML
    public void handleCategoryChipFilter(ActionEvent event) {
        if (!(event.getSource() instanceof Button)) {
            return;
        }

        Button selectedButton = (Button) event.getSource();
        String rawLabel = safe(selectedButton.getText()).trim();
        String selectedCategory = resolveCategoryFromChipLabel(rawLabel);

        if (categorieFilter != null) {
            categorieFilter.setValue(selectedCategory);
        }

        Node parent = selectedButton.getParent();
        if (parent instanceof HBox) {
            HBox row = (HBox) parent;
            for (Node node : row.getChildren()) {
                if (node instanceof Button) {
                    node.getStyleClass().remove("active");
                }
            }
            if (!selectedButton.getStyleClass().contains("active")) {
                selectedButton.getStyleClass().add("active");
            }
        }

        applyProduitFilters();
    }

    private void applyProduitFilters() {
        try {
            List<Produit> base = produitService.afficher();
            String keyword = "";
            if (searchField != null && searchField.getText() != null) {
                keyword = searchField.getText().trim().toLowerCase();
            }

            String selectedType = "Tous";
            if (typeFilter != null && typeFilter.getValue() != null) {
                selectedType = typeFilter.getValue();
            } else if (typeFilterCombo != null && typeFilterCombo.getValue() != null) {
                selectedType = typeFilterCombo.getValue();
            }

            String selectedCategory = CATEGORY_ALL;
            if (categorieFilter != null && categorieFilter.getValue() != null) {
                selectedCategory = categorieFilter.getValue();
            }

            String selectedPromo = "Tous";
            if (promoFilterCombo != null && promoFilterCombo.getValue() != null) {
                selectedPromo = promoFilterCombo.getValue();
            }

            List<Produit> filtered = new ArrayList<>();
            for (Produit p : base) {
                String nom = p.getNom() == null ? "" : p.getNom().toLowerCase();
                String categorie = p.getCategorie() == null ? "" : p.getCategorie().toLowerCase();
                String type = p.getType() == null ? "" : p.getType();

                boolean matchesKeyword = keyword.isEmpty()
                        || nom.contains(keyword)
                        || categorie.contains(keyword);
                boolean matchesType = TYPE_ALL.equals(selectedType)
                        || normalizeTypeForDisplay(type).equalsIgnoreCase(selectedType);
                boolean matchesCategory = CATEGORY_ALL.equals(selectedCategory)
                        || categorie.equalsIgnoreCase(selectedCategory);
                boolean matchesPromo = "Tous".equals(selectedPromo)
                        || ("En promotion".equals(selectedPromo) && p.isPromotion())
                        || ("Sans promotion".equals(selectedPromo) && !p.isPromotion());

                if (matchesKeyword && matchesType && matchesCategory && matchesPromo) {
                    filtered.add(p);
                }
            }

            String selectedSort = sortCombo == null || sortCombo.getValue() == null ? "A-Z" : sortCombo.getValue();
            if ("Prix croissant".equals(selectedSort)) {
                filtered.sort(Comparator.comparingDouble(Produit::getPrix));
            } else if ("Prix decroissant".equals(selectedSort)) {
                filtered.sort(Comparator.comparingDouble(Produit::getPrix).reversed());
            } else {
                filtered.sort(Comparator.comparing(p -> safe(p.getNom()).toLowerCase()));
            }

            filteredProduits = filtered;
            currentPage = 0;
            renderCurrentPage();
        } catch (SQLException e) {
            showSqlAlert(e);
        }
    }

    @FXML
    public void handlePrevPage() {
        if (currentPage > 0) {
            currentPage--;
            renderCurrentPage();
        }
    }

    @FXML
    public void handleNextPage() {
        int totalPages = getTotalPages();
        if (currentPage < totalPages - 1) {
            currentPage++;
            renderCurrentPage();
        }
    }

    private int getTotalPages() {
        if (filteredProduits == null || filteredProduits.isEmpty()) {
            return 1;
        }
        return (int) Math.ceil(filteredProduits.size() / (double) PAGE_SIZE);
    }

    private void renderCurrentPage() {
        List<Produit> source = filteredProduits == null ? new ArrayList<>() : filteredProduits;
        int totalPages = getTotalPages();
        if (currentPage >= totalPages) {
            currentPage = Math.max(0, totalPages - 1);
        }

        int fromIndex = currentPage * PAGE_SIZE;
        int toIndex = Math.min(fromIndex + PAGE_SIZE, source.size());
        List<Produit> pageSlice = fromIndex < source.size() ? source.subList(fromIndex, toIndex) : new ArrayList<>();

        if (produitTable != null) {
            produitTable.setItems(FXCollections.observableArrayList(source));
        }
        if (productGrid != null) {
            populateProductGrid(pageSlice);
        }

        if (pageInfoLabel != null) {
            pageInfoLabel.setText("Page " + (currentPage + 1) + " / " + totalPages);
        }
        if (prevPageButton != null) {
            prevPageButton.setDisable(currentPage <= 0);
        }
        if (nextPageButton != null) {
            nextPageButton.setDisable(currentPage >= totalPages - 1);
        }
        if (statusLabel != null) {
            statusLabel.setText(source.size() + " resultat(s)");
        }
    }

    @FXML
    public void openAjouterProduit() {
        if (modalOverlay == null) return;
        currentEditProduit = null; // Important for ADD
        modalTitle.setText("Nouvelle Annonce");
        chkPromo.setSelected(false);
        fldNom.clear();
        fldDescription.clear();
        fldPrix.clear();
        fldStock.clear();
        if (fldCategorie != null) {
            fldCategorie.setValue(CATEGORY_PLACEHOLDER);
        }
        if (fldImage != null) {
            fldImage.clear();
        }
        fldPromoPrice.clear();
        if (fldType != null) {
            fldType.setValue(TYPE_VENTE);
        }
        updatePublishPreview();
        modalOverlay.setVisible(true);
        modalOverlay.toFront();
    }

    @FXML
    public void updatePublishPreview() {
        if (publishPreviewTypeBadge != null) {
            String displayType = fldType == null ? TYPE_VENTE : safe(fldType.getValue()).trim();
            if (displayType.isEmpty()) {
                displayType = TYPE_VENTE;
            }
            publishPreviewTypeBadge.setText(displayType.toUpperCase());
        }

        if (publishPreviewTitle != null) {
            String name = safe(fldNom == null ? "" : fldNom.getText()).trim();
            publishPreviewTitle.setText(name.isEmpty() ? "Nom de l'annonce" : normalizeText(name));
        }

        if (publishPreviewCategory != null) {
            String category = fldCategorie == null ? "" : safe(fldCategorie.getValue()).trim();
            publishPreviewCategory.setText(category.isEmpty() || CATEGORY_PLACEHOLDER.equals(category) ? "CHOISIR..." : normalizeText(category));
        }

        if (publishPreviewPrice != null) {
            Double price = parseDouble(fldPrix == null ? "" : fldPrix.getText());
            publishPreviewPrice.setText(price == null ? "0 TND" : String.format("%.2f TND", price));
        }

        if (publishPreviewStock != null) {
            Integer stock = parseInteger(fldStock == null ? "" : fldStock.getText());
            if (stock == null) {
                publishPreviewStock.setText("Stock: Non defini");
            } else if (stock <= 0) {
                publishPreviewStock.setText("Stock: Rupture");
            } else if (stock <= 5) {
                publishPreviewStock.setText("Stock: Faible (" + stock + ")");
            } else {
                publishPreviewStock.setText("Stock: " + stock + " disponibles");
            }
        }

        if (publishPreviewLocation != null) {
            publishPreviewLocation.setText("Ville / Region non definie");
        }

        if (publishPreviewDescription != null) {
            String desc = safe(fldDescription == null ? "" : fldDescription.getText()).trim();
            publishPreviewDescription.setText(desc.isEmpty() ? "Description..." : normalizeText(desc));
        }

        if (publishPreviewImage != null) {
            publishPreviewImage.setImage(resolveProductImage(fldImage == null ? "" : fldImage.getText()));
        }
    }

    @FXML
    public void chooseImageFile() {
        if (fldImage == null || fldImage.getScene() == null) {
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir une image produit");
        chooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")
        );

        Window owner = fldImage.getScene().getWindow();
        File selected = chooser.showOpenDialog(owner);
        if (selected != null) {
            String lowerName = selected.getName().toLowerCase();
            if (lowerName.endsWith(".webp")) {
                showAlert("Format non supporte", "Le format WEBP n'est pas supporte par JavaFX. Choisissez une image PNG, JPG, JPEG, GIF ou BMP.");
                return;
            }

            fldImage.setText(selected.getAbsolutePath());
            if (statusLabel != null) {
                statusLabel.setText("Image selectionnee: " + selected.getName());
            }
            updatePublishPreview();
        }
    }

    private void openModifierProduit(Produit produit) {
        if (modalOverlay == null) return;
        currentEditProduit = produit;
        modalTitle.setText("Modifier " + produit.getNom());
        fldNom.setText(produit.getNom());
        fldDescription.setText(produit.getDescription());
        fldPrix.setText(String.valueOf(produit.getPrix()));
        fldStock.setText(String.valueOf(produit.getQuantiteStock()));
        if (fldCategorie != null) {
            String category = safe(produit.getCategorie());
            if (!fldCategorie.getItems().contains(category)) {
                fldCategorie.getItems().add(category);
            }
            fldCategorie.setValue(category);
        }
        if (fldImage != null) {
            fldImage.setText(safe(produit.getImage()));
        }
        if (fldType != null) {
            fldType.setValue(normalizeTypeForDisplay(produit.getType()));
        }
        chkPromo.setSelected(produit.isPromotion());
        fldPromoPrice.setText(String.valueOf(produit.getPromotionPrice()));
        updatePublishPreview();
        modalOverlay.setVisible(true);
        modalOverlay.toFront();
    }

    @FXML
    public void saveFormProduct() {
        Double prix = parseDouble(fldPrix.getText());
        Double promoPrix = parseDouble(fldPromoPrice.getText());
        Integer stock = parseInteger(fldStock.getText());
        
        if (prix == null || stock == null || fldNom.getText().isBlank()) {
            showAlert("Erreur", "Champs invalides");
            return;
        }
        
        Produit p = currentEditProduit == null ? new Produit() : currentEditProduit;
        String selectedCategory = fldCategorie == null ? "" : safe(fldCategorie.getValue()).trim();
        if (selectedCategory.isEmpty() || CATEGORY_PLACEHOLDER.equals(selectedCategory)) {
            showAlert("Erreur", "Veuillez choisir une categorie");
            return;
        }

        String selectedType = fldType == null ? "" : safe(fldType.getValue()).trim();
        if (selectedType.isEmpty()) {
            showAlert("Erreur", "Veuillez choisir un type d'offre");
            return;
        }

        p.setNom(fldNom.getText().trim());
        p.setDescription(fldDescription.getText().trim());
        p.setPrix(prix);
        p.setQuantiteStock(stock);
        p.setCategorie(selectedCategory);
        p.setType(normalizeTypeForStorage(selectedType));
        if (fldImage != null) {
            p.setImage(fldImage.getText() == null ? "" : fldImage.getText().trim());
        }
        p.setPromotion(chkPromo.isSelected());
        if(promoPrix != null && chkPromo.isSelected()){
            p.setPromotionPrice(promoPrix);
        } else {
            p.setPromotionPrice(0.0);
        }
        p.setVendeurId(1); // placeholder
        
        try {
            if (currentEditProduit == null) {
                produitService.ajouter(p);
            } else {
                produitService.modifier(p);
            }
            loadProduits();
            refreshStats();
            closeModal();
        } catch (SQLException e) {
            showSqlAlert(e);
        }
    }

    @FXML
    public void showProductDetails(Produit p) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Views/ProductDetailsView.fxml"));
            Parent root = loader.load();

            ProductDetailsController controller = loader.getController();
            controller.setProduct(p);
            controller.setOnContactSeller(this::openMessagingForProduct);

            Stage stage = new Stage();
            stage.setTitle("Details Produit");
            stage.initModality(Modality.APPLICATION_MODAL);
            if (mainContent != null && mainContent.getScene() != null) {
                stage.initOwner(mainContent.getScene().getWindow());
            }
            stage.setScene(new Scene(root));
            stage.setMinWidth(1100);
            stage.setMinHeight(760);
            stage.show();
        } catch (IOException ex) {
            showAlert("Erreur", "Impossible d'ouvrir la page details: " + ex.getMessage());
        }
    }

    // legacy build dialog intact in case needed
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
        if (statusLabel != null) {
            statusLabel.setText("Erreur SQL detectee");
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String normalizeTypeForDisplay(String type) {
        String value = safe(type).trim().toLowerCase();
        if ("location".equals(value)) {
            return TYPE_LOCATION;
        }
        return TYPE_VENTE;
    }

    private String normalizeTypeForStorage(String type) {
        return TYPE_LOCATION.equalsIgnoreCase(safe(type).trim()) ? "location" : "vente";
    }

    private String resolveCategoryFromChipLabel(String label) {
        String normalized = safe(label).trim();
        if (normalized.isEmpty() || "toutes".equalsIgnoreCase(normalized)) {
            return CATEGORY_ALL;
        }

        for (String category : CATEGORIES) {
            if (category.equalsIgnoreCase(normalized)) {
                return category;
            }
        }

        return CATEGORY_ALL;
    }

    private String normalizeText(String input) {
        if (input == null) {
            return "";
        }
        String trimmed = input.trim();
        if (trimmed.contains("Ã") || trimmed.contains("Â")) {
            return new String(trimmed.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
        }
        return trimmed;
    }

    private Image resolveProductImage(String rawImagePath) {
        String raw = safe(rawImagePath).trim();
        raw = raw.replace("\"", "");
        if (raw.isEmpty()) {
            return loadPlaceholderImage();
        }

        try {
            if (raw.startsWith("http://") || raw.startsWith("https://") || raw.startsWith("file:")) {
                Image direct = new Image(raw, false);
                return direct.isError() ? loadPlaceholderImage() : direct;
            }

            Path absolutePath = Paths.get(raw).toAbsolutePath();
            if (Files.exists(absolutePath)) {
                Image local = new Image(absolutePath.toUri().toString(), false);
                return local.isError() ? loadPlaceholderImage() : local;
            }

            Path projectRelative = Paths.get(System.getProperty("user.dir"), raw).toAbsolutePath();
            if (Files.exists(projectRelative)) {
                Image local = new Image(projectRelative.toUri().toString(), false);
                return local.isError() ? loadPlaceholderImage() : local;
            }
        } catch (Exception ignored) {
            return loadPlaceholderImage();
        }

        return loadPlaceholderImage();
    }

    private Image loadPlaceholderImage() {
        try {
            String[] candidates = {
                    "/images/placeholder_agrismart.png",
                    "/images/product_placeholder.png",
                    "/images/logo.png"
            };

            for (String candidate : candidates) {
                URL resource = getClass().getResource(candidate);
                if (resource == null) {
                    continue;
                }
                Image placeholder = new Image(resource.toExternalForm(), true);
                if (!placeholder.isError()) {
                    return placeholder;
                }
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }

    private void populateProductGrid(java.util.List<Produit> produits) {
        if (productGrid == null) return;
        productGrid.getChildren().clear();
        
        if (produits == null || produits.isEmpty()) {
            Label emptyLabel = new Label("Aucun produit trouve dans la base de donnees. Ajoutez-en un pour les voir apparaitre ici.");
            emptyLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #888; -fx-padding: 30;");
            productGrid.getChildren().add(emptyLabel);
            return;
        }

        for (Produit p : produits) {
            VBox card = new VBox();
            card.getStyleClass().add("product-card");
            card.setSpacing(5);
            card.setStyle("-fx-cursor: hand;");
            card.setMinWidth(260);
            card.setPrefWidth(260);
            card.setMaxWidth(260);
            card.setMinHeight(320);
            card.setPrefHeight(320);
            card.setMaxHeight(320);

            StackPane imageContainer = new StackPane();
            imageContainer.getStyleClass().add("product-image-container");
            imageContainer.setMinWidth(260);
            imageContainer.setPrefWidth(260);
            imageContainer.setMaxWidth(260);
            imageContainer.setMinHeight(150);
            imageContainer.setPrefHeight(150);
            imageContainer.setMaxHeight(150);
            
            try {
                Image loadedImage = resolveProductImage(p.getImage());
                if (loadedImage != null) {
                    ImageView imgView = new ImageView(loadedImage);
                    imgView.setFitHeight(150);
                    imgView.setFitWidth(260);
                    imgView.setPreserveRatio(false);
                    imgView.setSmooth(true);
                    imageContainer.getChildren().add(imgView);
                } else {
                    Label fallback = new Label("No image");
                    fallback.setStyle("-fx-font-size: 16px; -fx-text-fill: #999;");
                    imageContainer.getChildren().add(fallback);
                }
            } catch (Exception e) {
                 Label fallback = new Label("No image");
                 fallback.setStyle("-fx-text-fill: #aaa;");
                 imageContainer.getChildren().add(fallback);
            }

            Button btnHeart = new Button("\u2661");
            btnHeart.getStyleClass().add("wishlist-heart-btn");
            if (wishlistProductIds.contains(p.getId())) {
                btnHeart.getStyleClass().add("active");
                btnHeart.setText("\u2665");
            }
            btnHeart.setOnAction(e -> {
                toggleWishlistForProduct(p);
                e.consume();
            });
            StackPane.setAlignment(btnHeart, javafx.geometry.Pos.TOP_LEFT);
            StackPane.setMargin(btnHeart, new javafx.geometry.Insets(8, 0, 0, 8));
            imageContainer.getChildren().add(btnHeart);
            
            if (p.isPromotion()) {
                Label promoBadge = new Label("PROMO");
                promoBadge.getStyleClass().add("promo-badge");
                StackPane.setAlignment(promoBadge, javafx.geometry.Pos.TOP_RIGHT);
                StackPane.setMargin(promoBadge, new javafx.geometry.Insets(8, 8, 0, 0));
                imageContainer.getChildren().add(promoBadge);
            }

            VBox infoBox = new VBox(6);
            infoBox.setPadding(new javafx.geometry.Insets(12));
            VBox.setVgrow(infoBox, Priority.ALWAYS);
            infoBox.setPrefHeight(170);

            Label titre = new Label(normalizeText(safe(p.getNom())));
            titre.getStyleClass().add("product-title-text");
            titre.setWrapText(false);
            titre.setTextOverrun(OverrunStyle.ELLIPSIS);
            titre.setMaxWidth(230);

            Label categorieLib = new Label(normalizeText(safe(p.getCategorie())) + " - " + normalizeText(safe(p.getType())));
            categorieLib.getStyleClass().add("product-category-text");
            categorieLib.setWrapText(false);
            categorieLib.setTextOverrun(OverrunStyle.ELLIPSIS);
            categorieLib.setMaxWidth(230);
            
            HBox priceRow = new HBox(8);
            priceRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            if (p.isPromotion()) {
                Label oldPrice = new Label(p.getPrix() + " TND");
                oldPrice.getStyleClass().add("product-old-price");
                Label newPrice = new Label(p.getPromotionPrice() + " TND");
                newPrice.getStyleClass().add("product-new-price");
                priceRow.getChildren().addAll(newPrice, oldPrice);
            } else {
                Label price = new Label(p.getPrix() + " TND");
                price.getStyleClass().add("product-price");
                priceRow.getChildren().add(price);
            }

            HBox actions = new HBox(8);
            actions.getStyleClass().add("product-bottom-actions");
            actions.setAlignment(javafx.geometry.Pos.CENTER);

            Button btnAddCart = new Button("Ajouter au panier");
            btnAddCart.getStyleClass().add("btn-cart-small");
            btnAddCart.setOnAction(e -> {
                addToCart(p, 1);
                e.consume();
            });

            Button btnView = new Button("Voir");
            btnView.getStyleClass().add("btn-primary-small");
            btnView.setOnAction(e -> {
                showProductDetails(p);
                e.consume();
            });

            HBox.setHgrow(btnAddCart, Priority.ALWAYS);
            HBox.setHgrow(btnView, Priority.ALWAYS);
            btnAddCart.setMaxWidth(Double.MAX_VALUE);
            btnView.setMaxWidth(Double.MAX_VALUE);
            actions.getChildren().addAll(btnAddCart, btnView);

            Region spacer = new Region();
            VBox.setVgrow(spacer, Priority.ALWAYS);
            
            infoBox.getChildren().addAll(titre, categorieLib, spacer, priceRow, actions);
            card.getChildren().addAll(imageContainer, infoBox);
            card.setOnMouseClicked(e -> showProductDetails(p));
            
            productGrid.getChildren().add(card);
        }
    }
}

