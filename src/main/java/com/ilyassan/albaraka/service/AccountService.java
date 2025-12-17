package com.ilyassan.albaraka.service;

import com.ilyassan.albaraka.entity.Account;
import com.ilyassan.albaraka.entity.User;
import com.ilyassan.albaraka.repository.AccountRepository;
import com.ilyassan.albaraka.util.AccountNumberGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@Slf4j
public class AccountService {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AccountNumberGenerator accountNumberGenerator;

    @Transactional
    public Account createAccount(User user) {
        String accountNumber = accountNumberGenerator.generateAccountNumber();

        Account account = Account.builder()
                .user(user)
                .accountNumber(accountNumber)
                .balance(BigDecimal.ZERO)
                .build();

        Account savedAccount = accountRepository.save(account);
        log.info("Account created for user: {} with account number: {}", user.getEmail(), accountNumber);
        return savedAccount;
    }

    public Optional<Account> getAccountByNumber(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber);
    }

    public Optional<Account> getAccountByUserId(Long userId) {
        return accountRepository.findByUserId(userId);
    }

    public Optional<Account> getAccountById(Long accountId) {
        return accountRepository.findById(accountId);
    }

    public BigDecimal getBalance(Long accountId) {
        return accountRepository.findById(accountId)
                .map(Account::getBalance)
                .orElse(null);
    }

    @Transactional
    public void updateBalance(Long accountId, BigDecimal amount) {
        accountRepository.findById(accountId).ifPresent(account -> {
            account.setBalance(account.getBalance().add(amount));
            accountRepository.save(account);
            log.debug("Balance updated for account: {} new balance: {}", accountId, account.getBalance());
        });
    }
}
