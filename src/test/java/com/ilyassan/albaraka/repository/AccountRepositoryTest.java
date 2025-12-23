package com.ilyassan.albaraka.repository;

import com.ilyassan.albaraka.entity.Account;
import com.ilyassan.albaraka.entity.User;
import com.ilyassan.albaraka.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class AccountRepositoryTest {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private Account testAccount;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("test@example.com")
                .password("password")
                .firstName("John")
                .lastName("Doe")
                .role(UserRole.CLIENT)
                .enabled(true)
                .build();

        userRepository.save(testUser);

        testAccount = Account.builder()
                .accountNumber("ALBARAKA202512171630459a7b8c9d")
                .user(testUser)
                .balance(BigDecimal.ZERO)
                .build();
    }

    @Test
    void testSaveAccount() {
        Account savedAccount = accountRepository.save(testAccount);

        assertNotNull(savedAccount.getId());
        assertEquals("ALBARAKA202512171630459a7b8c9d", savedAccount.getAccountNumber());
        assertEquals(testUser.getId(), savedAccount.getUser().getId());
    }

    @Test
    void testFindByAccountNumber() {
        accountRepository.save(testAccount);

        Optional<Account> result = accountRepository.findByAccountNumber("ALBARAKA202512171630459a7b8c9d");

        assertTrue(result.isPresent());
        assertEquals("ALBARAKA202512171630459a7b8c9d", result.get().getAccountNumber());
    }

    @Test
    void testFindByAccountNumberNotFound() {
        Optional<Account> result = accountRepository.findByAccountNumber("INVALID");

        assertFalse(result.isPresent());
    }

    @Test
    void testFindByUserId() {
        accountRepository.save(testAccount);

        Optional<Account> result = accountRepository.findByUserId(testUser.getId());

        assertTrue(result.isPresent());
        assertEquals(testUser.getId(), result.get().getUser().getId());
    }

    @Test
    void testFindByUserIdNotFound() {
        Optional<Account> result = accountRepository.findByUserId(999L);

        assertFalse(result.isPresent());
    }

    @Test
    void testUpdateBalance() {
        Account savedAccount = accountRepository.save(testAccount);
        savedAccount.setBalance(new BigDecimal("5000"));
        accountRepository.save(savedAccount);

        Optional<Account> result = accountRepository.findById(savedAccount.getId());

        assertTrue(result.isPresent());
        assertEquals(new BigDecimal("5000"), result.get().getBalance());
    }

    @Test
    void testDeleteAccount() {
        Account savedAccount = accountRepository.save(testAccount);
        accountRepository.delete(savedAccount);

        Optional<Account> result = accountRepository.findById(savedAccount.getId());

        assertFalse(result.isPresent());
    }
}
