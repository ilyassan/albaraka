package com.ilyassan.albaraka.repository;

import com.ilyassan.albaraka.entity.Account;
import com.ilyassan.albaraka.entity.Transaction;
import com.ilyassan.albaraka.entity.TransactionStatus;
import com.ilyassan.albaraka.entity.User;
import com.ilyassan.albaraka.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class TransactionRepositoryTest {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    private Account testAccount;
    private Transaction testTransaction;

    @BeforeEach
    void setUp() {
        User user = User.builder()
                .email("test@example.com")
                .password("password")
                .firstName("John")
                .lastName("Doe")
                .role(UserRole.CLIENT)
                .enabled(true)
                .build();

        userRepository.save(user);

        testAccount = Account.builder()
                .accountNumber("ALBARAKA202512171630459a7b8c9d")
                .user(user)
                .balance(new BigDecimal("5000"))
                .build();

        accountRepository.save(testAccount);

        testTransaction = Transaction.builder()
                .account(testAccount)
                .type("DEPOSIT")
                .amount(new BigDecimal("1000"))
                .status(TransactionStatus.COMPLETED)
                .build();
    }

    @Test
    void testSaveTransaction() {
        Transaction savedTransaction = transactionRepository.save(testTransaction);

        assertNotNull(savedTransaction.getId());
        assertEquals("DEPOSIT", savedTransaction.getType());
        assertEquals(TransactionStatus.COMPLETED, savedTransaction.getStatus());
    }

    @Test
    void testFindByAccountId() {
        transactionRepository.save(testTransaction);

        List<Transaction> result = transactionRepository.findByStatus(TransactionStatus.COMPLETED);

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals(testAccount.getId(), result.get(0).getAccount().getId());
    }

    @Test
    void testFindByStatus() {
        transactionRepository.save(testTransaction);

        List<Transaction> result = transactionRepository.findByStatus(TransactionStatus.COMPLETED);

        assertEquals(1, result.size());
        assertEquals(TransactionStatus.COMPLETED, result.get(0).getStatus());
    }

    @Test
    void testFindByAccountIdAndStatus() {
        transactionRepository.save(testTransaction);

        List<Transaction> result = transactionRepository.findByAccountIdAndStatus(
                testAccount.getId(),
                TransactionStatus.COMPLETED
        );

        assertEquals(1, result.size());
        assertEquals(TransactionStatus.COMPLETED, result.get(0).getStatus());
    }

    @Test
    void testCreateMultipleTransactions() {
        Transaction transaction2 = Transaction.builder()
                .account(testAccount)
                .type("WITHDRAWAL")
                .amount(new BigDecimal("500"))
                .status(TransactionStatus.COMPLETED)
                .build();

        transactionRepository.save(testTransaction);
        transactionRepository.save(transaction2);

        List<Transaction> result = transactionRepository.findByStatus(TransactionStatus.COMPLETED);

        assertEquals(2, result.size());
    }
}
