package com.ilyassan.albaraka.controller;

import com.ilyassan.albaraka.dto.TransactionRequest;
import com.ilyassan.albaraka.dto.TransactionResponse;
import com.ilyassan.albaraka.entity.Account;
import com.ilyassan.albaraka.entity.Transaction;
import com.ilyassan.albaraka.entity.User;
import com.ilyassan.albaraka.repository.UserRepository;
import com.ilyassan.albaraka.service.AccountService;
import com.ilyassan.albaraka.service.TransactionService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/transactions")
@Slf4j
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/deposit")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<?> createDeposit(@Valid @RequestBody TransactionRequest request, Authentication authentication) {
        try {
            String email = authentication.getName();
            User user = userRepository.findByEmail(email).orElse(null);

            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
            }

            Account account = accountService.getAccountByUserId(user.getId()).orElse(null);

            if (account == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Account not found");
            }

            Transaction transaction = transactionService.createDeposit(account.getId(), request.getAmount());
            return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(transaction));
        } catch (Exception e) {
            log.error("Error creating deposit", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping("/withdrawal")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<?> createWithdrawal(@Valid @RequestBody TransactionRequest request, Authentication authentication) {
        try {
            String email = authentication.getName();
            User user = userRepository.findByEmail(email).orElse(null);

            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
            }

            Account account = accountService.getAccountByUserId(user.getId()).orElse(null);

            if (account == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Account not found");
            }

            Transaction transaction = transactionService.createWithdrawal(account.getId(), request.getAmount());
            return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(transaction));
        } catch (Exception e) {
            log.error("Error creating withdrawal", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping("/transfer")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<?> createTransfer(@Valid @RequestBody TransactionRequest request, Authentication authentication) {
        try {
            String email = authentication.getName();
            User user = userRepository.findByEmail(email).orElse(null);

            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
            }

            Account sourceAccount = accountService.getAccountByUserId(user.getId()).orElse(null);

            if (sourceAccount == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Account not found");
            }

            if (request.getBeneficiaryAccountId() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Beneficiary account ID is required");
            }

            Transaction transaction = transactionService.createTransfer(
                    sourceAccount.getId(),
                    request.getBeneficiaryAccountId(),
                    request.getAmount()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(transaction));
        } catch (Exception e) {
            log.error("Error creating transfer", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<?> getMyTransactions(Authentication authentication, Pageable pageable) {
        try {
            String email = authentication.getName();
            User user = userRepository.findByEmail(email).orElse(null);

            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
            }

            Account account = accountService.getAccountByUserId(user.getId()).orElse(null);

            if (account == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Account not found");
            }

            Page<Transaction> transactions = transactionService.getAccountTransactions(account.getId(), pageable);
            Page<TransactionResponse> responses = transactions.map(this::mapToResponse);
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            log.error("Error getting transactions", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error retrieving transactions");
        }
    }

    @GetMapping("/{transactionId}")
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN')")
    public ResponseEntity<?> getTransaction(@PathVariable Long transactionId) {
        try {
            Transaction transaction = transactionService.getTransactionById(transactionId).orElse(null);

            if (transaction == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Transaction not found");
            }

            return ResponseEntity.ok(mapToResponse(transaction));
        } catch (Exception e) {
            log.error("Error getting transaction", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error retrieving transaction");
        }
    }

    private TransactionResponse mapToResponse(Transaction transaction) {
        return TransactionResponse.builder()
                .id(transaction.getId())
                .type(transaction.getType())
                .amount(transaction.getAmount())
                .status(transaction.getStatus().name())
                .justificationPath(transaction.getJustificationPath())
                .beneficiaryAccountId(transaction.getBeneficiaryAccountId())
                .createdAt(transaction.getCreatedAt())
                .updatedAt(transaction.getUpdatedAt())
                .build();
    }
}
