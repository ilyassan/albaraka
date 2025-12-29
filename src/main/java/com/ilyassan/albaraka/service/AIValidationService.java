package com.ilyassan.albaraka.service;

import com.ilyassan.albaraka.dto.AIValidationResult;
import com.ilyassan.albaraka.entity.*;
import com.ilyassan.albaraka.repository.DocumentRepository;
import com.ilyassan.albaraka.repository.TransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.model.Media;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
@Slf4j
public class AIValidationService {

    @Autowired
    private ChatModel chatModel;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    private static final BigDecimal LARGE_AMOUNT_THRESHOLD = new BigDecimal("50000");
    private static final BigDecimal MEDIUM_AMOUNT_THRESHOLD = new BigDecimal("20000");

    /**
     * Analyzes a transaction document using AI to provide recommendation
     * @param transactionId The transaction ID
     * @return AIValidationResult with recommendation and reasoning
     */
    public AIValidationResult analyzeTransactionDocument(Long transactionId) {
        log.info("Starting AI analysis for transaction ID: {}", transactionId);

        // Get transaction
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found with ID: " + transactionId));

        // Get document
        Document document = documentRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("No document found for transaction ID: " + transactionId));

        try {
            // Build the analysis prompt based on transaction type and amount
            String analysisPrompt = buildAnalysisPrompt(transaction);

            // Create message with image if document is image-based
            Message message = createMessageWithDocument(analysisPrompt, document);

            // Call Gemini AI
            Prompt prompt = new Prompt(List.of(message));
            String aiResponse = chatModel.call(prompt).getResult().getOutput().getContent();

            log.info("AI Response for transaction {}: {}", transactionId, aiResponse);

            // Parse the AI response
            return parseAIResponse(aiResponse, transaction);

        } catch (Exception e) {
            log.error("Error analyzing document with AI for transaction {}: {}", transactionId, e.getMessage(), e);
            // Return a default response requiring human review on error
            return AIValidationResult.builder()
                    .recommendation(AIRecommendation.REQUIRES_HUMAN_REVIEW)
                    .reasoning("AI analysis failed: " + e.getMessage())
                    .confidenceScore(0.0)
                    .isSuspicious(false)
                    .build();
        }
    }

    private String buildAnalysisPrompt(Transaction transaction) {
        String transactionType = transaction.getType();
        BigDecimal amount = transaction.getAmount();

        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a banking AI assistant specialized in analyzing transaction justification documents.\n\n");
        prompt.append("Transaction Details:\n");
        prompt.append("- Type: ").append(transactionType).append("\n");
        prompt.append("- Amount: ").append(amount).append(" DH\n\n");

        prompt.append("Your task is to analyze the provided document and determine if it's a valid justification for this transaction.\n\n");

        prompt.append("Please analyze the document and provide your response in the following format:\n");
        prompt.append("RECOMMENDATION: [APPROVE/REJECT/REQUIRES_HUMAN_REVIEW]\n");
        prompt.append("CONFIDENCE: [0.0-1.0]\n");
        prompt.append("SUSPICIOUS: [true/false]\n");
        prompt.append("REASONING: [Your detailed analysis]\n");
        prompt.append("EXTRACTED_TEXT: [Key text extracted from the document]\n\n");

        prompt.append("Consider the following:\n");
        prompt.append("1. Is the document authentic (no signs of tampering or forgery)?\n");
        prompt.append("2. Does the document amount match or justify the transaction amount?\n");
        prompt.append("3. Is the document type appropriate for this transaction?\n");
        prompt.append("4. Are there any red flags or suspicious elements?\n");
        prompt.append("5. Is the document clear and readable?\n\n");

        // Add specific guidelines based on transaction type
        if ("DEPOSIT".equals(transactionType)) {
            prompt.append("For DEPOSITS: Look for proof of income, bank statements, or legitimate source of funds.\n");
        } else if ("WITHDRAWAL".equals(transactionType)) {
            prompt.append("For WITHDRAWALS: Look for legitimate reasons, receipts, or proof of expenses.\n");
        } else if ("TRANSFER".equals(transactionType)) {
            prompt.append("For TRANSFERS: Look for invoices, contracts, or legitimate transfer reasons.\n");
        }

        // Add amount-based guidelines
        if (amount.compareTo(LARGE_AMOUNT_THRESHOLD) > 0) {
            prompt.append("\nNOTE: This is a LARGE amount transaction (>50,000 DH). Apply stricter scrutiny.\n");
        } else if (amount.compareTo(MEDIUM_AMOUNT_THRESHOLD) > 0) {
            prompt.append("\nNOTE: This is a MEDIUM amount transaction (>20,000 DH). Apply moderate scrutiny.\n");
        }

        return prompt.toString();
    }

    private Message createMessageWithDocument(String prompt, Document document) throws Exception {
        FileType fileType = document.getFileType();
        Path filePath = Paths.get(document.getStoragePath());
        File file = filePath.toFile();

        if (!file.exists()) {
            throw new IllegalArgumentException("Document file not found: " + document.getStoragePath());
        }

        // For image files (JPG, PNG, JPEG), include the image in the message
        if (fileType == FileType.JPG || fileType == FileType.PNG || fileType == FileType.JPEG) {
            String mimeType = getMimeType(fileType);
            Media media = new Media(MimeTypeUtils.parseMimeType(mimeType), new FileSystemResource(file));
            return new UserMessage(prompt, List.of(media));
        }

        // For PDF, just send text prompt (PDF vision support may vary)
        // In production, you might want to extract text from PDF first
        return new UserMessage(prompt + "\n\nNote: PDF document attached for review. Filename: " + document.getFileName());
    }

    private String getMimeType(FileType fileType) {
        return switch (fileType) {
            case JPG, JPEG -> "image/jpeg";
            case PNG -> "image/png";
            case PDF -> "application/pdf";
        };
    }

    private AIValidationResult parseAIResponse(String response, Transaction transaction) {
        try {
            AIRecommendation recommendation = extractRecommendation(response);
            Double confidence = extractConfidence(response);
            Boolean suspicious = extractSuspicious(response);
            String reasoning = extractReasoning(response);
            String extractedText = extractExtractedText(response);

            return AIValidationResult.builder()
                    .recommendation(recommendation)
                    .confidenceScore(confidence)
                    .isSuspicious(suspicious)
                    .reasoning(reasoning)
                    .extractedText(extractedText)
                    .build();

        } catch (Exception e) {
            log.error("Error parsing AI response: {}", e.getMessage());
            return AIValidationResult.builder()
                    .recommendation(AIRecommendation.REQUIRES_HUMAN_REVIEW)
                    .reasoning("Failed to parse AI response: " + response)
                    .confidenceScore(0.5)
                    .isSuspicious(false)
                    .build();
        }
    }

    private AIRecommendation extractRecommendation(String response) {
        if (response.contains("RECOMMENDATION:")) {
            String recLine = extractLine(response, "RECOMMENDATION:");
            if (recLine.contains("APPROVE") && !recLine.contains("REJECT")) {
                return AIRecommendation.APPROVE;
            } else if (recLine.contains("REJECT")) {
                return AIRecommendation.REJECT;
            } else {
                return AIRecommendation.REQUIRES_HUMAN_REVIEW;
            }
        }
        return AIRecommendation.REQUIRES_HUMAN_REVIEW;
    }

    private Double extractConfidence(String response) {
        try {
            String confLine = extractLine(response, "CONFIDENCE:");
            String confValue = confLine.replaceAll("[^0-9.]", "");
            return Double.parseDouble(confValue);
        } catch (Exception e) {
            return 0.5; // Default confidence
        }
    }

    private Boolean extractSuspicious(String response) {
        String suspLine = extractLine(response, "SUSPICIOUS:");
        return suspLine.toLowerCase().contains("true");
    }

    private String extractReasoning(String response) {
        return extractLine(response, "REASONING:");
    }

    private String extractExtractedText(String response) {
        return extractLine(response, "EXTRACTED_TEXT:");
    }

    private String extractLine(String response, String prefix) {
        try {
            int startIndex = response.indexOf(prefix);
            if (startIndex == -1) {
                return "";
            }
            startIndex += prefix.length();
            int endIndex = response.indexOf("\n", startIndex);
            if (endIndex == -1) {
                endIndex = response.length();
            }
            return response.substring(startIndex, endIndex).trim();
        } catch (Exception e) {
            return "";
        }
    }
}
