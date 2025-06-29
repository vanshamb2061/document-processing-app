package com.documentprocessing.repository;

import com.documentprocessing.model.DrivingLicense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DrivingLicenseRepository extends JpaRepository<DrivingLicense, Long> {

    Optional<DrivingLicense> findByLicenseNumber(String licenseNumber);
    
    List<DrivingLicense> findByState(String state);
    
    List<DrivingLicense> findByProcessingStatus(DrivingLicense.ProcessingStatus status);
    
    List<DrivingLicense> findByExpiryDateBefore(LocalDate date);
    
    @Query("SELECT dl FROM DrivingLicense dl WHERE dl.firstName LIKE %:name% OR dl.lastName LIKE %:name%")
    List<DrivingLicense> findByNameContaining(@Param("name") String name);
    
    @Query("SELECT dl FROM DrivingLicense dl WHERE dl.confidenceScore < :threshold")
    List<DrivingLicense> findByLowConfidence(@Param("threshold") Double threshold);
    
    boolean existsByLicenseNumber(String licenseNumber);
} 