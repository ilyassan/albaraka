package com.ilyassan.albaraka.config;

import com.ilyassan.albaraka.entity.*;
import com.ilyassan.albaraka.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
@Slf4j
public class DataSeeder implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // Check if data already exists
        if (userRepository.count() > 0) {
            log.info("Database already seeded. Skipping seeding process.");
            return;
        }

        log.info("Starting database seeding...");

        try {
            // Seed Users
            User client1 = seedClient("client@albaraka.com", "Ahmed", "Hassan");
            User client2 = seedClient("client2@albaraka.com", "Fatima", "Zahra");
            User agent = seedAgent("agent@albaraka.com", "Sara", "Ali");
            User admin = seedAdmin("admin@albaraka.com", "Mohammed", "Ahmed");

            // Get Accounts
            Account account1 = accountRepository.findByUserId(client1.getId()).orElseThrow();
            Account account2 = accountRepository.findByUserId(client2.getId()).orElseThrow();

            // Seed Transactions for Client 1
            seedSmallDeposit(account1, new BigDecimal("5000")); // Auto-approved
            seedSmallDeposit(account1, new BigDecimal("3000")); // Auto-approved
            seedLargeDeposit(account1, new BigDecimal("15000")); // Pending
            seedSmallWithdrawal(account1, new BigDecimal("1000")); // Auto-approved
            seedLargeWithdrawal(account1, new BigDecimal("12000")); // Pending

            // Seed Transactions for Client 2
            seedSmallDeposit(account2, new BigDecimal("8000")); // Auto-approved
            seedLargeDeposit(account2, new BigDecimal("20000")); // Pending

            // Seed Transfer from Client 1 to Client 2
            seedSmallTransfer(account1, account2, new BigDecimal("500")); // Auto-approved

            log.info("Database seeding completed successfully!");
            log.info("=================================================");
            log.info("SEEDED DATA SUMMARY:");
            log.info("=================================================");
            log.info("USERS:");
            log.info("  CLIENT 1: client@albaraka.com / Password123!");
            log.info("  CLIENT 2: client2@albaraka.com / Password123!");
            log.info("  AGENT:    agent@albaraka.com / Password123!");
            log.info("  ADMIN:    admin@albaraka.com / Password123!");
            log.info("=================================================");
            log.info("TRANSACTIONS:");
            log.info("  Client 1 Balance: " + account1.getBalance() + " DH");
            log.info("  Client 2 Balance: " + account2.getBalance() + " DH");
            log.info("  Total Users: " + userRepository.count());
            log.info("  Total Accounts: " + accountRepository.count());
            log.info("  Total Transactions: " + transactionRepository.count());
            log.info("  Pending Transactions: " + transactionRepository.findByStatus(TransactionStatus.PENDING).size());
            log.info("=================================================");

        } catch (Exception e) {
            log.error("Error during database seeding: {}", e.getMessage(), e);
        }
    }

    private User seedClient(String email, String firstName, String lastName) {
        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode("Password123!"))
                .firstName(firstName)
                .lastName(lastName)
                .role(UserRole.CLIENT)
                .build();
        user = userRepository.save(user);

        // Create account for client
        Account account = Account.builder()
                .user(user)
                .accountNumber(generateAccountNumber())
                .balance(BigDecimal.ZERO)
                .build();
        accountRepository.save(account);

        log.info("Seeded CLIENT: {} - Account: {}", email, account.getAccountNumber());
        return user;
    }

    private User seedAgent(String email, String firstName, String lastName) {
        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode("Password123!"))
                .firstName(firstName)
                .lastName(lastName)
                .role(UserRole.AGENT_BANCAIRE)
                .build();
        user = userRepository.save(user);
        log.info("Seeded AGENT: {}", email);
        return user;
    }

    private User seedAdmin(String email, String firstName, String lastName) {
        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode("Password123!"))
                .firstName(firstName)
                .lastName(lastName)
                .role(UserRole.ADMIN)
                .build();
        user = userRepository.save(user);
        log.info("Seeded ADMIN: {}", email);
        return user;
    }

    private void seedSmallDeposit(Account account, BigDecimal amount) {
        Transaction transaction = Transaction.builder()
                .account(account)
                .type("DEPOSIT")
                .amount(amount)
                .status(TransactionStatus.COMPLETED) // Auto-approved (≤ 10,000 DH)
                .build();
        transactionRepository.save(transaction);

        // Update balance
        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);

        log.info("Seeded DEPOSIT (auto-approved): {} DH for account {}", amount, account.getAccountNumber());
    }

    private void seedLargeDeposit(Account account, BigDecimal amount) {
        Transaction transaction = Transaction.builder()
                .account(account)
                .type("DEPOSIT")
                .amount(amount)
                .status(TransactionStatus.PENDING) // Requires approval (> 10,000 DH)
                .build();
        transactionRepository.save(transaction);

        log.info("Seeded DEPOSIT (pending): {} DH for account {}", amount, account.getAccountNumber());
    }

    private void seedSmallWithdrawal(Account account, BigDecimal amount) {
        if (account.getBalance().compareTo(amount) >= 0) {
            Transaction transaction = Transaction.builder()
                    .account(account)
                    .type("WITHDRAWAL")
                    .amount(amount)
                    .status(TransactionStatus.COMPLETED) // Auto-approved (≤ 10,000 DH)
                    .build();
            transactionRepository.save(transaction);

            // Update balance
            account.setBalance(account.getBalance().subtract(amount));
            accountRepository.save(account);

            log.info("Seeded WITHDRAWAL (auto-approved): {} DH for account {}", amount, account.getAccountNumber());
        } else {
            log.warn("Skipped WITHDRAWAL: Insufficient balance for account {}", account.getAccountNumber());
        }
    }

    private void seedLargeWithdrawal(Account account, BigDecimal amount) {
        Transaction transaction = Transaction.builder()
                .account(account)
                .type("WITHDRAWAL")
                .amount(amount)
                .status(TransactionStatus.PENDING) // Requires approval (> 10,000 DH)
                .build();
        transactionRepository.save(transaction);

        log.info("Seeded WITHDRAWAL (pending): {} DH for account {}", amount, account.getAccountNumber());
    }

    private void seedSmallTransfer(Account sourceAccount, Account beneficiaryAccount, BigDecimal amount) {
        if (sourceAccount.getBalance().compareTo(amount) >= 0) {
            Transaction transaction = Transaction.builder()
                    .account(sourceAccount)
                    .type("TRANSFER")
                    .amount(amount)
                    .beneficiaryAccountId(beneficiaryAccount.getId())
                    .status(TransactionStatus.COMPLETED) // Auto-approved (≤ 10,000 DH)
                    .build();
            transactionRepository.save(transaction);

            // Update balances
            sourceAccount.setBalance(sourceAccount.getBalance().subtract(amount));
            beneficiaryAccount.setBalance(beneficiaryAccount.getBalance().add(amount));
            accountRepository.save(sourceAccount);
            accountRepository.save(beneficiaryAccount);

            log.info("Seeded TRANSFER (auto-approved): {} DH from {} to {}",
                    amount, sourceAccount.getAccountNumber(), beneficiaryAccount.getAccountNumber());
        } else {
            log.warn("Skipped TRANSFER: Insufficient balance for account {}", sourceAccount.getAccountNumber());
        }
    }

    private String generateAccountNumber() {
        return "ALBARAKA" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8);
    }
}
