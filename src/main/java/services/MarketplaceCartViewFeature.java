package services;

import entities.CartItem;
import entities.Produit;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class MarketplaceCartViewFeature {

    public interface Host {
        javafx.scene.image.Image resolveProductImage(String imagePath);
        String normalizeText(String input);
        String normalizeTypeForDisplay(String rawType);
        void onQuantityDelta(CartItem item, int delta);
    }

    public HBox buildCartRow(CartItem item, Host host) {
        Produit produit = item.getProduit();
        StackPane thumbWrap = new StackPane();
        thumbWrap.getStyleClass().add("cart-thumb-wrap");
        ImageView thumb = new ImageView(host.resolveProductImage(produit == null ? "" : produit.getImage()));
        thumb.setFitWidth(74);
        thumb.setFitHeight(58);
        thumb.setPreserveRatio(false);
        thumb.setSmooth(true);
        thumbWrap.getChildren().add(thumb);

        Label nameLabel = new Label(host.normalizeText(safe(produit == null ? "" : produit.getNom())));
        nameLabel.getStyleClass().add("cart-item-name");

        Label detailLabel = new Label(
                "Qte: " + item.getQuantite()
                        + "   |   PU: " + String.format("%.2f", item.getUnitPrice()) + " TND"
                        + "   |   Ligne: " + String.format("%.2f", item.getLineTotal()) + " TND"
        );
        detailLabel.getStyleClass().add("cart-item-meta");

        Label typeLabel = new Label(host.normalizeTypeForDisplay(produit == null ? "" : produit.getType()));
        typeLabel.getStyleClass().add("cart-type-pill");

        VBox left = new VBox(4, nameLabel, detailLabel, typeLabel);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button minusBtn = new Button("-");
        minusBtn.getStyleClass().add("cart-qty-btn");

        Label qtyLabel = new Label(String.valueOf(item.getQuantite()));
        qtyLabel.getStyleClass().add("cart-qty-value");

        Button plusBtn = new Button("+");
        plusBtn.getStyleClass().add("cart-qty-btn");

        minusBtn.setOnAction(e -> {
            host.onQuantityDelta(item, -1);
            e.consume();
        });

        plusBtn.setOnAction(e -> {
            host.onQuantityDelta(item, 1);
            e.consume();
        });

        HBox qtyBox = new HBox(8, minusBtn, qtyLabel, plusBtn);
        qtyBox.setAlignment(Pos.CENTER_LEFT);
        qtyBox.getStyleClass().add("cart-qty-box");

        Label totalBadge = new Label(String.format("%.2f TND", item.getLineTotal()));
        totalBadge.getStyleClass().add("cart-line-total");

        HBox row = new HBox(12, thumbWrap, left, spacer, qtyBox, totalBadge);
        row.getStyleClass().add("cart-line-row");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 12, 10, 12));
        return row;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
