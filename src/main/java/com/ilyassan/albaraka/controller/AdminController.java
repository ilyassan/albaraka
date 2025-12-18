package com.ilyassan.albaraka.controller;

import com.ilyassan.albaraka.dto.CreateUserRequest;
import com.ilyassan.albaraka.dto.UserResponse;
import com.ilyassan.albaraka.dto.TransactionResponse;
import com.ilyassan.albaraka.entity.User;
import com.ilyassan.albaraka.entity.UserRole;
import com.ilyassan.albaraka.entity.Transaction;
import com.ilyassan.albaraka.service.UserService;
import com.ilyassan.albaraka.service.TransactionService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@Slf4j
public class AdminController {

    @Autowired
    private UserService userService;

    @Autowired
    private TransactionService transactionService;

    @PostMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createUser(@Valid @RequestBody CreateUserRequest request) {
        try {
            UserRole role = UserRole.valueOf(request.getRole().toUpperCase());
            User user = userService.createUser(
                    request.getEmail(),
                    request.getPassword(),
                    request.getFirstName(),
                    request.getLastName(),
                    role
            );

            UserResponse response = mapToUserResponse(user);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            log.error("Error creating user", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            log.error("Error creating user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error creating user");
        }
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllUsers() {
        try {
            List<User> users = userService.getAllUsers();
            List<UserResponse> responses = users.stream()
                    .map(this::mapToUserResponse)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            log.error("Error getting users", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error retrieving users");
        }
    }

    @GetMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getUser(@PathVariable Long userId) {
        try {
            User user = userService.getUserById(userId).orElse(null);

            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
            }

            return ResponseEntity.ok(mapToUserResponse(user));
        } catch (Exception e) {
            log.error("Error getting user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error retrieving user");
        }
    }

    @PutMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateUser(@PathVariable Long userId, @RequestBody CreateUserRequest request) {
        try {
            User user = userService.updateUser(userId, request.getFirstName(), request.getLastName());
            return ResponseEntity.ok(mapToUserResponse(user));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            log.error("Error updating user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error updating user");
        }
    }

    @DeleteMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deactivateUser(@PathVariable Long userId) {
        try {
            userService.deactivateUser(userId);
            return ResponseEntity.ok("User deactivated successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            log.error("Error deactivating user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error deactivating user");
        }
    }

    @GetMapping("/transactions/pending")
    @PreAuthorize("hasRole('AGENT_BANCAIRE')")
    public ResponseEntity<?> getPendingTransactions() {
        try {
            List<Transaction> pendingTransactions = transactionService.getPendingTransactions();
            List<TransactionResponse> responses = pendingTransactions.stream()
                    .map(this::mapToTransactionResponse)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            log.error("Error getting pending transactions", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error retrieving pending transactions");
        }
    }

    @PostMapping("/transactions/{transactionId}/approve")
    @PreAuthorize("hasRole('AGENT_BANCAIRE')")
    public ResponseEntity<?> approveTransaction(@PathVariable Long transactionId) {
        try {
            Transaction transaction = transactionService.approveTransaction(transactionId);
            return ResponseEntity.ok(mapToTransactionResponse(transaction));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            log.error("Error approving transaction", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error approving transaction");
        }
    }

    @PostMapping("/transactions/{transactionId}/reject")
    @PreAuthorize("hasRole('AGENT_BANCAIRE')")
    public ResponseEntity<?> rejectTransaction(@PathVariable Long transactionId) {
        try {
            Transaction transaction = transactionService.rejectTransaction(transactionId);
            return ResponseEntity.ok(mapToTransactionResponse(transaction));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            log.error("Error rejecting transaction", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error rejecting transaction");
        }
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .enabled(user.getEnabled())
                .build();
    }

    private TransactionResponse mapToTransactionResponse(Transaction transaction) {
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
