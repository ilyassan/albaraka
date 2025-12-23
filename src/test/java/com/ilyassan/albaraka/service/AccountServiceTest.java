package com.ilyassan.albaraka.service;

import com.ilyassan.albaraka.entity.Account;
import com.ilyassan.albaraka.entity.User;
import com.ilyassan.albaraka.entity.UserRole;
import com.ilyassan.albaraka.repository.AccountRepository;
import com.ilyassan.albaraka.util.AccountNumberGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountNumberGenerator accountNumberGenerator;

    @InjectMocks
    private AccountService accountService;

    private User testUser;
    private Account testAccount;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .role(UserRole.CLIENT)
                .enabled(true)
                .build();

        testAccount = Account.builder()
                .id(1L)
                .user(testUser)
                .accountNumber("ALBARAKA202512171630459a7b8c9d")
                .balance(BigDecimal.ZERO)
                .build();
    }

    @Test
    void testCreateAccount() {
        String generatedNumber = "ALBARAKA202512171630459a7b8c9d";
        when(accountNumberGenerator.generateAccountNumber()).thenReturn(generatedNumber);
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        Account result = accountService.createAccount(testUser);

        assertNotNull(result);
        assertEquals(generatedNumber, result.getAccountNumber());
        assertEquals(BigDecimal.ZERO, result.getBalance());
        assertEquals(testUser, result.getUser());
        verify(accountRepository, times(1)).save(any(Account.class));
    }

    @Test
    void testGetAccountByNumber() {
        when(accountRepository.findByAccountNumber("ALBARAKA202512171630459a7b8c9d"))
                .thenReturn(Optional.of(testAccount));

        Optional<Account> result = accountService.getAccountByNumber("ALBARAKA202512171630459a7b8c9d");

        assertTrue(result.isPresent());
        assertEquals(testAccount, result.get());
        verify(accountRepository, times(1)).findByAccountNumber("ALBARAKA202512171630459a7b8c9d");
    }

    @Test
    void testGetAccountByUserId() {
        when(accountRepository.findByUserId(1L))
                .thenReturn(Optional.of(testAccount));

        Optional<Account> result = accountService.getAccountByUserId(1L);

        assertTrue(result.isPresent());
        assertEquals(testAccount, result.get());
        verify(accountRepository, times(1)).findByUserId(1L);
    }

    @Test
    void testGetAccountById() {
        when(accountRepository.findById(1L))
                .thenReturn(Optional.of(testAccount));

        Optional<Account> result = accountService.getAccountById(1L);

        assertTrue(result.isPresent());
        assertEquals(testAccount, result.get());
    }

    @Test
    void testGetBalance() {
        when(accountRepository.findById(1L))
                .thenReturn(Optional.of(testAccount));

        BigDecimal balance = accountService.getBalance(1L);

        assertEquals(BigDecimal.ZERO, balance);
    }

    @Test
    void testUpdateBalance() {
        when(accountRepository.findById(1L))
                .thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class)))
                .thenReturn(testAccount);

        accountService.updateBalance(1L, new BigDecimal("1000"));

        verify(accountRepository, times(1)).findById(1L);
        verify(accountRepository, times(1)).save(any(Account.class));
    }

    @Test
    void testGetAccountByNumberNotFound() {
        when(accountRepository.findByAccountNumber("INVALID"))
                .thenReturn(Optional.empty());

        Optional<Account> result = accountService.getAccountByNumber("INVALID");

        assertFalse(result.isPresent());
    }
}
