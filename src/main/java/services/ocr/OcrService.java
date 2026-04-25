package services.ocr;

import com.google.api.gax.rpc.FixedHeaderProvider;
import com.google.cloud.vision.v1.*;
import com.google.protobuf.ByteString;
import utils.ConfigLoader;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * OcrService is responsible for extracting text from visual documents.
 * It uses Google Cloud Vision as the foundational OCR layer.
 */
public class OcrService {

    private static final String API_KEY = ConfigLoader.get("GOOGLE_VISION_API_KEY");

    /**
     * Extracts text from an image or PDF file using Google Cloud Vision.
     * @param filePath absolute path to the image/PDF
     * @return extracted text as a String
     */
    public String extractText(String filePath) throws Exception {
        if (API_KEY == null || API_KEY.isBlank()) {
            throw new Exception("Google Vision API Key is missing in config.properties");
        }

        System.out.println("OCR: Processing file: " + filePath);

        // Read file as bytes
        byte[] fileBytes = Files.readAllBytes(Paths.get(filePath));
        ByteString imgBytes = ByteString.copyFrom(fileBytes);

        // Detect file type
        Feature.Type featureType = filePath.toLowerCase().endsWith(".pdf")
                ? Feature.Type.DOCUMENT_TEXT_DETECTION  // better for PDFs/dense text
                : Feature.Type.TEXT_DETECTION;           // better for images/IDs

        // Build request
        Image image = Image.newBuilder().setContent(imgBytes).build();
        Feature feature = Feature.newBuilder().setType(featureType).build();
        AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                .addFeatures(feature)
                .setImage(image)
                .build();

        // Call API using API Key in headers (standard for Vision API key auth)
        ImageAnnotatorSettings settings = ImageAnnotatorSettings.newBuilder()
                .setHeaderProvider(FixedHeaderProvider.create("x-goog-api-key", API_KEY))
                .build();

        try (ImageAnnotatorClient client = ImageAnnotatorClient.create(settings)) {
            BatchAnnotateImagesResponse response = client.batchAnnotateImages(List.of(request));
            AnnotateImageResponse imageResponse = response.getResponsesList().get(0);

            if (imageResponse.hasError()) {
                throw new Exception("Vision API error: " + imageResponse.getError().getMessage());
            }

            TextAnnotation annotation = imageResponse.getFullTextAnnotation();
            if (annotation == null || annotation.getText().isBlank()) {
                System.out.println("OCR: No text detected in " + filePath);
                return "";
            }

            String text = annotation.getText();
            System.out.println("OCR: Successfully extracted " + text.length() + " characters.");
            return text;
        }
    }
}