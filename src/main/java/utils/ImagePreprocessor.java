package utils;

import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.*;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Utility for image preprocessing before OCR to improve accuracy.
 */
public class ImagePreprocessor {

    /**
     * Preprocesses an image for OCR: grayscale, contrast, sharpening, and scaling.
     * 
     * @param inputPath Path to the raw image
     * @return Processed BufferedImage
     */
    public static BufferedImage preprocess(String inputPath) throws Exception {
        File inputFile = new File(inputPath);
        BufferedImage image;
        
        try {
            image = ImageIO.read(inputFile);
        } catch (Exception e) {
            throw new Exception("Format d'image non supporté ou fichier corrompu.");
        }

        if (image == null) {
            throw new Exception("L'image n'a pas pu être chargée. Format non supporté (utilisez JPG, PNG, BMP).");
        }

        System.out.println("   🖼️ Detected image: " + image.getWidth() + "x" + image.getHeight() + " px");

        // 0. Normalize to RGB (Removes transparency/alpha channels that confuse Tesseract)
        image = normalizeToRGB(image);

        // 1. Scale image if too small (OCR likes ~300 DPI)
        image = scaleImage(image, 2.0f);

        // 2. Convert to Grayscale
        image = convertToGrayscale(image);

        // 3. Increase Contrast & Brightness
        image = adjustContrastAndBrightness(image, 1.3f, 15.0f);

        // 4. Sharpen
        image = sharpen(image);

        return image;
    }

    private static BufferedImage normalizeToRGB(BufferedImage src) {
        BufferedImage newImage = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = newImage.createGraphics();
        g.setColor(Color.WHITE); // Background for transparent PNGs
        g.fillRect(0, 0, src.getWidth(), src.getHeight());
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return newImage;
    }

    public static BufferedImage cropTop(BufferedImage image, double percentage) {
        int height = (int) (image.getHeight() * percentage);
        return image.getSubimage(0, 0, image.getWidth(), height);
    }

    /**
     * Extracts the central region of the image.
     */
    public static BufferedImage cropCenter(BufferedImage image, double widthPercent, double heightPercent) {
        int w = (int) (image.getWidth() * widthPercent);
        int h = (int) (image.getHeight() * heightPercent);
        int x = (image.getWidth() - w) / 2;
        int y = (image.getHeight() - h) / 2;
        return image.getSubimage(x, y, w, h);
    }

    private static BufferedImage scaleImage(BufferedImage image, float factor) {
        int w = (int) (image.getWidth() * factor);
        int h = (int) (image.getHeight() * factor);
        BufferedImage scaled = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(image, 0, 0, w, h, null);
        g.dispose();
        return scaled;
    }

    private static BufferedImage convertToGrayscale(BufferedImage image) {
        ColorConvertOp op = new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_GRAY), null);
        return op.filter(image, null);
    }

    private static BufferedImage adjustContrastAndBrightness(BufferedImage image, float contrast, float brightness) {
        RescaleOp rescaleOp = new RescaleOp(contrast, brightness, null);
        rescaleOp.filter(image, image);
        return image;
    }

    private static BufferedImage sharpen(BufferedImage image) {
        float[] kernel = {
            0f, -1f, 0f,
            -1f, 5f, -1f,
            0f, -1f, 0f
        };
        ConvolveOp op = new ConvolveOp(new Kernel(3, 3, kernel), ConvolveOp.EDGE_NO_OP, null);
        return op.filter(image, null);
    }
}
