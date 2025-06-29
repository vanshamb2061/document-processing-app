package com.documentprocessing.service;

import com.documentprocessing.model.DrivingLicense;
import com.documentprocessing.repository.DrivingLicenseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class DrivingLicenseService {
    private static final Logger log = LoggerFactory.getLogger(DrivingLicenseService.class);

    private final DrivingLicenseRepository drivingLicenseRepository;

    public DrivingLicenseService(DrivingLicenseRepository drivingLicenseRepository) {
        this.drivingLicenseRepository = drivingLicenseRepository;
    }

    public DrivingLicense saveDrivingLicense(DrivingLicense license) {
        // Check if we already have this license number
        Optional<DrivingLicense> existing = drivingLicenseRepository.findByLicenseNumber(license.getLicenseNumber());
        
        if (existing.isPresent()) {
            // Update the existing record
            DrivingLicense current = existing.get();
            log.info("Updating existing license: {}", license.getLicenseNumber());
            
            DrivingLicense updated = current.toBuilder()
                    .firstName(license.getFirstName())
                    .lastName(license.getLastName())
                    .middleName(license.getMiddleName())
                    .dateOfBirth(license.getDateOfBirth())
                    .address(license.getAddress())
                    .city(license.getCity())
                    .state(license.getState())
                    .zipCode(license.getZipCode())
                    .issueDate(license.getIssueDate())
                    .expiryDate(license.getExpiryDate())
                    .issuingAuthority(license.getIssuingAuthority())
                    .licenseClass(license.getLicenseClass())
                    .restrictions(license.getRestrictions())
                    .endorsements(license.getEndorsements())
                    .processingStatus(license.getProcessingStatus())
                    .confidenceScore(license.getConfidenceScore())
                    .aiProcessed(license.getAiProcessed())
                    .aiConfidence(license.getAiConfidence())
                    .handwritten(license.getHandwritten())
                    .documentType(license.getDocumentType())
                    .createdAt(license.getCreatedAt())
                    .build();
            
            return drivingLicenseRepository.save(updated);
        } else {
            log.info("Saving new license: {}", license.getLicenseNumber());
            return drivingLicenseRepository.save(license);
        }
    }

    public List<DrivingLicense> getAllLicenses() {
        return drivingLicenseRepository.findAll();
    }

    public Optional<DrivingLicense> findById(Long id) {
        return drivingLicenseRepository.findById(id);
    }

    public Optional<DrivingLicense> findByLicenseNumber(String licenseNumber) {
        return drivingLicenseRepository.findByLicenseNumber(licenseNumber);
    }

    public List<DrivingLicense> findByState(String state) {
        return drivingLicenseRepository.findByState(state);
    }

    public List<DrivingLicense> findByStatus(DrivingLicense.ProcessingStatus status) {
        return drivingLicenseRepository.findByProcessingStatus(status);
    }

    public List<DrivingLicense> findExpiredLicenses() {
        return drivingLicenseRepository.findByExpiryDateBefore(LocalDate.now());
    }

    public List<DrivingLicense> searchByName(String name) {
        return drivingLicenseRepository.findByNameContaining(name);
    }

    public List<DrivingLicense> findLowConfidenceLicenses(Double threshold) {
        return drivingLicenseRepository.findByLowConfidence(threshold);
    }

    public List<DrivingLicense> findAIProcessedLicenses() {
        // Filter licenses that were processed by AI
        return drivingLicenseRepository.findAll().stream()
                .filter(license -> Boolean.TRUE.equals(license.getAiProcessed()))
                .toList();
    }

    public DrivingLicense updateLicense(Long id, DrivingLicense updatedLicense) {
        Optional<DrivingLicense> existing = drivingLicenseRepository.findById(id);
        
        if (existing.isPresent()) {
            DrivingLicense current = existing.get();
            
            DrivingLicense updated = current.toBuilder()
                    .firstName(updatedLicense.getFirstName())
                    .lastName(updatedLicense.getLastName())
                    .middleName(updatedLicense.getMiddleName())
                    .dateOfBirth(updatedLicense.getDateOfBirth())
                    .address(updatedLicense.getAddress())
                    .city(updatedLicense.getCity())
                    .state(updatedLicense.getState())
                    .zipCode(updatedLicense.getZipCode())
                    .issueDate(updatedLicense.getIssueDate())
                    .expiryDate(updatedLicense.getExpiryDate())
                    .issuingAuthority(updatedLicense.getIssuingAuthority())
                    .licenseClass(updatedLicense.getLicenseClass())
                    .restrictions(updatedLicense.getRestrictions())
                    .endorsements(updatedLicense.getEndorsements())
                    .processingStatus(updatedLicense.getProcessingStatus())
                    .confidenceScore(updatedLicense.getConfidenceScore())
                    .aiProcessed(updatedLicense.getAiProcessed())
                    .aiConfidence(updatedLicense.getAiConfidence())
                    .build();
            
            log.info("Updated license: {}", updated.getLicenseNumber());
            return drivingLicenseRepository.save(updated);
        } else {
            throw new RuntimeException("License not found with id: " + id);
        }
    }

    public void deleteLicense(Long id) {
        if (drivingLicenseRepository.existsById(id)) {
            drivingLicenseRepository.deleteById(id);
            log.info("Deleted license with id: {}", id);
        } else {
            throw new RuntimeException("License not found with id: " + id);
        }
    }

    public boolean licenseNumberExists(String licenseNumber) {
        return drivingLicenseRepository.existsByLicenseNumber(licenseNumber);
    }

    public List<DrivingLicense> findByAIConfidenceRange(Double minConfidence, Double maxConfidence) {
        return drivingLicenseRepository.findAll().stream()
                .filter(license -> license.getAiConfidence() != null &&
                        license.getAiConfidence() >= minConfidence &&
                        license.getAiConfidence() <= maxConfidence)
                .toList();
    }

    // Legacy method names for backward compatibility
    public List<DrivingLicense> getAllDrivingLicenses() { return getAllLicenses(); }
    public Optional<DrivingLicense> getDrivingLicenseById(Long id) { return findById(id); }
    public Optional<DrivingLicense> getDrivingLicenseByNumber(String licenseNumber) { return findByLicenseNumber(licenseNumber); }
    public List<DrivingLicense> getDrivingLicensesByState(String state) { return findByState(state); }
    public List<DrivingLicense> getDrivingLicensesByStatus(DrivingLicense.ProcessingStatus status) { return findByStatus(status); }
    public List<DrivingLicense> getExpiredLicenses() { return findExpiredLicenses(); }
    public List<DrivingLicense> getLowConfidenceLicenses(Double threshold) { return findLowConfidenceLicenses(threshold); }
    public List<DrivingLicense> getAIProcessedLicenses() { return findAIProcessedLicenses(); }
    public DrivingLicense updateDrivingLicense(Long id, DrivingLicense updatedLicense) { return updateLicense(id, updatedLicense); }
    public void deleteDrivingLicense(Long id) { deleteLicense(id); }
    public boolean existsByLicenseNumber(String licenseNumber) { return licenseNumberExists(licenseNumber); }
    public List<DrivingLicense> getLicensesByAIConfidenceRange(Double minConfidence, Double maxConfidence) { return findByAIConfidenceRange(minConfidence, maxConfidence); }
} 