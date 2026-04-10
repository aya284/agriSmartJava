package controllers;

import entities.Commande;
import entities.MarketplaceMessage;
import entities.Produit;
import entities.WishlistItem;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
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
import javafx.scene.layout.Priority;
import javafx.scene.layout.HBox;
import services.CommandeService;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class MarketplaceController implements Initializable {

    private static final String CATEGORY_PLACEHOLDER = "Choisir...";
    private static final String CATEGORY_ALL = "Toutes";
    private static final String TYPE_ALL = "Tous";
    private static final String TYPE_VENTE = "Vente";
    private static final String TYPE_LOCATION = "Location";
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

    

    private final ProduitService produitService = new ProduitService();
    private final CommandeService commandeService = new CommandeService();
    private final WishlistService wishlistService = new WishlistService();
    private final MarketplaceMessageService messageService = new MarketplaceMessageService();

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
            if (produitTable != null) {
                produitTable.setItems(FXCollections.observableArrayList(produits));
            }
            if (productGrid != null) populateProductGrid(produits);
            if (statusLabel != null) {
                statusLabel.setText(produits.size() + " produits charges");
            }
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
            return;
        }
        try {
            List<WishlistItem> items = wishlistService.afficher();
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

    private void refreshStats() { }

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

            if (produitTable != null) {
                produitTable.setItems(FXCollections.observableArrayList(filtered));
            }
            if (productGrid != null) populateProductGrid(filtered);
            if (statusLabel != null) {
                statusLabel.setText(filtered.size() + " resultat(s)");
            }
        } catch (SQLException e) {
            showSqlAlert(e);
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
            btnHeart.setOnAction(e -> {
                boolean active = btnHeart.getStyleClass().contains("active");
                if (active) {
                    btnHeart.getStyleClass().remove("active");
                    btnHeart.setText("\u2661");
                } else {
                    btnHeart.getStyleClass().add("active");
                    btnHeart.setText("\u2665");
                }
                if (statusLabel != null) {
                    statusLabel.setText(active
                            ? normalizeText(safe(p.getNom())) + " retire de la wishlist"
                            : normalizeText(safe(p.getNom())) + " ajoute a la wishlist");
                }
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
                if (statusLabel != null) {
                    statusLabel.setText(normalizeText(safe(p.getNom())) + " ajoute au panier");
                }
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

