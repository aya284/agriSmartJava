package services;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import utils.ImagePreprocessor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OcrService {

    private final Tesseract tesseract;
    private double lastQualityScore = 0.0;

    public OcrService() {
        tesseract = new Tesseract();

        String foundPath = resolveDataPath();

        if (foundPath == null) {
            throw new RuntimeException(
                "ara.traineddata not found. Ensure Tesseract Arabic data is installed."
            );
        }

        System.out.println(" Tesseract tessdata path: " + foundPath);
        tesseract.setDatapath(foundPath);
        
        // --- Enhanced Configuration for Arabic ---
        tesseract.setLanguage("ara+eng"); // Arabic primary, English secondary
        tesseract.setOcrEngineMode(1);    // LSTM only
        tesseract.setPageSegMode(3);      // Fully automatic page segmentation
        
        // Critical variables for noisy scans
        tesseract.setVariable("preserve_interword_spaces", "1");
        tesseract.setVariable("user_defined_dpi", "300");
        tesseract.setVariable("tessedit_char_whitelist", ""); // Reset whitelist
    }

    /**
     * Extracts text with region-based preprocessing and quality validation.
     * Strategy: Scan top 35% (CIN zone) with PSM 11, then full image with PSM 3.
     */
    public String extractText(String filePath) throws Exception {
        if (filePath == null || filePath.isBlank()) {
            throw new Exception("Chemin du fichier invalide.");
        }

        System.out.println("\n REGION-BASED OCR PIPELINE STARTED: " + new File(filePath).getName());

        try {
            // 1. Image Preprocessing
            System.out.println("    Step 1: Loading & Normalizing image...");
            java.awt.image.BufferedImage fullImage = ImagePreprocessor.preprocess(filePath);

            StringBuilder mergedText = new StringBuilder();

            // 2. Focused Scan: Center Region (Where CIN is most likely located)
            System.out.println("    Step 2: Focused scan of CENTER region (CIN Zone)...");
            java.awt.image.BufferedImage centerCrop = ImagePreprocessor.cropCenter(fullImage, 0.7, 0.5);
            
            tesseract.setPageSegMode(11); // Sparse text — best for isolated numbers
            String centerText = tesseract.doOCR(centerCrop);
            if (centerText != null) {
                mergedText.append(centerText).append("\n");
                System.out.println("      ✓ Center Scan complete (" + centerText.length() + " chars)");
            }

            // 3. Fallback/Context Scan: Full Image
            System.out.println("    Step 3: Full image fallback scan...");
            tesseract.setPageSegMode(3); // Fully automatic
            String fullText = tesseract.doOCR(fullImage);
            if (fullText != null) {
                mergedText.append(fullText);
                System.out.println("      ✓ Full Scan complete (" + fullText.length() + " chars)");
            }

            String rawResult = mergedText.toString();
            if (rawResult.isBlank()) {
                throw new Exception("OCR returned no text from any region.");
            }

            // 4. Encoding & Cleanup
            String result = new String(rawResult.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
            result = cleanOcrText(result);

            // 5. Quality Validation (based on combined result)
            validateQuality(result);

            // 6. Debug Logging
            saveDebugLog(result);

            System.out.println("    Step 4: Success. Total chars: " + result.length() + " | Quality: " + (int)(lastQualityScore * 100) + "%");
            return result;

        } catch (TesseractException e) {
            System.err.println("Tesseract Error: " + e.getMessage());
            throw new Exception("Erreur lors de l'OCR : " + e.getMessage());
        } catch (Exception e) {
            System.err.println(" OCR Pipeline Error: " + e.getMessage());
            throw e;
        }
    }

    public double getLastQualityScore() {
        return lastQualityScore;
    }

    private String cleanOcrText(String text) {
        // Remove excessive empty lines and strange artifacts
        return text.replaceAll("(?m)^\\s*$[\n\r]{1,}", "")
                   .replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "")
                   .trim();
    }

    private void validateQuality(String text) throws Exception {
        if (text.length() < 10) {
            this.lastQualityScore = 0.1;
            throw new Exception("Texte extrait trop court pour être valide.");
        }

        // Count '?' characters which indicate OCR failure for a char
        long questionMarks = text.chars().filter(ch -> ch == '?').count();
        double ratio = (double) questionMarks / text.length();
        
        this.lastQualityScore = 1.0 - ratio;

        if (ratio > 0.35) {
            System.err.println(" OCR Quality too low (" + (int)(lastQualityScore * 100) + "%). Detected many '?' characters.");
            throw new Exception("Qualité OCR insuffisante (" + (int)(lastQualityScore * 100) + "%). Image floue ou illisible.");
        }
    }

    private void saveDebugLog(String text) {
        try (OutputStreamWriter writer = new OutputStreamWriter(
                new FileOutputStream("ocr_debug.txt"), StandardCharsets.UTF_8)) {
            writer.write("--- OCR DEBUG LOG ---\n");
            writer.write("Timestamp: " + java.time.LocalDateTime.now() + "\n");
            writer.write("Quality Score: " + lastQualityScore + "\n");
            writer.write("Content:\n");
            writer.write(text);
            writer.flush();
        } catch (Exception e) {
            System.err.println("Failed to save debug log: " + e.getMessage());
        }
    }

    private String resolveDataPath() {
        String[] systemPaths = {
            "C:/Program Files/Tesseract-OCR/tessdata",
            "C:/Program Files (x86)/Tesseract-OCR/tessdata",
            System.getenv("TESSDATA_PREFIX") != null ? System.getenv("TESSDATA_PREFIX") + "/tessdata" : null,
            System.getenv("TESSDATA_PREFIX")
        };

        for (String path : systemPaths) {
            if (path == null) continue;
            File araFile = new File(path + "/ara.traineddata");
            if (araFile.exists()) return path;
        }
        return null;
    }
}