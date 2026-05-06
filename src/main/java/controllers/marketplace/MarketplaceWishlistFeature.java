package controllers.marketplace;

import entities.Produit;
import entities.WishlistItem;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import services.MarketplaceImageService;
import services.ProduitService;
import services.WishlistService;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

public class MarketplaceWishlistFeature {
    private static final String TYPE_LOCATION = "Location";

    private final MarketplaceWishlistState state;
    private final WishlistService wishlistService;
    private final ProduitService produitService;
    private final MarketplaceImageService imageService;
    private final FlowPane wishlistGrid;
    private final Label wishlistMetaLabel;
    private final Button btnWishlistCenter;
    private final IntSupplier currentUserIdSupplier;
    private final java.util.function.Supplier<Integer> sessionUserIdSupplier;
    private final BiConsumer<String, String> showAlert;
    private final Consumer<SQLException> showSqlAlert;
    private final BiConsumer<String, Boolean> showToast;
    private final Consumer<Produit> showProductDetails;
    private final Runnable renderCurrentPage;
    private final java.util.function.Function<String, String> normalizeText;
    private final java.util.function.Function<String, String> normalizeTypeForDisplay;
    private final java.util.function.Supplier<Class<?>> resourceClassSupplier;

    public MarketplaceWishlistFeature(
            MarketplaceWishlistState state,
            WishlistService wishlistService,
            ProduitService produitService,
            MarketplaceImageService imageService,
            FlowPane wishlistGrid,
            Label wishlistMetaLabel,
            Button btnWishlistCenter,
            IntSupplier currentUserIdSupplier,
            java.util.function.Supplier<Integer> sessionUserIdSupplier,
            BiConsumer<String, String> showAlert,
            Consumer<SQLException> showSqlAlert,
            BiConsumer<String, Boolean> showToast,
            Consumer<Produit> showProductDetails,
            Runnable renderCurrentPage,
            java.util.function.Function<String, String> normalizeText,
            java.util.function.Function<String, String> normalizeTypeForDisplay,
            java.util.function.Supplier<Class<?>> resourceClassSupplier) {
        this.state = state;
        this.wishlistService = wishlistService;
        this.produitService = produitService;
        this.imageService = imageService;
        this.wishlistGrid = wishlistGrid;
        this.wishlistMetaLabel = wishlistMetaLabel;
        this.btnWishlistCenter = btnWishlistCenter;
        this.currentUserIdSupplier = currentUserIdSupplier;
        this.sessionUserIdSupplier = sessionUserIdSupplier;
        this.showAlert = showAlert;
        this.showSqlAlert = showSqlAlert;
        this.showToast = showToast;
        this.showProductDetails = showProductDetails;
        this.renderCurrentPage = renderCurrentPage;
        this.normalizeText = normalizeText;
        this.normalizeTypeForDisplay = normalizeTypeForDisplay;
        this.resourceClassSupplier = resourceClassSupplier;
    }

    public void openWishlistCenter() {
        refreshWishlistState();
    }

    public void closeWishlistCenter() {
        // overlay is managed by the controller; this feature only refreshes state
    }

    public void toggleWishlistForProduct(Produit produit) {
        if (produit == null) {
            return;
        }

        Integer sessionUserId = sessionUserIdSupplier.get();
        if (sessionUserId == null) {
            showToast.accept("Connectez-vous pour utiliser la wishlist.", false);
            return;
        }

        try {
            if (state.wishlistProductIds.contains(produit.getId())) {
                wishlistService.supprimerByUserAndProduit(sessionUserId, produit.getId());
                showToast.accept("Retire de la wishlist.", true);
            } else {
                wishlistService.ajouter(new WishlistItem(sessionUserId, produit.getId()));
                showToast.accept("Ajoute a la wishlist.", true);
            }

            loadWishlist();
            refreshWishlistState();
            renderCurrentPage.run();
        } catch (SQLException e) {
            showSqlAlert.accept(e);
        }
    }

    public void refreshWishlistState() {
        Integer sessionUserId = sessionUserIdSupplier.get();
        state.wishlistProductIds.clear();
        if (sessionUserId == null) {
            refreshWishlistBadge();
            renderWishlistGrid();
            return;
        }
        try {
            Set<Integer> availableProductIds = new java.util.HashSet<>();
            for (Produit produit : produitService.afficher()) {
                if (produit != null) {
                    availableProductIds.add(produit.getId());
                }
            }

            List<WishlistItem> userItems = wishlistService.getByUser(sessionUserId);
            for (WishlistItem item : userItems) {
                if (availableProductIds.contains(item.getProduitId())) {
                    state.wishlistProductIds.add(item.getProduitId());
                }
            }
            refreshWishlistBadge();
            renderWishlistGrid();
        } catch (SQLException e) {
            showSqlAlert.accept(e);
        }
    }

    public void refreshWishlistBadge() {
        if (btnWishlistCenter == null) {
            return;
        }
        int count = state.wishlistProductIds.size();
        btnWishlistCenter.setText(count <= 0 ? "Wishlist" : "Wishlist " + count);
    }

    public void loadWishlist() {
        // kept for compatibility with the controller refresh flow
        refreshWishlistState();
    }

    public boolean isProductInWishlist(int productId) {
        return state.wishlistProductIds.contains(productId);
    }

    public void renderWishlistGrid() {
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
            for (Integer productId : state.wishlistProductIds) {
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

                ImageView imageView = new ImageView(imageService.resolveProductImage(produit.getImage(), resourceClassSupplier.get()));
                imageView.setFitWidth(260);
                imageView.setFitHeight(140);
                imageView.setPreserveRatio(false);
                imageView.setSmooth(true);
                StackPane imageWrap = new StackPane(imageView);
                imageWrap.getStyleClass().add("wishlist-image-wrap");

                Label title = new Label(normalizeText.apply(safe(produit.getNom())));
                title.getStyleClass().add("wishlist-card-title");
                title.setWrapText(true);

                Label categoryChip = new Label(normalizeText.apply(safe(produit.getCategorie())));
                categoryChip.getStyleClass().add("wishlist-chip");

                Label typeChip = new Label(normalizeTypeForDisplay.apply(produit.getType()));
                typeChip.getStyleClass().addAll("wishlist-chip", "wishlist-chip-type");
                if (TYPE_LOCATION.equalsIgnoreCase(normalizeTypeForDisplay.apply(produit.getType()))) {
                    typeChip.getStyleClass().add("location");
                }

                HBox chips = new HBox(6, categoryChip, typeChip);
                chips.getStyleClass().add("wishlist-chip-row");
                chips.setAlignment(Pos.CENTER_LEFT);

                double displayPrice = (produit.isPromotion() && produit.getPromotionPrice() > 0)
                        ? produit.getPromotionPrice()
                        : produit.getPrix();
                Label price = new Label(String.format("%.2f TND", displayPrice));
                price.getStyleClass().add("wishlist-card-price");

                Button viewBtn = new Button("Voir");
                viewBtn.getStyleClass().add("wishlist-view-btn");
                viewBtn.setOnAction(e -> showProductDetails.accept(produit));

                Button removeBtn = new Button("Retirer");
                removeBtn.getStyleClass().add("wishlist-remove-btn");
                removeBtn.setOnAction(e -> toggleWishlistForProduct(produit));

                HBox actions = new HBox(8, viewBtn, removeBtn);
                actions.getStyleClass().add("wishlist-actions");
                actions.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(viewBtn, Priority.ALWAYS);
                HBox.setHgrow(removeBtn, Priority.ALWAYS);
                viewBtn.setMaxWidth(Double.MAX_VALUE);
                removeBtn.setMaxWidth(Double.MAX_VALUE);

                card.getChildren().addAll(imageWrap, title, chips, price, actions);
                wishlistGrid.getChildren().add(card);
            }
        } catch (SQLException e) {
            showSqlAlert.accept(e);
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
