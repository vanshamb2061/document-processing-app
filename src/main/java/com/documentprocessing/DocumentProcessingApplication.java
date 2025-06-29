package com.documentprocessing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DocumentProcessingApplication {

    public static void main(String[] args) {
        // Set Tesseract paths before Spring Boot starts
        System.setProperty("jna.library.path", "/opt/homebrew/opt/tesseract/lib");
        System.setProperty("TESSDATA_PREFIX", "/opt/homebrew/Cellar/tesseract/5.5.1/share/tessdata");
        
        SpringApplication.run(DocumentProcessingApplication.class, args);
    }
} 