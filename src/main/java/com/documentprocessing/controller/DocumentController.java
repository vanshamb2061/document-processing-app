package com.documentprocessing.controller;

import com.documentprocessing.model.DrivingLicense;
import com.documentprocessing.service.DocumentProcessingService;
import com.documentprocessing.service.DrivingLicenseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.HashMap;

@RestController
@RequestMapping("/api/documents")
@CrossOrigin(origins = "*")
public class DocumentController {
    private static final Logger log = LoggerFactory.getLogger(DocumentController.class);

    private final DocumentProcessingService documentProcessingService;
    private final DrivingLicenseService drivingLicenseService;

    public DocumentController(DocumentProcessingService documentProcessingService, 
                            DrivingLicenseService drivingLicenseService) {
        this.documentProcessingService = documentProcessingService;
        this.drivingLicenseService = drivingLicenseService;
    }

    @PostMapping("/process")
    public ResponseEntity<?> uploadAndProcess(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("Please upload a file");
            }

            // Check if it's a supported file type
            String contentType = file.getContentType();
            if (contentType == null || (!contentType.contains("pdf") && !contentType.contains("image"))) {
                return ResponseEntity.badRequest().body("Only PDF and image files are supported");
            }

            log.info("Processing {} ({} bytes)", file.getOriginalFilename(), file.getSize());
            DrivingLicense result = documentProcessingService.processDocument(file);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Failed to process document: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing document: " + e.getMessage());
        }
    }

    @GetMapping("/licenses")
    public ResponseEntity<List<DrivingLicense>> getAllLicenses() {
        List<DrivingLicense> licenses = drivingLicenseService.getAllDrivingLicenses();
        return ResponseEntity.ok(licenses);
    }

    @GetMapping("/licenses/{id}")
    public ResponseEntity<DrivingLicense> getLicenseById(@PathVariable Long id) {
        Optional<DrivingLicense> license = drivingLicenseService.getDrivingLicenseById(id);
        return license.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/licenses/number/{licenseNumber}")
    public ResponseEntity<DrivingLicense> getLicenseByNumber(@PathVariable String licenseNumber) {
        Optional<DrivingLicense> license = drivingLicenseService.getDrivingLicenseByNumber(licenseNumber);
        return license.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/licenses/state/{state}")
    public ResponseEntity<List<DrivingLicense>> getLicensesByState(@PathVariable String state) {
        List<DrivingLicense> licenses = drivingLicenseService.getDrivingLicensesByState(state);
        return ResponseEntity.ok(licenses);
    }

    @GetMapping("/licenses/status/{status}")
    public ResponseEntity<List<DrivingLicense>> getLicensesByStatus(@PathVariable String status) {
        try {
            DrivingLicense.ProcessingStatus processingStatus = DrivingLicense.ProcessingStatus.valueOf(status.toUpperCase());
            List<DrivingLicense> licenses = drivingLicenseService.getDrivingLicensesByStatus(processingStatus);
            return ResponseEntity.ok(licenses);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/licenses/expired")
    public ResponseEntity<List<DrivingLicense>> getExpiredLicenses() {
        List<DrivingLicense> licenses = drivingLicenseService.getExpiredLicenses();
        return ResponseEntity.ok(licenses);
    }

    @GetMapping("/licenses/search")
    public ResponseEntity<List<DrivingLicense>> searchByName(@RequestParam String name) {
        List<DrivingLicense> licenses = drivingLicenseService.searchByName(name);
        return ResponseEntity.ok(licenses);
    }

    @GetMapping("/licenses/low-confidence")
    public ResponseEntity<List<DrivingLicense>> getLowConfidenceLicenses(@RequestParam(defaultValue = "0.7") Double threshold) {
        List<DrivingLicense> licenses = drivingLicenseService.getLowConfidenceLicenses(threshold);
        return ResponseEntity.ok(licenses);
    }

    @GetMapping("/licenses/ai-processed")
    public ResponseEntity<List<DrivingLicense>> getAIProcessedLicenses() {
        List<DrivingLicense> licenses = drivingLicenseService.getAIProcessedLicenses();
        return ResponseEntity.ok(licenses);
    }

    @GetMapping("/licenses/ai-confidence-range")
    public ResponseEntity<List<DrivingLicense>> getLicensesByAIConfidenceRange(
            @RequestParam(defaultValue = "0.0") Double minConfidence,
            @RequestParam(defaultValue = "1.0") Double maxConfidence) {
        List<DrivingLicense> licenses = drivingLicenseService.getLicensesByAIConfidenceRange(minConfidence, maxConfidence);
        return ResponseEntity.ok(licenses);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getProcessingStats() {
        try {
            List<DrivingLicense> allLicenses = drivingLicenseService.getAllDrivingLicenses();
            
            long total = allLicenses.size();
            long aiProcessed = allLicenses.stream()
                    .filter(license -> Boolean.TRUE.equals(license.getAiProcessed()))
                    .count();
            
            double avgConfidence = allLicenses.stream()
                    .filter(license -> license.getAiConfidence() != null)
                    .mapToDouble(DrivingLicense::getAiConfidence)
                    .average()
                    .orElse(0.0);
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalLicenses", total);
            stats.put("aiProcessed", aiProcessed);
            stats.put("aiProcessingRate", total > 0 ? (double) aiProcessed / total : 0.0);
            stats.put("averageAIConfidence", avgConfidence);
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Failed to get stats: {}", e.getMessage(), e);
            Map<String, Object> errorStats = new HashMap<>();
            errorStats.put("error", "Failed to get statistics");
            errorStats.put("totalLicenses", 0);
            errorStats.put("aiProcessed", 0);
            errorStats.put("aiProcessingRate", 0.0);
            errorStats.put("averageAIConfidence", 0.0);
            return ResponseEntity.ok(errorStats);
        }
    }

    @PutMapping("/licenses/{id}")
    public ResponseEntity<DrivingLicense> updateLicense(@PathVariable Long id, @RequestBody DrivingLicense updatedLicense) {
        try {
            DrivingLicense license = drivingLicenseService.updateDrivingLicense(id, updatedLicense);
            return ResponseEntity.ok(license);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/licenses/{id}")
    public ResponseEntity<?> deleteLicense(@PathVariable Long id) {
        try {
            drivingLicenseService.deleteDrivingLicense(id);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Document Processing API is running");
    }

    @PostMapping("/test-handwriting")
    public ResponseEntity<String> testHandwritingDetection(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("Please upload a file");
            }

            String result = documentProcessingService.testHandwritingDetection(file.getBytes());
            return ResponseEntity.ok("Handwriting detection result: " + result);
            
        } catch (Exception e) {
            log.error("Handwriting detection test failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error testing handwriting detection: " + e.getMessage());
        }
    }
} 