package com.ilyassan.albaraka.repository;

import com.ilyassan.albaraka.entity.User;
import com.ilyassan.albaraka.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private User testUser;

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
    }

    @Test
    void testSaveUser() {
        User savedUser = userRepository.save(testUser);

        assertNotNull(savedUser.getId());
        assertEquals("test@example.com", savedUser.getEmail());
    }

    @Test
    void testFindByEmail() {
        userRepository.save(testUser);

        Optional<User> result = userRepository.findByEmail("test@example.com");

        assertTrue(result.isPresent());
        assertEquals("test@example.com", result.get().getEmail());
    }

    @Test
    void testFindByEmailNotFound() {
        Optional<User> result = userRepository.findByEmail("notfound@example.com");

        assertFalse(result.isPresent());
    }

    @Test
    void testExistsByEmail() {
        userRepository.save(testUser);

        assertTrue(userRepository.existsByEmail("test@example.com"));
        assertFalse(userRepository.existsByEmail("notfound@example.com"));
    }

    @Test
    void testFindByRole() {
        User adminUser = User.builder()
                .email("admin@example.com")
                .password("password")
                .firstName("Admin")
                .lastName("User")
                .role(UserRole.ADMIN)
                .enabled(true)
                .build();

        userRepository.save(testUser);
        userRepository.save(adminUser);

        List<User> clients = userRepository.findByRole(UserRole.CLIENT);
        List<User> admins = userRepository.findByRole(UserRole.ADMIN);

        assertEquals(1, clients.size());
        assertEquals(1, admins.size());
        assertEquals(UserRole.CLIENT, clients.get(0).getRole());
        assertEquals(UserRole.ADMIN, admins.get(0).getRole());
    }

    @Test
    void testDeleteUser() {
        User savedUser = userRepository.save(testUser);
        userRepository.delete(savedUser);

        Optional<User> result = userRepository.findById(savedUser.getId());

        assertFalse(result.isPresent());
    }
}
