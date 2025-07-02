package com.documentprocessing.service;

import com.documentprocessing.model.DrivingLicense;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.InputStreamEntity;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import com.github.pemistahl.lingua.api.Language;
import com.github.pemistahl.lingua.api.LanguageDetector;
import com.github.pemistahl.lingua.api.LanguageDetectorBuilder;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DocumentProcessingService {
    private static final Logger log = LoggerFactory.getLogger(DocumentProcessingService.class);

    @Autowired
    private DrivingLicenseService drivingLicenseService;

    @Autowired
    private RealAIDocumentProcessingService realAIDocumentProcessingService;

    @Autowired
    private OllamaAIDocumentProcessingService ollamaAIDocumentProcessingService;

    private final Tesseract tesseract;
    private static final double MIN_CONFIDENCE = 0.5;
    private final LanguageDetector languageDetector;

    public DocumentProcessingService() {
        this.tesseract = new Tesseract();
        
        // Setup Tesseract paths for macOS
        System.setProperty("jna.library.path", "/opt/homebrew/opt/tesseract/lib");
        System.setProperty("TESSDATA_PREFIX", "/opt/homebrew/Cellar/tesseract/5.5.1/share/tessdata");
        
        tesseract.setLanguage("eng");
        tesseract.setDatapath("/opt/homebrew/Cellar/tesseract/5.5.1/share/tessdata");
        
        log.info("Tesseract OCR engine ready");
        this.languageDetector = LanguageDetectorBuilder.fromAllLanguages().build();
    }

    public DrivingLicense processDocument(MultipartFile uploadedFile) throws Exception {
        String fileType = figureOutFileType(uploadedFile);
        log.info("Processing {} file: {}", fileType, uploadedFile.getOriginalFilename());
        
        String rawText = null;
        boolean hasHandwriting = false;
        
        try {
            byte[] fileBytes = uploadedFile.getBytes();
            
            if ("IMAGE".equals(fileType)) {
                // Figure out if it's handwritten or printed
                String handwritingResult = checkForHandwriting(fileBytes);
                log.info("Handwriting check result: {}", handwritingResult);
                
                hasHandwriting = "handwritten".equals(handwritingResult);
                
                if (hasHandwriting) {
                    log.info("Looks like handwriting - using TrOCR");
                    rawText = extractHandwrittenText(fileBytes);
                } else {
                    log.info("Looks like printed text - using Tesseract");
                    rawText = extractPrintedText(uploadedFile);
                }
            } else if ("PDF".equals(fileType)) {
                log.info("PDF detected - extracting text");
                rawText = extractPdfText(uploadedFile);
            } else {
                log.warn("Can't handle this file type: {}", fileType);
                throw new UnsupportedOperationException("Unsupported file type: " + fileType);
            }
        } catch (Exception e) {
            log.error("Failed to extract text: {}", e.getMessage());
            return createFailedRecord(fileType, "Text extraction failed: " + e.getMessage());
        }
        
        if (rawText == null || rawText.trim().isEmpty()) {
            log.warn("No text found in document");
            return createFailedRecord(fileType, "No text extracted from document");
        }

        // Language detection step
        Language detectedLanguage = detectLanguage(rawText);
        String langName = detectedLanguage != null ? detectedLanguage.getIsoCode639_1().name() : "unknown";
        log.info("Detected language: {} (ISO 639-1: {})", detectedLanguage, langName);

        log.info("Extracted {} characters of text", rawText.length());

        // Try to extract license data using AI
        Map<String, Object> extractedData = extractLicenseData(rawText);

        // Build the license record
        DrivingLicense license = buildLicenseRecord(extractedData, fileType, hasHandwriting);
        
        log.info("Processing complete - Status: {}, Confidence: {}", 
                license.getProcessingStatus(), license.getConfidenceScore());

        return drivingLicenseService.saveDrivingLicense(license);
    }

    private Map<String, Object> extractLicenseData(String text) {
        // Try Ollama first (local AI)
        Map<String, Object> ollamaResult = new HashMap<>();
        try {
            ollamaResult = ollamaAIDocumentProcessingService.extractDataWithOllama(text);
            log.info("Ollama extracted data with confidence: {}", ollamaResult.get("aiConfidence"));
        } catch (Exception e) {
            log.warn("Ollama failed, trying OpenAI: {}", e.getMessage());
        }

        // Fallback to OpenAI if needed
        Map<String, Object> openaiResult = new HashMap<>();
        if (ollamaResult.isEmpty() || isLowConfidence(ollamaResult)) {
            try {
                openaiResult = realAIDocumentProcessingService.extractDataWithRealAI(text);
                log.info("OpenAI extracted data with confidence: {}", openaiResult.get("aiConfidence"));
            } catch (Exception e) {
                log.warn("OpenAI also failed: {}", e.getMessage());
            }
        }

        return pickBestResult(ollamaResult, openaiResult);
    }

    private boolean isLowConfidence(Map<String, Object> result) {
        Object confidence = result.get("aiConfidence");
        return confidence != null && (Double) confidence < MIN_CONFIDENCE;
    }

    private Map<String, Object> pickBestResult(Map<String, Object> ollama, Map<String, Object> openai) {
        // Prefer the one with higher confidence
        if (!ollama.isEmpty() && !openai.isEmpty()) {
            double ollamaConf = ollama.get("aiConfidence") != null ? (Double) ollama.get("aiConfidence") : 0.0;
            double openaiConf = openai.get("aiConfidence") != null ? (Double) openai.get("aiConfidence") : 0.0;
            return ollamaConf >= openaiConf ? ollama : openai;
        }
        
        // Return whichever one has data
        return !ollama.isEmpty() ? ollama : openai;
    }

    private DrivingLicense buildLicenseRecord(Map<String, Object> data, String fileType, boolean hasHandwriting) {
        DrivingLicense license = createLicenseFromData(data, fileType);
        license.setHandwritten(hasHandwriting);
        
        // Set AI metadata
        license.setAiProcessed(data.containsKey("aiProcessed") ? (Boolean) data.get("aiProcessed") : false);
        license.setAiConfidence(data.containsKey("aiConfidence") ? (Double) data.get("aiConfidence") : 0.0);

        // Calculate overall confidence
        double confidence = calculateConfidence(data);
        license.setConfidenceScore(confidence);

        // Decide processing status
        if (confidence < MIN_CONFIDENCE || missingImportantFields(data)) {
            license.setProcessingStatus(DrivingLicense.ProcessingStatus.MANUAL_REVIEW_REQUIRED);
            log.info("Needs manual review - confidence: {}, missing fields: {}", 
                    confidence, missingImportantFields(data));
        } else {
            license.setProcessingStatus(DrivingLicense.ProcessingStatus.PROCESSED);
        }

        license.setCreatedAt(LocalDate.now());
        return license;
    }

    private boolean missingImportantFields(Map<String, Object> data) {
        String[] importantFields = {"licenseNumber", "firstName", "lastName"};
        
        for (String field : importantFields) {
            if (!data.containsKey(field) || data.get(field) == null || 
                data.get(field).toString().trim().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private DrivingLicense createFailedRecord(String fileType, String reason) {
        DrivingLicense failedLicense = DrivingLicense.builder()
                .documentType(fileType)
                .processingStatus(DrivingLicense.ProcessingStatus.FAILED)
                .confidenceScore(0.0)
                .aiProcessed(false)
                .aiConfidence(0.0)
                .createdAt(LocalDate.now())
                .build();
        
        log.warn("Created failed record: {}", reason);
        return drivingLicenseService.saveDrivingLicense(failedLicense);
    }

    private String figureOutFileType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType != null) {
            if (contentType.contains("pdf")) {
                return "PDF";
            } else if (contentType.contains("image")) {
                return "IMAGE";
            }
        }
        return "UNKNOWN";
    }

    private String extractPdfText(MultipartFile file) throws IOException {
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private String extractPrintedText(MultipartFile file) throws IOException, TesseractException {
        BufferedImage image = ImageIO.read(file.getInputStream());
        return tesseract.doOCR(image);
    }

    private String checkForHandwriting(byte[] imageBytes) {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost("http://localhost:8002/detect");
            
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.addBinaryBody("file", imageBytes, ContentType.IMAGE_JPEG, "image.jpg");
            HttpEntity entity = builder.build();
            post.setEntity(entity);

            String response = client.execute(post, response1 -> {
                return EntityUtils.toString(response1.getEntity());
            });

            JSONObject json = new JSONObject(response);
            return json.getString("result");
        } catch (Exception e) {
            log.error("Handwriting detection failed: {}", e.getMessage());
            return "printed"; // Default to printed if detection fails
        }
    }

    private String extractHandwrittenText(byte[] imageBytes) {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost("http://localhost:8001/ocr");
            
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.addBinaryBody("file", imageBytes, ContentType.IMAGE_JPEG, "image.jpg");
            HttpEntity entity = builder.build();
            post.setEntity(entity);

            String response = client.execute(post, response1 -> {
                return EntityUtils.toString(response1.getEntity());
            });

            JSONObject json = new JSONObject(response);
            return json.getString("text");
        } catch (Exception e) {
            log.error("TrOCR failed: {}", e.getMessage());
            throw new RuntimeException("TrOCR microservice failed: " + e.getMessage());
        }
    }

    private DrivingLicense createLicenseFromData(Map<String, Object> data, String documentType) {
        DrivingLicense.DrivingLicenseBuilder builder = DrivingLicense.builder()
                .licenseNumber((String) data.getOrDefault("licenseNumber", ""))
                .firstName((String) data.getOrDefault("firstName", ""))
                .lastName((String) data.getOrDefault("lastName", ""))
                .middleName((String) data.getOrDefault("middleName", ""))
                .address((String) data.getOrDefault("address", ""))
                .city((String) data.getOrDefault("city", ""))
                .state((String) data.getOrDefault("state", ""))
                .zipCode((String) data.getOrDefault("zipCode", ""))
                .issuingAuthority((String) data.getOrDefault("issuingAuthority", ""))
                .licenseClass((String) data.getOrDefault("licenseClass", ""))
                .documentType(documentType);

        // Robustly handle date fields (accept String or LocalDate)
        builder.dateOfBirth(parseDateFlexible(data.get("dateOfBirth")));
        builder.issueDate(parseDateFlexible(data.get("issueDate")));
        builder.expiryDate(parseDateFlexible(data.get("expiryDate")));

        // Add handwritten field if present in data
        if (data.containsKey("handwritten")) {
            builder.handwritten((Boolean) data.get("handwritten"));
        }

        return builder.build();
    }

    private LocalDate parseDateFlexible(Object value) {
        if (value == null) return null;
        if (value instanceof LocalDate) return (LocalDate) value;
        if (value instanceof String) {
            String dateStr = ((String) value).trim();
            if (dateStr.isEmpty()) return null;
            String[] patterns = {"yyyy-MM-dd", "MM/dd/yyyy", "MM-dd-yyyy", "MM/dd/yy", "MM-dd-yy", "dd/MM/yyyy", "dd-MM-yyyy"};
            for (String pattern : patterns) {
                try {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
                    return LocalDate.parse(dateStr, formatter);
                } catch (DateTimeParseException e) {
                    // try next pattern
                }
            }
        }
        return null;
    }

    private double calculateConfidence(Map<String, Object> data) {
        int totalFields = 10; // Key fields we expect
        int foundFields = 0;
        
        String[] keyFields = {"licenseNumber", "firstName", "lastName", "dateOfBirth", 
                             "address", "city", "state", "zipCode", "issueDate", "expiryDate"};
        
        for (String field : keyFields) {
            if (data.containsKey(field) && data.get(field) != null && 
                !data.get(field).toString().trim().isEmpty()) {
                foundFields++;
            }
        }
        
        return (double) foundFields / totalFields;
    }

    public String testHandwritingDetection(byte[] imageBytes) {
        return checkForHandwriting(imageBytes);
    }

    private Language detectLanguage(String text) {
        if (text == null || text.trim().isEmpty()) return null;
        return languageDetector.detectLanguageOf(text);
    }
} 