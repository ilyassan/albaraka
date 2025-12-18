package com.ilyassan.albaraka.service;

import com.ilyassan.albaraka.entity.User;
import com.ilyassan.albaraka.entity.UserRole;
import com.ilyassan.albaraka.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AccountService accountService;

    @Transactional
    public User createUser(String email, String password, String firstName, String lastName, UserRole role) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists");
        }

        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .firstName(firstName)
                .lastName(lastName)
                .role(role)
                .enabled(true)
                .build();

        User savedUser = userRepository.save(user);

        // Create account for CLIENT role
        if (role == UserRole.CLIENT) {
            accountService.createAccount(savedUser);
            log.info("User and account created for CLIENT: {}", email);
        } else {
            log.info("User created with role {}: {}", role, email);
        }

        return savedUser;
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public List<User> getUsersByRole(UserRole role) {
        return userRepository.findByRole(role);
    }

    @Transactional
    public User updateUser(Long id, String firstName, String lastName) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setFirstName(firstName);
        user.setLastName(lastName);

        log.info("User updated: {}", id);
        return userRepository.save(user);
    }

    @Transactional
    public void deactivateUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setEnabled(false);
        userRepository.save(user);
        log.info("User deactivated: {}", id);
    }
}
