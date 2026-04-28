package controllers;

import entities.CartItem;
import entities.Commande;
import entities.MarketplaceMessage;
import entities.Produit;
import entities.WishlistItem;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Spinner;
import javafx.scene.control.ToggleButton;
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
import controllers.marketplace.MarketplaceMessagingFeature;
import controllers.marketplace.MarketplaceMessagingState;
import controllers.marketplace.MarketplaceWishlistFeature;
import controllers.marketplace.MarketplaceWishlistState;
import services.CommandeService;
import services.CartSessionService;
import services.MarketplaceCartViewFeature;
import services.MarketplaceCheckoutFeature;
import services.MarketplaceConversationService;
import services.MarketplaceImageService;
import services.MarketplaceMessageService;
import services.ProduitService;
import services.UserService;
import services.WishlistService;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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
import java.util.UUID;
import utils.MarketplaceValidator;
import utils.SessionManager;
import javafx.util.Duration;

public class MarketplaceController implements Initializable {

    private static final String CATEGORY_PLACEHOLDER = "Choisir...";
    private static final String CATEGORY_ALL = "Toutes";
    private static final String TYPE_ALL = "Tous";
    private static final String TYPE_VENTE = "Vente";
    private static final String TYPE_LOCATION = "Location";
    private static final int PAGE_SIZE = 8;
    private static final int DEFAULT_GUEST_USER_ID = 1;
    private static final int DEFAULT_FALLBACK_SELLER_ID = 2;
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
    @FXML private ToggleButton btnManageProducts;
    @FXML private ToggleButton btnSoldOrders;
    @FXML private ToggleButton btnBoughtOrders;
    @FXML private Label sellerOrdersTitleLabel;

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
    @FXML private HBox toastContainer;
    @FXML private Label toastIconLabel;
    @FXML private Label toastTitleLabel;
    @FXML private Label toastLabel;

    @FXML private StackPane wishlistOverlay;
    @FXML private Label wishlistMetaLabel;
    @FXML private FlowPane wishlistGrid;

    

    private final ProduitService produitService = new ProduitService();
    private final CommandeService commandeService = new CommandeService();
    private final CartSessionService cartSessionService = CartSessionService.getInstance();
    private final MarketplaceCartViewFeature cartViewFeature = new MarketplaceCartViewFeature();
    private final MarketplaceCheckoutFeature checkoutFeature = new MarketplaceCheckoutFeature(commandeService, cartSessionService);
    private final MarketplaceConversationService conversationService = new MarketplaceConversationService();
    private final MarketplaceImageService imageService = new MarketplaceImageService();
    private final WishlistService wishlistService = new WishlistService();
    private final MarketplaceMessageService messageService = new MarketplaceMessageService();
    private final UserService userService = new UserService();
    private final MarketplaceMessagingState messagingState = new MarketplaceMessagingState();
    private final MarketplaceWishlistState wishlistState = new MarketplaceWishlistState();
    private MarketplaceMessagingFeature messagingFeature;
    private MarketplaceWishlistFeature wishlistFeature;
    private List<Produit> filteredProduits = new ArrayList<>();
    private final Set<Integer> wishlistProductIds = new HashSet<>();
    private final Map<Integer, String> productNameById = new HashMap<>();
    private final Map<Integer, String> userNameById = new HashMap<>();
    private final Set<Integer> purchasedProductIds = new HashSet<>();
    private boolean showingBoughtOrders = false;
    private boolean openSellerOnBoughtOrders = false;
    private int currentPage = 0;

    @FXML private StackPane modalOverlay;
    @FXML private StackPane sellerOverlay;
    @FXML private VBox sellerProductsSection;
    @FXML private VBox sellerOrdersSection;
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
    @FXML private Label errNom;
    @FXML private Label errCategorie;
    @FXML private Label errType;
    @FXML private Label errPrix;
    @FXML private Label errStock;
    @FXML private Label errDescription;
    @FXML private Label errPromoPrice;

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
    @FXML private Label detailsTypeBadge;
    @FXML private Label detailsTypePill;
    @FXML private ImageView detailsImageView;
    @FXML private Spinner<Integer> detailsQuantitySpinner;

    @FXML private StackPane cartOverlay;
    @FXML private VBox cartItemsBox;
    @FXML private Label cartCountLabel;
    @FXML private Label cartTotalLabel;
    
    private Produit currentEditProduit = null;
    private Produit currentDetailsProduit = null;
    private entities.MarketplaceConversation selectedConversation = null;
    private int pendingProductId = -1;
    private int pendingSellerId = -1;

    @FXML
    public void closeModal() {
        animateOverlayOut(modalOverlay);
        currentEditProduit = null;
        clearProductFieldErrors();
    }

    @FXML
    public void openSellerSpace() {
        loadProduits();
        if (openSellerOnBoughtOrders) {
            showingBoughtOrders = true;
            setSellerSectionVisibility(false);
            applyOrderToggleSelection();
            openSellerOnBoughtOrders = false;
        } else {
            showManageProducts();
        }
        loadCommandes();
        animateOverlayIn(sellerOverlay);
    }

    @FXML
    public void closeSellerSpace() {
        animateOverlayOut(sellerOverlay);
    }

    @FXML
    public void showManageProducts() {
        setSellerSectionVisibility(true);
        applyOrderToggleSelection();
    }

    @FXML
    public void showSoldOrders() {
        showingBoughtOrders = false;
        setSellerSectionVisibility(false);
        applyOrderToggleSelection();
        loadCommandes();
    }

    @FXML
    public void showBoughtOrders() {
        showingBoughtOrders = true;
        setSellerSectionVisibility(false);
        applyOrderToggleSelection();
        loadCommandes();
    }

    private void setSellerSectionVisibility(boolean showProducts) {
        if (sellerProductsSection != null) {
            sellerProductsSection.setVisible(showProducts);
            sellerProductsSection.setManaged(showProducts);
        }
        if (sellerOrdersSection != null) {
            sellerOrdersSection.setVisible(!showProducts);
            sellerOrdersSection.setManaged(!showProducts);
        }
    }

    private void initializeOrderToggleState() {
        setSellerSectionVisibility(true);
        applyOrderToggleSelection();
    }

    private void applyOrderToggleSelection() {
        boolean showingProducts = sellerProductsSection != null && sellerProductsSection.isVisible();
        if (btnManageProducts != null) {
            btnManageProducts.setSelected(showingProducts);
        }
        if (btnSoldOrders != null) {
            btnSoldOrders.setSelected(!showingProducts && !showingBoughtOrders);
        }
        if (btnBoughtOrders != null) {
            btnBoughtOrders.setSelected(!showingProducts && showingBoughtOrders);
        }
        if (sellerOrdersTitleLabel != null) {
            sellerOrdersTitleLabel.setText(showingBoughtOrders ? "Produits achetes" : "Produits vendus");
        }
    }

    private void resolveCurrentUserRole() {
        // Role intentionally ignored in this view: all connected non-admin marketplace users
        // can switch between sold and bought orders.
    }

    @FXML
    public void closeDetails() {
        animateOverlayOut(detailsOverlay);
        currentDetailsProduit = null;
    }

    @FXML
    public void closeCartOverlay() {
        animateOverlayOut(cartOverlay);
    }

    @FXML
    public void clearCartFromOverlay() {
        cartSessionService.clear(getCurrentUserId());
        renderCartOverlay();
        updateCartStatus();
        showToast("Panier vide avec succes.", true);
    }

    @FXML
    public void checkoutFromOverlay() {
        List<CartItem> cartItems = cartSessionService.getItems(getCurrentUserId());
        if (cartItems.isEmpty()) {
            showToast("Panier", "Votre panier est vide.", false);
            return;
        }
        processCheckout(cartItems);
        renderCartOverlay();
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupProduitTable();
        setupCommandeTable();
        setupWishlistTable();
        setupMessageTable();
        setupFilters();
        initializeOrderToggleState();
        configureToastUi();
        initializeMarketplaceFeatures();

        loadProduits();
        loadCommandes();
        refreshPurchasedState();
        loadWishlist();
        loadMessages();
        refreshStats();
        updateCartStatus();
        refreshMessagingBadge();
        refreshWishlistState();
    }

    private void initializeMarketplaceFeatures() {
        messagingFeature = new MarketplaceMessagingFeature(
                messagingState,
                conversationService,
                messageService,
                produitService,
                userService,
                btnMessagingCenter,
                messagingOverlay,
                conversationListBox,
                messageListBox,
                messagingTitleLabel,
                messagingMetaLabel,
                messageInputArea,
                this::animateOverlayIn,
                this::animateOverlayOut,
                this::getCurrentUserId,
                this::getFallbackSellerId,
                this::showAlert,
                this::showSqlAlert,
                this::showToast,
                this::normalizeText
        );

        wishlistFeature = new MarketplaceWishlistFeature(
                wishlistState,
                wishlistService,
                produitService,
                imageService,
                wishlistGrid,
                wishlistMetaLabel,
                btnWishlistCenter,
                this::getCurrentUserId,
                this::getSessionUserId,
                this::showAlert,
                this::showSqlAlert,
                this::showToast,
                this::showProductDetails,
                this::renderCurrentPage,
                this::normalizeText,
                this::normalizeTypeForDisplay,
                () -> MarketplaceController.this.getClass()
        );
    }

    private void configureToastUi() {
        if (toastContainer != null) {
            toastContainer.setMaxWidth(320);
            toastContainer.setMinWidth(Region.USE_PREF_SIZE);
            toastContainer.setPrefWidth(Region.USE_COMPUTED_SIZE);
        }
        if (toastLabel != null) {
            toastLabel.setWrapText(false);
            toastLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
            toastLabel.setMaxWidth(292);
        }
    }

    private void animateOverlayIn(StackPane overlay) {
        if (overlay == null) {
            return;
        }

        Node shell = overlay.getChildren().isEmpty() ? overlay : overlay.getChildren().get(0);
        overlay.setVisible(true);
        overlay.toFront();
        overlay.setOpacity(0);
        shell.setScaleX(0.985);
        shell.setScaleY(0.985);

        FadeTransition fade = new FadeTransition(Duration.millis(170), overlay);
        fade.setFromValue(0);
        fade.setToValue(1);

        ScaleTransition scale = new ScaleTransition(Duration.millis(170), shell);
        scale.setFromX(0.985);
        scale.setFromY(0.985);
        scale.setToX(1.0);
        scale.setToY(1.0);

        new ParallelTransition(fade, scale).play();
    }

    private void animateOverlayOut(StackPane overlay) {
        if (overlay == null || !overlay.isVisible()) {
            return;
        }

        Node shell = overlay.getChildren().isEmpty() ? overlay : overlay.getChildren().get(0);

        FadeTransition fade = new FadeTransition(Duration.millis(130), overlay);
        fade.setFromValue(Math.max(0.0, overlay.getOpacity()));
        fade.setToValue(0);

        ScaleTransition scale = new ScaleTransition(Duration.millis(130), shell);
        scale.setFromX(1.0);
        scale.setFromY(1.0);
        scale.setToX(0.992);
        scale.setToY(0.992);

        ParallelTransition out = new ParallelTransition(fade, scale);
        out.setOnFinished(e -> {
            overlay.setVisible(false);
            overlay.setOpacity(1);
            shell.setScaleX(1.0);
            shell.setScaleY(1.0);
        });
        out.play();
    }

    private void showToast(String message, boolean success) {
        String title = success ? "Succes" : "Attention";
        showToast(title, message, success);
    }

    private void showToast(String title, String message, boolean success) {
        String safeTitle = title == null || title.isBlank() ? (success ? "Succes" : "Attention") : title.trim();
        String safeMessage = message == null ? "" : message.trim();
        String payload = safeMessage.isEmpty() ? safeTitle : safeTitle + " - " + safeMessage;
        MainController.publishHeaderAlert(payload, success);
    }

    private void showNotice(String title, String message) {
        String safeTitle = title == null || title.isBlank() ? "Information" : title.trim();
        String safeMessage = message == null ? "" : message.trim();
        String payload = safeMessage.isEmpty() ? safeTitle : safeTitle + " - " + safeMessage;
        MainController.publishHeaderNotice(payload);
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
                Image img = imageService.resolveProductImage(p.getImage(), getClass());
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
            private final Button btnView = new Button("👁");
            private final Button btnEdit = new Button("✏");
            private final Button btnDelete = new Button("❌");
            private final HBox box = new HBox(8, btnView, btnEdit, btnDelete);

            {
                box.setAlignment(Pos.CENTER_LEFT);
                btnView.getStyleClass().addAll("seller-action-btn", "view");
                btnEdit.getStyleClass().addAll("seller-action-btn", "edit");
                btnDelete.getStyleClass().addAll("seller-action-btn", "delete");
                btnView.setOnAction(e -> {
                    Produit p = getTableView().getItems().get(getIndex());
                    if (p != null) showProductDetails(p);
                });
                btnEdit.setOnAction(e -> {
                    Produit p = getTableView().getItems().get(getIndex());
                    if (p != null) openModifierProduit(p);
                });
                btnDelete.setOnAction(e -> {
                    Produit p = getTableView().getItems().get(getIndex());
                    if (p != null) handleDeleteProduit(p);
                });
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
        if (!commandeTable.getStyleClass().contains("commande-table")) {
            commandeTable.getStyleClass().add("commande-table");
        }

        colCmdStatut.setCellFactory(col -> new TableCell<>() {
            private final Label chip = new Label();

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }
                chip.getStyleClass().removeAll("cmd-status-chip", "pending", "confirmed", "delivered", "cancelled");
                chip.getStyleClass().addAll("cmd-status-chip", resolveStatusStyle(item));
                chip.setText(resolveStatusLabel(item));
                setGraphic(chip);
            }
        });

        colCmdMontant.setCellFactory(col -> new TableCell<>() {
            private final Label amount = new Label();

            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }
                amount.getStyleClass().setAll("cmd-amount");
                amount.setText(String.format("%.2f TND", item));
                setGraphic(amount);
            }
        });

        colCmdPaiement.setCellFactory(col -> new TableCell<>() {
            private final Label chip = new Label();

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }
                chip.getStyleClass().removeAll("cmd-payment-chip", "card", "home");
                chip.getStyleClass().addAll("cmd-payment-chip", resolvePaymentStyle(item));
                chip.setText(resolvePaymentLabel(item));
                setGraphic(chip);
            }
        });

        colCmdActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit = new Button("✏");
            private final Button btnDelete = new Button("❌");
            private final Button btnFacture = new Button("📥");
            private final HBox box = new HBox(8, btnEdit, btnDelete, btnFacture);

            {
                box.setAlignment(Pos.CENTER_LEFT);
                btnEdit.getStyleClass().addAll("cmd-action-btn", "edit");
                btnDelete.getStyleClass().addAll("cmd-action-btn", "delete");
                btnFacture.getStyleClass().addAll("cmd-action-btn", "facture");
                btnEdit.setOnAction(e -> {
                    Commande cmd = getTableView().getItems().get(getIndex());
                    if(cmd != null) openModifierCommande(cmd);
                });
                btnDelete.setOnAction(e -> {
                    Commande cmd = getTableView().getItems().get(getIndex());
                    if(cmd != null) handleDeleteCommande(cmd);
                });
                btnFacture.setOnAction(e -> {
                    Commande cmd = getTableView().getItems().get(getIndex());
                    if(cmd != null) downloadFacture(cmd);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Commande cmd = getTableView().getItems().get(getIndex());
                    if (cmd != null) {
                        btnFacture.setVisible(cmd.getClientId() == getCurrentUserId());
                        btnFacture.setManaged(cmd.getClientId() == getCurrentUserId());
                    }
                    setGraphic(box);
                }
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
        int userId = getCurrentUserId();
        try {
            List<Commande> commandes;
            if (showingBoughtOrders) {
                commandes = commandeService.getByClient(userId);
            } else {
                commandes = commandeService.getBySeller(userId);
            }
            commandeTable.setItems(FXCollections.observableArrayList(commandes));
            refreshPurchasedState();
        } catch (SQLException e) {
            showSqlAlert(e);
        }
    }

    private void refreshPurchasedState() {
        purchasedProductIds.clear();
        try {
            purchasedProductIds.addAll(commandeService.getPurchasedProductIdsByClient(getCurrentUserId()));
        } catch (SQLException e) {
            showSqlAlert(e);
        }
    }

    private void loadWishlist() {
        if (wishlistFeature != null) {
            wishlistFeature.loadWishlist();
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
        if (messagingFeature != null) {
            messagingFeature.openMessagingCenter();
        }
    }

    @FXML
    public void closeMessagingCenter() {
        if (messagingFeature != null) {
            messagingFeature.closeMessagingCenter();
        }
    }

    @FXML
    public void openWishlistCenter() {
        if (wishlistFeature != null) {
            wishlistFeature.openWishlistCenter();
        }
        animateOverlayIn(wishlistOverlay);
    }

    @FXML
    public void closeWishlistCenter() {
        if (wishlistFeature != null) {
            wishlistFeature.closeWishlistCenter();
        }
        animateOverlayOut(wishlistOverlay);
    }

    @FXML
    public void sendCurrentMessage() {
        if (messagingFeature != null) {
            messagingFeature.sendCurrentMessage();
        }
    }

    private void openMessagingForProduct(Produit produit) {
        if (messagingFeature != null) {
            messagingFeature.openMessagingForProduct(produit);
        }
    }

    private void toggleWishlistForProduct(Produit produit) {
        if (wishlistFeature != null) {
            wishlistFeature.toggleWishlistForProduct(produit);
        }
    }

    private void refreshWishlistState() {
        if (wishlistFeature != null) {
            wishlistFeature.refreshWishlistState();
        }
    }

    private void refreshWishlistBadge() {
        if (wishlistFeature != null) {
            wishlistFeature.refreshWishlistBadge();
        }
    }

    private void renderWishlistGrid() {
        if (wishlistFeature != null) {
            wishlistFeature.renderWishlistGrid();
        }
    }

    private void loadConversationsAndRender(Integer preferredConversationId) {
        if (messagingFeature != null) {
            messagingFeature.loadConversationsAndRender(preferredConversationId);
        }
    }

    private VBox buildConversationTile(entities.MarketplaceConversation conversation) {
        Label title = new Label(resolveProductDisplayName(conversation.getProduitId()));
        title.getStyleClass().add("chat-tile-title");

        Label subtitle = new Label("Vendeur: " + resolveUserDisplayName(conversation.getSellerId()));
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
        if (messagingFeature != null) {
            messagingFeature.selectConversation(conversation);
        }
    }

    private void renderMessages(entities.MarketplaceConversation conversation) {
        if (messagingFeature != null) {
            messagingFeature.loadConversationsAndRender(conversation == null ? null : conversation.getId());
        }
    }

    private void refreshMessagingBadge() {
        if (messagingFeature != null) {
            messagingFeature.refreshMessagingBadge();
        }
    }

    private int resolveSellerIdForProduct(Produit produit) {
        int sellerId = produit.getVendeurId() > 0 ? produit.getVendeurId() : getFallbackSellerId();
        if (sellerId == getCurrentUserId()) {
            return getFallbackSellerId();
        }
        return sellerId;
    }

    private boolean hasPendingMessageContext() {
        return messagingFeature != null && messagingFeature.hasPendingMessageContext();
    }

    private void clearPendingMessageContext() {
        if (messagingFeature != null) {
            messagingFeature.clearPendingMessageContext();
        }
    }

    private void refreshMessagingDisplayMaps() throws SQLException {
        if (messagingFeature != null) {
            messagingFeature.loadConversationsAndRender(null);
        }
    }

    private String resolveProductDisplayName(int productId) {
        return productNameById.getOrDefault(productId, "Produit non disponible");
    }

    private String resolveUserDisplayName(int userId) {
        return userNameById.getOrDefault(userId, "Vendeur inconnu");
    }

    private String buildUserDisplayName(entities.User user) {
        String first = normalizeText(safe(user.getFirstName()));
        String last = normalizeText(safe(user.getLastName()));
        String fullName = (first + " " + last).trim();
        if (!fullName.isBlank()) {
            return fullName;
        }
        String email = normalizeText(safe(user.getEmail()));
        return email.isBlank() ? "Utilisateur" : email;
    }

    private String buildConversationHeader(int productId, int sellerId) {
        return resolveProductDisplayName(productId) + " - vendeur: " + resolveUserDisplayName(sellerId);
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "-" : value.format(CHAT_TIME_FORMAT);
    }

    @FXML
    public void openPanier() {
        renderCartOverlay();
        animateOverlayIn(cartOverlay);
    }

    private void renderCartOverlay() {
        if (cartItemsBox == null) {
            return;
        }

        List<CartItem> cartItems = cartSessionService.getItems(getCurrentUserId());
        cartItemsBox.getChildren().clear();

        if (cartItems.isEmpty()) {
            Label empty = new Label("Votre panier est vide.");
            empty.getStyleClass().add("wishlist-empty-label");
            cartItemsBox.getChildren().add(empty);
        } else {
            for (CartItem item : cartItems) {
                cartItemsBox.getChildren().add(cartViewFeature.buildCartRow(item, new MarketplaceCartViewFeature.Host() {
                    @Override
                    public Image resolveProductImage(String imagePath) {
                        return imageService.resolveProductImage(imagePath, getClass());
                    }

                    @Override
                    public String normalizeText(String input) {
                        return MarketplaceController.this.normalizeText(input);
                    }

                    @Override
                    public String normalizeTypeForDisplay(String rawType) {
                        return MarketplaceController.this.normalizeTypeForDisplay(rawType);
                    }

                    @Override
                    public void onQuantityDelta(CartItem item, int delta) {
                        MarketplaceController.this.adjustCartQuantity(item, delta);
                    }
                }));
            }
        }

        if (cartCountLabel != null) {
            cartCountLabel.setText("Articles: " + cartSessionService.countItems(getCurrentUserId()));
        }
        if (cartTotalLabel != null) {
            cartTotalLabel.setText("Total: " + String.format("%.2f", cartSessionService.getTotal(getCurrentUserId())) + " TND");
        }
    }

    private void processCheckout(List<CartItem> cartItems) {
        checkoutFeature.openCheckoutDialog(cartItems, new MarketplaceCheckoutFeature.Host() {
            @Override
            public int getCurrentUserId() {
                return MarketplaceController.this.getCurrentUserId();
            }

            @Override
            public Produit findLatestProduitById(int produitId) {
                return MarketplaceController.this.findLatestProduitById(produitId);
            }

            @Override
            public String normalizeText(String input) {
                return MarketplaceController.this.normalizeText(input);
            }

            @Override
            public void applyDialogStylesheet(DialogPane pane) {
                MarketplaceController.this.applyDialogStylesheet(pane);
            }

            @Override
            public void showToast(String title, String message, boolean success) {
                MarketplaceController.this.showToast(title, message, success);
            }

            @Override
            public void showSqlAlert(SQLException exception) {
                MarketplaceController.this.showSqlAlert(exception);
            }

            @Override
            public void onCheckoutSuccess(int commandeId, int itemCount, double totalAmount) {
                openSellerOnBoughtOrders = true;
                showingBoughtOrders = true;
                loadCommandes();
                loadProduits();
                refreshStats();
                renderCartOverlay();
                closeCartOverlay();
                if (statusLabel != null) {
                    statusLabel.setText("Commande #" + commandeId + " creee avec succes");
                }
            }
        });
    }

    private void adjustCartQuantity(CartItem item, int delta) {
        if (item == null || item.getProduit() == null || delta == 0) {
            return;
        }

        Produit produit = item.getProduit();

        if (delta > 0) {
            Produit latest = findLatestProduitById(produit.getId());
            int availableStock = latest == null ? produit.getQuantiteStock() : latest.getQuantiteStock();
            if (item.getQuantite() >= availableStock) {
                showAlert("Stock", "Stock maximum atteint pour ce produit (" + availableStock + ").");
                return;
            }
            cartSessionService.addProduit(getCurrentUserId(), produit, 1);
            showToast("Quantite augmentee.", true);
        } else {
            if (item.getQuantite() <= 1) {
                cartSessionService.removeProduit(getCurrentUserId(), produit.getId());
                showToast("Article retire du panier.", true);
            } else {
                item.increment(-1);
                showToast("Quantite reduite.", true);
            }
        }

        renderCartOverlay();
        updateCartStatus();
    }

    private Produit findLatestProduitById(int produitId) {
        try {
            List<Produit> all = produitService.afficher();
            for (Produit p : all) {
                if (p.getId() == produitId) {
                    return p;
                }
            }
        } catch (SQLException e) {
            showSqlAlert(e);
        }
        return null;
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
                .append(String.format("%.2f", cartSessionService.getTotal(getCurrentUserId())))
                .append(" TND\n")
                .append("Nombre d'articles: ")
                .append(cartSessionService.countItems(getCurrentUserId()));
        return builder.toString();
    }

    private void addToCart(Produit produit, int quantite) {
        if (produit == null) {
            return;
        }
        if (produit.getVendeurId() > 0 && produit.getVendeurId() == getCurrentUserId()) {
            showAlert("Panier", "Vous ne pouvez pas ajouter vos propres produits au panier.");
            return;
        }
        if (produit.getQuantiteStock() <= 0) {
            showAlert("Stock", "Ce produit est en rupture de stock.");
            return;
        }

        List<CartItem> cartItems = cartSessionService.getItems(getCurrentUserId());
        int currentQty = 0;
        for (CartItem item : cartItems) {
            if (item.getProduit() != null && item.getProduit().getId() == produit.getId()) {
                currentQty = item.getQuantite();
                break;
            }
        }

        Produit latest = findLatestProduitById(produit.getId());
        int availableStock = latest == null ? produit.getQuantiteStock() : latest.getQuantiteStock();
        if (currentQty + quantite > availableStock) {
            showAlert("Stock", "Stock insuffisant. Disponible: " + availableStock + ".");
            return;
        }

        cartSessionService.addProduit(getCurrentUserId(), produit, quantite);
        updateCartStatus();
        if (statusLabel != null) {
            statusLabel.setText(normalizeText(safe(produit.getNom())) + " ajoute au panier");
        }
    }

    private void updateCartStatus() {
        if (statusLabel == null) {
            return;
        }
        int count = cartSessionService.countItems(getCurrentUserId());
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
            produitTable.setItems(FXCollections.observableArrayList(filterSellerProducts(source)));
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

    private List<Produit> filterSellerProducts(List<Produit> produits) {
        Integer sessionUserId = getSessionUserId();
        if (sessionUserId == null || produits == null || produits.isEmpty()) {
            return new ArrayList<>();
        }

        List<Produit> sellerProducts = new ArrayList<>();
        for (Produit produit : produits) {
            if (produit != null && produit.getVendeurId() == sessionUserId) {
                sellerProducts.add(produit);
            }
        }
        return sellerProducts;
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
        clearProductFieldErrors();
        updatePublishPreview();
        animateOverlayIn(modalOverlay);
    }

    @FXML
    public void handleProductFormInput() {
        updatePublishPreview();
        clearProductFieldErrors();
    }

    @FXML
    public void suggestDescriptionForProduct() {
        String productName = safe(fldNom == null ? "" : fldNom.getText()).trim();

        if (productName.isBlank()) {
            showToast("IA", "Ajoutez d'abord un nom de produit.", false);
            return;
        }

        // AI service not yet implemented - placeholder for future feature
        showToast("IA", "Service de suggestion par IA non disponible actuellement.", false);
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
            publishPreviewImage.setImage(imageService.resolveProductImage(fldImage == null ? "" : fldImage.getText(), getClass()));
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

            try {
                String relativePath = storeImageForSharedProjects(selected);
                fldImage.setText(relativePath);
                if (statusLabel != null) {
                    statusLabel.setText("Image enregistree: " + relativePath);
                }
                showToast("Image importee avec succes.", true);
                updatePublishPreview();
            } catch (IOException e) {
                showAlert("Image", "Echec import image: " + e.getMessage());
            }
        }
    }

    private String storeImageForSharedProjects(File sourceFile) throws IOException {
        String original = sourceFile.getName();
        String extension = "";
        int dot = original.lastIndexOf('.');
        if (dot >= 0) {
            extension = original.substring(dot).toLowerCase();
        }

        String generatedName = UUID.randomUUID() + extension;
        Path uploadsDir = imageService.getSharedUploadsDir().resolve("produits");
        Files.createDirectories(uploadsDir);

        Path target = uploadsDir.resolve(generatedName);
        Files.copy(sourceFile.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
        return "uploads/produits/" + generatedName;
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
        clearProductFieldErrors();
        updatePublishPreview();
        animateOverlayIn(modalOverlay);
    }

    @FXML
    public void saveFormProduct() {
        if (!validateProductForm(true)) {
            return;
        }

        Double prix = parseDouble(fldPrix.getText());
        Double promoPrix = parseDouble(fldPromoPrice.getText());
        Integer stock = parseInteger(fldStock.getText());

        Produit p = currentEditProduit == null ? new Produit() : currentEditProduit;
        String selectedCategory = fldCategorie == null ? "" : safe(fldCategorie.getValue()).trim();
        String selectedType = fldType == null ? "" : safe(fldType.getValue()).trim();

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
        p.setVendeurId(getCurrentUserId());
        
        try {
            if (currentEditProduit == null) {
                produitService.ajouter(p);
                showToast("Annonce ajoutee avec succes.", true);
            } else {
                produitService.modifier(p);
                showToast("Annonce modifiee avec succes.", true);
            }
            loadProduits();
            refreshStats();
            closeModal();
        } catch (SQLException e) {
            showSqlAlert(e);
        }
    }

    private boolean validateProductForm(boolean strictMode) {
        clearProductFieldErrors();
        boolean valid = true;

        String nomErr = MarketplaceValidator.validateProductName(fldNom == null ? "" : fldNom.getText());
        if (nomErr != null) {
            showFieldError(fldNom, errNom, nomErr);
            valid = false;
        }

        String categorieErr = MarketplaceValidator.validateProductCategory(
                fldCategorie == null ? null : fldCategorie.getValue(),
                CATEGORY_PLACEHOLDER
        );
        if (categorieErr != null) {
            showFieldError(fldCategorie, errCategorie, categorieErr);
            valid = false;
        }

        String typeErr = MarketplaceValidator.validateOfferType(fldType == null ? null : fldType.getValue());
        if (typeErr != null) {
            showFieldError(fldType, errType, typeErr);
            valid = false;
        }

        String prixErr = MarketplaceValidator.validatePositivePrice(fldPrix == null ? "" : fldPrix.getText());
        if (prixErr != null) {
            showFieldError(fldPrix, errPrix, prixErr);
            valid = false;
        }

        String stockErr = MarketplaceValidator.validateStock(fldStock == null ? "" : fldStock.getText());
        if (stockErr != null) {
            showFieldError(fldStock, errStock, stockErr);
            valid = false;
        }

        String descErr = MarketplaceValidator.validateProductDescription(fldDescription == null ? "" : fldDescription.getText());
        if (descErr != null) {
            showFieldError(fldDescription, errDescription, descErr);
            valid = false;
        }

        String promoErr = MarketplaceValidator.validatePromoPrice(
                chkPromo != null && chkPromo.isSelected(),
                fldPromoPrice == null ? "" : fldPromoPrice.getText(),
                fldPrix == null ? "" : fldPrix.getText()
        );
        if (promoErr != null) {
            showFieldError(fldPromoPrice, errPromoPrice, promoErr);
            valid = false;
        }

        if (!valid && strictMode) {
            showAlert("Validation", "Veuillez corriger les champs en rouge.");
        }

        return valid;
    }

    private void clearProductFieldErrors() {
        clearFieldError(fldNom, errNom);
        clearFieldError(fldCategorie, errCategorie);
        clearFieldError(fldType, errType);
        clearFieldError(fldPrix, errPrix);
        clearFieldError(fldStock, errStock);
        clearFieldError(fldDescription, errDescription);
        clearFieldError(fldPromoPrice, errPromoPrice);
    }

    private void showFieldError(Control field, Label label, String message) {
        if (field != null && !field.getStyleClass().contains("field-error")) {
            field.getStyleClass().add("field-error");
        }
        if (label != null) {
            label.setText(message);
            label.setManaged(true);
            label.setVisible(true);
        }
    }

    private void clearFieldError(Control field, Label label) {
        if (field != null) {
            field.getStyleClass().remove("field-error");
        }
        if (label != null) {
            label.setText(" ");
            label.setManaged(false);
            label.setVisible(false);
        }
    }

    @FXML
    public void showProductDetails(Produit p) {
        if (p == null || detailsOverlay == null) {
            return;
        }

        currentDetailsProduit = p;

        detailsTitle.setText(normalizeText(safe(p.getNom())));
        detailsCategory.setText(normalizeText(safe(p.getCategorie())));
        detailsDesc.setText(normalizeText(safe(p.getDescription())));
        detailsStock.setText("Stock: " + p.getQuantiteStock() + " unites disponibles");
        detailsLocation.setText(normalizeText(safe(p.getLocationAddress())).isEmpty()
                ? "Localisation: Non precisee"
                : "Localisation: " + normalizeText(safe(p.getLocationAddress())));
        detailsId.setText("# ID: " + p.getId());

        String displayType = normalizeTypeForDisplay(p.getType());
        if (detailsTypeBadge != null) {
            detailsTypeBadge.setText(displayType.toUpperCase());
        }
        if (detailsTypePill != null) {
            detailsTypePill.setText(displayType);
        }

        detailsImageView.setImage(imageService.resolveProductImage(p.getImage(), getClass()));

        int max = Math.max(1, p.getQuantiteStock());
        detailsQuantitySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, max, 1));

        if (p.isPromotion()) {
            detailsPrice.setText(String.format("%.2f", p.getPromotionPrice()));
            detailsOldPrice.setText(String.format("%.2f TND", p.getPrix()));
            detailsOldPrice.setVisible(true);
            detailsOldPrice.setManaged(true);
            detailsConvertedPrice.setText(String.format("~ %.2f EUR | %.2f USD", p.getPromotionPrice() * 0.29, p.getPromotionPrice() * 0.32));
        } else {
            detailsPrice.setText(String.format("%.2f", p.getPrix()));
            detailsOldPrice.setVisible(false);
            detailsOldPrice.setManaged(false);
            detailsConvertedPrice.setText(String.format("~ %.2f EUR | %.2f USD", p.getPrix() * 0.29, p.getPrix() * 0.32));
        }

        animateOverlayIn(detailsOverlay);
    }

    @FXML
    public void openMessagingForCurrentProduct() {
        if (currentDetailsProduit == null) {
            openMessagingCenter();
            return;
        }
        openMessagingForProduct(currentDetailsProduit);
    }

    @FXML
    public void addCurrentDetailsToCart() {
        if (currentDetailsProduit == null) {
            return;
        }
        int quantity = 1;
        if (detailsQuantitySpinner != null && detailsQuantitySpinner.getValue() != null) {
            quantity = Math.max(1, detailsQuantitySpinner.getValue());
        }
        addToCart(currentDetailsProduit, quantity);
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
            showToast("Annonce supprimee.", true);
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
                showToast("Commande ajoutee.", true);
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
                String currentStatus = safe(commande.getStatut()).trim().toLowerCase();
                String nextStatus = safe(c.getStatut()).trim().toLowerCase();
                CommandeService.OrderActor actor = showingBoughtOrders
                        ? CommandeService.OrderActor.BUYER
                        : CommandeService.OrderActor.SELLER;

                if (currentStatus.equals(nextStatus)) {
                    showToast("Aucun changement de statut.", false);
                    return;
                }

                commandeService.updateStatut(
                        commande.getId(),
                        nextStatus,
                        getCurrentUserId(),
                        actor,
                        "Mise a jour depuis Marketplace"
                );
                loadCommandes();
                showToast("Statut commande mis a jour.", true);
            } catch (SQLException e) {
                showSqlAlert(e);
            }
        });
    }

    private Dialog<Commande> buildCommandeDialog(Commande existing) {
        Dialog<Commande> dialog = new Dialog<>();
        boolean isEditing = existing != null;
        dialog.setTitle(isEditing ? "Mettre a jour statut" : "Ajouter Commande");
        dialog.getDialogPane().getStyleClass().add("commande-dialog-pane");
        applyDialogStylesheet(dialog.getDialogPane());
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
                if (!statusOptions.contains(candidate)) {
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
        TextField clientIdField = new TextField(existing == null ? String.valueOf(getCurrentUserId()) : String.valueOf(existing.getClientId()));

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

        Node saveButtonNode = dialog.getDialogPane().lookupButton(saveBtn);
        Node cancelButtonNode = dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        if (saveButtonNode != null) {
            saveButtonNode.getStyleClass().add("commande-save-btn");
        }
        if (cancelButtonNode != null) {
            cancelButtonNode.getStyleClass().add("commande-cancel-btn");
        }

        dialog.setResultConverter(btn -> {
            if (btn != saveBtn) {
                return null;
            }
            String err = MarketplaceValidator.validateCommande(
                    statutField.getValue(),
                    paiementField.getValue(),
                    adresseField.getText(),
                    montantField.getText(),
                    clientIdField.getText()
            );
            if (err != null) {
                showAlert("Validation", err);
                return null;
            }
            Double montant = parseDouble(montantField.getText());
            Integer clientId = parseInteger(clientIdField.getText());
            Commande c = new Commande();
            c.setStatut(safe(statutField.getValue()).trim());
            c.setModePaiement(safe(paiementField.getValue()).trim());
            c.setAdresseLivraison(adresseField.getText().trim());
            c.setMontantTotal(montant);
            c.setPaymentRef(paymentRefField.getText().trim());
            c.setClientId(clientId);
            return c;
        });

        return dialog;
    }

    private String resolveStatusLabel(String rawStatus) {
        String value = safe(rawStatus).trim().toLowerCase();
        if ("confirmee".equals(value)) {
            return "Confirmee";
        }
        if ("livree".equals(value)) {
            return "Livree";
        }
        if ("annulee".equals(value)) {
            return "Annulee";
        }
        return "En attente";
    }

    private String resolveStatusStyle(String rawStatus) {
        String value = safe(rawStatus).trim().toLowerCase();
        if ("confirmee".equals(value)) {
            return "confirmed";
        }
        if ("livree".equals(value)) {
            return "delivered";
        }
        if ("annulee".equals(value)) {
            return "cancelled";
        }
        return "pending";
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

    private String resolvePaymentStyle(String rawPayment) {
        String value = safe(rawPayment).trim().toLowerCase();
        return "carte".equals(value) ? "card" : "home";
    }

    private void applyDialogStylesheet(DialogPane pane) {
        if (pane == null) {
            return;
        }
        URL css = getClass().getResource("/css/style.css");
        if (css == null) {
            return;
        }
        String cssUrl = css.toExternalForm();
        if (!pane.getStylesheets().contains(cssUrl)) {
            pane.getStylesheets().add(cssUrl);
        }
    }

    private void handleDeleteCommande(Commande commande) {
        if (!confirm("Supprimer commande", "Supprimer la commande #" + commande.getId() + " ?")) {
            return;
        }
        try {
            commandeService.supprimer(commande.getId());
            loadCommandes();
            refreshStats();
            showToast("Commande supprimee.", true);
        } catch (SQLException e) {
            showSqlAlert(e);
        }
    }

    private void downloadFacture(Commande commande) {
        try {
            javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
            chooser.setTitle("Enregistrer la facture");
            chooser.setInitialFileName("facture_commande_" + commande.getId() + ".pdf");
            chooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("PDF", "*.pdf"));

            java.io.File selected = chooser.showSaveDialog(mainContent.getScene().getWindow());
            if (selected == null) {
                return;
            }

            services.MarketplaceInvoicePdfService.InvoiceData invoice = new services.MarketplaceInvoicePdfService.InvoiceData();
            invoice.commandeId = commande.getId();
            entities.User currentUser = utils.SessionManager.getInstance().getCurrentUser();
            invoice.clientName = currentUser != null ? currentUser.getFirstName() + " " + currentUser.getLastName() : "Client " + commande.getClientId();
            invoice.paymentMode = resolvePaymentLabel(commande.getModePaiement());
            invoice.deliveryAddress = commande.getAdresseLivraison();
            invoice.itemCount = 1; // Defaulting to 1 as count isn't stored in Commande directly
            invoice.totalAmount = commande.getMontantTotal();
            invoice.issuedAt = commande.getCreatedAt();

            services.MarketplaceInvoicePdfService invoicePdfService = new services.MarketplaceInvoicePdfService();
            invoicePdfService.generateInvoice(selected.toPath(), invoice);
            showToast("Facture PDF enregistree.", true);
        } catch (Exception ex) {
            showToast("Echec generation PDF: " + ex.getMessage(), false);
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
                showToast("Element wishlist ajoute.", true);
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
                showToast("Element wishlist modifie.", true);
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

        TextField userIdField = new TextField(existing == null ? String.valueOf(getCurrentUserId()) : String.valueOf(existing.getUserId()));
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
            String err = MarketplaceValidator.validateWishlist(
                    userIdField.getText(),
                    produitIdField.getText()
            );
            if (err != null) {
                showAlert("Validation", err);
                return null;
            }
            Integer userId = parseInteger(userIdField.getText());
            Integer produitId = parseInteger(produitIdField.getText());
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
            showToast("Element wishlist supprime.", true);
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
                showToast("Message ajoute.", true);
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
                showToast("Message modifie.", true);
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
        TextField senderIdField = new TextField(existing == null ? String.valueOf(getCurrentUserId()) : String.valueOf(existing.getSenderId()));
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
            String err = MarketplaceValidator.validateMessage(
                    conversationIdField.getText(),
                    senderIdField.getText(),
                    contentField.getText()
            );
            if (err != null) {
                showAlert("Validation", err);
                return null;
            }
            Integer conversationId = parseInteger(conversationIdField.getText());
            Integer senderId = parseInteger(senderIdField.getText());

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
            showToast("Message supprime.", true);
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
        showNotice(title, message);
    }

    private void showSqlAlert(SQLException exception) {
        showToast("Erreur SQL", "Operation base de donnees echouee: " + exception.getMessage(), false);
        if (statusLabel != null) {
            statusLabel.setText("Erreur SQL detectee");
        }
    }

    private int getCurrentUserId() {
        entities.User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null && currentUser.getId() > 0) {
            return currentUser.getId();
        }
        return DEFAULT_GUEST_USER_ID;
    }

    private Integer getSessionUserId() {
        entities.User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null && currentUser.getId() > 0) {
            return currentUser.getId();
        }
        return null;
    }

    private int getFallbackSellerId() {
        int currentUserId = getCurrentUserId();
        return currentUserId == DEFAULT_FALLBACK_SELLER_ID
                ? DEFAULT_GUEST_USER_ID
                : DEFAULT_FALLBACK_SELLER_ID;
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
        if (trimmed.contains("Ãƒ") || trimmed.contains("Ã‚")) {
            return new String(trimmed.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
        }
        return trimmed;
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
                Image loadedImage = imageService.resolveProductImage(p.getImage(), getClass());
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
            if (wishlistFeature != null && wishlistFeature.isProductInWishlist(p.getId())) {
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

            if (purchasedProductIds.contains(p.getId())) {
                Label boughtBadge = new Label("ACHETE");
                boughtBadge.getStyleClass().add("promo-badge");
                StackPane.setAlignment(boughtBadge, javafx.geometry.Pos.BOTTOM_RIGHT);
                StackPane.setMargin(boughtBadge, new javafx.geometry.Insets(0, 8, 8, 0));
                imageContainer.getChildren().add(boughtBadge);
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
            boolean isOwnOffer = p.getVendeurId() > 0 && p.getVendeurId() == getCurrentUserId();
            boolean isAlreadyBought = purchasedProductIds.contains(p.getId());
            if (isOwnOffer) {
                btnAddCart.setText("C ton offre");
                btnAddCart.setDisable(true);
                btnAddCart.getStyleClass().add("own-offer");
            } else if (isAlreadyBought) {
                btnAddCart.setText("Deja achete");
                btnAddCart.setDisable(true);
                btnAddCart.getStyleClass().add("own-offer");
            } else {
                btnAddCart.setOnAction(e -> {
                    addToCart(p, 1);
                    e.consume();
                });
            }

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
            attachCardHoverMotion(card);
            animateCardEntry(card, productGrid.getChildren().size());
            
            productGrid.getChildren().add(card);
        }
    }

    private void animateCardEntry(Node card, int index) {
        if (card == null) {
            return;
        }

        card.setOpacity(0);
        card.setTranslateY(14);

        FadeTransition fade = new FadeTransition(Duration.millis(220), card);
        fade.setFromValue(0);
        fade.setToValue(1);

        TranslateTransition slide = new TranslateTransition(Duration.millis(220), card);
        slide.setFromY(14);
        slide.setToY(0);

        ParallelTransition in = new ParallelTransition(fade, slide);
        in.setDelay(Duration.millis(Math.min(index, 10) * 26L));
        in.play();
    }

    private void attachCardHoverMotion(VBox card) {
        if (card == null) {
            return;
        }

        card.setOnMouseEntered(e -> {
            ScaleTransition grow = new ScaleTransition(Duration.millis(110), card);
            grow.setToX(1.015);
            grow.setToY(1.015);
            grow.play();
        });

        card.setOnMouseExited(e -> {
            ScaleTransition reset = new ScaleTransition(Duration.millis(110), card);
            reset.setToX(1.0);
            reset.setToY(1.0);
            reset.play();
        });
    }

    private void attachWishlistCardMotion(VBox card) {
        if (card == null) {
            return;
        }

        card.setOnMouseEntered(e -> {
            ScaleTransition grow = new ScaleTransition(Duration.millis(110), card);
            grow.setToX(1.012);
            grow.setToY(1.012);
            grow.play();
        });

        card.setOnMouseExited(e -> {
            ScaleTransition reset = new ScaleTransition(Duration.millis(110), card);
            reset.setToX(1.0);
            reset.setToY(1.0);
            reset.play();
        });
    }
}




