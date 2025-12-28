package com.ilyassan.albaraka.service;

import com.ilyassan.albaraka.entity.Document;
import com.ilyassan.albaraka.entity.FileType;
import com.ilyassan.albaraka.entity.Transaction;
import com.ilyassan.albaraka.entity.TransactionStatus;
import com.ilyassan.albaraka.repository.DocumentRepository;
import com.ilyassan.albaraka.repository.TransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class DocumentService {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    @Transactional
    public Document uploadJustification(Long transactionId, MultipartFile file) {
        // Find transaction
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found with ID: " + transactionId));

        // Only PENDING transactions can have justification uploaded
        if (transaction.getStatus() != TransactionStatus.PENDING) {
            throw new IllegalArgumentException("Only PENDING transactions require justification. Transaction status: " + transaction.getStatus());
        }

        // Check if document already exists
        if (documentRepository.existsByTransactionId(transactionId)) {
            throw new IllegalArgumentException("Justification already uploaded for this transaction");
        }

        // Validate file size
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds maximum limit of 5MB");
        }

        // Validate file type
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new IllegalArgumentException("Invalid filename");
        }

        String fileExtension = getFileExtension(originalFilename);
        FileType fileType = validateAndGetFileType(fileExtension);

        // Generate unique filename
        String uniqueFilename = UUID.randomUUID() + "." + fileExtension.toLowerCase();
        String storagePath = uploadDir + "/" + uniqueFilename;

        try {
            // Create upload directory if it doesn't exist
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Save file
            Path filePath = uploadPath.resolve(uniqueFilename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            log.info("File saved successfully: {}", filePath.toAbsolutePath());

            // Create and save Document entity
            Document document = Document.builder()
                    .fileName(originalFilename)
                    .fileType(fileType)
                    .storagePath(storagePath)
                    .transaction(transaction)
                    .build();

            document = documentRepository.save(document);

            // Update transaction justificationPath
            transaction.setJustificationPath(storagePath);
            transactionRepository.save(transaction);

            log.info("Document uploaded successfully for transaction ID: {}", transactionId);

            return document;

        } catch (IOException e) {
            log.error("Error saving file: {}", e.getMessage());
            throw new RuntimeException("Error uploading file: " + e.getMessage());
        }
    }

    public Resource loadFileAsResource(Long transactionId) {
        Document document = documentRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("No document found for transaction ID: " + transactionId));

        try {
            Path filePath = Paths.get(document.getStoragePath()).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("File not found or not readable: " + document.getFileName());
            }
        } catch (Exception e) {
            log.error("Error loading file: {}", e.getMessage());
            throw new RuntimeException("Error loading file: " + e.getMessage());
        }
    }

    public Optional<Document> getDocumentByTransactionId(Long transactionId) {
        return documentRepository.findByTransactionId(transactionId);
    }

    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf(".");
        if (lastDotIndex == -1) {
            throw new IllegalArgumentException("File has no extension");
        }
        return filename.substring(lastDotIndex + 1);
    }

    private FileType validateAndGetFileType(String extension) {
        try {
            return FileType.valueOf(extension.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unsupported file type. Only PDF, JPG, PNG, and JPEG are allowed");
        }
    }
}
