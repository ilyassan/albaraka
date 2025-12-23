package com.ilyassan.albaraka.controller;

import com.ilyassan.albaraka.entity.Account;
import com.ilyassan.albaraka.entity.User;
import com.ilyassan.albaraka.entity.UserRole;
import com.ilyassan.albaraka.repository.AccountRepository;
import com.ilyassan.albaraka.repository.UserRepository;
import com.ilyassan.albaraka.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private Account testAccount;

    @BeforeEach
    void setUp() {
        accountRepository.deleteAll();
        userRepository.deleteAll();

        testUser = User.builder()
                .email("client@example.com")
                .password(passwordEncoder.encode("password123"))
                .firstName("John")
                .lastName("Doe")
                .role(UserRole.CLIENT)
                .enabled(true)
                .build();

        userRepository.save(testUser);

        testAccount = Account.builder()
                .accountNumber("ALBARAKA202512171630459a7b8c9d")
                .user(testUser)
                .balance(new BigDecimal("5000"))
                .build();

        accountRepository.save(testAccount);
    }

    @Test
    @WithMockUser(username = "client@example.com", roles = "CLIENT")
    void testGetCurrentUserAccount() throws Exception {
        mockMvc.perform(get("/api/accounts/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("ALBARAKA202512171630459a7b8c9d"))
                .andExpect(jsonPath("$.balance").value(5000));
    }

    @Test
    @WithMockUser(username = "nonexistent@example.com", roles = "CLIENT")
    void testGetCurrentUserAccountNotFound() throws Exception {
        mockMvc.perform(get("/api/accounts/me"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetCurrentUserAccountUnauthorized() throws Exception {
        mockMvc.perform(get("/api/accounts/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void testGetAccountByIdAsAdmin() throws Exception {
        mockMvc.perform(get("/api/accounts/" + testAccount.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("ALBARAKA202512171630459a7b8c9d"));
    }

    @Test
    @WithMockUser(username = "client@example.com", roles = "CLIENT")
    void testGetAccountByIdAsClient() throws Exception {
        mockMvc.perform(get("/api/accounts/" + testAccount.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "client@example.com", roles = "CLIENT")
    void testGetBalance() throws Exception {
        mockMvc.perform(get("/api/accounts/me/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(5000));
    }
}
