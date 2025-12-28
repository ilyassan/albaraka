package com.ilyassan.albaraka.controller;

import com.ilyassan.albaraka.entity.Document;
import com.ilyassan.albaraka.service.DocumentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
@Slf4j
public class DocumentController {

    @Autowired
    private DocumentService documentService;

    @PostMapping("/upload/{transactionId}")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<?> uploadJustification(
            @PathVariable Long transactionId,
            @RequestParam("file") MultipartFile file) {
        try {
            Document document = documentService.uploadJustification(transactionId, file);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Justification uploaded successfully");
            response.put("documentId", document.getId());
            response.put("fileName", document.getFileName());
            response.put("fileType", document.getFileType());
            response.put("uploadedAt", document.getUploadedAt());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            log.error("Validation error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            log.error("Error uploading document: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error uploading justification: " + e.getMessage());
        }
    }
}
