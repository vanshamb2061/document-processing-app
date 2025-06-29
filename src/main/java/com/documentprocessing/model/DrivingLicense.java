package com.documentprocessing.model;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "driving_licenses")
public class DrivingLicense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "license_number", unique = true)
    private String licenseNumber;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "middle_name")
    private String middleName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "address")
    private String address;

    @Column(name = "city")
    private String city;

    @Column(name = "state")
    private String state;

    @Column(name = "zip_code")
    private String zipCode;

    @Column(name = "license_class")
    private String licenseClass;

    @Column(name = "issue_date")
    private LocalDate issueDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "issuing_authority")
    private String issuingAuthority;

    @Column(name = "restrictions")
    private String restrictions;

    @Column(name = "endorsements")
    private String endorsements;

    @Column(name = "document_type")
    private String documentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status")
    private ProcessingStatus processingStatus;

    @Column(name = "confidence_score")
    private Double confidenceScore;

    @Column(name = "ai_processed")
    private Boolean aiProcessed;

    @Column(name = "ai_confidence")
    private Double aiConfidence;

    @Column(name = "created_at")
    private LocalDate createdAt;

    @Column(name = "handwritten")
    private Boolean handwritten;

    // Default constructor
    public DrivingLicense() {}

    // Constructor with all fields
    public DrivingLicense(Long id, String licenseNumber, String firstName, String lastName, String middleName,
                         LocalDate dateOfBirth, String address, String city, String state, String zipCode,
                         String licenseClass, LocalDate issueDate, LocalDate expiryDate, String issuingAuthority,
                         String restrictions, String endorsements, String documentType, ProcessingStatus processingStatus,
                         Double confidenceScore, Boolean aiProcessed, Double aiConfidence, LocalDate createdAt,
                         Boolean handwritten) {
        this.id = id;
        this.licenseNumber = licenseNumber;
        this.firstName = firstName;
        this.lastName = lastName;
        this.middleName = middleName;
        this.dateOfBirth = dateOfBirth;
        this.address = address;
        this.city = city;
        this.state = state;
        this.zipCode = zipCode;
        this.licenseClass = licenseClass;
        this.issueDate = issueDate;
        this.expiryDate = expiryDate;
        this.issuingAuthority = issuingAuthority;
        this.restrictions = restrictions;
        this.endorsements = endorsements;
        this.documentType = documentType;
        this.processingStatus = processingStatus;
        this.confidenceScore = confidenceScore;
        this.aiProcessed = aiProcessed;
        this.aiConfidence = aiConfidence;
        this.createdAt = createdAt;
        this.handwritten = handwritten;
    }

    // Builder pattern
    public static DrivingLicenseBuilder builder() {
        return new DrivingLicenseBuilder();
    }

    public DrivingLicenseBuilder toBuilder() {
        return new DrivingLicenseBuilder()
                .id(this.id)
                .licenseNumber(this.licenseNumber)
                .firstName(this.firstName)
                .lastName(this.lastName)
                .middleName(this.middleName)
                .dateOfBirth(this.dateOfBirth)
                .address(this.address)
                .city(this.city)
                .state(this.state)
                .zipCode(this.zipCode)
                .licenseClass(this.licenseClass)
                .issueDate(this.issueDate)
                .expiryDate(this.expiryDate)
                .issuingAuthority(this.issuingAuthority)
                .restrictions(this.restrictions)
                .endorsements(this.endorsements)
                .documentType(this.documentType)
                .processingStatus(this.processingStatus)
                .confidenceScore(this.confidenceScore)
                .aiProcessed(this.aiProcessed)
                .aiConfidence(this.aiConfidence)
                .createdAt(this.createdAt)
                .handwritten(this.handwritten);
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getLicenseNumber() { return licenseNumber; }
    public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getMiddleName() { return middleName; }
    public void setMiddleName(String middleName) { this.middleName = middleName; }

    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getZipCode() { return zipCode; }
    public void setZipCode(String zipCode) { this.zipCode = zipCode; }

    public String getLicenseClass() { return licenseClass; }
    public void setLicenseClass(String licenseClass) { this.licenseClass = licenseClass; }

    public LocalDate getIssueDate() { return issueDate; }
    public void setIssueDate(LocalDate issueDate) { this.issueDate = issueDate; }

    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }

    public String getIssuingAuthority() { return issuingAuthority; }
    public void setIssuingAuthority(String issuingAuthority) { this.issuingAuthority = issuingAuthority; }

    public String getRestrictions() { return restrictions; }
    public void setRestrictions(String restrictions) { this.restrictions = restrictions; }

    public String getEndorsements() { return endorsements; }
    public void setEndorsements(String endorsements) { this.endorsements = endorsements; }

    public String getDocumentType() { return documentType; }
    public void setDocumentType(String documentType) { this.documentType = documentType; }

    public ProcessingStatus getProcessingStatus() { return processingStatus; }
    public void setProcessingStatus(ProcessingStatus processingStatus) { this.processingStatus = processingStatus; }

    public Double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(Double confidenceScore) { this.confidenceScore = confidenceScore; }

    public Boolean getAiProcessed() { return aiProcessed; }
    public void setAiProcessed(Boolean aiProcessed) { this.aiProcessed = aiProcessed; }

    public Double getAiConfidence() { return aiConfidence; }
    public void setAiConfidence(Double aiConfidence) { this.aiConfidence = aiConfidence; }

    public LocalDate getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDate createdAt) { this.createdAt = createdAt; }

    public Boolean getHandwritten() { return handwritten; }
    public void setHandwritten(Boolean handwritten) { this.handwritten = handwritten; }

    public enum ProcessingStatus {
        PROCESSING,
        PROCESSED,
        FAILED,
        MANUAL_REVIEW_REQUIRED
    }

    // Builder class
    public static class DrivingLicenseBuilder {
        private Long id;
        private String licenseNumber;
        private String firstName;
        private String lastName;
        private String middleName;
        private LocalDate dateOfBirth;
        private String address;
        private String city;
        private String state;
        private String zipCode;
        private String licenseClass;
        private LocalDate issueDate;
        private LocalDate expiryDate;
        private String issuingAuthority;
        private String restrictions;
        private String endorsements;
        private String documentType;
        private ProcessingStatus processingStatus;
        private Double confidenceScore;
        private Boolean aiProcessed;
        private Double aiConfidence;
        private LocalDate createdAt;
        private Boolean handwritten;

        public DrivingLicenseBuilder id(Long id) { this.id = id; return this; }
        public DrivingLicenseBuilder licenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; return this; }
        public DrivingLicenseBuilder firstName(String firstName) { this.firstName = firstName; return this; }
        public DrivingLicenseBuilder lastName(String lastName) { this.lastName = lastName; return this; }
        public DrivingLicenseBuilder middleName(String middleName) { this.middleName = middleName; return this; }
        public DrivingLicenseBuilder dateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; return this; }
        public DrivingLicenseBuilder address(String address) { this.address = address; return this; }
        public DrivingLicenseBuilder city(String city) { this.city = city; return this; }
        public DrivingLicenseBuilder state(String state) { this.state = state; return this; }
        public DrivingLicenseBuilder zipCode(String zipCode) { this.zipCode = zipCode; return this; }
        public DrivingLicenseBuilder licenseClass(String licenseClass) { this.licenseClass = licenseClass; return this; }
        public DrivingLicenseBuilder issueDate(LocalDate issueDate) { this.issueDate = issueDate; return this; }
        public DrivingLicenseBuilder expiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; return this; }
        public DrivingLicenseBuilder issuingAuthority(String issuingAuthority) { this.issuingAuthority = issuingAuthority; return this; }
        public DrivingLicenseBuilder restrictions(String restrictions) { this.restrictions = restrictions; return this; }
        public DrivingLicenseBuilder endorsements(String endorsements) { this.endorsements = endorsements; return this; }
        public DrivingLicenseBuilder documentType(String documentType) { this.documentType = documentType; return this; }
        public DrivingLicenseBuilder processingStatus(ProcessingStatus processingStatus) { this.processingStatus = processingStatus; return this; }
        public DrivingLicenseBuilder confidenceScore(Double confidenceScore) { this.confidenceScore = confidenceScore; return this; }
        public DrivingLicenseBuilder aiProcessed(Boolean aiProcessed) { this.aiProcessed = aiProcessed; return this; }
        public DrivingLicenseBuilder aiConfidence(Double aiConfidence) { this.aiConfidence = aiConfidence; return this; }
        public DrivingLicenseBuilder createdAt(LocalDate createdAt) { this.createdAt = createdAt; return this; }
        public DrivingLicenseBuilder handwritten(Boolean handwritten) { this.handwritten = handwritten; return this; }

        public DrivingLicense build() {
            return new DrivingLicense(id, licenseNumber, firstName, lastName, middleName, dateOfBirth, address, city, state, zipCode,
                    licenseClass, issueDate, expiryDate, issuingAuthority, restrictions, endorsements, documentType, processingStatus,
                    confidenceScore, aiProcessed, aiConfidence, createdAt, handwritten);
        }
    }
} 