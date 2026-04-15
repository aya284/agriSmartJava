package services;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MarketplaceInvoicePdfService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static class InvoiceData {
        public int commandeId;
        public String clientName;
        public String paymentMode;
        public String deliveryAddress;
        public int itemCount;
        public double totalAmount;
        public LocalDateTime issuedAt;
    }

    public void generateInvoice(Path outputPath, InvoiceData data) throws IOException {
        if (outputPath == null) {
            throw new IOException("Chemin de sortie facture invalide.");
        }
        if (data == null) {
            throw new IOException("Donnees de facture manquantes.");
        }

        Path parent = outputPath.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        Document document = new Document(PageSize.A4, 36, 36, 42, 42);
        try (FileOutputStream out = new FileOutputStream(outputPath.toFile())) {
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20);
            Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 11);

            Paragraph title = new Paragraph("AgriSmart - Facture", titleFont);
            title.setSpacingAfter(8);
            document.add(title);

            Paragraph ref = new Paragraph("Facture commande #" + data.commandeId, subtitleFont);
            ref.setSpacingAfter(2);
            document.add(ref);

            String dateText = data.issuedAt == null
                    ? LocalDateTime.now().format(DATE_FORMAT)
                    : data.issuedAt.format(DATE_FORMAT);
            Paragraph date = new Paragraph("Date emission: " + dateText, normalFont);
            date.setSpacingAfter(14);
            document.add(date);

            PdfPTable summary = new PdfPTable(2);
            summary.setWidthPercentage(100);
            summary.setSpacingAfter(12);
            summary.setWidths(new float[]{34f, 66f});

            addRow(summary, "Client", safe(data.clientName), subtitleFont, normalFont);
            addRow(summary, "Paiement", safe(data.paymentMode), subtitleFont, normalFont);
            addRow(summary, "Adresse", safe(data.deliveryAddress), subtitleFont, normalFont);
            addRow(summary, "Articles", String.valueOf(data.itemCount), subtitleFont, normalFont);
            addRow(summary, "Montant total", String.format("%.2f TND", data.totalAmount), subtitleFont, normalFont);

            document.add(summary);

            Paragraph note = new Paragraph(
                    "Merci pour votre achat sur AgriSmart Marketplace.",
                    normalFont
            );
            document.add(note);
            
            // Il FAUT fermer le document avant que le FileOutputStream ne se ferme
            document.close();
        } catch (DocumentException e) {
            throw new IOException("Generation PDF echouee: " + e.getMessage(), e);
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }
    }

    private void addRow(PdfPTable table, String key, String value, Font keyFont, Font valueFont) {
        PdfPCell keyCell = new PdfPCell(new Phrase(key, keyFont));
        keyCell.setPadding(7);
        table.addCell(keyCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setPadding(7);
        table.addCell(valueCell);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
