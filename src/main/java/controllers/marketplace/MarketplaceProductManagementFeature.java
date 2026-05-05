package controllers.marketplace;

import entities.Produit;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import services.HuggingFaceAiService;
import services.MarketplaceImageService;
import services.ProduitService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

/**
 * Extracted feature for product management (add, edit, delete, validation)
 */
public class MarketplaceProductManagementFeature {
    private static final String CATEGORY_PLACEHOLDER = "Choisir...";
    private static final String TYPE_VENTE = "Vente";
    private static final String TYPE_LOCATION = "Location";

    private final ProduitService produitService;
    private final MarketplaceImageService imageService;
    private final StackPane modalOverlay;
    private final Label modalTitle;
    private final TextField fldNom;
    private final TextArea fldDescription;
    private final TextField fldPrix;
    private final TextField fldStock;
    private final ComboBox<String> fldCategorie;
    private final TextField fldImage;
    private final ComboBox<String> fldType;
    private final CheckBox chkPromo;
    private final TextField fldPromoPrice;
    private final Label errNom;
    private final Label errCategorie;
    private final Label errType;
    private final Label errPrix;
    private final Label errStock;
    private final Label errDescription;
    private final Label errPromoPrice;
    private final Label publishPreviewTitle;
    private final Label publishPreviewCategory;
    private final Label publishPreviewTypeBadge;
    private final Label publishPreviewPrice;
    private final Label publishPreviewStock;
    private final Label publishPreviewDescription;
    private final ImageView publishPreviewImage;
    private final Consumer<StackPane> animateOverlayIn;
    private final Consumer<StackPane> animateOverlayOut;
    private final BiConsumer<String, Boolean> showToast;
    private final Consumer<SQLException> showSqlAlert;
    private final BiConsumer<String, String> showAlert;
    private final IntSupplier currentUserIdSupplier;
    private final java.util.function.Function<String, String> normalizeText;
    private final java.util.function.Function<String, String> normalizeTypeForDisplay;
    private final java.util.function.Function<String, String> normalizeTypeForStorage;
    private final Runnable onProductSaved;
    private final HuggingFaceAiService huggingFaceService = new HuggingFaceAiService();

    private Produit currentEditProduit = null;

    public MarketplaceProductManagementFeature(
            ProduitService produitService,
            MarketplaceImageService imageService,
            StackPane modalOverlay,
            Label modalTitle,
            TextField fldNom,
            TextArea fldDescription,
            TextField fldPrix,
            TextField fldStock,
            ComboBox<String> fldCategorie,
            TextField fldImage,
            ComboBox<String> fldType,
            CheckBox chkPromo,
            TextField fldPromoPrice,
            Label errNom,
            Label errCategorie,
            Label errType,
            Label errPrix,
            Label errStock,
            Label errDescription,
            Label errPromoPrice,
            Label publishPreviewTitle,
            Label publishPreviewCategory,
            Label publishPreviewTypeBadge,
            Label publishPreviewPrice,
            Label publishPreviewStock,
            Label publishPreviewDescription,
            ImageView publishPreviewImage,
            Consumer<StackPane> animateOverlayIn,
            Consumer<StackPane> animateOverlayOut,
            BiConsumer<String, Boolean> showToast,
            Consumer<SQLException> showSqlAlert,
            BiConsumer<String, String> showAlert,
            IntSupplier currentUserIdSupplier,
            java.util.function.Function<String, String> normalizeText,
            java.util.function.Function<String, String> normalizeTypeForDisplay,
            java.util.function.Function<String, String> normalizeTypeForStorage,
            Runnable onProductSaved) {
        this.produitService = produitService;
        this.imageService = imageService;
        this.modalOverlay = modalOverlay;
        this.modalTitle = modalTitle;
        this.fldNom = fldNom;
        this.fldDescription = fldDescription;
        this.fldPrix = fldPrix;
        this.fldStock = fldStock;
        this.fldCategorie = fldCategorie;
        this.fldImage = fldImage;
        this.fldType = fldType;
        this.chkPromo = chkPromo;
        this.fldPromoPrice = fldPromoPrice;
        this.errNom = errNom;
        this.errCategorie = errCategorie;
        this.errType = errType;
        this.errPrix = errPrix;
        this.errStock = errStock;
        this.errDescription = errDescription;
        this.errPromoPrice = errPromoPrice;
        this.publishPreviewTitle = publishPreviewTitle;
        this.publishPreviewCategory = publishPreviewCategory;
        this.publishPreviewTypeBadge = publishPreviewTypeBadge;
        this.publishPreviewPrice = publishPreviewPrice;
        this.publishPreviewStock = publishPreviewStock;
        this.publishPreviewDescription = publishPreviewDescription;
        this.publishPreviewImage = publishPreviewImage;
        this.animateOverlayIn = animateOverlayIn;
        this.animateOverlayOut = animateOverlayOut;
        this.showToast = showToast;
        this.showSqlAlert = showSqlAlert;
        this.showAlert = showAlert;
        this.currentUserIdSupplier = currentUserIdSupplier;
        this.normalizeText = normalizeText;
        this.normalizeTypeForDisplay = normalizeTypeForDisplay;
        this.normalizeTypeForStorage = normalizeTypeForStorage;
        this.onProductSaved = onProductSaved;
    }

    public void openAjouterProduit() {
        if (modalOverlay == null) return;
        currentEditProduit = null;
        modalTitle.setText("Nouvelle Annonce");
        chkPromo.setSelected(false);
        fldNom.clear();
        fldDescription.clear();
        fldPrix.clear();
        fldStock.clear();
        if (fldCategorie != null) fldCategorie.setValue(CATEGORY_PLACEHOLDER);
        if (fldImage != null) fldImage.clear();
        fldPromoPrice.clear();
        if (fldType != null) fldType.setValue(TYPE_VENTE);
        clearProductFieldErrors();
        updatePublishPreview();
        animateOverlayIn.accept(modalOverlay);
    }

    public void openModifierProduit(Produit produit) {
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
        if (fldImage != null) fldImage.setText(safe(produit.getImage()));
        if (fldType != null) fldType.setValue(normalizeTypeForDisplay.apply(produit.getType()));
        chkPromo.setSelected(produit.isPromotion());
        fldPromoPrice.setText(String.valueOf(produit.getPromotionPrice()));
        clearProductFieldErrors();
        updatePublishPreview();
        animateOverlayIn.accept(modalOverlay);
    }

    public void closeModal() {
        animateOverlayOut.accept(modalOverlay);
        currentEditProduit = null;
        clearProductFieldErrors();
    }

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
        p.setType(normalizeTypeForStorage.apply(selectedType));
        if (fldImage != null) {
            p.setImage(fldImage.getText() == null ? "" : fldImage.getText().trim());
        }
        p.setPromotion(chkPromo.isSelected());
        if(promoPrix != null && chkPromo.isSelected()){
            p.setPromotionPrice(promoPrix);
        } else {
            p.setPromotionPrice(0.0);
        }
        p.setVendeurId(currentUserIdSupplier.getAsInt());

        try {
            if (currentEditProduit == null) {
                produitService.ajouter(p);
                showToast.accept("Annonce ajoutee avec succes.", true);
            } else {
                produitService.modifier(p);
                showToast.accept("Annonce modifiee avec succes.", true);
            }
            closeModal();
            onProductSaved.run();
        } catch (SQLException e) {
            showSqlAlert.accept(e);
        }
    }

    public void handleDeleteProduit(Produit produit) {
        if (produit == null) return;
        try {
            produitService.supprimer(produit.getId());
            showToast.accept("Annonce supprimee.", true);
            onProductSaved.run();
        } catch (SQLException e) {
            showSqlAlert.accept(e);
        }
    }

    public void handleProductFormInput() {
        updatePublishPreview();
        clearProductFieldErrors();
    }

    public void generateDescriptionWithAI() {
        String productName = safe(fldNom == null ? "" : fldNom.getText()).trim();
        String category = fldCategorie == null ? "" : safe(fldCategorie.getValue()).trim();
        String offerType = fldType == null ? "Vente" : safe(fldType.getValue()).trim();

        if (productName.isEmpty()) {
            showToast.accept("Entrez un nom de produit d'abord.", false);
            return;
        }

        if (category.isEmpty() || CATEGORY_PLACEHOLDER.equals(category)) {
            showToast.accept("Selectionnez une categorie d'abord.", false);
            return;
        }

        showToast.accept("Génération de description avec IA...", true);

        new Thread(() -> {
            try {
                String generatedDesc = huggingFaceService.suggestProductDescription(productName, category, offerType);
                javafx.application.Platform.runLater(() -> {
                    fldDescription.setText(generatedDesc);
                    updatePublishPreview();
                    showToast.accept("Description générée avec succès!", true);
                });
            } catch (IOException ex) {
                javafx.application.Platform.runLater(() -> {
                    showToast.accept("Erreur: " + ex.getMessage(), false);
                });
            } catch (InterruptedException ex) {
                javafx.application.Platform.runLater(() -> {
                    showToast.accept("Génération annulée.", false);
                });
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    public void chooseImageFile() {
        if (fldImage == null || fldImage.getScene() == null) {
            return;
        }

        javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
        chooser.setTitle("Choisir une image produit");
        chooser.getExtensionFilters().addAll(
            new javafx.stage.FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")
        );

        javafx.stage.Window owner = fldImage.getScene().getWindow();
        File selected = chooser.showOpenDialog(owner);
        if (selected != null) {
            String lowerName = selected.getName().toLowerCase();
            if (lowerName.endsWith(".webp")) {
                showAlert.accept("Format non supporte", "Le format WEBP n'est pas supporte par JavaFX. Choisissez une image PNG, JPG, JPEG, GIF ou BMP.");
                return;
            }

            try {
                String relativePath = storeImageForSharedProjects(selected);
                fldImage.setText(relativePath);
                showToast.accept("Image importee avec succes.", true);
                updatePublishPreview();
            } catch (IOException e) {
                showAlert.accept("Image", "Echec import image: " + e.getMessage());
            }
        }
    }

    public void updatePublishPreview() {
        if (publishPreviewTypeBadge != null) {
            String displayType = fldType == null ? TYPE_VENTE : safe(fldType.getValue()).trim();
            if (displayType.isEmpty()) displayType = TYPE_VENTE;
            publishPreviewTypeBadge.setText(displayType.toUpperCase());
        }

        if (publishPreviewTitle != null) {
            String name = safe(fldNom == null ? "" : fldNom.getText()).trim();
            publishPreviewTitle.setText(name.isEmpty() ? "Nom de l'annonce" : normalizeText.apply(name));
        }

        if (publishPreviewCategory != null) {
            String category = fldCategorie == null ? "" : safe(fldCategorie.getValue()).trim();
            publishPreviewCategory.setText(category.isEmpty() || CATEGORY_PLACEHOLDER.equals(category) ? "CHOISIR..." : normalizeText.apply(category));
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

        if (publishPreviewDescription != null) {
            String desc = safe(fldDescription == null ? "" : fldDescription.getText()).trim();
            publishPreviewDescription.setText(desc.isEmpty() ? "Description..." : normalizeText.apply(desc));
        }

        if (publishPreviewImage != null) {
            publishPreviewImage.setImage(imageService.resolveProductImage(fldImage == null ? "" : fldImage.getText(), getClass()));
        }
    }

    private boolean validateProductForm(boolean strictMode) {
        clearProductFieldErrors();
        boolean valid = true;

        String nomErr = utils.MarketplaceValidator.validateProductName(fldNom == null ? "" : fldNom.getText());
        if (nomErr != null) {
            showFieldError(fldNom, errNom, nomErr);
            valid = false;
        }

        String categorieErr = utils.MarketplaceValidator.validateProductCategory(
                fldCategorie == null ? null : fldCategorie.getValue(),
                CATEGORY_PLACEHOLDER
        );
        if (categorieErr != null) {
            showFieldError(fldCategorie, errCategorie, categorieErr);
            valid = false;
        }

        String typeErr = utils.MarketplaceValidator.validateOfferType(fldType == null ? null : fldType.getValue());
        if (typeErr != null) {
            showFieldError(fldType, errType, typeErr);
            valid = false;
        }

        String prixErr = utils.MarketplaceValidator.validatePositivePrice(fldPrix == null ? "" : fldPrix.getText());
        if (prixErr != null) {
            showFieldError(fldPrix, errPrix, prixErr);
            valid = false;
        }

        String stockErr = utils.MarketplaceValidator.validateStock(fldStock == null ? "" : fldStock.getText());
        if (stockErr != null) {
            showFieldError(fldStock, errStock, stockErr);
            valid = false;
        }

        String descErr = utils.MarketplaceValidator.validateProductDescription(fldDescription == null ? "" : fldDescription.getText());
        if (descErr != null) {
            showFieldError(fldDescription, errDescription, descErr);
            valid = false;
        }

        String promoErr = utils.MarketplaceValidator.validatePromoPrice(
                chkPromo != null && chkPromo.isSelected(),
                fldPromoPrice == null ? "" : fldPromoPrice.getText(),
                fldPrix == null ? "" : fldPrix.getText()
        );
        if (promoErr != null) {
            showFieldError(fldPromoPrice, errPromoPrice, promoErr);
            valid = false;
        }

        if (!valid && strictMode) {
            showAlert.accept("Validation", "Veuillez corriger les champs en rouge.");
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
