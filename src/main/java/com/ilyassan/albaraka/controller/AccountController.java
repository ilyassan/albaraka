package com.ilyassan.albaraka.controller;

import com.ilyassan.albaraka.entity.Account;
import com.ilyassan.albaraka.entity.User;
import com.ilyassan.albaraka.mapper.AccountMapper;
import com.ilyassan.albaraka.repository.UserRepository;
import com.ilyassan.albaraka.service.AccountService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/accounts")
@Slf4j
public class AccountController {

    @Autowired
    private AccountService accountService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountMapper accountMapper;

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('CLIENT', 'AGENT_BANCAIRE', 'ADMIN')")
    public ResponseEntity<?> getCurrentUserAccount(Authentication authentication) {
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

            return ResponseEntity.ok(accountMapper.toAccountResponse(account));
        } catch (Exception e) {
            log.error("Error getting current user account", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error retrieving account");
        }
    }

    @GetMapping("/{accountId}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<?> getAccount(@PathVariable Long accountId) {
        try {
            Account account = accountService.getAccountById(accountId).orElse(null);

            if (account == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Account not found");
            }

            return ResponseEntity.ok(accountMapper.toAccountResponse(account));
        } catch (Exception e) {
            log.error("Error getting account", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error retrieving account");
        }
    }

    @GetMapping("/me/balance")
    @PreAuthorize("hasAnyRole('CLIENT')")
    public ResponseEntity<?> getBalance(Authentication authentication) {
        try {
            String email = authentication.getName();
            User user = userRepository.findByEmail(email).orElse(null);

            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
            }

            BigDecimal balance = accountService.getAccountByUserId(user.getId())
                    .map(Account::getBalance)
                    .orElse(null);

            if (balance == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Account not found");
            }

            Map<String, Object> response = new HashMap<>();
            response.put("balance", balance);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting balance", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error retrieving balance");
        }
    }
}
