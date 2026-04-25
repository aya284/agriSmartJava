package services.ocr;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * AiVerificationService acts as the automated document verification pipeline.
 * It combines OCR (Google Vision) and AI Analysis (Claude) to validate 
 * user-uploaded documents without manual intervention.
 */
public class AiVerificationService {

    private final OcrService ocrService = new OcrService();
    private final ClaudeService claudeService = new ClaudeService();

    /**
     * Fully automated pipeline: OCR -> AI Analysis -> Verification Result
     * @param filePath Path to the document image/PDF
     * @param documentType Type of document (e.g., "ID_CARD", "AGRICULTURAL_LICENSE")
     * @return JsonNode containing verification status and extracted data
     */
    public JsonNode verifyDocument(String filePath, String documentType) throws Exception {
        
        System.out.println("Pipeline started for " + documentType + ": " + filePath);

        // Layer 1: OCR - Extract raw text
        String extractedText = ocrService.extractText(filePath);
        
        if (extractedText == null || extractedText.isBlank()) {
            throw new Exception("OCR failed: No text could be extracted from the document.");
        }

        System.out.println("OCR Layer completed. Character count: " + extractedText.length());

        // Layer 2: AI Analysis - Verify and Structure data
        String systemPrompt = """
            You are an expert document verification agent for AgriSmart, an agricultural management platform.
            Your task is to analyze OCR text and determine if the document is a valid %s.
            
            Strictly return a JSON object with the following fields:
            - "valid": boolean (true if the document is authentic and matches the type)
            - "confidence": float (0.0 to 1.0)
            - "reason": string (explanation if invalid or low confidence)
            - "extracted_data": object (key-value pairs of important info like name, expiry date, license number)
            
            Be extremely strict. If the document is blurry, fake, or unrelated, set valid to false.
            """.formatted(documentType);

        String userPrompt = "Analyze this OCR text extracted from a " + documentType + ":\n\n" + extractedText;

        return claudeService.askForJson(userPrompt, systemPrompt);
    }
}
