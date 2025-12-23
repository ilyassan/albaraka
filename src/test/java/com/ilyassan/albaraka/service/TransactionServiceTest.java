package com.ilyassan.albaraka.service;

import com.ilyassan.albaraka.entity.Account;
import com.ilyassan.albaraka.entity.Transaction;
import com.ilyassan.albaraka.entity.TransactionStatus;
import com.ilyassan.albaraka.entity.User;
import com.ilyassan.albaraka.entity.UserRole;
import com.ilyassan.albaraka.repository.AccountRepository;
import com.ilyassan.albaraka.repository.TransactionRepository;
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
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountService accountService;

    @InjectMocks
    private TransactionService transactionService;

    private User testUser;
    private Account testAccount;
    private Transaction testTransaction;

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
                .balance(new BigDecimal("5000"))
                .build();

        testTransaction = Transaction.builder()
                .id(1L)
                .account(testAccount)
                .type("DEPOSIT")
                .amount(new BigDecimal("500"))
                .status(TransactionStatus.COMPLETED)
                .build();
    }

    @Test
    void testCreateDepositAutoApproved() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));

        Transaction depositTransaction = Transaction.builder()
                .id(1L)
                .account(testAccount)
                .type("DEPOSIT")
                .amount(new BigDecimal("5000"))
                .status(TransactionStatus.COMPLETED)
                .build();

        when(transactionRepository.save(any(Transaction.class))).thenReturn(depositTransaction);

        Transaction result = transactionService.createDeposit(1L, new BigDecimal("5000"));

        assertEquals(TransactionStatus.COMPLETED, result.getStatus());
        assertEquals("DEPOSIT", result.getType());
        assertEquals(new BigDecimal("5000"), result.getAmount());
        verify(accountService, times(1)).updateBalance(1L, new BigDecimal("5000"));
    }

    @Test
    void testCreateDepositPending() {
        Transaction pendingTransaction = Transaction.builder()
                .id(1L)
                .account(testAccount)
                .type("DEPOSIT")
                .amount(new BigDecimal("15000"))
                .status(TransactionStatus.PENDING)
                .build();

        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(pendingTransaction);

        Transaction result = transactionService.createDeposit(1L, new BigDecimal("15000"));

        assertEquals(TransactionStatus.PENDING, result.getStatus());
        verify(accountService, never()).updateBalance(anyLong(), any());
    }

    @Test
    void testCreateWithdrawalAutoApproved() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);

        Transaction result = transactionService.createWithdrawal(1L, new BigDecimal("2000"));

        assertEquals(TransactionStatus.COMPLETED, result.getStatus());
        verify(accountService, times(1)).updateBalance(1L, new BigDecimal("-2000"));
    }

    @Test
    void testCreateWithdrawalInsufficientBalance() {
        testAccount.setBalance(new BigDecimal("100"));
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));

        assertThrows(IllegalArgumentException.class, () -> {
            transactionService.createWithdrawal(1L, new BigDecimal("5000"));
        });
    }

    @Test
    void testCreateTransfer() {
        Account beneficiaryAccount = Account.builder()
                .id(2L)
                .user(testUser)
                .accountNumber("ALBARAKA202512171630459a7b8c9e")
                .balance(BigDecimal.ZERO)
                .build();

        Transaction transferTransaction = Transaction.builder()
                .id(1L)
                .account(testAccount)
                .type("TRANSFER")
                .amount(new BigDecimal("1000"))
                .beneficiaryAccountId(2L)
                .status(TransactionStatus.COMPLETED)
                .build();

        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(beneficiaryAccount));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transferTransaction);

        Transaction result = transactionService.createTransfer(1L, 2L, new BigDecimal("1000"));

        assertEquals(TransactionStatus.COMPLETED, result.getStatus());
        assertEquals("TRANSFER", result.getType());
        verify(accountService, times(2)).updateBalance(anyLong(), any());
    }

    @Test
    void testApproveTransaction() {
        Transaction pendingTransaction = Transaction.builder()
                .id(1L)
                .account(testAccount)
                .type("DEPOSIT")
                .amount(new BigDecimal("15000"))
                .status(TransactionStatus.PENDING)
                .build();

        Transaction approvedTransaction = Transaction.builder()
                .id(1L)
                .account(testAccount)
                .type("DEPOSIT")
                .amount(new BigDecimal("15000"))
                .status(TransactionStatus.COMPLETED)
                .build();

        when(transactionRepository.findById(1L)).thenReturn(Optional.of(pendingTransaction));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(approvedTransaction);

        Transaction result = transactionService.approveTransaction(1L);

        assertEquals(TransactionStatus.COMPLETED, result.getStatus());
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void testRejectTransaction() {
        Transaction pendingTransaction = Transaction.builder()
                .id(1L)
                .account(testAccount)
                .type("DEPOSIT")
                .amount(new BigDecimal("15000"))
                .status(TransactionStatus.PENDING)
                .build();

        Transaction rejectedTransaction = Transaction.builder()
                .id(1L)
                .account(testAccount)
                .type("DEPOSIT")
                .amount(new BigDecimal("15000"))
                .status(TransactionStatus.REJECTED)
                .build();

        when(transactionRepository.findById(1L)).thenReturn(Optional.of(pendingTransaction));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(rejectedTransaction);

        Transaction result = transactionService.rejectTransaction(1L);

        assertEquals(TransactionStatus.REJECTED, result.getStatus());
    }

    @Test
    void testApproveNonPendingTransaction() {
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(testTransaction));

        assertThrows(IllegalArgumentException.class, () -> {
            transactionService.approveTransaction(1L);
        });
    }
}
