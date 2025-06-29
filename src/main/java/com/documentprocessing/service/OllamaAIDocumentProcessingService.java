package com.documentprocessing.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.EntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
public class OllamaAIDocumentProcessingService {
    private static final Logger log = LoggerFactory.getLogger(OllamaAIDocumentProcessingService.class);

    @Value("${ollama.api.url:http://localhost:11434}")
    private String ollamaApiUrl;

    @Value("${ollama.model:llama2:7b}")
    private String ollamaModel;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Extract driving license data using Ollama with Llama 2
     */
    public Map<String, Object> extractDataWithOllama(String extractedText) {
        Map<String, Object> result = new HashMap<>();
        double confidence = 0.0;

        try {
            log.info("Using Ollama with model: {} for AI extraction", ollamaModel);

            // Create the prompt for Llama 2
            String prompt = createExtractionPrompt(extractedText);

            // Call Ollama API
            String aiResponse = callOllamaAPI(prompt);

            // Parse the AI response
            Map<String, Object> extractedData = parseAIResponse(aiResponse);

            // Calculate confidence based on extracted fields
            confidence = calculateConfidence(extractedData);

            result.putAll(extractedData);
            result.put("aiConfidence", confidence);
            result.put("aiProcessed", true);
            result.put("aiModel", "Ollama-Llama2");
            result.put("extractionMethod", "AI");
            result.put("modelUsed", ollamaModel);

            log.info("Ollama AI extraction completed with confidence: {}", confidence);

        } catch (Exception e) {
            log.error("Ollama AI extraction failed: {}", e.getMessage(), e);
            result.put("error", "AI extraction failed: " + e.getMessage());
            result.put("aiConfidence", 0.0);
        }

        return result;
    }

    /**
     * Create a structured prompt for Llama 2 to extract driving license data
     */
    private String createExtractionPrompt(String extractedText) {
        return String.format("""
            You are an AI assistant specialized in extracting structured data from driving license documents.
            
            Please analyze the following OCR text from a driving license and extract the information in JSON format.
            
            OCR Text:
            %s
            
            Extract the following fields and return ONLY a valid JSON object:
            - licenseNumber: The driving license number
            - firstName: First name of the license holder
            - lastName: Last name of the license holder
            - middleName: Middle name (if any)
            - dateOfBirth: Date of birth in YYYY-MM-DD format
            - address: Complete address
            - city: City name
            - state: State name
            - zipCode: ZIP/Postal code
            - issueDate: Issue date in YYYY-MM-DD format
            - expiryDate: Expiry date in YYYY-MM-DD format
            - issuingAuthority: Authority that issued the license
            - licenseClass: License class/category
            - restrictions: Any restrictions (if none, use "None")
            - endorsements: Any endorsements (if none, use "None")
            
            If a field is not found in the text, use null for that field.
            Return ONLY the JSON object, no additional text or explanation.
            """, extractedText);
    }

    /**
     * Call the Ollama API with the given prompt
     */
    private String callOllamaAPI(String prompt) throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(ollamaApiUrl + "/api/generate");

            // Create the request payload
            Map<String, Object> requestPayload = new HashMap<>();
            requestPayload.put("model", ollamaModel);
            requestPayload.put("prompt", prompt);
            requestPayload.put("stream", false);
            requestPayload.put("options", Map.of(
                "temperature", 0.1,
                "top_p", 0.9,
                "max_tokens", 1000
            ));

            String jsonPayload = objectMapper.writeValueAsString(requestPayload);
            httpPost.setEntity(EntityBuilder.create()
                .setText(jsonPayload)
                .setContentType(ContentType.APPLICATION_JSON)
                .build());

            log.debug("Sending request to Ollama API: {}", jsonPayload);

            return httpClient.execute(httpPost, response -> {
                String responseBody = EntityUtils.toString(response.getEntity());
                log.debug("Ollama API response: {}", responseBody);

                if (response.getCode() != 200) {
                    throw new RuntimeException("Ollama API error: " + response.getCode() + " - " + responseBody);
                }

                JsonNode responseJson = objectMapper.readTree(responseBody);
                return responseJson.get("response").asText();
            });

        } catch (Exception e) {
            log.error("Error calling Ollama API: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to call Ollama API: " + e.getMessage(), e);
        }
    }

    /**
     * Parse the AI response and extract structured data
     */
    private Map<String, Object> parseAIResponse(String aiResponse) {
        Map<String, Object> extractedData = new HashMap<>();

        try {
            // Try to extract JSON from the response
            int jsonStart = aiResponse.indexOf("{");
            int jsonEnd = aiResponse.lastIndexOf("}") + 1;
            
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                String jsonString = aiResponse.substring(jsonStart, jsonEnd);
                JsonNode jsonNode = objectMapper.readTree(jsonString);
                
                // Extract all fields
                extractedData.put("licenseNumber", getStringValue(jsonNode, "licenseNumber"));
                extractedData.put("firstName", getStringValue(jsonNode, "firstName"));
                extractedData.put("lastName", getStringValue(jsonNode, "lastName"));
                extractedData.put("middleName", getStringValue(jsonNode, "middleName"));
                extractedData.put("dateOfBirth", parseDate(getStringValue(jsonNode, "dateOfBirth")));
                extractedData.put("address", getStringValue(jsonNode, "address"));
                extractedData.put("city", getStringValue(jsonNode, "city"));
                extractedData.put("state", getStringValue(jsonNode, "state"));
                extractedData.put("zipCode", getStringValue(jsonNode, "zipCode"));
                extractedData.put("issueDate", parseDate(getStringValue(jsonNode, "issueDate")));
                extractedData.put("expiryDate", parseDate(getStringValue(jsonNode, "expiryDate")));
                extractedData.put("issuingAuthority", getStringValue(jsonNode, "issuingAuthority"));
                extractedData.put("licenseClass", getStringValue(jsonNode, "licenseClass"));
                extractedData.put("restrictions", getStringValue(jsonNode, "restrictions"));
                extractedData.put("endorsements", getStringValue(jsonNode, "endorsements"));
            } else {
                // Fallback: try to extract using regex patterns
                extractedData = extractUsingRegex(aiResponse);
            }

        } catch (Exception e) {
            log.warn("Failed to parse AI response as JSON, using regex fallback: {}", e.getMessage());
            extractedData = extractUsingRegex(aiResponse);
        }

        return extractedData;
    }

    /**
     * Get string value from JSON node, handling null values
     */
    private String getStringValue(JsonNode node, String fieldName) {
        JsonNode fieldNode = node.get(fieldName);
        if (fieldNode != null && !fieldNode.isNull()) {
            String value = fieldNode.asText();
            return "null".equalsIgnoreCase(value) ? null : value;
        }
        return null;
    }

    /**
     * Parse date string to LocalDate
     */
    private LocalDate parseDate(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) {
            return null;
        }

        try {
            // Try different date formats
            String[] formats = {"yyyy-MM-dd", "dd-MM-yyyy", "MM/dd/yyyy", "dd/MM/yyyy"};
            
            for (String format : formats) {
                try {
                    return LocalDate.parse(dateString.trim(), DateTimeFormatter.ofPattern(format));
                } catch (DateTimeParseException e) {
                    // Continue to next format
                }
            }
            
            log.warn("Could not parse date: {}", dateString);
            return null;
        } catch (Exception e) {
            log.warn("Error parsing date: {}", dateString, e);
            return null;
        }
    }

    /**
     * Fallback extraction using regex patterns
     */
    private Map<String, Object> extractUsingRegex(String text) {
        Map<String, Object> extractedData = new HashMap<>();
        
        // Simple regex patterns for fallback
        extractedData.put("licenseNumber", extractPattern(text, "license.*?number.*?[:\\s]*(\\w+)", "license.*?[:\\s]*(\\w+)"));
        extractedData.put("firstName", extractPattern(text, "name.*?[:\\s]*([A-Z]+)", "first.*?name.*?[:\\s]*([A-Z]+)"));
        extractedData.put("lastName", extractPattern(text, "last.*?name.*?[:\\s]*([A-Z]+)", "surname.*?[:\\s]*([A-Z]+)"));
        extractedData.put("dateOfBirth", extractPattern(text, "birth.*?[:\\s]*(\\d{1,2}[-/]\\d{1,2}[-/]\\d{2,4})", "dob.*?[:\\s]*(\\d{1,2}[-/]\\d{1,2}[-/]\\d{2,4})"));
        extractedData.put("address", extractPattern(text, "address.*?[:\\s]*(.+?)(?=\\n|$)", "addr.*?[:\\s]*(.+?)(?=\\n|$)"));
        
        return extractedData;
    }

    /**
     * Extract value using multiple regex patterns
     */
    private String extractPattern(String text, String... patterns) {
        for (String pattern : patterns) {
            try {
                java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern, java.util.regex.Pattern.CASE_INSENSITIVE);
                java.util.regex.Matcher m = p.matcher(text);
                if (m.find()) {
                    String result = m.group(1);
                    return result != null ? result.trim() : null;
                }
            } catch (Exception e) {
                log.debug("Regex pattern failed: {}", pattern);
            }
        }
        return null;
    }

    /**
     * Calculate confidence score based on extracted fields
     */
    private double calculateConfidence(Map<String, Object> extractedData) {
        int totalFields = 15; // Total number of fields we're trying to extract
        int extractedFields = 0;
        
        for (Object value : extractedData.values()) {
            if (value != null && !value.toString().trim().isEmpty()) {
                extractedFields++;
            }
        }
        
        double baseConfidence = (double) extractedFields / totalFields;
        
        // Boost confidence if key fields are present
        if (extractedData.get("licenseNumber") != null) baseConfidence += 0.1;
        if (extractedData.get("firstName") != null) baseConfidence += 0.1;
        if (extractedData.get("lastName") != null) baseConfidence += 0.1;
        
        return Math.min(baseConfidence, 1.0);
    }
} 