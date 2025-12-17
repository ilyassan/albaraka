package com.ilyassan.albaraka.service;

import com.ilyassan.albaraka.entity.Account;
import com.ilyassan.albaraka.entity.Transaction;
import com.ilyassan.albaraka.entity.TransactionStatus;
import com.ilyassan.albaraka.repository.AccountRepository;
import com.ilyassan.albaraka.repository.TransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class TransactionService {

    private static final BigDecimal VALIDATION_THRESHOLD = new BigDecimal("10000");

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AccountService accountService;

    @Transactional
    public Transaction createDeposit(Long accountId, BigDecimal amount) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        Transaction transaction = Transaction.builder()
                .account(account)
                .type("DEPOSIT")
                .amount(amount)
                .build();

        // Auto-approve if amount <= 10,000 DH
        if (amount.compareTo(VALIDATION_THRESHOLD) <= 0) {
            transaction.setStatus(TransactionStatus.COMPLETED);
            accountService.updateBalance(accountId, amount);
            log.info("Deposit auto-approved for account: {} amount: {}", accountId, amount);
        } else {
            transaction.setStatus(TransactionStatus.PENDING);
            log.info("Deposit pending validation for account: {} amount: {}", accountId, amount);
        }

        return transactionRepository.save(transaction);
    }

    @Transactional
    public Transaction createWithdrawal(Long accountId, BigDecimal amount) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        // Check sufficient balance
        if (account.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient balance");
        }

        Transaction transaction = Transaction.builder()
                .account(account)
                .type("WITHDRAWAL")
                .amount(amount)
                .build();

        // Auto-approve if amount <= 10,000 DH
        if (amount.compareTo(VALIDATION_THRESHOLD) <= 0) {
            transaction.setStatus(TransactionStatus.COMPLETED);
            accountService.updateBalance(accountId, amount.negate());
            log.info("Withdrawal auto-approved for account: {} amount: {}", accountId, amount);
        } else {
            transaction.setStatus(TransactionStatus.PENDING);
            log.info("Withdrawal pending validation for account: {} amount: {}", accountId, amount);
        }

        return transactionRepository.save(transaction);
    }

    @Transactional
    public Transaction createTransfer(Long sourceAccountId, Long beneficiaryAccountId, BigDecimal amount) {
        Account sourceAccount = accountRepository.findById(sourceAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Source account not found"));

        Account beneficiaryAccount = accountRepository.findById(beneficiaryAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Beneficiary account not found"));

        // Check sufficient balance
        if (sourceAccount.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient balance");
        }

        Transaction transaction = Transaction.builder()
                .account(sourceAccount)
                .type("TRANSFER")
                .amount(amount)
                .beneficiaryAccountId(beneficiaryAccountId)
                .build();

        // Auto-approve if amount <= 10,000 DH
        if (amount.compareTo(VALIDATION_THRESHOLD) <= 0) {
            transaction.setStatus(TransactionStatus.COMPLETED);
            accountService.updateBalance(sourceAccountId, amount.negate());
            accountService.updateBalance(beneficiaryAccountId, amount);
            log.info("Transfer auto-approved from account: {} to account: {} amount: {}",
                    sourceAccountId, beneficiaryAccountId, amount);
        } else {
            transaction.setStatus(TransactionStatus.PENDING);
            log.info("Transfer pending validation from account: {} to account: {} amount: {}",
                    sourceAccountId, beneficiaryAccountId, amount);
        }

        return transactionRepository.save(transaction);
    }

    @Transactional
    public Transaction approveTransaction(Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));

        if (transaction.getStatus() != TransactionStatus.PENDING) {
            throw new IllegalArgumentException("Transaction is not pending");
        }

        transaction.setStatus(TransactionStatus.APPROVED);

        // Execute the transaction based on type
        if ("DEPOSIT".equals(transaction.getType())) {
            accountService.updateBalance(transaction.getAccount().getId(), transaction.getAmount());
        } else if ("WITHDRAWAL".equals(transaction.getType())) {
            accountService.updateBalance(transaction.getAccount().getId(), transaction.getAmount().negate());
        } else if ("TRANSFER".equals(transaction.getType())) {
            accountService.updateBalance(transaction.getAccount().getId(), transaction.getAmount().negate());
            accountService.updateBalance(transaction.getBeneficiaryAccountId(), transaction.getAmount());
        }

        transaction.setStatus(TransactionStatus.COMPLETED);
        log.info("Transaction approved and completed: {}", transactionId);
        return transactionRepository.save(transaction);
    }

    @Transactional
    public Transaction rejectTransaction(Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));

        if (transaction.getStatus() != TransactionStatus.PENDING) {
            throw new IllegalArgumentException("Transaction is not pending");
        }

        transaction.setStatus(TransactionStatus.REJECTED);
        log.info("Transaction rejected: {}", transactionId);
        return transactionRepository.save(transaction);
    }

    public Optional<Transaction> getTransactionById(Long transactionId) {
        return transactionRepository.findById(transactionId);
    }

    public Page<Transaction> getAccountTransactions(Long accountId, Pageable pageable) {
        return transactionRepository.findByAccountId(accountId, pageable);
    }

    public List<Transaction> getPendingTransactions() {
        return transactionRepository.findByStatus(TransactionStatus.PENDING);
    }

    public List<Transaction> getAccountPendingTransactions(Long accountId) {
        return transactionRepository.findByAccountIdAndStatus(accountId, TransactionStatus.PENDING);
    }
}
