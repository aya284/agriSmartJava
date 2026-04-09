package controllers;

import entities.Produit;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ProductDetailsController {

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

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String normalizeText(String input) {
        if (input == null) {
            return "";
        }
        return input.trim();
    }

    private Image loadPlaceholderImage() {
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
        return null;
    }

    private Image resolveProductImage(String rawImagePath) {
        String raw = safe(rawImagePath).trim();
        if (raw.isEmpty()) {
            return loadPlaceholderImage();
        }

        try {
            if (raw.startsWith("http://") || raw.startsWith("https://") || raw.startsWith("file:")) {
                Image direct = new Image(raw, true);
                return direct.isError() ? loadPlaceholderImage() : direct;
            }

            Path absolutePath = Paths.get(raw).toAbsolutePath();
            if (Files.exists(absolutePath)) {
                Image local = new Image(absolutePath.toUri().toString(), true);
                return local.isError() ? loadPlaceholderImage() : local;
            }

            Path projectRelative = Paths.get(System.getProperty("user.dir"), raw).toAbsolutePath();
            if (Files.exists(projectRelative)) {
                Image local = new Image(projectRelative.toUri().toString(), true);
                return local.isError() ? loadPlaceholderImage() : local;
            }
        } catch (Exception ignored) {
            return loadPlaceholderImage();
        }

        return loadPlaceholderImage();
    }

    public void setProduct(Produit p) {
        if (p == null) {
            return;
        }

        detailsTitle.setText(normalizeText(safe(p.getNom())));
        detailsCategory.setText(normalizeText(safe(p.getCategorie())) + " - " + normalizeText(safe(p.getType())));
        detailsDesc.setText(normalizeText(safe(p.getDescription())));
        detailsStock.setText("Stock: " + p.getQuantiteStock() + " unites disponibles");
        detailsLocation.setText(normalizeText(safe(p.getLocationAddress())).isEmpty()
                ? "Localisation: Non precisee"
                : "Localisation: " + normalizeText(safe(p.getLocationAddress())));
        detailsId.setText("# ID: " + p.getId());

        Image detailsImage = resolveProductImage(p.getImage());
        detailsImageView.setImage(detailsImage);

        int max = Math.max(1, p.getQuantiteStock());
        detailsQuantitySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, max, 1));

        if (p.isPromotion()) {
            detailsPrice.setText(String.format("%.2f TND", p.getPromotionPrice()));
            detailsOldPrice.setText(String.format("%.2f TND", p.getPrix()));
            detailsOldPrice.setVisible(true);
            detailsOldPrice.setManaged(true);
            detailsConvertedPrice.setText(String.format("≈ %.2f EUR · %.2f USD", p.getPromotionPrice() * 0.29, p.getPromotionPrice() * 0.32));
        } else {
            detailsPrice.setText(String.format("%.2f TND", p.getPrix()));
            detailsOldPrice.setVisible(false);
            detailsOldPrice.setManaged(false);
            detailsConvertedPrice.setText(String.format("≈ %.2f EUR · %.2f USD", p.getPrix() * 0.29, p.getPrix() * 0.32));
        }
    }

    @FXML
    private void closeWindow() {
        Stage stage = (Stage) detailsTitle.getScene().getWindow();
        stage.close();
    }
}
